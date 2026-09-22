package za.co.kinplus.app.location

import android.annotation.SuppressLint
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.LocationSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Feeds the map's blue "my location" dot a GPS fix whose bearing comes from
 * the device compass rather than GPS course — so the direction cone tracks
 * which way the phone is facing even while stationary. Position updates
 * (every few seconds) and heading updates (much more frequent) are merged:
 * a heading change re-emits the last known position with the new bearing,
 * so the dot rotates smoothly without waiting for the next GPS fix.
 */
class HeadingLocationSource(
    private val fusedClient: FusedLocationProviderClient,
    private val compassProvider: CompassProvider,
    private val scope: CoroutineScope
) : LocationSource {

    private var callback: LocationCallback? = null
    private var headingJob: Job? = null
    private var lastLocation: Location? = null

    @SuppressLint("MissingPermission")
    override fun activate(listener: LocationSource.OnLocationChangedListener) {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L).build()
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                location.bearing = lastLocation?.bearing ?: 0f
                lastLocation = location
                listener.onLocationChanged(location)
            }
        }
        callback = locationCallback
        fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())

        headingJob = scope.launch {
            compassProvider.headingUpdates().collect { heading ->
                val current = lastLocation ?: return@collect
                val withHeading = Location(current).apply { bearing = heading }
                lastLocation = withHeading
                listener.onLocationChanged(withHeading)
            }
        }
    }

    override fun deactivate() {
        callback?.let { fusedClient.removeLocationUpdates(it) }
        headingJob?.cancel()
        callback = null
        headingJob = null
        lastLocation = null
    }
}
