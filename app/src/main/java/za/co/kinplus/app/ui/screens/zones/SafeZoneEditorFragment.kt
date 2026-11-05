package za.co.kinplus.app.ui.screens.zones

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import za.co.kinplus.app.R
import za.co.kinplus.app.databinding.FragmentSafeZoneEditorBinding
import za.co.kinplus.app.domain.Circle
import za.co.kinplus.app.ui.common.DistanceFormat
import za.co.kinplus.app.ui.common.collectWhileStarted
import za.co.kinplus.app.ui.common.popBackSafely
import za.co.kinplus.app.ui.common.setTextIfDifferent
import za.co.kinplus.app.ui.common.setTextOrGone
import za.co.kinplus.app.ui.common.setup

/**
 * Safe Zone editor (screen 9). Creates a zone centred on the device's current
 * position with a radius of 100 to 5000 m (FR-20) and enter or exit alerts,
 * sent to the circle picked here (the primary circle by default). Opened from
 * the Safe Zones list it edits that zone instead.
 */
@AndroidEntryPoint
class SafeZoneEditorFragment : Fragment() {

    private var _binding: FragmentSafeZoneEditorBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SafeZoneEditorViewModel by viewModels()
    private var shownCircles: List<Circle> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSafeZoneEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val editing = viewModel.state.value.editing
        binding.topBar.setup(
            title = getString(if (editing) R.string.edit_safe_zone else R.string.create_safe_zone),
            onBack = { popBackSafely() }
        )
        shownCircles = emptyList()

        binding.nameInput.doAfterTextChanged { viewModel.onName(it?.toString().orEmpty()) }
        binding.radiusSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) viewModel.onRadius(value.toInt())
        }
        binding.notifyEnterSwitch.setOnCheckedChangeListener { _, checked -> viewModel.onNotifyEnter(checked) }
        binding.notifyExitSwitch.setOnCheckedChangeListener { _, checked -> viewModel.onNotifyExit(checked) }
        binding.saveButton.setOnClickListener { viewModel.save { popBackSafely() } }

        collectWhileStarted(viewModel.state) { state ->
            binding.nameInput.setTextIfDifferent(state.name)
            // The slider moves in 50 m steps from 100 m, so snap the value onto a step.
            val radius = (100 + ((state.radiusM.coerceIn(100, 5000) - 100) / 50) * 50).toFloat()
            if (binding.radiusSlider.value != radius) binding.radiusSlider.value = radius
            binding.radiusValue.text = DistanceFormat.radius(requireContext(), state.radiusM, state.useMetric)
            if (binding.notifyEnterSwitch.isChecked != state.notifyEnter) binding.notifyEnterSwitch.isChecked = state.notifyEnter
            if (binding.notifyExitSwitch.isChecked != state.notifyExit) binding.notifyExitSwitch.isChecked = state.notifyExit
            renderCircles(state)
            binding.errorText.setTextOrGone(
                when (state.error) {
                    null -> null
                    "NAME" -> getString(R.string.error_zone_name_required)
                    "LOCATION" -> getString(R.string.location_unavailable)
                    else -> getString(R.string.error_generic)
                }
            )
            binding.saveButton.isEnabled = !state.saving
            binding.progress.isVisible = state.saving
        }
    }

    /** Up to four circles as single choice chips (as on Safe Journey); rebuilt only when the list changes. */
    private fun renderCircles(state: ZoneEditorUiState) {
        val circles = state.circles.take(4)
        binding.circleGroup.isVisible = circles.isNotEmpty()
        if (circles != shownCircles) {
            shownCircles = circles
            binding.circleChips.removeAllViews()
            circles.forEach { circle ->
                val chip = layoutInflater.inflate(R.layout.item_choice_chip, binding.circleChips, false) as Chip
                chip.id = View.generateViewId()
                chip.text = circle.name
                chip.tag = circle.id
                chip.setOnClickListener { viewModel.selectCircle(circle.id) }
                binding.circleChips.addView(chip)
            }
        }
        for (i in 0 until binding.circleChips.childCount) {
            val chip = binding.circleChips.getChildAt(i) as Chip
            chip.isChecked = chip.tag == state.selectedCircleId
            // The API cannot move an existing zone to another circle, so the choice is fixed when editing.
            chip.isEnabled = !state.editing
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
