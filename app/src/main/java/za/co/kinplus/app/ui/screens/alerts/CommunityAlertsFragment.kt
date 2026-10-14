package za.co.kinplus.app.ui.screens.alerts

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
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import za.co.kinplus.app.R
import za.co.kinplus.app.databinding.FragmentCommunityAlertsBinding
import za.co.kinplus.app.databinding.ItemAlertBinding
import za.co.kinplus.app.databinding.SheetReportAlertBinding
import za.co.kinplus.app.domain.CommunityAlert
import za.co.kinplus.app.ui.common.collectWhileStarted
import za.co.kinplus.app.ui.common.setTextOrGone
import za.co.kinplus.app.ui.common.setup

/** Category filter chips: null shows every category. */
private val filterCategories = listOf(
    null to R.string.all,
    "CRIME" to R.string.category_crime,
    "HAZARD" to R.string.category_hazard,
    "MEDICAL" to R.string.category_medical
)

/** Categories offered when reporting a new incident. */
private val reportCategories = listOf(
    "CRIME" to R.string.category_crime,
    "HAZARD" to R.string.category_hazard,
    "MEDICAL" to R.string.category_medical,
    "FIRE" to R.string.category_fire,
    "OTHER" to R.string.category_other
)

private fun iconForCategory(category: String): Int = when (category.uppercase()) {
    "CRIME" -> R.drawable.ic_gavel
    "HAZARD" -> R.drawable.ic_warning
    "MEDICAL" -> R.drawable.ic_medical_services
    "FIRE" -> R.drawable.ic_local_fire_department
    "ACCIDENT" -> R.drawable.ic_car_crash
    else -> R.drawable.ic_report_problem
}

/** The category name in the user's language, falling back to the server's text for unknown categories. */
private fun categoryLabel(context: android.content.Context, category: String): String = when (category.uppercase()) {
    "CRIME" -> context.getString(R.string.category_crime)
    "HAZARD" -> context.getString(R.string.category_hazard)
    "MEDICAL" -> context.getString(R.string.category_medical)
    "FIRE" -> context.getString(R.string.category_fire)
    "ACCIDENT" -> context.getString(R.string.category_accident)
    "DANGER" -> context.getString(R.string.category_danger)
    "OTHER" -> context.getString(R.string.category_other)
    else -> category.lowercase().replaceFirstChar { it.uppercase() }
}

/**
 * Community alerts (screen 15). Nearby reports from other Kin+ users, sorted
 * nearest first and filtered by category and distance (FR-25), with
 * confirmations ("I see it too") and a form to report a new incident (FR-26).
 */
@AndroidEntryPoint
class CommunityAlertsFragment : Fragment() {

    private var _binding: FragmentCommunityAlertsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CommunityAlertsViewModel by viewModels()

    private var categoryFilter: String? = null
    private var maxKm: Int? = null
    private var latestState = AlertsUiState()
    private lateinit var adapter: AlertAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCommunityAlertsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.topBar.setup(title = getString(R.string.community_alerts))

        adapter = AlertAdapter(
            onConfirm = { alert -> viewModel.confirm(alert.id) },
            onDetails = { alert, km -> showDetails(alert, km) }
        )
        binding.alertList.layoutManager = LinearLayoutManager(requireContext())
        binding.alertList.adapter = adapter

        addChoiceChips(binding.categoryChips, filterCategories.map { (key, label) -> key to getString(label) }, categoryFilter) {
            categoryFilter = it
            renderList()
        }
        addChoiceChips(
            binding.distanceChips,
            distanceFilters.map { km ->
                km to if (km == null) getString(R.string.alerts_distance_any) else getString(R.string.alerts_distance_km, km)
            },
            maxKm
        ) {
            maxKm = it
            Log.d("CommunityAlerts", "Distance filter set to ${it ?: "any"} km")
            renderList()
        }

        binding.reportButton.setOnClickListener { showReportSheet() }

