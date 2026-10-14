package za.co.kinplus.app.ui.screens.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import za.co.kinplus.app.R
import za.co.kinplus.app.databinding.FragmentEmergencyInformationBinding
import za.co.kinplus.app.databinding.ItemInfoCategoryBinding
import za.co.kinplus.app.ui.common.addDividerIfNeeded
import za.co.kinplus.app.ui.common.collectWhileStarted
import za.co.kinplus.app.ui.common.navigateSafely
import za.co.kinplus.app.ui.common.popBackSafely
import za.co.kinplus.app.ui.common.setup

/**
 * Emergency information (screen 18). Five categories of details first
 * responders may need, each with a completion bar. Everything is stored only
 * on this device, encrypted (FR-33).
 */
@AndroidEntryPoint
class EmergencyInformationFragment : Fragment() {

    private var _binding: FragmentEmergencyInformationBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EmergencyInfoListViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEmergencyInformationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.topBar.setup(title = getString(R.string.emergency_information), onBack = { popBackSafely() })

        collectWhileStarted(viewModel.info) { info ->
            binding.categoriesContainer.removeAllViews()
            EmergencyInfoCategory.entries.forEach { category ->
                binding.categoriesContainer.addDividerIfNeeded()
                val row = ItemInfoCategoryBinding.inflate(layoutInflater, binding.categoriesContainer, true)
                val percent = info.percentFor(category)
                row.title.setText(category.titleRes)
                row.percent.text = getString(R.string.percent_complete, percent)
                row.progress.setProgressCompat(percent, false)
                row.root.setOnClickListener {
                    navigateSafely(R.id.emergencyInfoFormFragment, bundleOf("category" to category.route))
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
