package za.co.kinplus.app.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** A single location fix with the data the API needs. */
data class LocationFix(val lat: Double, val lng: Double, val accuracyM: Double, val batteryPct: Int)

/** True if either fine or coarse location is granted — the single definition of "has location permission" the whole app uses. */
fun hasAnyLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

/**
 * Thin wrapper over the Fused Location Provider (FR-13). Returns the current
 * fix plus battery level, or null if permission has not been granted. Location
 * accuracy is balanced against battery use per NFR-02.
 */
@Singleton
class LocationProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fused = LocationServices.getFusedLocationProviderClient(context)

    fun hasLocationPermission(): Boolean = hasAnyLocationPermission(context)

    /**
     * The device's last cached fix, if any — returns near-instantly since it
     * never negotiates a new GPS/network fix. Used to centre the map the
     * moment it opens instead of waiting on [currentFix]'s slower fresh fix.
     */
    suspend fun lastKnownFix(): LocationFix? {
        if (!hasLocationPermission()) return null
        return try {
            @Suppress("MissingPermission")
            fused.lastLocation.await()?.let { LocationFix(it.latitude, it.longitude, it.accuracy.toDouble(), batteryPct()) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to obtain a last-known location", e)
            null
        }
    }

    suspend fun currentFix(): LocationFix? {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Location requested without permission")
            return null
        }
        val fresh = try {
            @Suppress("MissingPermission")
            fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to obtain a fresh location fix", e)
            null
        }
        // A fresh fix can come back null with no GPS signal (indoors, poor
        // reception); fall back to the last known fix rather than reporting
        // "couldn't get your location" when the device does have a recent one.
        val location = fresh ?: try {
            @Suppress("MissingPermission")
            fused.lastLocation.await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to obtain a last-known location", e)
            null
        }
        return location?.let { LocationFix(it.latitude, it.longitude, it.accuracy.toDouble(), batteryPct()) }
    }

    private fun batteryPct(): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0, 100)
    }

    private companion object {
        const val TAG = "LocationProvider"
    }
}
