package za.co.kinplus.app.ui.screens.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import za.co.kinplus.app.R
import za.co.kinplus.app.databinding.FragmentRegisterBinding
import za.co.kinplus.app.ui.common.collectWhileStarted
import za.co.kinplus.app.ui.common.navigateSafely
import za.co.kinplus.app.ui.common.popBackSafely
import za.co.kinplus.app.ui.common.setTextIfDifferent
import za.co.kinplus.app.ui.common.setTextOrGone

/**
 * Register (screen 3). Creates a Firebase account (FR-01) after client side
 * validation in [RegisterViewModel], with POPIA privacy consent (FR-32).
 */
@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.hero.headline.text = getString(R.string.register_headline)
        binding.hero.subtitle.text = getString(R.string.register_subtitle)
        binding.tabs.select(signIn = false)
        binding.tabs.tabSignIn.setOnClickListener { popBackSafely() }
        binding.backToLogin.setOnClickListener { popBackSafely() }

        binding.firstNameInput.doAfterTextChanged { viewModel.onFirstName(it?.toString().orEmpty()) }
        binding.lastNameInput.doAfterTextChanged { viewModel.onLastName(it?.toString().orEmpty()) }
        binding.phoneInput.doAfterTextChanged { viewModel.onPhone(it?.toString().orEmpty()) }
        binding.emailInput.doAfterTextChanged { viewModel.onEmail(it?.toString().orEmpty()) }
        binding.passwordInput.doAfterTextChanged { viewModel.onPassword(it?.toString().orEmpty()) }
        binding.confirmInput.doAfterTextChanged { viewModel.onConfirm(it?.toString().orEmpty()) }
        binding.consentCheck.setOnCheckedChangeListener { _, checked -> viewModel.onConsent(checked) }

        binding.privacyPolicyLink.setOnClickListener { showPrivacyPolicy() }

        binding.registerButton.setOnClickListener {
            viewModel.register(requireContext().applicationContext) {
                navigateSafely(
                    R.id.homeFragment, null,
                    NavOptions.Builder().setPopUpTo(R.id.loginFragment, true).build()
                )
            }
        }

        collectWhileStarted(viewModel.state) { state ->
            binding.firstNameInput.setTextIfDifferent(state.firstName)
            binding.lastNameInput.setTextIfDifferent(state.lastName)
            binding.phoneInput.setTextIfDifferent(state.phone)
            binding.emailInput.setTextIfDifferent(state.email)
            binding.passwordInput.setTextIfDifferent(state.password)
            binding.confirmInput.setTextIfDifferent(state.confirm)
            if (binding.consentCheck.isChecked != state.consent) binding.consentCheck.isChecked = state.consent
            binding.errorText.setTextOrGone(state.error?.let { mapAuthMessage(requireContext(), it) })
            binding.registerButton.isEnabled = !state.loading
            binding.registerButton.text = if (state.loading) "" else getString(R.string.create_account)
            binding.registerProgress.isVisible = state.loading
        }
    }

    private fun showPrivacyPolicy() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.privacy_policy_title)
            .setMessage(R.string.privacy_policy_body)
            .setPositiveButton(R.string.confirm, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
