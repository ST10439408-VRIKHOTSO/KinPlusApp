package za.co.kinplus.app.ui.navigation

/**
 * Central list of navigation routes. Matches the Part 1B navigation model
 * (Figure 4.13). Deep links from push notifications reuse the kinplus:// scheme.
 */
object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"

    // Bottom-navigation destinations
    const val HOME = "home"
    const val CIRCLES = "circles"
    const val SAFETY = "safety"
    const val PROFILE = "profile"

    // Secondary destinations
    const val CIRCLE_DETAIL = "circle/{circleId}"
    const val CREATE_JOIN_CIRCLE = "circle_create_join"
    const val SOS = "sos?category={category}"
    const val ZONES = "zones"
    const val ZONE_EDITOR = "zone_editor?zoneId={zoneId}"
    const val JOURNEY_SETUP = "journey_setup"
    const val JOURNEY_MONITOR = "journey_monitor"
    const val ALERTS = "alerts"
    const val INCIDENT_HISTORY = "incident_history"
    const val EDIT_PROFILE = "edit_profile"
    const val EMERGENCY_INFO = "emergency_info"
    const val EMERGENCY_INFO_FORM = "emergency_info_form/{category}"

    fun circleDetail(id: String) = "circle/$id"
    fun zoneEditor(zoneId: String? = null) = if (zoneId == null) "zone_editor?zoneId=" else "zone_editor?zoneId=$zoneId"
    fun emergencyInfoForm(category: String) = "emergency_info_form/$category"
    fun sos(category: String? = null) = if (category == null) "sos?category=" else "sos?category=$category"
}

/** Bottom navigation items (five primary destinations from the design, SOS centred). */
enum class BottomTab(val route: String) {
    MAP(Routes.HOME),
    CIRCLES(Routes.CIRCLES),
    SOS(Routes.SOS),
    ALERTS(Routes.ALERTS),
    SAFETY(Routes.SAFETY)
}
