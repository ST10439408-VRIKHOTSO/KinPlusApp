package za.co.kinplus.app.ui.screens.journey

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import za.co.kinplus.app.R
import za.co.kinplus.app.databinding.FragmentJourneySetupBinding
import za.co.kinplus.app.databinding.ItemWatcherBinding
import za.co.kinplus.app.domain.Circle
import za.co.kinplus.app.ui.common.addDividerIfNeeded
import za.co.kinplus.app.ui.common.collectWhileStarted
import za.co.kinplus.app.ui.common.navigateSafely
import za.co.kinplus.app.ui.common.popBackSafely
import za.co.kinplus.app.ui.common.setTextIfDifferent
import za.co.kinplus.app.ui.common.setTextOrGone
import za.co.kinplus.app.ui.common.setup

/**
 * Safe Journey setup (screen 10). The traveller sets a destination and ETA
 * and picks which circle members watch the trip (FR-22, FR-23). The "I'm OK"
 * check in timer sits below.
 */
@AndroidEntryPoint
class JourneySetupFragment : Fragment() {

    private var _binding: FragmentJourneySetupBinding? = null
    private val binding get() = _binding!!
    private val viewModel: JourneySetupViewModel by viewModels()
    private val checkInCard = CheckInCard(this)
    private var shownCircles: List<Circle> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentJourneySetupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.topBar.setup(title = getString(R.string.safe_journey), onBack = { popBackSafely() })
        checkInCard.bind(binding.checkIn)
        shownCircles = emptyList()

        binding.destinationInput.doAfterTextChanged { viewModel.onDestination(it?.toString().orEmpty()) }
        binding.etaSlider.addOnChangeListener { _, value, fromUser -> if (fromUser) viewModel.onEta(value.toInt()) }
        binding.startButton.setOnClickListener {
            viewModel.start {
                navigateSafely(
                    R.id.journeyMonitorFragment, null,
                    NavOptions.Builder().setPopUpTo(R.id.journeySetupFragment, true).build()
                )
            }
        }

        collectWhileStarted(viewModel.state) { state -> render(state) }
    }

    private fun render(state: JourneySetupUiState) {
        binding.destinationInput.setTextIfDifferent(state.destination)
        val eta = (5 + ((state.etaMinutes.coerceIn(5, 180) - 5) / 5) * 5).toFloat()
        if (binding.etaSlider.value != eta) binding.etaSlider.value = eta
        binding.etaValue.text = getString(R.string.eta_minutes, state.etaMinutes)

        // Up to four circles as single choice chips; rebuilt only when the list changes.
        val circles = state.circles.take(4)
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
        }

        binding.watchersGroup.isVisible = state.members.isNotEmpty()
        binding.watchersContainer.removeAllViews()
        state.members.forEach { member ->
            binding.watchersContainer.addDividerIfNeeded()
            val row = ItemWatcherBinding.inflate(layoutInflater, binding.watchersContainer, true)
            row.initial.text = member.displayName.take(1).ifBlank { "?" }.uppercase()
            row.name.text = member.displayName
            row.watchSwitch.isChecked = state.selectedWatcherIds.contains(member.uid)
            row.watchSwitch.setOnCheckedChangeListener { button, _ ->
                if (button.isPressed) viewModel.toggleWatcher(member.uid)
            }
            row.root.setOnClickListener { viewModel.toggleWatcher(member.uid) }
        }

        binding.errorText.setTextOrGone(
            when (state.error) {
                null -> null
                "DEST" -> getString(R.string.enter_destination)
                else -> getString(R.string.error_generic)
            }
        )
        binding.startButton.isEnabled = !state.saving
        binding.progress.isVisible = state.saving
    }

    override fun onDestroyView() {
        super.onDestroyView()
        checkInCard.unbind()
        _binding = null
    }
}
