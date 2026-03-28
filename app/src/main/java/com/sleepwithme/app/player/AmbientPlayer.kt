package com.sleepwithme.app.player

import android.content.Context
import android.media.MediaPlayer
import com.sleepwithme.app.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class AmbientMode { Off, Rain, Storm }

class AmbientPlayer(private val context: Context) {

    private var player: MediaPlayer? = null

    private val _mode = MutableStateFlow(AmbientMode.Off)
    val mode: StateFlow<AmbientMode> = _mode

    private var baseVolume = 0.45f
    private var volume = 0.45f
    private var paused = false

    fun setMode(mode: AmbientMode) {
        release()
        _mode.value = mode

        val resId = when (mode) {
            AmbientMode.Off -> return
            AmbientMode.Rain -> R.raw.rain_light
            AmbientMode.Storm -> R.raw.rain_storm
        }

        baseVolume = when (mode) {
            AmbientMode.Rain -> 0.35f
            AmbientMode.Storm -> 0.45f
            else -> 0.45f
        }
        volume = baseVolume

        player = MediaPlayer.create(context, resId).apply {
            isLooping = true
            setVolume(volume, volume)
            start()
        }
        paused = false
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
        player?.setVolume(volume, volume)
    }

    fun pause() {
        paused = true
        player?.takeIf { it.isPlaying }?.pause()
    }

    fun resume() {
        if (_mode.value == AmbientMode.Off) return
        paused = false
        player?.start()
    }

    fun release() {
        player?.release()
        player = null
    }
}
