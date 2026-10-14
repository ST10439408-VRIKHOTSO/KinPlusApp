package za.co.kinplus.app.ui.screens.circles

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import za.co.kinplus.app.R
import za.co.kinplus.app.databinding.FragmentCircleDetailBinding
import za.co.kinplus.app.databinding.ItemCircleMemberBinding
import za.co.kinplus.app.domain.CircleDetail
import za.co.kinplus.app.ui.common.addDividerIfNeeded
import za.co.kinplus.app.ui.common.bindAvatar
import za.co.kinplus.app.ui.common.collectWhileStarted
import za.co.kinplus.app.ui.common.popBackSafely
import za.co.kinplus.app.ui.common.setup
import za.co.kinplus.app.ui.common.toast

/**
 * Circle detail (screen 6). Circle photo, join code and invite actions
 * (FR-09), members with their roles, your own sharing switch for this circle
 * (FR-15), and leaving the circle.
 */
@AndroidEntryPoint
class CircleDetailFragment : Fragment() {

    private var _binding: FragmentCircleDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CircleDetailViewModel by viewModels()

    private val circleId: String by lazy { requireArguments().getString("circleId").orEmpty() }
    private var circleName: String = ""

    private val pickPhoto = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.pickPhoto(circleId, it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCircleDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.topBar.setup(title = getString(R.string.circles_title), onBack = { popBackSafely() })
        binding.avatar.cameraBadge.setOnClickListener {
            pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        binding.leaveRow.setOnClickListener { viewModel.leave(circleId) { popBackSafely() } }

        viewModel.load(circleId)

        collectWhileStarted(viewModel.state) { state ->
            binding.loading.isVisible = state.loading
            binding.content.isVisible = !state.loading && state.detail != null
            binding.errorText.isVisible = !state.loading && state.detail == null
            binding.errorText.text = state.error ?: getString(R.string.error_generic)
            binding.topBar.title.text = state.detail?.name ?: getString(R.string.circles_title)
            binding.leaveRow.isEnabled = !state.leaving
            binding.leaveProgress.isVisible = state.leaving

            state.leaveError?.let { message ->
                requireContext().toast(message, long = true)
                viewModel.leaveErrorShown()
            }
            state.detail?.let { renderDetail(it, state.myUid) }
        }

        collectWhileStarted(viewModel.photoPath(circleId)) { path ->
            bindAvatar(binding.avatar.photo, binding.avatar.initial, path, circleName)
        }
    }

    private fun renderDetail(detail: CircleDetail, myUid: String) {
        circleName = detail.name
        if (binding.avatar.photo.drawable == null) {
            binding.avatar.initial.text = detail.name.take(1).ifBlank { "?" }.uppercase()
        }
        binding.joinCode.text = getString(R.string.join_code) + " · " + detail.joinCode
        binding.inviteButton.setOnClickListener { shareInviteCode(detail.joinCode) }
        binding.copyCodeButton.setOnClickListener { copyInviteCode(detail.joinCode) }

        binding.membersLabel.text = getString(R.string.members) + " · " + detail.members.size
        binding.membersContainer.removeAllViews()
        detail.members.forEach { member ->
            binding.membersContainer.addDividerIfNeeded()
            val row = ItemCircleMemberBinding.inflate(layoutInflater, binding.membersContainer, true)
            row.initial.text = member.displayName.take(1).ifBlank { "?" }.uppercase()
            row.name.text = member.displayName
            row.roleBadge.text = getString(if (member.isOwner) R.string.owner else R.string.member).uppercase()
            if (!member.isOwner) {
                row.roleBadge.setBackgroundResource(R.drawable.bg_badge_neutral)
                row.roleBadge.setTextColor(requireContext().getColor(R.color.kin_muted))
            }
            row.sharingText.setText(if (member.sharingEnabled) R.string.sharing_on else R.string.sharing_off)
            // A member can toggle their own sharing within this circle (FR-15).
            val isMe = member.uid == myUid
            row.sharingSwitch.isVisible = isMe
            row.sharingSwitch.isChecked = member.sharingEnabled
            row.sharingSwitch.setOnCheckedChangeListener { button, checked ->
                if (button.isPressed) viewModel.toggleSharing(circleId, checked)
            }
        }
    }

    private fun shareInviteCode(code: String) {
        val text = getString(R.string.invite_friends_message) + " " + code
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) },
                null
            )
        )
    }

    private fun copyInviteCode(code: String) {
        val clipboard = requireContext().getSystemService(ClipboardManager::class.java)
        clipboard?.setPrimaryClip(ClipData.newPlainText("Kin+ join code", code))
        requireContext().toast(getString(R.string.code_copied))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
