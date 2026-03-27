package com.sleepwithme.app.player

import android.content.Context
import android.media.MediaPlayer
import com.sleepwithme.app.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class AmbientMode { Off, Rain, Storm }

class AmbientPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null

    private val _mode = MutableStateFlow(AmbientMode.Off)
    val mode: StateFlow<AmbientMode> = _mode

    private var volume = 0.45f

    fun setMode(mode: AmbientMode) {
        release()
        _mode.value = mode

        val resId = when (mode) {
            AmbientMode.Off -> return
            AmbientMode.Rain -> R.raw.rain_light
            AmbientMode.Storm -> R.raw.rain_storm
        }

        mediaPlayer = MediaPlayer.create(context, resId).apply {
            isLooping = true
            setVolume(volume, volume)
            start()
        }
    }

    fun cycleMode() {
        val next = when (_mode.value) {
            AmbientMode.Off -> AmbientMode.Rain
            AmbientMode.Rain -> AmbientMode.Storm
            AmbientMode.Storm -> AmbientMode.Off
        }
        setMode(next)
    }

    fun setVolume(vol: Float) {
        volume = vol.coerceIn(0f, 1f)
        mediaPlayer?.setVolume(volume, volume)
    }

    fun pause() {
        mediaPlayer?.takeIf { it.isPlaying }?.pause()
    }

    fun resume() {
        if (_mode.value != AmbientMode.Off) {
            mediaPlayer?.start()
        }
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
