package za.co.kinplus.app.data.repository

import android.util.Log
import za.co.kinplus.app.data.remote.KinPlusApi
import za.co.kinplus.app.data.remote.dto.AlertRequest
import za.co.kinplus.app.data.sync.ActionType
import za.co.kinplus.app.data.sync.OfflineQueue
import za.co.kinplus.app.domain.CommunityAlert
import za.co.kinplus.app.util.ConnectivityObserver
import za.co.kinplus.app.util.Resource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Community safety alerts (FR-25 to FR-27). Reports are submitted as PENDING
 * and only approved reports are returned by the nearby query; the reporter's
 * identity is never exposed by the API.
 */
@Singleton
class AlertRepository @Inject constructor(
    private val api: KinPlusApi,
    private val offlineQueue: OfflineQueue,
    private val connectivity: ConnectivityObserver
) {
    suspend fun nearby(lat: Double, lng: Double, radiusKm: Double? = null): Resource<List<CommunityAlert>> = try {
        val list = api.getNearbyAlerts(lat, lng, radiusKm).map { it ->
            CommunityAlert(it.id, it.category, it.description, it.lat, it.lng, it.photoBlobUrl, it.status, it.confirmCount)
        }
        Resource.Success(list)
    } catch (e: Exception) {
        Resource.Error("Could not load nearby reports.", e)
    }

    suspend fun submit(
        category: String, description: String, lat: Double, lng: Double, photoBlobUrl: String?
    ): Resource<Unit> {
        val request = AlertRequest(category, description, lat, lng, photoBlobUrl)
        if (!connectivity.isOnlineNow()) {
            offlineQueue.enqueue(ActionType.ALERT, request)
            return Resource.Success(Unit, fromCache = true)
        }
        return try {
            api.createAlert(request)
            Resource.Success(Unit)
        } catch (e: Exception) {
            offlineQueue.enqueue(ActionType.ALERT, request)
            Log.w(TAG, "Alert submit failed; queued", e)
            Resource.Success(Unit, fromCache = true)
        }
    }

    suspend fun confirm(id: String): Resource<Unit> = try {
        api.confirmAlert(id)
        Resource.Success(Unit)
    } catch (e: Exception) {
        Resource.Error("Could not confirm the report.", e)
    }

    private companion object {
        const val TAG = "AlertRepository"
    }
}
