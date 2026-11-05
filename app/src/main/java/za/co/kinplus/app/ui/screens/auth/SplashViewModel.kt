package za.co.kinplus.app.ui.screens.auth

import android.util.Log
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
    fun isSignedIn(): Boolean = authManager.isSignedIn.also { signedIn ->
        // Firebase keeps the session on the device, so a returning user skips the login screen.
        Log.i(TAG, if (signedIn) "Saved Firebase session found" else "No saved session; showing Login")
    }

    private companion object {
        const val TAG = "SplashViewModel"
    }
}
