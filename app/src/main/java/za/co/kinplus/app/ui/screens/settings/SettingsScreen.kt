package za.co.kinplus.app.ui.screens.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import za.co.kinplus.app.data.local.SettingsStore
import za.co.kinplus.app.data.repository.CircleRepository
import za.co.kinplus.app.data.repository.UserRepository
import za.co.kinplus.app.domain.Circle
import za.co.kinplus.app.ui.components.IconBadge
import za.co.kinplus.app.ui.components.KinTopAppBar
import za.co.kinplus.app.ui.screens.auth.AuthDivider
import za.co.kinplus.app.ui.screens.auth.AuthMuted
import za.co.kinplus.app.ui.theme.kinSwitchColors
import za.co.kinplus.app.util.LocaleManager
import za.co.kinplus.app.util.Resource
import javax.inject.Inject

/**
 * The app's Settings menu (Part 2 "settings menu" criterion): notification,
 * language and unit preferences, the location-sharing privacy control centre
 * (global pause + per-circle), and device-level app permissions — split out
 * of the Profile screen (which stays focused on identity and account
 * management) so Settings is its own reachable destination.
 */
data class SettingsUiState(
    val useMetric: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val languageTag: String = "en",
    val globalPaused: Boolean = false,
    val circles: List<Circle> = emptyList()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
    private val userRepository: UserRepository,
    private val circleRepository: CircleRepository
) : ViewModel() {

    private val globalPausedFlow = MutableStateFlow(false)

    val state: StateFlow<SettingsUiState> = combine(
        settingsStore.useMetric,
        settingsStore.notificationsEnabled,
        settingsStore.languageTag,
        circleRepository.circles,
        globalPausedFlow
    ) { useMetric, notifications, lang, circles, paused ->
        SettingsUiState(
            useMetric = useMetric,
            notificationsEnabled = notifications,
            languageTag = lang,
            circles = circles,
            globalPaused = paused
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    init {
        viewModelScope.launch {
            circleRepository.refreshCircles()
            val profile = userRepository.getProfile()
            globalPausedFlow.value = (profile as? Resource.Success)?.data?.sharingEnabled?.not() ?: false
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
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showLanguageDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        KinTopAppBar(title = stringResource(R.string.settings_title), onBack = onBack)

        SettingsSectionLabel(stringResource(R.string.privacy_control_centre))
        PrivacyControlCentre(state, viewModel)

        SettingsSectionLabel(stringResource(R.string.preferences_section))
        SettingsCard {
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
            SettingsRow(Icons.Filled.Language, stringResource(R.string.language), currentLanguageLabel(state.languageTag)) {
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

        SettingsSectionLabel(stringResource(R.string.share_permissions_section))
        SettingsCard {
            SettingsRow(Icons.Filled.Share, stringResource(R.string.invite_friends), stringResource(R.string.invite_friends_sub)) {
                inviteFriends(context)
            }
            HorizontalDivider(color = AuthDivider)
            SettingsRow(Icons.Filled.VpnKey, stringResource(R.string.app_permissions), stringResource(R.string.app_permissions_sub)) {
                openAppPermissions(context)
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    if (showLanguageDialog) {
        SettingsLanguageDialog(
            current = state.languageTag,
            onSelect = { tag ->
                showLanguageDialog = false
                viewModel.setLanguage(context, tag)
                (context as? Activity)?.recreate()
            },
            onDismiss = { showLanguageDialog = false }
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
        Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
    )
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text, color = AuthMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun PrivacyControlCentre(state: SettingsUiState, viewModel: SettingsViewModel) {
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
                                    checked = true,
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
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp)) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        leadingContent = { IconBadge(icon) },
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
private fun SettingsLanguageDialog(current: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.choose_language)) },
        text = {
            Column {
                SettingsLanguageOption("en", stringResource(R.string.language_english), current, onSelect)
                SettingsLanguageOption("zu", stringResource(R.string.language_zulu), current, onSelect)
                SettingsLanguageOption("nso", stringResource(R.string.language_sepedi), current, onSelect)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun SettingsLanguageOption(tag: String, label: String, current: String, onSelect: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onSelect(tag) }.padding(vertical = 12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        RadioButton(selected = current == tag, onClick = { onSelect(tag) })
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}
