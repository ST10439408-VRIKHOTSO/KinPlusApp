package za.co.kinplus.app.ui.screens.journey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import za.co.kinplus.app.data.repository.JourneyRepository
import za.co.kinplus.app.domain.Journey
import za.co.kinplus.app.util.Resource
import javax.inject.Inject

data class JourneyMonitorUiState(
    val journeys: List<Journey> = emptyList(),
    val loading: Boolean = true,
    val arrivedMessage: Boolean = false
)

@HiltViewModel
class JourneyMonitorViewModel @Inject constructor(
    private val journeyRepository: JourneyRepository
) : ViewModel() {

    private val _state = MutableStateFlow(JourneyMonitorUiState())
    val state: StateFlow<JourneyMonitorUiState> = _state.asStateFlow()

    init { load() }

    fun load() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        val res = journeyRepository.watchingJourneys()
        _state.update {
            it.copy(loading = false, journeys = (res as? Resource.Success)?.data ?: emptyList())
        }
    }

    fun confirmArrival(id: String) = viewModelScope.launch {
        journeyRepository.confirmArrival(id)
        _state.update { it.copy(arrivedMessage = true) }
        load()
    }
}
