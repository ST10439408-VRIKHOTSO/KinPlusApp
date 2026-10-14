package za.co.kinplus.app.ui.screens.zones

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import za.co.kinplus.app.R
import za.co.kinplus.app.databinding.FragmentSafeZonesBinding
import za.co.kinplus.app.domain.SafeZone
import za.co.kinplus.app.ui.common.addRow
import za.co.kinplus.app.ui.common.collectWhileStarted
import za.co.kinplus.app.ui.common.navigateSafely
import za.co.kinplus.app.ui.common.popBackSafely
import za.co.kinplus.app.ui.common.setEndAction
import za.co.kinplus.app.ui.common.setup

/** Safe Zones (screen 8). Saved places that notify the circle on enter or exit (FR-20, FR-21). */
@AndroidEntryPoint
class SafeZonesFragment : Fragment() {

    private var _binding: FragmentSafeZonesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SafeZonesViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSafeZonesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.topBar.setup(title = getString(R.string.safe_zones_title), onBack = { popBackSafely() })
        binding.addZoneButton.setOnClickListener { navigateSafely(R.id.safeZoneEditorFragment) }

        collectWhileStarted(viewModel.zones) { zones ->
            binding.emptyText.isVisible = zones.isEmpty()
            binding.zonesGroup.isVisible = zones.isNotEmpty()
            binding.zonesContainer.removeAllViews()
            zones.forEach { zone ->
                val row = binding.zonesContainer.addRow(
                    title = zone.name,
                    subtitle = zoneSummary(zone),
                    icon = iconForZone(zone.name),
                    onClick = { navigateSafely(R.id.safeZoneEditorFragment, bundleOf("zoneId" to zone.id)) }
                )
                row.chevron.isVisible = false
                row.setEndAction(R.drawable.ic_close, R.color.kin_muted, getString(R.string.remove)) {
                    viewModel.delete(zone.id)
                }
            }
        }
    }

    /** "200 m · Notify on enter · Notify on exit". */
    private fun zoneSummary(zone: SafeZone): String {
        val directions = listOfNotNull(
            getString(R.string.notify_on_enter).takeIf { zone.notifyOnEnter },
            getString(R.string.notify_on_exit).takeIf { zone.notifyOnExit }
        ).joinToString(" · ")
        return "${zone.radiusM} m · $directions"
    }

    private fun iconForZone(name: String): Int = when {
        name.contains("home", ignoreCase = true) -> R.drawable.ic_home
        name.contains("school", ignoreCase = true) || name.contains("university", ignoreCase = true) -> R.drawable.ic_school
        name.contains("work", ignoreCase = true) || name.contains("office", ignoreCase = true) -> R.drawable.ic_work
        else -> R.drawable.ic_place
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
