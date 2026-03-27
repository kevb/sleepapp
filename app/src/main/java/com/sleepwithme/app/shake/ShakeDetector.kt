package com.sleepwithme.app.shake

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * Shake detection based on the Bumble/Badoo reversal algorithm.
 * Tracks X and Z axes independently — a shake on either triggers detection.
 */
class ShakeDetector(
    context: Context,
    private val onShake: () -> Unit
) : SensorEventListener {

    companion object {
        private const val THRESHOLD = 10f
        private const val REVERSALS_NEEDED = 3
        private const val SHAKE_WINDOW_NS = 1_000_000_000L
        private const val COOLDOWN_NS = 1_000_000_000L
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

    private val axisTrackers = arrayOf(AxisTracker(), AxisTracker()) // X=0, Z=1
    private var lastShakeTimestampNs = 0L

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        axisTrackers.forEach { it.reset() }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val timestamp = event.timestamp
        val values = floatArrayOf(event.values[0], event.values[2]) // X, Z

        for (i in values.indices) {
            if (axisTrackers[i].onValue(values[i], timestamp)) {
                if (timestamp - lastShakeTimestampNs > COOLDOWN_NS) {
                    lastShakeTimestampNs = timestamp
                    axisTrackers.forEach { it.reset() }
                    onShake()
                    return
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private class AxisTracker {
        private var lastSignificant: Float = 0f
        private val reversalTimestamps = LongArray(REVERSALS_NEEDED)
        private var reversalCount = 0

        fun reset() {
            lastSignificant = 0f
            reversalCount = 0
        }

        /** Returns true if a shake is detected on this axis. */
        fun onValue(value: Float, timestampNs: Long): Boolean {
            if (Math.abs(value) <= THRESHOLD) return false

            if (lastSignificant != 0f && lastSignificant * value < 0f) {
                val index = reversalCount % REVERSALS_NEEDED
                reversalTimestamps[index] = timestampNs
                reversalCount++

                if (reversalCount >= REVERSALS_NEEDED) {
                    val oldestIndex = reversalCount % REVERSALS_NEEDED
                    val oldest = reversalTimestamps[oldestIndex]
                    if (timestampNs - oldest < SHAKE_WINDOW_NS) {
                        return true
                    }
                }
            }

            lastSignificant = value
            return false
        }
    }
}
