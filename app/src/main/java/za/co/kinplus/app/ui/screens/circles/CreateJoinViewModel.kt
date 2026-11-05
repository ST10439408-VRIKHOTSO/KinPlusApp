package za.co.kinplus.app.ui.screens.circles

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import za.co.kinplus.app.data.repository.CircleRepository
import za.co.kinplus.app.util.Resource
import javax.inject.Inject

@HiltViewModel
class CreateJoinViewModel @Inject constructor(
    private val circleRepository: CircleRepository
) : ViewModel() {

    data class UiState(val loading: Boolean = false, val error: String? = null)
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun create(name: String, description: String, onDone: () -> Unit) {
        if (name.isBlank()) { _state.update { it.copy(error = "NAME") }; return }
        run("Create circle", onDone) { circleRepository.createCircle(name.trim(), description.ifBlank { null }) }
    }

    fun join(code: String, onDone: () -> Unit) {
        // Join codes are six characters (KinDomain.IsValidJoinCode on the API).
        if (code.trim().length != 6) { _state.update { it.copy(error = "CODE") }; return }
        run("Join circle", onDone) { circleRepository.joinCircle(code) }
    }

    private fun run(action: String, onDone: () -> Unit, block: suspend () -> Resource<*>) {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val res = block()) {
                is Resource.Success -> {
                    Log.i(TAG, "$action succeeded")
                    _state.update { it.copy(loading = false) }
                    onDone()
                }
                is Resource.Error -> {
                    Log.w(TAG, "$action failed: ${res.message}")
                    _state.update { it.copy(loading = false, error = res.message) }
                }
                Resource.Loading -> Unit
            }
        }
    }

    private companion object {
        const val TAG = "CreateJoinViewModel"
    }
}
