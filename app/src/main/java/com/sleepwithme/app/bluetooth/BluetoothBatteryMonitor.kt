package com.sleepwithme.app.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Monitors connected Bluetooth device battery level.
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
                Log.d("BTBattery", "Broadcast: $name = $level%")
                updateBattery(level, name)
            }
        }
    }

    private fun updateBattery(level: Int, name: String?) {
        _batteryLevel.value = level
        if (name != null) _deviceName.value = name
    }

    fun start() {
        val filter = IntentFilter(ACTION_BATTERY_LEVEL_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        // Read current level from already-connected devices
        pollConnectedDevices()
    }

    /**
     * Check bonded + connected audio devices for battery level using hidden getBatteryLevel().
     * This catches the case where earbuds were connected before the app started.
     */
    private fun pollConnectedDevices() {
        try {
            val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = btManager?.adapter ?: return
            val bondedDevices = try { adapter.bondedDevices } catch (_: SecurityException) { return }

            // Check A2DP (audio) connected devices
            adapter.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    try {
                        val connected = proxy.connectedDevices
                        for (device in connected) {
                            val level = getDeviceBattery(device)
                            val name = try { device.name } catch (_: SecurityException) { null }
                            Log.d("BTBattery", "Poll: $name = $level%")
                            if (level >= 0) {
                                updateBattery(level, name)
                                break // Use first device with valid battery
                            }
                        }
                    } catch (e: SecurityException) {
                        Log.d("BTBattery", "No BT permission for polling")
                    }
                    adapter.closeProfileProxy(profile, proxy)
                }

                override fun onServiceDisconnected(profile: Int) {}
            }, BluetoothProfile.A2DP)
        } catch (e: Exception) {
            Log.d("BTBattery", "Poll failed: ${e.message}")
        }
    }

    private fun getDeviceBattery(device: BluetoothDevice): Int {
        return try {
            val method = device.javaClass.getMethod("getBatteryLevel")
            method.invoke(device) as? Int ?: -1
        } catch (_: Exception) {
            -1
        }
    }

    fun stop() {
        try {
            context.unregisterReceiver(receiver)
        } catch (_: IllegalArgumentException) { }
    }
}
