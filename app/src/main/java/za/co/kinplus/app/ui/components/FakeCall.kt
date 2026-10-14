package za.co.kinplus.app.ui.components

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import za.co.kinplus.app.R

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

/** Delay choices offered in the setup dialog, in seconds. */
private val delayOptions = listOf(
    0 to R.string.fake_call_now,
    10 to R.string.fake_call_10s,
    30 to R.string.fake_call_30s,
    60 to R.string.fake_call_60s
)

/** Lets the user pick who is "calling" and how long until the phone rings. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FakeCallSetupDialog(
    defaultCaller: String,
    onDismiss: () -> Unit,
    onSchedule: (caller: String, delaySeconds: Int) -> Unit
) {
    var caller by remember { mutableStateOf(defaultCaller) }
    var delaySeconds by remember { mutableIntStateOf(10) }
    val callerMissing = caller.isBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.fake_call_setup_title), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                KinTextField(
                    value = caller,
                    onValueChange = { caller = it.take(30) },
                    label = stringResource(R.string.fake_call_caller_label),
                    isError = callerMissing,
                    supportingText = if (callerMissing) stringResource(R.string.error_name_required) else null
                )
                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.fake_call_delay_label), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    delayOptions.forEach { (seconds, labelRes) ->
                        FilterChip(
                            selected = delaySeconds == seconds,
                            onClick = { delaySeconds = seconds },
                            label = { Text(stringResource(labelRes)) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSchedule(caller.trim(), delaySeconds) },
                enabled = !callerMissing
            ) { Text(stringResource(R.string.fake_call_schedule), fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

private enum class FakeCallPhase { WAITING, RINGING, ACTIVE }

/**
 * Runs one fake call: waits [delaySeconds], rings until answered or declined,
 * then shows the in call screen. Calls [onFinished] when the call ends.
 */
@Composable
fun FakeCallHost(caller: String, delaySeconds: Int, onFinished: () -> Unit) {
    val context = LocalContext.current
    var phase by remember { mutableStateOf(FakeCallPhase.WAITING) }
    var elapsed by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        Log.d(TAG, "Fake call from '$caller' scheduled in ${delaySeconds}s")
        delay(delaySeconds * 1000L)
        phase = FakeCallPhase.RINGING
        Log.d(TAG, "Fake call ringing")
    }

    // Ring and vibrate only while the call is ringing; always stop on exit.
    DisposableEffect(phase) {
        val alert = if (phase == FakeCallPhase.RINGING) startRinging(context) else null
        onDispose { alert?.stop() }
    }

    // Call timer once answered.
    LaunchedEffect(phase) {
        if (phase == FakeCallPhase.ACTIVE) {
            while (true) {
                delay(1000)
                elapsed++
            }
        }
    }

    if (phase == FakeCallPhase.WAITING) return

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.inverseSurface),
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 72.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (phase == FakeCallPhase.RINGING) stringResource(R.string.fake_call_incoming)
                    else formatCallTime(elapsed),
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(28.dp))
                Box(
                    Modifier.size(112.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(caller.take(1).uppercase(), color = Color.White, fontSize = 46.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(20.dp))
                Text(caller, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                Text(stringResource(R.string.fake_call_mobile), color = Color.White.copy(alpha = 0.6f), fontSize = 15.sp)

                Spacer(Modifier.weight(1f))

                if (phase == FakeCallPhase.RINGING) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        CallButton(
                            icon = Icons.Filled.CallEnd,
                            label = stringResource(R.string.fake_call_decline),
                            color = MaterialTheme.colorScheme.error
                        ) {
                            Log.d(TAG, "Fake call declined")
                            onFinished()
                        }
                        CallButton(
                            icon = Icons.Filled.Call,
                            label = stringResource(R.string.fake_call_accept),
                            color = Color(0xFF3FB68B)
                        ) {
                            Log.d(TAG, "Fake call answered")
                            phase = FakeCallPhase.ACTIVE
                        }
                    }
                } else {
                    CallButton(
                        icon = Icons.Filled.CallEnd,
                        label = stringResource(R.string.fake_call_end),
                        color = MaterialTheme.colorScheme.error
                    ) {
                        Log.d(TAG, "Fake call ended after ${elapsed}s")
                        onFinished()
                    }
                }
            }
        }
    }
}

@Composable
private fun CallButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(color).clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
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
