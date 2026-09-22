package za.co.kinplus.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import za.co.kinplus.app.R
import za.co.kinplus.app.ui.screens.alerts.CommunityAlertsScreen
import za.co.kinplus.app.ui.screens.auth.LoginScreen
import za.co.kinplus.app.ui.screens.auth.RegisterScreen
import za.co.kinplus.app.ui.screens.auth.SplashScreen
import za.co.kinplus.app.ui.screens.circles.CircleDetailScreen
import za.co.kinplus.app.ui.screens.circles.CirclesScreen
import za.co.kinplus.app.ui.screens.circles.CreateJoinCircleScreen
import za.co.kinplus.app.ui.screens.home.HomeScreen
import za.co.kinplus.app.ui.screens.journey.JourneyMonitorScreen
import za.co.kinplus.app.ui.screens.journey.JourneySetupScreen
import za.co.kinplus.app.ui.screens.profile.EditProfileScreen
import za.co.kinplus.app.ui.screens.profile.EmergencyInfoCategory
import za.co.kinplus.app.ui.screens.profile.EmergencyInfoFormScreen
import za.co.kinplus.app.ui.screens.profile.EmergencyInformationScreen
import za.co.kinplus.app.ui.screens.profile.ProfileScreen
import za.co.kinplus.app.ui.screens.safety.IncidentHistoryScreen
import za.co.kinplus.app.ui.screens.safety.SafetyScreen
import za.co.kinplus.app.ui.screens.sos.SosScreen
import za.co.kinplus.app.ui.screens.zones.SafeZoneEditorScreen
import za.co.kinplus.app.ui.screens.zones.SafeZonesScreen

/**
 * The app's navigation graph. Four primary destinations (Map, Circles, Safety,
 * Profile) share a bottom navigation bar via [MainShell]; auth screens and
 * secondary screens are shown full-bleed.
 */
