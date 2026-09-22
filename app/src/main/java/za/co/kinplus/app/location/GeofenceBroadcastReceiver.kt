package za.co.kinplus.app.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import za.co.kinplus.app.data.repository.ZoneRepository
import javax.inject.Inject

/**
 * Receives Safe Zone geofence transitions from Google Play services (FR-21) and
 * reports each ENTER/EXIT crossing through [ZoneRepository]. Because a receiver
 * is short-lived, goAsync() keeps it alive while the suspend call runs.
 */
@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    @Inject lateinit var zoneRepository: ZoneRepository

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) {
            Log.e(TAG, "Geofence event error code ${event.errorCode}")
            return
        }

        val type = when (event.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> "ENTER"
            Geofence.GEOFENCE_TRANSITION_EXIT -> "EXIT"
            else -> return
        }

        val zoneIds = event.triggeringGeofences?.map { it.requestId } ?: emptyList()
        if (zoneIds.isEmpty()) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                zoneIds.forEach { zoneId ->
                    Log.i(TAG, "Geofence $type for zone $zoneId")
                    zoneRepository.reportEvent(zoneId, type)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "GeofenceReceiver"
    }
}
