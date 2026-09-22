package za.co.kinplus.app.ui.screens.home

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import za.co.kinplus.app.R
import za.co.kinplus.app.auth.AuthManager
import za.co.kinplus.app.data.repository.CircleRepository
import za.co.kinplus.app.data.repository.LocationRepository
import za.co.kinplus.app.data.repository.ZoneRepository
import za.co.kinplus.app.domain.Circle
import za.co.kinplus.app.domain.MemberLocation
import za.co.kinplus.app.location.LocationFix
import za.co.kinplus.app.location.LocationProvider
import za.co.kinplus.app.location.hasAnyLocationPermission
import za.co.kinplus.app.ui.components.KinAppTopBar
import za.co.kinplus.app.ui.components.OfflineBanner
import za.co.kinplus.app.util.ConnectivityObserver
import javax.inject.Inject

/** Roughly the centre of South Africa — the default camera position before any member location is known. */
private val DEFAULT_CAMERA = LatLng(-28.4793, 24.6727)

data class HomeUiState(
    val circles: List<Circle> = emptyList(),
    val selectedCircleId: String? = null,
    val members: List<MemberLocation> = emptyList(),
    val safeZoneCount: Int = 0,
    val myUid: String = "",
    val myDisplayName: String = "",
    val isOffline: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val circleRepository: CircleRepository,
    private val locationRepository: LocationRepository,
    private val locationProvider: LocationProvider,
    private val zoneRepository: ZoneRepository,
    private val authManager: AuthManager,
    connectivity: ConnectivityObserver
) : ViewModel() {

    private val selectedCircleId = MutableStateFlow<String?>(null)

    private data class Snapshot(val circles: List<Circle>, val selected: String?, val online: Boolean, val zoneCount: Int)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<HomeUiState> = combine(
        circleRepository.circles,
        selectedCircleId,
        connectivity.online,
        zoneRepository.zones
    ) { circles, selected, online, zones ->
        Snapshot(circles, selected ?: circles.firstOrNull()?.id, online, zones.size)
    }.flatMapLatest { snapshot ->
        val locationFlow = if (snapshot.selected != null) circleRepository.observeCircleLocations(snapshot.selected)
        else flowOf(emptyList())
        locationFlow.map { members ->
            HomeUiState(
                snapshot.circles, snapshot.selected, members, snapshot.zoneCount,
                authManager.currentUid, authManager.currentDisplayName, isOffline = !snapshot.online
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    init {
        refresh()
        publishOwnLocation()
    }

    fun selectCircle(id: String) { selectedCircleId.value = id }

    fun refresh() = viewModelScope.launch {
        circleRepository.refreshCircles()
        zoneRepository.refreshZones()
        state.value.selectedCircleId?.let { circleRepository.refreshCircleLocations(it) }
    }

    /** Publishes this device's location once so members appear on the map (FR-13). */
    private fun publishOwnLocation() = viewModelScope.launch {
        locationProvider.currentFix()?.let {
            locationRepository.publish(it.lat, it.lng, it.accuracyM, it.batteryPct)
        }
    }

    /** A fresh GPS fix for the map's "locate me" button. */
    suspend fun locateMe() = locationProvider.currentFix()

    /** The device's last cached fix, if any — used to centre the map instantly on open. */
    suspend fun locateFast() = locationProvider.lastKnownFix()
}

@Composable
fun HomeScreen(
    onOpenProfile: () -> Unit,
    onOpenSafePlaces: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val primary = state.circles.firstOrNull { it.id == state.selectedCircleId } ?: state.circles.firstOrNull()

    Column(Modifier.fillMaxSize()) {
        KinAppTopBar(title = stringResource(R.string.map_home_title), onFilter = {}, onProfile = onOpenProfile)
        OfflineBanner(visible = state.isOffline)

        MemberMap(
            members = state.members,
            isOffline = state.isOffline,
            myUid = state.myUid,
            myDisplayName = state.myDisplayName,
            onLocateMe = { viewModel.locateMe() },
            onLocateFast = { viewModel.locateFast() },
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp)
        )

        FamilyCirclePanel(
            circle = primary,
            members = state.members,
            safeZoneCount = state.safeZoneCount,
            onOpenSafePlaces = onOpenSafePlaces,
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        )
    }
}

@Composable
private fun FamilyCirclePanel(circle: Circle?, members: List<MemberLocation>, safeZoneCount: Int, onOpenSafePlaces: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        if (circle != null) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.family_circle_header), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text(circle.memberCount.toString(), color = AuthMutedColor(), fontSize = 15.sp)
                }
                Button(onClick = {}, shape = RoundedCornerShape(50), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)) {
                    Text(stringResource(R.string.action_check_in), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (members.isEmpty()) {
            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.location_sharing_active),
                    color = AuthMutedColor(), fontSize = 13.sp,
                    modifier = Modifier.padding(20.dp)
                )
            }
        } else {
            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column {
                    members.forEachIndexed { index, member ->
                        FamilyMemberRow(member)
                        if (index != members.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InfoTile(
                Icons.Filled.Home, stringResource(R.string.safe_places),
                if (safeZoneCount > 0) stringResource(R.string.safe_places_active, safeZoneCount) else stringResource(R.string.safe_places_none),
                Modifier.weight(1f),
                onClick = onOpenSafePlaces
            )
            val minBattery = members.minOfOrNull { it.batteryPct }
            val devicesLabel = when {
                minBattery == null -> stringResource(R.string.circle_devices_no_devices)
                minBattery < 20 -> stringResource(R.string.circle_devices_low_battery)
                else -> stringResource(R.string.circle_devices_optimal)
            }
            InfoTile(Icons.Filled.Bolt, stringResource(R.string.circle_devices_title), devicesLabel, Modifier.weight(1f), onClick = null)
        }
    }
}

@Composable
private fun FamilyMemberRow(member: MemberLocation) {
    ListItem(
        leadingContent = {
            Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Text(member.displayName.take(1).uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        },
        headlineContent = { Text(member.displayName, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(stringResource(R.string.location_sharing_active), color = AuthMutedColor(), fontSize = 12.sp, maxLines = 1) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.battery_pct, member.batteryPct), color = AuthMutedColor(), fontSize = 13.sp)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = AuthMutedColor())
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun InfoTile(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, sub: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val tileModifier = if (onClick != null) modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick) else modifier
    Surface(modifier = tileModifier, color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(sub, fontSize = 11.sp, color = AuthMutedColor())
        }
    }
}

@Composable
private fun AuthMutedColor() = za.co.kinplus.app.ui.screens.auth.AuthMuted

/** Up to two letters ("Sarah Miller" -> "SM", "Sarah" -> "S") so a person is identifiable on the map at a glance. */
private fun initialsOf(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(1).uppercase()
        else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
    }
}

