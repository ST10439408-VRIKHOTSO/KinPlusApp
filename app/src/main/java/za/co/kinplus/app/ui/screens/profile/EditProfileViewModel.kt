package za.co.kinplus.app.ui.screens.profile

import android.net.Uri
import android.util.Log
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
import za.co.kinplus.app.auth.AuthManager
import za.co.kinplus.app.data.local.SettingsStore
import za.co.kinplus.app.data.repository.LocalPhotoRepository
import za.co.kinplus.app.data.repository.UserRepository
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

    /**
     * Saves the name on Firebase and the name and phone number on the Kin+
     * profile (MongoDB, through the API). [onDone] runs only when the profile
     * was saved; otherwise [onError] gets an error code for the screen.
     */
    fun save(onDone: () -> Unit, onError: (String) -> Unit) = viewModelScope.launch {
        val e = edits.value
        val displayName = "${e.firstName.trim()} ${e.lastName.trim()}"
        authManager.updateDisplayName(displayName)
        when (userRepository.updateProfile(displayName = displayName, phone = e.phone.trim())) {
            is Resource.Success -> {
                Log.i(TAG, "Profile saved")
                onDone()
            }
            else -> {
                Log.w(TAG, "Profile could not be saved")
                onError("PROFILE_SAVE")
            }
        }
    }

    private companion object {
        const val TAG = "EditProfileViewModel"
    }
}
