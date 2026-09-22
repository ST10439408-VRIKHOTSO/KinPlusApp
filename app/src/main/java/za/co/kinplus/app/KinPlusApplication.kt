package za.co.kinplus.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
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
        createSafetyNotificationChannel()
        Log.i(TAG, "Kin+ application started")
    }

    private fun createSafetyNotificationChannel() {
        val channel = NotificationChannel(
            SAFETY_CHANNEL_ID,
            getString(R.string.channel_safety_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.channel_safety_desc)
            enableVibration(true)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val SAFETY_CHANNEL_ID = "kinplus_safety"
        private const val TAG = "KinPlusApplication"
    }
}
