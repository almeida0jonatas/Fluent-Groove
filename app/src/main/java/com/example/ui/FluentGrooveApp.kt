package com.example.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.Album
import com.example.model.MyMusicTab
import com.example.model.ScreenRoute
import com.example.model.Track
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.MiniPlayerView
import com.example.ui.components.PlayerBar
import com.example.ui.components.PlaylistNameDialog
import com.example.ui.components.TopAcrylicBar
import com.example.ui.screens.AlbumDetailScreen
import com.example.ui.screens.ArtistDetailScreen
import com.example.ui.screens.MyMusicScreen
import com.example.ui.screens.NowPlayingScreen
import com.example.ui.screens.PlaylistScreen
import com.example.ui.screens.RecentPlaysScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.FgSurface
import com.example.ui.theme.FgToastBg
import com.example.viewmodel.GrooveViewModel

@Composable
fun FluentGrooveApp(viewModel: GrooveViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Dialog states
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var trackPendingPlaylist by remember { mutableStateOf<Track?>(null) }
    var albumPendingPlaylist by remember { mutableStateOf<Pair<Album, List<Track>>?>(null) }

    // Request POST_NOTIFICATIONS on Android 13+ so Media3 playback notification displays cleanly
    val notificationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {},
    )
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Zero-permission Storage Access Framework (SAF) audio pickers
    val pickAudioFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                viewModel.importAudioUris(uris)
            }
        },
    )
    val pickMusicFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { treeUri ->
            if (treeUri != null) {
                viewModel.importMusicFolder(treeUri)
            }
        },
    )

    val pickLyricsFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.importLyricsFile(uri)
            }
        },
    )

    val launchFilePicker: () -> Unit = {
        pickAudioFilesLauncher.launch(arrayOf("audio/*"))
    }
    val launchFolderPicker: () -> Unit = {
        pickMusicFolderLauncher.launch(null)
    }
    val launchLyricsPicker: () -> Unit = {
        pickLyricsFileLauncher.launch(arrayOf("*/*"))
    }

    // System back button support for mini player, active My Music search, and navigation stack
    val hasActiveMyMusicSearch = state.route is ScreenRoute.MyMusic && state.searchQuery.isNotBlank()
    BackHandler(enabled = state.isMiniPlayer || hasActiveMyMusicSearch || state.backStack.isNotEmpty() || state.route !is ScreenRoute.MyMusic) {
        viewModel.navigateBackOr(ScreenRoute.MyMusic(MyMusicTab.HOME))
    }

    if (showCreatePlaylistDialog) {
        PlaylistNameDialog(
            title = stringResource(R.string.action_new_playlist),
            confirmLabel = stringResource(R.string.action_create),
            onDismiss = { showCreatePlaylistDialog = false },
            onConfirm = { name ->
                showCreatePlaylistDialog = false
                viewModel.createPlaylist(name)
            },
        )
    }

    trackPendingPlaylist?.let { pendingTrack ->
        AddToPlaylistDialog(
            playlists = state.playlists,
            onDismiss = { trackPendingPlaylist = null },
            onSelectPlaylist = { playlistId ->
                trackPendingPlaylist = null
                viewModel.addTrackToPlaylist(playlistId, pendingTrack.id)
            },
            onCreateNewPlaylist = { newName ->
                trackPendingPlaylist = null
                viewModel.createPlaylist(newName, listOf(pendingTrack.id))
            },
        )
    }

    albumPendingPlaylist?.let { (_, albumTracks) ->
        AddToPlaylistDialog(
            playlists = state.playlists,
            onDismiss = { albumPendingPlaylist = null },
            onSelectPlaylist = { playlistId ->
                albumPendingPlaylist = null
                viewModel.addAlbumToPlaylist(playlistId, albumTracks)
            },
            onCreateNewPlaylist = { newName ->
                albumPendingPlaylist = null
                viewModel.createPlaylist(newName, albumTracks.map { it.id })
            },
        )
    }

    // Mini Player full-screen compact view matching MiniPlayer.tsx
    if (state.isMiniPlayer) {
        MiniPlayerView(
            currentTrack = state.currentTrack,
            isPlaying = state.isPlaying,
            elapsedSeconds = state.elapsedSeconds,
            durationSeconds = state.durationSeconds,
            isShuffled = state.isShuffled,
            repeatMode = state.repeatMode,
            hasNextTrack = state.queue.size > 1 || state.repeatMode != com.example.model.RepeatMode.OFF,
            onTogglePlay = viewModel::togglePlayPause,
            onNext = viewModel::nextTrack,
            onPrevious = viewModel::previousTrack,
            onSeek = viewModel::seekTo,
            onToggleShuffle = viewModel::toggleShuffle,
            onCycleRepeat = viewModel::cycleRepeatMode,
            onRestoreFull = { viewModel.setMiniPlayer(false) },
        )
        return
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(FgSurface),
    ) {
        val isWideScreen = maxWidth >= 720.dp

        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                val isNowPlayingRoute = state.route is ScreenRoute.NowPlaying
                if (!isNowPlayingRoute) {
                    val canNavigateBack = state.route is ScreenRoute.Settings ||
                        state.route is ScreenRoute.PlaylistDetail ||
                        state.route is ScreenRoute.RecentPlays
                    TopAcrylicBar(
                        acrylicDensity = state.acrylicDensity,
                        canNavigateBack = canNavigateBack,
                        isSettingsActive = state.route is ScreenRoute.Settings,
                        onNavigateBack = {
                            viewModel.navigateBackOr(ScreenRoute.MyMusic(MyMusicTab.HOME))
                        },
                        onOpenHome = viewModel::navigateHome,
                        onOpenSettings = { viewModel.navigateTo(ScreenRoute.Settings) },
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(FgSurface),
                ) {
                    when (val currentRoute = state.route) {
                        is ScreenRoute.MyMusic -> {
                            MyMusicScreen(
                                activeTab = currentRoute.tab,
                                searchQuery = state.searchQuery,
                                albums = state.albums,
                                artists = state.artists,
                                tracks = state.tracks,
                                playlists = state.playlists,
                                recentPlays = state.recentPlays,
                                songSortOrder = state.songSortOrder,
                                currentTrackId = state.currentTrack?.id,
                                isPlaying = state.isPlaying,
                                isImporting = state.isImporting,
                                importingCount = state.importingCount,
                                onChangeTab = viewModel::selectMyMusicTab,
                                onSearchQueryChange = viewModel::onSearchQueryChanged,
                                onChangeSortOrder = viewModel::setSongSortOrder,
                                onPlayAlbum = viewModel::playAlbum,
                                onPlayArtist = viewModel::playArtist,
                                onPlayPlaylist = viewModel::playTrackList,
                                onPlayTrack = viewModel::playTrack,
                                onTogglePlay = viewModel::togglePlayPause,
                                onShuffleAll = viewModel::shuffleTrackList,
                                onOpenAlbum = { id -> viewModel.navigateTo(ScreenRoute.AlbumDetail(id)) },
                                onOpenArtist = { id -> viewModel.navigateTo(ScreenRoute.ArtistDetail(id)) },
                                onOpenPlaylist = { id -> viewModel.navigateTo(ScreenRoute.PlaylistDetail(id)) },
                                onRequestCreatePlaylist = { showCreatePlaylistDialog = true },
                                onOpenRecentPlays = { viewModel.navigateTo(ScreenRoute.RecentPlays) },
                                onAddToQueue = viewModel::addToQueue,
                                onAddAlbumToQueue = viewModel::addAlbumToQueue,
                                onRequestAddTrackToPlaylist = { track -> trackPendingPlaylist = track },
                                onImportFiles = launchFilePicker,
                                onImportFolder = launchFolderPicker,
                                onLoadDemoTrack = viewModel::loadDemoTrack,
                            )
                        }

                                is ScreenRoute.AlbumDetail -> {
                                    val album = state.albums.firstOrNull { it.id == currentRoute.albumId }
                                    if (album != null) {
                                        val albumTracks = state.tracks
                                            .filter { it.albumId == album.id }
                                            .sortedWith(
                                                compareBy<Track> { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE }
                                                    .thenBy { it.title.lowercase() }
                                            )
                                        AlbumDetailScreen(
                                            album = album,
                                            tracks = albumTracks,
                                            currentTrackId = state.currentTrack?.id,
                                            isPlaying = state.isPlaying,
                                            onBack = { viewModel.navigateBackOr(ScreenRoute.MyMusic(MyMusicTab.ALBUMS)) },
                                            onPlayAlbum = { viewModel.playAlbum(album) },
                                            onShuffleAlbum = { viewModel.shuffleTrackList(albumTracks) },
                                            onPlayTrack = viewModel::playTrack,
                                            onTogglePlay = viewModel::togglePlayPause,
                                            onAddToQueue = viewModel::addToQueue,
                                            onAddAlbumToQueue = { viewModel.addAlbumToQueue(album) },
                                            onRequestAddTrackToPlaylist = { t -> trackPendingPlaylist = t },
                                            onRequestAddAlbumToPlaylist = { albumPendingPlaylist = album to albumTracks },
                                            onOpenArtist = { id -> viewModel.navigateTo(ScreenRoute.ArtistDetail(id)) },
                                        )
                                    } else {
                                        Text(stringResource(R.string.album_not_found), modifier = Modifier.padding(24.dp))
                                    }
                                }

                                is ScreenRoute.ArtistDetail -> {
                                    val artist = state.artists.firstOrNull { it.id == currentRoute.artistId }
                                    if (artist != null) {
                                        val artistAlbums = state.albums
                                            .filter { it.artistId == artist.id }
                                            .sortedWith(
                                                compareByDescending<Album> { it.year }
                                                    .thenBy { it.title.lowercase() }
                                            )
                                        val albumOrder = artistAlbums.withIndex().associate { (index, alb) -> alb.id to index }
                                        val artistTracks = state.tracks
                                            .filter { it.artistId == artist.id }
                                            .sortedWith(
                                                compareBy<Track> { albumOrder[it.albumId] ?: Int.MAX_VALUE }
                                                    .thenByDescending { it.year }
                                                    .thenBy { it.album.lowercase() }
                                                    .thenBy { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE }
                                                    .thenBy { it.title.lowercase() }
                                            )
                                        ArtistDetailScreen(
                                            artist = artist,
                                            albums = artistAlbums,
                                            tracks = artistTracks,
                                            currentTrackId = state.currentTrack?.id,
                                            isPlaying = state.isPlaying,
                                            onBack = { viewModel.navigateBackOr(ScreenRoute.MyMusic(MyMusicTab.ARTISTS)) },
                                            onPlayAll = viewModel::playTrackList,
                                            onShuffleAll = viewModel::shuffleTrackList,
                                            onPlayAlbum = viewModel::playAlbum,
                                            onOpenAlbum = { id -> viewModel.navigateTo(ScreenRoute.AlbumDetail(id)) },
                                            onPlayTrack = viewModel::playTrack,
                                            onTogglePlay = viewModel::togglePlayPause,
                                            onAddToQueue = viewModel::addToQueue,
                                            onRequestAddTrackToPlaylist = { t -> trackPendingPlaylist = t },
                                        )
                                    } else {
                                        Text(stringResource(R.string.artist_not_found), modifier = Modifier.padding(24.dp))
                                    }
                                }

                                is ScreenRoute.RecentPlays -> {
                                    RecentPlaysScreen(
                                        recentPlays = state.recentPlays,
                                        tracks = state.tracks,
                                        albums = state.albums,
                                        currentTrackId = state.currentTrack?.id,
                                        isPlaying = state.isPlaying,
                                        onPlayAlbum = viewModel::playAlbum,
                                        onPlayTrack = viewModel::playTrack,
                                        onTogglePlay = viewModel::togglePlayPause,
                                        onAddToQueue = viewModel::addToQueue,
                                        onRequestAddTrackToPlaylist = { t -> trackPendingPlaylist = t },
                                        onOpenAlbum = { id -> viewModel.navigateTo(ScreenRoute.AlbumDetail(id)) },
                                        onOpenArtist = { id -> viewModel.navigateTo(ScreenRoute.ArtistDetail(id)) },
                                    )
                                }

                                is ScreenRoute.NowPlaying -> {
                                    val currentArtistImageUrl = state.currentTrack?.let { tr ->
                                        state.artists.firstOrNull { it.id == tr.artistId }?.imageUrl
                                    }
                                    NowPlayingScreen(
                                        currentTrack = state.currentTrack,
                                        artistImageUrl = currentArtistImageUrl,
                                        artworkStyle = state.nowPlayingArtworkStyle,
                                        isPlaying = state.isPlaying,
                                        elapsedSeconds = state.elapsedSeconds,
                                        elapsedMs = state.elapsedMs,
                                        durationSeconds = state.durationSeconds,
                                        isShuffled = state.isShuffled,
                                        repeatMode = state.repeatMode,
                                        queue = state.queue,
                                        currentQueueIndex = state.currentQueueIndex,
                                        lyricsState = state.lyricsState,
                                        onBack = {
                                            viewModel.navigateBackOr(ScreenRoute.MyMusic(MyMusicTab.HOME))
                                        },
                                        onTogglePlay = viewModel::togglePlayPause,
                                        onNext = viewModel::nextTrack,
                                        onPrevious = viewModel::previousTrack,
                                        onSeek = viewModel::seekTo,
                                        onSeekMs = viewModel::seekToMs,
                                        onToggleShuffle = viewModel::toggleShuffle,
                                        onCycleRepeat = viewModel::cycleRepeatMode,
                                        onJumpToQueueIndex = viewModel::jumpToQueueIndex,
                                        onMoveQueueItem = viewModel::moveQueueItem,
                                        onRemoveQueueItem = viewModel::removeQueueItem,
                                        onClearQueue = viewModel::clearQueue,
                                        onSaveQueueAsPlaylist = { name ->
                                            viewModel.createPlaylist(name, state.queue.map { it.id })
                                        },
                                        onRequestAddTrackToPlaylist = { t -> trackPendingPlaylist = t },
                                        onRefreshLyrics = viewModel::refreshCurrentLyrics,
                                        onImportLyricsFile = launchLyricsPicker,
                                        onEnterMiniPlayer = { viewModel.setMiniPlayer(true) },
                                        onOpenAlbum = { id -> viewModel.navigateTo(ScreenRoute.AlbumDetail(id)) },
                                        onOpenArtist = { id -> viewModel.navigateTo(ScreenRoute.ArtistDetail(id)) },
                                    )
                                }

                                is ScreenRoute.PlaylistDetail -> {
                                    val playlist = state.playlists.firstOrNull { it.id == currentRoute.playlistId }
                                    if (playlist != null) {
                                        PlaylistScreen(
                                            playlist = playlist,
                                            allTracks = state.tracks,
                                            currentTrackId = state.currentTrack?.id,
                                            isPlaying = state.isPlaying,
                                            onPlayPlaylist = viewModel::playTrackList,
                                            onShufflePlaylist = viewModel::shuffleTrackList,
                                            onPlayTrack = viewModel::playTrack,
                                            onTogglePlay = viewModel::togglePlayPause,
                                            onAddToQueue = viewModel::addToQueue,
                                            onRequestAddTrackToPlaylist = { t -> trackPendingPlaylist = t },
                                            onRemoveFromPlaylist = { trackId ->
                                                viewModel.removeTrackFromPlaylist(playlist.id, trackId)
                                            },
                                            onRenamePlaylist = { newName ->
                                                viewModel.renamePlaylist(playlist.id, newName)
                                            },
                                            onDeletePlaylist = { viewModel.deletePlaylist(playlist.id) },
                                            onOpenAlbum = { id -> viewModel.navigateTo(ScreenRoute.AlbumDetail(id)) },
                                            onOpenArtist = { id -> viewModel.navigateTo(ScreenRoute.ArtistDetail(id)) },
                                        )
                                    } else {
                                        Text(stringResource(R.string.playlist_not_found), modifier = Modifier.padding(24.dp))
                                    }
                                }

                                is ScreenRoute.Settings -> {
                                    SettingsScreen(
                                        themeMode = state.themeMode,
                                        accentColor = state.accentColor,
                                        appLanguage = state.appLanguage,
                                        nowPlayingArtworkStyle = state.nowPlayingArtworkStyle,
                                        acrylicDensity = state.acrylicDensity,
                                        eqPreset = state.eqPreset,
                                        eqGains = state.eqGains,
                                        trackCount = state.tracks.size,
                                        albumCount = state.albums.size,
                                        playlistCount = state.playlists.size,
                                        onChangeThemeMode = viewModel::setThemeMode,
                                        onChangeAccentColor = viewModel::setAccentColor,
                                        onChangeAppLanguage = viewModel::setAppLanguage,
                                        onChangeNowPlayingArtworkStyle = viewModel::setNowPlayingArtworkStyle,
                                        onChangeAcrylicDensity = viewModel::setAcrylicDensity,
                                        onSelectEqPreset = viewModel::selectEqPreset,
                                        onChangeEqGains = viewModel::updateEqGains,
                                        onImportFiles = launchFilePicker,
                                        onImportFolder = launchFolderPicker,
                                        onLoadDemoTrack = viewModel::loadDemoTrack,
                                        onResetLibrary = viewModel::resetLibrary,
                                    )
                                }
                            }
                        }

                // Bottom Persistent Accent Blue Transport Deck (hidden while main Now Playing player is open)
                val showBottomPlayerBar = state.route !is ScreenRoute.NowPlaying
                if (showBottomPlayerBar) {
                    PlayerBar(
                        currentTrack = state.currentTrack,
                        isPlaying = state.isPlaying,
                        elapsedSeconds = state.elapsedSeconds,
                        durationSeconds = state.durationSeconds,
                        volume = state.volume,
                        isMuted = state.isMuted,
                        isShuffled = state.isShuffled,
                        repeatMode = state.repeatMode,
                        eqPreset = state.eqPreset,
                        isWideLayout = isWideScreen,
                        onTogglePlay = viewModel::togglePlayPause,
                        onNext = viewModel::nextTrack,
                        onPrevious = viewModel::previousTrack,
                        onSeek = viewModel::seekTo,
                        onVolumeChange = viewModel::setVolume,
                        onToggleMute = viewModel::toggleMute,
                        onToggleShuffle = viewModel::toggleShuffle,
                        onCycleRepeat = viewModel::cycleRepeatMode,
                        onOpenNowPlaying = { viewModel.navigateTo(ScreenRoute.NowPlaying) },
                        onOpenArtist = { id -> viewModel.navigateTo(ScreenRoute.ArtistDetail(id)) },
                        onOpenSettingsEq = { viewModel.navigateTo(ScreenRoute.Settings) },
                    )
                }
            }

                // Crisp Windows 10 Toast matching App.tsx (#1C1B1B rounded-none)
                val toastBottomPadding = when {
                    state.route is ScreenRoute.NowPlaying -> 16.dp
                    isWideScreen -> 96.dp
                    else -> 88.dp
                }
                AnimatedVisibility(
                    visible = state.toastMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(end = 16.dp, bottom = toastBottomPadding),
                ) {
                    state.toastMessage?.let { msg ->
                        Box(
                            modifier = Modifier
                                .background(FgToastBg)
                                .border(1.dp, Color.White.copy(alpha = 0.16f))
                                .padding(horizontal = 14.dp, vertical = 9.dp),
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                            )
                        }
                }
            }
        }
    }
}
