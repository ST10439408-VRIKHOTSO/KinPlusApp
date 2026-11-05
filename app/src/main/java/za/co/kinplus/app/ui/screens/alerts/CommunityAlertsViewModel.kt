package za.co.kinplus.app.ui.screens.alerts

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import za.co.kinplus.app.data.local.SettingsStore
import za.co.kinplus.app.data.repository.AlertRepository
import za.co.kinplus.app.data.repository.ReportOutcome
import za.co.kinplus.app.data.repository.ReportPhotoRepository
import za.co.kinplus.app.domain.CommunityAlert
import za.co.kinplus.app.location.LocationProvider
import za.co.kinplus.app.util.Resource
import java.io.File
import javax.inject.Inject

data class AlertsUiState(
    val nearby: List<CommunityAlert> = emptyList(),
    val loading: Boolean = true,
    val category: String = "HAZARD",
    val description: String = "",
    /** The prepared photo for the report being written, if one was picked. */
    val photo: File? = null,
    val preparingPhoto: Boolean = false,
    val submitting: Boolean = false,
    /** The phone's position, or null when no location fix is available. */
    val lat: Double? = null,
    val lng: Double? = null,
    /** Settings > Units: distances in km, or in miles when false. */
    val useMetric: Boolean = true
)

/** One-off results for the screen to show. */
sealed interface AlertsEvent {
    data class Submitted(val outcome: ReportOutcome) : AlertsEvent
    data object NoLocation : AlertsEvent
    data object PhotoUnreadable : AlertsEvent
}

/**
 * Community Alerts (FR-25 to FR-27): nearby approved reports and the report
 * form, which can attach one photo that is uploaded to Azure Blob Storage.
 */
@HiltViewModel
class CommunityAlertsViewModel @Inject constructor(
    private val alertRepository: AlertRepository,
    private val photoRepository: ReportPhotoRepository,
    private val locationProvider: LocationProvider,
    settingsStore: SettingsStore
) : ViewModel() {

    private val _state = MutableStateFlow(AlertsUiState())
    val state: StateFlow<AlertsUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<AlertsEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<AlertsEvent> = _events.asSharedFlow()

    init {
        load()
        viewModelScope.launch {
            settingsStore.useMetric.collect { metric -> _state.update { it.copy(useMetric = metric) } }
        }
    }

    fun load() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        val fix = locationProvider.currentFix()
        // Without a fix the list is still loaded around the Pretoria campus, but no
        // distances are shown, because they would not be measured from the user.
        val res = alertRepository.nearby(fix?.lat ?: FALLBACK_LAT, fix?.lng ?: FALLBACK_LNG)
        _state.update {
            it.copy(loading = false, lat = fix?.lat, lng = fix?.lng, nearby = (res as? Resource.Success)?.data ?: emptyList())
        }
    }

    fun onCategory(v: String) = _state.update { it.copy(category = v) }
    fun onDescription(v: String) = _state.update { it.copy(description = v) }

    /** Prepares the picked image for upload and shows it in the form. */
    fun onPhotoPicked(uri: Uri) = viewModelScope.launch {
        _state.update { it.copy(preparingPhoto = true) }
        val file = photoRepository.prepare(uri)
        if (file == null) {
            // Keep any photo picked earlier and tell the user this one could not be used.
            _state.update { it.copy(preparingPhoto = false) }
            _events.tryEmit(AlertsEvent.PhotoUnreadable)
            return@launch
        }
        val previous = _state.value.photo
        _state.update { it.copy(preparingPhoto = false, photo = file) }
        photoRepository.discard(previous)
    }

    fun removePhoto() {
        val previous = _state.value.photo
        _state.update { it.copy(photo = null) }
        photoRepository.discard(previous)
    }

    /**
     * Sends the report from the user's current position. The description is
     * optional. A report needs a real location fix, so it is never placed at a
     * made up position; without one the user is asked to turn on location.
     */
    fun submit() {
        val s = _state.value
        if (s.submitting || s.preparingPhoto) return
        _state.update { it.copy(submitting = true) }
        viewModelScope.launch {
            val fix = locationProvider.currentFix()
            val lat = fix?.lat ?: s.lat
            val lng = fix?.lng ?: s.lng
            if (lat == null || lng == null) {
                Log.w(TAG, "Report not sent: no location fix")
                _state.update { it.copy(submitting = false) }
                _events.tryEmit(AlertsEvent.NoLocation)
                return@launch
            }
            val outcome = alertRepository.submit(s.category, s.description.trim(), lat, lng, s.photo)
            Log.i(TAG, "Report submitted: $outcome")
            _state.update { it.copy(submitting = false, description = "", photo = null) }
            _events.tryEmit(AlertsEvent.Submitted(outcome))
            load()
        }
    }

    fun confirm(id: String) = viewModelScope.launch { alertRepository.confirm(id); load() }

    /** Downloads a report's photo for the details dialog. */
    suspend fun loadPhoto(url: String, maxSidePx: Int): Bitmap? = photoRepository.load(url, maxSidePx)

    override fun onCleared() {
        // A photo picked for a report that was never sent is not kept.
        photoRepository.discard(_state.value.photo)
    }

    private companion object {
        const val TAG = "CommunityAlertsVM"

        // Rosebank College Pretoria campus, used only to list reports when the phone has no fix.
        const val FALLBACK_LAT = -25.7545
        const val FALLBACK_LNG = 28.2314
    }
}
