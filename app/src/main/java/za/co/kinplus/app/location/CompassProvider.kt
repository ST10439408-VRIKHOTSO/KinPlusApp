package za.co.kinplus.app.location

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.sample
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The device's compass heading (0-360°, 0 = north), read from the rotation
 * vector sensor. Used to point the map's "my location" dot the direction the
 * phone is actually facing, not just the direction of travel — the Maps SDK's
 * own default location source only derives a bearing from GPS course while
 * moving, so standing still and turning the phone does nothing without this.
 *
 * Assumes portrait use (no display-rotation compensation) — the Home screen
 * map isn't used in landscape, so this is a deliberate simplification.
 */
@Singleton
class CompassProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    @OptIn(FlowPreview::class)
    fun headingUpdates(): Flow<Float> = callbackFlow {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (rotationSensor == null) {
            close()
            return@callbackFlow
        }

        val rotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientation)
                val degrees = Math.toDegrees(orientation[0].toDouble()).toFloat()
                trySend((degrees + 360f) % 360f)
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
        }

        sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        awaitClose { sensorManager.unregisterListener(listener) }
    }.conflate().sample(120)
}
