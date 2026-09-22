package za.co.kinplus.app.ui.screens.journey

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import za.co.kinplus.app.ui.components.AvatarImage
import za.co.kinplus.app.ui.screens.auth.AuthDivider
import za.co.kinplus.app.ui.screens.auth.AuthMuted
import za.co.kinplus.app.ui.theme.kinSwitchColors
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import za.co.kinplus.app.R
import za.co.kinplus.app.data.repository.CircleRepository
import za.co.kinplus.app.data.repository.JourneyRepository
import za.co.kinplus.app.domain.Circle
import za.co.kinplus.app.domain.CircleMember
import za.co.kinplus.app.ui.components.KinPrimaryButton
import za.co.kinplus.app.ui.components.KinTextField
import za.co.kinplus.app.ui.components.KinTopAppBar
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JourneySetupScreen(
    onStarted: () -> Unit,
    onBack: () -> Unit,
    viewModel: JourneySetupViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            KinTopAppBar(title = stringResource(R.string.safe_journey), onBack = onBack)
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text(stringResource(R.string.plan_your_journey), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))

            Text(stringResource(R.string.trip_section), color = AuthMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column {
                    TripRow(Icons.Filled.LocationOn, stringResource(R.string.from), stringResource(R.string.current_location))
                    HorizontalDivider(color = AuthDivider)
                    TripDestinationRow(state.destination, viewModel::onDestination)
                    HorizontalDivider(color = AuthDivider)
                    TripRow(Icons.Filled.Schedule, stringResource(R.string.eta), stringResource(R.string.eta_minutes, state.etaMinutes))
                }
            }
            Spacer(Modifier.height(4.dp))
            Slider(
                value = state.etaMinutes.toFloat(),
                onValueChange = { viewModel.onEta(it.toInt()) },
                valueRange = 5f..180f
            )

            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.select_watchers), style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.circles.take(4).forEach { circle ->
                    FilterChip(
                        selected = state.selectedCircleId == circle.id,
                        onClick = { viewModel.selectCircle(circle.id) },
                        label = { Text(circle.name) }
                    )
                }
            }

            if (state.members.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.notify_these_people), color = AuthMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column {
                        state.members.forEachIndexed { index, member ->
                            ListItem(
                                leadingContent = { AvatarImage(photoPath = null, fallbackText = member.displayName, size = 36.dp) },
                                headlineContent = { Text(member.displayName) },
                                trailingContent = {
                                    Switch(
                                        checked = state.selectedWatcherIds.contains(member.uid),
                                        onCheckedChange = { viewModel.toggleWatcher(member.uid) },
                                        colors = kinSwitchColors()
                                    )
                                },
                                colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                                modifier = Modifier.clickable { viewModel.toggleWatcher(member.uid) }
                            )
                            if (index != state.members.lastIndex) HorizontalDivider(color = AuthDivider)
                        }
                    }
                }
            }

            state.error?.let {
                Spacer(Modifier.height(8.dp))
                Text(if (it == "DEST") stringResource(R.string.enter_destination) else stringResource(R.string.error_generic),
                    color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(24.dp))
            KinPrimaryButton(stringResource(R.string.start_journey), { viewModel.start(onStarted) }, loading = state.saving)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TripRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    ListItem(
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        headlineContent = { Text(label, color = AuthMuted, fontSize = 13.sp) },
        trailingContent = { Text(value, fontWeight = FontWeight.Medium) },
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
    )
}

@Composable
private fun TripDestinationRow(value: String, onValueChange: (String) -> Unit) {
    ListItem(
        leadingContent = { Icon(Icons.Filled.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        headlineContent = { Text(stringResource(R.string.destination), color = AuthMuted, fontSize = 13.sp) },
        trailingContent = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text(stringResource(R.string.enter_destination)) },
                singleLine = true,
                modifier = Modifier.width(180.dp)
            )
        },
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
    )
}
