package za.co.kinplus.app.ui.screens.journey

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
import za.co.kinplus.app.data.repository.JourneyRepository
import za.co.kinplus.app.domain.Circle
import za.co.kinplus.app.domain.CircleMember
import za.co.kinplus.app.util.Resource
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class JourneySetupUiState(
    val destination: String = "",
    val etaMinutes: Int = 30,
    val circles: List<Circle> = emptyList(),
    val selectedCircleId: String? = null,
    val members: List<CircleMember> = emptyList(),
    val selectedWatcherIds: Set<String> = emptySet(),
    val saving: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class JourneySetupViewModel @Inject constructor(
    private val journeyRepository: JourneyRepository,
    private val circleRepository: CircleRepository
) : ViewModel() {

    private val _state = MutableStateFlow(JourneySetupUiState())
    val state: StateFlow<JourneySetupUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            circleRepository.refreshCircles()
            val circles = circleRepository.circles.first()
            _state.update { it.copy(circles = circles) }
            circles.firstOrNull()?.let { selectCircle(it.id) }
        }
    }

    fun onDestination(v: String) = _state.update { it.copy(destination = v) }
    fun onEta(v: Int) = _state.update { it.copy(etaMinutes = v) }

    fun selectCircle(id: String) {
        _state.update { it.copy(selectedCircleId = id, members = emptyList(), selectedWatcherIds = emptySet()) }
        viewModelScope.launch {
            val members = (circleRepository.getCircleDetail(id) as? Resource.Success)?.data?.members ?: emptyList()
            _state.update { it.copy(members = members, selectedWatcherIds = members.map { m -> m.uid }.toSet()) }
        }
    }

    fun toggleWatcher(uid: String) = _state.update {
        val set = it.selectedWatcherIds.toMutableSet().apply { if (contains(uid)) remove(uid) else add(uid) }
        it.copy(selectedWatcherIds = set)
    }

    fun start(onStarted: () -> Unit) {
        val s = _state.value
        if (s.destination.isBlank()) { _state.update { it.copy(error = "DEST") }; return }

        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val etaIso = Instant.now().plus(s.etaMinutes.toLong(), ChronoUnit.MINUTES).toString()
            when (journeyRepository.startJourney(
                s.destination.trim(), null, null, etaIso, s.selectedCircleId, s.selectedWatcherIds.toList()
            )) {
                is Resource.Success -> { _state.update { it.copy(saving = false) }; onStarted() }
                is Resource.Error -> _state.update { it.copy(saving = false, error = "SAVE") }
                Resource.Loading -> Unit
            }
        }
    }
}