/** The three map modes exposed to the user — "Explore" and "Driving" are both the standard road map, Driving adds live traffic. */
private enum class HomeMapMode(val labelRes: Int, val mapType: MapType, val traffic: Boolean) {
    EXPLORE(R.string.map_mode_explore, MapType.NORMAL, traffic = false),
    DRIVING(R.string.map_mode_driving, MapType.NORMAL, traffic = true),
    SATELLITE(R.string.map_mode_satellite, MapType.HYBRID, traffic = false)
}

@Composable
private fun MemberMap(
    members: List<MemberLocation>,
    isOffline: Boolean,
    myUid: String,
    myDisplayName: String,
    onLocateMe: suspend () -> LocationFix?,
    onLocateFast: suspend () -> LocationFix?,
    modifier: Modifier
) {
    val context = LocalContext.current
    var hasLocationPermission by remember { mutableStateOf(hasAnyLocationPermission(context)) }
    var mapMode by remember { mutableStateOf(HomeMapMode.EXPLORE) }
    var locating by remember { mutableStateOf(false) }
    // The device's own position, tracked independently of the circle's member
    // list — so a "you" marker always appears even before the backend has
    // published/returned this device's own location as part of the circle.
    var myFix by remember { mutableStateOf<LocationFix?>(null) }
    val scope = rememberCoroutineScope()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(DEFAULT_CAMERA, 5f)
    }

    fun locate() {
        scope.launch {
            locating = true
            val fix = onLocateMe()
            if (fix != null) {
                myFix = fix
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(fix.lat, fix.lng), 15f))
            } else {
                Toast.makeText(context, context.getString(R.string.location_unavailable), Toast.LENGTH_SHORT).show()
            }
            locating = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.any { it }
        hasLocationPermission = granted
        if (granted) locate()
        else Toast.makeText(context, context.getString(R.string.location_permission_needed), Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(Unit) {
        if (hasLocationPermission) {
            // A cached fix (near-instant) centres the map right away; a fresh
            // fix then refines it in the background instead of leaving the map
            // sitting on the wide, slow-to-tile default view while it waits.
            onLocateFast()?.let { fix ->
                myFix = fix
                cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(fix.lat, fix.lng), 15f)
            }
            onLocateMe()?.let { fix ->
                myFix = fix
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(fix.lat, fix.lng), 15f))
            }
        }
    }

    LaunchedEffect(members) {
        when {
            members.size == 1 -> cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(members[0].lat, members[0].lng), 14f)
            )
            members.size > 1 -> {
                val bounds = LatLngBounds.Builder().apply {
                    members.forEach { include(LatLng(it.lat, it.lng)) }
                }.build()
                cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 120))
            }
        }
    }

    val mapProperties = remember(mapMode) {
        MapProperties(
            mapType = mapMode.mapType,
            isTrafficEnabled = mapMode.traffic,
            // Off: every member (including the signed-in user, once they appear
            // in the circle's member list) already gets its own initials marker
            // below, so the plain unlabeled "blue dot" would just be a redundant,
            // unidentifiable second pin for the same position.
            isMyLocationEnabled = false
        )
    }
    val mapUiSettings = remember { MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false) }

    Box(modifier = modifier.clip(RoundedCornerShape(16.dp))) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = mapUiSettings
        ) {
            val markerColor = MaterialTheme.colorScheme.primary.toArgb()
            val selfMarkerColor = MaterialTheme.colorScheme.error.toArgb()
            members.forEach { m ->
                val isMe = m.uid == myUid
                val icon = remember(m.uid, m.displayName, markerColor, isMe) {
                    initialsMarkerIcon(context, initialsOf(m.displayName), if (isMe) selfMarkerColor else markerColor)
                }
                Marker(
                    state = rememberMarkerState(position = LatLng(m.lat, m.lng)),
                    title = if (isMe) m.displayName + context.getString(R.string.you_suffix) else m.displayName,
                    snippet = stringResource(R.string.battery_pct, m.batteryPct),
                    icon = icon
                )
            }

            // Guaranteed "you" marker: shown from this device's own tracked fix
            // whenever the circle's member list hasn't (yet, or ever) surfaced
            // this user's own entry — otherwise no location marker for the
            // signed-in user would appear on the map at all.
            val myFixNow = myFix
            if (myFixNow != null && members.none { it.uid == myUid }) {
                val icon = remember(myDisplayName, selfMarkerColor) {
                    initialsMarkerIcon(context, initialsOf(myDisplayName), selfMarkerColor)
                }
                Marker(
                    state = remember(myFixNow) { MarkerState(position = LatLng(myFixNow.lat, myFixNow.lng)) },
                    title = myDisplayName + stringResource(R.string.you_suffix),
                    icon = icon
                )
            }
        }

        Surface(
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
            color = Color.Black.copy(alpha = 0.6f),
            shape = RoundedCornerShape(50)
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(if (isOffline) AuthMutedColor() else Color(0xFF34C759)))
                Spacer(Modifier.width(6.dp))
                Text(
                    if (isOffline) stringResource(R.string.map_status_offline) else stringResource(R.string.map_status_live),
                    color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium
                )
            }
        }

        Row(
            Modifier.align(Alignment.BottomStart).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MapModeSwitcher(selected = mapMode, onSelect = { mapMode = it })
        }

        LocateMeButton(
            loading = locating,
            onClick = {
                if (hasLocationPermission) locate()
                else permissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                )
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)
        )
    }
}

