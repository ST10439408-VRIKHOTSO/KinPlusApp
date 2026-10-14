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

data class RegisterUiState(
    val firstName: String = "",
    val lastName: String = "",
    val phone: String = "",
    val email: String = "",
    val password: String = "",
    val confirm: String = "",
    val consent: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val userRepository: UserRepository,
    private val messaging: FirebaseMessaging
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    fun onFirstName(v: String) = _state.update { it.copy(firstName = v, error = null) }
    fun onLastName(v: String) = _state.update { it.copy(lastName = v, error = null) }
    fun onPhone(v: String) = _state.update { it.copy(phone = v, error = null) }
    fun onEmail(v: String) = _state.update { it.copy(email = v, error = null) }
    fun onPassword(v: String) = _state.update { it.copy(password = v, error = null) }
    fun onConfirm(v: String) = _state.update { it.copy(confirm = v, error = null) }
    fun onConsent(v: Boolean) = _state.update { it.copy(consent = v, error = null) }

    fun register(context: Context, onSuccess: () -> Unit) {
        val s = _state.value
        val displayName = "${s.firstName.trim()} ${s.lastName.trim()}".trim()
        // Client-side validation (FR-01) before hitting the network.
        val validationError = when {
            s.firstName.isBlank() || s.lastName.isBlank() -> "NAME_REQUIRED"
            !AuthManager.isValidPhone(s.phone) -> "INVALID_PHONE"
            !AuthManager.isValidEmail(s.email) -> "INVALID_EMAIL"
            !AuthManager.isStrongPassword(s.password) -> "WEAK_PASSWORD"
            s.password != s.confirm -> "PASSWORD_MISMATCH"
            !s.consent -> "CONSENT_REQUIRED"
            else -> null
        }
        if (validationError != null) {
            _state.update { it.copy(error = validationError) }
            return
        }

        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val result = authManager.register(displayName, s.email, s.password)) {
                is Resource.Success -> {
                    val token = runCatching { messaging.token.await() }.getOrNull()
                    userRepository.syncProfile(
                        displayName = displayName,
                        phone = s.phone.trim(),
                        fcmToken = token,
                        locale = LocaleManager.getLanguageTag(context)
                    )
                    _state.update { it.copy(loading = false) }
                    onSuccess()
                }
                is Resource.Error -> _state.update { it.copy(loading = false, error = result.message) }
                Resource.Loading -> Unit
            }
        }
    }
}
