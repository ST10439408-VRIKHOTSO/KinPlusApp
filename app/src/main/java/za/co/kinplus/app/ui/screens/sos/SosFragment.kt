package za.co.kinplus.app.ui.screens.sos

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import za.co.kinplus.app.R
import za.co.kinplus.app.databinding.FragmentSosBinding
import za.co.kinplus.app.databinding.ItemIncidentTypeBinding
import za.co.kinplus.app.databinding.ItemResponderChipBinding
import za.co.kinplus.app.databinding.ViewSafetyToolBinding
import za.co.kinplus.app.databinding.ViewSosSectionHeaderBinding
import za.co.kinplus.app.location.LocationFix
import za.co.kinplus.app.ui.common.collectWhileStarted
import za.co.kinplus.app.ui.common.navigateSafely
import za.co.kinplus.app.ui.common.popBackSafely
import za.co.kinplus.app.ui.common.setTextOrGone
import za.co.kinplus.app.ui.common.setup
import za.co.kinplus.app.ui.common.toast
import za.co.kinplus.app.ui.components.FakeCallController
import za.co.kinplus.app.ui.components.SirenStrobeController
import za.co.kinplus.app.ui.components.showFakeCallSetupDialog

private data class IncidentType(val titleRes: Int, val subtitleRes: Int, val icon: Int)

private val incidentTypes = listOf(
    IncidentType(R.string.incident_medical, R.string.incident_medical_sub, R.drawable.ic_medical_services),
    IncidentType(R.string.incident_collision, R.string.incident_collision_sub, R.drawable.ic_car_crash),
    IncidentType(R.string.incident_followed, R.string.incident_followed_sub, R.drawable.ic_directions_walk),
    IncidentType(R.string.incident_silent, R.string.incident_silent_sub, R.drawable.ic_volume_off)
)

/**
 * Emergency SOS (screen 12). Holding the red button for three seconds raises
 * an SOS to the selected circles with the current location (FR-16 to FR-19).
 * It works offline: the alert is queued and sent on reconnect. Also offers
 * the incident type, direct dialling, the siren with strobe and a fake call.
 */
@AndroidEntryPoint
class SosFragment : Fragment() {

    private var _binding: FragmentSosBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SosViewModel by viewModels()

    private var selectedIncident = 0
    private val incidentViews = mutableListOf<ItemIncidentTypeBinding>()

    private var holdAnimator: ValueAnimator? = null
    private var pulseAnimator: ValueAnimator? = null
    private var pressing = false
    private var closing = false

    private var siren: SirenStrobeController? = null
    private var sirenOn = false
    private val fakeCall = FakeCallController(this) { active -> _binding?.let { renderFakeCallTile(it.fakeCallTool, active) } }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        siren = SirenStrobeController(requireContext().applicationContext)
        closing = false

        binding.topBar.setup(
            title = getString(R.string.emergency_sos),
            onBack = { popBackSafely() },
            onProfile = { navigateSafely(R.id.profileFragment) }
        )

        val holdSeconds = SosViewModel.HOLD_SECONDS
        binding.holdLabel.text = getString(R.string.sos_hold_seconds_label, holdSeconds)
        binding.holdInstructions.text = getString(R.string.sos_hold_instructions, holdSeconds)
        binding.protocolText.text = getString(R.string.emergency_protocol_title) + " " + getString(R.string.emergency_protocol_body)

        setupHeader(binding.incidentHeader, getString(R.string.incident_nature_title), getString(R.string.incident_nature_sub))
        setupHeader(binding.interventionsHeader, getString(R.string.direct_interventions_title), null)
        setupHeader(binding.toolsHeader, getString(R.string.safety_tools_title), null)

        setupHoldButton()
        startPulse()
        setupIncidentGrid()
        setupInterventions()
        setupTools()

        // Opened from the Safety hub with a category: pre fill the message once.
        val initialCategory = arguments?.getString("category")
        if (savedInstanceState == null && !initialCategory.isNullOrBlank() && viewModel.state.value.message.isBlank()) {
            viewModel.onMessage(initialCategory)
        }

