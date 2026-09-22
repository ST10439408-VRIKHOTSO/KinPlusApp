package za.co.kinplus.app.data.repository

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import za.co.kinplus.app.auth.AuthManager
import za.co.kinplus.app.data.local.SosHistoryDao
import za.co.kinplus.app.data.local.SosHistoryEntity
import za.co.kinplus.app.data.remote.KinPlusApi
import za.co.kinplus.app.data.remote.dto.SosRequest
import za.co.kinplus.app.data.sync.ActionType
import za.co.kinplus.app.data.sync.OfflineQueue
import za.co.kinplus.app.domain.SosEvent
import za.co.kinplus.app.domain.SosHistoryItem
import za.co.kinplus.app.util.ConnectivityObserver
import za.co.kinplus.app.util.Resource
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Emergency SOS (FR-17 to FR-19). Because an SOS may be raised in an area with
 * no signal, this is the clearest example of offline-with-sync: if the network
 * call fails the alert is queued and delivered the moment connectivity returns
 * (Part 1B, Figure 6.3, alternate flow).
 *
 * Every raise is also logged to [SosHistoryDao] on-device, regardless of
 * outcome, so Safety > Recent Activity > Incident History has something real
 * to show even though the API has no "list my past SOS events" endpoint.
 */
@Singleton
class SosRepository @Inject constructor(
    private val api: KinPlusApi,
    private val offlineQueue: OfflineQueue,
    private val connectivity: ConnectivityObserver,
    private val sosHistoryDao: SosHistoryDao,
    private val authManager: AuthManager
) {
    val history: Flow<List<SosHistoryItem>> =
        sosHistoryDao.observeForUser(authManager.currentUid).map { rows ->
            rows.map { SosHistoryItem(it.id, it.status, it.message, it.raisedAt) }
        }

    /** Returns Resource.Success(null) when the SOS was queued for later delivery. */
    suspend fun raiseSos(
        lat: Double?, lng: Double?, message: String?, circleIds: List<String>
    ): Resource<SosEvent?> {
        val request = SosRequest(lat, lng, message, circleIds)

        if (!connectivity.isOnlineNow()) {
            offlineQueue.enqueue(ActionType.SOS, request)
            logHistory(status = "QUEUED", message = message, lat = lat, lng = lng)
            Log.w(TAG, "Offline: SOS queued for delivery on reconnect")
            return Resource.Success(null, fromCache = true)
        }

        return try {
            val dto = api.raiseSos(request)
            logHistory(status = "SENT", message = message, lat = lat, lng = lng)
            Resource.Success(SosEvent(dto.id, dto.raisedByName, dto.status, dto.message))
        } catch (e: Exception) {
            // Network failed mid-flight: fall back to the queue so the alert is not lost.
            offlineQueue.enqueue(ActionType.SOS, request)
            logHistory(status = "QUEUED", message = message, lat = lat, lng = lng)
            Log.w(TAG, "SOS send failed; queued for retry", e)
            Resource.Success(null, fromCache = true)
        }
    }

    suspend fun cancelSos(id: String): Resource<Unit> = try {
        api.cancelSos(id)
        Resource.Success(Unit)
    } catch (e: Exception) {
        Resource.Error("Could not cancel the SOS.", e)
    }

    private suspend fun logHistory(status: String, message: String?, lat: Double?, lng: Double?) {
        sosHistoryDao.insert(
            SosHistoryEntity(
                id = UUID.randomUUID().toString(),
                uid = authManager.currentUid,
                status = status,
                message = message,
                lat = lat,
                lng = lng
            )
        )
    }

    private companion object {
        const val TAG = "SosRepository"
    }
}
