package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import coil.transform.Transformation
import com.example.R
import com.example.model.LyricsUiState
import com.example.model.NowPlayingArtworkStyle
import com.example.model.RepeatMode
import com.example.model.Track
import com.example.model.formatDuration
import com.example.ui.components.AlbumArtwork
import com.example.ui.components.GrooveSeekBar
import com.example.ui.components.LyricsPanel
import com.example.ui.components.PlayingEqualizerIndicator
import com.example.ui.components.PlaylistNameDialog
import com.example.ui.theme.FgAccentOnDark
import com.example.ui.theme.FgBorder
import com.example.ui.theme.FgDarkBackdrop
import com.example.ui.theme.FgPlayingTint
import com.example.ui.theme.FgPrimary
import com.example.ui.theme.FgSurface
import com.example.ui.theme.FgText
import com.example.ui.theme.FgTextMuted
import com.example.ui.theme.TabularNumStyle

@Composable
fun NowPlayingScreen(
    currentTrack: Track?,
    artistImageUrl: String?,
    artworkStyle: NowPlayingArtworkStyle = NowPlayingArtworkStyle.ARTIST_FULLSCREEN,
    isPlaying: Boolean,
    elapsedSeconds: Int,
    elapsedMs: Long,
    durationSeconds: Int,
    isShuffled: Boolean,
    repeatMode: RepeatMode,
    queue: List<Track>,
    currentQueueIndex: Int,
    lyricsState: LyricsUiState,
    onBack: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Int) -> Unit,
    onSeekMs: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onJumpToQueueIndex: (Int) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit,
    onRemoveQueueItem: (Int) -> Unit,
    onClearQueue: () -> Unit,
    onSaveQueueAsPlaylist: (String) -> Unit,
    onRequestAddTrackToPlaylist: (Track) -> Unit,
    onRefreshLyrics: () -> Unit,
    onImportLyricsFile: () -> Unit,
    onEnterMiniPlayer: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
) {
    var showSaveQueueDialog by remember { mutableStateOf(false) }
    var isFullscreenLyrics by remember { mutableStateOf(false) }
    var isQueueExpanded by remember { mutableStateOf(false) }
    var overflowMenuExpanded by remember { mutableStateOf(false) }

    BackHandler(enabled = isFullscreenLyrics || isQueueExpanded) {
        when {
            isFullscreenLyrics -> isFullscreenLyrics = false
            isQueueExpanded -> isQueueExpanded = false
        }
    }

    if (showSaveQueueDialog) {
        PlaylistNameDialog(
            title = stringResource(R.string.save_queue_title),
            confirmLabel = stringResource(R.string.action_save),
            initialValue = stringResource(R.string.save_queue_default_name),
            onDismiss = { showSaveQueueDialog = false },
            onConfirm = { name ->
                showSaveQueueDialog = false
                onSaveQueueAsPlaylist(name)
            },
        )
    }

    val navBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    if (currentTrack == null && queue.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(FgSurface)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("np_back_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = FgText,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.nav_now_playing),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal),
                    color = FgText,
                )
            }
            HorizontalDivider(color = FgBorder)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.GraphicEq,
                        contentDescription = null,
                        tint = FgTextMuted.copy(alpha = 0.55f),
                        modifier = Modifier.size(52.dp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(stringResource(R.string.nothing_playing), style = MaterialTheme.typography.titleLarge, color = FgText)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.nothing_playing_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = FgTextMuted,
                    )
                }
            }
        }
        return
    }

    val hasNextTrack = queue.size > 1 || repeatMode != RepeatMode.OFF

    // Full-screen immersive synced lyrics view with pinned compact playback transport
    if (isFullscreenLyrics && currentTrack != null) {
        FullscreenLyricsStage(
            currentTrack = currentTrack,
            isPlaying = isPlaying,
            elapsedSeconds = elapsedSeconds,
            elapsedMs = elapsedMs,
            durationSeconds = durationSeconds,
            isShuffled = isShuffled,
            repeatMode = repeatMode,
            hasNextTrack = hasNextTrack,
            lyricsState = lyricsState,
            onTogglePlay = onTogglePlay,
            onNext = onNext,
            onPrevious = onPrevious,
            onSeek = onSeek,
            onSeekMs = onSeekMs,
            onToggleShuffle = onToggleShuffle,
            onCycleRepeat = onCycleRepeat,
            onRefreshLyrics = onRefreshLyrics,
            onImportLyricsFile = onImportLyricsFile,
            onOpenArtist = onOpenArtist,
            onExitFullscreen = { isFullscreenLyrics = false },
        )
        return
    }

    val totalDur = durationSeconds.coerceAtLeast(currentTrack?.duration ?: 0).coerceAtLeast(1)
    val safeElapsed = elapsedSeconds.coerceIn(0, totalDur)
    var isDraggingSeek by remember { mutableStateOf(false) }
    var dragSeekSeconds by remember { mutableFloatStateOf(0f) }
    val displayedElapsed = if (isDraggingSeek) dragSeekSeconds.toInt() else safeElapsed

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FgDarkBackdrop),
    ) {
        if (artworkStyle == NowPlayingArtworkStyle.ARTIST_FULLSCREEN) {
            // 1. Full-bleed edge-to-edge backdrop (artist portrait with album artwork fallback)
            NowPlayingBackdrop(
                currentTrack = currentTrack,
                artistImageUrl = artistImageUrl,
                modifier = Modifier.fillMaxSize(),
            )

            // 2. Top atmospheric scrim for crisp overlay header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(136.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Black.copy(alpha = 0.56f),
                                0.55f to Color.Black.copy(alpha = 0.22f),
                                1.0f to Color.Transparent,
                            )
                        )
                    ),
            )

            // 3. Bottom atmospheric scrim for crisp metadata, seekbar, transport & queue arrow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.56f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Transparent,
                                0.30f to Color.Black.copy(alpha = 0.26f),
                                0.58f to Color.Black.copy(alpha = 0.64f),
                                0.84f to Color.Black.copy(alpha = 0.84f),
                                1.0f to Color.Black.copy(alpha = 0.92f),
                            )
                        )
                    ),
            )
        } else {
            // 1. Full-bleed blurred album artwork backdrop
            BlurredAlbumBackdrop(
                currentTrack = currentTrack,
                modifier = Modifier.fillMaxSize(),
            )

            // 2. Subtle atmospheric tint so the centered crisp cover and white overlays pop cleanly
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.28f)),
            )

            // 3. Top scrim for crisp overlay header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(136.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Black.copy(alpha = 0.48f),
                                0.55f to Color.Black.copy(alpha = 0.18f),
                                1.0f to Color.Transparent,
                            )
                        )
                    ),
            )

            // 4. Bottom scrim for crisp metadata, seekbar, transport & queue arrow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.56f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Transparent,
                                0.30f to Color.Black.copy(alpha = 0.22f),
                                0.58f to Color.Black.copy(alpha = 0.58f),
                                0.84f to Color.Black.copy(alpha = 0.82f),
                                1.0f to Color.Black.copy(alpha = 0.90f),
                            )
                        )
                    ),
            )
        }

        // Foreground layout: Top Bar + Center Artwork Stage + Bottom Controls Deck
        Column(modifier = Modifier.fillMaxSize()) {
            // 4. Transparent Overlay Top Bar: Back Arrow + "Now playing" + Lyrics Icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("np_back_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = Color.White,
                        modifier = Modifier.size(25.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.nav_now_playing),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp),
                )
                IconButton(
                    onClick = onEnterMiniPlayer,
                    modifier = Modifier.testTag("np_mini_player_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.PictureInPictureAlt,
                        contentDescription = stringResource(R.string.cd_mini_player),
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
                if (currentTrack != null) {
                    IconButton(
                        onClick = { isFullscreenLyrics = true },
                        modifier = Modifier.testTag("np_lyrics_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lyrics,
                            contentDescription = stringResource(R.string.cd_lyrics),
                            tint = Color.White,
                            modifier = Modifier.size(23.dp),
                        )
                    }
                }
            }

            // Center artwork stage (transparent in fullscreen artist mode; crisp centered album cover in normal mode)
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                if (artworkStyle == NowPlayingArtworkStyle.ALBUM_ART_NORMAL && currentTrack != null) {
                    val artSize = minOf(maxWidth - 64.dp, maxHeight - 32.dp, 320.dp).coerceAtLeast(156.dp)
                    AlbumArtwork(
                        coverUri = currentTrack.coverUri,
                        artDesign = currentTrack.artDesign,
                        modifier = Modifier
                            .size(artSize)
                            .shadow(20.dp, RectangleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.16f))
                            .testTag("np_album_artwork_card"),
                    )
                }
            }

            // 5. Bottom Controls Deck matching Groove Music Android screenshot
            Column(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp),
            ) {
            // Track Title (left-aligned, large crisp white)
            Text(
                text = currentTrack?.title ?: stringResource(R.string.not_playing),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 31.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.Normal,
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Artist • Album subtitle (left-aligned, tappable)
            if (currentTrack != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = currentTrack.artist,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Normal,
                        ),
                        color = Color.White.copy(alpha = 0.92f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { onOpenArtist(currentTrack.artistId) },
                    )
                    if (currentTrack.album.isNotBlank()) {
                        Text(
                            text = " • ",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.5.sp),
                            color = Color.White.copy(alpha = 0.85f),
                        )
                        Text(
                            text = currentTrack.album,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Normal,
                            ),
                            color = Color.White.copy(alpha = 0.92f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .clickable { onOpenAlbum(currentTrack.albumId) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Inline Seekbar Row: 0:06  ────●────────────  4:40
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatDuration(displayedElapsed),
                    style = TabularNumStyle.copy(fontSize = 12.sp),
                    color = Color.White.copy(alpha = 0.92f),
                )
                GrooveSeekBar(
                    progress = (displayedElapsed.toFloat() / totalDur.toFloat()).coerceIn(0f, 1f),
                    enabled = currentTrack != null,
                    isDragging = isDraggingSeek,
                    onScrubStartOrMove = { fraction ->
                        isDraggingSeek = true
                        dragSeekSeconds = fraction * totalDur.toFloat()
                    },
                    onScrubEnd = { fraction ->
                        isDraggingSeek = false
                        onSeek((fraction * totalDur.toFloat()).toInt())
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                )
                Text(
                    text = formatDuration(currentTrack?.duration ?: durationSeconds),
                    style = TabularNumStyle.copy(fontSize = 12.sp),
                    color = Color.White.copy(alpha = 0.92f),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 6-Action Minimalist Transport Row: |<  ||  >|  Shuffle  Repeat  •••
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("np_prev_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = stringResource(R.string.cd_previous),
                        tint = Color.White,
                        modifier = Modifier.size(30.dp),
                    )
                }

                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("np_play_pause_button"),
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = stringResource(if (isPlaying) R.string.cd_pause else R.string.cd_play),
                        tint = Color.White,
                        modifier = Modifier.size(36.dp),
                    )
                }

                IconButton(
                    onClick = onNext,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("np_next_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = stringResource(R.string.cd_next),
                        tint = if (hasNextTrack) Color.White else Color.White.copy(alpha = 0.42f),
                        modifier = Modifier.size(30.dp),
                    )
                }

                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("np_shuffle_button"),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Shuffle,
                            contentDescription = stringResource(R.string.cd_shuffle),
                            tint = if (isShuffled) Color.White else Color.White.copy(alpha = 0.56f),
                            modifier = Modifier.size(24.dp),
                        )
                        if (isShuffled) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(FgAccentOnDark),
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onCycleRepeat,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("np_repeat_button"),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                            contentDescription = stringResource(R.string.cd_repeat),
                            tint = if (repeatMode != RepeatMode.OFF) Color.White else Color.White.copy(alpha = 0.56f),
                            modifier = Modifier.size(24.dp),
                        )
                        if (repeatMode != RepeatMode.OFF) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(FgAccentOnDark),
                            )
                        }
                    }
                }

                Box {
                    IconButton(
                        onClick = { if (currentTrack != null) overflowMenuExpanded = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("np_more_options_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreHoriz,
                            contentDescription = stringResource(R.string.cd_more_options),
                            tint = Color.White,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = overflowMenuExpanded,
                        onDismissRequest = { overflowMenuExpanded = false },
                        containerColor = FgSurface,
                    ) {
                        if (currentTrack != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.lyrics_label)) },
                                onClick = {
                                    overflowMenuExpanded = false
                                    isFullscreenLyrics = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_add_to_playlist)) },
                                onClick = {
                                    overflowMenuExpanded = false
                                    onRequestAddTrackToPlaylist(currentTrack)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_go_to_album)) },
                                onClick = {
                                    overflowMenuExpanded = false
                                    onOpenAlbum(currentTrack.albumId)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_go_to_artist)) },
                                onClick = {
                                    overflowMenuExpanded = false
                                    onOpenArtist(currentTrack.artistId)
                                },
                            )
                        }
                    }
                }
            }

            // Bottom centered Up-Arrow (^) to reveal the Play Queue panel below
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount < -18f) {
                                isQueueExpanded = true
                            }
                        }
                    }
                    .clickable { isQueueExpanded = true }
                    .testTag("np_open_queue_button"),
                contentAlignment = Alignment.Center,
            ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = stringResource(R.string.cd_open_queue),
                        tint = Color.White.copy(alpha = 0.92f),
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        }

        // 6. Scrim behind expanded bottom Play Queue drawer
        AnimatedVisibility(
            visible = isQueueExpanded,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.48f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { isQueueExpanded = false },
                    ),
            )
        }

        // 7. Slide-Up Play Queue Panel from the bottom
        AnimatedVisibility(
            visible = isQueueExpanded,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .widthIn(max = 680.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.68f),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(FgSurface)
                    .border(1.dp, FgBorder)
                    .testTag("np_queue_sheet"),
            ) {
                QueueHeaderBar(
                    queueSize = queue.size,
                    onCollapseQueue = { isQueueExpanded = false },
                    onRequestSaveQueue = { showSaveQueueDialog = true },
                    onClearQueue = {
                        isQueueExpanded = false
                        onClearQueue()
                    },
                )
                HorizontalDivider(color = FgBorder)
                QueueListContent(
                    queue = queue,
                    currentQueueIndex = currentQueueIndex,
                    isPlaying = isPlaying,
                    onJumpToQueueIndex = onJumpToQueueIndex,
                    onMoveQueueItem = onMoveQueueItem,
                    onRemoveQueueItem = onRemoveQueueItem,
                    bottomPadding = navBarBottomPadding,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun NowPlayingBackdrop(
    currentTrack: Track?,
    artistImageUrl: String?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.background(FgDarkBackdrop)) {
        AlbumArtwork(
            coverUri = currentTrack?.coverUri,
            artDesign = currentTrack?.artDesign,
            modifier = Modifier.fillMaxSize(),
        )
        if (!artistImageUrl.isNullOrBlank()) {
            AsyncImage(
                model = artistImageUrl,
                contentDescription = stringResource(R.string.cd_artist_photo),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun BlurredAlbumBackdrop(
    currentTrack: Track?,
    modifier: Modifier = Modifier,
) {
    val coverUri = currentTrack?.coverUri?.takeIf { it.isNotBlank() }
    Box(
        modifier = modifier
            .clip(RectangleShape)
            .background(FgDarkBackdrop),
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlbumArtwork(
                coverUri = coverUri,
                artDesign = currentTrack?.artDesign,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(1.14f)
                    .blur(
                        radius = 30.dp,
                        edgeTreatment = BlurredEdgeTreatment.Rectangle,
                    ),
            )
        } else {
            AlbumArtwork(
                coverUri = null,
                artDesign = currentTrack?.artDesign,
                modifier = Modifier.fillMaxSize(),
            )
            if (coverUri != null) {
                val context = LocalContext.current
                val request = remember(coverUri) {
                    ImageRequest.Builder(context)
                        .data(coverUri)
                        .size(128, 128)
                        .allowHardware(false)
                        .transformations(FastBoxBlurTransformation(radius = 8, passes = 2))
                        .build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(1.14f),
                )
            }
        }
    }
}

private class FastBoxBlurTransformation(
    private val radius: Int = 8,
    private val passes: Int = 2,
) : Transformation {
    override val cacheKey: String = "FastBoxBlurTransformation(radius=$radius,passes=$passes)"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val w = input.width
        val h = input.height
        if (w <= 2 || h <= 2 || radius <= 0) return input

        val src = if (input.config == Bitmap.Config.ARGB_8888) {
            input.copy(Bitmap.Config.ARGB_8888, true)
        } else {
            input.copy(Bitmap.Config.ARGB_8888, true)
        }

        val pixels = IntArray(w * h)
        val temp = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        val r = radius.coerceAtMost((minOf(w, h) - 1).coerceAtLeast(1))
        val window = r * 2 + 1

        repeat(passes) {
            // Horizontal pass
            for (y in 0 until h) {
                val rowOffset = y * w
                var sumR = 0
                var sumG = 0
                var sumB = 0
                for (i in -r..r) {
                    val p = pixels[rowOffset + i.coerceIn(0, w - 1)]
                    sumR += (p shr 16) and 0xFF
                    sumG += (p shr 8) and 0xFF
                    sumB += p and 0xFF
                }
                for (x in 0 until w) {
                    temp[rowOffset + x] = (0xFF shl 24) or
                        ((sumR / window) shl 16) or
                        ((sumG / window) shl 8) or
                        (sumB / window)
                    val outP = pixels[rowOffset + (x - r).coerceIn(0, w - 1)]
                    val inP = pixels[rowOffset + (x + r + 1).coerceIn(0, w - 1)]
                    sumR += ((inP shr 16) and 0xFF) - ((outP shr 16) and 0xFF)
                    sumG += ((inP shr 8) and 0xFF) - ((outP shr 8) and 0xFF)
                    sumB += (inP and 0xFF) - (outP and 0xFF)
                }
            }
            // Vertical pass
            for (x in 0 until w) {
                var sumR = 0
                var sumG = 0
                var sumB = 0
                for (i in -r..r) {
                    val p = temp[i.coerceIn(0, h - 1) * w + x]
                    sumR += (p shr 16) and 0xFF
                    sumG += (p shr 8) and 0xFF
                    sumB += p and 0xFF
                }
                for (y in 0 until h) {
                    pixels[y * w + x] = (0xFF shl 24) or
                        ((sumR / window) shl 16) or
                        ((sumG / window) shl 8) or
                        (sumB / window)
                    val outP = temp[(y - r).coerceIn(0, h - 1) * w + x]
                    val inP = temp[(y + r + 1).coerceIn(0, h - 1) * w + x]
                    sumR += ((inP shr 16) and 0xFF) - ((outP shr 16) and 0xFF)
                    sumG += ((inP shr 8) and 0xFF) - ((outP shr 8) and 0xFF)
                    sumB += (inP and 0xFF) - (outP and 0xFF)
                }
            }
        }

        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        output.setPixels(pixels, 0, w, 0, 0, w, h)
        if (src !== input) src.recycle()
        return output
    }
}

@Composable
private fun FullscreenLyricsStage(
    currentTrack: Track,
    isPlaying: Boolean,
    elapsedSeconds: Int,
    elapsedMs: Long,
    durationSeconds: Int,
    isShuffled: Boolean,
    repeatMode: RepeatMode,
    hasNextTrack: Boolean,
    lyricsState: LyricsUiState,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Int) -> Unit,
    onSeekMs: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onRefreshLyrics: () -> Unit,
    onImportLyricsFile: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onExitFullscreen: () -> Unit,
) {
    val view = LocalView.current
    DisposableEffect(view) {
        val previousKeepScreenOn = view.keepScreenOn
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = previousKeepScreenOn
        }
    }

    val totalDur = durationSeconds.coerceAtLeast(currentTrack.duration).coerceAtLeast(1)
    val safeElapsed = elapsedSeconds.coerceIn(0, totalDur)
    var isDragging by remember { mutableStateOf(false) }
    var dragPos by remember { mutableFloatStateOf(0f) }
    val displayedElapsed = if (isDragging) dragPos.toInt() else safeElapsed

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FgDarkBackdrop),
    ) {
        // 1. Full-bleed blurred album artwork backdrop matching the main player
        BlurredAlbumBackdrop(
            currentTrack = currentTrack,
            modifier = Modifier.fillMaxSize(),
        )

        // 2. Balanced atmospheric tint for crisp lyrics readability on any album cover
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.38f)),
        )

        // 3. Top scrim for header legibility
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(136.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Black.copy(alpha = 0.48f),
                            0.55f to Color.Black.copy(alpha = 0.18f),
                            1.0f to Color.Transparent,
                        )
                    )
                ),
        )

        // 4. Bottom scrim for pinned seekbar & transport controls
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.32f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.45f to Color.Black.copy(alpha = 0.44f),
                            1.0f to Color.Black.copy(alpha = 0.84f),
                        )
                    )
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
        // Top track bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlbumArtwork(
                coverUri = currentTrack.coverUri,
                artDesign = currentTrack.artDesign,
                modifier = Modifier
                    .size(44.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.25f)),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentTrack.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = currentTrack.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = FgAccentOnDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { onOpenArtist(currentTrack.artistId) },
                )
            }
            IconButton(
                onClick = onRefreshLyrics,
                modifier = Modifier.testTag("lyrics_refresh_button"),
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.cd_refresh_lyrics),
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
            IconButton(
                onClick = onExitFullscreen,
                modifier = Modifier.testTag("lyrics_exit_fullscreen_button"),
            ) {
                Icon(
                    imageVector = Icons.Filled.CloseFullscreen,
                    contentDescription = stringResource(R.string.cd_collapse_lyrics),
                    tint = Color.White,
                )
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.14f))

        // Immersive synced lyrics body
        LyricsPanel(
            lyricsState = lyricsState,
            elapsedMs = elapsedMs,
            onSeekToMs = onSeekMs,
            onRefreshLyrics = onRefreshLyrics,
            onImportLyricsFile = onImportLyricsFile,
            darkBackdrop = true,
            isFullscreen = true,
            onToggleFullscreen = null,
            bottomPadding = 8.dp,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )

        // Pinned minimalist playback transport bar matching the main Now Playing player
        HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.28f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                // Inline Seekbar Row: 0:06  ────●────────────  4:40
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = formatDuration(displayedElapsed),
                        style = TabularNumStyle.copy(fontSize = 12.sp),
                        color = Color.White.copy(alpha = 0.92f),
                    )
                    GrooveSeekBar(
                        progress = (displayedElapsed.toFloat() / totalDur.toFloat()).coerceIn(0f, 1f),
                        enabled = true,
                        isDragging = isDragging,
                        onScrubStartOrMove = { fraction ->
                            isDragging = true
                            dragPos = fraction * totalDur.toFloat()
                        },
                        onScrubEnd = { fraction ->
                            isDragging = false
                            onSeek((fraction * totalDur.toFloat()).toInt())
                        },
                        seekBarTestTag = "lyrics_fs_seek_bar",
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                    )
                    Text(
                        text = formatDuration(currentTrack.duration.coerceAtLeast(durationSeconds)),
                        style = TabularNumStyle.copy(fontSize = 12.sp),
                        color = Color.White.copy(alpha = 0.92f),
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Minimalist White Transport Row: |<  ||  >|  Shuffle  Repeat
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onPrevious,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("lyrics_fs_prev_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipPrevious,
                            contentDescription = stringResource(R.string.cd_previous),
                            tint = Color.White,
                            modifier = Modifier.size(30.dp),
                        )
                    }

                    IconButton(
                        onClick = onTogglePlay,
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("lyrics_fs_play_pause"),
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = stringResource(if (isPlaying) R.string.cd_pause else R.string.cd_play),
                            tint = Color.White,
                            modifier = Modifier.size(36.dp),
                        )
                    }

                    IconButton(
                        onClick = onNext,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("lyrics_fs_next_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = stringResource(R.string.cd_next),
                            tint = if (hasNextTrack) Color.White else Color.White.copy(alpha = 0.42f),
                            modifier = Modifier.size(30.dp),
                        )
                    }

                    IconButton(
                        onClick = onToggleShuffle,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("lyrics_fs_shuffle_button"),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.Shuffle,
                                contentDescription = stringResource(R.string.cd_shuffle),
                                tint = if (isShuffled) Color.White else Color.White.copy(alpha = 0.56f),
                                modifier = Modifier.size(24.dp),
                            )
                            if (isShuffled) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(FgAccentOnDark),
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onCycleRepeat,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("lyrics_fs_repeat_button"),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                                contentDescription = stringResource(R.string.cd_repeat),
                                tint = if (repeatMode != RepeatMode.OFF) Color.White else Color.White.copy(alpha = 0.56f),
                                modifier = Modifier.size(24.dp),
                            )
                            if (repeatMode != RepeatMode.OFF) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(FgAccentOnDark),
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun QueueHeaderBar(
    queueSize: Int,
    onCollapseQueue: () -> Unit,
    onRequestSaveQueue: () -> Unit,
    onClearQueue: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FgSurface)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 18f) {
                        onCollapseQueue()
                    }
                }
            }
            .padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            IconButton(
                onClick = onCollapseQueue,
                modifier = Modifier.testTag("np_close_queue_button"),
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.cd_close_queue),
                    tint = FgText,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                text = stringResource(R.string.play_queue_count, queueSize),
                style = MaterialTheme.typography.titleMedium,
                color = FgText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            TextButton(
                onClick = onRequestSaveQueue,
                enabled = queueSize > 0,
                modifier = Modifier.testTag("save_queue_playlist_button"),
            ) {
                Icon(Icons.Filled.Save, contentDescription = null, tint = FgPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.action_save), color = FgPrimary, style = MaterialTheme.typography.labelMedium)
            }
            TextButton(
                onClick = onClearQueue,
                enabled = queueSize > 0,
                modifier = Modifier.testTag("clear_queue_button"),
            ) {
                Icon(Icons.Filled.DeleteSweep, contentDescription = null, tint = FgTextMuted, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.action_clear), color = FgTextMuted, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun QueueListContent(
    queue: List<Track>,
    currentQueueIndex: Int,
    isPlaying: Boolean,
    onJumpToQueueIndex: (Int) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit,
    onRemoveQueueItem: (Int) -> Unit,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 16.dp + bottomPadding),
    ) {
        itemsIndexed(queue, key = { idx, t -> "${t.id}-$idx" }) { idx, track ->
            QueueTrackRow(
                track = track,
                index = idx,
                totalCount = queue.size,
                isCurrent = idx == currentQueueIndex,
                isPlaying = isPlaying,
                onJump = { onJumpToQueueIndex(idx) },
                onMoveUp = { onMoveQueueItem(idx, idx - 1) },
                onMoveDown = { onMoveQueueItem(idx, idx + 1) },
                onRemove = { onRemoveQueueItem(idx) },
            )
        }
    }
}

