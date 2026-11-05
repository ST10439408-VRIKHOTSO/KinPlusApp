package za.co.kinplus.app.data.repository

import android.util.Log
import za.co.kinplus.app.data.remote.KinPlusApi
import za.co.kinplus.app.data.remote.dto.JourneyRequest
import za.co.kinplus.app.domain.Journey
import za.co.kinplus.app.util.Resource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Safe Journey (FR-23, FR-24). A traveller declares a destination and expected
 * arrival time; watchers are notified on arrival or, from the server, on
 * overdue escalation.
 */
@Singleton
class JourneyRepository @Inject constructor(
    private val api: KinPlusApi
) {
    suspend fun startJourney(
        destinationLabel: String, destLat: Double?, destLng: Double?,
        etaIso: String, circleId: String?, watcherUids: List<String>
    ): Resource<Journey> = try {
        val dto = api.createJourney(
            JourneyRequest(destinationLabel, destLat, destLng, etaIso, circleId, watcherUids)
        )
        Log.i(TAG, "Journey ${dto.id} started with ${watcherUids.size} watcher(s), ETA $etaIso")
        Resource.Success(Journey(dto.id, dto.uid, dto.travellerName, dto.destinationLabel, dto.etaAt, dto.status))
    } catch (e: Exception) {
        Log.e(TAG, "Could not start the journey", e)
        Resource.Error("Could not start the journey.", e)
    }

    suspend fun confirmArrival(id: String): Resource<Unit> = try {
        api.arriveJourney(id)
        Log.i(TAG, "Arrival confirmed for journey $id; the API notifies the watchers")
        Resource.Success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Could not confirm arrival for journey $id", e)
        Resource.Error("Could not confirm arrival.", e)
    }

    suspend fun watchingJourneys(): Resource<List<Journey>> = try {
        val list = api.getWatchingJourneys()
            .map { Journey(it.id, it.uid, it.travellerName, it.destinationLabel, it.etaAt, it.status) }
        Log.d(TAG, "Watching ${list.size} active journey(s)")
        Resource.Success(list)
    } catch (e: Exception) {
        Log.w(TAG, "Could not load watched journeys", e)
        Resource.Error("Could not load journeys.", e)
    }

    private companion object {
        const val TAG = "JourneyRepository"
    }
}
