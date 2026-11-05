package za.co.kinplus.app.ui.screens.auth

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import za.co.kinplus.app.auth.AuthManager
import za.co.kinplus.app.data.repository.UserRepository
import za.co.kinplus.app.messaging.PushRegistration
import za.co.kinplus.app.util.LocaleManager
import za.co.kinplus.app.util.Resource
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val info: String? = null
)

/**
 * Sign in state for the Login screen (FR-02, FR-05). Empty or badly formed
 * input is caught here before any network call, and every error is kept as a
 * code that [mapAuthMessage] turns into text in the user's language.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val userRepository: UserRepository,
    private val pushRegistration: PushRegistration
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmail(v: String) = _state.update { it.copy(email = v, error = null) }
    fun onPassword(v: String) = _state.update { it.copy(password = v, error = null) }

    fun login(context: Context, onSuccess: () -> Unit) {
        val s = _state.value
        val validationError = validateLogin(s.email, s.password)
        if (validationError != null) {
            Log.d(TAG, "Sign in blocked by validation: $validationError")
            _state.update { it.copy(error = validationError) }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val result = authManager.login(s.email, s.password)) {
                is Resource.Success -> { syncThen(context, onSuccess) }
                is Resource.Error -> _state.update { it.copy(loading = false, error = result.message) }
                Resource.Loading -> Unit
            }
        }
    }

    /** Single sign on with Google (FR-05). Cancelling the picker is not shown as an error. */
    fun googleSignIn(activityContext: Context, onSuccess: () -> Unit) {
        _state.update { it.copy(loading = true, error = null, info = null) }
        viewModelScope.launch {
            when (val result = authManager.signInWithGoogle(activityContext)) {
                is Resource.Success -> syncThen(activityContext.applicationContext, onSuccess)
                is Resource.Error -> _state.update {
                    it.copy(loading = false, error = result.message.takeUnless { m -> m == AuthManager.GOOGLE_CANCELLED })
                }
                Resource.Loading -> Unit
            }
        }
    }

    fun forgotPassword() {
        val email = _state.value.email
        if (!AuthManager.isValidEmail(email)) {
            _state.update { it.copy(error = "INVALID_EMAIL") }
            return
        }
        viewModelScope.launch {
            when (val result = authManager.sendPasswordReset(email)) {
                is Resource.Success -> {
                    Log.i(TAG, "Password reset email requested")
                    _state.update { it.copy(info = "RESET_SENT", error = null) }
                }
                is Resource.Error -> _state.update { it.copy(error = result.message) }
                Resource.Loading -> Unit
            }
        }
    }

    /** After sign-in, register the profile + FCM token with the API, then continue. */
    private suspend fun syncThen(context: Context, onSuccess: () -> Unit) {
        // No token is sent while the user has switched notifications off in Settings.
        val token = pushRegistration.tokenIfEnabled()
        Log.i(TAG, "Signed in; syncing profile (push token ${if (token != null) "included" else "not included"})")
        userRepository.syncProfile(
            displayName = authManager.currentDisplayName,
            phone = null,
            fcmToken = token,
            locale = LocaleManager.getLanguageTag(context)
        )
        _state.update { it.copy(loading = false) }
        onSuccess()
    }

    companion object {
        private const val TAG = "LoginViewModel"

        /**
         * Checks the sign in form before it reaches Firebase, which would otherwise
         * reject an empty field with an English only message. Returns an error
         * code, or null when the form can be sent.
         */
        fun validateLogin(email: String, password: String): String? = when {
            email.isBlank() -> "EMAIL_REQUIRED"
            !AuthManager.isValidEmail(email) -> "INVALID_EMAIL"
            password.isEmpty() -> "PASSWORD_REQUIRED"
            else -> null
        }
    }
}
