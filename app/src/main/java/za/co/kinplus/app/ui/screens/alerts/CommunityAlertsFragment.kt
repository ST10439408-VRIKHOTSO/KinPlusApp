package za.co.kinplus.app.ui.screens.alerts

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import za.co.kinplus.app.R
import za.co.kinplus.app.data.repository.ReportOutcome
import za.co.kinplus.app.databinding.FragmentCommunityAlertsBinding
import za.co.kinplus.app.databinding.ItemAlertBinding
import za.co.kinplus.app.databinding.SheetReportAlertBinding
import za.co.kinplus.app.databinding.ViewReportPhotoBinding
import za.co.kinplus.app.domain.CommunityAlert
import za.co.kinplus.app.ui.common.DistanceFormat
import za.co.kinplus.app.ui.common.collectWhileStarted
import za.co.kinplus.app.ui.common.dp
import za.co.kinplus.app.ui.common.setTextOrGone
import za.co.kinplus.app.ui.common.setup
import za.co.kinplus.app.ui.common.toast

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
 * confirmations ("I see it too") and a form to report a new incident with an
 * optional photo (FR-26). Distances follow Settings > Units.
 *
 * Reference: Android Developers (2026) Photo picker. Available at:
 * https://developer.android.com/training/data-storage/shared/photopicker [Accessed 5 November 2026].
 */
@AndroidEntryPoint
class CommunityAlertsFragment : Fragment() {

    private var _binding: FragmentCommunityAlertsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CommunityAlertsViewModel by viewModels()

    private var categoryFilter: String? = null
    /** Distance filter in the chosen unit (km or miles); null means any distance. */
    private var maxDistance: Int? = null
    private var shownMetric: Boolean? = null
    private var latestState = AlertsUiState()
    private lateinit var adapter: AlertAdapter

    private var reportSheet: BottomSheetDialog? = null
    private var reportForm: SheetReportAlertBinding? = null

    /** The system photo picker: no storage permission is needed to choose one image. */
    private val pickReportPhoto = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.onPhotoPicked(uri)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCommunityAlertsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.topBar.setup(title = getString(R.string.community_alerts))
        shownMetric = null

        adapter = AlertAdapter(
            distanceText = { km -> DistanceFormat.away(requireContext(), km, latestState.useMetric) },
            onConfirm = { alert -> viewModel.confirm(alert.id) },
            onDetails = { alert, km -> showDetails(alert, km) }
        )
        binding.alertList.layoutManager = LinearLayoutManager(requireContext())
        binding.alertList.adapter = adapter

        addChoiceChips(binding.categoryChips, filterCategories.map { (key, label) -> key to getString(label) }, categoryFilter) {
            categoryFilter = it
            renderList()
        }

        binding.reportButton.setOnClickListener { showReportSheet() }

