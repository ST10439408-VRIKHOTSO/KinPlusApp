package za.co.kinplus.app.data.repository

import android.util.Log
import za.co.kinplus.app.data.remote.KinPlusApi
import za.co.kinplus.app.data.remote.dto.LocationUploadRequest
import za.co.kinplus.app.data.sync.ActionType
import za.co.kinplus.app.data.sync.OfflineQueue
import za.co.kinplus.app.util.ConnectivityObserver
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Publishes this device's location (FR-13). When offline, the location is
 * queued and uploaded in a batch on reconnect (FR-28, FR-29), which is why the
 * timestamp is captured on the device rather than the server.
 */
@Singleton
class LocationRepository @Inject constructor(
    private val api: KinPlusApi,
    private val offlineQueue: OfflineQueue,
    private val connectivity: ConnectivityObserver
) {
    suspend fun publish(lat: Double, lng: Double, accuracyM: Double, batteryPct: Int) {
        val request = LocationUploadRequest(
            lat = lat,
            lng = lng,
            accuracyM = accuracyM,
            batteryPct = batteryPct,
            capturedAt = Instant.now().toString()
        )

        if (!connectivity.isOnlineNow()) {
            offlineQueue.enqueue(ActionType.LOCATION, request)
            return
        }
        try {
            api.uploadLocation(request)
        } catch (e: Exception) {
            offlineQueue.enqueue(ActionType.LOCATION, request)
            Log.w(TAG, "Location upload failed; queued", e)
        }
    }

    private companion object {
        const val TAG = "LocationRepository"
    }
}
