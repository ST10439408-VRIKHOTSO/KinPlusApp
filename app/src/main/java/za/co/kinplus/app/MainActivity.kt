package za.co.kinplus.app

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import za.co.kinplus.app.ui.navigation.KinPlusNavHost
import za.co.kinplus.app.ui.theme.KinPlusTheme
import za.co.kinplus.app.util.LocaleManager

/**
 * Single-activity host. The whole UI is Jetpack Compose; navigation between the
 * fifteen screens is handled by [KinPlusNavHost].
 *
 * attachBaseContext is overridden so the saved in-app language (FR-31) is
 * applied to the entire activity's resources.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        // Wrap the base context with the user's chosen locale before inflation.
        super.attachBaseContext(LocaleManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // A deep link may arrive from a tapped push notification (kinplus://...).
        val deepLink = intent?.data?.toString()

        setContent {
            KinPlusTheme {
                // Request the location and notification permissions the safety
                // features depend on (FR-13, FR-30). Declined permissions
                // degrade gracefully rather than crashing.
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { /* results handled by each feature at point of use */ }

                LaunchedEffect(Unit) {
                    val permissions = mutableListOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    permissionLauncher.launch(permissions.toTypedArray())
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    KinPlusNavHost(
                        navController = navController,
                        initialDeepLink = deepLink
                    )
                }
            }
        }
    }
}
