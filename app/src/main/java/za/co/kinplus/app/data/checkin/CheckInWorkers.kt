package za.co.kinplus.app.data.checkin

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import za.co.kinplus.app.KinPlusApplication
import za.co.kinplus.app.MainActivity
import za.co.kinplus.app.R
import za.co.kinplus.app.data.repository.CircleRepository
import za.co.kinplus.app.data.repository.SosRepository
import za.co.kinplus.app.location.LocationProvider

/**
 * Fires when the check in interval runs out. Shows a high priority "Are you OK?"
 * notification with an "I'm OK" action, then arms the escalation worker.
 */
@HiltWorker
class CheckInPromptWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        Log.i(TAG, "Check in due, prompting user")

        val okIntent = Intent(context, CheckInReceiver::class.java).setAction(CheckInReceiver.ACTION_IM_OK)
        val okPending = PendingIntent.getBroadcast(
            context, 0, okIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openPending = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val body = context.getString(R.string.checkin_notif_body, CheckInScheduler.GRACE_MINUTES.toInt())

        val notification = NotificationCompat.Builder(context, KinPlusApplication.SAFETY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.checkin_notif_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setContentIntent(openPending)
            .addAction(0, context.getString(R.string.checkin_im_ok), okPending)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(CheckInScheduler.NOTIFICATION_ID, notification)
        } catch (se: SecurityException) {
            // Without notification permission the user cannot answer, so still arm the SOS.
            Log.w(TAG, "Notification permission not granted", se)
        }

        CheckInScheduler.armEscalation(context)
        return Result.success()
    }

    private companion object {
        const val TAG = "CheckInPromptWorker"
    }
}

/**
 * Fires when the grace period ends without an "I'm OK". Raises an SOS to every
 * circle the user belongs to, using the last location fix when one is available.
 */
@HiltWorker
class CheckInEscalateWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val sosRepository: SosRepository,
    private val circleRepository: CircleRepository,
    private val locationProvider: LocationProvider
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        if (CheckInScheduler.dueAt(context) == null) {
            Log.i(TAG, "Check in was stopped before escalation, nothing to do")
            return Result.success()
        }

        Log.w(TAG, "Check in missed, raising SOS")
        val fix = runCatching { locationProvider.currentFix() ?: locationProvider.lastKnownFix() }.getOrNull()
        val circleIds = runCatching { circleRepository.circles.first().map { it.id } }.getOrDefault(emptyList())

        sosRepository.raiseSos(
            lat = fix?.lat,
            lng = fix?.lng,
            message = context.getString(R.string.checkin_sos_message),
            circleIds = circleIds
        )
        CheckInScheduler.clear(context)

        val notification = NotificationCompat.Builder(context, KinPlusApplication.SAFETY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.checkin_escalated_title))
            .setContentText(context.getString(R.string.checkin_escalated_body))
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.checkin_escalated_body)))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(CheckInScheduler.NOTIFICATION_ID, notification)
        } catch (se: SecurityException) {
            Log.w(TAG, "Notification permission not granted", se)
        }
        return Result.success()
    }

    private companion object {
        const val TAG = "CheckInEscalateWorker"
    }
}
