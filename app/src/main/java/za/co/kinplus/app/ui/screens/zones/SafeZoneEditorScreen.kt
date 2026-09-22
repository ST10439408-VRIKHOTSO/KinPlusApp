package za.co.kinplus.app.ui.screens.zones

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import za.co.kinplus.app.data.repository.ZoneRepository
import za.co.kinplus.app.location.GeofenceManager
import za.co.kinplus.app.location.LocationProvider
import za.co.kinplus.app.ui.components.KinPrimaryButton
import za.co.kinplus.app.ui.components.KinTextField
import za.co.kinplus.app.ui.components.KinTopAppBar
import za.co.kinplus.app.ui.theme.kinSwitchColors
import za.co.kinplus.app.util.Resource
import javax.inject.Inject

data class ZoneEditorUiState(
    val name: String = "",
    val radiusM: Int = 200,
    val notifyEnter: Boolean = true,
    val notifyExit: Boolean = true,
    val lat: Double? = null,
    val lng: Double? = null,
    val saving: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SafeZoneEditorViewModel @Inject constructor(
    private val zoneRepository: ZoneRepository,
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
    }

    fun onName(v: String) = _state.update { it.copy(name = v) }
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
            when (zoneRepository.createZone(s.name.trim(), lat, lng, s.radiusM, null, s.notifyEnter, s.notifyExit)) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafeZoneEditorScreen(
    zoneId: String?,
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: SafeZoneEditorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            KinTopAppBar(title = stringResource(R.string.create_safe_zone), onBack = onBack)
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            KinTextField(state.name, viewModel::onName, stringResource(R.string.zone_name))
            Spacer(Modifier.height(24.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.radius), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.radius_metres, state.radiusM))
            }
            // Radius constrained to 100..5000 m (FR-20).
            Slider(
                value = state.radiusM.toFloat(),
                onValueChange = { viewModel.onRadius(it.toInt()) },
                valueRange = 100f..5000f
            )

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.notify_on_enter), Modifier.weight(1f))
                Switch(checked = state.notifyEnter, onCheckedChange = viewModel::onNotifyEnter, colors = kinSwitchColors())
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.notify_on_exit), Modifier.weight(1f))
                Switch(checked = state.notifyExit, onCheckedChange = viewModel::onNotifyExit, colors = kinSwitchColors())
            }

            state.error?.let {
                Spacer(Modifier.height(8.dp))
                Text(if (it == "NAME") stringResource(R.string.error_name_required) else stringResource(R.string.error_generic),
                    color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(24.dp))
            KinPrimaryButton(stringResource(R.string.save_safe_zone), { viewModel.save(onDone) }, loading = state.saving)
        }
    }
}
