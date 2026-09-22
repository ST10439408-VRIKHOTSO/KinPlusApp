package za.co.kinplus.app.ui.screens.auth

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
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
import za.co.kinplus.app.R
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

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onRegister: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var keepSignedIn by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Color.White)) {
        Column(Modifier.fillMaxSize()) {
            AuthHero(
                headline = stringResource(R.string.welcome_back) + ".",
                subtitle = stringResource(R.string.auth_subtitle),
                modifier = Modifier.fillMaxWidth().height(300.dp)
            )

            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f).imePadding(),
                color = Color.White,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                shadowElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    AuthTabRow(active = AuthTabKind.SIGN_IN, onSignIn = {}, onSignUp = onRegister)
                    Spacer(Modifier.height(16.dp))

                    AuthTextField(
                        value = state.email,
                        onValueChange = viewModel::onEmail,
                        placeholder = stringResource(R.string.email),
                        leadingIcon = Icons.Outlined.Email,
                        keyboardType = KeyboardType.Email
                    )
                    Spacer(Modifier.height(12.dp))
                    AuthTextField(
                        value = state.password,
                        onValueChange = viewModel::onPassword,
                        placeholder = stringResource(R.string.password),
                        leadingIcon = Icons.Outlined.Lock,
                        isPassword = true
                    )
                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = keepSignedIn,
                                onCheckedChange = { keepSignedIn = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color.Black,
                                    checkmarkColor = Color.White,
                                    uncheckedColor = AuthPlaceholder
                                )
                            )
                            Text(
                                stringResource(R.string.keep_signed_in),
                                color = Color(0xFF424242),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            stringResource(R.string.forgot_password),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable(onClick = viewModel::forgotPassword)
                        )
                    }

                    // Localised feedback for validation / reset outcomes.
                    state.error?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(mapAuthMessage(it), color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    }
                    state.info?.let {
                        if (it == "RESET_SENT") {
                            Spacer(Modifier.height(8.dp))
                            Text(stringResource(R.string.reset_email_sent), color = MaterialTheme.colorScheme.tertiary, fontSize = 13.sp)
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { viewModel.login(context, onLoggedIn) },
                        enabled = !state.loading,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (state.loading) {
                            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Text(stringResource(R.string.log_in), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Text(stringResource(R.string.new_to_kinplus), color = Color(0xFF757575), fontSize = 13.sp)
                        Text(
                            stringResource(R.string.create_account),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable(onClick = onRegister)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

/** Maps auth error codes to localised, user-friendly strings. */
@Composable
fun mapAuthMessage(code: String): String = when (code) {
    "INVALID_EMAIL" -> stringResource(R.string.error_invalid_email)
    "WEAK_PASSWORD" -> stringResource(R.string.error_weak_password)
    "PASSWORD_MISMATCH" -> stringResource(R.string.error_password_mismatch)
    "NAME_REQUIRED" -> stringResource(R.string.error_name_required)
    "INVALID_PHONE" -> stringResource(R.string.error_invalid_phone)
    "CONSENT_REQUIRED" -> stringResource(R.string.error_consent_required)
    "AUTH_FAILED" -> stringResource(R.string.error_auth_failed)
    else -> code // Firebase already returns a readable message for most failures.
}
