package za.co.kinplus.app.ui.screens.profile

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import za.co.kinplus.app.R
import za.co.kinplus.app.auth.AuthManager
import za.co.kinplus.app.data.repository.EmergencyInfoRepository
import za.co.kinplus.app.data.repository.LocalPhotoRepository
import za.co.kinplus.app.data.repository.UserRepository
import za.co.kinplus.app.ui.components.AvatarImage
import za.co.kinplus.app.ui.components.IconBadge
import za.co.kinplus.app.ui.components.IconBadgeVariant
import za.co.kinplus.app.ui.components.KinTopAppBar
import za.co.kinplus.app.ui.screens.auth.AuthDivider
import za.co.kinplus.app.ui.screens.auth.AuthMuted
import za.co.kinplus.app.util.Resource
import javax.inject.Inject

data class ProfileUiState(
    val displayName: String = "",
    val email: String = "",
    val phone: String? = null,
    val photoPath: String? = null,
    val emergencyInfoPercent: Int = 0
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authManager: AuthManager,
    emergencyInfoRepository: EmergencyInfoRepository,
    localPhotoRepository: LocalPhotoRepository
) : ViewModel() {

    // The saved phone number lives on the Kin+ profile (set at registration or
    // a previous edit), not on the Firebase Auth user — reading it from Auth
    // here showed a blank number even after one had been saved.
    private val phoneFlow = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    val state: StateFlow<ProfileUiState> = combine(
        emergencyInfoRepository.observe(),
        localPhotoRepository.userPhotoPath(),
        phoneFlow
    ) { info, photoPath, phone ->
        ProfileUiState(
            displayName = authManager.currentDisplayName,
            email = authManager.currentEmail,
            phone = phone,
            photoPath = photoPath,
            emergencyInfoPercent = info.overallPercentComplete
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileUiState())

    init {
        viewModelScope.launch {
            val profile = userRepository.getProfile()
            phoneFlow.value = (profile as? Resource.Success)?.data?.phone
        }
    }

    fun signOut() = authManager.signOut()

    fun deleteAccount(onDone: () -> Unit) = viewModelScope.launch {
        userRepository.deleteAccount()
        authManager.signOut()
        onDone()
    }
}

@Composable
fun ProfileScreen(
    onEditProfile: () -> Unit,
    onOpenEmergencyInfo: () -> Unit,
    onOpenSettings: () -> Unit,
    onSignedOut: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        KinTopAppBar(title = stringResource(R.string.profile_title), onBack = onBack)

        ProfileHeader(state, onClick = onEditProfile)

        SectionLabel(stringResource(R.string.account_section))
        ProfileCard {
            ProfileRow(Icons.Filled.HealthAndSafety, stringResource(R.string.emergency_information),
                stringResource(R.string.percent_complete, state.emergencyInfoPercent), onClick = onOpenEmergencyInfo)
            HorizontalDivider(color = AuthDivider)
            ProfileRow(Icons.Filled.Settings, stringResource(R.string.settings_title), stringResource(R.string.settings_row_sub), onClick = onOpenSettings)
        }

        SectionLabel(stringResource(R.string.support_section))
        ProfileCard {
            ProfileRow(Icons.Filled.Email, stringResource(R.string.contact_support), stringResource(R.string.contact_support_sub)) {
                contactSupport(context)
            }
        }

        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = { viewModel.signOut(); onSignedOut() },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text(stringResource(R.string.sign_out))
        }
        TextButton(onClick = { showDeleteDialog = true },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(stringResource(R.string.delete_account), color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_account)) },
            text = { Text(stringResource(R.string.delete_account_confirm)) },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; viewModel.deleteAccount(onSignedOut) }) {
                    Text(stringResource(R.string.delete_account), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

private fun contactSupport(context: Context) {
    context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")))
}

@Composable
private fun ProfileHeader(state: ProfileUiState, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarImage(photoPath = state.photoPath, fallbackText = state.displayName, size = 56.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(state.displayName.ifBlank { stringResource(R.string.account) }, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(state.phone?.takeIf { it.isNotBlank() } ?: state.email, color = AuthMuted, fontSize = 13.sp)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = AuthMuted)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text, color = AuthMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun ProfileCard(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp)) {
            Column(content = content)
        }
    }
}

@Composable
private fun ProfileRow(
    icon: ImageVector, title: String, subtitle: String,
    variant: IconBadgeVariant = IconBadgeVariant.NEUTRAL,
    onClick: () -> Unit
) {
    ListItem(
        leadingContent = { IconBadge(icon, variant) },
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(subtitle, color = AuthMuted, fontSize = 13.sp) },
        trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = AuthMuted) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick)
    )
}

