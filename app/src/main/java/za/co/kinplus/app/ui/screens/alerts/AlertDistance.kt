package za.co.kinplus.app.ui.screens.alerts

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * Distance filter (Batch 1 enhancement to Community Alerts, FR-25). Reports are
 * sorted nearest first, each card shows how far away it is, and the user can
 * narrow the list to a radius. Distance is worked out on the device with the
 * haversine formula, so no extra API call is needed.
 *
 * Reference: Veness, C. (2022) Calculate distance between two latitude
 * longitude points. Available at: https://www.movable-type.co.uk/scripts/latlong.html
 * [Accessed 13 October 2026].
 */

/** Radius choices for the distance filter in km; null means any distance. */
internal val distanceFilters = listOf<Int?>(1, 5, 20, null)

/** Great circle distance in kilometres between two points. */
internal fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val earthRadiusKm = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2) * sin(dLng / 2)
    return earthRadiusKm * 2 * atan2(sqrt(a), sqrt(1 - a))
}
