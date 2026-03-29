package com.sleepwithme.app.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Monitors connected Bluetooth device battery level via HFP broadcasts.
 * For TWS earbuds, the reported value is typically the lowest of the two earbuds.
 */
class BluetoothBatteryMonitor(private val context: Context) {

    companion object {
        private const val ACTION_BATTERY_LEVEL_CHANGED =
            "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
        private const val EXTRA_BATTERY_LEVEL =
            "android.bluetooth.device.extra.BATTERY_LEVEL"
        const val LOW_BATTERY_THRESHOLD = 50
    }

    private val _batteryLevel = MutableStateFlow(-1) // -1 = unknown/no device
    val batteryLevel: StateFlow<Int> = _batteryLevel

    private val _deviceName = MutableStateFlow<String?>(null)
    val deviceName: StateFlow<String?> = _deviceName

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_BATTERY_LEVEL_CHANGED) {
                val level = intent.getIntExtra(EXTRA_BATTERY_LEVEL, -1)
                val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }
                val name = try { device?.name } catch (_: SecurityException) { null }
                Log.d("BTBattery", "Battery update: $name = $level%")
                _batteryLevel.value = level
                _deviceName.value = name
            }
        }
    }

    fun start() {
        val filter = IntentFilter(ACTION_BATTERY_LEVEL_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
    }

    fun stop() {
        try {
            context.unregisterReceiver(receiver)
        } catch (_: IllegalArgumentException) { }
    }
}
