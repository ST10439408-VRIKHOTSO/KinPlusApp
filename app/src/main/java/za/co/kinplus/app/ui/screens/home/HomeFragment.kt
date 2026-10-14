package za.co.kinplus.app.ui.screens.home

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import za.co.kinplus.app.R
import za.co.kinplus.app.databinding.FragmentHomeBinding
import za.co.kinplus.app.databinding.ItemFamilyMemberBinding
import za.co.kinplus.app.domain.MemberLocation
import za.co.kinplus.app.location.LocationFix
import za.co.kinplus.app.location.hasAnyLocationPermission
import za.co.kinplus.app.ui.common.addDividerIfNeeded
import za.co.kinplus.app.ui.common.collectWhileStarted
import za.co.kinplus.app.ui.common.initialsOf
import za.co.kinplus.app.ui.common.navigateSafely
import za.co.kinplus.app.ui.common.setup
import za.co.kinplus.app.ui.common.toast

/** Roughly the centre of South Africa: the default camera position before any member location is known. */
private val DEFAULT_CAMERA = LatLng(-28.4793, 24.6727)

/** The three map modes. "Explore" and "Driving" are both the road map; Driving adds live traffic. */
private enum class HomeMapMode(val mapType: Int, val traffic: Boolean) {
    EXPLORE(GoogleMap.MAP_TYPE_NORMAL, traffic = false),
    DRIVING(GoogleMap.MAP_TYPE_NORMAL, traffic = true),
    SATELLITE(GoogleMap.MAP_TYPE_HYBRID, traffic = false)
}

/**
 * Home / Family Map (screen 4). Shows the circle's members on a Google Map
 * with initials markers (FR-13), a live status pill, map modes, a locate me
 * button, and a panel listing each member's battery.
 */
