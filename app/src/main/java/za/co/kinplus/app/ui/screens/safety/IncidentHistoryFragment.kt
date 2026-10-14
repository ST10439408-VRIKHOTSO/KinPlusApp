package za.co.kinplus.app.ui.screens.safety

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import za.co.kinplus.app.R
import za.co.kinplus.app.databinding.FragmentIncidentHistoryBinding
import za.co.kinplus.app.databinding.ItemIncidentBinding
import za.co.kinplus.app.domain.SosHistoryItem
import za.co.kinplus.app.ui.common.collectWhileStarted
import za.co.kinplus.app.ui.common.popBackSafely
import za.co.kinplus.app.ui.common.setTextOrGone
import za.co.kinplus.app.ui.common.setup
import java.text.DateFormat
import java.util.Date

/** Incident history (screen 14). Every SOS raised from this device, cached in Room. */
@AndroidEntryPoint
class IncidentHistoryFragment : Fragment() {

    private var _binding: FragmentIncidentHistoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: IncidentHistoryViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentIncidentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.topBar.setup(title = getString(R.string.incident_history_title), onBack = { popBackSafely() })

        val adapter = IncidentAdapter()
        binding.incidentList.layoutManager = LinearLayoutManager(requireContext())
        binding.incidentList.adapter = adapter

        collectWhileStarted(viewModel.history) { history ->
            binding.emptyText.isVisible = history.isEmpty()
            adapter.submitList(history)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

private fun statusLabel(context: Context, status: String): String = when (status) {
    "SENT" -> context.getString(R.string.incident_status_sent)
    "QUEUED" -> context.getString(R.string.incident_status_queued)
    else -> status
}

/** RecyclerView adapter for past SOS alerts. */
private class IncidentAdapter : ListAdapter<SosHistoryItem, IncidentAdapter.Holder>(Diff) {

    class Holder(val binding: ItemIncidentBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemIncidentBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = getItem(position)
        val context = holder.itemView.context
        holder.binding.status.text = statusLabel(context, item.status)
        holder.binding.date.text = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(item.raisedAt))
        holder.binding.message.setTextOrGone(item.message)
    }

    private object Diff : DiffUtil.ItemCallback<SosHistoryItem>() {
        override fun areItemsTheSame(oldItem: SosHistoryItem, newItem: SosHistoryItem) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: SosHistoryItem, newItem: SosHistoryItem) = oldItem == newItem
    }
}
