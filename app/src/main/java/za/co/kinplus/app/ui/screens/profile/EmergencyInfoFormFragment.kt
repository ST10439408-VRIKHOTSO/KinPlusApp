package za.co.kinplus.app.ui.screens.profile

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import za.co.kinplus.app.R
import za.co.kinplus.app.databinding.FragmentEmergencyInfoFormBinding
import za.co.kinplus.app.databinding.ItemFormFieldBinding
import za.co.kinplus.app.ui.common.collectWhileStarted
import za.co.kinplus.app.ui.common.popBackSafely
import za.co.kinplus.app.ui.common.setTextIfDifferent
import za.co.kinplus.app.ui.common.setup

/**
 * Emergency information form (screen 19). One text field per detail in the
 * chosen category; the category comes from the navigation argument.
 */
@AndroidEntryPoint
class EmergencyInfoFormFragment : Fragment() {

    private var _binding: FragmentEmergencyInfoFormBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EmergencyInfoFormViewModel by viewModels()
    private val fieldViews = mutableMapOf<String, ItemFormFieldBinding>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEmergencyInfoFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.topBar.setup(
            title = getString(viewModel.category.titleRes),
            onBack = { popBackSafely() },
            actionLabel = getString(R.string.save),
            onAction = { viewModel.save { popBackSafely() } }
        )

        binding.fieldsContainer.removeAllViews()
        fieldViews.clear()
        fieldsByCategory[viewModel.category].orEmpty().forEach { spec ->
            val field = ItemFormFieldBinding.inflate(layoutInflater, binding.fieldsContainer, true)
            field.root.hint = getString(spec.labelRes)
            // Notes can run over several lines; every other field is a single line.
            if (spec.key == "notes") {
                field.input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                field.input.minLines = 3
            } else {
                field.input.isSingleLine = true
            }
            field.input.doAfterTextChanged { viewModel.onFieldChange(spec.key, it?.toString().orEmpty()) }
            fieldViews[spec.key] = field
        }

        collectWhileStarted(viewModel.fields) { values ->
            fieldViews.forEach { (key, field) -> field.input.setTextIfDifferent(values[key].orEmpty()) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fieldViews.clear()
        _binding = null
    }
}
