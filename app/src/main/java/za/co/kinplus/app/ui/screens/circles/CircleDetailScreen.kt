package za.co.kinplus.app.ui.screens.circles

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import za.co.kinplus.app.auth.AuthManager
import za.co.kinplus.app.data.repository.CircleRepository
import za.co.kinplus.app.data.repository.LocalPhotoRepository
import za.co.kinplus.app.domain.CircleDetail
import za.co.kinplus.app.ui.components.AvatarImage
import za.co.kinplus.app.ui.components.EditableAvatar
import za.co.kinplus.app.ui.components.KinTopAppBar
import za.co.kinplus.app.ui.components.LoadingBox
import za.co.kinplus.app.ui.screens.auth.AuthDivider
import za.co.kinplus.app.ui.screens.auth.AuthMuted
import za.co.kinplus.app.ui.theme.kinSwitchColors
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircleDetailScreen(
    circleId: String,
    onBack: () -> Unit,
    viewModel: CircleDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val photoPath by remember(circleId) { viewModel.photoPath(circleId) }.collectAsState(initial = null)
    val context = LocalContext.current
    LaunchedEffect(circleId) { viewModel.load(circleId) }

    LaunchedEffect(state.leaveError) {
        state.leaveError?.let { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
            viewModel.leaveErrorShown()
        }
    }

    val pickPhotoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { viewModel.pickPhoto(circleId, it) } }

    Scaffold(
        topBar = {
            KinTopAppBar(title = state.detail?.name ?: stringResource(R.string.circles_title), onBack = onBack)
        }
    ) { padding ->
        when {
            state.loading -> LoadingBox(Modifier.padding(padding))
            state.detail != null -> {
                val detail = state.detail!!
                LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                    item {
                        Column(Modifier.padding(16.dp)) {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                EditableAvatar(
                                    photoPath = photoPath,
                                    fallbackText = detail.name,
                                    onPick = { pickPhotoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                    size = 88.dp
                                )
                            }
                            Spacer(Modifier.height(16.dp))

                            // Join code pill, shown so members can invite others (FR-09).
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        stringResource(R.string.join_code) + " · " + detail.joinCode,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        fontWeight = FontWeight.SemiBold, fontSize = 14.sp
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(
                                    onClick = { shareInviteCode(context, detail.joinCode) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.invite_friends))
                                }
                                OutlinedButton(
                                    onClick = { copyInviteCode(context, detail.joinCode) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.share_code))
                                }
                            }
                        }

                        CircleDetailSectionLabel(stringResource(R.string.members) + " · " + detail.members.size)
                        Column(Modifier.padding(horizontal = 16.dp)) {
                            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
                                Column {
                                    detail.members.forEachIndexed { index, member ->
                                        MemberRow(member, isMe = member.uid == state.myUid) {
                                            viewModel.toggleSharing(circleId, it)
                                        }
                                        if (index != detail.members.lastIndex) HorizontalDivider(color = AuthDivider)
                                    }
                                }
                            }
                        }

                        CircleDetailSectionLabel(stringResource(R.string.circle_section))
                        Column(Modifier.padding(horizontal = 16.dp)) {
                            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
                                ListItem(
                                    headlineContent = {
                                        Text(stringResource(R.string.leave_circle), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                                    },
                                    trailingContent = {
                                        if (state.leaving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                    },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    modifier = Modifier.clickable(enabled = !state.leaving) { viewModel.leave(circleId, onBack) }
                                )
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
            else -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(state.error ?: stringResource(R.string.error_generic))
            }
        }
    }
}

private fun shareInviteCode(context: Context, code: String) {
    val text = context.getString(R.string.invite_friends_message) + " " + code
    context.startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) },
            null
        )
    )
}

private fun copyInviteCode(context: Context, code: String) {
    val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
    clipboard?.setPrimaryClip(ClipData.newPlainText("Kin+ join code", code))
    android.widget.Toast.makeText(context, context.getString(R.string.code_copied), android.widget.Toast.LENGTH_SHORT).show()
}

@Composable
private fun CircleDetailSectionLabel(text: String) {
    Text(
        text,
        color = AuthMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun MemberRow(member: za.co.kinplus.app.domain.CircleMember, isMe: Boolean, onToggleSharing: (Boolean) -> Unit) {
    ListItem(
        leadingContent = { AvatarImage(photoPath = null, fallbackText = member.displayName, size = 40.dp) },
        headlineContent = { Text(member.displayName, fontWeight = FontWeight.Medium) },
        supportingContent = {
            RoleBadge(isOwner = member.isOwner, sharingEnabled = member.sharingEnabled)
        },
        trailingContent = {
            // A member can toggle their own sharing within this circle (FR-15).
            if (isMe) {
                Switch(checked = member.sharingEnabled, onCheckedChange = onToggleSharing, colors = kinSwitchColors())
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun RoleBadge(isOwner: Boolean, sharingEnabled: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (isOwner) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                stringResource(if (isOwner) R.string.owner else R.string.member).uppercase(),
                fontSize = 10.sp, fontWeight = FontWeight.Bold,
                color = if (isOwner) MaterialTheme.colorScheme.primary else AuthMuted,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            if (sharingEnabled) stringResource(R.string.sharing_on) else stringResource(R.string.sharing_off),
            color = AuthMuted, fontSize = 12.sp
        )
    }
}
