package za.co.kinplus.app.ui.screens.safety

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import za.co.kinplus.app.R
import za.co.kinplus.app.data.repository.SosRepository
import za.co.kinplus.app.domain.SosHistoryItem
import za.co.kinplus.app.ui.components.EmptyState
import za.co.kinplus.app.ui.components.KinTopAppBar
import za.co.kinplus.app.ui.screens.auth.AuthMuted
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class IncidentHistoryViewModel @Inject constructor(
    sosRepository: SosRepository
) : ViewModel() {
    val history: StateFlow<List<SosHistoryItem>> =
        sosRepository.history.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@Composable
fun IncidentHistoryScreen(
    onBack: () -> Unit,
    viewModel: IncidentHistoryViewModel = hiltViewModel()
) {
    val history by viewModel.history.collectAsState()

    Column(Modifier.fillMaxSize()) {
        KinTopAppBar(title = stringResource(R.string.incident_history_title), onBack = onBack)

        if (history.isEmpty()) {
            EmptyState(stringResource(R.string.no_incidents_yet))
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(history) { item -> IncidentRow(item) }
            }
        }
    }
}

@Composable
private fun IncidentRow(item: SosHistoryItem) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.History, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(statusLabel(item.status), fontWeight = FontWeight.SemiBold)
                Text(
                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(item.raisedAt)),
                    color = AuthMuted, style = MaterialTheme.typography.bodySmall
                )
                item.message?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = AuthMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun statusLabel(status: String): String = when (status) {
    "SENT" -> stringResource(R.string.incident_status_sent)
    "QUEUED" -> stringResource(R.string.incident_status_queued)
    else -> status
}
