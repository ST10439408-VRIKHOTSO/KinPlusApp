package za.co.kinplus.app.ui.screens.zones

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import za.co.kinplus.app.data.local.SettingsStore
import za.co.kinplus.app.data.repository.ZoneRepository
import za.co.kinplus.app.domain.SafeZone
import za.co.kinplus.app.location.GeofenceManager
import za.co.kinplus.app.util.Resource
import javax.inject.Inject

/** Saved Safe Zones from the Room cache, refreshed from the API, with the chosen units. */
@HiltViewModel
class SafeZonesViewModel @Inject constructor(
    private val zoneRepository: ZoneRepository,
    private val geofenceManager: GeofenceManager,
    settingsStore: SettingsStore
) : ViewModel() {

    val zones: StateFlow<List<SafeZone>> =
        zoneRepository.zones.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Settings > Units: radii are shown in metres, or in feet when false. */
    val useMetric: StateFlow<Boolean> =
        settingsStore.useMetric.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        val result = zoneRepository.refreshZones()
        Log.d(TAG, "Zones refreshed: ${if (result is Resource.Success) "${result.data.size} zone(s)" else "failed"}")
        // Re-register device geofences whenever the zone set changes (FR-21).
        geofenceManager.syncZones(zoneRepository.cachedZones())
    }

    fun delete(id: String) = viewModelScope.launch {
        val result = zoneRepository.deleteZone(id)
        Log.i(TAG, "Delete zone $id: ${if (result is Resource.Success) "done" else "failed"}")
        geofenceManager.syncZones(zoneRepository.cachedZones())
    }

    private companion object {
        const val TAG = "SafeZonesViewModel"
    }
}
