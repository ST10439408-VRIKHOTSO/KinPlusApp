package za.co.kinplus.app.ui.screens.sos

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CarCrash
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import za.co.kinplus.app.R
import za.co.kinplus.app.data.local.EmergencyContactEntity
import za.co.kinplus.app.data.repository.CircleRepository
import za.co.kinplus.app.data.repository.EmergencyContactRepository
import za.co.kinplus.app.data.repository.SosRepository
import za.co.kinplus.app.domain.Circle
import za.co.kinplus.app.location.LocationFix
import za.co.kinplus.app.location.LocationProvider
import za.co.kinplus.app.ui.components.KinAppTopBar
import za.co.kinplus.app.ui.screens.auth.AuthMuted
import za.co.kinplus.app.util.Resource
import javax.inject.Inject

data class SosUiState(
    val circles: List<Circle> = emptyList(),
    val selectedCircleIds: Set<String> = emptySet(),
    val message: String = "",
    val responders: List<EmergencyContactEntity> = emptyList(),
    val fix: LocationFix? = null,
    val sending: Boolean = false,
    val sentQueued: Boolean = false,
    val sent: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SosViewModel @Inject constructor(
    private val sosRepository: SosRepository,
    private val circleRepository: CircleRepository,
    private val emergencyContactRepository: EmergencyContactRepository,
    private val locationProvider: LocationProvider
) : ViewModel() {

    private val _state = MutableStateFlow(SosUiState())
    val state: StateFlow<SosUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            circleRepository.circles.collect { circles ->
                _state.update { it.copy(circles = circles, selectedCircleIds = circles.map { c -> c.id }.toSet()) }
            }
        }
        viewModelScope.launch {
            emergencyContactRepository.observe().collect { contacts -> _state.update { it.copy(responders = contacts) } }
        }
        viewModelScope.launch {
            val fix = locationProvider.currentFix()
            _state.update { it.copy(fix = fix) }
        }
    }

    fun onMessage(v: String) = _state.update { it.copy(message = v) }

    /** Raises the SOS. Works offline: the alert is queued and sent on reconnect. */
    fun raiseSos() {
        val s = _state.value
        _state.update { it.copy(sending = true, error = null) }
        viewModelScope.launch {
            val fix = s.fix ?: locationProvider.currentFix()
            when (val res = sosRepository.raiseSos(
                lat = fix?.lat, lng = fix?.lng,
                message = s.message.ifBlank { null },
                circleIds = s.selectedCircleIds.toList()
            )) {
                is Resource.Success -> _state.update {
                    it.copy(sending = false, sent = res.data != null, sentQueued = res.data == null)
                }
                is Resource.Error -> _state.update { it.copy(sending = false, error = res.message) }
                Resource.Loading -> Unit
            }
        }
    }

    companion object { const val HOLD_SECONDS = 3 }
}

private data class IncidentType(val titleRes: Int, val subtitleRes: Int, val icon: ImageVector)
private val incidentTypes = listOf(
    IncidentType(R.string.incident_medical, R.string.incident_medical_sub, Icons.Filled.MedicalServices),
    IncidentType(R.string.incident_collision, R.string.incident_collision_sub, Icons.Filled.CarCrash),
    IncidentType(R.string.incident_followed, R.string.incident_followed_sub, Icons.Filled.DirectionsWalk),
    IncidentType(R.string.incident_silent, R.string.incident_silent_sub, Icons.Filled.VolumeOff)
)

