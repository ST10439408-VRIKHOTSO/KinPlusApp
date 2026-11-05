package za.co.kinplus.app.ui.screens.settings

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import za.co.kinplus.app.KinPlusApplication
import za.co.kinplus.app.data.local.SettingsStore
import za.co.kinplus.app.data.repository.CircleRepository
import za.co.kinplus.app.data.repository.UserRepository
import za.co.kinplus.app.domain.Circle
import za.co.kinplus.app.messaging.PushRegistration
import za.co.kinplus.app.util.LocaleManager
import za.co.kinplus.app.util.Resource
import javax.inject.Inject

/**
 * The app's Settings menu (Part 2 "settings menu" criterion): notification,
 * language and unit preferences, the location-sharing privacy control centre
 * (global pause + per-circle), and device-level app permissions — split out
 * of the Profile screen (which stays focused on identity and account
 * management) so Settings is its own reachable destination.
 *
 * Every switch does what it says: notifications register or remove this
 * phone's push token, units change how distances are shown on the Community
 * Alerts and Safe Zone screens, and the per circle switches show the sharing
 * state saved on the server.
 */
data class SettingsUiState(
    val useMetric: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val languageTag: String = "en",
    val globalPaused: Boolean = false,
    val circles: List<Circle> = emptyList(),
    /** Sharing per circle id. A circle missing from the map shows as sharing, the default. */
    val circleSharing: Map<String, Boolean> = emptyMap()
)

/** One-off problems for the screen to show as a short message. */
enum class SettingsProblem { NOTIFICATIONS, SHARING }

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
    private val userRepository: UserRepository,
    private val circleRepository: CircleRepository,
    private val pushRegistration: PushRegistration
) : ViewModel() {

    private val globalPausedFlow = MutableStateFlow(false)
    private val circleSharingFlow = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    private val _problems = MutableSharedFlow<SettingsProblem>(extraBufferCapacity = 4)
    val problems: SharedFlow<SettingsProblem> = _problems.asSharedFlow()

    val state: StateFlow<SettingsUiState> = combine(
        settingsStore.useMetric,
        settingsStore.notificationsEnabled,
        settingsStore.languageTag,
        circleRepository.circles,
        combine(globalPausedFlow, circleSharingFlow) { paused, sharing -> paused to sharing }
    ) { useMetric, notifications, lang, circles, (paused, sharing) ->
        SettingsUiState(
            useMetric = useMetric,
            notificationsEnabled = notifications,
            languageTag = lang,
            circles = circles,
            globalPaused = paused,
            circleSharing = sharing
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    init {
        viewModelScope.launch {
            val circles = (circleRepository.refreshCircles() as? Resource.Success)?.data.orEmpty()
            circleSharingFlow.value = circleRepository.mySharingStates(circles.map { it.id })
            val profile = userRepository.getProfile()
            globalPausedFlow.value = (profile as? Resource.Success)?.data?.sharingEnabled?.not() ?: false
            Log.d(TAG, "Loaded settings for ${circles.size} circle(s); global pause ${globalPausedFlow.value}")
        }
    }

    fun setMetric(v: Boolean) = viewModelScope.launch {
        settingsStore.setUseMetric(v)
        Log.i(TAG, "Units set to ${if (v) "metric" else "imperial"}")
    }

    /** Switches push notifications on or off for this phone. The switch goes back if that fails. */
    fun setNotifications(enabled: Boolean) = viewModelScope.launch {
        if (!pushRegistration.setEnabled(enabled)) {
            Log.w(TAG, "Notifications could not be switched ${if (enabled) "on" else "off"}")
            // Turning off always takes effect on this phone; only a failed "on" is undone.
            if (enabled) settingsStore.setNotificationsEnabled(false)
            _problems.tryEmit(SettingsProblem.NOTIFICATIONS)
        }
    }

    fun setLanguage(context: Context, tag: String) {
        LocaleManager.setLanguageTag(context, tag)
        // The notification channel name shown in the phone's settings follows the new language too.
        KinPlusApplication.createSafetyNotificationChannel(context)
        viewModelScope.launch {
            settingsStore.setLanguageTag(tag)
            userRepository.updateProfile(locale = tag)
        }
    }

    /** Global pause instantly hides the user's location everywhere (FR-15). */
    fun setGlobalPaused(paused: Boolean) {
        globalPausedFlow.value = paused
        Log.i(TAG, "Global location sharing pause set to $paused")
        viewModelScope.launch { userRepository.updateProfile(sharingEnabled = !paused) }
    }

    /** Changes sharing for one circle, showing the change at once and undoing it if the server refuses. */
    fun setCircleSharing(circleId: String, enabled: Boolean) = viewModelScope.launch {
        val before = circleSharingFlow.value
        circleSharingFlow.value = before + (circleId to enabled)
        if (circleRepository.setSharing(circleId, enabled) is Resource.Error) {
            circleSharingFlow.value = before
            _problems.tryEmit(SettingsProblem.SHARING)
        }
    }

    private companion object {
        const val TAG = "SettingsViewModel"
    }
}
