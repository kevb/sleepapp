package com.sleepwithme.app.player

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import com.sleepwithme.app.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class AmbientMode { Off, Rain, Storm }

class AmbientPlayer(private val context: Context) {

    companion object {
        private const val CROSSFADE_MS = 2000L
        private const val FADE_STEPS = 20
    }

    private var playerA: MediaPlayer? = null
    private var playerB: MediaPlayer? = null
    private var activeIsA = true
    private var currentResId: Int? = null

    private val _mode = MutableStateFlow(AmbientMode.Off)
    val mode: StateFlow<AmbientMode> = _mode

    private var volume = 0.45f
    private var paused = false
    private val handler = Handler(Looper.getMainLooper())

    private val loopChecker = object : Runnable {
        override fun run() {
            if (paused || _mode.value == AmbientMode.Off) return
            val active = if (activeIsA) playerA else playerB
            val duration = active?.duration ?: return
            val position = active.currentPosition
            val remaining = duration - position

            if (remaining in 1..CROSSFADE_MS.toInt()) {
                startCrossfade()
            } else {
                handler.postDelayed(this, 500)
            }
        }
    }

    fun setMode(mode: AmbientMode) {
        release()
        _mode.value = mode

        currentResId = when (mode) {
            AmbientMode.Off -> return
            AmbientMode.Rain -> R.raw.rain_light
            AmbientMode.Storm -> R.raw.rain_storm
        }

        playerA = createPlayer(currentResId!!)
        playerA?.start()
        activeIsA = true
        paused = false
        handler.postDelayed(loopChecker, 500)
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
        playerA?.setVolume(volume, volume)
        playerB?.setVolume(volume, volume)
    }

    fun pause() {
        paused = true
        handler.removeCallbacks(loopChecker)
        playerA?.takeIf { it.isPlaying }?.pause()
        playerB?.takeIf { it.isPlaying }?.pause()
    }

    fun resume() {
        if (_mode.value == AmbientMode.Off) return
        paused = false
        val active = if (activeIsA) playerA else playerB
        active?.start()
        handler.postDelayed(loopChecker, 500)
    }

    fun release() {
        handler.removeCallbacks(loopChecker)
        handler.removeCallbacksAndMessages(null)
        playerA?.release()
        playerB?.release()
        playerA = null
        playerB = null
    }

    private fun startCrossfade() {
        val resId = currentResId ?: return
        val fadeOut = if (activeIsA) playerA else playerB
        val fadeIn = createPlayer(resId)

        if (activeIsA) playerB = fadeIn else playerA = fadeIn

        fadeIn.setVolume(0f, 0f)
        fadeIn.start()

        val stepMs = CROSSFADE_MS / FADE_STEPS
        for (i in 1..FADE_STEPS) {
            handler.postDelayed({
                val frac = i.toFloat() / FADE_STEPS
                fadeIn.setVolume(volume * frac, volume * frac)
                fadeOut?.setVolume(volume * (1f - frac), volume * (1f - frac))

                if (i == FADE_STEPS) {
                    fadeOut?.stop()
                    fadeOut?.release()
                    if (activeIsA) playerA = null else playerB = null
                    activeIsA = !activeIsA
                    handler.postDelayed(loopChecker, 500)
                }
            }, stepMs * i)
        }
    }

    private fun createPlayer(resId: Int): MediaPlayer {
        return MediaPlayer.create(context, resId).apply {
            setVolume(volume, volume)
        }
    }
}
