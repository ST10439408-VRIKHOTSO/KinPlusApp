package za.co.kinplus.app.ui.screens.safety

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import za.co.kinplus.app.R
import za.co.kinplus.app.databinding.DialogAddContactBinding
import za.co.kinplus.app.databinding.FragmentSafetyBinding
import za.co.kinplus.app.databinding.ItemEmergencyContactBinding
import za.co.kinplus.app.databinding.ItemEmergencyNumberBinding
import za.co.kinplus.app.databinding.ItemRowSwitchBinding
import za.co.kinplus.app.databinding.ItemRowBinding
import za.co.kinplus.app.databinding.ViewQuickTileBinding
import za.co.kinplus.app.databinding.ViewSectionLabelBinding
import za.co.kinplus.app.databinding.ViewVaultTileBinding
import za.co.kinplus.app.domain.EmergencyInfo
import za.co.kinplus.app.ui.common.addDividerIfNeeded
import za.co.kinplus.app.ui.common.addRow
import za.co.kinplus.app.ui.common.addSwitchRow
import za.co.kinplus.app.ui.common.collectWhileStarted
import za.co.kinplus.app.ui.common.navigateSafely
import za.co.kinplus.app.ui.common.setCheckedSilently
import za.co.kinplus.app.ui.common.setTextOrGone
import za.co.kinplus.app.ui.common.setup
import za.co.kinplus.app.ui.common.toast
import za.co.kinplus.app.ui.components.FakeCallController
import za.co.kinplus.app.ui.components.showFakeCallSetupDialog

private data class EmergencyNumber(val labelRes: Int, val number: String)

private val emergencyNumbers = listOf(
    EmergencyNumber(R.string.num_police, "10111"),
    EmergencyNumber(R.string.num_ambulance, "10177"),
    EmergencyNumber(R.string.num_112, "112"),
    EmergencyNumber(R.string.num_flying_squad, "10111")
)

/*
 * Nearby help (Batch 1 enhancement to the Safety hub). Each row asks the
 * phone's maps app to search around the user's current position, so it works
 * without an extra API key. The search terms stay in English because that is
 * what map providers index in South Africa.
 */
private data class NearbyPlace(val labelRes: Int, val query: String, val icon: Int)

private val nearbyPlaces = listOf(
    NearbyPlace(R.string.nearby_police, "police station", R.drawable.ic_local_police),
    NearbyPlace(R.string.nearby_hospital, "hospital", R.drawable.ic_local_hospital),
    NearbyPlace(R.string.nearby_clinic, "clinic", R.drawable.ic_medical_services),
    NearbyPlace(R.string.nearby_pharmacy, "pharmacy", R.drawable.ic_local_pharmacy)
)

/**
 * Safety hub (screen 13). Safety setup progress, quick actions, trusted
 * emergency contacts, the encrypted local device vault (FR-33), pausing
 * location sharing (FR-15), incident history, nearby help and emergency
 * numbers.
 */
@AndroidEntryPoint
class SafetyFragment : Fragment() {

    private var _binding: FragmentSafetyBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SafetyViewModel by viewModels()

    private var pauseRow: ItemRowSwitchBinding? = null
    private var incidentRow: ItemRowBinding? = null
    private val fakeCall = FakeCallController(this) { }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSafetyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.topBar.setup(
            title = getString(R.string.safety_hub_title),
            onFilter = {},
            onProfile = { navigateSafely(R.id.profileFragment) }
        )

        setupQuickTile(binding.quickJourney, R.drawable.ic_near_me, R.string.quick_share_journey, R.string.quick_share_journey_sub) {
            navigateSafely(R.id.journeySetupFragment)
        }
        setupQuickTile(binding.quickCheckIn, R.drawable.ic_verified_user, R.string.quick_checkin, R.string.quick_checkin_sub) {
            viewModel.shareLocation(requireActivity())
        }
        setupQuickTile(binding.quickFakeCall, R.drawable.ic_phone_callback, R.string.quick_fake_call, R.string.quick_fake_call_sub) {
            openFakeCallSetup()
        }

        binding.addContactButton.setOnClickListener { showAddContactDialog() }

