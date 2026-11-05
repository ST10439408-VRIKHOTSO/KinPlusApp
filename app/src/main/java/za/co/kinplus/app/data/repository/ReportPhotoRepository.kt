package za.co.kinplus.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import za.co.kinplus.app.data.remote.KinPlusApi
import za.co.kinplus.app.data.remote.dto.UploadUrlRequest
import java.io.File
import java.net.URI
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Photos attached to Community Alert reports (FR-26), stored in Azure Blob
 * Storage, the PoE's blob storage outcome.
 *
 * 1. The picked image is copied into app storage as an upright JPEG of at
 *    most [MAX_SIDE_PX] pixels, so uploads stay small on mobile data.
 * 2. The Kin+ API issues a write only SAS URL that expires after 15 minutes
 *    (POST alerts/upload-url).
 * 3. The bytes are PUT straight to Azure with that URL, so the image never
 *    passes through the API server, and the blob's URL is saved on the report.
 *
 * The upload uses a copy of the API's OkHttp client without the Firebase
 * token interceptor: Azure is authorised by the SAS in the URL, and the
 * user's token must never be sent to another host.
 *
 * References:
 * Microsoft (2023) Put Blob (REST API). Available at:
 * https://learn.microsoft.com/en-us/rest/api/storageservices/put-blob [Accessed 5 November 2026].
 * Microsoft (2026) Create a service SAS. Available at:
 * https://learn.microsoft.com/en-us/rest/api/storageservices/create-service-sas [Accessed 5 November 2026].
 * Android Developers (2026) Loading Large Bitmaps Efficiently. Available at:
 * https://developer.android.com/topic/performance/graphics/load-bitmap [Accessed 5 November 2026].
 */
@Singleton
class ReportPhotoRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: KinPlusApi,
    apiClient: OkHttpClient
) {
    private val blobClient: OkHttpClient = apiClient.newBuilder()
        .apply {
            interceptors().clear()
            networkInterceptors().clear()
        }
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Copies the picked image into app storage as an upright JPEG no larger
     * than [MAX_SIDE_PX] on its longest side. Returns null when the image
     * cannot be read. The file is kept until the report has been sent, so a
     * report made offline still has its photo when it syncs.
     */
    suspend fun prepare(uri: Uri): File? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, MAX_SIDE_PX)
            }
            val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: error("The picked file is not an image")
            val orientation = resolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
            val photo = scaleDown(upright(decoded, orientation), MAX_SIDE_PX)

            val dir = File(context.filesDir, PHOTO_DIR).apply { mkdirs() }
            val file = File(dir, "${UUID.randomUUID()}.jpg")
            file.outputStream().use { photo.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
            Log.i(TAG, "Prepared report photo ${photo.width}x${photo.height}, ${file.length() / 1024} KB")
            file
        }.onFailure { Log.e(TAG, "Could not read the picked photo", it) }.getOrNull()
    }

    /**
     * Uploads [file] to Azure Blob Storage and returns the blob's URL, or null
     * when it could not be uploaded (offline, or blob storage not configured
     * on the server, which answers 503). A failed upload never blocks the report.
     */
    suspend fun upload(file: File): String? = withContext(Dispatchers.IO) {
        try {
            val target = api.createUploadUrl(UploadUrlRequest(JPEG))
            val request = Request.Builder()
                .url(target.uploadUrl)
                .header("x-ms-blob-type", "BlockBlob")
                .put(file.asRequestBody(JPEG.toMediaType()))
                .build()
            val uploaded = blobClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) Log.w(TAG, "Azure rejected the photo upload: HTTP ${response.code}")
                response.isSuccessful
            }
            if (uploaded) {
                Log.i(TAG, "Report photo uploaded to blob storage (${file.length() / 1024} KB)")
                target.blobUrl
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Report photo upload failed", e)
            null
        }
    }

    /** Downloads a report photo for the details dialog, decoded no larger than [maxSidePx]. */
    suspend fun load(url: String, maxSidePx: Int): Bitmap? = withContext(Dispatchers.IO) {
        if (!isBlobStorageUrl(url)) {
            Log.w(TAG, "Not loading a report photo from an unexpected host")
            return@withContext null
        }
        runCatching {
            blobClient.newCall(Request.Builder().url(url).get().build()).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Report photo not available: HTTP ${response.code}")
                    return@use null
                }
                val bytes = response.body?.bytes() ?: return@use null
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                val options = BitmapFactory.Options().apply {
                    inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxSidePx)
                }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            }
        }.onFailure { Log.w(TAG, "Could not load the report photo", it) }.getOrNull()
    }

    /** Deletes a prepared photo that is no longer needed. */
    fun discard(file: File?) {
        if (file != null && file.exists() && !file.delete()) Log.w(TAG, "Could not delete ${file.name}")
    }

    private fun upright(bitmap: Bitmap, exifOrientation: Int): Bitmap {
        val degrees = when (exifOrientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> return bitmap
        }
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun scaleDown(bitmap: Bitmap, maxSide: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxSide) return bitmap
        val scale = maxSide.toFloat() / longest
        return Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
    }

    companion object {
        const val JPEG = "image/jpeg"
        const val MAX_SIDE_PX = 1600
        const val JPEG_QUALITY = 80
        private const val PHOTO_DIR = "report_photos"
        private const val TAG = "ReportPhotoRepository"

        /**
         * The largest power of two sample size that still decodes the image at
         * [maxSide] pixels or more on its longest side (BitmapFactory only
         * samples in powers of two). Returns 1 for small or unknown sizes.
         */
        fun sampleSizeFor(width: Int, height: Int, maxSide: Int): Int {
            if (width <= 0 || height <= 0 || maxSide <= 0) return 1
            var sample = 1
            while (maxOf(width, height) / (sample * 2) >= maxSide) sample *= 2
            return sample
        }

        /**
         * Report photos are only ever loaded from Azure Blob Storage over HTTPS.
         * The URL is stored with the report, so this stops a tampered report from
         * making the app contact any other server.
         */
        fun isBlobStorageUrl(url: String): Boolean = runCatching {
            val uri = URI(url)
            uri.scheme == "https" && (uri.host?.endsWith(".blob.core.windows.net") == true)
        }.getOrDefault(false)
    }
}
