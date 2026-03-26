package com.sleepwithme.app

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.sleepwithme.app.player.PlaybackService
import com.sleepwithme.app.ui.PlayerScreen
import com.sleepwithme.app.ui.theme.SleepTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* don't care about result for now */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermission()

        setContent {
            SleepTheme {
                PlayerScreen(viewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture.addListener({
            Log.d("MainActivity", "MediaController connected, service instance: ${PlaybackService.instance != null}")
            connectToService()
        }, MoreExecutors.directExecutor())
    }

    private fun connectToService() {
        // Service instance may not be ready immediately
        lifecycleScope.launch {
            var attempts = 0
            while (PlaybackService.instance == null && attempts < 20) {
                delay(250)
                attempts++
            }
            PlaybackService.instance?.let { service ->
                Log.d("MainActivity", "Service connected after $attempts attempts")
                viewModel.onServiceConnected(service)
            } ?: Log.e("MainActivity", "Failed to connect to PlaybackService")
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
