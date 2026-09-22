package za.co.kinplus.app.ui.screens.zones

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import za.co.kinplus.app.data.repository.ZoneRepository
import za.co.kinplus.app.domain.SafeZone
import za.co.kinplus.app.location.GeofenceManager
import za.co.kinplus.app.ui.components.EmptyState
import za.co.kinplus.app.ui.components.IconBadge
import za.co.kinplus.app.ui.components.KinTopAppBar
import za.co.kinplus.app.ui.screens.auth.AuthDivider
import za.co.kinplus.app.ui.screens.auth.AuthMuted
import javax.inject.Inject

@HiltViewModel
class SafeZonesViewModel @Inject constructor(
    private val zoneRepository: ZoneRepository,
    private val geofenceManager: GeofenceManager
) : ViewModel() {

    val zones: StateFlow<List<SafeZone>> =
        zoneRepository.zones.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        zoneRepository.refreshZones()
        // Re-register device geofences whenever the zone set changes (FR-21).
        geofenceManager.syncZones(zoneRepository.cachedZones())
    }

    fun delete(id: String) = viewModelScope.launch {
        zoneRepository.deleteZone(id)
        geofenceManager.syncZones(zoneRepository.cachedZones())
    }
}

@Composable
fun SafeZonesScreen(
    onAddZone: () -> Unit,
    onEditZone: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: SafeZonesViewModel = hiltViewModel()
) {
    val zones by viewModel.zones.collectAsState()

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            KinTopAppBar(title = stringResource(R.string.safe_zones_title), onBack = onBack)
            Text(
                stringResource(R.string.safe_zones_subtitle),
                color = AuthMuted, fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (zones.isEmpty()) {
                EmptyState(stringResource(R.string.empty_zones))
            } else {
                Text(
                    stringResource(R.string.saved_zones_section),
                    color = AuthMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp)
                )
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
                        Column {
                            zones.forEachIndexed { index, zone ->
                                ZoneRow(zone, onClick = { onEditZone(zone.id) }, onDelete = { viewModel.delete(zone.id) })
                                if (index != zones.lastIndex) HorizontalDivider(color = AuthDivider)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(96.dp))
        }

        ExtendedFloatingActionButton(
            onClick = onAddZone,
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.add_safe_zone)) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
        )
    }
}

private fun iconForZone(name: String): ImageVector = when {
    name.contains("home", ignoreCase = true) -> Icons.Filled.Home
    name.contains("school", ignoreCase = true) || name.contains("university", ignoreCase = true) -> Icons.Filled.School
    name.contains("work", ignoreCase = true) || name.contains("office", ignoreCase = true) -> Icons.Filled.Work
    else -> Icons.Filled.Place
}

@Composable
private fun ZoneRow(zone: SafeZone, onClick: () -> Unit, onDelete: () -> Unit) {
    ListItem(
        leadingContent = { IconBadge(iconForZone(zone.name)) },
        headlineContent = { Text(zone.name, fontWeight = FontWeight.Medium) },
        supportingContent = {
            val enterLabel = stringResource(R.string.notify_on_enter)
            val exitLabel = stringResource(R.string.notify_on_exit)
            val dir = listOfNotNull(
                enterLabel.takeIf { zone.notifyOnEnter },
                exitLabel.takeIf { zone.notifyOnExit }
            ).joinToString(" · ")
            Text("${zone.radiusM} m · $dir", color = AuthMuted, fontSize = 13.sp)
        },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.remove), tint = AuthMuted)
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick)
    )
}