@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()

    private var map: GoogleMap? = null
    private var mapMode = HomeMapMode.EXPLORE
    private var myFix: LocationFix? = null
    private var lastFittedMembers: List<MemberLocation>? = null
    private var latestState = HomeUiState()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.any { it }) locate()
        else requireContext().toast(getString(R.string.location_permission_needed))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.topBar.setup(
            title = getString(R.string.map_home_title),
            onFilter = {},
            onProfile = { navigateSafely(R.id.profileFragment) }
        )
        binding.safePlacesTile.setOnClickListener { navigateSafely(R.id.safeZonesFragment) }
        binding.checkInButton.setOnClickListener { /* reserved for a quick check in */ }

        binding.modeExplore.setOnClickListener { setMapMode(HomeMapMode.EXPLORE) }
        binding.modeDriving.setOnClickListener { setMapMode(HomeMapMode.DRIVING) }
        binding.modeSatellite.setOnClickListener { setMapMode(HomeMapMode.SATELLITE) }
        renderMapModes()

        binding.locateMe.setOnClickListener {
            if (hasAnyLocationPermission(requireContext())) locate()
            else permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }

        val mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync { googleMap -> onMapReady(googleMap) }

        collectWhileStarted(viewModel.state) { state ->
            latestState = state
            renderPanel(state)
            renderMarkers()
            fitCameraToMembers(state.members)
        }
    }

    private fun onMapReady(googleMap: GoogleMap) {
        if (_binding == null) return
        map = googleMap
        googleMap.uiSettings.isZoomControlsEnabled = false
        googleMap.uiSettings.isMyLocationButtonEnabled = false
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_CAMERA, 5f))
        applyMapMode()
        renderMarkers()

        if (hasAnyLocationPermission(requireContext())) {
            viewLifecycleOwner.lifecycleScope.launch {
                // A cached fix (near instant) centres the map right away; a fresh
                // fix then refines it, instead of sitting on the wide default view.
                viewModel.locateFast()?.let { fix ->
                    myFix = fix
                    map?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(fix.lat, fix.lng), 15f))
                    renderMarkers()
                }
                viewModel.locateMe()?.let { fix ->
                    myFix = fix
                    map?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(fix.lat, fix.lng), 15f))
                    renderMarkers()
                }
            }
        }
    }

    /** Recentres the map on a fresh GPS fix. */
    private fun locate() {
        viewLifecycleOwner.lifecycleScope.launch {
            setLocating(true)
            val fix = viewModel.locateMe()
            if (fix != null) {
                myFix = fix
                map?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(fix.lat, fix.lng), 15f))
                renderMarkers()
            } else {
                context?.toast(getString(R.string.location_unavailable))
            }
            setLocating(false)
        }
    }

    private fun setLocating(locating: Boolean) {
        val b = _binding ?: return
        b.locateMe.isEnabled = !locating
        b.locateIcon.isVisible = !locating
        b.locateProgress.isVisible = locating
    }

    private fun setMapMode(mode: HomeMapMode) {
        mapMode = mode
        renderMapModes()
        applyMapMode()
    }

    private fun applyMapMode() {
        val googleMap = map ?: return
        googleMap.mapType = mapMode.mapType
        googleMap.isTrafficEnabled = mapMode.traffic
    }

    /** The selected mode is a forest pill with white text; the others are muted. */
    private fun renderMapModes() {
        val context = requireContext()
        val pairs = listOf(
            binding.modeExplore to HomeMapMode.EXPLORE,
            binding.modeDriving to HomeMapMode.DRIVING,
            binding.modeSatellite to HomeMapMode.SATELLITE
        )
        pairs.forEach { (view: TextView, mode) ->
            val selected = mode == mapMode
            if (selected) {
                view.setBackgroundResource(R.drawable.bg_pill)
                view.backgroundTintList = ContextCompat.getColorStateList(context, R.color.kin_forest)
            } else {
                view.background = null
            }
            view.setTextColor(ContextCompat.getColor(context, if (selected) R.color.white else R.color.kin_muted))
        }
    }

    private fun renderPanel(state: HomeUiState) {
        val context = requireContext()
        binding.offlineBanner.root.isVisible = state.isOffline
        binding.liveDot.backgroundTintList = ContextCompat.getColorStateList(
            context, if (state.isOffline) R.color.kin_muted else R.color.kin_green_bright
        )
        binding.liveLabel.setText(if (state.isOffline) R.string.map_status_offline else R.string.map_status_live)

        val primary = state.circles.firstOrNull { it.id == state.selectedCircleId } ?: state.circles.firstOrNull()
        binding.circleHeader.isVisible = primary != null
        binding.memberCount.text = primary?.memberCount?.toString().orEmpty()

        binding.membersEmpty.isVisible = state.members.isEmpty()
        binding.membersContainer.removeAllViews()
        state.members.forEach { member ->
            binding.membersContainer.addDividerIfNeeded()
            val row = ItemFamilyMemberBinding.inflate(layoutInflater, binding.membersContainer, true)
            row.initials.text = initialsOf(member.displayName)
            row.name.text = member.displayName
            row.battery.text = getString(R.string.battery_pct, member.batteryPct)
            row.battery.setTextColor(
                ContextCompat.getColor(context, if (member.batteryPct < 20) R.color.kin_red else R.color.kin_muted)
            )
        }

        binding.safePlacesSub.text = if (state.safeZoneCount > 0) {
            getString(R.string.safe_places_active, state.safeZoneCount)
        } else {
            getString(R.string.safe_places_none)
        }
        val minBattery = state.members.minOfOrNull { it.batteryPct }
        binding.devicesSub.text = when {
            minBattery == null -> getString(R.string.circle_devices_no_devices)
            minBattery < 20 -> getString(R.string.circle_devices_low_battery)
            else -> getString(R.string.circle_devices_optimal)
        }
    }

    /**
     * Draws one initials marker per member, plus a guaranteed "you" marker
     * from this device's own fix when the circle has not returned this user's
     * own location yet.
     */
    private fun renderMarkers() {
        val googleMap = map ?: return
        val context = context ?: return
        val state = latestState
        val memberColor = ContextCompat.getColor(context, R.color.kin_green)
        val selfColor = ContextCompat.getColor(context, R.color.kin_forest)
        val you = getString(R.string.you_suffix)

        googleMap.clear()
        state.members.forEach { m ->
            val isMe = m.uid == state.myUid
            googleMap.addMarker(
                MarkerOptions()
                    .position(LatLng(m.lat, m.lng))
                    .title(if (isMe) m.displayName + you else m.displayName)
                    .snippet(getString(R.string.battery_pct, m.batteryPct))
                    .icon(initialsMarkerIcon(context, initialsOf(m.displayName), if (isMe) selfColor else memberColor))
            )
        }
        val fix = myFix
        if (fix != null && state.members.none { it.uid == state.myUid }) {
            googleMap.addMarker(
                MarkerOptions()
                    .position(LatLng(fix.lat, fix.lng))
                    .title(state.myDisplayName + you)
                    .icon(initialsMarkerIcon(context, initialsOf(state.myDisplayName), selfColor))
            )
        }
    }

    /** Frames every member on the map whenever the member list changes. */
    private fun fitCameraToMembers(members: List<MemberLocation>) {
        val googleMap = map ?: return
        if (members == lastFittedMembers) return
        lastFittedMembers = members
        when {
            members.size == 1 -> googleMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(members[0].lat, members[0].lng), 14f)
            )
            members.size > 1 -> {
                val bounds = LatLngBounds.Builder().apply {
                    members.forEach { include(LatLng(it.lat, it.lng)) }
                }.build()
                runCatching { googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120)) }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        map = null
        lastFittedMembers = null
        _binding = null
    }
}

/**
 * Draws a small circular map pin with a person's initials, so every member
 * marker is identifiable at a glance instead of a plain generic pin.
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
