package za.co.kinplus.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import dagger.hilt.android.AndroidEntryPoint
import za.co.kinplus.app.databinding.ActivityMainBinding
import za.co.kinplus.app.util.LocaleManager

/**
 * Single activity host. Every screen is a Fragment with an XML layout; the
 * Navigation component (res/navigation/nav_graph.xml) moves between them, and
 * the bottom navigation bar is shown on the five primary tabs only.
 *
 * attachBaseContext is overridden so the saved in-app language (FR-31) is
 * applied to the entire activity's resources.
 *
 * The app targets Android 16 (API 36), where every screen is drawn edge to
 * edge. [applySystemBarInsets] pads the content so nothing sits under the
 * status bar, the navigation bar or the keyboard.
 *
 * References:
 * Android Developers (2026) Display content edge-to-edge in views. Available at:
 * https://developer.android.com/develop/ui/views/layout/edge-to-edge [Accessed 5 November 2026].
 * Android Developers (2026) Navigation. Available at:
 * https://developer.android.com/guide/navigation [Accessed 5 November 2026].
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    /** Destinations that show the bottom navigation bar. */
    private val tabDestinations = setOf(
        R.id.homeFragment,
        R.id.circlesFragment,
        R.id.sosFragment,
        R.id.alertsFragment,
        R.id.safetyFragment
    )

    // Location and notification permissions the safety features depend on
    // (FR-13, FR-30). Declined permissions degrade gracefully at point of use.
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        // Each feature checks its permission again at point of use; this only records the answer.
        results.forEach { (permission, granted) -> Log.i(TAG, "Permission $permission granted=$granted") }
    }

    override fun attachBaseContext(newBase: Context) {
        // Wrap the base context with the user's chosen locale before inflation.
        super.attachBaseContext(LocaleManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Edge to edge on every Android version, so the layout looks the same on all
        // phones. The app is light only, so both bars use dark icons. Android 8.0
        // cannot draw dark navigation icons, so it gets a dark scrim instead.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, NAV_BAR_DARK_SCRIM)
        )
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHost = supportFragmentManager.findFragmentById(R.id.navHost) as NavHostFragment
        navController = navHost.navController

        setupBottomNavigation()
        applySystemBarInsets()

        Log.i(TAG, "Started in language '${LocaleManager.getLanguageTag(this)}', deep link: ${intent?.data}")
        if (savedInstanceState == null) requestSafetyPermissions()
    }

    /**
     * Keeps the content clear of the status bar, any display cutout, the
     * navigation bar and the on-screen keyboard. The bottom bar takes the
     * navigation bar padding itself, so its white background runs to the edge
     * of the screen. Insets are consumed here so no child view pads twice.
     */
    private fun applySystemBarInsets() {
        val bottomNav = binding.bottomNav
        val navBasePadding = bottomNav.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { root, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val navShown = bottomNav.isVisible
            val rootBottom = when {
                keyboard > 0 -> keyboard   // lift the screen above the keyboard
                navShown -> 0              // the bottom bar covers the navigation bar
                else -> bars.bottom
            }
            root.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = rootBottom)
            bottomNav.updatePadding(bottom = navBasePadding + if (navShown && keyboard == 0) bars.bottom else 0)
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = binding.bottomNav

        // Tapping a tab returns to Home's back stack entry first, so the back
        // button from any tab always lands on the map rather than piling up tabs.
        bottomNav.setOnItemSelectedListener { item ->
            if (navController.currentDestination?.id != item.itemId) {
                val options = NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setPopUpTo(R.id.homeFragment, false)
                    .build()
                navController.navigate(item.itemId, null, options)
            }
            true
        }
        bottomNav.setOnItemReselectedListener { /* already on this tab */ }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isTab = destination.id in tabDestinations
            if (bottomNav.isVisible != isTab) {
                bottomNav.isVisible = isTab
                // The bottom padding depends on whether the bar is shown, so work it out again.
                ViewCompat.requestApplyInsets(binding.root)
            }
            if (isTab) {
                bottomNav.menu.findItem(destination.id)?.isChecked = true
            }
            tintSosTab()
            Log.d(TAG, "Showing ${destination.label ?: destination.id}")
        }
    }

    /**
     * The centred SOS tab is always shown in the emergency colour (FR-17).
     * Material Components has no public way to colour a single tab, so the
     * tab's item view is tinted directly. That view is marked as internal to
     * the library, which lint reports as RestrictedApi; the call is checked
     * with `is` first, so a future Material version that changes it only
     * loses the red colour and never crashes.
     */
    @SuppressLint("RestrictedApi")
    private fun tintSosTab() {
        val red = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.kin_red))
        val sosItemView = binding.bottomNav.findViewById<View>(R.id.sosFragment)
        if (sosItemView is com.google.android.material.navigation.NavigationBarItemView) {
            sosItemView.setIconTintList(red)
            sosItemView.setTextColor(red)
        }
    }

    private fun requestSafetyPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    override fun onSupportNavigateUp(): Boolean = navController.navigateUp() || super.onSupportNavigateUp()

    private companion object {
        const val TAG = "MainActivity"

        /** Behind the light navigation icons on Android 8.0, which cannot draw dark ones. */
        val NAV_BAR_DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
