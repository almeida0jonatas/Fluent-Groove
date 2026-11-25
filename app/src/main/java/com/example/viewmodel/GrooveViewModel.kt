package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.audio.GrooveAudioController
import com.example.data.MusicDatabase
import com.example.data.MusicRepository
import com.example.data.PreferencesRepository
import com.example.model.AccentColorOption
import com.example.model.AcrylicDensity
import com.example.model.Album
import com.example.model.AppLanguage
import com.example.model.AppThemeMode
import com.example.model.Artist
import com.example.model.EqualizerGains
import com.example.model.EqualizerPresetName
import com.example.model.LyricsUiState
import com.example.model.MyMusicTab
import com.example.model.NowPlayingArtworkStyle
import com.example.model.Playlist
import com.example.model.RecentPlayEntry
import com.example.model.RepeatMode
import com.example.model.ScreenRoute
import com.example.model.SongSortOrder
import com.example.model.Track
import com.example.ui.labelRes
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class GrooveUiState(
    val tracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val recentPlays: List<RecentPlayEntry> = emptyList(),
    val route: ScreenRoute = ScreenRoute.MyMusic(MyMusicTab.HOME),
    val backStack: List<ScreenRoute> = emptyList(),
    val searchQuery: String = "",
    val songSortOrder: SongSortOrder = SongSortOrder.DATE_ADDED,
    val sidebarExpanded: Boolean = true,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val accentColor: AccentColorOption = AccentColorOption.BLUE,
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val nowPlayingArtworkStyle: NowPlayingArtworkStyle = NowPlayingArtworkStyle.ARTIST_FULLSCREEN,
    val acrylicDensity: AcrylicDensity = AcrylicDensity.STANDARD,
    val showTitleBar: Boolean = true,
    val isMiniPlayer: Boolean = false,
    val isImporting: Boolean = false,
    val importingCount: Int = 0,
    val toastMessage: String? = null,
    // Playback
    val queue: List<Track> = emptyList(),
    val currentQueueIndex: Int = 0,
    val isPlaying: Boolean = false,
    val elapsedSeconds: Int = 0,
    val elapsedMs: Long = 0L,
    val durationSeconds: Int = 0,
    val volume: Float = 0.78f,
    val isMuted: Boolean = false,
    val isShuffled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.ALL,
    val eqPreset: EqualizerPresetName = EqualizerPresetName.FLAT,
    val eqGains: EqualizerGains = EqualizerGains(),
    // Lyrics
    val lyricsState: LyricsUiState = LyricsUiState.Idle,
) {
    val currentTrack: Track?
        get() = queue.getOrNull(currentQueueIndex)
}

class GrooveViewModel(application: Application) : AndroidViewModel(application) {

    private val database = MusicDatabase.getInstance(application)
    private val repository = MusicRepository(application, database.musicDao())
    private val prefsRepo = PreferencesRepository(application)

    private val _uiState = MutableStateFlow(GrooveUiState())
    val uiState: StateFlow<GrooveUiState> = _uiState.asStateFlow()

    private var toastJob: Job? = null
    private var lyricsJob: Job? = null
    private var lastLyricsTrackId: String? = null
    private var lastFetchedArtistsKey: String = ""
    private var hasRestoredPlayback: Boolean = false
    private var lastPersistedAtMs: Long = 0L
    private var wasPlayingSnapshot: Boolean = false

    private val audioController = GrooveAudioController(
        context = application,
        onTrackChanged = { newIdx ->
            val track = _uiState.value.queue.getOrNull(newIdx)
            if (track != null) {
                viewModelScope.launch {
                    repository.recordRecentPlay(track.id)
                }
                loadLyricsForTrack(track, forceRefresh = false)
                persistPlaybackState(force = true, overrideIndex = newIdx, overridePositionMs = 0L)
            }
        },
    )

    init {
        audioController.connect()
        seedDemoTrackOnFirstLaunch()
        observeLibraryData()
        observePreferences()
        observePlaybackState()
    }

