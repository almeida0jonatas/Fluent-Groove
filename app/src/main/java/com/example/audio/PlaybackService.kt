package com.example.audio

import android.app.PendingIntent
import android.content.Intent
import android.media.audiofx.Equalizer
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.MainActivity
import com.example.model.EqualizerGains

object HardwareEqualizerBridge {
    @Volatile
    private var equalizer: Equalizer? = null

    @Volatile
    private var currentSessionId: Int = C.AUDIO_SESSION_ID_UNSET

    @Volatile
    private var latestGains: EqualizerGains = EqualizerGains()

    @Synchronized
    fun attachToSession(audioSessionId: Int) {
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET || audioSessionId == 0) return
        if (audioSessionId == currentSessionId && equalizer != null) {
            applyGains(latestGains)
            return
        }
        release()
        runCatching {
            val eq = Equalizer(0, audioSessionId).apply {
                enabled = true
            }
            equalizer = eq
            currentSessionId = audioSessionId
            applyGains(latestGains)
        }
    }

    @Synchronized
    fun applyGains(gains: EqualizerGains) {
        latestGains = gains
        val eq = equalizer ?: return
        runCatching {
            val bands = eq.numberOfBands.toInt()
            if (bands <= 0) return
            val range = eq.bandLevelRange
            val minMb = range[0].toInt()
            val maxMb = range[1].toInt()

            for (i in 0 until bands) {
                val band = i.toShort()
                val centerFreqHz = eq.getCenterFreq(band) / 1000
                val db = when {
                    centerFreqHz <= 300 -> gains.low
                    centerFreqHz <= 2200 -> gains.mid
                    else -> gains.high
                }
                val levelMb = (db * 100).coerceIn(minMb, maxMb).toShort()
                eq.setBandLevel(band, levelMb)
            }
        }
    }

    @Synchronized
    fun release() {
        runCatching { equalizer?.release() }
        equalizer = null
        currentSessionId = C.AUDIO_SESSION_ID_UNSET
    }
}

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        HardwareEqualizerBridge.attachToSession(player.audioSessionId)
        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                HardwareEqualizerBridge.attachToSession(audioSessionId)
            }
        })

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        HardwareEqualizerBridge.release()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
