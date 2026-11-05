package za.co.kinplus.app.data.repository

import android.util.Log
import za.co.kinplus.app.data.remote.KinPlusApi
import za.co.kinplus.app.data.remote.dto.AlertRequest
import za.co.kinplus.app.data.sync.ActionType
import za.co.kinplus.app.data.sync.OfflineQueue
import za.co.kinplus.app.data.sync.QueuedAlert
import za.co.kinplus.app.domain.CommunityAlert
import za.co.kinplus.app.util.ConnectivityObserver
import za.co.kinplus.app.util.Resource
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** What happened to a submitted community report, so the screen can say so. */
enum class ReportOutcome {
    /** Sent to the API, with its photo if one was attached. */
    SENT,
    /** Sent, but the attached photo could not be uploaded to blob storage. */
    SENT_WITHOUT_PHOTO,
    /** Saved in the offline queue; SyncWorker sends it (and its photo) on reconnect. */
    QUEUED
}

/**
 * Community safety alerts (FR-25 to FR-27). Reports are submitted as PENDING
 * and only approved reports are returned by the nearby query; the reporter's
 * identity is never exposed by the API. A report can carry one photo, which
 * is uploaded to Azure Blob Storage first (see [ReportPhotoRepository]).
 */
@Singleton
class AlertRepository @Inject constructor(
    private val api: KinPlusApi,
    private val offlineQueue: OfflineQueue,
    private val connectivity: ConnectivityObserver,
    private val photoRepository: ReportPhotoRepository
) {
    suspend fun nearby(lat: Double, lng: Double, radiusKm: Double? = null): Resource<List<CommunityAlert>> = try {
        val list = api.getNearbyAlerts(lat, lng, radiusKm).map { it ->
            CommunityAlert(it.id, it.category, it.description, it.lat, it.lng, it.photoBlobUrl, it.status, it.confirmCount)
        }
        Log.d(TAG, "Loaded ${list.size} approved report(s) nearby")
        Resource.Success(list)
    } catch (e: Exception) {
        Log.w(TAG, "Nearby reports could not be loaded", e)
        Resource.Error("Could not load nearby reports.", e)
    }

    /**
     * Sends a report. Offline, it is queued with the local path of its photo;
     * online, the photo is uploaded first and the report is sent even if the
     * upload fails, because the safety report matters more than the picture.
     */
    suspend fun submit(
        category: String, description: String, lat: Double, lng: Double, photo: File?
    ): ReportOutcome {
        if (!connectivity.isOnlineNow()) {
            offlineQueue.enqueue(ActionType.ALERT, QueuedAlert(category, description, lat, lng, localPhotoPath = photo?.absolutePath))
            Log.i(TAG, "Offline: report queued${if (photo != null) " with its photo" else ""}")
            return ReportOutcome.QUEUED
        }

        val blobUrl = photo?.let { photoRepository.upload(it) }
        return try {
            api.createAlert(AlertRequest(category, description, lat, lng, blobUrl))
            photoRepository.discard(photo)
            Log.i(TAG, "Report ($category) sent for moderation, photo: ${blobUrl != null}")
            if (photo != null && blobUrl == null) ReportOutcome.SENT_WITHOUT_PHOTO else ReportOutcome.SENT
        } catch (e: Exception) {
            // Keep the local photo for the retry only if it has not been uploaded yet.
            offlineQueue.enqueue(
                ActionType.ALERT,
                QueuedAlert(category, description, lat, lng, photoBlobUrl = blobUrl, localPhotoPath = photo?.absolutePath.takeIf { blobUrl == null })
            )
            if (blobUrl != null) photoRepository.discard(photo)
            Log.w(TAG, "Report submit failed; queued", e)
            ReportOutcome.QUEUED
        }
    }

    suspend fun confirm(id: String): Resource<Unit> = try {
        api.confirmAlert(id)
        Log.i(TAG, "Confirmed report $id")
        Resource.Success(Unit)
    } catch (e: Exception) {
        Log.w(TAG, "Could not confirm report $id", e)
        Resource.Error("Could not confirm the report.", e)
    }

    private companion object {
        const val TAG = "AlertRepository"
    }
}