    private fun localizedContext(): Context {
        val app = getApplication<Application>()
        return when (_uiState.value.appLanguage) {
            AppLanguage.SYSTEM -> app
            AppLanguage.ENGLISH -> {
                val config = Configuration(app.resources.configuration)
                config.setLocale(Locale.ENGLISH)
                app.createConfigurationContext(config)
            }
        }
    }

    private fun str(@StringRes resId: Int, vararg args: Any): String {
        val ctx = localizedContext()
        return if (args.isEmpty()) ctx.getString(resId) else ctx.getString(resId, *args)
    }

    private fun pluralStr(@PluralsRes resId: Int, count: Int): String {
        return localizedContext().resources.getQuantityString(resId, count, count)
    }

    private fun seedDemoTrackOnFirstLaunch() {
        viewModelScope.launch {
            val prefs = prefsRepo.prefsFlow.first()
            if (!prefs.demoSeeded) {
                repository.seedDemoTrack()
                prefsRepo.setDemoSeeded(true)
            }
        }
    }

    fun loadDemoTrack() {
        viewModelScope.launch {
            val track = repository.seedDemoTrack()
            prefsRepo.setDemoSeeded(true)
            showToast(str(R.string.toast_added_demo, track.title, track.artist))
        }
    }

    private fun observeLibraryData() {
        viewModelScope.launch {
            combine(
                repository.tracksFlow,
                repository.artistImageMapFlow,
                repository.playlistsFlow,
                repository.recentPlaysFlow,
            ) { tracks, artistImageMap, playlists, recentPlays ->
                val catalog = repository.buildCatalogFromTracks(tracks, artistImageMap)
                QuadData(tracks, catalog.albums, catalog.artists, playlists, recentPlays)
            }.collect { data ->
                _uiState.update {
                    it.copy(
                        tracks = data.tracks,
                        albums = data.albums,
                        artists = data.artists,
                        playlists = data.playlists,
                        recentPlays = data.recentPlays,
                    )
                }

                // Restore saved queue, active track, and exact position once tracks are loaded
                if (!hasRestoredPlayback && data.tracks.isNotEmpty()) {
                    hasRestoredPlayback = true
                    if (_uiState.value.queue.isEmpty()) {
                        launch { restoreSavedPlayback(data.tracks) }
                    }
                }

                // Automatically fetch artist portraits from the internet for newly added artists
                val artistNames = data.artists.map { it.name.trim() }.filter { it.isNotEmpty() }.distinct()
                val signature = artistNames.sorted().joinToString("|")
                if (artistNames.isNotEmpty() && signature != lastFetchedArtistsKey) {
                    lastFetchedArtistsKey = signature
                    launch {
                        repository.fetchMissingArtistImages(artistNames)
                    }
                }
            }
        }
    }

    private suspend fun restoreSavedPlayback(allTracks: List<Track>) {
        if (_uiState.value.queue.isNotEmpty()) return
        val saved = prefsRepo.getSavedPlaybackState()
        if (saved.queueTrackIds.isEmpty()) return

        val trackById = allTracks.associateBy { it.id }
        val restoredQueue = mutableListOf<Track>()
        var adjustedIndex = 0
        saved.queueTrackIds.forEachIndexed { oldIdx, id ->
            val track = trackById[id]
            if (track != null) {
                if (oldIdx == saved.currentIndex) {
                    adjustedIndex = restoredQueue.size
                } else if (oldIdx < saved.currentIndex) {
                    adjustedIndex = restoredQueue.size + 1
                }
                restoredQueue.add(track)
            }
        }
        if (restoredQueue.isEmpty()) return

        val safeIndex = adjustedIndex.coerceIn(0, restoredQueue.lastIndex)
        val activeTrack = restoredQueue[safeIndex]
        val maxDurationMs = (activeTrack.duration.coerceAtLeast(0)) * 1000L
        val safePositionMs = if (maxDurationMs > 0L) {
            saved.positionMs.coerceIn(0L, maxDurationMs)
        } else {
            saved.positionMs.coerceAtLeast(0L)
        }

        _uiState.update { state ->
            if (state.queue.isNotEmpty()) return@update state
            state.copy(
                queue = restoredQueue,
                currentQueueIndex = safeIndex,
                elapsedMs = safePositionMs,
                elapsedSeconds = (safePositionMs / 1000L).toInt(),
                durationSeconds = activeTrack.duration,
                isPlaying = false,
            )
        }
        audioController.restoreQueueIfIdle(
            queue = restoredQueue,
            startIndex = safeIndex,
            startPositionMs = safePositionMs,
        )
        loadLyricsForTrack(activeTrack, forceRefresh = false)
    }

