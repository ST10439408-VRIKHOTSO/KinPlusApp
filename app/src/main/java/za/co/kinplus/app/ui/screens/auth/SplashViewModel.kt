package za.co.kinplus.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import za.co.kinplus.app.auth.AuthManager
import javax.inject.Inject

/**
 * Splash / Authentication Gate (screen 1). Checks the current Firebase session
 * (FR-06) and routes to Home when signed in, or Login otherwise.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {
    fun isSignedIn(): Boolean = authManager.isSignedIn
}
