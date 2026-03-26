package com.sleepwithme.app.player

import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.sleepwithme.app.shake.ShakeDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class PlaybackService : MediaSessionService() {

    companion object {
        var instance: PlaybackService? = null
            private set
    }

    lateinit var playerManager: PlayerManager
        private set
    private lateinit var mediaSession: MediaSession
    private lateinit var shakeDetector: ShakeDetector
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    var onShake: (() -> Unit)? = null

    override fun onCreate() {
        super.onCreate()
        instance = this

        playerManager = PlayerManager(this, serviceScope)
        mediaSession = MediaSession.Builder(this, playerManager.player).build()

        shakeDetector = ShakeDetector(this) {
            onShake?.invoke()
        }
        shakeDetector.start()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession.player
        if (!player.playWhenReady || player.mediaItemCount == 0 ||
            player.playbackState == Player.STATE_ENDED) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        shakeDetector.stop()
        mediaSession.release()
        playerManager.release()
        serviceScope.cancel()
        instance = null
        super.onDestroy()
    }
}
