package za.co.kinplus.app.ui.common

import android.content.Context
import za.co.kinplus.app.R
import kotlin.math.roundToInt

/**
 * Shows distances in the units chosen in Settings > Units (FR-32): kilometres
 * and metres, or miles and feet. Distances are always worked out in kilometres
 * or metres and only converted for display, so filtering and the API stay metric.
 */
object DistanceFormat {

    const val KM_PER_MILE = 1.609344
    const val FEET_PER_METRE = 3.28084

    /** Community Alerts distance filter choices in the chosen unit; null means any distance. */
    fun filterOptions(metric: Boolean): List<Int?> =
        if (metric) listOf(1, 5, 20, null) else listOf(1, 3, 12, null)

    /** A filter choice (km or miles) converted to kilometres for filtering. */
    fun filterToKm(value: Int, metric: Boolean): Double =
        if (metric) value.toDouble() else value * KM_PER_MILE

    fun kmToMiles(km: Double): Double = km / KM_PER_MILE

    fun metresToFeet(metres: Int): Int = (metres * FEET_PER_METRE).roundToInt()

    /** "1.2 km away" or "0.7 mi away". */
    fun away(context: Context, km: Double, metric: Boolean): String =
        if (metric) context.getString(R.string.km_away, km)
        else context.getString(R.string.mi_away, kmToMiles(km))

    /** Label for a distance filter chip. */
    fun filterLabel(context: Context, value: Int?, metric: Boolean): String = when {
        value == null -> context.getString(R.string.alerts_distance_any)
        metric -> context.getString(R.string.alerts_distance_km, value)
        else -> context.getString(R.string.alerts_distance_mi, value)
    }

    /** A Safe Zone radius: "250 m" or "820 ft". */
    fun radius(context: Context, metres: Int, metric: Boolean): String =
        if (metric) context.getString(R.string.radius_metres, metres)
        else context.getString(R.string.radius_feet, metresToFeet(metres))
}
