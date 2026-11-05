package za.co.kinplus.app.ui.screens.zones

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import za.co.kinplus.app.data.local.SettingsStore
import za.co.kinplus.app.data.repository.CircleRepository
import za.co.kinplus.app.data.repository.ZoneRepository
import za.co.kinplus.app.domain.Circle
import za.co.kinplus.app.domain.SafeZone
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
    val error: String? = null,
    /** Settings > Units: the radius is shown in metres, or in feet when false. */
    val useMetric: Boolean = true,
    /** True when an existing zone was opened from the Safe Zones list. */
    val editing: Boolean = false
)

/**
 * Creates a Safe Zone, or edits one opened from the Safe Zones list (the
 * "zoneId" navigation argument). An edit can change the name, radius and
 * alert switches, which is what the API's PATCH endpoint accepts.
 */
@HiltViewModel
class SafeZoneEditorViewModel @Inject constructor(
    private val zoneRepository: ZoneRepository,
    private val circleRepository: CircleRepository,
    private val locationProvider: LocationProvider,
    private val geofenceManager: GeofenceManager,
    private val settingsStore: SettingsStore,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val zoneId: String? = savedStateHandle["zoneId"]
    private var existing: SafeZone? = null

    private val _state = MutableStateFlow(ZoneEditorUiState(editing = zoneId != null))
    val state: StateFlow<ZoneEditorUiState> = _state.asStateFlow()

    init {
        if (zoneId != null) {
            // Editing: start from the saved zone instead of the current position.
            viewModelScope.launch {
                val zone = zoneRepository.cachedZones().firstOrNull { it.id == zoneId }
                existing = zone
                if (zone == null) {
                    Log.w(TAG, "Zone $zoneId is not in the cache; showing an empty form")
                    _state.update { it.copy(editing = false) }
                } else {
                    Log.d(TAG, "Editing zone ${zone.id}")
                    _state.update {
                        it.copy(
                            name = zone.name, radiusM = zone.radiusM, notifyEnter = zone.notifyOnEnter,
                            notifyExit = zone.notifyOnExit, lat = zone.lat, lng = zone.lng,
                            selectedCircleId = zone.circleId ?: it.selectedCircleId
                        )
                    }
                }
            }
        } else {
            // Centre the new zone on the device's current position by default.
            viewModelScope.launch {
                locationProvider.currentFix()?.let { fix ->
                    _state.update { it.copy(lat = fix.lat, lng = fix.lng) }
                }
            }
        }
        // The circle told about enter/exit crossings; the API sends no alerts
        // for a zone without one. Defaults to the primary (first) circle.
        viewModelScope.launch {
            circleRepository.refreshCircles()
            val circles = circleRepository.circles.first()
            _state.update { it.copy(circles = circles, selectedCircleId = it.selectedCircleId ?: circles.firstOrNull()?.id) }
        }
        viewModelScope.launch {
            settingsStore.useMetric.collect { metric -> _state.update { it.copy(useMetric = metric) } }
        }
    }

    fun onName(v: String) = _state.update { it.copy(name = v, error = null) }
    fun selectCircle(id: String) = _state.update { it.copy(selectedCircleId = id) }
    fun onRadius(v: Int) = _state.update { it.copy(radiusM = v) }
    fun onNotifyEnter(v: Boolean) = _state.update { it.copy(notifyEnter = v) }
    fun onNotifyExit(v: Boolean) = _state.update { it.copy(notifyExit = v) }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        if (s.name.isBlank()) { _state.update { it.copy(error = "NAME") }; return }

        // Opened for editing but the saved zone has not loaded yet: wait for it.
        if (s.editing && existing == null) return
        _state.update { it.copy(saving = true, error = null) }
        val zone = existing
        if (zone != null) {
            viewModelScope.launch {
                when (zoneRepository.updateZone(zone, s.name.trim(), s.radiusM, s.notifyEnter, s.notifyExit)) {
                    is Resource.Success -> {
                        geofenceManager.syncZones(zoneRepository.cachedZones())
                        _state.update { it.copy(saving = false) }
                        onDone()
                    }
                    is Resource.Error -> _state.update { it.copy(saving = false, error = "SAVE") }
                    Resource.Loading -> Unit
                }
            }
            return
        }
        viewModelScope.launch {
            // The zone is centred on where the user is. Without a location fix it
            // would be placed somewhere wrong, so the user is asked to turn on GPS.
            val fix = if (s.lat != null && s.lng != null) null else locationProvider.currentFix()
            val lat = s.lat ?: fix?.lat
            val lng = s.lng ?: fix?.lng
            if (lat == null || lng == null) {
                Log.w(TAG, "Safe Zone not saved: no location fix")
                _state.update { it.copy(saving = false, error = "LOCATION") }
                return@launch
            }

            // Saved before the circle list loaded: still use the cached primary circle.
            val circleId = s.selectedCircleId ?: circleRepository.circles.first().firstOrNull()?.id
            when (zoneRepository.createZone(s.name.trim(), lat, lng, s.radiusM, circleId, s.notifyEnter, s.notifyExit)) {
                is Resource.Success -> {
                    geofenceManager.syncZones(zoneRepository.cachedZones())
                    Log.i(TAG, "Safe Zone '${s.name.trim()}' saved with a ${s.radiusM} m radius")
                    _state.update { it.copy(saving = false) }
                    onDone()
                }
                is Resource.Error -> _state.update { it.copy(saving = false, error = "SAVE") }
                Resource.Loading -> Unit
            }
        }
    }

    private companion object {
        const val TAG = "SafeZoneEditorVM"
    }
}
