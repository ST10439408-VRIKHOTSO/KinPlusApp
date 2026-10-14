package za.co.kinplus.app.ui.screens.journey

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
import za.co.kinplus.app.databinding.FragmentJourneyMonitorBinding
import za.co.kinplus.app.databinding.ItemJourneyBinding
import za.co.kinplus.app.domain.Journey
import za.co.kinplus.app.ui.common.collectWhileStarted
import za.co.kinplus.app.ui.common.popBackSafely
import za.co.kinplus.app.ui.common.setup

/** Journey monitor (screen 11). Journeys you are watching, with "confirm arrival" (FR-24). */
@AndroidEntryPoint
class JourneyMonitorFragment : Fragment() {

    private var _binding: FragmentJourneyMonitorBinding? = null
    private val binding get() = _binding!!
    private val viewModel: JourneyMonitorViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentJourneyMonitorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.topBar.setup(title = getString(R.string.journey_in_progress), onBack = { popBackSafely() })

        val adapter = JourneyAdapter { journey -> viewModel.confirmArrival(journey.id) }
        binding.journeyList.layoutManager = LinearLayoutManager(requireContext())
        binding.journeyList.adapter = adapter

        collectWhileStarted(viewModel.state) { state ->
            binding.arrivedBanner.isVisible = state.arrivedMessage
            binding.loading.isVisible = state.loading && state.journeys.isEmpty()
            binding.emptyText.isVisible = !state.loading && state.journeys.isEmpty()
            adapter.submitList(state.journeys)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/** RecyclerView adapter for watched journeys. */
private class JourneyAdapter(
    private val onArrive: (Journey) -> Unit
) : ListAdapter<Journey, JourneyAdapter.Holder>(Diff) {

    class Holder(val binding: ItemJourneyBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemJourneyBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val journey = getItem(position)
        val context = holder.itemView.context
        holder.binding.traveller.text = journey.travellerName
        holder.binding.destination.text = journey.destinationLabel
        holder.binding.eta.text = "${context.getString(R.string.eta)}: ${journey.etaAt}"
        holder.binding.arriveButton.setOnClickListener { onArrive(journey) }
    }

    private object Diff : DiffUtil.ItemCallback<Journey>() {
        override fun areItemsTheSame(oldItem: Journey, newItem: Journey) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Journey, newItem: Journey) = oldItem == newItem
    }
}
