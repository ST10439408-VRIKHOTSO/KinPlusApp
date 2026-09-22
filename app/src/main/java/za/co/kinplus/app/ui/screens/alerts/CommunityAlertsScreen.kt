package za.co.kinplus.app.ui.screens.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CarCrash
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import za.co.kinplus.app.data.repository.AlertRepository
import za.co.kinplus.app.domain.CommunityAlert
import za.co.kinplus.app.location.LocationProvider
import za.co.kinplus.app.ui.components.EmptyState
import za.co.kinplus.app.ui.components.KinPrimaryButton
import za.co.kinplus.app.ui.components.KinTextField
import za.co.kinplus.app.ui.components.KinTopAppBar
import za.co.kinplus.app.ui.screens.auth.AuthMuted
import za.co.kinplus.app.util.Resource
import javax.inject.Inject

data class AlertsUiState(
    val nearby: List<CommunityAlert> = emptyList(),
    val loading: Boolean = true,
    val category: String = "HAZARD",
    val description: String = "",
    val submitting: Boolean = false,
    val submitted: Boolean = false,
    val lat: Double? = null,
    val lng: Double? = null
)

@HiltViewModel
class CommunityAlertsViewModel @Inject constructor(
    private val alertRepository: AlertRepository,
    private val locationProvider: LocationProvider
) : ViewModel() {

    private val _state = MutableStateFlow(AlertsUiState())
    val state: StateFlow<AlertsUiState> = _state.asStateFlow()

    init { load() }

    fun load() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        // Default to Pretoria campus if no fix is available, so the list still loads.
        val fix = locationProvider.currentFix()
        val lat = fix?.lat ?: -25.7545
        val lng = fix?.lng ?: 28.2314
        val res = alertRepository.nearby(lat, lng)
        _state.update {
            it.copy(loading = false, lat = lat, lng = lng, nearby = (res as? Resource.Success)?.data ?: emptyList())
        }
    }

    fun onCategory(v: String) = _state.update { it.copy(category = v) }
    fun onDescription(v: String) = _state.update { it.copy(description = v) }

    fun submit() {
        val s = _state.value
        if (s.description.isBlank()) return
        _state.update { it.copy(submitting = true) }
        viewModelScope.launch {
            alertRepository.submit(s.category, s.description.trim(), s.lat ?: -25.7545, s.lng ?: 28.2314, null)
            _state.update { it.copy(submitting = false, submitted = true, description = "") }
            load()
        }
    }

    fun confirm(id: String) = viewModelScope.launch { alertRepository.confirm(id); load() }
}

private val filterCategories = listOf(
    null to R.string.all,
    "CRIME" to R.string.category_crime,
    "HAZARD" to R.string.category_hazard,
    "MEDICAL" to R.string.category_medical
)

private fun iconForCategory(category: String): ImageVector = when (category.uppercase()) {
    "CRIME" -> Icons.Filled.Gavel
    "HAZARD" -> Icons.Filled.Warning
    "MEDICAL" -> Icons.Filled.MedicalServices
    "FIRE" -> Icons.Filled.LocalFireDepartment
    "ACCIDENT" -> Icons.Filled.CarCrash
    else -> Icons.Filled.ReportProblem
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityAlertsScreen(
    onBack: (() -> Unit)?,
    viewModel: CommunityAlertsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showReport by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf<String?>(null) }

    val visible = state.nearby.filter { filter == null || it.category.equals(filter, ignoreCase = true) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            KinTopAppBar(title = stringResource(R.string.community_alerts), onBack = onBack)

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterCategories.forEach { (key, labelRes) ->
                    FilterChip(
                        selected = filter == key,
                        onClick = { filter = key },
                        label = { Text(stringResource(labelRes)) }
                    )
                }
            }

            if (visible.isEmpty() && !state.loading) {
                EmptyState(stringResource(R.string.empty_alerts))
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    items(visible) { alert -> AlertCard(alert) { viewModel.confirm(alert.id) } }
                    item { Spacer(Modifier.height(96.dp)) }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { showReport = true },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.report)) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
        )
    }

    if (showReport) {
        ModalBottomSheet(onDismissRequest = { showReport = false }) {
            ReportForm(state, viewModel, onSubmitted = { showReport = false })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReportForm(state: AlertsUiState, viewModel: CommunityAlertsViewModel, onSubmitted: () -> Unit) {
    val categories = listOf(
        "CRIME" to R.string.category_crime,
        "HAZARD" to R.string.category_hazard,
        "MEDICAL" to R.string.category_medical,
        "FIRE" to R.string.category_fire,
        "OTHER" to R.string.category_other
    )
    Column(Modifier.fillMaxWidth().padding(20.dp).padding(bottom = 12.dp)) {
        Text(stringResource(R.string.report_incident), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.category), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.forEach { (key, label) ->
                FilterChip(
                    selected = state.category == key,
                    onClick = { viewModel.onCategory(key) },
                    label = { Text(stringResource(label)) }
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        KinTextField(state.description, viewModel::onDescription, stringResource(R.string.description_optional),
            singleLine = false)
        Spacer(Modifier.height(20.dp))
        KinPrimaryButton(stringResource(R.string.submit_report), { viewModel.submit(); onSubmitted() }, loading = state.submitting)
    }
}

@Composable
private fun AlertCard(alert: CommunityAlert, onConfirm: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(iconForCategory(alert.category), contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(alert.category.lowercase().replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                Text(stringResource(R.string.confirm_count, alert.confirmCount), color = AuthMuted, fontSize = 12.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(alert.description, fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onConfirm, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.i_see_it_too), fontSize = 13.sp)
                }
                OutlinedButton(onClick = {}, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.details), fontSize = 13.sp)
                }
            }
        }
    }
}
