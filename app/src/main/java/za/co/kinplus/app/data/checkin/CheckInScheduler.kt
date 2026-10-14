package za.co.kinplus.app.data.checkin

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/*
 * "I'm OK" check in timer (Batch 1 enhancement to Safe Journey, FR-23 and FR-24).
 *
 * The user picks an interval. When it runs out Kin+ posts a notification asking
 * "Are you OK?". If the user does not tap "I'm OK" within the grace period, an
 * SOS is raised to all of their circles through SosRepository, which already
 * queues the alert when the phone is offline (FR-29).
 *
 * Scheduling uses WorkManager so the timer survives the app being closed and
 * the phone restarting.
 *
 * Reference: Android Developers (2024) Schedule tasks with WorkManager.
 * Available at: https://developer.android.com/topic/libraries/architecture/workmanager
 * [Accessed 13 October 2026].
 */
object CheckInScheduler {

    private const val TAG = "CheckInScheduler"
    private const val PREFS = "kinplus_checkin"
    private const val KEY_DUE_AT = "due_at"

    const val PROMPT_WORK = "kinplus_checkin_prompt"
    const val ESCALATE_WORK = "kinplus_checkin_escalate"
    const val NOTIFICATION_ID = 7310

    /** Minutes the user has to answer the "Are you OK?" prompt before the SOS is sent. */
    const val GRACE_MINUTES = 5L

    /** Interval choices offered on the Safe Journey screen. */
    val intervalOptions = listOf(15, 30, 60, 120)

    /** Starts (or restarts) the timer so the first prompt appears after [minutes]. */
    fun start(context: Context, minutes: Int) {
        require(minutes > 0) { "Check in interval must be positive" }
        val dueAt = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(minutes.toLong())
        prefs(context).edit().putLong(KEY_DUE_AT, dueAt).apply()

        val work = WorkManager.getInstance(context)
        work.cancelUniqueWork(ESCALATE_WORK)
        work.enqueueUniqueWork(
            PROMPT_WORK,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<CheckInPromptWorker>()
                .setInitialDelay(minutes.toLong(), TimeUnit.MINUTES)
                .build()
        )
        Log.i(TAG, "Check in started, first prompt in $minutes min")
    }

    /** Called when the prompt is shown: arms the SOS that fires if nobody answers. */
    fun armEscalation(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            ESCALATE_WORK,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<CheckInEscalateWorker>()
                .setInitialDelay(GRACE_MINUTES, TimeUnit.MINUTES)
                .build()
        )
        Log.i(TAG, "Escalation armed, SOS in $GRACE_MINUTES min unless confirmed")
    }

    /** The user confirmed they are OK: stop everything and clear the prompt. */
    fun confirmOk(context: Context) {
        stop(context)
        Log.i(TAG, "User confirmed they are OK")
    }

    /** Stops the timer without raising anything. */
    fun stop(context: Context) {
        val work = WorkManager.getInstance(context)
        work.cancelUniqueWork(PROMPT_WORK)
        work.cancelUniqueWork(ESCALATE_WORK)
        prefs(context).edit().remove(KEY_DUE_AT).apply()
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        Log.i(TAG, "Check in stopped")
    }

    /** Marks the timer finished after an escalation, so the UI shows it as off. */
    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_DUE_AT).apply()
    }

    /** When the next prompt is due (epoch millis), or null when no check in is running. */
    fun dueAt(context: Context): Long? =
        prefs(context).getLong(KEY_DUE_AT, 0L).takeIf { it > 0L }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
