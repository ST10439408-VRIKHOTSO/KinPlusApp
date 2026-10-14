package za.co.kinplus.app.ui.screens.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import za.co.kinplus.app.R
import za.co.kinplus.app.databinding.FragmentProfileBinding
import za.co.kinplus.app.databinding.ItemRowBinding
import za.co.kinplus.app.ui.common.addRow
import za.co.kinplus.app.ui.common.bindAvatar
import za.co.kinplus.app.ui.common.collectWhileStarted
import za.co.kinplus.app.ui.common.navigateSafely
import za.co.kinplus.app.ui.common.popBackSafely
import za.co.kinplus.app.ui.common.setup

/**
 * Profile (screen 16). Identity and account management: edit profile,
 * emergency information progress, settings, support, sign out and deleting the
 * account (POPIA right to erasure, FR-34).
 */
@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()
    private var emergencyRow: ItemRowBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.topBar.setup(title = getString(R.string.profile_title), onBack = { popBackSafely() })
        binding.headerCard.setOnClickListener { navigateSafely(R.id.editProfileFragment) }

        binding.accountContainer.removeAllViews()
        emergencyRow = binding.accountContainer.addRow(
            title = getString(R.string.emergency_information),
            subtitle = getString(R.string.percent_complete, 0),
            icon = R.drawable.ic_health_and_safety,
            onClick = { navigateSafely(R.id.emergencyInfoFragment) }
        )
        binding.accountContainer.addRow(
            title = getString(R.string.settings_title),
            subtitle = getString(R.string.settings_row_sub),
            icon = R.drawable.ic_settings,
            onClick = { navigateSafely(R.id.settingsFragment) }
        )

        binding.supportContainer.removeAllViews()
        binding.supportContainer.addRow(
            title = getString(R.string.contact_support),
            subtitle = getString(R.string.contact_support_sub),
            icon = R.drawable.ic_email,
            onClick = { startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))) }
        )

        binding.signOutButton.setOnClickListener {
            viewModel.signOut()
            goToLogin()
        }
        binding.deleteAccountButton.setOnClickListener { confirmDeleteAccount() }

        collectWhileStarted(viewModel.state) { state ->
            binding.displayName.text = state.displayName.ifBlank { getString(R.string.account) }
            binding.contactLine.text = state.phone?.takeIf { it.isNotBlank() } ?: state.email
            bindAvatar(binding.avatarPhoto, binding.avatarInitial, state.photoPath, state.displayName)
            emergencyRow?.subtitle?.text = getString(R.string.percent_complete, state.emergencyInfoPercent)
        }
    }

    private fun confirmDeleteAccount() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_account)
            .setMessage(R.string.delete_account_confirm)
            .setPositiveButton(R.string.delete_account) { _, _ -> viewModel.deleteAccount { goToLogin() } }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** Clears the whole back stack so Back cannot return to a signed in screen. */
    private fun goToLogin() {
        navigateSafely(
            R.id.loginFragment, null,
            NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        emergencyRow = null
        _binding = null
    }
}
