package za.co.kinplus.app.ui.screens.safety

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import za.co.kinplus.app.R
import za.co.kinplus.app.data.local.EmergencyContactEntity
import za.co.kinplus.app.data.repository.EmergencyContactRepository
import za.co.kinplus.app.data.repository.EmergencyInfoRepository
import za.co.kinplus.app.data.repository.SosRepository
import za.co.kinplus.app.data.repository.UserRepository
import za.co.kinplus.app.domain.EmergencyInfo
import za.co.kinplus.app.location.LocationProvider
import za.co.kinplus.app.ui.components.KinAppTopBar
import za.co.kinplus.app.ui.screens.auth.AuthDivider
import za.co.kinplus.app.ui.screens.auth.AuthMuted
import za.co.kinplus.app.ui.theme.kinSwitchColors
import za.co.kinplus.app.util.Resource
import javax.inject.Inject

data class SafetyUiState(
    val setupPercent: Int = 0,
    val setupFieldsFilled: Int = 0,
    val setupFieldsTotal: Int = TOTAL_SETUP_FIELDS,
    val incidentCount: Int = 0,
    val contacts: List<EmergencyContactEntity> = emptyList(),
    val emergencyInfo: EmergencyInfo = EmergencyInfo(),
    val locationPaused: Boolean = false
) {
    companion object {
        const val TOTAL_EMERGENCY_INFO_FIELDS = 18
        // Emergency Info's own fields (About Me, Medical, Vehicle, Addresses,
        // Home Security) plus one more for "has at least one emergency
        // contact" — Safety Setup isn't complete until every one of these is
        // in place, not just the Emergency Info fields on their own.
        const val TOTAL_SETUP_FIELDS = TOTAL_EMERGENCY_INFO_FIELDS + 1
    }
}

@HiltViewModel
class SafetyViewModel @Inject constructor(
    private val emergencyInfoRepository: EmergencyInfoRepository,
    private val emergencyContactRepository: EmergencyContactRepository,
    private val userRepository: UserRepository,
    sosRepository: SosRepository,
    private val locationProvider: LocationProvider
) : ViewModel() {

    private val pauseState = kotlinx.coroutines.flow.MutableStateFlow(false)

    val state: StateFlow<SafetyUiState> = combine(
        emergencyInfoRepository.observe(),
        sosRepository.history,
        emergencyContactRepository.observe(),
        pauseState
    ) { info, history, contacts, paused ->
        // Safety Setup spans Emergency Info (About Me, Medical, Vehicle,
        // Addresses, Home Security) *and* having a primary + backup emergency
        // contact — filling in just one half shouldn't read as "done", and
        // this threshold matches the "add a backup contact" nudge below.
        val infoTotal = SafetyUiState.TOTAL_EMERGENCY_INFO_FIELDS
        val infoFilled = (info.overallPercentComplete * infoTotal) / 100
        val total = SafetyUiState.TOTAL_SETUP_FIELDS
        val filled = infoFilled + if (contacts.size >= 2) 1 else 0
        SafetyUiState(
            setupPercent = (filled * 100) / total,
            setupFieldsFilled = filled,
            setupFieldsTotal = total,
            incidentCount = history.size,
            contacts = contacts,
            emergencyInfo = info,
            locationPaused = paused
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SafetyUiState())

    init {
        viewModelScope.launch {
            val profile = userRepository.getProfile()
            pauseState.value = (profile as? Resource.Success)?.data?.sharingEnabled?.not() ?: false
        }
    }

    /** Pausing instantly hides the user's location everywhere (mirrors Profile > Location Settings). */
    fun togglePauseLocation(paused: Boolean) {
        pauseState.value = paused
        viewModelScope.launch { userRepository.updateProfile(sharingEnabled = !paused) }
    }

    /** Builds a "here's where I am" share text from a fresh location fix, or null without permission/fix. */
    fun shareLocation(context: Context) = viewModelScope.launch {
        val fix = locationProvider.currentFix() ?: return@launch
        val text = "I'm sharing my location with you via Kin+: https://maps.google.com/?q=${fix.lat},${fix.lng}"
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) },
                null
            )
        )
    }

    fun addContact(name: String, relationship: String, phone: String) = viewModelScope.launch {
        emergencyContactRepository.add(name, relationship, phone)
    }

    fun deleteContact(id: String) = viewModelScope.launch {
        emergencyContactRepository.delete(id)
    }
}

private data class EmergencyNumber(val labelRes: Int, val number: String)
private val emergencyNumbers = listOf(
    EmergencyNumber(R.string.num_police, "10111"),
    EmergencyNumber(R.string.num_ambulance, "10177"),
    EmergencyNumber(R.string.num_112, "112"),
    EmergencyNumber(R.string.num_flying_squad, "10111")
)

private fun dial(context: Context, number: String) {
    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
}

