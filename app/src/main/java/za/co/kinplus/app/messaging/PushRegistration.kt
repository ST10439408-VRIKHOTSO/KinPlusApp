package za.co.kinplus.app.messaging

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import za.co.kinplus.app.auth.AuthManager
import za.co.kinplus.app.data.local.SettingsStore
import za.co.kinplus.app.data.repository.UserRepository
import za.co.kinplus.app.util.LocaleManager
import za.co.kinplus.app.util.Resource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Switches real-time push notifications on or off for this phone (Settings >
 * Notifications, FR-30 and FR-32).
 *
 * Off: the Firebase Cloud Messaging registration token is deleted and
 * automatic token creation is paused, so the server can no longer reach this
 * phone, even while the app is closed. On: a new token is created and
 * registered with the Kin+ API through users/sync.
 *
 * Reference: Firebase (2026) Best practices for FCM registration
 * management. Available at: https://firebase.google.com/docs/cloud-messaging/manage-tokens
 * [Accessed 5 November 2026].
 */
@Singleton
class PushRegistration @Inject constructor(
    private val messaging: FirebaseMessaging,
    private val settingsStore: SettingsStore,
    private val userRepository: UserRepository,
    private val authManager: AuthManager,
    @ApplicationContext private val context: Context
) {
    /** Whether the user wants push notifications on this phone. */
    suspend fun isEnabled(): Boolean = settingsStore.notificationsEnabled.first()

    /** This phone's FCM token, or null while notifications are switched off or the token is unavailable. */
    suspend fun tokenIfEnabled(): String? {
        if (!isEnabled()) {
            Log.i(TAG, "Notifications are off, so no device token is registered")
            return null
        }
        return runCatching { messaging.token.await() }
            .onFailure { Log.w(TAG, "Could not get the FCM token", it) }
            .getOrNull()
    }

    /** Saves the choice and registers or removes this phone's token. Returns false if the change did not fully apply. */
    suspend fun setEnabled(enabled: Boolean): Boolean {
        settingsStore.setNotificationsEnabled(enabled)
        messaging.isAutoInitEnabled = enabled
        return if (enabled) {
            val token = tokenIfEnabled() ?: return false
            val result = userRepository.syncProfile(
                displayName = authManager.currentDisplayName,
                phone = null,
                fcmToken = token,
                locale = LocaleManager.getLanguageTag(context)
            )
            Log.i(TAG, "Notifications switched on, token registered: ${result is Resource.Success}")
            result is Resource.Success
        } else {
            withTimeoutOrNull(TOKEN_TIMEOUT_MS) {
                runCatching { messaging.deleteToken().await() }
                    .onSuccess { Log.i(TAG, "Notifications switched off, device token deleted") }
                    .onFailure { Log.w(TAG, "Could not delete the FCM token", it) }
                    .isSuccess
            } ?: false
        }
    }

    /**
     * On sign out: deletes this phone's token so the signed out account's alerts
     * stop arriving here. The user's on or off choice is kept, and the next sign
     * in registers a new token.
     */
    suspend fun unregisterDevice() {
        // Bounded so signing out without signal never hangs on the FCM server.
        val deleted = withTimeoutOrNull(TOKEN_TIMEOUT_MS) {
            runCatching { messaging.deleteToken().await() }
                .onFailure { Log.w(TAG, "Could not delete the device token on sign out", it) }
                .isSuccess
        }
        Log.i(TAG, "Device token deleted on sign out: ${deleted == true}")
    }

    private companion object {
        const val TAG = "PushRegistration"
        const val TOKEN_TIMEOUT_MS = 5_000L
    }
}
