package com.example.mazmazradio.service

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Воспроизведение прямого эфира.
 *
 * Держит [ExoPlayer] в [MediaSessionService] и переподключает поток: у live-потока
 * обрыв — штатная ситуация, без автоповтора пользователь получает тишину и
 * застывший «буферизация».
 */
class RadioService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    private val retryHandler = Handler(Looper.getMainLooper())
    private var retryAttempts = 0

    private val playerListener = object : Player.Listener {

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) retryAttempts = 0
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            retryAttempts = 0
        }

        override fun onPlayerError(error: PlaybackException) {
            val current = player ?: return
            // Пауза нажата руками — переподключаться не нужно.
            if (!current.playWhenReady || current.mediaItemCount == 0) return
            if (retryAttempts >= MAX_RETRIES) {
                Log.w(TAG, "Поток упал (${error.errorCodeName}), автоповтор исчерпан")
                return
            }
            val waitMs = RETRY_BACKOFF_MS[retryAttempts]
            retryAttempts++
            Log.w(
                TAG,
                "Поток упал (${error.errorCodeName}), попытка $retryAttempts/$MAX_RETRIES через ${waitMs}мс"
            )
            // playWhenReady остался true — prepare() сам продолжит воспроизведение.
            retryHandler.postDelayed({ player?.prepare() }, waitMs)
        }
    }

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        player?.let { exoPlayer ->
            exoPlayer.addListener(playerListener)
            mediaSession = MediaSession.Builder(this, exoPlayer).build()
            SleepTimer.onExpire = { exoPlayer.pause() }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    /**
     * Задачу свайпнули. Если эфир не играет — сервис больше не нужен, иначе он
     * остаётся с висящим уведомлением. Играет — оставляем: управление с локскрина
     * и из уведомления должно жить.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val current = mediaSession?.player
        if (current == null || !current.playWhenReady || current.mediaItemCount == 0) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        retryHandler.removeCallbacksAndMessages(null)
        SleepTimer.cancel()
        SleepTimer.onExpire = null
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        player = null
        super.onDestroy()
    }

    private companion object {
        const val TAG = "RadioService"

        /** Паузы перед переподключением: 1s, 2s, 4s. */
        val RETRY_BACKOFF_MS = longArrayOf(1_000L, 2_000L, 4_000L)
        const val MAX_RETRIES = 3
    }
}
