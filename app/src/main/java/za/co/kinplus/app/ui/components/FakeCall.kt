package za.co.kinplus.app.ui.components

import android.app.Dialog
import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import za.co.kinplus.app.R
import za.co.kinplus.app.databinding.DialogFakeCallSetupBinding
import za.co.kinplus.app.databinding.ViewFakeCallBinding

/*
 * Fake incoming call (Batch 1 enhancement to Emergency SOS, FR-17).
 *
 * Gives the user a believable reason to walk away from an uncomfortable or
 * unsafe situation. The phone plays its own ringtone and vibrates, shows a full
 * screen incoming call, and once answered shows a running call timer. Nothing
 * is ever dialled and no data leaves the device.
 *
 * Reference: Android Developers (2024) RingtoneManager and VibrationEffect.
 * Available at: https://developer.android.com/reference/android/media/RingtoneManager
 * [Accessed 13 October 2026].
 */

private const val TAG = "FakeCall"

/**
 * Lets the user pick who is "calling" and how long until the phone rings.
 * [onSchedule] receives the caller name and the delay in seconds.
 */
fun showFakeCallSetupDialog(
    context: Context,
    defaultCaller: String,
    onSchedule: (caller: String, delaySeconds: Int) -> Unit
) {
    val view = DialogFakeCallSetupBinding.inflate(LayoutInflater.from(context))
    view.callerInput.setText(defaultCaller)

    val dialog = MaterialAlertDialogBuilder(context)
        .setTitle(R.string.fake_call_setup_title)
        .setView(view.root)
        .setPositiveButton(R.string.fake_call_schedule) { _, _ ->
            val delaySeconds = when (view.delayChips.checkedChipId) {
                R.id.delayNow -> 0
                R.id.delay30 -> 30
                R.id.delay60 -> 60
                else -> 10
            }
            onSchedule(view.callerInput.text?.toString().orEmpty().trim(), delaySeconds)
        }
        .setNegativeButton(R.string.cancel, null)
        .show()

    // The caller name is required before the call can be scheduled.
    fun validate() {
        val missing = view.callerInput.text.isNullOrBlank()
        view.callerLayout.error = if (missing) context.getString(R.string.error_name_required) else null
        dialog.getButton(Dialog.BUTTON_POSITIVE)?.isEnabled = !missing
    }
    view.callerInput.doAfterTextChanged { validate() }
    validate()
}

/**
 * Runs one fake call for a screen: waits the chosen delay, rings until the
 * user answers or declines, then shows the in call timer. Everything is tied
 * to the screen's view lifecycle, so leaving the screen always stops the
 * ringing and closes the call.
 */
class FakeCallController(private val fragment: Fragment, private val onStateChanged: (active: Boolean) -> Unit) {

    private var job: Job? = null

    val isActive: Boolean get() = job?.isActive == true

    fun start(caller: String, delaySeconds: Int) {
        job?.cancel()
        job = fragment.viewLifecycleOwner.lifecycleScope.launch {
            onStateChanged(true)
            var dialog: Dialog? = null
            var ringing: RingingHandle? = null
            try {
                Log.d(TAG, "Fake call from '$caller' scheduled in ${delaySeconds}s")
                delay(delaySeconds * 1000L)

                val context = fragment.requireContext()
                val view = ViewFakeCallBinding.inflate(LayoutInflater.from(context))
                view.caller.text = caller
                view.initial.text = caller.take(1).uppercase()
                view.status.setText(R.string.fake_call_incoming)

                dialog = Dialog(context, R.style.Theme_KinPlus_FakeCall).apply {
                    setContentView(view.root)
                    setCancelable(false)
                    window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    show()
                }
                Log.d(TAG, "Fake call ringing")
                ringing = startRinging(context)

                var elapsed = 0
                var timerJob: Job? = null
                val finished = CompletableDeferred<Unit>()

                view.declineButton.setOnClickListener {
                    Log.d(TAG, "Fake call declined")
                    finished.complete(Unit)
                }
                view.acceptButton.setOnClickListener {
                    Log.d(TAG, "Fake call answered")
                    ringing?.stop()
                    ringing = null
                    // Once answered only the red "End call" button remains, with a running timer.
                    view.acceptGroup.isVisible = false
                    view.declineLabel.setText(R.string.fake_call_end)
                    view.declineButton.contentDescription = context.getString(R.string.fake_call_end)
                    view.declineButton.setOnClickListener {
                        Log.d(TAG, "Fake call ended after ${elapsed}s")
                        finished.complete(Unit)
                    }
                    view.status.text = formatCallTime(0)
                    timerJob = launch {
                        while (true) {
                            delay(1000)
                            elapsed++
                            view.status.text = formatCallTime(elapsed)
                        }
                    }
                }

                finished.await()
                timerJob?.cancel()
            } finally {
                ringing?.stop()
                dialog?.dismiss()
                onStateChanged(false)
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
    }
}

private fun formatCallTime(seconds: Int): String = "%02d:%02d".format(seconds / 60, seconds % 60)

/** Plays the default ringtone and a repeating vibration. Returned handle stops both. */
private class RingingHandle(private val ringtone: Ringtone?, private val vibrator: Vibrator?) {
    fun stop() {
        runCatching { ringtone?.stop() }
        runCatching { vibrator?.cancel() }
    }
}

private fun startRinging(context: Context): RingingHandle {
    val ringtone = runCatching {
        RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))?.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) isLooping = true
            play()
        }
    }.onFailure { Log.w(TAG, "Could not play ringtone", it) }.getOrNull()

    val vibrator = runCatching {
        val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        v?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 800, 600), 0))
        v
    }.onFailure { Log.w(TAG, "Could not vibrate", it) }.getOrNull()

    return RingingHandle(ringtone, vibrator)
}