@Composable
fun SafetyScreen(
    onOpenSos: (String?) -> Unit,
    onOpenCircles: () -> Unit,
    onOpenSafePlaces: () -> Unit,
    onOpenSmartAlerts: () -> Unit,
    onOpenCommunityAlerts: () -> Unit,
    onOpenIncidentHistory: () -> Unit,
    onOpenEmergencyInfo: () -> Unit,
    onOpenProfile: () -> Unit,
    viewModel: SafetyViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showAddContact by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        KinAppTopBar(title = stringResource(R.string.safety_hub_title), onFilter = {}, onProfile = onOpenProfile)

        SecurityScoreCard(
            state,
            onCompleteSetup = onOpenEmergencyInfo,
            onAddBackupContact = { showAddContact = true },
            modifier = Modifier.padding(16.dp)
        )

        Spacer(Modifier.height(4.dp))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionTile(Icons.Filled.NearMe, stringResource(R.string.quick_share_journey), stringResource(R.string.quick_share_journey_sub), Modifier.weight(1f), onClick = onOpenSmartAlerts)
            QuickActionTile(Icons.Filled.VerifiedUser, stringResource(R.string.quick_checkin), stringResource(R.string.quick_checkin_sub), Modifier.weight(1f)) { viewModel.shareLocation(context) }
            QuickActionTile(Icons.Filled.PhoneCallback, stringResource(R.string.quick_fake_call), stringResource(R.string.quick_fake_call_sub), Modifier.weight(1f)) { onOpenSos(null) }
        }

        SectionLabel(stringResource(R.string.emergency_contacts)) {
            Text(stringResource(R.string.emergency_contacts_count, state.contacts.size), color = AuthMuted, fontSize = 12.sp)
        }
        EmergencyContactsSection(
            contacts = state.contacts,
            onCall = { dial(context, it) },
            onDelete = viewModel::deleteContact,
            onAdd = { showAddContact = true }
        )

        SectionLabel(stringResource(R.string.local_device_vault_title)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = AuthMuted, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.encrypted_label), color = AuthMuted, fontSize = 12.sp)
            }
        }
        LocalDeviceVaultSection(state.emergencyInfo, onClick = onOpenEmergencyInfo, modifier = Modifier.padding(horizontal = 16.dp))

        SectionLabel(stringResource(R.string.privacy_controls_title))
        Column(Modifier.padding(horizontal = 16.dp)) {
            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
                Column {
                    ListItem(
                        leadingContent = { Icon(Icons.Filled.LocationOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) },
                        headlineContent = { Text(stringResource(R.string.pause_location_sharing), fontWeight = FontWeight.Medium) },
                        supportingContent = { Text(stringResource(R.string.pause_location_sharing_sub), color = AuthMuted, fontSize = 12.sp) },
                        trailingContent = {
                            Switch(checked = state.locationPaused, onCheckedChange = viewModel::togglePauseLocation, colors = kinSwitchColors())
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }

        SectionLabel(stringResource(R.string.recent_activity))
        Column(Modifier.padding(horizontal = 16.dp)) {
            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
                SafetyNetworkRow(
                    Icons.Filled.History, stringResource(R.string.incident_history_title),
                    if (state.incidentCount == 0) stringResource(R.string.incident_history_sub)
                    else stringResource(R.string.incident_count, state.incidentCount),
                    onOpenIncidentHistory
                )
            }
        }

        SectionLabel(stringResource(R.string.emergency_numbers_section))
        Column(Modifier.padding(horizontal = 16.dp)) {
            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
                Column {
                    emergencyNumbers.forEachIndexed { index, entry ->
                        EmergencyNumberRow(entry) { dial(context, entry.number) }
                        if (index != emergencyNumbers.lastIndex) HorizontalDivider(color = AuthDivider)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showAddContact) {
        AddContactDialog(
            onDismiss = { showAddContact = false },
            onSave = { name, relationship, phone -> viewModel.addContact(name, relationship, phone) }
        )
    }
}

@Composable
private fun SecurityScoreCard(
    state: SafetyUiState,
    onCompleteSetup: () -> Unit,
    onAddBackupContact: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.security_score_label), color = AuthMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.safety_setup_title), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.safety_setup_complete, state.setupPercent), color = AuthMuted, fontSize = 13.sp)
                }
                ScoreRing(percent = state.setupPercent)
            }
            if (state.contacts.size < 2) {
                Spacer(Modifier.height(14.dp))
                Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.backup_contact_nudge_title), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text(stringResource(R.string.backup_contact_nudge_sub), fontSize = 11.sp, color = AuthMuted, maxLines = 1)
                        }
                        Button(onClick = onAddBackupContact, shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                            Text(stringResource(R.string.action_complete), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else if (state.setupPercent < 100) {
                Spacer(Modifier.height(14.dp))
                Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.emergency_information), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text(stringResource(R.string.percent_complete, state.setupPercent), fontSize = 11.sp, color = AuthMuted, maxLines = 1)
                        }
                        Button(onClick = onCompleteSetup, shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                            Text(stringResource(R.string.action_complete), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreRing(percent: Int, size: androidx.compose.ui.unit.Dp = 56.dp) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { 1f }, modifier = Modifier.fillMaxSize(), strokeWidth = 5.dp,
            color = MaterialTheme.colorScheme.surfaceContainer, trackColor = Color.Transparent
        )
        CircularProgressIndicator(
            progress = { percent / 100f }, modifier = Modifier.fillMaxSize(), strokeWidth = 5.dp,
            color = MaterialTheme.colorScheme.primary, trackColor = Color.Transparent
        )
        Text("$percent%", fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun QuickActionTile(icon: ImageVector, title: String, subtitle: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(6.dp))
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 1)
            Text(subtitle, fontSize = 10.sp, color = AuthMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 1)
        }
    }
}

