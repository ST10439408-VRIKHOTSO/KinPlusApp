package za.co.kinplus.app.ui.screens.circles

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
        run(onDone) { circleRepository.createCircle(name.trim(), description.ifBlank { null }) }
    }

    fun join(code: String, onDone: () -> Unit) {
        if (code.trim().length != 6) { _state.update { it.copy(error = "CODE") }; return }
        run(onDone) { circleRepository.joinCircle(code) }
    }

    private fun run(onDone: () -> Unit, block: suspend () -> Resource<*>) {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val res = block()) {
                is Resource.Success -> { _state.update { it.copy(loading = false) }; onDone() }
                is Resource.Error -> _state.update { it.copy(loading = false, error = res.message) }
                Resource.Loading -> Unit
            }
        }
    }
}
