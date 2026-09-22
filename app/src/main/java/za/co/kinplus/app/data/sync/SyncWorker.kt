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
import za.co.kinplus.app.data.remote.KinPlusApi
import za.co.kinplus.app.data.remote.dto.*

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
    private val gson: Gson
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
                    }
                    ActionType.ZONE_EVENT -> {
                        val body = gson.fromJson(action.payloadJson, QueuedZoneEvent::class.java)
                        api.reportZoneEvent(body.zoneId, ZoneEventRequest(body.type))
                    }
                    ActionType.ALERT -> {
                        val body = gson.fromJson(action.payloadJson, AlertRequest::class.java)
                        api.createAlert(body)
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