        collectWhileStarted(viewModel.state) { state ->
            latestState = state
            binding.loading.isVisible = state.loading && state.nearby.isEmpty()
            renderList()
        }
    }

    /** Adds one checkable chip per option into a single selection ChipGroup. */
    private fun <T> addChoiceChips(group: ChipGroup, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
        group.removeAllViews()
        options.forEach { (value, label) ->
            val chip = layoutInflater.inflate(R.layout.item_choice_chip, group, false) as Chip
            chip.id = View.generateViewId()
            chip.text = label
            chip.isChecked = value == selected
            chip.setOnClickListener { onSelect(value) }
            group.addView(chip)
        }
    }

    /** Pairs every alert with its distance from the user, then filters and sorts nearest first. */
    private fun visibleAlerts(state: AlertsUiState): List<Pair<CommunityAlert, Double?>> {
        val lat = state.lat
        val lng = state.lng
        return state.nearby
            .map { alert -> alert to if (lat != null && lng != null) distanceKm(lat, lng, alert.lat, alert.lng) else null }
            .filter { (alert, _) -> categoryFilter == null || alert.category.equals(categoryFilter, ignoreCase = true) }
            .filter { (_, km) -> maxKm == null || km == null || km <= maxKm!! }
            .sortedBy { (_, km) -> km ?: Double.MAX_VALUE }
    }

    private fun renderList() {
        val b = _binding ?: return
        val visible = visibleAlerts(latestState)
        b.emptyText.isVisible = visible.isEmpty() && !latestState.loading
        adapter.submitList(visible)
    }

    /** Full details of one report, with a shortcut to see the spot in the maps app. */
    private fun showDetails(alert: CommunityAlert, km: Double?) {
        val lines = buildList {
            add(alert.description.ifBlank { getString(R.string.alert_no_description) })
            add("")
            if (km != null) add(getString(R.string.km_away, km))
            add(getString(R.string.confirm_count, alert.confirmCount))
        }
        MaterialAlertDialogBuilder(requireContext())
            .setIcon(iconForCategory(alert.category))
            .setTitle(categoryLabel(requireContext(), alert.category))
            .setMessage(lines.joinToString("\n"))
            .setPositiveButton(R.string.alert_open_in_maps) { _, _ ->
                Log.d("CommunityAlerts", "Opening alert ${alert.id} in maps")
                val label = Uri.encode(alert.category)
                runCatching {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:${alert.lat},${alert.lng}?q=${alert.lat},${alert.lng}($label)")))
                }
            }
            .setNeutralButton(R.string.i_see_it_too) { _, _ -> viewModel.confirm(alert.id) }
            .setNegativeButton(R.string.alert_close, null)
            .show()
    }

    /** Bottom sheet to report a new incident at the user's location. */
    private fun showReportSheet() {
        val sheet = BottomSheetDialog(requireContext())
        val form = SheetReportAlertBinding.inflate(layoutInflater)
        sheet.setContentView(form.root)

        addChoiceChips(form.categoryChips, reportCategories.map { (key, label) -> key to getString(label) }, viewModel.state.value.category) {
            viewModel.onCategory(it)
        }
        form.descriptionInput.setText(viewModel.state.value.description)
        form.descriptionInput.doAfterTextChanged { viewModel.onDescription(it?.toString().orEmpty()) }
        form.submitButton.setOnClickListener {
            viewModel.submit()
            sheet.dismiss()
        }
        sheet.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/** RecyclerView adapter for alert cards, each paired with its distance in km. */
private class AlertAdapter(
    private val onConfirm: (CommunityAlert) -> Unit,
    private val onDetails: (CommunityAlert, Double?) -> Unit
) : ListAdapter<Pair<CommunityAlert, Double?>, AlertAdapter.Holder>(Diff) {

    class Holder(val binding: ItemAlertBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemAlertBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val (alert, km) = getItem(position)
        val context = holder.itemView.context
        holder.binding.icon.setImageResource(iconForCategory(alert.category))
        holder.binding.category.text = categoryLabel(context, alert.category)
        holder.binding.distance.setTextOrGone(km?.let { context.getString(R.string.km_away, it) })
        holder.binding.confirmCount.text = context.getString(R.string.confirm_count, alert.confirmCount)
        holder.binding.description.text = alert.description
        holder.binding.confirmButton.setOnClickListener { onConfirm(alert) }
        holder.binding.detailsButton.setOnClickListener { onDetails(alert, km) }
    }

    private object Diff : DiffUtil.ItemCallback<Pair<CommunityAlert, Double?>>() {
        override fun areItemsTheSame(oldItem: Pair<CommunityAlert, Double?>, newItem: Pair<CommunityAlert, Double?>) =
            oldItem.first.id == newItem.first.id
        override fun areContentsTheSame(oldItem: Pair<CommunityAlert, Double?>, newItem: Pair<CommunityAlert, Double?>) =
            oldItem == newItem
    }
}
