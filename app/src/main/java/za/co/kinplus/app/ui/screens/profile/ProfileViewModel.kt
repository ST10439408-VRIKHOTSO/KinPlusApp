package za.co.kinplus.app.ui.screens.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import za.co.kinplus.app.auth.AuthManager
import za.co.kinplus.app.data.local.LocalDataCleaner
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
    private val localDataCleaner: LocalDataCleaner,
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

    /** Clears this account's cached data from the phone, then signs out of Firebase. */
    fun signOut(onDone: () -> Unit) = viewModelScope.launch {
        localDataCleaner.clearSession()
        authManager.signOut()
        onDone()
    }

    /**
     * Deletes the account (FR-08): the API removes the profile, location history
     * and circle memberships from MongoDB, then the Firebase sign in and every
     * bit of Kin+ data on the phone are deleted. If the API cannot be reached
     * nothing is removed and [onError] runs, so the user is never signed out
     * with their data still on the server.
     */
    fun deleteAccount(onDone: () -> Unit, onError: () -> Unit) = viewModelScope.launch {
        if (userRepository.deleteAccount() is Resource.Error) {
            Log.w(TAG, "Account deletion failed at the API; nothing was removed")
            onError()
            return@launch
        }
        val signInRemoved = authManager.deleteFirebaseUser()
        localDataCleaner.clearEverything()
        Log.i(TAG, "Account data deleted; Firebase sign in removed: $signInRemoved")
        authManager.signOut()
        onDone()
    }

    private companion object {
        const val TAG = "ProfileViewModel"
    }
}
