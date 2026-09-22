package za.co.kinplus.app.ui.screens.profile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import za.co.kinplus.app.R
import za.co.kinplus.app.auth.AuthManager
import za.co.kinplus.app.data.local.SettingsStore
import za.co.kinplus.app.data.repository.CircleRepository
import za.co.kinplus.app.data.repository.EmergencyInfoRepository
import za.co.kinplus.app.data.repository.LocalPhotoRepository
import za.co.kinplus.app.data.repository.UserRepository
import za.co.kinplus.app.domain.Circle
import za.co.kinplus.app.ui.components.AvatarImage
import za.co.kinplus.app.ui.components.IconBadge
import za.co.kinplus.app.ui.components.IconBadgeVariant
import za.co.kinplus.app.ui.components.KinTopAppBar
import za.co.kinplus.app.ui.theme.kinSwitchColors
import za.co.kinplus.app.ui.screens.auth.AuthDivider
import za.co.kinplus.app.ui.screens.auth.AuthMuted
import za.co.kinplus.app.util.LocaleManager
import za.co.kinplus.app.util.Resource
import javax.inject.Inject

data class ProfileUiState(
    val displayName: String = "",
    val email: String = "",
    val phone: String? = null,
    val photoPath: String? = null,
    val emergencyInfoPercent: Int = 0,
    val useMetric: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val languageTag: String = "en",
    val globalPaused: Boolean = false,
    val circles: List<Circle> = emptyList(),
    val perCircleSharing: Map<String, Boolean> = emptyMap()
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
    private val userRepository: UserRepository,
    private val authManager: AuthManager,
    private val circleRepository: CircleRepository,
    emergencyInfoRepository: EmergencyInfoRepository,
    localPhotoRepository: LocalPhotoRepository
) : ViewModel() {

    private val globalPausedFlow = kotlinx.coroutines.flow.MutableStateFlow(false)
    // The saved phone number lives on the Kin+ profile (set at registration or
    // a previous edit), not on the Firebase Auth user — reading it from Auth
    // here showed a blank number even after one had been saved.
    private val phoneFlow = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    val state: StateFlow<ProfileUiState> = combine(
        settingsStore.useMetric,
        settingsStore.notificationsEnabled,
        settingsStore.languageTag,
        emergencyInfoRepository.observe(),
        localPhotoRepository.userPhotoPath(),
        circleRepository.circles,
        globalPausedFlow,
        phoneFlow
    ) { values ->
        val metric = values[0] as Boolean
        val notifications = values[1] as Boolean
        val lang = values[2] as String
        val info = values[3] as za.co.kinplus.app.domain.EmergencyInfo
        val photoPath = values[4] as String?
        @Suppress("UNCHECKED_CAST") val circles = values[5] as List<Circle>
        val paused = values[6] as Boolean
        val phone = values[7] as String?
        ProfileUiState(
            displayName = authManager.currentDisplayName,
            email = authManager.currentEmail,
            phone = phone,
            photoPath = photoPath,
            emergencyInfoPercent = info.overallPercentComplete,
            useMetric = metric,
            notificationsEnabled = notifications,
            languageTag = lang,
            circles = circles,
            perCircleSharing = circles.associate { c -> c.id to true },
            globalPaused = paused
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileUiState())

    init {
        viewModelScope.launch {
            circleRepository.refreshCircles()
            val profile = userRepository.getProfile()
            globalPausedFlow.value = (profile as? Resource.Success)?.data?.sharingEnabled?.not() ?: false
            phoneFlow.value = (profile as? Resource.Success)?.data?.phone
        }
    }

    fun setMetric(v: Boolean) = viewModelScope.launch { settingsStore.setUseMetric(v) }
    fun setNotifications(v: Boolean) = viewModelScope.launch { settingsStore.setNotificationsEnabled(v) }

    fun setLanguage(context: Context, tag: String) {
        LocaleManager.setLanguageTag(context, tag)
        viewModelScope.launch {
            settingsStore.setLanguageTag(tag)
            userRepository.updateProfile(locale = tag)
        }
    }

    /** Global pause instantly hides the user's location everywhere (FR-15). */
    fun setGlobalPaused(paused: Boolean) {
        globalPausedFlow.value = paused
        viewModelScope.launch { userRepository.updateProfile(sharingEnabled = !paused) }
    }

    fun setCircleSharing(circleId: String, enabled: Boolean) = viewModelScope.launch {
        circleRepository.setSharing(circleId, enabled)
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
    onSignedOut: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        KinTopAppBar(title = stringResource(R.string.profile_title), onBack = onBack)

        ProfileHeader(state, onClick = onEditProfile)

        SectionLabel(stringResource(R.string.account_section))
        ProfileCard {
            ProfileRow(Icons.Filled.HealthAndSafety, stringResource(R.string.emergency_information),
                stringResource(R.string.percent_complete, state.emergencyInfoPercent), onClick = onOpenEmergencyInfo)
        }

        SectionLabel(stringResource(R.string.privacy_control_centre))
        PrivacyControlCentre(state, viewModel)

        SectionLabel(stringResource(R.string.preferences_section))
        ProfileCard {
            ListItem(
                leadingContent = { IconBadge(Icons.Filled.Notifications) },
                headlineContent = { Text(stringResource(R.string.notifications), fontWeight = FontWeight.Medium) },
                supportingContent = { Text(stringResource(R.string.notifications_sub), color = AuthMuted, fontSize = 13.sp) },
                trailingContent = {
                    Switch(checked = state.notificationsEnabled, onCheckedChange = viewModel::setNotifications, colors = kinSwitchColors())
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            HorizontalDivider(color = AuthDivider)
            ProfileRow(Icons.Filled.Language, stringResource(R.string.language), currentLanguageLabel(state.languageTag)) {
                showLanguageDialog = true
            }
            HorizontalDivider(color = AuthDivider)
            ListItem(
                leadingContent = { IconBadge(Icons.Filled.Straighten) },
                headlineContent = { Text(stringResource(R.string.units), fontWeight = FontWeight.Medium) },
                supportingContent = {
                    Text(if (state.useMetric) stringResource(R.string.units_metric) else stringResource(R.string.units_imperial), color = AuthMuted, fontSize = 13.sp)
                },
                trailingContent = {
                    Switch(checked = state.useMetric, onCheckedChange = viewModel::setMetric, colors = kinSwitchColors())
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }

        SectionLabel(stringResource(R.string.share_permissions_section))
        ProfileCard {
            ProfileRow(Icons.Filled.Share, stringResource(R.string.invite_friends), stringResource(R.string.invite_friends_sub)) {
                inviteFriends(context)
            }
            HorizontalDivider(color = AuthDivider)
            ProfileRow(Icons.Filled.VpnKey, stringResource(R.string.app_permissions), stringResource(R.string.app_permissions_sub)) {
                openAppPermissions(context)
            }
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

    if (showLanguageDialog) {
        ProfileLanguageDialog(
            current = state.languageTag,
            onSelect = { tag ->
                showLanguageDialog = false
                viewModel.setLanguage(context, tag)
                (context as? Activity)?.recreate()
            },
            onDismiss = { showLanguageDialog = false }
        )
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

private fun inviteFriends(context: Context) {
    context.startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, context.getString(R.string.invite_friends_message))
            },
            null
        )
    )
}

private fun openAppPermissions(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
    )
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
private fun PrivacyControlCentre(state: ProfileUiState, viewModel: ProfileViewModel) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp)) {
            Column {
                ListItem(
                    headlineContent = {
                        Text(stringResource(R.string.pause_all_sharing), fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.error)
                    },
                    supportingContent = { Text(stringResource(R.string.pause_all_sub), color = AuthMuted, fontSize = 13.sp) },
                    trailingContent = {
                        Switch(checked = state.globalPaused, onCheckedChange = viewModel::setGlobalPaused, colors = kinSwitchColors())
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                if (state.circles.isNotEmpty()) {
                    HorizontalDivider(color = AuthDivider)
                    Text(
                        stringResource(R.string.sharing_per_circle),
                        color = AuthMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
                    )
                    state.circles.forEachIndexed { index, circle ->
                        ListItem(
                            headlineContent = { Text(circle.name, fontWeight = FontWeight.Medium) },
                            trailingContent = {
                                Switch(
                                    checked = state.perCircleSharing[circle.id] ?: true,
                                    enabled = !state.globalPaused,
                                    onCheckedChange = { viewModel.setCircleSharing(circle.id, it) },
                                    colors = kinSwitchColors()
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        if (index != state.circles.lastIndex) HorizontalDivider(color = AuthDivider)
                    }
                }
            }
        }
    }
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

@Composable
private fun currentLanguageLabel(tag: String): String = when (tag) {
    "zu" -> stringResource(R.string.language_zulu)
    "nso" -> stringResource(R.string.language_sepedi)
    else -> stringResource(R.string.language_english)
}

@Composable
private fun ProfileLanguageDialog(current: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.choose_language)) },
        text = {
            Column {
                ProfileLanguageOption("en", stringResource(R.string.language_english), current, onSelect)
                ProfileLanguageOption("zu", stringResource(R.string.language_zulu), current, onSelect)
                ProfileLanguageOption("nso", stringResource(R.string.language_sepedi), current, onSelect)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun ProfileLanguageOption(tag: String, label: String, current: String, onSelect: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onSelect(tag) }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = current == tag, onClick = { onSelect(tag) })
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}
