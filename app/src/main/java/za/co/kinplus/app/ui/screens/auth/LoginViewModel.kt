package za.co.kinplus.app.ui.screens.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import za.co.kinplus.app.auth.AuthManager
import za.co.kinplus.app.data.repository.UserRepository
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

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val userRepository: UserRepository,
    private val messaging: FirebaseMessaging
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmail(v: String) = _state.update { it.copy(email = v, error = null) }
    fun onPassword(v: String) = _state.update { it.copy(password = v, error = null) }

    fun login(context: Context, onSuccess: () -> Unit) {
        val s = _state.value
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val result = authManager.login(s.email, s.password)) {
                is Resource.Success -> { syncThen(context, onSuccess) }
                is Resource.Error -> _state.update { it.copy(loading = false, error = result.message) }
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
            when (authManager.sendPasswordReset(email)) {
                is Resource.Success -> _state.update { it.copy(info = "RESET_SENT") }
                else -> _state.update { it.copy(error = "AUTH_FAILED") }
            }
        }
    }

    /** After sign-in, register the profile + FCM token with the API, then continue. */
    private suspend fun syncThen(context: Context, onSuccess: () -> Unit) {
        val token = runCatching { messaging.token.await() }.getOrNull()
        userRepository.syncProfile(
            displayName = authManager.currentDisplayName,
            phone = null,
            fcmToken = token,
            locale = LocaleManager.getLanguageTag(context)
        )
        _state.update { it.copy(loading = false) }
        onSuccess()
    }
}
