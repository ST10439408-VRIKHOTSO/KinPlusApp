package za.co.kinplus.app.ui.screens.circles

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import za.co.kinplus.app.auth.AuthManager
import za.co.kinplus.app.data.repository.CircleRepository
import za.co.kinplus.app.data.repository.LocalPhotoRepository
import za.co.kinplus.app.domain.CircleDetail
import za.co.kinplus.app.util.Resource
import javax.inject.Inject

data class CircleDetailUiState(
    val detail: CircleDetail? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val myUid: String = "",
    val leaving: Boolean = false,
    val leaveError: String? = null
)

@HiltViewModel
class CircleDetailViewModel @Inject constructor(
    private val circleRepository: CircleRepository,
    private val authManager: AuthManager,
    private val localPhotoRepository: LocalPhotoRepository,
    firebaseAuth: com.google.firebase.auth.FirebaseAuth
) : ViewModel() {

    private val _state = MutableStateFlow(CircleDetailUiState(myUid = firebaseAuth.currentUser?.uid ?: ""))
    val state: StateFlow<CircleDetailUiState> = _state.asStateFlow()

    fun photoPath(circleId: String) = localPhotoRepository.circlePhotoPath(circleId)
    fun pickPhoto(circleId: String, uri: Uri) = viewModelScope.launch { localPhotoRepository.setCirclePhoto(circleId, uri) }

    fun load(circleId: String) = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        when (val res = circleRepository.getCircleDetail(circleId)) {
            is Resource.Success -> _state.update { it.copy(detail = res.data, loading = false) }
            is Resource.Error -> _state.update { it.copy(error = res.message, loading = false) }
            Resource.Loading -> Unit
        }
    }

    fun toggleSharing(circleId: String, enabled: Boolean) = viewModelScope.launch {
        circleRepository.setSharing(circleId, enabled)
        load(circleId)
    }

    fun leave(circleId: String, onLeft: () -> Unit) = viewModelScope.launch {
        _state.update { it.copy(leaving = true, leaveError = null) }
        when (val res = circleRepository.leaveOrRemove(circleId, _state.value.myUid)) {
            is Resource.Success -> onLeft()
            is Resource.Error -> _state.update { it.copy(leaving = false, leaveError = res.message) }
            Resource.Loading -> Unit
        }
    }

    fun leaveErrorShown() = _state.update { it.copy(leaveError = null) }
}