        collectWhileStarted(viewModel.state) { state ->
            latestState = state
            binding.loading.isVisible = state.loading && state.nearby.isEmpty()
            if (state.useMetric != shownMetric) renderDistanceChips(state.useMetric)
            renderList()
            renderReportForm(state)
        }
        collectWhileStarted(viewModel.events) { event -> onEvent(event) }
    }

    /** Distance chips in km or miles. A choice from the other unit is cleared when the unit changes. */
    private fun renderDistanceChips(metric: Boolean) {
        val options = DistanceFormat.filterOptions(metric)
        val unitChanged = shownMetric != null
        if (unitChanged && maxDistance !in options) maxDistance = null
        shownMetric = metric
        // Rows already on screen show their distance in the old unit, so draw them again.
        if (unitChanged) adapter.notifyItemRangeChanged(0, adapter.itemCount)
        addChoiceChips(
            binding.distanceChips,
            options.map { value -> value to DistanceFormat.filterLabel(requireContext(), value, metric) },
            maxDistance
        ) {
            maxDistance = it
            Log.d(TAG, "Distance filter set to ${it ?: "any"} ${if (metric) "km" else "mi"}")
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
        val limitKm = maxDistance?.let { DistanceFormat.filterToKm(it, state.useMetric) }
        return state.nearby
            .map { alert -> alert to if (lat != null && lng != null) distanceKm(lat, lng, alert.lat, alert.lng) else null }
            .filter { (alert, _) -> categoryFilter == null || alert.category.equals(categoryFilter, ignoreCase = true) }
            .filter { (_, km) -> limitKm == null || km == null || km <= limitKm }
            .sortedBy { (_, km) -> km ?: Double.MAX_VALUE }
    }

    private fun renderList() {
        val b = _binding ?: return
        val visible = visibleAlerts(latestState)
        b.emptyText.isVisible = visible.isEmpty() && !latestState.loading
        adapter.submitList(visible)
    }

    private fun onEvent(event: AlertsEvent) {
        val context = context ?: return
        when (event) {
            is AlertsEvent.Submitted -> {
                reportSheet?.dismiss()
                val message = when (event.outcome) {
                    ReportOutcome.SENT -> R.string.report_submitted
                    ReportOutcome.SENT_WITHOUT_PHOTO -> R.string.report_photo_failed
                    ReportOutcome.QUEUED -> R.string.report_queued_offline
                }
                context.toast(getString(message), long = true)
            }
            AlertsEvent.NoLocation -> context.toast(getString(R.string.location_unavailable), long = true)
            AlertsEvent.PhotoUnreadable -> context.toast(getString(R.string.error_generic))
        }
    }

    /** Full details of one report, its photo when it has one, and a shortcut to the maps app. */
    private fun showDetails(alert: CommunityAlert, km: Double?) {
        val lines = buildList {
            add(alert.description.ifBlank { getString(R.string.alert_no_description) })
            add("")
            if (km != null) add(DistanceFormat.away(requireContext(), km, latestState.useMetric))
            add(getString(R.string.confirm_count, alert.confirmCount))
        }
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setIcon(iconForCategory(alert.category))
            .setTitle(categoryLabel(requireContext(), alert.category))
            .setMessage(lines.joinToString("\n"))
            .setPositiveButton(R.string.alert_open_in_maps) { _, _ ->
                Log.d(TAG, "Opening alert ${alert.id} in maps")
                val label = Uri.encode(alert.category)
                runCatching {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:${alert.lat},${alert.lng}?q=${alert.lat},${alert.lng}($label)")))
                }
            }
            .setNeutralButton(R.string.i_see_it_too) { _, _ -> viewModel.confirm(alert.id) }
            .setNegativeButton(R.string.alert_close, null)

        val photoUrl = alert.photoUrl
        if (!photoUrl.isNullOrBlank()) {
            // The photo is downloaded from Azure Blob Storage while the dialog is open.
            val photoView = ViewReportPhotoBinding.inflate(layoutInflater)
            builder.setView(photoView.root)
            viewLifecycleOwner.lifecycleScope.launch {
                val bitmap = viewModel.loadPhoto(photoUrl, requireContext().dp(PHOTO_MAX_DP))
                photoView.photoLoading.isVisible = false
                if (bitmap != null) {
                    photoView.photo.setImageBitmap(bitmap)
                    photoView.photo.isVisible = true
                } else {
                    photoView.root.isVisible = false
                }
            }
        }
        builder.show()
    }

    /** Bottom sheet to report a new incident at the user's location. */
    private fun showReportSheet() {
        if (reportSheet?.isShowing == true) return
        val sheet = BottomSheetDialog(requireContext())
        val form = SheetReportAlertBinding.inflate(layoutInflater)
        sheet.setContentView(form.root)

        addChoiceChips(form.categoryChips, reportCategories.map { (key, label) -> key to getString(label) }, viewModel.state.value.category) {
            viewModel.onCategory(it)
        }
        form.descriptionInput.setText(viewModel.state.value.description)
        form.descriptionInput.doAfterTextChanged { viewModel.onDescription(it?.toString().orEmpty()) }
        form.addPhotoButton.setOnClickListener {
            pickReportPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        form.removePhotoButton.setOnClickListener { viewModel.removePhoto() }
        // The sheet stays open while sending, so a missing location fix can be fixed and retried.
        form.submitButton.setOnClickListener { viewModel.submit() }
        sheet.setOnDismissListener {
            reportSheet = null
            reportForm = null
        }

        reportSheet = sheet
        reportForm = form
        renderReportForm(viewModel.state.value)
        sheet.show()
    }

    /** Shows the picked photo, and disables sending while a photo is prepared or the report is sent. */
    private fun renderReportForm(state: AlertsUiState) {
        val form = reportForm ?: return
        val photo = state.photo
        form.photoPreviewFrame.isVisible = photo != null || state.preparingPhoto
        form.photoProgress.isVisible = state.preparingPhoto
        if (photo != null && form.photoPreview.tag != photo.path) {
            form.photoPreview.tag = photo.path
            form.photoPreview.setImageURI(Uri.fromFile(photo))
        } else if (photo == null) {
            form.photoPreview.tag = null
            form.photoPreview.setImageDrawable(null)
        }
        form.removePhotoButton.isVisible = photo != null && !state.submitting
        form.addPhotoButton.isEnabled = !state.submitting && !state.preparingPhoto
        form.submitButton.isEnabled = !state.submitting && !state.preparingPhoto
        form.submitProgress.isVisible = state.submitting
    }

    override fun onDestroyView() {
        super.onDestroyView()
        reportSheet?.dismiss()
        reportSheet = null
        reportForm = null
        _binding = null
    }

    private companion object {
        const val TAG = "CommunityAlerts"
        const val PHOTO_MAX_DP = 400
    }
}

/** RecyclerView adapter for alert cards, each paired with its distance in km. */
private class AlertAdapter(
    private val distanceText: (Double) -> String,
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
        holder.binding.distance.setTextOrGone(km?.let(distanceText))
        holder.binding.confirmCount.text = context.getString(R.string.confirm_count, alert.confirmCount)
        holder.binding.description.setTextOrGone(alert.description)
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
