package za.co.kinplus.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import za.co.kinplus.app.auth.AuthManager
import za.co.kinplus.app.data.repository.EmergencyInfoRepository
import za.co.kinplus.app.data.repository.LocalPhotoRepository
import za.co.kinplus.app.data.repository.UserRepository
import za.co.kinplus.app.util.Resource
import javax.inject.Inject

data class ProfileUiState(
    val displayName: String = "",
    val email: String = "",
    val phone: String? = null,
    val photoPath: String? = null,
    val emergencyInfoPercent: Int = 0
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authManager: AuthManager,
    emergencyInfoRepository: EmergencyInfoRepository,
    localPhotoRepository: LocalPhotoRepository
) : ViewModel() {

    // The saved phone number lives on the Kin+ profile (set at registration or
    // a previous edit), not on the Firebase Auth user — reading it from Auth
    // here showed a blank number even after one had been saved.
    private val phoneFlow = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    val state: StateFlow<ProfileUiState> = combine(
        emergencyInfoRepository.observe(),
        localPhotoRepository.userPhotoPath(),
        phoneFlow
    ) { info, photoPath, phone ->
        ProfileUiState(
            displayName = authManager.currentDisplayName,
            email = authManager.currentEmail,
            phone = phone,
            photoPath = photoPath,
            emergencyInfoPercent = info.overallPercentComplete
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileUiState())

    init {
        viewModelScope.launch {
            val profile = userRepository.getProfile()
            phoneFlow.value = (profile as? Resource.Success)?.data?.phone
        }
    }

    fun signOut() = authManager.signOut()

    fun deleteAccount(onDone: () -> Unit) = viewModelScope.launch {
        userRepository.deleteAccount()
        authManager.signOut()
        onDone()
    }
}
