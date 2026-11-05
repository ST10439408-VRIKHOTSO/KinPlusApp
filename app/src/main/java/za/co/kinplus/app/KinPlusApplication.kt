package za.co.kinplus.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import za.co.kinplus.app.util.LocaleManager
import javax.inject.Inject

/**
 * Kin+ application entry point.
 *
 * Responsibilities:
 *  - Bootstraps Hilt dependency injection (@HiltAndroidApp).
 *  - Provides the WorkManager configuration so offline-sync workers can be
 *    injected with repositories (FR-29).
 *  - Creates the "safety" notification channel used by Firebase Cloud
 *    Messaging for real-time SOS / Safe Zone / Journey alerts (FR-30).
 *
 * Reference: Android Developers (2026) Dependency injection with Hilt. Available at:
 * https://developer.android.com/training/dependency-injection/hilt-android [Accessed 5 November 2026].
 */
@HiltAndroidApp
class KinPlusApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        createSafetyNotificationChannel(this)
        Log.i(TAG, "Kin+ application started")
    }

    companion object {
        const val SAFETY_CHANNEL_ID = "kinplus_safety"
        private const val TAG = "KinPlusApplication"

        /**
         * Creates the safety channel, or renames it when called again after the
         * in-app language changes (Android updates an existing channel's name
         * and description in place). Its importance stays as the user set it.
         */
        fun createSafetyNotificationChannel(context: Context) {
            val text = LocaleManager.localized(context)
            val channel = NotificationChannel(
                SAFETY_CHANNEL_ID,
                text.getString(R.string.channel_safety_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = text.getString(R.string.channel_safety_desc)
                enableVibration(true)
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
