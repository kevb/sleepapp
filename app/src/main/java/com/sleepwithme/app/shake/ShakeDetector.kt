package com.sleepwithme.app.shake

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * Shake detection based on the Bumble/Badoo algorithm (ArkadyGamza/ShakeDetector, UNLICENSE).
 * Detects direction reversals on the X axis rather than raw magnitude,
 * which avoids false positives from rolling over in bed.
 */
class ShakeDetector(
    context: Context,
    private val onShake: () -> Unit
) : SensorEventListener {

    companion object {
        private const val THRESHOLD = 10f // m/s^2 on X axis (original: 13, lowered for gentle shake)
        private const val REVERSALS_NEEDED = 3
        private const val SHAKE_WINDOW_NS = 1_000_000_000L // 1 second
        private const val COOLDOWN_NS = 1_000_000_000L // 1 second throttle
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

    private var lastSignificantX: Float = 0f
    private val reversalTimestamps = LongArray(REVERSALS_NEEDED)
    private var reversalCount = 0
    private var lastShakeTimestampNs = 0L

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        reversalCount = 0
        lastSignificantX = 0f
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]

        if (Math.abs(x) <= THRESHOLD) return

        if (lastSignificantX != 0f && lastSignificantX * x < 0f) {
            val timestampNs = event.timestamp
            val index = reversalCount % REVERSALS_NEEDED
            reversalTimestamps[index] = timestampNs
            reversalCount++

            if (reversalCount >= REVERSALS_NEEDED) {
                val oldestIndex = reversalCount % REVERSALS_NEEDED
                val oldest = reversalTimestamps[oldestIndex]

                if (timestampNs - oldest < SHAKE_WINDOW_NS) {
                    if (timestampNs - lastShakeTimestampNs > COOLDOWN_NS) {
                        lastShakeTimestampNs = timestampNs
                        reversalCount = 0
                        onShake()
                    }
                }
            }
        }

        lastSignificantX = x
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
