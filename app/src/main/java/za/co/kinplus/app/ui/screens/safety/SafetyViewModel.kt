package za.co.kinplus.app.ui.screens.safety

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import za.co.kinplus.app.data.local.EmergencyContactEntity
import za.co.kinplus.app.data.repository.EmergencyContactRepository
import za.co.kinplus.app.data.repository.EmergencyInfoRepository
import za.co.kinplus.app.data.repository.SosRepository
import za.co.kinplus.app.data.repository.UserRepository
import za.co.kinplus.app.domain.EmergencyInfo
import za.co.kinplus.app.location.LocationProvider
import za.co.kinplus.app.util.Resource
import javax.inject.Inject

data class SafetyUiState(
    val setupPercent: Int = 0,
    val setupFieldsFilled: Int = 0,
    val setupFieldsTotal: Int = TOTAL_SETUP_FIELDS,
    val incidentCount: Int = 0,
    val contacts: List<EmergencyContactEntity> = emptyList(),
    val emergencyInfo: EmergencyInfo = EmergencyInfo(),
    val locationPaused: Boolean = false
) {
    companion object {
        const val TOTAL_EMERGENCY_INFO_FIELDS = 18
        // Emergency Info's own fields (About Me, Medical, Vehicle, Addresses,
        // Home Security) plus one more for "has at least one emergency
        // contact" — Safety Setup isn't complete until every one of these is
        // in place, not just the Emergency Info fields on their own.
        const val TOTAL_SETUP_FIELDS = TOTAL_EMERGENCY_INFO_FIELDS + 1
    }
}

@HiltViewModel
class SafetyViewModel @Inject constructor(
    private val emergencyInfoRepository: EmergencyInfoRepository,
    private val emergencyContactRepository: EmergencyContactRepository,
    private val userRepository: UserRepository,
    sosRepository: SosRepository,
    private val locationProvider: LocationProvider
) : ViewModel() {

    private val pauseState = kotlinx.coroutines.flow.MutableStateFlow(false)

    val state: StateFlow<SafetyUiState> = combine(
        emergencyInfoRepository.observe(),
        sosRepository.history,
        emergencyContactRepository.observe(),
        pauseState
    ) { info, history, contacts, paused ->
        // Safety Setup spans Emergency Info (About Me, Medical, Vehicle,
        // Addresses, Home Security) *and* having a primary + backup emergency
        // contact — filling in just one half shouldn't read as "done", and
        // this threshold matches the "add a backup contact" nudge below.
        val infoTotal = SafetyUiState.TOTAL_EMERGENCY_INFO_FIELDS
        val infoFilled = (info.overallPercentComplete * infoTotal) / 100
        val total = SafetyUiState.TOTAL_SETUP_FIELDS
        val filled = infoFilled + if (contacts.size >= 2) 1 else 0
        SafetyUiState(
            setupPercent = (filled * 100) / total,
            setupFieldsFilled = filled,
            setupFieldsTotal = total,
            incidentCount = history.size,
            contacts = contacts,
            emergencyInfo = info,
            locationPaused = paused
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SafetyUiState())

    init {
        viewModelScope.launch {
            val profile = userRepository.getProfile()
            pauseState.value = (profile as? Resource.Success)?.data?.sharingEnabled?.not() ?: false
        }
    }

    /** Pausing instantly hides the user's location everywhere (mirrors Profile > Location Settings). */
    fun togglePauseLocation(paused: Boolean) {
        pauseState.value = paused
        viewModelScope.launch { userRepository.updateProfile(sharingEnabled = !paused) }
    }

    /** Builds a "here's where I am" share text from a fresh location fix, or null without permission/fix. */
    fun shareLocation(context: Context) = viewModelScope.launch {
        val fix = locationProvider.currentFix() ?: return@launch
        val text = "I'm sharing my location with you via Kin+: https://maps.google.com/?q=${fix.lat},${fix.lng}"
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) },
                null
            )
        )
    }

    fun addContact(name: String, relationship: String, phone: String) = viewModelScope.launch {
        emergencyContactRepository.add(name, relationship, phone)
    }

    fun deleteContact(id: String) = viewModelScope.launch {
        emergencyContactRepository.delete(id)
    }
}