@Composable
fun SosScreen(
    onClose: () -> Unit,
    onOpenProfile: () -> Unit,
    initialCategory: String? = null,
    viewModel: SosViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var pressing by remember { mutableStateOf(false) }
    var selectedIncident by remember { mutableIntStateOf(0) }

    LaunchedEffect(initialCategory) {
        if (!initialCategory.isNullOrBlank() && state.message.isBlank()) viewModel.onMessage(initialCategory)
    }

    val progress by animateFloatAsState(
        targetValue = if (pressing) 1f else 0f,
        animationSpec = tween(durationMillis = SosViewModel.HOLD_SECONDS * 1000),
        label = "sosHold",
        finishedListener = { value -> if (value >= 1f && pressing) viewModel.raiseSos() }
    )

    val pulseTransition = rememberInfiniteTransition(label = "sosPulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label = "sosPulseValue"
    )

    LaunchedEffect(state.sent, state.sentQueued) {
        if (state.sent || state.sentQueued) { kotlinx.coroutines.delay(1500); onClose() }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        KinAppTopBar(title = stringResource(R.string.emergency_sos), onBack = onClose, onProfile = onOpenProfile)

        SignalLockBanner(state.fix, Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(20.dp))

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                if (!pressing) {
                    Box(
                        Modifier.size(220.dp * (0.72f + 0.28f * pulse)).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f * (1f - pulse)))
                    )
                    Box(
                        Modifier.size(198.dp * (0.8f + 0.2f * pulse)).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.18f * (1f - pulse)))
                    )
                }
                Box(
                    modifier = Modifier
                        .size(184.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.55f + 0.45f * progress))
                        .pointerInput(Unit) {
                            detectTapGestures(onPress = {
                                pressing = true
                                val released = tryAwaitRelease()
                                if (released) pressing = false
                            })
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.WbSunny, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                        Text(stringResource(R.string.sos), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(stringResource(R.string.sos_hold_seconds_label, SosViewModel.HOLD_SECONDS), color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            stringResource(R.string.sos_hold_instructions, SosViewModel.HOLD_SECONDS),
            fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Text(
            stringResource(R.string.sos_hold_cancel_note),
            color = AuthMuted, fontSize = 12.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        if (state.sending) { Spacer(Modifier.height(12.dp)); CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally)) }
        if (state.sent) Text(stringResource(R.string.sos_sent), color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp))
        if (state.sentQueued) Text(stringResource(R.string.error_offline_banner), color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp))

        SosSectionHeader(stringResource(R.string.incident_nature_title), stringResource(R.string.incident_nature_sub))
        IncidentGrid(
            selected = selectedIncident,
            onSelect = { index ->
                selectedIncident = index
                viewModel.onMessage(context.getString(incidentTypes[index].titleRes))
            },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        SosSectionHeader(stringResource(R.string.direct_interventions_title), null)
        Column(Modifier.padding(horizontal = 16.dp)) {
            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
                Column {
                    InterventionRow(
                        icon = Icons.Filled.Call, iconTint = MaterialTheme.colorScheme.error, iconBg = MaterialTheme.colorScheme.errorContainer,
                        title = stringResource(R.string.intervention_emergency_services),
                        subtitle = stringResource(R.string.intervention_emergency_services_sub),
                        actionLabel = stringResource(R.string.intervention_dial),
                        actionIcon = Icons.Filled.Call,
                        actionContainerColor = MaterialTheme.colorScheme.error,
                        actionContentColor = Color.White
                    ) { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))) }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    InterventionRow(
                        icon = Icons.Filled.PersonAddAlt1, iconTint = MaterialTheme.colorScheme.primary, iconBg = MaterialTheme.colorScheme.primaryContainer,
                        title = stringResource(R.string.intervention_notify_family),
                        subtitle = stringResource(R.string.intervention_notify_family_sub),
                        actionLabel = stringResource(R.string.intervention_alert),
                        actionIcon = Icons.Filled.Campaign,
                        actionContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        actionContentColor = MaterialTheme.colorScheme.onSurface
                    ) { viewModel.raiseSos() }
                }
            }
        }

        SosSectionHeader(
            stringResource(R.string.designated_responders_title, state.responders.size),
            stringResource(R.string.high_priority_reach)
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            state.responders.take(6).forEach { responder ->
                ResponderChip(responder.name)
            }
        }

        Spacer(Modifier.height(20.dp))
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(14.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(Modifier.padding(14.dp)) {
                Icon(Icons.Filled.Shield, contentDescription = null, tint = AuthMuted, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.emergency_protocol_title) + " " + stringResource(R.string.emergency_protocol_body),
                    color = AuthMuted, fontSize = 11.sp, lineHeight = 15.sp
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SignalLockBanner(fix: LocationFix?, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.active_signal_lock),
                    fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(4.dp))
            val coords = if (fix != null) "%.4f° N, %.4f° W".format(fix.lat, fix.lng) else "…"
            Text(
                stringResource(R.string.signal_lock_body, coords),
                fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SosSectionHeader(title: String, trailing: String?) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).padding(top = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        if (trailing != null) Text(trailing, fontSize = 11.sp, color = AuthMuted, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun IncidentGrid(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        incidentTypes.chunked(2).forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEachIndexed { colIndex, type ->
                    val index = rowIndex * 2 + colIndex
                    IncidentCard(type, isSelected = index == selected, onClick = { onSelect(index) }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun IncidentCard(type: IncidentType, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Box(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(type.icon, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(type.titleRes), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(stringResource(type.subtitleRes), fontSize = 10.sp, color = AuthMuted, maxLines = 1)
                }
            }
            if (isSelected) {
                Icon(
                    Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun InterventionRow(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    actionLabel: String,
    actionIcon: ImageVector,
    actionContainerColor: Color,
    actionContentColor: Color,
    onAction: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(iconBg), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(subtitle, fontSize = 11.sp, color = AuthMuted, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(50),
            color = actionContainerColor,
            modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onAction)
        ) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(actionIcon, contentDescription = null, tint = actionContentColor, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(5.dp))
                Text(actionLabel, color = actionContentColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ResponderChip(name: String) {
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
            Spacer(Modifier.width(6.dp))
            Text(name, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}
