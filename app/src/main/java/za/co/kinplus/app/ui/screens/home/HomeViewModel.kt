package za.co.kinplus.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import za.co.kinplus.app.auth.AuthManager
import za.co.kinplus.app.data.repository.CircleRepository
import za.co.kinplus.app.data.repository.LocationRepository
import za.co.kinplus.app.data.repository.ZoneRepository
import za.co.kinplus.app.domain.Circle
import za.co.kinplus.app.domain.MemberLocation
import za.co.kinplus.app.location.LocationProvider
import za.co.kinplus.app.util.ConnectivityObserver
import javax.inject.Inject

data class HomeUiState(
    val circles: List<Circle> = emptyList(),
    val selectedCircleId: String? = null,
    val members: List<MemberLocation> = emptyList(),
    val safeZoneCount: Int = 0,
    val myUid: String = "",
    val myDisplayName: String = "",
    val isOffline: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val circleRepository: CircleRepository,
    private val locationRepository: LocationRepository,
    private val locationProvider: LocationProvider,
    private val zoneRepository: ZoneRepository,
    private val authManager: AuthManager,
    connectivity: ConnectivityObserver
) : ViewModel() {

    private val selectedCircleId = MutableStateFlow<String?>(null)

    private data class Snapshot(val circles: List<Circle>, val selected: String?, val online: Boolean, val zoneCount: Int)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<HomeUiState> = combine(
        circleRepository.circles,
        selectedCircleId,
        connectivity.online,
        zoneRepository.zones
    ) { circles, selected, online, zones ->
        Snapshot(circles, selected ?: circles.firstOrNull()?.id, online, zones.size)
    }.flatMapLatest { snapshot ->
        val locationFlow = if (snapshot.selected != null) circleRepository.observeCircleLocations(snapshot.selected)
        else flowOf(emptyList())
        locationFlow.map { members ->
            HomeUiState(
                snapshot.circles, snapshot.selected, members, snapshot.zoneCount,
                authManager.currentUid, authManager.currentDisplayName, isOffline = !snapshot.online
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    init {
        refresh()
        publishOwnLocation()
    }

    fun selectCircle(id: String) { selectedCircleId.value = id }

    fun refresh() = viewModelScope.launch {
        circleRepository.refreshCircles()
        zoneRepository.refreshZones()
        state.value.selectedCircleId?.let { circleRepository.refreshCircleLocations(it) }
    }

    /** Publishes this device's location once so members appear on the map (FR-13). */
    private fun publishOwnLocation() = viewModelScope.launch {
        locationProvider.currentFix()?.let {
            locationRepository.publish(it.lat, it.lng, it.accuracyM, it.batteryPct)
        }
    }

    /** A fresh GPS fix for the map's "locate me" button. */
    suspend fun locateMe() = locationProvider.currentFix()

    /** The device's last cached fix, if any — used to centre the map instantly on open. */
    suspend fun locateFast() = locationProvider.lastKnownFix()
}
