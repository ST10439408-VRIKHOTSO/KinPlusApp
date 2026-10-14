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
import dagger.hilt.android.AndroidEntryPoint
import za.co.kinplus.app.R
import za.co.kinplus.app.databinding.FragmentLoginBinding
import za.co.kinplus.app.ui.common.collectWhileStarted
import za.co.kinplus.app.ui.common.navigateSafely
import za.co.kinplus.app.ui.common.setTextIfDifferent
import za.co.kinplus.app.ui.common.setTextOrGone

/**
 * Login (screen 2). Email and password sign in through Firebase Auth (FR-02),
 * a password reset link, and single sign on with Google (FR-05). The form
 * state lives in [LoginViewModel] so it survives rotation.
 */
@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.hero.headline.text = getString(R.string.welcome_back) + "."
        binding.hero.subtitle.text = getString(R.string.auth_subtitle)
        binding.tabs.select(signIn = true)
        binding.tabs.tabSignUp.setOnClickListener { openRegister() }
        binding.createAccount.setOnClickListener { openRegister() }

        binding.emailInput.doAfterTextChanged { viewModel.onEmail(it?.toString().orEmpty()) }
        binding.passwordInput.doAfterTextChanged { viewModel.onPassword(it?.toString().orEmpty()) }
        binding.forgotPassword.setOnClickListener { viewModel.forgotPassword() }

        binding.loginButton.setOnClickListener {
            viewModel.login(requireContext().applicationContext) {
                navigateSafely(
                    R.id.homeFragment, null,
                    NavOptions.Builder().setPopUpTo(R.id.loginFragment, true).build()
                )
            }
        }

        // Single sign on (FR-05). The Google "G" keeps its own colours, so the button tint is cleared.
        binding.googleButton.iconTint = null
        binding.googleButton.setOnClickListener {
            viewModel.googleSignIn(requireActivity()) {
                navigateSafely(
                    R.id.homeFragment, null,
                    NavOptions.Builder().setPopUpTo(R.id.loginFragment, true).build()
                )
            }
        }

        collectWhileStarted(viewModel.state) { state ->
            binding.emailInput.setTextIfDifferent(state.email)
            binding.passwordInput.setTextIfDifferent(state.password)
            binding.errorText.setTextOrGone(state.error?.let { mapAuthMessage(requireContext(), it) })
            binding.infoText.isVisible = state.info == "RESET_SENT"
            binding.loginButton.isEnabled = !state.loading
            binding.googleButton.isEnabled = !state.loading
            binding.loginButton.text = if (state.loading) "" else getString(R.string.log_in)
            binding.loginProgress.isVisible = state.loading
        }
    }

    private fun openRegister() = navigateSafely(R.id.registerFragment)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
