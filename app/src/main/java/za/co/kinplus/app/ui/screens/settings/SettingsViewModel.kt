package za.co.kinplus.app.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import za.co.kinplus.app.data.local.SettingsStore
import za.co.kinplus.app.data.repository.CircleRepository
import za.co.kinplus.app.data.repository.UserRepository
import za.co.kinplus.app.domain.Circle
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
