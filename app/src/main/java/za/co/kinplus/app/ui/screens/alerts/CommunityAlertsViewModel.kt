package za.co.kinplus.app.ui.screens.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import za.co.kinplus.app.data.repository.AlertRepository
import za.co.kinplus.app.domain.CommunityAlert
import za.co.kinplus.app.location.LocationProvider
import za.co.kinplus.app.util.Resource
import javax.inject.Inject

data class AlertsUiState(
    val nearby: List<CommunityAlert> = emptyList(),
    val loading: Boolean = true,
    val category: String = "HAZARD",
    val description: String = "",
    val submitting: Boolean = false,
    val submitted: Boolean = false,
    val lat: Double? = null,
    val lng: Double? = null
)

@HiltViewModel
class CommunityAlertsViewModel @Inject constructor(
    private val alertRepository: AlertRepository,
    private val locationProvider: LocationProvider
) : ViewModel() {

    private val _state = MutableStateFlow(AlertsUiState())
    val state: StateFlow<AlertsUiState> = _state.asStateFlow()

    init { load() }

    fun load() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        // Default to Pretoria campus if no fix is available, so the list still loads.
        val fix = locationProvider.currentFix()
        val lat = fix?.lat ?: -25.7545
        val lng = fix?.lng ?: 28.2314
        val res = alertRepository.nearby(lat, lng)
        _state.update {
            it.copy(loading = false, lat = lat, lng = lng, nearby = (res as? Resource.Success)?.data ?: emptyList())
        }
    }

    fun onCategory(v: String) = _state.update { it.copy(category = v) }
    fun onDescription(v: String) = _state.update { it.copy(description = v) }

    fun submit() {
        val s = _state.value
        if (s.description.isBlank()) return
        _state.update { it.copy(submitting = true) }
        viewModelScope.launch {
            alertRepository.submit(s.category, s.description.trim(), s.lat ?: -25.7545, s.lng ?: 28.2314, null)
            _state.update { it.copy(submitting = false, submitted = true, description = "") }
            load()
        }
    }

    fun confirm(id: String) = viewModelScope.launch { alertRepository.confirm(id); load() }
}