@Composable
private fun EmergencyContactsSection(
    contacts: List<EmergencyContactEntity>,
    onCall: (String) -> Unit,
    onDelete: (String) -> Unit,
    onAdd: () -> Unit
) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
            Column {
                if (contacts.isEmpty()) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Group, contentDescription = null, tint = AuthMuted, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.no_emergency_contacts), fontWeight = FontWeight.Medium)
                        Text(stringResource(R.string.no_emergency_contacts_sub), color = AuthMuted, fontSize = 13.sp)
                    }
                    HorizontalDivider(color = AuthDivider)
                } else {
                    contacts.forEachIndexed { index, contact ->
                        EmergencyContactRow(contact, index, onCall = { onCall(contact.phone) }, onDelete = { onDelete(contact.id) })
                        if (index != contacts.lastIndex) HorizontalDivider(color = AuthDivider)
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.add_trusted_contact), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun EmergencyContactRow(contact: EmergencyContactEntity, index: Int, onCall: () -> Unit, onDelete: () -> Unit) {
    ListItem(
        leadingContent = { Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainer), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
        } },
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(contact.name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Spacer(Modifier.width(6.dp))
                Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                        stringResource(R.string.priority_badge, index + 1),
                        fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        },
        supportingContent = { Text(contact.relationship, color = AuthMuted, fontSize = 12.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
        trailingContent = {
            Row {
                IconButton(onClick = onCall) {
                    Icon(Icons.Filled.Call, contentDescription = stringResource(R.string.call), tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.remove), tint = AuthMuted)
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun LocalDeviceVaultSection(info: EmergencyInfo, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(16.dp)) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Filled.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(stringResource(R.string.vault_card_title), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(2.dp))
                    Text(stringResource(R.string.vault_card_body), fontSize = 11.sp, color = AuthMuted, lineHeight = 15.sp)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        val bloodType = info.aboutMe.bloodType.trim()
        val allergies = info.medical.allergies.trim()
        val medicalLine1 = bloodType.takeIf { it.isNotBlank() }?.let { "Blood: $it" }
        val medicalLine2 = allergies.takeIf { it.isNotBlank() }?.let { "Allergies: $it" }
        val vehicleSummary = listOf(info.vehicle.color, info.vehicle.make, info.vehicle.model)
            .filter { it.isNotBlank() }.joinToString(" ").trim()
        val vehiclePlate = info.vehicle.plate.trim()
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            VaultTile(
                Icons.Filled.MedicalServices, stringResource(R.string.vault_medical_id),
                line1 = medicalLine1 ?: stringResource(R.string.vault_not_added),
                line2 = medicalLine2 ?: stringResource(R.string.vault_tap_to_add),
                onClick = onClick, modifier = Modifier.weight(1f)
            )
            VaultTile(
                Icons.Filled.DirectionsCar, stringResource(R.string.vault_vehicle),
                line1 = vehicleSummary.takeIf { it.isNotBlank() } ?: stringResource(R.string.vault_not_added),
                line2 = vehiclePlate.takeIf { it.isNotBlank() }?.let { "Plate: $it" } ?: stringResource(R.string.vault_tap_to_add),
                onClick = onClick, modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun VaultTile(icon: ImageVector, label: String, line1: String, line2: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Icon(icon, contentDescription = null, tint = AuthMuted, modifier = Modifier.size(16.dp))
                Text(label, fontSize = 10.sp, color = AuthMuted, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(10.dp))
            Text(line1, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(line2, fontSize = 11.sp, color = AuthMuted, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun AddContactDialog(onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_contact)) },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.first_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = relationship, onValueChange = { relationship = it }, label = { Text(stringResource(R.string.relationship)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text(stringResource(R.string.phone_number)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && phone.isNotBlank(),
                onClick = { onSave(name.trim(), relationship.trim(), phone.trim()); onDismiss() }
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun SectionLabel(text: String, trailing: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = AuthMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        trailing?.invoke()
    }
}

@Composable
private fun SafetyNetworkRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        leadingContent = { Box(Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainer), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
        } },
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(subtitle, color = AuthMuted, fontSize = 13.sp) },
        trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = AuthMuted) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun EmergencyNumberRow(entry: EmergencyNumber, onCall: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(entry.number, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        },
        supportingContent = { Text(stringResource(entry.labelRes), color = AuthMuted) },
        trailingContent = { Icon(Icons.Filled.Call, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onCall)
    )
}
