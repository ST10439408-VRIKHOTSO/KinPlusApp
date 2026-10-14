package za.co.kinplus.app.ui.screens.circles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import za.co.kinplus.app.auth.AuthManager
import za.co.kinplus.app.data.repository.CircleRepository
import za.co.kinplus.app.data.repository.LocalPhotoRepository
import za.co.kinplus.app.data.repository.ZoneRepository
import za.co.kinplus.app.domain.Circle
import za.co.kinplus.app.domain.MemberLocation
import za.co.kinplus.app.util.ConnectivityObserver
import za.co.kinplus.app.util.Resource
import javax.inject.Inject

data class CirclesUiState(
    val circles: List<Circle> = emptyList(),
    val primaryMembers: List<MemberLocation> = emptyList(),
    val myUid: String = "",
    val safeZoneCount: Int = 0,
    val isOffline: Boolean = false,
    val deletingCircleId: String? = null,
    val deleteError: String? = null
)

@HiltViewModel
class CirclesViewModel @Inject constructor(
    private val circleRepository: CircleRepository,
    private val localPhotoRepository: LocalPhotoRepository,
    private val zoneRepository: ZoneRepository,
    private val authManager: AuthManager,
    connectivity: ConnectivityObserver
) : ViewModel() {

    private val deleteState = MutableStateFlow<Pair<String?, String?>>(null to null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<CirclesUiState> = circleRepository.circles
        .flatMapLatest { circles ->
            val primaryId = circles.firstOrNull()?.id
            val membersFlow = if (primaryId != null) circleRepository.observeCircleLocations(primaryId) else flowOf(emptyList())
            combine(membersFlow, zoneRepository.zones, connectivity.online, deleteState) { members, zones, online, (deletingId, deleteError) ->
                CirclesUiState(circles, members, authManager.currentUid, zones.size, isOffline = !online, deletingCircleId = deletingId, deleteError = deleteError)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CirclesUiState())

    fun photoPath(circleId: String) = localPhotoRepository.circlePhotoPath(circleId)

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        circleRepository.refreshCircles()
        zoneRepository.refreshZones()
        state.value.circles.firstOrNull()?.let { circleRepository.refreshCircleLocations(it.id) }
    }

    /** Removes the current user from a circle — for the sole owner this is effectively deleting it. */
    fun deleteCircle(circleId: String) = viewModelScope.launch {
        deleteState.value = circleId to null
        when (val res = circleRepository.leaveOrRemove(circleId, authManager.currentUid)) {
            is Resource.Success -> {
                deleteState.value = null to null
                circleRepository.refreshCircles()
            }
            is Resource.Error -> deleteState.value = null to res.message
            Resource.Loading -> Unit
        }
    }

    fun deleteErrorShown() { deleteState.value = deleteState.value.copy(second = null) }
}
