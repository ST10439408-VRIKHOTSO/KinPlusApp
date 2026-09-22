package za.co.kinplus.app.ui.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import za.co.kinplus.app.R
import za.co.kinplus.app.auth.AuthManager
import za.co.kinplus.app.data.local.SettingsStore
import za.co.kinplus.app.data.repository.LocalPhotoRepository
import za.co.kinplus.app.data.repository.UserRepository
import za.co.kinplus.app.ui.components.EditableAvatar
import za.co.kinplus.app.ui.components.KinTopAppBar
import za.co.kinplus.app.ui.screens.auth.mapAuthMessage
import za.co.kinplus.app.util.Resource
import javax.inject.Inject

data class EditProfileUiState(
    val firstName: String = "",
    val lastName: String = "",
    val phone: String = "",
    val email: String = "",
    val photoPath: String? = null,
    val mailingListConsent: Boolean = false
)

private data class NameEdits(val firstName: String, val lastName: String, val phone: String)

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val userRepository: UserRepository,
    private val settingsStore: SettingsStore,
    private val localPhotoRepository: LocalPhotoRepository
) : ViewModel() {

    private val nameParts = authManager.currentDisplayName.trim().split(" ", limit = 2)
    private val edits = MutableStateFlow(
        NameEdits(
            firstName = nameParts.getOrElse(0) { "" },
            lastName = nameParts.getOrElse(1) { "" },
            phone = ""
        )
    )

    val state: StateFlow<EditProfileUiState> = combine(
        edits, settingsStore.mailingListConsent, localPhotoRepository.userPhotoPath()
    ) { e, consent, photoPath ->
        EditProfileUiState(
            firstName = e.firstName, lastName = e.lastName, phone = e.phone,
            email = authManager.currentEmail, photoPath = photoPath, mailingListConsent = consent
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EditProfileUiState())

    init {
        // The saved phone number lives on the Kin+ profile (set at registration
        // or a previous edit) — not on the Firebase Auth user, which only ever
        // carries a phone number for phone-number sign-in. Loading from Auth
        // here was why a previously entered number always came back blank.
        viewModelScope.launch {
            val profile = userRepository.getProfile()
            if (profile is Resource.Success) {
                edits.update { it.copy(phone = profile.data.phone ?: it.phone) }
            }
        }
    }

    fun onFirstName(v: String) { edits.value = edits.value.copy(firstName = v) }
    fun onLastName(v: String) { edits.value = edits.value.copy(lastName = v) }
    fun onPhone(v: String) { edits.value = edits.value.copy(phone = v) }
    fun onMailingListConsent(v: Boolean) = viewModelScope.launch { settingsStore.setMailingListConsent(v) }

    fun pickPhoto(uri: Uri) = viewModelScope.launch { localPhotoRepository.setUserPhoto(uri) }

    fun save(onDone: () -> Unit) = viewModelScope.launch {
        val e = edits.value
        val displayName = "${e.firstName.trim()} ${e.lastName.trim()}"
        authManager.updateDisplayName(displayName)
        userRepository.updateProfile(displayName = displayName, phone = e.phone.trim())
        onDone()
    }
}

@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var validationError by remember { mutableStateOf<String?>(null) }

    val pickPhotoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { viewModel.pickPhoto(it) } }

    fun trySave() {
        validationError = when {
            state.firstName.isBlank() || state.lastName.isBlank() -> "NAME_REQUIRED"
            !AuthManager.isValidPhone(state.phone) -> "INVALID_PHONE"
            else -> null
        }
        if (validationError == null) viewModel.save(onBack)
    }

    Column(Modifier.fillMaxSize()) {
        KinTopAppBar(
            title = stringResource(R.string.edit_profile_title),
            onBack = onBack,
            actions = { TextButton(onClick = { trySave() }) { Text(stringResource(R.string.save)) } }
        )

        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            EditableAvatar(
                photoPath = state.photoPath,
                fallbackText = state.firstName,
                onPick = { pickPhotoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                size = 96.dp
            )
            Spacer(Modifier.height(28.dp))

            OutlinedTextField(
                value = state.firstName,
                onValueChange = viewModel::onFirstName,
                label = { Text(stringResource(R.string.first_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = state.lastName,
                onValueChange = viewModel::onLastName,
                label = { Text(stringResource(R.string.last_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = state.phone,
                onValueChange = viewModel::onPhone,
                label = { Text(stringResource(R.string.phone_number)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = state.email,
                onValueChange = {},
                label = { Text(stringResource(R.string.email)) },
                singleLine = true,
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = state.mailingListConsent, onCheckedChange = viewModel::onMailingListConsent)
                Text(stringResource(R.string.mailing_list_consent), style = MaterialTheme.typography.bodyMedium)
            }

            validationError?.let {
                Spacer(Modifier.height(8.dp))
                Text(mapAuthMessage(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(28.dp))
            Button(
                onClick = { trySave() },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}
