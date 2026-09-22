package za.co.kinplus.app.location

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import za.co.kinplus.app.domain.SafeZone
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registers Android geofences for the user's Safe Zones (FR-21). Transitions
 * are delivered to [GeofenceBroadcastReceiver], which reports the crossing to
 * the API (or queues it while offline).
 */
@Singleton
class GeofenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val client: GeofencingClient = LocationServices.getGeofencingClient(context)

    private val pendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    /** Re-registers all geofences from the current (cached) Safe Zones. */
    suspend fun syncZones(zones: List<SafeZone>) {
        if (!hasBackgroundLocation()) {
            Log.w(TAG, "Background location not granted; skipping geofence registration")
            return
        }
        // Clear existing registrations, then add the current set.
        runCatching { client.removeGeofences(pendingIntent).await() }

        val geofences = zones.map { zone ->
            var transitions = 0
            if (zone.notifyOnEnter) transitions = transitions or Geofence.GEOFENCE_TRANSITION_ENTER
            if (zone.notifyOnExit) transitions = transitions or Geofence.GEOFENCE_TRANSITION_EXIT

            Geofence.Builder()
                .setRequestId(zone.id)
                .setCircularRegion(zone.lat, zone.lng, zone.radiusM.toFloat())
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(if (transitions == 0) Geofence.GEOFENCE_TRANSITION_ENTER else transitions)
                .build()
        }
        if (geofences.isEmpty()) return

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(0)
            .addGeofences(geofences)
            .build()

        try {
            @Suppress("MissingPermission")
            client.addGeofences(request, pendingIntent).await()
            Log.i(TAG, "Registered ${geofences.size} geofence(s)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register geofences", e)
        }
    }

    private fun hasBackgroundLocation(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private companion object {
        const val TAG = "GeofenceManager"
    }
}
