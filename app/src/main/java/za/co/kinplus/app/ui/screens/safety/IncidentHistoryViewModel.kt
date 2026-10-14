package za.co.kinplus.app.ui.screens.safety

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import za.co.kinplus.app.data.repository.SosRepository
import za.co.kinplus.app.domain.SosHistoryItem
import javax.inject.Inject

@HiltViewModel
class IncidentHistoryViewModel @Inject constructor(
    sosRepository: SosRepository
) : ViewModel() {
    val history: StateFlow<List<SosHistoryItem>> =
        sosRepository.history.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
