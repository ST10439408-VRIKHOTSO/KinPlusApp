package za.co.kinplus.app.data.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import za.co.kinplus.app.data.local.PendingActionDao
import za.co.kinplus.app.data.local.PendingActionEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central entry point for offline-with-sync behaviour (FR-29).
 *
 * When an action cannot reach the server (no connectivity, or a 5xx), the
 * repository calls [enqueue]. The action is persisted in Room with a stable
 * clientActionId and a WorkManager job is scheduled with a network constraint,
 * so Android flushes the queue automatically once the device is back online.
 */
@Singleton
class OfflineQueue @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: PendingActionDao,
    private val gson: Gson
) {
    val pendingCount: Flow<Int> = dao.observeCount()

    /** Serializes [payload] and queues it for later replay. Returns the action id. */
    suspend fun enqueue(type: ActionType, payload: Any): String {
        val id = UUID.randomUUID().toString()
        val entity = PendingActionEntity(
            clientActionId = id,
            type = type.name,
            payloadJson = gson.toJson(payload)
        )
        dao.enqueue(entity)
        Log.i(TAG, "Queued offline action ${type.name} ($id)")
        scheduleSync()
        return id
    }

    /** Schedules a one-off sync constrained to run only when online. */
    fun scheduleSync() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    companion object {
        const val WORK_NAME = "kinplus_offline_sync"
        private const val TAG = "OfflineQueue"
    }
}

/** The kinds of action that can be queued while offline. */
enum class ActionType { LOCATION, SOS, ZONE_EVENT, ALERT }
