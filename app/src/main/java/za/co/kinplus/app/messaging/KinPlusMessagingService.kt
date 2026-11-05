package za.co.kinplus.app.messaging

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import za.co.kinplus.app.KinPlusApplication
import za.co.kinplus.app.MainActivity
import za.co.kinplus.app.R
import za.co.kinplus.app.auth.AuthManager
import za.co.kinplus.app.data.repository.UserRepository
import za.co.kinplus.app.util.LocaleManager
import za.co.kinplus.app.util.Resource
import javax.inject.Inject

/**
 * Receives real-time push notifications (FR-30) for SOS, Safe Zone and Safe
 * Journey events. A tapped notification carries a "deepLink" data value
 * (kinplus://...) so the app opens the relevant safety screen directly.
 * Nothing is registered or shown while the user has switched notifications
 * off in Settings.
 *
 * Reference: Firebase (2026) Receive messages in Android apps. Available at:
 * https://firebase.google.com/docs/cloud-messaging/android/receive
 * [Accessed 5 November 2026].
 */
@AndroidEntryPoint
class KinPlusMessagingService : FirebaseMessagingService() {

    @Inject lateinit var userRepository: UserRepository
    @Inject lateinit var authManager: AuthManager
    @Inject lateinit var pushRegistration: PushRegistration

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Register a refreshed device token with the API so this device can be reached. */
    override fun onNewToken(token: String) {
        Log.i(TAG, "New FCM token received")
        if (!authManager.isSignedIn) return
        scope.launch {
            if (!pushRegistration.isEnabled()) {
                Log.i(TAG, "Notifications are off in Settings; token not registered")
                return@launch
            }
            val result = userRepository.syncProfile(
                displayName = authManager.currentDisplayName,
                phone = null,
                fcmToken = token,
                locale = LocaleManager.getLanguageTag(applicationContext)
            )
            if (result is Resource.Error) Log.w(TAG, "Token registration failed: ${result.message}")
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // FCM calls this on a background thread, so a short blocking read of the setting is safe.
        if (!runBlocking { pushRegistration.isEnabled() }) {
            Log.i(TAG, "Push (${message.data["type"]}) ignored: notifications are off in Settings")
            return
        }
        val title = message.notification?.title
            ?: message.data["title"]
            ?: getString(R.string.app_name)
        val body = message.notification?.body ?: message.data["body"] ?: ""
        val deepLink = message.data["deepLink"] ?: "kinplus://home"

        Log.i(TAG, "Push received (${message.data["type"]}) -> $deepLink")
        showNotification(title, body, deepLink)
    }

    private fun showNotification(title: String, body: String, deepLink: String) {
        // Tapping opens MainActivity with the deep link so navigation can route it.
        val intent = Intent(this, MainActivity::class.java).apply {
            data = Uri.parse(deepLink)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, deepLink.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, KinPlusApplication.SAFETY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // POST_NOTIFICATIONS is requested at runtime in MainActivity; guard here.
        try {
            NotificationManagerCompat.from(this).notify(deepLink.hashCode(), notification)
        } catch (se: SecurityException) {
            Log.w(TAG, "Notification permission not granted", se)
        }
    }

    private companion object {
        const val TAG = "KinPlusMessaging"
    }
}