/**
 * Draws a small circular map-pin bitmap with a person's initials — so every
 * member marker on the map is identifiable at a glance instead of a plain
 * generic pin.
 */
private fun initialsMarkerIcon(context: Context, initials: String, backgroundColor: Int): BitmapDescriptor {
    val density = context.resources.displayMetrics.density
    val sizePx = (40 * density).toInt()
    val strokeWidthPx = 3f * density
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = sizePx / 2f
    val radius = center - strokeWidthPx / 2f

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = backgroundColor; style = Paint.Style.FILL }
    canvas.drawCircle(center, center, radius, fillPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
    }
    canvas.drawCircle(center, center, radius, borderPaint)

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 14f * density
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val textY = center - (textPaint.descent() + textPaint.ascent()) / 2f
    canvas.drawText(initials, center, textY, textPaint)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

/** A dedicated "go to my current location" toggle — recentres the map on a fresh GPS fix, requesting permission first if needed. */
@Composable
private fun LocateMeButton(loading: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(enabled = !loading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
        } else {
            Icon(Icons.Filled.MyLocation, contentDescription = stringResource(R.string.locate_me), tint = Color.White)
        }
    }
}

@Composable
private fun MapModeSwitcher(selected: HomeMapMode, onSelect: (HomeMapMode) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.65f),
        shape = RoundedCornerShape(50)
    ) {
        Row {
            HomeMapMode.entries.forEach { mode ->
                val selectedNow = mode == selected
                Text(
                    stringResource(mode.labelRes),
                    color = if (selectedNow) Color.White else Color.White.copy(alpha = 0.6f),
                    fontWeight = if (selectedNow) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clickable { onSelect(mode) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}