        setupLabel(binding.vaultLabel, getString(R.string.local_device_vault_title))
        binding.vaultLabel.trailingIcon.isVisible = true
        binding.vaultLabel.trailingIcon.setImageResource(R.drawable.ic_lock)
        binding.vaultLabel.trailingIcon.imageTintList = requireContext().getColorStateList(R.color.kin_muted)
        binding.vaultLabel.trailing.setTextOrGone(getString(R.string.encrypted_label))
        binding.vaultMedical.root.setOnClickListener { openEmergencyInfo() }
        binding.vaultVehicle.root.setOnClickListener { openEmergencyInfo() }

        setupLabel(binding.privacyLabel, getString(R.string.privacy_controls_title))
        binding.privacyContainer.removeAllViews()
        pauseRow = binding.privacyContainer.addSwitchRow(
            title = getString(R.string.pause_location_sharing),
            subtitle = getString(R.string.pause_location_sharing_sub),
            icon = R.drawable.ic_location_off,
            checked = false,
            onChanged = { paused -> viewModel.togglePauseLocation(paused) }
        )

        setupLabel(binding.activityLabel, getString(R.string.recent_activity))
        binding.activityContainer.removeAllViews()
        incidentRow = binding.activityContainer.addRow(
            title = getString(R.string.incident_history_title),
            subtitle = getString(R.string.incident_history_sub),
            icon = R.drawable.ic_history,
            onClick = { navigateSafely(R.id.incidentHistoryFragment) }
        )

        setupLabel(binding.nearbyLabel, getString(R.string.nearby_help_title), getString(R.string.nearby_help_sub))
        binding.nearbyContainer.removeAllViews()
        nearbyPlaces.forEach { place ->
            binding.nearbyContainer.addRow(
                title = getString(place.labelRes),
                subtitle = getString(R.string.nearby_help_row_sub),
                icon = place.icon,
                onClick = { openNearby(place.query) }
            )
        }

        setupLabel(binding.numbersLabel, getString(R.string.emergency_numbers_section))
        binding.numbersContainer.removeAllViews()
        emergencyNumbers.forEach { entry ->
            binding.numbersContainer.addDividerIfNeeded()
            val row = ItemEmergencyNumberBinding.inflate(layoutInflater, binding.numbersContainer, true)
            row.number.text = entry.number
            row.label.setText(entry.labelRes)
            row.root.setOnClickListener { dial(entry.number) }
        }

