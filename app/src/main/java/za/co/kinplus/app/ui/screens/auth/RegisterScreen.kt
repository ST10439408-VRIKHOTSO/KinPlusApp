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
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
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

@Composable
fun RegisterScreen(
    onRegistered: () -> Unit,
    onBackToLogin: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showPrivacyPolicy by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Color.White)) {
        Column(Modifier.fillMaxSize()) {
            AuthHero(
                headline = stringResource(R.string.register_headline),
                subtitle = stringResource(R.string.register_subtitle),
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
                    AuthTabRow(active = AuthTabKind.SIGN_UP, onSignIn = onBackToLogin, onSignUp = {})
                    Spacer(Modifier.height(16.dp))

                    AuthTextField(
                        value = state.firstName,
                        onValueChange = viewModel::onFirstName,
                        placeholder = stringResource(R.string.first_name),
                        leadingIcon = Icons.Outlined.Person
                    )
                    Spacer(Modifier.height(12.dp))
                    AuthTextField(
                        value = state.lastName,
                        onValueChange = viewModel::onLastName,
                        placeholder = stringResource(R.string.last_name),
                        leadingIcon = Icons.Outlined.Person
                    )
                    Spacer(Modifier.height(12.dp))
                    AuthTextField(
                        value = state.phone,
                        onValueChange = viewModel::onPhone,
                        placeholder = stringResource(R.string.phone_number),
                        leadingIcon = Icons.Outlined.Phone,
                        keyboardType = KeyboardType.Phone
                    )
                    Spacer(Modifier.height(12.dp))
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
                        isPassword = true,
                        supportingText = stringResource(R.string.password_hint)
                    )
                    Spacer(Modifier.height(12.dp))
                    AuthTextField(
                        value = state.confirm,
                        onValueChange = viewModel::onConfirm,
                        placeholder = stringResource(R.string.confirm_password),
                        leadingIcon = Icons.Outlined.Lock,
                        isPassword = true
                    )
                    Spacer(Modifier.height(14.dp))

                    Text(
                        stringResource(R.string.view_privacy_policy),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { showPrivacyPolicy = true }
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = state.consent,
                            onCheckedChange = viewModel::onConsent,
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color.Black,
                                checkmarkColor = Color.White,
                                uncheckedColor = AuthPlaceholder
                            )
                        )
                        Text(
                            stringResource(R.string.privacy_consent),
                            color = Color(0xFF424242),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    state.error?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(mapAuthMessage(it), color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    }

                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { viewModel.register(context, onRegistered) },
                        enabled = !state.loading,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (state.loading) {
                            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Text(stringResource(R.string.create_account), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Text(stringResource(R.string.already_have_account_prompt), color = Color(0xFF757575), fontSize = 13.sp)
                        Text(
                            stringResource(R.string.log_in),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable(onClick = onBackToLogin)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }

    if (showPrivacyPolicy) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicy = false },
            title = { Text(stringResource(R.string.privacy_policy_title)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(stringResource(R.string.privacy_policy_body), fontSize = 13.sp, lineHeight = 19.sp)
                }
            },
            confirmButton = { TextButton(onClick = { showPrivacyPolicy = false }) { Text(stringResource(R.string.confirm)) } }
        )
    }
}
