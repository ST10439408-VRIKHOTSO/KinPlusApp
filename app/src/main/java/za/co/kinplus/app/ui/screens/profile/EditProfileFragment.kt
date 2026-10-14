package za.co.kinplus.app.ui.screens.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import za.co.kinplus.app.R
import za.co.kinplus.app.auth.AuthManager
import za.co.kinplus.app.databinding.FragmentEditProfileBinding
import za.co.kinplus.app.ui.common.bindAvatar
import za.co.kinplus.app.ui.common.collectWhileStarted
import za.co.kinplus.app.ui.common.popBackSafely
import za.co.kinplus.app.ui.common.setTextIfDifferent
import za.co.kinplus.app.ui.common.setTextOrGone
import za.co.kinplus.app.ui.common.setup
import za.co.kinplus.app.ui.screens.auth.mapAuthMessage

/** Edit profile (screen 17). Name, phone number, profile photo and mailing list consent. */
@AndroidEntryPoint
class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EditProfileViewModel by viewModels()

    private val pickPhoto = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.pickPhoto(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.topBar.setup(
            title = getString(R.string.edit_profile_title),
            onBack = { popBackSafely() },
            actionLabel = getString(R.string.save),
            onAction = { trySave() }
        )
        binding.avatar.cameraBadge.setOnClickListener {
            pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.firstNameInput.doAfterTextChanged { viewModel.onFirstName(it?.toString().orEmpty()) }
        binding.lastNameInput.doAfterTextChanged { viewModel.onLastName(it?.toString().orEmpty()) }
        binding.phoneInput.doAfterTextChanged { viewModel.onPhone(it?.toString().orEmpty()) }
        binding.mailingListCheck.setOnCheckedChangeListener { button, checked ->
            if (button.isPressed) viewModel.onMailingListConsent(checked)
        }
        binding.saveButton.setOnClickListener { trySave() }

        collectWhileStarted(viewModel.state) { state ->
            binding.firstNameInput.setTextIfDifferent(state.firstName)
            binding.lastNameInput.setTextIfDifferent(state.lastName)
            binding.phoneInput.setTextIfDifferent(state.phone)
            binding.emailInput.setTextIfDifferent(state.email)
            if (binding.mailingListCheck.isChecked != state.mailingListConsent) {
                binding.mailingListCheck.isChecked = state.mailingListConsent
            }
            bindAvatar(binding.avatar.photo, binding.avatar.initial, state.photoPath, state.firstName)
        }
    }

    /** Validates the name and phone number before saving (FR-01 rules). */
    private fun trySave() {
        val state = viewModel.state.value
        val error = when {
            state.firstName.isBlank() || state.lastName.isBlank() -> "NAME_REQUIRED"
            !AuthManager.isValidPhone(state.phone) -> "INVALID_PHONE"
            else -> null
        }
        binding.errorText.setTextOrGone(error?.let { mapAuthMessage(requireContext(), it) })
        if (error == null) viewModel.save { popBackSafely() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