        collectWhileStarted(viewModel.state) { state -> render(state) }
    }

    private fun render(state: SafetyUiState) {
        binding.setupComplete.text = getString(R.string.safety_setup_complete, state.setupPercent)
        binding.scoreRing.setProgressCompat(state.setupPercent, false)
        binding.scoreText.text = "${state.setupPercent}%"

        // Nudge: a backup contact first, then the rest of the emergency information.
        when {
            state.contacts.size < 2 -> {
                binding.nudgeCard.isVisible = true
                binding.nudgeTitle.setText(R.string.backup_contact_nudge_title)
                binding.nudgeSub.setText(R.string.backup_contact_nudge_sub)
                binding.nudgeButton.setOnClickListener { showAddContactDialog() }
            }
            state.setupPercent < 100 -> {
                binding.nudgeCard.isVisible = true
                binding.nudgeTitle.setText(R.string.emergency_information)
                binding.nudgeSub.text = getString(R.string.percent_complete, state.setupPercent)
                binding.nudgeButton.setOnClickListener { openEmergencyInfo() }
            }
            else -> binding.nudgeCard.isVisible = false
        }

        setupLabel(binding.contactsLabel, getString(R.string.emergency_contacts), getString(R.string.emergency_contacts_count, state.contacts.size))
        binding.contactsEmpty.isVisible = state.contacts.isEmpty()
        binding.contactsContainer.removeAllViews()
        state.contacts.forEachIndexed { index, contact ->
            binding.contactsContainer.addDividerIfNeeded()
            val row = ItemEmergencyContactBinding.inflate(layoutInflater, binding.contactsContainer, true)
            row.name.text = contact.name
            row.priority.text = getString(R.string.priority_badge, index + 1)
            row.relationship.text = contact.relationship
            row.callButton.setOnClickListener { dial(contact.phone) }
            row.removeButton.setOnClickListener { viewModel.deleteContact(contact.id) }
        }

        renderVault(state.emergencyInfo)
        pauseRow?.setCheckedSilently(state.locationPaused)
        incidentRow?.subtitle?.text = if (state.incidentCount == 0) {
            getString(R.string.incident_history_sub)
        } else {
            getString(R.string.incident_count, state.incidentCount)
        }
    }

    private fun renderVault(info: EmergencyInfo) {
        val bloodType = info.aboutMe.bloodType.trim()
        val allergies = info.medical.allergies.trim()
        bindVaultTile(
            binding.vaultMedical, R.drawable.ic_medical_services, getString(R.string.vault_medical_id),
            line1 = bloodType.takeIf { it.isNotBlank() }?.let { "Blood: $it" } ?: getString(R.string.vault_not_added),
            line2 = allergies.takeIf { it.isNotBlank() }?.let { "Allergies: $it" } ?: getString(R.string.vault_tap_to_add)
        )
        val vehicleSummary = listOf(info.vehicle.color, info.vehicle.make, info.vehicle.model)
            .filter { it.isNotBlank() }.joinToString(" ").trim()
        val plate = info.vehicle.plate.trim()
        bindVaultTile(
            binding.vaultVehicle, R.drawable.ic_directions_car, getString(R.string.vault_vehicle),
            line1 = vehicleSummary.takeIf { it.isNotBlank() } ?: getString(R.string.vault_not_added),
            line2 = plate.takeIf { it.isNotBlank() }?.let { "Plate: $it" } ?: getString(R.string.vault_tap_to_add)
        )
    }

    private fun bindVaultTile(tile: ViewVaultTileBinding, icon: Int, label: String, line1: String, line2: String) {
        tile.icon.setImageResource(icon)
        tile.label.text = label
        tile.line1.text = line1
        tile.line2.text = line2
    }

    private fun setupQuickTile(tile: ViewQuickTileBinding, icon: Int, title: Int, subtitle: Int, onClick: () -> Unit) {
        tile.icon.setImageResource(icon)
        tile.title.setText(title)
        tile.subtitle.setText(subtitle)
        tile.root.setOnClickListener { onClick() }
    }

    private fun setupLabel(label: ViewSectionLabelBinding, text: String, trailing: String? = null) {
        label.label.text = text
        label.trailing.setTextOrGone(trailing)
    }

    private fun openEmergencyInfo() = navigateSafely(R.id.emergencyInfoFragment)

    private fun openFakeCallSetup() {
        val defaultCaller = viewModel.state.value.contacts.firstOrNull()?.name
            ?: getString(R.string.fake_call_default_caller)
        showFakeCallSetupDialog(requireContext(), defaultCaller) { caller, delaySeconds ->
            fakeCall.start(caller, delaySeconds)
            if (delaySeconds > 0) {
                context?.toast(getString(R.string.fake_call_scheduled, delaySeconds), long = true)
            }
        }
    }

    private fun showAddContactDialog() {
        val form = DialogAddContactBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_contact)
            .setView(form.root)
            .setPositiveButton(R.string.save) { _, _ ->
                viewModel.addContact(
                    form.nameInput.text?.toString().orEmpty().trim(),
                    form.relationshipInput.text?.toString().orEmpty().trim(),
                    form.phoneInput.text?.toString().orEmpty().trim()
                )
            }
            .setNegativeButton(R.string.cancel, null)
            .show()

        // Save is only enabled once a name and phone number are entered.
        fun validate() {
            dialog.getButton(android.app.Dialog.BUTTON_POSITIVE)?.isEnabled =
                !form.nameInput.text.isNullOrBlank() && !form.phoneInput.text.isNullOrBlank()
        }
        form.nameInput.doAfterTextChanged { validate() }
        form.phoneInput.doAfterTextChanged { validate() }
        validate()
    }

    private fun dial(number: String) {
        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
    }

    private fun openNearby(query: String) {
        Log.d("SafetyFragment", "Opening nearby search for '$query'")
        val mapsIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(query)))
        try {
            startActivity(mapsIntent)
        } catch (e: ActivityNotFoundException) {
            // No maps app installed: fall back to Google Maps in the browser.
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/" + Uri.encode(query))))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fakeCall.cancel()
        pauseRow = null
        incidentRow = null
        _binding = null
    }
}
