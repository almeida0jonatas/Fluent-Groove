package com.example.audio

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.model.EqualizerGains
import com.example.model.RepeatMode
import com.example.model.Track
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlaybackSnapshot(
    val isConnected: Boolean = false,
    val isPlaying: Boolean = false,
    val currentQueueIndex: Int = 0,
    val elapsedMs: Long = 0L,
    val elapsedSeconds: Int = 0,
    val durationSeconds: Int = 0,
    val errorMessage: String? = null,
)

class GrooveAudioController(
    private val context: Context,
    private val onTrackChanged: (Int) -> Unit,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var progressJob: Job? = null

    private val _snapshot = MutableStateFlow(PlaybackSnapshot())
    val snapshot: StateFlow<PlaybackSnapshot> = _snapshot.asStateFlow()

    private var pendingQueue: List<Track>? = null
    private var pendingStartIndex: Int = 0
    private var pendingAutoPlay: Boolean = false
    private var pendingStartPositionMs: Long = 0L
    private var pendingIsRestoreOnly: Boolean = false
    private var lastVolume: Float = 0.78f
    private var lastMuted: Boolean = false
    private var lastShuffled: Boolean = false
    private var lastRepeatMode: RepeatMode = RepeatMode.ALL

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _snapshot.update { it.copy(isPlaying = isPlaying, errorMessage = null) }
            if (isPlaying) startProgressPolling() else stopProgressPolling()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val c = controller
            val idx = c?.currentMediaItemIndex ?: 0
            val dur = ((c?.duration ?: 0L).coerceAtLeast(0L) / 1000L).toInt()
            val posMs = if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
                (c?.currentPosition ?: 0L).coerceAtLeast(0L)
            } else {
                0L
            }
            _snapshot.update {
                it.copy(
                    currentQueueIndex = idx,
                    elapsedMs = posMs,
                    elapsedSeconds = (posMs / 1000L).toInt(),
                    durationSeconds = if (dur > 0) dur else it.durationSeconds,
                    errorMessage = null,
                )
            }
            if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
                onTrackChanged(idx)
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val c = controller ?: return
            val dur = (c.duration.coerceAtLeast(0L) / 1000L).toInt()
            val posMs = c.currentPosition.coerceAtLeast(0L)
            val pos = (posMs / 1000L).toInt()
            _snapshot.update {
                it.copy(
                    elapsedMs = posMs,
                    elapsedSeconds = pos,
                    durationSeconds = if (dur > 0) dur else it.durationSeconds,
                    isPlaying = c.isPlaying,
                )
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _snapshot.update {
                it.copy(
                    isPlaying = false,
                    errorMessage = "Could not play audio file: ${error.errorCodeName}",
                )
            }
        }
    }

    fun connect() {
        if (controllerFuture != null) return
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                runCatching {
                    val mediaController = future.get()
                    controller = mediaController
                    mediaController.addListener(playerListener)
                    mediaController.volume = if (lastMuted) 0f else lastVolume
                    mediaController.shuffleModeEnabled = lastShuffled
                    mediaController.repeatMode = mapRepeatMode(lastRepeatMode)
                    _snapshot.update { it.copy(isConnected = true) }

                    val queued = pendingQueue
                    val isRestoreOnly = pendingIsRestoreOnly
                    pendingQueue = null
                    pendingIsRestoreOnly = false

                    if (queued != null && (!isRestoreOnly || mediaController.mediaItemCount == 0)) {
                        setQueueAndPlay(
                            queue = queued,
                            startIndex = pendingStartIndex,
                            autoPlay = pendingAutoPlay,
                            startPositionMs = pendingStartPositionMs,
                        )
                    } else if (mediaController.mediaItemCount > 0) {
                        val posMs = mediaController.currentPosition.coerceAtLeast(0L)
                        val durSec = (mediaController.duration.coerceAtLeast(0L) / 1000L).toInt()
                        _snapshot.update {
                            it.copy(
                                isPlaying = mediaController.isPlaying,
                                currentQueueIndex = mediaController.currentMediaItemIndex.coerceAtLeast(0),
                                elapsedMs = posMs,
                                elapsedSeconds = (posMs / 1000L).toInt(),
                                durationSeconds = if (durSec > 0) durSec else it.durationSeconds,
                            )
                        }
                        if (mediaController.isPlaying) startProgressPolling()
                    }
                }
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    fun setQueueAndPlay(
        queue: List<Track>,
        startIndex: Int,
        autoPlay: Boolean = true,
        startPositionMs: Long = 0L,
    ) {
        val clampedPos = startPositionMs.coerceAtLeast(0L)
        val c = controller
        if (c == null) {
            pendingQueue = queue
            pendingStartIndex = startIndex
            pendingAutoPlay = autoPlay
            pendingStartPositionMs = clampedPos
            pendingIsRestoreOnly = false
            return
        }
        if (queue.isEmpty()) {
            c.stop()
            c.clearMediaItems()
            _snapshot.update {
                it.copy(isPlaying = false, currentQueueIndex = 0, elapsedMs = 0L, elapsedSeconds = 0, durationSeconds = 0)
            }
            return
        }
        val safeIdx = startIndex.coerceIn(0, queue.lastIndex)
        val items = queue.map { track -> track.toMediaItem() }
        c.setMediaItems(items, safeIdx, clampedPos)
        c.prepare()
        c.playWhenReady = autoPlay
        _snapshot.update {
            it.copy(
                currentQueueIndex = safeIdx,
                elapsedMs = clampedPos,
                elapsedSeconds = (clampedPos / 1000L).toInt(),
                durationSeconds = queue[safeIdx].duration,
                isPlaying = autoPlay,
                errorMessage = null,
            )
        }
        if (autoPlay) startProgressPolling() else stopProgressPolling()
    }

    fun restoreQueueIfIdle(
        queue: List<Track>,
        startIndex: Int,
        startPositionMs: Long,
    ) {
        if (queue.isEmpty()) return
        val clampedPos = startPositionMs.coerceAtLeast(0L)
        val c = controller
        if (c == null) {
            if (pendingQueue == null) {
                pendingQueue = queue
                pendingStartIndex = startIndex
                pendingAutoPlay = false
                pendingStartPositionMs = clampedPos
                pendingIsRestoreOnly = true
            }
            return
        }
        if (c.mediaItemCount == 0) {
            setQueueAndPlay(
                queue = queue,
                startIndex = startIndex,
                autoPlay = false,
                startPositionMs = clampedPos,
            )
        } else {
            val posMs = c.currentPosition.coerceAtLeast(0L)
            val durSec = (c.duration.coerceAtLeast(0L) / 1000L).toInt()
            _snapshot.update {
                it.copy(
                    isPlaying = c.isPlaying,
                    currentQueueIndex = c.currentMediaItemIndex.coerceAtLeast(0),
                    elapsedMs = posMs,
                    elapsedSeconds = (posMs / 1000L).toInt(),
                    durationSeconds = if (durSec > 0) durSec else it.durationSeconds,
                )
            }
            if (c.isPlaying) startProgressPolling()
        }
    }

    fun getCurrentPositionMs(): Long {
        return controller?.currentPosition?.coerceAtLeast(0L) ?: _snapshot.value.elapsedMs
    }

    fun appendToQueue(tracks: List<Track>) {
        val c = controller ?: return
        if (tracks.isEmpty()) return
        c.addMediaItems(tracks.map { it.toMediaItem() })
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val c = controller ?: return
        if (fromIndex in 0 until c.mediaItemCount && toIndex in 0 until c.mediaItemCount) {
            c.moveMediaItem(fromIndex, toIndex)
            _snapshot.update { it.copy(currentQueueIndex = c.currentMediaItemIndex) }
        }
    }

    fun removeQueueItem(index: Int) {
        val c = controller ?: return
        if (index in 0 until c.mediaItemCount) {
            c.removeMediaItem(index)
            _snapshot.update { it.copy(currentQueueIndex = c.currentMediaItemIndex.coerceAtLeast(0)) }
        }
    }

    fun clearQueue() {
        val c = controller ?: return
        c.stop()
        c.clearMediaItems()
        stopProgressPolling()
        _snapshot.update {
            it.copy(isPlaying = false, currentQueueIndex = 0, elapsedMs = 0L, elapsedSeconds = 0, durationSeconds = 0)
        }
    }

    fun jumpToIndex(index: Int) {
        val c = controller ?: return
        if (index in 0 until c.mediaItemCount) {
            c.seekToDefaultPosition(index)
            c.play()
            _snapshot.update { it.copy(currentQueueIndex = index, elapsedMs = 0L, elapsedSeconds = 0) }
        }
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) {
            c.pause()
        } else {
            if (c.playbackState == Player.STATE_ENDED) {
                c.seekTo(0L)
            }
            c.play()
        }
    }

    fun pause() {
        controller?.pause()
    }

    fun seekToSeconds(seconds: Int) {
        seekToMs(seconds.coerceAtLeast(0) * 1000L)
    }

    fun seekToMs(positionMs: Long) {
        val clampedMs = positionMs.coerceAtLeast(0L)
        controller?.seekTo(clampedMs)
        _snapshot.update {
            it.copy(
                elapsedMs = clampedMs,
                elapsedSeconds = (clampedMs / 1000L).toInt(),
            )
        }
    }

    fun next() {
        val c = controller ?: return
        if (c.hasNextMediaItem()) {
            c.seekToNextMediaItem()
            c.play()
        } else if (lastRepeatMode == RepeatMode.ALL && c.mediaItemCount > 0) {
            c.seekToDefaultPosition(0)
            c.play()
        }
    }

    fun previous() {
        val c = controller ?: return
        if (c.currentPosition > 4000L) {
            c.seekTo(0L)
            _snapshot.update { it.copy(elapsedMs = 0L, elapsedSeconds = 0) }
        } else if (c.hasPreviousMediaItem()) {
            c.seekToPreviousMediaItem()
            c.play()
        } else if (lastRepeatMode == RepeatMode.ALL && c.mediaItemCount > 0) {
            c.seekToDefaultPosition(c.mediaItemCount - 1)
            c.play()
        } else {
            c.seekTo(0L)
            _snapshot.update { it.copy(elapsedMs = 0L, elapsedSeconds = 0) }
        }
    }

    fun setVolume(volume: Float, muted: Boolean) {
        lastVolume = volume.coerceIn(0f, 1f)
        lastMuted = muted
        controller?.volume = if (muted) 0f else lastVolume
    }

    fun setShuffle(enabled: Boolean) {
        lastShuffled = enabled
        controller?.shuffleModeEnabled = enabled
    }

    fun setRepeatMode(mode: RepeatMode) {
        lastRepeatMode = mode
        controller?.repeatMode = mapRepeatMode(mode)
    }

    fun setEqualizerGains(gains: EqualizerGains) {
        HardwareEqualizerBridge.applyGains(gains)
    }

    private fun mapRepeatMode(mode: RepeatMode): Int = when (mode) {
        RepeatMode.OFF -> Player.REPEAT_MODE_OFF
        RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        RepeatMode.ONE -> Player.REPEAT_MODE_ONE
    }

    private fun Track.toMediaItem(): MediaItem {
        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setTrackNumber(trackNumber)
        if (!coverUri.isNullOrBlank()) {
            metadataBuilder.setArtworkUri(Uri.parse(coverUri))
        }
        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(Uri.parse(contentUri))
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }

    private fun startProgressPolling() {
        if (progressJob?.isActive == true) return
        progressJob = scope.launch {
            while (isActive) {
                val c = controller
                if (c != null) {
                    val posMs = c.currentPosition.coerceAtLeast(0L)
                    val pos = (posMs / 1000L).toInt()
                    val dur = (c.duration.coerceAtLeast(0L) / 1000L).toInt()
                    _snapshot.update {
                        it.copy(
                            elapsedMs = posMs,
                            elapsedSeconds = pos,
                            durationSeconds = if (dur > 0) dur else it.durationSeconds,
                            isPlaying = c.isPlaying,
                        )
                    }
                }
                delay(150L)
            }
        }
    }

    private fun stopProgressPolling() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stopProgressPolling()
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        controller = null
    }
}