    private fun persistPlaybackState(
        force: Boolean,
        overrideQueue: List<Track>? = null,
        overrideIndex: Int? = null,
        overridePositionMs: Long? = null,
    ) {
        val state = _uiState.value
        val queue = overrideQueue ?: state.queue
        if (queue.isEmpty()) return

        val now = System.currentTimeMillis()
        if (!force && now - lastPersistedAtMs < POSITION_SAVE_INTERVAL_MS) return
        lastPersistedAtMs = now

        val ids = queue.map { it.id }
        val idx = (overrideIndex ?: state.currentQueueIndex).coerceIn(0, queue.lastIndex)
        val posMs = (overridePositionMs ?: audioController.getCurrentPositionMs()).coerceAtLeast(0L)

        viewModelScope.launch(NonCancellable) {
            prefsRepo.savePlaybackState(
                queueTrackIds = ids,
                currentIndex = idx,
                positionMs = posMs,
            )
        }
    }

    fun onAppBackgrounded() {
        persistPlaybackState(force = true)
    }

    private fun observePreferences() {
        viewModelScope.launch {
            prefsRepo.prefsFlow.collect { prefs ->
                _uiState.update { state ->
                    state.copy(
                        themeMode = prefs.themeMode,
                        accentColor = prefs.accentColor,
                        appLanguage = prefs.appLanguage,
                        nowPlayingArtworkStyle = prefs.nowPlayingArtworkStyle,
                        acrylicDensity = prefs.acrylicDensity,
                        eqPreset = prefs.eqPreset,
                        eqGains = prefs.eqGains,
                        showTitleBar = prefs.showTitleBar,
                        volume = prefs.volume,
                        isShuffled = prefs.isShuffled,
                        repeatMode = prefs.repeatMode,
                    )
                }
                audioController.setVolume(prefs.volume, _uiState.value.isMuted)
                audioController.setShuffle(prefs.isShuffled)
                audioController.setRepeatMode(prefs.repeatMode)
                audioController.setEqualizerGains(prefs.eqGains)
            }
        }
    }

    private fun observePlaybackState() {
        viewModelScope.launch {
            audioController.snapshot.collect { snap ->
                var activeTrack: Track? = null
                _uiState.update { state ->
                    if (state.queue.isEmpty()) return@update state
                    val safeIdx = snap.currentQueueIndex.coerceIn(0, (state.queue.lastIndex).coerceAtLeast(0))
                    val track = state.queue.getOrNull(safeIdx)
                    activeTrack = track
                    val fallbackDur = track?.duration ?: 0
                    state.copy(
                        isPlaying = snap.isPlaying,
                        currentQueueIndex = safeIdx,
                        elapsedSeconds = snap.elapsedSeconds,
                        elapsedMs = snap.elapsedMs,
                        durationSeconds = if (snap.durationSeconds > 0) snap.durationSeconds else fallbackDur,
                    )
                }
                val track = activeTrack
                if (track != null && track.id != lastLyricsTrackId) {
                    loadLyricsForTrack(track, forceRefresh = false)
                }
                // Save immediately when playback pauses, or periodically while playing
                if (wasPlayingSnapshot && !snap.isPlaying) {
                    persistPlaybackState(force = true, overridePositionMs = snap.elapsedMs)
                } else if (snap.isPlaying) {
                    persistPlaybackState(force = false, overridePositionMs = snap.elapsedMs)
                }
                wasPlayingSnapshot = snap.isPlaying

                snap.errorMessage?.let { showToast(it) }
            }
        }
    }

