package com.sleepwithme.app.timer

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class TimerState { Running, Fading, Stopped }

class SleepTimer(
    private val scope: CoroutineScope,
    private val onSetVolume: (Float) -> Unit,
    private val onPause: () -> Unit,
    private val onResume: () -> Unit,
    private val onSavePosition: () -> Unit
) {
    companion object {
        private const val FADE_DURATION_MS = 150_000L // 2.5 minutes
        private const val GRACE_PERIOD_MS = 5 * 60 * 1000L // 5 minutes
        private const val FADE_TICK_MS = 200L
    }

    private val _state = MutableStateFlow(TimerState.Stopped)
    val state: StateFlow<TimerState> = _state

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds

    private var durationMs = 0L
    private var timerJob: Job? = null
    private var stoppedAtMs = 0L

    fun start(durationMins: Int) {
        Log.d("SleepTimer", "start: ${durationMins} mins")
        durationMs = durationMins * 60 * 1000L
        startCountdown()
    }

    fun reset() {
        timerJob?.cancel()

        when (_state.value) {
            TimerState.Fading -> {
                onSetVolume(1f)
                startCountdown()
            }
            TimerState.Stopped -> {
                val elapsed = System.currentTimeMillis() - stoppedAtMs
                if (stoppedAtMs > 0 && elapsed < GRACE_PERIOD_MS) {
                    onSetVolume(1f)
                    onResume()
                    startCountdown()
                }
            }
            TimerState.Running -> {
                startCountdown()
            }
        }
    }

    fun stop() {
        timerJob?.cancel()
        _state.value = TimerState.Stopped
        _remainingSeconds.value = 0
    }

    private fun startCountdown() {
        _state.value = TimerState.Running
        _remainingSeconds.value = (durationMs / 1000).toInt()

        timerJob = scope.launch {
            var remainingMs = durationMs
            while (remainingMs > 0 && isActive) {
                delay(1000)
                remainingMs -= 1000
                _remainingSeconds.value = (remainingMs / 1000).toInt()
            }
            if (isActive) {
                startFade()
            }
        }
    }

    private fun startFade() {
        _state.value = TimerState.Fading

        timerJob = scope.launch {
            var elapsed = 0L
            while (elapsed < FADE_DURATION_MS && isActive) {
                val volume = 1f - (elapsed.toFloat() / FADE_DURATION_MS)
                onSetVolume(volume.coerceIn(0f, 1f))
                delay(FADE_TICK_MS)
                elapsed += FADE_TICK_MS
            }
            if (isActive) {
                onSetVolume(0f)
                onSavePosition()
                onPause()
                stoppedAtMs = System.currentTimeMillis()
                _state.value = TimerState.Stopped
                _remainingSeconds.value = 0
            }
        }
    }
}
