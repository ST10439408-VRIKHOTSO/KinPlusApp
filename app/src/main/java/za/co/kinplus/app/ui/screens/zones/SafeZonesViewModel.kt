package za.co.kinplus.app.ui.screens.zones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import za.co.kinplus.app.data.repository.ZoneRepository
import za.co.kinplus.app.domain.SafeZone
import za.co.kinplus.app.location.GeofenceManager
import javax.inject.Inject

@HiltViewModel
class SafeZonesViewModel @Inject constructor(
    private val zoneRepository: ZoneRepository,
    private val geofenceManager: GeofenceManager
) : ViewModel() {

    val zones: StateFlow<List<SafeZone>> =
        zoneRepository.zones.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        zoneRepository.refreshZones()
        // Re-register device geofences whenever the zone set changes (FR-21).
        geofenceManager.syncZones(zoneRepository.cachedZones())
    }

    fun delete(id: String) = viewModelScope.launch {
        zoneRepository.deleteZone(id)
        geofenceManager.syncZones(zoneRepository.cachedZones())
    }
}