    private fun loadLyricsForTrack(track: Track, forceRefresh: Boolean) {
        if (!forceRefresh && lastLyricsTrackId == track.id && _uiState.value.lyricsState !is LyricsUiState.Idle) {
            return
        }
        lastLyricsTrackId = track.id
        lyricsJob?.cancel()
        _uiState.update { it.copy(lyricsState = LyricsUiState.Loading(track.id)) }
        lyricsJob = viewModelScope.launch {
            val result = runCatching {
                repository.getLyricsForTrack(track, forceRefresh = forceRefresh)
            }
            if (_uiState.value.currentTrack?.id != track.id) return@launch
            result.fold(
                onSuccess = { lyrics ->
                    if (lyrics != null && lyrics.hasAnyLyrics) {
                        _uiState.update { it.copy(lyricsState = LyricsUiState.Loaded(lyrics)) }
                    } else {
                        _uiState.update { it.copy(lyricsState = LyricsUiState.NotFound(track.id)) }
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            lyricsState = LyricsUiState.Error(
                                trackId = track.id,
                                message = err.localizedMessage ?: "Unable to load lyrics",
                            )
                        )
                    }
                },
            )
        }
    }

    fun refreshCurrentLyrics() {
        val track = _uiState.value.currentTrack ?: return
        loadLyricsForTrack(track, forceRefresh = true)
    }

    fun importLyricsFile(uri: Uri) {
        val track = _uiState.value.currentTrack ?: return
        lyricsJob?.cancel()
        viewModelScope.launch {
            val imported = repository.importLocalLyrics(track.id, uri)
            if (imported != null && imported.hasAnyLyrics) {
                lastLyricsTrackId = track.id
                _uiState.update { it.copy(lyricsState = LyricsUiState.Loaded(imported)) }
                val resId = if (imported.hasSyncedLyrics) {
                    R.string.toast_synced_lyrics_imported
                } else {
                    R.string.toast_lyrics_imported
                }
                showToast(str(resId, track.title))
            } else {
                showToast(str(R.string.toast_lyrics_import_failed))
            }
        }
    }

    fun showToast(message: String) {
        toastJob?.cancel()
        _uiState.update { it.copy(toastMessage = message) }
        toastJob = viewModelScope.launch {
            delay(2600L)
            _uiState.update { it.copy(toastMessage = null) }
        }
    }

    // Navigation
    fun navigateTo(next: ScreenRoute) {
        _uiState.update { state ->
            if (state.route == next) return@update state
            val updatedStack = (state.backStack + state.route).takeLast(20)
            state.copy(route = next, backStack = updatedStack)
        }
    }

    fun navigateBack(): Boolean {
        val state = _uiState.value
        if (state.isMiniPlayer) {
            _uiState.update { it.copy(isMiniPlayer = false) }
            return true
        }
        if (state.route is ScreenRoute.MyMusic && state.searchQuery.isNotBlank()) {
            _uiState.update { it.copy(searchQuery = "") }
            return true
        }
        val previous = state.backStack.lastOrNull() ?: return false
        _uiState.update {
            it.copy(
                route = previous,
                backStack = it.backStack.dropLast(1),
            )
        }
        return true
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleSidebarExpanded() {
        _uiState.update { it.copy(sidebarExpanded = !it.sidebarExpanded) }
    }

    fun setSongSortOrder(order: SongSortOrder) {
        _uiState.update { it.copy(songSortOrder = order) }
    }

    fun setMiniPlayer(enabled: Boolean) {
        _uiState.update { it.copy(isMiniPlayer = enabled) }
    }

    // Importing local audio files & folders (Zero-permission SAF)
    fun importAudioUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, importingCount = uris.size) }
            val result = repository.importAudioUris(uris)
            _uiState.update { it.copy(isImporting = false, importingCount = 0) }
            if (result.trackCount == 0) {
                showToast(str(R.string.toast_no_audio_files))
            } else {
                showToast(
                    str(
                        R.string.toast_organized_summary,
                        pluralStr(R.plurals.song_count, result.trackCount),
                        pluralStr(R.plurals.album_count, result.albumCount),
                        pluralStr(R.plurals.artist_count, result.artistCount),
                    )
                )
            }
        }
    }

    fun importMusicFolder(treeUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, importingCount = 0) }
            val result = repository.importFolderTree(treeUri)
            _uiState.update { it.copy(isImporting = false, importingCount = 0) }
            if (result.trackCount == 0) {
                showToast(str(R.string.toast_no_audio_folder))
            } else {
                showToast(
                    str(
                        R.string.toast_organized_summary,
                        pluralStr(R.plurals.song_count, result.trackCount),
                        pluralStr(R.plurals.album_count, result.albumCount),
                        pluralStr(R.plurals.artist_count, result.artistCount),
                    )
                )
            }
        }
    }

    // Playback controls
    private fun startPlaybackAt(newQueue: List<Track>, index: Int) {
        if (newQueue.isEmpty()) return
        val safeIdx = index.coerceIn(0, newQueue.lastIndex)
        val track = newQueue[safeIdx]
        _uiState.update {
            it.copy(
                queue = newQueue,
                currentQueueIndex = safeIdx,
                elapsedSeconds = 0,
                elapsedMs = 0L,
                durationSeconds = track.duration,
                isPlaying = true,
            )
        }
        audioController.setQueueAndPlay(newQueue, safeIdx, autoPlay = true, startPositionMs = 0L)
        viewModelScope.launch { repository.recordRecentPlay(track.id) }
        loadLyricsForTrack(track, forceRefresh = false)
        persistPlaybackState(force = true, overrideQueue = newQueue, overrideIndex = safeIdx, overridePositionMs = 0L)
    }

    fun playTrack(track: Track, contextTracks: List<Track>) {
        val idx = contextTracks.indexOfFirst { it.id == track.id }
        if (idx >= 0) {
            startPlaybackAt(contextTracks, idx)
        } else {
            startPlaybackAt(listOf(track) + contextTracks, 0)
        }
    }

    fun playAlbum(album: Album) {
        val albumTracks = _uiState.value.tracks
            .filter { it.albumId == album.id }
            .sortedBy { it.trackNumber }
        if (albumTracks.isNotEmpty()) {
            startPlaybackAt(albumTracks, 0)
        }
    }

    fun playArtist(artistId: String) {
        val artistTracks = _uiState.value.tracks
            .filter { it.artistId == artistId }
            .sortedWith(compareBy<Track> { it.album.lowercase() }.thenBy { it.trackNumber })
        if (artistTracks.isNotEmpty()) {
            startPlaybackAt(artistTracks, 0)
        }
    }

    fun playTrackList(tracks: List<Track>) {
        if (tracks.isNotEmpty()) {
            startPlaybackAt(tracks, 0)
        }
    }

    fun shuffleTrackList(sourceTracks: List<Track>) {
        if (sourceTracks.isEmpty()) return
        val shuffled = sourceTracks.shuffled()
        viewModelScope.launch { prefsRepo.setShuffled(true) }
        startPlaybackAt(shuffled, 0)
    }

    fun togglePlayPause() {
        val state = _uiState.value
        if (state.currentTrack == null) {
            if (state.tracks.isNotEmpty()) {
                startPlaybackAt(state.tracks, 0)
            }
            return
        }
        audioController.togglePlayPause()
    }

    fun nextTrack() = audioController.next()

    fun previousTrack() = audioController.previous()

    fun seekTo(seconds: Int) {
        val ms = (seconds.coerceAtLeast(0)) * 1000L
        _uiState.update { it.copy(elapsedSeconds = seconds.coerceAtLeast(0), elapsedMs = ms) }
        audioController.seekToMs(ms)
        persistPlaybackState(force = true, overridePositionMs = ms)
    }

    fun seekToMs(positionMs: Long) {
        val clamped = positionMs.coerceAtLeast(0L)
        _uiState.update { it.copy(elapsedSeconds = (clamped / 1000L).toInt(), elapsedMs = clamped) }
        audioController.seekToMs(clamped)
        persistPlaybackState(force = true, overridePositionMs = clamped)
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        val muted = clamped == 0f
        _uiState.update { it.copy(volume = clamped, isMuted = muted) }
        audioController.setVolume(clamped, muted)
        viewModelScope.launch { prefsRepo.setVolume(clamped) }
    }

    fun toggleMute() {
        val nextMuted = !_uiState.value.isMuted
        _uiState.update { it.copy(isMuted = nextMuted) }
        audioController.setVolume(_uiState.value.volume, nextMuted)
    }

    fun toggleShuffle() {
        val next = !_uiState.value.isShuffled
        _uiState.update { it.copy(isShuffled = next) }
        audioController.setShuffle(next)
        viewModelScope.launch { prefsRepo.setShuffled(next) }
    }

    fun cycleRepeatMode() {
        val next = when (_uiState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _uiState.update { it.copy(repeatMode = next) }
        audioController.setRepeatMode(next)
        viewModelScope.launch { prefsRepo.setRepeatMode(next) }
    }

    // Queue management
    fun addToQueue(track: Track) {
        val wasEmpty = _uiState.value.queue.isEmpty()
        val updatedQueue = _uiState.value.queue + track
        _uiState.update { it.copy(queue = updatedQueue) }
        if (wasEmpty) {
            audioController.setQueueAndPlay(listOf(track), 0, autoPlay = false, startPositionMs = 0L)
            loadLyricsForTrack(track, forceRefresh = false)
        } else {
            audioController.appendToQueue(listOf(track))
        }
        persistPlaybackState(force = true, overrideQueue = updatedQueue)
        showToast(str(R.string.toast_added_to_queue, track.title))
    }

    fun addAlbumToQueue(album: Album) {
        val albumTracks = _uiState.value.tracks
            .filter { it.albumId == album.id }
            .sortedBy { it.trackNumber }
        if (albumTracks.isEmpty()) return
        val wasEmpty = _uiState.value.queue.isEmpty()
        val updatedQueue = _uiState.value.queue + albumTracks
        _uiState.update { it.copy(queue = updatedQueue) }
        if (wasEmpty) {
            audioController.setQueueAndPlay(albumTracks, 0, autoPlay = false, startPositionMs = 0L)
            loadLyricsForTrack(albumTracks.first(), forceRefresh = false)
        } else {
            audioController.appendToQueue(albumTracks)
        }
        persistPlaybackState(force = true, overrideQueue = updatedQueue)
        showToast(str(R.string.toast_added_to_queue, album.title))
    }

    fun jumpToQueueIndex(index: Int) {
        val queue = _uiState.value.queue
        if (index !in queue.indices) return
        val target = queue[index]
        _uiState.update { it.copy(currentQueueIndex = index, elapsedSeconds = 0, elapsedMs = 0L) }
        audioController.jumpToIndex(index)
        loadLyricsForTrack(target, forceRefresh = false)
        persistPlaybackState(force = true, overrideIndex = index, overridePositionMs = 0L)
    }

    fun moveQueueItem(fromIdx: Int, toIdx: Int) {
        val queue = _uiState.value.queue
        if (fromIdx !in queue.indices || toIdx !in queue.indices || fromIdx == toIdx) return
        val mutable = queue.toMutableList()
        val item = mutable.removeAt(fromIdx)
        mutable.add(toIdx, item)
        var nextCurrIndex = _uiState.value.currentQueueIndex
        _uiState.update { state ->
            val curr = state.currentQueueIndex
            nextCurrIndex = when {
                curr == fromIdx -> toIdx
                fromIdx < curr && toIdx >= curr -> curr - 1
                fromIdx > curr && toIdx <= curr -> curr + 1
                else -> curr
            }
            state.copy(queue = mutable, currentQueueIndex = nextCurrIndex)
        }
        audioController.moveQueueItem(fromIdx, toIdx)
        persistPlaybackState(force = true, overrideQueue = mutable, overrideIndex = nextCurrIndex)
    }

    fun removeQueueItem(index: Int) {
        val queue = _uiState.value.queue
        if (index !in queue.indices) return
        val mutable = queue.toMutableList().also { it.removeAt(index) }
        var nextCurrIndex = 0
        _uiState.update { state ->
            nextCurrIndex = when {
                mutable.isEmpty() -> 0
                index < state.currentQueueIndex -> (state.currentQueueIndex - 1).coerceAtLeast(0)
                index == state.currentQueueIndex -> state.currentQueueIndex.coerceAtMost(mutable.lastIndex)
                else -> state.currentQueueIndex
            }
            state.copy(queue = mutable, currentQueueIndex = nextCurrIndex)
        }
        audioController.removeQueueItem(index)
        if (mutable.isEmpty()) {
            viewModelScope.launch { prefsRepo.clearSavedPlaybackState() }
        } else {
            persistPlaybackState(force = true, overrideQueue = mutable, overrideIndex = nextCurrIndex)
        }
    }

    fun clearQueue() {
        lyricsJob?.cancel()
        lastLyricsTrackId = null
        audioController.clearQueue()
        viewModelScope.launch { prefsRepo.clearSavedPlaybackState() }
        _uiState.update {
            it.copy(
                queue = emptyList(),
                currentQueueIndex = 0,
                isPlaying = false,
                elapsedSeconds = 0,
                elapsedMs = 0L,
                durationSeconds = 0,
                lyricsState = LyricsUiState.Idle,
            )
        }
    }

    // Playlists
    fun createPlaylist(name: String, initialTrackIds: List<String> = emptyList()) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val playlist = Playlist(
            id = "pl-${System.currentTimeMillis()}",
            name = trimmed,
            description = str(R.string.toast_playlist_custom_desc),
            trackIds = initialTrackIds.distinct(),
            createdAt = System.currentTimeMillis(),
        )
        viewModelScope.launch {
            repository.savePlaylist(playlist)
            navigateTo(ScreenRoute.PlaylistDetail(playlist.id))
            showToast(str(R.string.toast_created_playlist, trimmed))
        }
    }

    fun addTrackToPlaylist(playlistId: String, trackId: String) {
        val pl = _uiState.value.playlists.firstOrNull { it.id == playlistId } ?: return
        if (trackId in pl.trackIds) {
            showToast(str(R.string.toast_already_in_playlist, pl.name))
            return
        }
        val updated = pl.copy(trackIds = pl.trackIds + trackId)
        viewModelScope.launch {
            repository.savePlaylist(updated)
            showToast(str(R.string.toast_added_to_playlist, pl.name))
        }
    }

    fun addAlbumToPlaylist(playlistId: String, albumTracks: List<Track>) {
        val pl = _uiState.value.playlists.firstOrNull { it.id == playlistId } ?: return
        val merged = (pl.trackIds + albumTracks.map { it.id }).distinct()
        val updated = pl.copy(trackIds = merged)
        viewModelScope.launch {
            repository.savePlaylist(updated)
            showToast(str(R.string.toast_added_album_to_playlist, albumTracks.size, pl.name))
        }
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        val pl = _uiState.value.playlists.firstOrNull { it.id == playlistId } ?: return
        val updated = pl.copy(trackIds = pl.trackIds.filterNot { it == trackId })
        viewModelScope.launch { repository.savePlaylist(updated) }
    }

    fun renamePlaylist(playlistId: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        val pl = _uiState.value.playlists.firstOrNull { it.id == playlistId } ?: return
        viewModelScope.launch {
            repository.savePlaylist(pl.copy(name = trimmed))
            showToast(str(R.string.toast_renamed_playlist, trimmed))
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            navigateTo(ScreenRoute.MyMusic(MyMusicTab.HOME))
            showToast(str(R.string.toast_deleted_playlist))
        }
    }

    // Settings & Equalizer
    fun setThemeMode(mode: AppThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
        viewModelScope.launch { prefsRepo.setThemeMode(mode) }
    }

    fun setAccentColor(accent: AccentColorOption) {
        _uiState.update { it.copy(accentColor = accent) }
        viewModelScope.launch { prefsRepo.setAccentColor(accent) }
    }

    fun setAppLanguage(language: AppLanguage) {
        _uiState.update { it.copy(appLanguage = language) }
        viewModelScope.launch { prefsRepo.setAppLanguage(language) }
    }

    fun setNowPlayingArtworkStyle(style: NowPlayingArtworkStyle) {
        _uiState.update { it.copy(nowPlayingArtworkStyle = style) }
        viewModelScope.launch { prefsRepo.setNowPlayingArtworkStyle(style) }
    }

    fun setAcrylicDensity(density: AcrylicDensity) {
        _uiState.update { it.copy(acrylicDensity = density) }
        viewModelScope.launch { prefsRepo.setAcrylicDensity(density) }
    }

    fun toggleShowTitleBar() {
        val next = !_uiState.value.showTitleBar
        _uiState.update { it.copy(showTitleBar = next) }
        viewModelScope.launch { prefsRepo.setShowTitleBar(next) }
    }

    fun selectEqPreset(preset: EqualizerPresetName) {
        val gains = preset.gains
        _uiState.update { it.copy(eqPreset = preset, eqGains = gains) }
        audioController.setEqualizerGains(gains)
        viewModelScope.launch { prefsRepo.setEqualizer(preset, gains) }
        showToast(str(R.string.toast_eq_preset, str(preset.labelRes())))
    }

    fun updateEqGains(gains: EqualizerGains) {
        _uiState.update { it.copy(eqPreset = EqualizerPresetName.CUSTOM, eqGains = gains) }
        audioController.setEqualizerGains(gains)
        viewModelScope.launch { prefsRepo.setEqualizer(EqualizerPresetName.CUSTOM, gains) }
    }

    fun resetLibrary() {
        lyricsJob?.cancel()
        lastLyricsTrackId = null
        audioController.clearQueue()
        viewModelScope.launch {
            repository.clearAllLibraryData()
            prefsRepo.resetAll()
            _uiState.update {
                it.copy(
                    queue = emptyList(),
                    currentQueueIndex = 0,
                    isPlaying = false,
                    elapsedSeconds = 0,
                    elapsedMs = 0L,
                    durationSeconds = 0,
                    lyricsState = LyricsUiState.Idle,
                    route = ScreenRoute.MyMusic(MyMusicTab.HOME),
                    backStack = emptyList(),
                )
            }
            showToast(str(R.string.toast_library_cleared))
        }
    }

    override fun onCleared() {
        persistPlaybackState(force = true)
        audioController.release()
        super.onCleared()
    }

    private data class QuadData(
        val tracks: List<Track>,
        val albums: List<Album>,
        val artists: List<Artist>,
        val playlists: List<Playlist>,
        val recentPlays: List<RecentPlayEntry>,
    )

    companion object {
        private const val POSITION_SAVE_INTERVAL_MS = 2_000L
    }
}
