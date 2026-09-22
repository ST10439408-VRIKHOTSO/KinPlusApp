package za.co.kinplus.app.ui.screens.circles

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import za.co.kinplus.app.R
import za.co.kinplus.app.data.repository.CircleRepository
import za.co.kinplus.app.ui.components.KinPrimaryButton
import za.co.kinplus.app.ui.components.KinTextField
import za.co.kinplus.app.ui.components.KinTopAppBar
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateJoinCircleScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: CreateJoinViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            KinTopAppBar(title = stringResource(R.string.create_or_join_circle), onBack = onBack)
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.create)) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.join)) })
            }
            Spacer(Modifier.height(24.dp))

            if (tab == 0) {
                KinTextField(name, { name = it }, stringResource(R.string.circle_name))
                Spacer(Modifier.height(12.dp))
                KinTextField(description, { description = it }, stringResource(R.string.description_optional),
                    singleLine = false)
                Spacer(Modifier.height(20.dp))
                KinPrimaryButton(stringResource(R.string.create_circle),
                    { viewModel.create(name, description, onDone) }, loading = state.loading)
            } else {
                KinTextField(code, { code = it.uppercase() }, stringResource(R.string.enter_join_code))
                Spacer(Modifier.height(20.dp))
                KinPrimaryButton(stringResource(R.string.join_circle),
                    { viewModel.join(code, onDone) }, loading = state.loading)
            }

            state.error?.let {
                Spacer(Modifier.height(12.dp))
                Text(
                    when (it) {
                        "NAME" -> stringResource(R.string.error_name_required)
                        "CODE" -> stringResource(R.string.enter_join_code)
                        else -> it
                    },
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
