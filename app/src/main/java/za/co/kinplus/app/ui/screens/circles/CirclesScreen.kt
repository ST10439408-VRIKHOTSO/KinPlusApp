package za.co.kinplus.app.ui.screens.circles

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.Tune
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import za.co.kinplus.app.R
import za.co.kinplus.app.auth.AuthManager
import za.co.kinplus.app.data.repository.CircleRepository
import za.co.kinplus.app.data.repository.LocalPhotoRepository
import za.co.kinplus.app.data.repository.ZoneRepository
import za.co.kinplus.app.domain.Circle
import za.co.kinplus.app.domain.MemberLocation
import za.co.kinplus.app.ui.components.AvatarImage
import za.co.kinplus.app.ui.components.KinAppTopBar
import za.co.kinplus.app.ui.components.KinPrimaryButton
import za.co.kinplus.app.ui.components.OfflineBanner
import za.co.kinplus.app.ui.screens.auth.AuthDivider
import za.co.kinplus.app.ui.screens.auth.AuthMuted
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

@Composable
fun CirclesScreen(
    onOpenCircle: (String) -> Unit,
    onCreateJoin: () -> Unit,
    onOpenSafePlaces: () -> Unit,
    onOpenProfile: () -> Unit,
    viewModel: CirclesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var deleteTarget by remember { mutableStateOf<Circle?>(null) }
    val primary = state.circles.firstOrNull()
    val otherCircles = state.circles.drop(1)

    LaunchedEffect(state.deleteError) {
        state.deleteError?.let { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
            viewModel.deleteErrorShown()
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        KinAppTopBar(
            title = stringResource(R.string.circles_title),
            onFilter = {},
            onProfile = onOpenProfile
        )
        OfflineBanner(visible = state.isOffline)

        CirclesSegmentedControl(
            selected = 0,
            onSelectSafeZones = onOpenSafePlaces,
            safeZoneCount = state.safeZoneCount,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(16.dp))

        if (primary != null) {
            PrimaryCircleCard(
                circle = primary,
                isAdmin = primary.ownerUid == state.myUid,
                onCopyCode = {
                    val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                    clipboard?.setPrimaryClip(ClipData.newPlainText("Kin+ join code", primary.joinCode))
                    android.widget.Toast.makeText(context, context.getString(R.string.code_copied), android.widget.Toast.LENGTH_SHORT).show()
                },
                onAddMember = onCreateJoin,
                onManagePermissions = { onOpenCircle(primary.id) },
                onClick = { onOpenCircle(primary.id) },
                onDelete = { deleteTarget = primary },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            CirclesSectionLabel(stringResource(R.string.section_live_member_status)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.label_mesh_live), color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Column(Modifier.padding(horizontal = 16.dp)) {
                Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
                    Column {
                        if (state.primaryMembers.isEmpty()) {
                            Text(
                                stringResource(R.string.location_sharing_active),
                                color = AuthMuted, fontSize = 13.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        } else {
                            state.primaryMembers.forEachIndexed { index, member ->
                                LiveMemberRow(member, isMe = member.uid == state.myUid)
                                if (index != state.primaryMembers.lastIndex) HorizontalDivider(color = AuthDivider)
                            }
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.empty_circles),
                    style = MaterialTheme.typography.bodyLarge,
                    color = AuthMuted
                )
                Spacer(Modifier.height(20.dp))
                KinPrimaryButton(
                    text = stringResource(R.string.create_or_join_circle),
                    onClick = onCreateJoin
                )
            }
        }

        if (otherCircles.isNotEmpty()) {
            CirclesSectionLabel(stringResource(R.string.section_other_connected_circles))
            Column(Modifier.padding(horizontal = 16.dp)) {
                Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
                    Column {
                        otherCircles.forEachIndexed { index, circle ->
                            OtherCircleRow(circle, onClick = { onOpenCircle(circle.id) }, onDelete = { deleteTarget = circle })
                            if (index != otherCircles.lastIndex) HorizontalDivider(color = AuthDivider)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        OutlinedButton(
            onClick = onCreateJoin,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.create_new_circle_cta), fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(24.dp))
    }

    deleteTarget?.let { circle ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.delete_circle_confirm_title, circle.name)) },
            text = { Text(stringResource(R.string.delete_circle_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCircle(circle.id)
                    deleteTarget = null
                }) { Text(stringResource(R.string.delete_circle), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
private fun CirclesSegmentedControl(selected: Int, onSelectSafeZones: () -> Unit, safeZoneCount: Int, modifier: Modifier = Modifier) {
    val labels = listOf(stringResource(R.string.my_circles), stringResource(R.string.circles_segment_safe_zones, safeZoneCount))
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(Modifier.padding(4.dp)) {
            labels.forEachIndexed { index, label ->
                val isSelected = index == selected
                Surface(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(11.dp)).clickable { if (index == 1) onSelectSafeZones() },
                    color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent
                ) {
                    Text(
                        label,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else AuthMuted,
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PrimaryCircleCard(
    circle: Circle,
    isAdmin: Boolean,
    onCopyCode: () -> Unit,
    onAddMember: () -> Unit,
    onManagePermissions: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(circle.name, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (isAdmin) {
                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Text(
                            stringResource(R.string.badge_admin),
                            fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                }
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.MoreVert, contentDescription = null, tint = AuthMuted, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete_circle), color = MaterialTheme.colorScheme.error) },
                            onClick = { showMenu = false; onDelete() }
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.circle_status_members, circle.memberCount)
                    + (circle.description?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
                color = AuthMuted, fontSize = 13.sp
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircleQuickAction(
                    Icons.Filled.Key, circle.joinCode, stringResource(R.string.action_join_code_hint),
                    modifier = Modifier.weight(1f), onClick = onCopyCode
                )
                CircleQuickAction(
                    Icons.Filled.PersonAddAlt1, stringResource(R.string.action_add_member), stringResource(R.string.action_add_member_sub),
                    modifier = Modifier.weight(1f), onClick = onAddMember
                )
                CircleQuickAction(
                    Icons.Filled.Tune, stringResource(R.string.action_manage_permissions), stringResource(R.string.action_manage_permissions_sub),
                    modifier = Modifier.weight(1f), onClick = onManagePermissions
                )
            }
        }
    }
}

@Composable
private fun CircleQuickAction(icon: ImageVector, title: String, subtitle: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(6.dp))
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text(subtitle, fontSize = 10.sp, color = AuthMuted, maxLines = 1, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun CirclesSectionLabel(text: String, trailing: (@Composable () -> Unit)? = null) {
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
private fun LiveMemberRow(member: MemberLocation, isMe: Boolean) {
    ListItem(
        leadingContent = { AvatarImage(photoPath = null, fallbackText = member.displayName, size = 44.dp) },
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(member.displayName + if (isMe) stringResource(R.string.you_suffix) else "", fontWeight = FontWeight.Medium)
                if (isMe) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                }
            }
        },
        supportingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.BatteryStd, contentDescription = null, tint = AuthMuted, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(3.dp))
                Text(stringResource(R.string.battery_pct, member.batteryPct), color = AuthMuted, fontSize = 13.sp)
            }
        },
        trailingContent = {
            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
                Text(
                    stringResource(R.string.status_sharing),
                    fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun OtherCircleRow(circle: Circle, onClick: () -> Unit, onDelete: () -> Unit) {
    ListItem(
        leadingContent = {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
            }
        },
        headlineContent = { Text(circle.name, fontWeight = FontWeight.Medium) },
        supportingContent = {
            Text(
                stringResource(R.string.members_count, circle.memberCount)
                    + (circle.description?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
                color = AuthMuted, fontSize = 13.sp
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = stringResource(R.string.delete_circle), tint = AuthMuted, modifier = Modifier.size(18.dp))
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = AuthMuted)
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick)
    )
}
