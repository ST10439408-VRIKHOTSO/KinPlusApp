package za.co.kinplus.app

import android.Manifest
import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
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
    ) { /* results handled by each feature at point of use */ }

    override fun attachBaseContext(newBase: Context) {
        // Wrap the base context with the user's chosen locale before inflation.
        super.attachBaseContext(LocaleManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHost = supportFragmentManager.findFragmentById(R.id.navHost) as NavHostFragment
        navController = navHost.navController

        setupBottomNavigation()

        if (savedInstanceState == null) requestSafetyPermissions()
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
            bottomNav.isVisible = isTab
            if (isTab) {
                bottomNav.menu.findItem(destination.id)?.isChecked = true
            }
            tintSosTab()
        }
    }

    /** The centred SOS tab is always shown in the emergency colour (FR-17). */
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
}
