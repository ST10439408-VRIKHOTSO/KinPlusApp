package za.co.kinplus.app.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import za.co.kinplus.app.data.local.PendingActionDao
import za.co.kinplus.app.data.local.SosHistoryDao
import za.co.kinplus.app.data.remote.KinPlusApi
import za.co.kinplus.app.data.remote.dto.*
import za.co.kinplus.app.data.repository.ReportPhotoRepository
import java.io.File

/**
 * Replays queued offline actions against the API when connectivity returns
 * (FR-29). Each action carries its clientActionId so the server can ignore
 * duplicates. An action that fails is left in the queue and retried; the worker
 * only removes actions the server has accepted.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val api: KinPlusApi,
    private val dao: PendingActionDao,
    private val sosHistoryDao: SosHistoryDao,
    private val gson: Gson,
    private val photoRepository: ReportPhotoRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val queued = dao.getAll()
        if (queued.isEmpty()) return Result.success()

        Log.i(TAG, "Flushing ${queued.size} queued action(s)")
        var hadFailure = false

        for (action in queued) {
            try {
                when (ActionType.valueOf(action.type)) {
                    ActionType.LOCATION -> {
                        val body = gson.fromJson(action.payloadJson, LocationUploadRequest::class.java)
                            .copy(clientActionId = action.clientActionId)
                        api.uploadLocation(body)
                    }
                    ActionType.SOS -> {
                        val body = gson.fromJson(action.payloadJson, SosRequest::class.java)
                            .copy(clientActionId = action.clientActionId)
                        api.raiseSos(body)
                        // Incident History logged this SOS as queued; it has now gone out.
                        sosHistoryDao.markSent(action.clientActionId)
                    }
                    ActionType.ZONE_EVENT -> {
                        val body = gson.fromJson(action.payloadJson, QueuedZoneEvent::class.java)
                        api.reportZoneEvent(body.zoneId, ZoneEventRequest(body.type))
                    }
                    ActionType.ALERT -> {
                        val queued = gson.fromJson(action.payloadJson, QueuedAlert::class.java)
                        val localPhoto = queued.localPhotoPath?.let(::File)?.takeIf { it.exists() }
                        // A photo attached offline is uploaded to blob storage first. If
                        // that fails the report still goes out, without the photo.
                        val blobUrl = queued.photoBlobUrl ?: localPhoto?.let { photoRepository.upload(it) }
                        api.createAlert(AlertRequest(queued.category, queued.description, queued.lat, queued.lng, blobUrl))
                        photoRepository.discard(localPhoto)
                    }
                }
                dao.remove(action.clientActionId)
                Log.d(TAG, "Synced action ${action.type} (${action.clientActionId})")
            } catch (e: Exception) {
                hadFailure = true
                dao.incrementAttempts(action.clientActionId)
                Log.w(TAG, "Failed to sync ${action.clientActionId}, will retry", e)
            }
        }

        // Ask WorkManager to retry (with back-off) if anything is still pending.
        return if (hadFailure) Result.retry() else Result.success()
    }

    private companion object {
        const val TAG = "SyncWorker"
    }
}

/** Zone-event payload persisted in the queue (the id is not part of the API body). */
data class QueuedZoneEvent(val zoneId: String, val type: String)

/**
 * Community report payload persisted in the queue. It has the same fields as
 * the API's AlertRequest (so reports queued by older versions still parse),
 * plus the local path of a photo that still has to be uploaded.
 */
data class QueuedAlert(
    val category: String,
    val description: String,
    val lat: Double,
    val lng: Double,
    val photoBlobUrl: String? = null,
    val localPhotoPath: String? = null
)
