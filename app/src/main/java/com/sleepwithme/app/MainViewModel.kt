package com.sleepwithme.app

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sleepwithme.app.data.Collection
import com.sleepwithme.app.data.ManifestRepository
import com.sleepwithme.app.data.PlaybackPrefs
import com.sleepwithme.app.player.AmbientMode
import com.sleepwithme.app.player.PlaybackService
import com.sleepwithme.app.timer.SleepTimer
import com.sleepwithme.app.timer.TimerState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val manifestRepo = ManifestRepository(application)
    val prefs = PlaybackPrefs(application)

    private var playerService: PlaybackService? = null
    private var positionSaveJob: kotlinx.coroutines.Job? = null

    // Observable UI state
    private val _collection = MutableStateFlow<Collection?>(null)
    val collection: StateFlow<Collection?> = _collection

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _trackIndex = MutableStateFlow(0)
    val trackIndex: StateFlow<Int> = _trackIndex

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs

    private val _timerDurationMins = MutableStateFlow(PlaybackPrefs.DEFAULT_TIMER_MINS)
    val timerDurationMins: StateFlow<Int> = _timerDurationMins

    private val _ambientMode = MutableStateFlow(AmbientMode.Off)
    val ambientMode: StateFlow<AmbientMode> = _ambientMode

    val sleepTimer = SleepTimer(
        scope = viewModelScope,
        onSetVolume = { volume ->
            playerService?.playerManager?.setVolume(volume)
            playerService?.ambientPlayer?.setVolume(volume * 0.5f)
        },
        onPause = {
            playerService?.playerManager?.pause()
            playerService?.ambientPlayer?.pause()
        },
        onResume = {
            playerService?.playerManager?.play()
            playerService?.ambientPlayer?.resume()
        },
        onSavePosition = { saveCurrentPosition() }
    )

    init {
        viewModelScope.launch {
            manifestRepo.refresh()
        }

        viewModelScope.launch {
            manifestRepo.manifest
                .filterNotNull()
                .collect { tryLoadCollection() }
        }
    }

    fun onServiceConnected(service: PlaybackService) {
        playerService = service
        service.onShake = { onShake() }
        tryLoadCollection()

        viewModelScope.launch {
            service.playerManager.isPlaying.collect { _isPlaying.value = it }
        }
        viewModelScope.launch {
            service.playerManager.currentTrackIndex.collect { index ->
                _trackIndex.value = index
                // Prefetch current + next track, clean up old ones
                _collection.value?.let { col ->
                    service.playerManager.trackCache.ensureCached(col, index)
                }
            }
        }
        viewModelScope.launch {
            service.playerManager.currentPositionMs.collect { _positionMs.value = it }
        }
        viewModelScope.launch {
            service.ambientPlayer.mode.collect { _ambientMode.value = it }
        }
    }

    val collections: StateFlow<List<Collection>> = manifestRepo.manifest
        .map { it?.collections ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun cycleAmbient() {
        playerService?.ambientPlayer?.cycleMode()
    }

    fun switchCollection(col: Collection) {
        val service = playerService ?: return
        if (col.id == _collection.value?.id) return
        saveCurrentPosition()
        sleepTimer.stop()
        _collection.value = col
        prefs.savePosition(collectionId = col.id, trackIndex = 0, positionMs = 0)
        service.playerManager.loadCollection(col, startTrackIndex = 0, startPositionMs = 0)
    }

    private fun tryLoadCollection() {
        val service = playerService ?: return
        val manifest = manifestRepo.manifest.value ?: return
        if (_collection.value != null) return

        Log.d("MainViewModel", "tryLoadCollection: service and manifest ready")

        val collection = if (prefs.collectionId != null) {
            manifest.collections.find { it.id == prefs.collectionId }
        } else {
            manifest.collections.firstOrNull()
        } ?: return

        _collection.value = collection
        Log.d("MainViewModel", "Loading collection: ${collection.title} with ${collection.tracks.size} tracks")
        service.playerManager.loadCollection(
            collection,
            startTrackIndex = prefs.trackIndex,
            startPositionMs = prefs.positionMs
        )

        startPeriodicSave()
    }

    fun togglePlayPause() {
        val pm = playerService?.playerManager ?: return
        val wasPlaying = pm.isPlaying.value
        Log.d("MainViewModel", "togglePlayPause: wasPlaying=$wasPlaying, timerState=${sleepTimer.state.value}")
        if (!wasPlaying) {
            // Always restore volume when manually playing
            pm.setVolume(1f)
            playerService?.ambientPlayer?.setVolume(0.45f)
            pm.play()
            playerService?.ambientPlayer?.resume()
            if (sleepTimer.state.value == TimerState.Stopped) {
                sleepTimer.start(_timerDurationMins.value)
            }
        } else {
            pm.pause()
        }
    }

    fun seekRelative(deltaMs: Long) {
        val pm = playerService?.playerManager ?: return
        val newPos = (pm.getCurrentPositionMs() + deltaMs).coerceAtLeast(0)
        pm.player.seekTo(newPos)
    }

    fun adjustTimer(deltaMins: Int) {
        sleepTimer.adjustTime(deltaMins * 60 * 1000L)
    }

    private fun onShake() {
        sleepTimer.reset()
    }

    private fun saveCurrentPosition() {
        val pm = playerService?.playerManager ?: return
        val collection = _collection.value ?: return
        prefs.savePosition(
            collectionId = collection.id,
            trackIndex = pm.currentTrackIndex.value,
            positionMs = pm.getCurrentPositionMs()
        )
    }

    private fun startPeriodicSave() {
        positionSaveJob?.cancel()
        positionSaveJob = viewModelScope.launch {
            while (true) {
                delay(30_000)
                if (playerService?.playerManager?.isPlaying?.value == true) {
                    saveCurrentPosition()
                }
            }
        }
    }

    override fun onCleared() {
        saveCurrentPosition()
        positionSaveJob?.cancel()
        super.onCleared()
    }
}