        collectWhileStarted(viewModel.state) { state -> render(state) }
    }

    private fun setupHeader(header: ViewSosSectionHeaderBinding, title: String, trailing: String?) {
        header.title.text = title
        header.trailing.setTextOrGone(trailing)
    }

    // ----- Hold to send -----

    /**
     * Press and hold: a progress ring fills over three seconds and the SOS is
     * raised only if the finger is still down when it completes. Letting go
     * early cancels it.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setupHoldButton() {
        binding.sosButton.setOnTouchListener { button, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    button.isPressed = true
                    startHold()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    button.isPressed = false
                    cancelHold()
                    if (event.actionMasked == MotionEvent.ACTION_UP) button.performClick()
                    true
                }
                else -> true
            }
        }
    }

    private fun startHold() {
        pressing = true
        binding.pulseOuter.isVisible = false
        binding.pulseInner.isVisible = false
        binding.holdProgress.isVisible = true
        holdAnimator?.cancel()
        holdAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = SosViewModel.HOLD_SECONDS * 1000L
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float
                _binding?.let {
                    it.holdProgress.setProgressCompat((progress * 100).toInt(), false)
                    it.sosButton.alpha = 0.7f + 0.3f * progress
                }
            }
            addListener(object : AnimatorListenerAdapter() {
                private var cancelled = false
                override fun onAnimationCancel(animation: Animator) { cancelled = true }
                override fun onAnimationEnd(animation: Animator) {
                    if (!cancelled && pressing) viewModel.raiseSos()
                }
            })
            start()
        }
    }

    private fun cancelHold() {
        pressing = false
        holdAnimator?.cancel()
        holdAnimator = null
        val b = _binding ?: return
        b.holdProgress.setProgressCompat(0, false)
        b.holdProgress.isVisible = false
        b.sosButton.alpha = 0.85f
        b.pulseOuter.isVisible = true
        b.pulseInner.isVisible = true
    }

    /** A soft red pulse behind the button while it is not being held. */
    private fun startPulse() {
        pulseAnimator?.cancel()
        pulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1800
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                val pulse = animator.animatedValue as Float
                _binding?.let {
                    val outer = 0.72f + 0.28f * pulse
                    it.pulseOuter.scaleX = outer
                    it.pulseOuter.scaleY = outer
                    it.pulseOuter.alpha = 1f - pulse
                    val inner = 0.8f + 0.2f * pulse
                    it.pulseInner.scaleX = inner
                    it.pulseInner.scaleY = inner
                    it.pulseInner.alpha = 1f - pulse
                }
            }
            start()
        }
    }

    // ----- Incident type -----

    private fun setupIncidentGrid() {
        binding.incidentGrid.removeAllViews()
        incidentViews.clear()
        incidentTypes.chunked(2).forEachIndexed { rowIndex, rowTypes ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                isBaselineAligned = false
            }
            val rowParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            if (rowIndex > 0) rowParams.topMargin = (10 * resources.displayMetrics.density).toInt()
            binding.incidentGrid.addView(row, rowParams)

            rowTypes.forEachIndexed { colIndex, type ->
                val index = rowIndex * 2 + colIndex
                val tile = ItemIncidentTypeBinding.inflate(layoutInflater, row, false)
                val params = tile.root.layoutParams as LinearLayout.LayoutParams
                if (colIndex == 1) params.marginStart = (10 * resources.displayMetrics.density).toInt()
                row.addView(tile.root, params)
                tile.icon.setImageResource(type.icon)
                tile.title.setText(type.titleRes)
                tile.subtitle.setText(type.subtitleRes)
                tile.root.setOnClickListener {
                    selectedIncident = index
                    viewModel.onMessage(getString(type.titleRes))
                    renderIncidents()
                }
                incidentViews.add(tile)
            }
        }
        renderIncidents()
    }

    private fun renderIncidents() {
        incidentViews.forEachIndexed { index, tile ->
            val selected = index == selectedIncident
            tile.root.setBackgroundResource(if (selected) R.drawable.bg_incident_selected else R.drawable.bg_incident_default)
            tile.check.isVisible = selected
        }
    }

    // ----- Direct interventions -----

    private fun setupInterventions() {
        val context = requireContext()
        val red = ContextCompat.getColorStateList(context, R.color.kin_red)
        val white = ContextCompat.getColorStateList(context, R.color.white)

        binding.emergencyServices.apply {
            icon.setImageResource(R.drawable.ic_call)
            icon.setBackgroundResource(R.drawable.bg_icon_badge_danger)
            icon.imageTintList = red
            title.setText(R.string.intervention_emergency_services)
            subtitle.setText(R.string.intervention_emergency_services_sub)
            action.setText(R.string.intervention_dial)
            action.setIconResource(R.drawable.ic_call)
            action.backgroundTintList = red
            action.setTextColor(white)
            action.iconTint = white
            action.setOnClickListener { startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))) }
        }

        val ink = ContextCompat.getColorStateList(context, R.color.kin_ink)
        binding.notifyFamily.apply {
            icon.setImageResource(R.drawable.ic_person_add_alt)
            title.setText(R.string.intervention_notify_family)
            subtitle.setText(R.string.intervention_notify_family_sub)
            action.setText(R.string.intervention_alert)
            action.setIconResource(R.drawable.ic_campaign)
            action.backgroundTintList = ContextCompat.getColorStateList(context, R.color.kin_stone_inset)
            action.setTextColor(ink)
            action.iconTint = ink
            action.setOnClickListener { viewModel.raiseSos() }
        }
    }

    // ----- Safety tools -----

    private fun setupTools() {
        binding.sirenTool.icon.setImageResource(R.drawable.ic_campaign)
        binding.sirenTool.title.setText(R.string.siren_title)
        binding.sirenTool.root.setOnClickListener {
            sirenOn = !sirenOn
            if (sirenOn) siren?.start() else siren?.stop()
            renderSirenTile()
        }
        renderSirenTile()

        binding.fakeCallTool.icon.setImageResource(R.drawable.ic_phone_callback)
        binding.fakeCallTool.title.setText(R.string.fake_call_title)
        binding.fakeCallTool.subtitle.setText(R.string.fake_call_sub)
        binding.fakeCallTool.root.setOnClickListener {
            val defaultCaller = viewModel.state.value.responders.firstOrNull()?.name
                ?: getString(R.string.fake_call_default_caller)
            showFakeCallSetupDialog(requireContext(), defaultCaller) { caller, delaySeconds ->
                fakeCall.start(caller, delaySeconds)
                if (delaySeconds > 0) {
                    context?.toast(getString(R.string.fake_call_scheduled, delaySeconds), long = true)
                }
            }
        }
        renderFakeCallTile(binding.fakeCallTool, fakeCall.isActive)
    }

    private fun renderSirenTile() {
        binding.sirenTool.subtitle.setText(if (sirenOn) R.string.siren_sub_on else R.string.siren_sub_off)
        renderToolActive(binding.sirenTool, sirenOn)
    }

    private fun renderFakeCallTile(tool: ViewSafetyToolBinding, active: Boolean) = renderToolActive(tool, active)

    /** An active tool tile turns solid red with white text and icon. */
    private fun renderToolActive(tool: ViewSafetyToolBinding, active: Boolean) {
        val context = tool.root.context
        tool.root.setCardBackgroundColor(ContextCompat.getColor(context, if (active) R.color.kin_red else R.color.kin_surface))
        tool.root.strokeWidth = if (active) 0 else (1 * context.resources.displayMetrics.density).toInt()
        tool.icon.backgroundTintList = if (active) ContextCompat.getColorStateList(context, R.color.white_18) else null
        tool.icon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, if (active) R.color.white else R.color.kin_red))
        tool.title.setTextColor(ContextCompat.getColor(context, if (active) R.color.white else R.color.kin_ink))
        tool.subtitle.setTextColor(ContextCompat.getColor(context, if (active) R.color.white_85 else R.color.kin_muted))
    }

    // ----- State -----

    private fun render(state: SosUiState) {
        binding.signalBody.text = getString(R.string.signal_lock_body, formatCoords(state.fix))

        binding.sendingProgress.isVisible = state.sending
        binding.sentText.isVisible = state.sent
        binding.queuedText.isVisible = state.sentQueued
        binding.errorText.setTextOrGone(state.error)

        setupHeader(
            binding.respondersHeader,
            getString(R.string.designated_responders_title, state.responders.size),
            getString(R.string.high_priority_reach)
        )
        binding.respondersContainer.removeAllViews()
        state.responders.take(6).forEach { responder ->
            val chip = ItemResponderChipBinding.inflate(layoutInflater, binding.respondersContainer, true)
            chip.name.text = responder.name
        }

        // Once the alert is sent (or queued offline), show the result briefly then close.
        if ((state.sent || state.sentQueued) && !closing) {
            closing = true
            viewLifecycleOwner.lifecycleScope.launch {
                delay(1500)
                popBackSafely()
            }
        }
    }

    /** Coordinates with the correct hemisphere, e.g. "25.7545° S, 28.2314° E" in Pretoria. */
    private fun formatCoords(fix: LocationFix?): String {
        if (fix == null) return "…"
        val ns = if (fix.lat < 0) "S" else "N"
        val ew = if (fix.lng < 0) "W" else "E"
        return String.format(java.util.Locale.US, "%.4f° %s, %.4f° %s", kotlin.math.abs(fix.lat), ns, kotlin.math.abs(fix.lng), ew)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        holdAnimator?.cancel()
        holdAnimator = null
        pulseAnimator?.cancel()
        pulseAnimator = null
        // Leaving the screen always stops the siren and any pending fake call.
        siren?.stop()
        siren = null
        sirenOn = false
        fakeCall.cancel()
        incidentViews.clear()
        _binding = null
    }
}
