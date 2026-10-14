package za.co.kinplus.app.ui.screens.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import za.co.kinplus.app.R
import za.co.kinplus.app.databinding.FragmentSplashBinding
import za.co.kinplus.app.ui.common.navigateSafely

/**
 * Splash / Authentication Gate (screen 1). Shows the brand splash briefly,
 * checks the current Firebase session (FR-06) and routes to Home when signed
 * in, or Login otherwise. A push notification deep link (kinplus://...) opens
 * the matching screen instead of Home.
 */
@AndroidEntryPoint
class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val viewModel: SplashViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return _binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            // Brief pause so the brand splash is visible, then route by session state.
            delay(900)
            val leaveSplash = NavOptions.Builder().setPopUpTo(R.id.splashFragment, true).build()
            if (viewModel.isSignedIn()) {
                val deepLink = activity?.intent?.data?.toString()
                when {
                    deepLink == null -> navigateSafely(R.id.homeFragment, null, leaveSplash)
                    deepLink.contains("sos") -> navigateSafely(R.id.sosFragment, null, leaveSplash)
                    deepLink.contains("zones") -> navigateSafely(R.id.safetyFragment, null, leaveSplash)
                    deepLink.contains("journey") -> {
                        navigateSafely(R.id.homeFragment, null, leaveSplash)
                        navigateSafely(R.id.journeyMonitorFragment)
                    }
                    else -> navigateSafely(R.id.homeFragment, null, leaveSplash)
                }
            } else {
                navigateSafely(R.id.loginFragment, null, leaveSplash)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
