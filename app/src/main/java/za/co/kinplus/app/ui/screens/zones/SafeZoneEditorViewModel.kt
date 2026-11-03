package za.co.kinplus.app.ui.screens.zones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import za.co.kinplus.app.data.repository.CircleRepository
import za.co.kinplus.app.data.repository.ZoneRepository
import za.co.kinplus.app.domain.Circle
import za.co.kinplus.app.location.GeofenceManager
import za.co.kinplus.app.location.LocationProvider
import za.co.kinplus.app.util.Resource
import javax.inject.Inject

data class ZoneEditorUiState(
    val name: String = "",
    val radiusM: Int = 200,
    val notifyEnter: Boolean = true,
    val notifyExit: Boolean = true,
    val lat: Double? = null,
    val lng: Double? = null,
    val circles: List<Circle> = emptyList(),
    val selectedCircleId: String? = null,
    val saving: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SafeZoneEditorViewModel @Inject constructor(
    private val zoneRepository: ZoneRepository,
    private val circleRepository: CircleRepository,
    private val locationProvider: LocationProvider,
    private val geofenceManager: GeofenceManager
) : ViewModel() {

    private val _state = MutableStateFlow(ZoneEditorUiState())
    val state: StateFlow<ZoneEditorUiState> = _state.asStateFlow()

    init {
        // Centre the new zone on the device's current position by default.
        viewModelScope.launch {
            locationProvider.currentFix()?.let { fix ->
                _state.update { it.copy(lat = fix.lat, lng = fix.lng) }
            }
        }
        // The circle told about enter/exit crossings; the API sends no alerts
        // for a zone without one. Defaults to the primary (first) circle.
        viewModelScope.launch {
            circleRepository.refreshCircles()
            val circles = circleRepository.circles.first()
            _state.update { it.copy(circles = circles, selectedCircleId = it.selectedCircleId ?: circles.firstOrNull()?.id) }
        }
    }

    fun onName(v: String) = _state.update { it.copy(name = v) }
    fun selectCircle(id: String) = _state.update { it.copy(selectedCircleId = id) }
    fun onRadius(v: Int) = _state.update { it.copy(radiusM = v) }
    fun onNotifyEnter(v: Boolean) = _state.update { it.copy(notifyEnter = v) }
    fun onNotifyExit(v: Boolean) = _state.update { it.copy(notifyExit = v) }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        if (s.name.isBlank()) { _state.update { it.copy(error = "NAME") }; return }
        // Fall back to Pretoria campus coordinates if no location fix is available.
        val lat = s.lat ?: -25.7545
        val lng = s.lng ?: 28.2314

        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            // Saved before the circle list loaded: still use the cached primary circle.
            val circleId = s.selectedCircleId ?: circleRepository.circles.first().firstOrNull()?.id
            when (zoneRepository.createZone(s.name.trim(), lat, lng, s.radiusM, circleId, s.notifyEnter, s.notifyExit)) {
                is Resource.Success -> {
                    geofenceManager.syncZones(zoneRepository.cachedZones())
                    _state.update { it.copy(saving = false) }
                    onDone()
                }
                is Resource.Error -> _state.update { it.copy(saving = false, error = "SAVE") }
                Resource.Loading -> Unit
            }
        }
    }
}