@Composable
private fun QueueTrackRow(
    track: Track,
    index: Int,
    totalCount: Int,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onJump: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isCurrent) FgPlayingTint else FgSurface)
                .clickable(onClick = onJump)
                .padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(38.dp),
                contentAlignment = Alignment.Center,
            ) {
                AlbumArtwork(
                    coverUri = track.coverUri,
                    artDesign = track.artDesign,
                    modifier = Modifier.size(38.dp),
                )
                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        PlayingEqualizerIndicator(isPlaying = isPlaying, color = Color.White)
                    }
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                    ),
                    color = if (isCurrent) FgPrimary else FgText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = FgTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = onMoveUp,
                enabled = index > 0,
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowUpward,
                    contentDescription = stringResource(R.string.cd_move_up),
                    tint = if (index > 0) FgTextMuted else FgTextMuted.copy(alpha = 0.25f),
                    modifier = Modifier.size(16.dp),
                )
            }
            IconButton(
                onClick = onMoveDown,
                enabled = index < totalCount - 1,
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowDownward,
                    contentDescription = stringResource(R.string.cd_move_down),
                    tint = if (index < totalCount - 1) FgTextMuted else FgTextMuted.copy(alpha = 0.25f),
                    modifier = Modifier.size(16.dp),
                )
            }
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.cd_remove_queue),
                    tint = FgTextMuted,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = FgBorder)
    }
}
