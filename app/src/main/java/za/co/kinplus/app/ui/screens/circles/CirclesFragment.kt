package za.co.kinplus.app.ui.screens.circles

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import za.co.kinplus.app.R
import za.co.kinplus.app.databinding.FragmentCirclesBinding
import za.co.kinplus.app.databinding.ItemLiveMemberBinding
import za.co.kinplus.app.databinding.ItemOtherCircleBinding
import za.co.kinplus.app.domain.Circle
import za.co.kinplus.app.ui.common.addDividerIfNeeded
import za.co.kinplus.app.ui.common.collectWhileStarted
import za.co.kinplus.app.ui.common.navigateSafely
import za.co.kinplus.app.ui.common.setup
import za.co.kinplus.app.ui.common.toast

/**
 * Circles (screen 5). The primary circle with its join code and quick
 * actions (FR-08, FR-09), live member status, other connected circles, and
 * a way to create or join another circle.
 */
@AndroidEntryPoint
class CirclesFragment : Fragment() {

    private var _binding: FragmentCirclesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CirclesViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCirclesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.topBar.setup(
            title = getString(R.string.circles_title),
            onFilter = {},
            onProfile = { navigateSafely(R.id.profileFragment) }
        )
        binding.segmentZones.setOnClickListener { navigateSafely(R.id.safeZonesFragment) }
        binding.emptyCreateButton.setOnClickListener { openCreateJoin() }
        binding.createCircleButton.setOnClickListener { openCreateJoin() }

        binding.actionCode.icon.setImageResource(R.drawable.ic_key)
        binding.actionCode.subtitle.setText(R.string.action_join_code_hint)
        binding.actionAddMember.icon.setImageResource(R.drawable.ic_person_add_alt)
        binding.actionAddMember.title.setText(R.string.action_add_member)
        binding.actionAddMember.subtitle.setText(R.string.action_add_member_sub)
        binding.actionPermissions.icon.setImageResource(R.drawable.ic_tune)
        binding.actionPermissions.title.setText(R.string.action_manage_permissions)
        binding.actionPermissions.subtitle.setText(R.string.action_manage_permissions_sub)

        collectWhileStarted(viewModel.state) { state -> render(state) }
    }

    private fun render(state: CirclesUiState) {
        binding.offlineBanner.root.isVisible = state.isOffline
        binding.segmentZones.text = getString(R.string.circles_segment_safe_zones, state.safeZoneCount)

        state.deleteError?.let { message ->
            requireContext().toast(message, long = true)
            viewModel.deleteErrorShown()
        }

        val primary = state.circles.firstOrNull()
        binding.primaryGroup.isVisible = primary != null
        binding.emptyGroup.isVisible = primary == null
        if (primary != null) renderPrimary(primary, state)

        val others = state.circles.drop(1)
        binding.otherGroup.isVisible = others.isNotEmpty()
        binding.otherContainer.removeAllViews()
        others.forEach { circle ->
            binding.otherContainer.addDividerIfNeeded()
            val row = ItemOtherCircleBinding.inflate(layoutInflater, binding.otherContainer, true)
            row.name.text = circle.name
            row.subtitle.text = getString(R.string.members_count, circle.memberCount) + descriptionSuffix(circle)
            row.root.setOnClickListener { openCircle(circle.id) }
            row.deleteButton.setOnClickListener { confirmDelete(circle) }
        }
    }

    private fun renderPrimary(primary: Circle, state: CirclesUiState) {
        binding.primaryName.text = primary.name
        binding.adminBadge.isVisible = primary.ownerUid == state.myUid
        binding.primaryStatus.text = getString(R.string.circle_status_members, primary.memberCount) + descriptionSuffix(primary)
        binding.primaryCard.setOnClickListener { openCircle(primary.id) }
        binding.primaryMenu.setOnClickListener { anchor ->
            PopupMenu(requireContext(), anchor).apply {
                menu.add(R.string.delete_circle)
                setOnMenuItemClickListener { confirmDelete(primary); true }
                show()
            }
        }

        binding.actionCode.title.text = primary.joinCode
        binding.actionCode.root.setOnClickListener {
            val clipboard = requireContext().getSystemService(ClipboardManager::class.java)
            clipboard?.setPrimaryClip(ClipData.newPlainText("Kin+ join code", primary.joinCode))
            requireContext().toast(getString(R.string.code_copied))
        }
        binding.actionAddMember.root.setOnClickListener { openCreateJoin() }
        binding.actionPermissions.root.setOnClickListener { openCircle(primary.id) }

        binding.membersEmpty.isVisible = state.primaryMembers.isEmpty()
        binding.membersContainer.removeAllViews()
        state.primaryMembers.forEach { member ->
            binding.membersContainer.addDividerIfNeeded()
            val row = ItemLiveMemberBinding.inflate(layoutInflater, binding.membersContainer, true)
            val isMe = member.uid == state.myUid
            row.initial.text = member.displayName.take(1).ifBlank { "?" }.uppercase()
            row.name.text = member.displayName + if (isMe) getString(R.string.you_suffix) else ""
            row.meCheck.isVisible = isMe
            row.battery.text = getString(R.string.battery_pct, member.batteryPct)
        }
    }

    private fun descriptionSuffix(circle: Circle): String =
        circle.description?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""

    /** Removing yourself from a circle; for the sole owner this effectively deletes it. */
    private fun confirmDelete(circle: Circle) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_circle_confirm_title, circle.name))
            .setMessage(R.string.delete_circle_confirm_body)
            .setPositiveButton(R.string.delete_circle) { _, _ -> viewModel.deleteCircle(circle.id) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun openCircle(id: String) = navigateSafely(R.id.circleDetailFragment, bundleOf("circleId" to id))
    private fun openCreateJoin() = navigateSafely(R.id.createJoinCircleFragment)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
