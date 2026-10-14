package za.co.kinplus.app.ui.screens.circles

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import za.co.kinplus.app.R
import za.co.kinplus.app.databinding.FragmentCreateJoinCircleBinding
import za.co.kinplus.app.ui.common.collectWhileStarted
import za.co.kinplus.app.ui.common.popBackSafely
import za.co.kinplus.app.ui.common.setTextOrGone
import za.co.kinplus.app.ui.common.setup

/** Create or join a circle (screen 7, FR-08 and FR-09). */
@AndroidEntryPoint
class CreateJoinCircleFragment : Fragment() {

    private var _binding: FragmentCreateJoinCircleBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CreateJoinViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCreateJoinCircleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.topBar.setup(title = getString(R.string.create_or_join_circle), onBack = { popBackSafely() })

        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) = showTab(tab.position)
            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
        showTab(binding.tabs.selectedTabPosition.coerceAtLeast(0))

        binding.createButton.setOnClickListener {
            viewModel.create(
                binding.nameInput.text?.toString().orEmpty(),
                binding.descriptionInput.text?.toString().orEmpty()
            ) { popBackSafely() }
        }
        binding.joinButton.setOnClickListener {
            viewModel.join(binding.codeInput.text?.toString().orEmpty().uppercase()) { popBackSafely() }
        }

        collectWhileStarted(viewModel.state) { state ->
            binding.progress.isVisible = state.loading
            binding.createButton.isEnabled = !state.loading
            binding.joinButton.isEnabled = !state.loading
            binding.errorText.setTextOrGone(
                when (state.error) {
                    null -> null
                    "NAME" -> getString(R.string.error_name_required)
                    "CODE" -> getString(R.string.enter_join_code)
                    else -> state.error
                }
            )
        }
    }

    private fun showTab(position: Int) {
        binding.createForm.isVisible = position == 0
        binding.joinForm.isVisible = position == 1
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