@Composable
fun KinPlusNavHost(
    navController: NavHostController,
    initialDeepLink: String?
) {
    NavHost(navController = navController, startDestination = Routes.SPLASH) {

        composable(Routes.SPLASH) {
            SplashScreen(
                onSignedIn = {
                    val target = deepLinkTarget(initialDeepLink)
                    navController.navigate(target) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onSignedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoggedIn = { navController.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } } },
                onRegister = { navController.navigate(Routes.REGISTER) }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegistered = { navController.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } } },
                onBackToLogin = { navController.popBackStack() }
            )
        }

        // ----- Primary destinations (bottom bar) -----
        composable(Routes.HOME) {
            MainShell(navController) {
                HomeScreen(
                    onOpenProfile = { navController.navigate(Routes.PROFILE) },
                    onOpenSafePlaces = { navController.navigate(Routes.ZONES) }
                )
            }
        }
        composable(Routes.CIRCLES) {
            MainShell(navController) {
                CirclesScreen(
                    onOpenCircle = { id -> navController.navigate(Routes.circleDetail(id)) },
                    onCreateJoin = { navController.navigate(Routes.CREATE_JOIN_CIRCLE) },
                    onOpenSafePlaces = { navController.navigate(Routes.ZONES) },
                    onOpenProfile = { navController.navigate(Routes.PROFILE) }
                )
            }
        }
        composable(Routes.SAFETY) {
            MainShell(navController) {
                SafetyScreen(
                    onOpenSos = { category -> navController.navigate(Routes.sos(category)) },
                    onOpenCircles = {
                        navController.navigate(Routes.CIRCLES) { popUpTo(Routes.HOME) { inclusive = false } }
                    },
                    onOpenSafePlaces = { navController.navigate(Routes.ZONES) },
                    onOpenSmartAlerts = { navController.navigate(Routes.JOURNEY_SETUP) },
                    onOpenCommunityAlerts = { navController.navigate(Routes.ALERTS) },
                    onOpenIncidentHistory = { navController.navigate(Routes.INCIDENT_HISTORY) },
                    onOpenEmergencyInfo = { navController.navigate(Routes.EMERGENCY_INFO) },
                    onOpenProfile = { navController.navigate(Routes.PROFILE) }
                )
            }
        }
        composable(Routes.ALERTS) {
            MainShell(navController) { CommunityAlertsScreen(onBack = null) }
        }
        composable(Routes.PROFILE) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onEditProfile = { navController.navigate(Routes.EDIT_PROFILE) },
                onOpenEmergencyInfo = { navController.navigate(Routes.EMERGENCY_INFO) },
                onSignedOut = { navController.navigate(Routes.LOGIN) { popUpTo(0) } }
            )
        }

        // ----- Secondary destinations -----
        composable(
            route = Routes.CIRCLE_DETAIL,
            arguments = listOf(navArgument("circleId") { type = NavType.StringType })
        ) { entry ->
            CircleDetailScreen(
                circleId = entry.arguments?.getString("circleId").orEmpty(),
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.CREATE_JOIN_CIRCLE) {
            CreateJoinCircleScreen(onDone = { navController.popBackStack() }, onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.SOS,
            arguments = listOf(navArgument("category") { type = NavType.StringType; nullable = true; defaultValue = null })
        ) { entry ->
            MainShell(navController) {
                SosScreen(
                    onClose = { navController.popBackStack() },
                    onOpenProfile = { navController.navigate(Routes.PROFILE) },
                    initialCategory = entry.arguments?.getString("category")
                )
            }
        }

        composable(Routes.ZONES) {
            SafeZonesScreen(
                onAddZone = { navController.navigate(Routes.zoneEditor()) },
                onEditZone = { id -> navController.navigate(Routes.zoneEditor(id)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.ZONE_EDITOR,
            arguments = listOf(navArgument("zoneId") { type = NavType.StringType; nullable = true; defaultValue = null })
        ) { entry ->
            SafeZoneEditorScreen(
                zoneId = entry.arguments?.getString("zoneId"),
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.JOURNEY_SETUP) {
            JourneySetupScreen(
                onStarted = { navController.navigate(Routes.JOURNEY_MONITOR) { popUpTo(Routes.JOURNEY_SETUP) { inclusive = true } } },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.JOURNEY_MONITOR) { JourneyMonitorScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.INCIDENT_HISTORY) { IncidentHistoryScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.EDIT_PROFILE) { EditProfileScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.EMERGENCY_INFO) {
            EmergencyInformationScreen(
                onBack = { navController.popBackStack() },
                onOpenCategory = { category -> navController.navigate(Routes.emergencyInfoForm(category.route)) }
            )
        }
        composable(
            route = Routes.EMERGENCY_INFO_FORM,
            arguments = listOf(navArgument("category") { type = NavType.StringType })
        ) {
            EmergencyInfoFormScreen(onBack = { navController.popBackStack() })
        }
    }
}

/**
 * Scaffold with the five-destination bottom navigation bar shared by the
 * primary screens — Map, Circles, a centred SOS tab (always shown in the
 * emergency colour, FR-17), Alerts and Safety.
 */
@Composable
private fun MainShell(
    navController: NavHostController,
    content: @Composable () -> Unit
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomItems.forEach { item ->
                    val selected = currentRoute == item.matchRoute
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (currentRoute != item.matchRoute) {
                                navController.navigate(item.navigateRoute) {
                                    popUpTo(Routes.HOME)
                                    launchSingleTop = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                if (selected) item.filledIcon else item.outlinedIcon,
                                contentDescription = stringResource(item.labelRes)
                            )
                        },
                        label = { Text(stringResource(item.labelRes)) },
                        colors = if (item.isEmergency) {
                            NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.error,
                                unselectedIconColor = MaterialTheme.colorScheme.error,
                                selectedTextColor = MaterialTheme.colorScheme.error,
                                unselectedTextColor = MaterialTheme.colorScheme.error,
                                indicatorColor = MaterialTheme.colorScheme.errorContainer
                            )
                        } else NavigationBarItemDefaults.colors()
                    )
                }
            }
        }
    ) { padding ->
        androidx.compose.foundation.layout.Box(Modifier.padding(padding)) { content() }
    }
}

private data class BottomItem(
    val matchRoute: String,
    val navigateRoute: String,
    val labelRes: Int,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector,
    val isEmergency: Boolean = false
)

private val bottomItems = listOf(
    BottomItem(Routes.HOME, Routes.HOME, R.string.nav_map, Icons.Filled.Map, Icons.Outlined.Map),
    BottomItem(Routes.CIRCLES, Routes.CIRCLES, R.string.nav_circles, Icons.Filled.Group, Icons.Outlined.Group),
    BottomItem(Routes.SOS, Routes.sos(), R.string.sos, Icons.Filled.WbSunny, Icons.Filled.WbSunny, isEmergency = true),
    BottomItem(Routes.ALERTS, Routes.ALERTS, R.string.nav_alerts, Icons.Filled.NotificationsActive, Icons.Outlined.Notifications),
    BottomItem(Routes.SAFETY, Routes.SAFETY, R.string.nav_safety, Icons.Filled.Shield, Icons.Outlined.Shield)
)

/** Maps a push-notification deep link to a starting destination. */
private fun deepLinkTarget(deepLink: String?): String = when {
    deepLink == null -> Routes.HOME
    deepLink.contains("sos") -> Routes.sos()
    deepLink.contains("zones") -> Routes.SAFETY
    deepLink.contains("journey") -> Routes.JOURNEY_MONITOR
    else -> Routes.HOME
}
