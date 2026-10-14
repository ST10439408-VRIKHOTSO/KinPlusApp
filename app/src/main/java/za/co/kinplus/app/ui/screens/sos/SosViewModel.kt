package za.co.kinplus.app.ui.screens.sos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import za.co.kinplus.app.data.local.EmergencyContactEntity
import za.co.kinplus.app.data.repository.CircleRepository
import za.co.kinplus.app.data.repository.EmergencyContactRepository
import za.co.kinplus.app.data.repository.SosRepository
import za.co.kinplus.app.domain.Circle
import za.co.kinplus.app.location.LocationFix
import za.co.kinplus.app.location.LocationProvider
import za.co.kinplus.app.util.Resource
import javax.inject.Inject

data class SosUiState(
    val circles: List<Circle> = emptyList(),
    val selectedCircleIds: Set<String> = emptySet(),
    val message: String = "",
    val responders: List<EmergencyContactEntity> = emptyList(),
    val fix: LocationFix? = null,
    val sending: Boolean = false,
    val sentQueued: Boolean = false,
    val sent: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SosViewModel @Inject constructor(
    private val sosRepository: SosRepository,
    private val circleRepository: CircleRepository,
    private val emergencyContactRepository: EmergencyContactRepository,
    private val locationProvider: LocationProvider
) : ViewModel() {

    private val _state = MutableStateFlow(SosUiState())
    val state: StateFlow<SosUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            circleRepository.circles.collect { circles ->
                _state.update { it.copy(circles = circles, selectedCircleIds = circles.map { c -> c.id }.toSet()) }
            }
        }
        viewModelScope.launch {
            emergencyContactRepository.observe().collect { contacts -> _state.update { it.copy(responders = contacts) } }
        }
        viewModelScope.launch {
            val fix = locationProvider.currentFix()
            _state.update { it.copy(fix = fix) }
        }
    }

    fun onMessage(v: String) = _state.update { it.copy(message = v) }

    /** Raises the SOS. Works offline: the alert is queued and sent on reconnect. */
    fun raiseSos() {
        val s = _state.value
        _state.update { it.copy(sending = true, error = null) }
        viewModelScope.launch {
            val fix = s.fix ?: locationProvider.currentFix()
            when (val res = sosRepository.raiseSos(
                lat = fix?.lat, lng = fix?.lng,
                message = s.message.ifBlank { null },
                circleIds = s.selectedCircleIds.toList()
            )) {
                is Resource.Success -> _state.update {
                    it.copy(sending = false, sent = res.data != null, sentQueued = res.data == null)
                }
                is Resource.Error -> _state.update { it.copy(sending = false, error = res.message) }
                Resource.Loading -> Unit
            }
        }
    }

    companion object { const val HOLD_SECONDS = 3 }
}
