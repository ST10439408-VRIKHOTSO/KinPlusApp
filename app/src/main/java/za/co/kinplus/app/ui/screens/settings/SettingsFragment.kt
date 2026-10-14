package za.co.kinplus.app.ui.screens.settings

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import za.co.kinplus.app.R
import za.co.kinplus.app.databinding.FragmentSettingsBinding
import za.co.kinplus.app.databinding.ItemRowBinding
import za.co.kinplus.app.databinding.ItemRowSwitchBinding
import za.co.kinplus.app.domain.Circle
import za.co.kinplus.app.ui.common.addRow
import za.co.kinplus.app.ui.common.addSwitchRow
import za.co.kinplus.app.ui.common.collectWhileStarted
import za.co.kinplus.app.ui.common.popBackSafely
import za.co.kinplus.app.ui.common.setCheckedSilently
import za.co.kinplus.app.ui.common.setup

/** Language options: tag to display name. */
private val languages = listOf(
    "en" to R.string.language_english,
    "zu" to R.string.language_zulu,
    "nso" to R.string.language_sepedi
)

/**
 * Settings (screen 20, the Part 2 "settings menu" criterion). The location
 * sharing privacy control centre (global pause and per circle, FR-15),
 * notifications, language (FR-31), units, inviting friends and app
 * permissions.
 */
@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()

    private var pauseRow: ItemRowSwitchBinding? = null
    private var notificationsRow: ItemRowSwitchBinding? = null
    private var languageRow: ItemRowBinding? = null
    private var unitsRow: ItemRowSwitchBinding? = null
    private var shownCircles: List<Circle>? = null
    private var shownPaused: Boolean? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.topBar.setup(title = getString(R.string.settings_title), onBack = { popBackSafely() })
        shownCircles = null
        shownPaused = null

        binding.pauseContainer.removeAllViews()
        pauseRow = binding.pauseContainer.addSwitchRow(
            title = getString(R.string.pause_all_sharing),
            subtitle = getString(R.string.pause_all_sub),
            checked = false,
            onChanged = { paused -> viewModel.setGlobalPaused(paused) }
        ).also { it.title.setTextColor(ContextCompat.getColor(requireContext(), R.color.kin_red)) }

        binding.preferencesContainer.removeAllViews()
        notificationsRow = binding.preferencesContainer.addSwitchRow(
            title = getString(R.string.notifications),
            subtitle = getString(R.string.notifications_sub),
            icon = R.drawable.ic_notifications,
            checked = true,
            onChanged = { viewModel.setNotifications(it) }
        )
        languageRow = binding.preferencesContainer.addRow(
            title = getString(R.string.language),
            subtitle = getString(R.string.language_english),
            icon = R.drawable.ic_language,
            onClick = { showLanguageDialog() }
        )
        unitsRow = binding.preferencesContainer.addSwitchRow(
            title = getString(R.string.units),
            subtitle = getString(R.string.units_metric),
            icon = R.drawable.ic_straighten,
            checked = true,
            onChanged = { viewModel.setMetric(it) }
        )

        binding.shareContainer.removeAllViews()
        binding.shareContainer.addRow(
            title = getString(R.string.invite_friends),
            subtitle = getString(R.string.invite_friends_sub),
            icon = R.drawable.ic_share,
            onClick = { inviteFriends() }
        )
        binding.shareContainer.addRow(
            title = getString(R.string.app_permissions),
            subtitle = getString(R.string.app_permissions_sub),
            icon = R.drawable.ic_vpn_key,
            onClick = { openAppPermissions() }
        )

        collectWhileStarted(viewModel.state) { state -> render(state) }
    }

    private fun render(state: SettingsUiState) {
        pauseRow?.setCheckedSilently(state.globalPaused)
        notificationsRow?.setCheckedSilently(state.notificationsEnabled)
        languageRow?.subtitle?.text = getString(languageLabel(state.languageTag))
        unitsRow?.setCheckedSilently(state.useMetric)
        unitsRow?.subtitle?.setText(if (state.useMetric) R.string.units_metric else R.string.units_imperial)

        // Per circle switches are rebuilt only when the circles or the global pause change.
        if (state.circles != shownCircles || state.globalPaused != shownPaused) {
            shownCircles = state.circles
            shownPaused = state.globalPaused
            binding.perCircleGroup.isVisible = state.circles.isNotEmpty()
            binding.circlesContainer.removeAllViews()
            state.circles.forEach { circle ->
                binding.circlesContainer.addSwitchRow(
                    title = circle.name,
                    checked = true,
                    enabled = !state.globalPaused,
                    onChanged = { enabled -> viewModel.setCircleSharing(circle.id, enabled) }
                )
            }
        }
    }

    private fun languageLabel(tag: String): Int = languages.firstOrNull { it.first == tag }?.second ?: R.string.language_english

    /** Choosing a language saves it and recreates the activity so every screen picks it up. */
    private fun showLanguageDialog() {
        val current = viewModel.state.value.languageTag
        val labels = languages.map { getString(it.second) }.toTypedArray()
        val checked = languages.indexOfFirst { it.first == current }.coerceAtLeast(0)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.choose_language)
            .setSingleChoiceItems(labels, checked) { dialog, which ->
                dialog.dismiss()
                viewModel.setLanguage(requireContext().applicationContext, languages[which].first)
                activity?.recreate()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun inviteFriends() {
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, getString(R.string.invite_friends_message))
                },
                null
            )
        )
    }

    private fun openAppPermissions() {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${requireContext().packageName}".toUri())
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pauseRow = null
        notificationsRow = null
        languageRow = null
        unitsRow = null
        _binding = null
    }
}
