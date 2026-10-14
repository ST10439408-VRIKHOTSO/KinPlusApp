package za.co.kinplus.app.ui.components

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/*
 * Siren and flashlight strobe (Batch 1 enhancement to Emergency SOS, FR-17).
 *
 * Draws attention in an emergency: plays the phone's alarm sound on a loop at
 * full alarm volume and flashes the camera torch. The previous alarm volume is
 * restored when the siren stops. Devices without a torch still get the sound.
 *
 * Reference: Android Developers (2024) CameraManager.setTorchMode.
 * Available at: https://developer.android.com/reference/android/hardware/camera2/CameraManager
 * [Accessed 13 October 2026].
 */
class SirenStrobeController(private val context: Context) {

    private var player: MediaPlayer? = null
    private var strobeJob: Job? = null
    private var previousVolume: Int? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    var isRunning: Boolean = false
        private set

    fun start() {
        if (isRunning) return
        isRunning = true
        Log.i(TAG, "Siren and strobe started")
        startSound()
        startStrobe()
    }

    fun stop() {
        if (!isRunning) return
        isRunning = false
        Log.i(TAG, "Siren and strobe stopped")
        runCatching { player?.stop(); player?.release() }
        player = null
        strobeJob?.cancel()
        strobeJob = null
        setTorch(false)
        restoreVolume()
    }

    private fun startSound() {
        val audio = context.getSystemService(AudioManager::class.java)
        runCatching {
            previousVolume = audio.getStreamVolume(AudioManager.STREAM_ALARM)
            audio.setStreamVolume(AudioManager.STREAM_ALARM, audio.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0)
        }.onFailure { Log.w(TAG, "Could not raise alarm volume", it) }

        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        player = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context, uri)
                isLooping = true
                prepare()
                start()
            }
        }.onFailure { Log.w(TAG, "Could not play siren sound", it) }.getOrNull()
    }

    private fun restoreVolume() {
        val volume = previousVolume ?: return
        runCatching {
            context.getSystemService(AudioManager::class.java)
                .setStreamVolume(AudioManager.STREAM_ALARM, volume, 0)
        }
        previousVolume = null
    }

    private fun startStrobe() {
        if (torchId == null) {
            Log.i(TAG, "No flashlight on this device, sound only")
            return
        }
        strobeJob = scope.launch {
            var on = false
            while (isActive) {
                on = !on
                setTorch(on)
                delay(STROBE_INTERVAL_MS)
            }
        }
    }

    /** The first camera with a flash unit, looked up once. */
    private val torchId: String? by lazy {
        runCatching {
            val manager = context.getSystemService(CameraManager::class.java)
            manager.cameraIdList.firstOrNull { id ->
                manager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull()
    }

    private fun setTorch(on: Boolean) {
        val id = torchId ?: return
        runCatching { context.getSystemService(CameraManager::class.java).setTorchMode(id, on) }
            .onFailure { Log.w(TAG, "Torch toggle failed", it) }
    }

    private companion object {
        const val TAG = "SirenStrobe"
        const val STROBE_INTERVAL_MS = 180L
    }
}
