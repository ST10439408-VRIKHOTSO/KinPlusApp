package za.co.kinplus.app.ui.screens.journey

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import za.co.kinplus.app.R
import za.co.kinplus.app.data.repository.JourneyRepository
import za.co.kinplus.app.domain.Journey
import za.co.kinplus.app.ui.components.EmptyState
import za.co.kinplus.app.ui.components.KinPrimaryButton
import za.co.kinplus.app.ui.components.KinTopAppBar
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JourneyMonitorScreen(
    onBack: () -> Unit,
    viewModel: JourneyMonitorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            KinTopAppBar(title = stringResource(R.string.journey_in_progress), onBack = onBack)
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.arrivedMessage) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.journey_arrived), Modifier.padding(16.dp))
                }
            }
            if (state.journeys.isEmpty() && !state.loading) {
                EmptyState(stringResource(R.string.plan_your_journey))
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
                    items(state.journeys) { journey -> JourneyCard(journey) { viewModel.confirmArrival(journey.id) } }
                }
            }
        }
    }
}

@Composable
private fun JourneyCard(journey: Journey, onArrive: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(journey.travellerName, style = MaterialTheme.typography.titleMedium)
            Text(journey.destinationLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text("${stringResource(R.string.eta)}: ${journey.etaAt}",
                style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            KinPrimaryButton(stringResource(R.string.confirm_arrival), onArrive)
        }
    }
}
