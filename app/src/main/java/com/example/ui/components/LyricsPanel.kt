package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.LyricLine
import com.example.model.LyricsUiState
import com.example.model.TrackLyrics
import com.example.ui.theme.FgAccentOnDark
import com.example.ui.theme.FgBorder
import com.example.ui.theme.FgPlayingTint
import com.example.ui.theme.FgPrimary
import com.example.ui.theme.FgSurface
import com.example.ui.theme.FgText
import com.example.ui.theme.FgTextMuted
import kotlinx.coroutines.launch

@Composable
fun LyricsPanel(
    lyricsState: LyricsUiState,
    elapsedMs: Long,
    onSeekToMs: (Long) -> Unit,
    onRefreshLyrics: () -> Unit,
    onImportLyricsFile: () -> Unit,
    modifier: Modifier = Modifier,
    darkBackdrop: Boolean = false,
    isFullscreen: Boolean = false,
    onToggleFullscreen: (() -> Unit)? = null,
    bottomPadding: Dp = 16.dp,
) {
    val primaryAccent = if (darkBackdrop) FgAccentOnDark else FgPrimary
    val textPrimary = if (darkBackdrop) Color.White else FgText
    val textMuted = if (darkBackdrop) Color.White.copy(alpha = 0.65f) else FgTextMuted

    Column(
        modifier = modifier
            .then(if (!darkBackdrop) Modifier.background(FgSurface) else Modifier),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            when (lyricsState) {
                is LyricsUiState.Idle -> {
                    LyricsEmptyState(
                        title = stringResource(R.string.lyrics_idle_title),
                        subtitle = stringResource(R.string.lyrics_idle_subtitle),
                        showActions = false,
                        darkBackdrop = darkBackdrop,
                        onRetry = onRefreshLyrics,
                        onImportLrc = onImportLyricsFile,
                    )
                }

                is LyricsUiState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(
                            color = primaryAccent,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(30.dp),
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = stringResource(R.string.lyrics_searching_synced),
                            style = MaterialTheme.typography.bodyMedium,
                            color = textMuted,
                        )
                    }
                }

                is LyricsUiState.NotFound -> {
                    LyricsEmptyState(
                        title = stringResource(R.string.lyrics_not_found_title),
                        subtitle = stringResource(R.string.lyrics_not_found_subtitle),
                        showActions = true,
                        darkBackdrop = darkBackdrop,
                        onRetry = onRefreshLyrics,
                        onImportLrc = onImportLyricsFile,
                    )
                }

                is LyricsUiState.Error -> {
                    LyricsEmptyState(
                        title = stringResource(R.string.lyrics_error_title),
                        subtitle = stringResource(R.string.lyrics_error_subtitle),
                        showActions = true,
                        darkBackdrop = darkBackdrop,
                        onRetry = onRefreshLyrics,
                        onImportLrc = onImportLyricsFile,
                    )
                }

                is LyricsUiState.Loaded -> {
                    val lyrics = lyricsState.lyrics
                    when {
                        lyrics.hasSyncedLyrics -> {
                            SyncedLyricsView(
                                lyrics = lyrics,
                                elapsedMs = elapsedMs,
                                darkBackdrop = darkBackdrop,
                                isFullscreen = isFullscreen,
                                bottomPadding = bottomPadding,
                                onSeekToMs = onSeekToMs,
                            )
                        }

                        lyrics.isInstrumental -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MusicNote,
                                    contentDescription = null,
                                    tint = primaryAccent,
                                    modifier = Modifier.size(44.dp),
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(R.string.lyrics_instrumental_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = textPrimary,
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = stringResource(R.string.lyrics_instrumental_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textMuted,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }

                        !lyrics.plainLyrics.isNullOrBlank() -> {
                            PlainLyricsView(
                                plainText = lyrics.plainLyrics,
                                darkBackdrop = darkBackdrop,
                                bottomPadding = bottomPadding,
                            )
                        }

                        else -> {
                            LyricsEmptyState(
                                title = stringResource(R.string.lyrics_not_found_title),
                                subtitle = stringResource(R.string.lyrics_fallback_empty_subtitle),
                                showActions = true,
                                darkBackdrop = darkBackdrop,
                                onRetry = onRefreshLyrics,
                                onImportLrc = onImportLyricsFile,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncedLyricsView(
    lyrics: TrackLyrics,
    elapsedMs: Long,
    darkBackdrop: Boolean,
    isFullscreen: Boolean,
    bottomPadding: Dp,
    onSeekToMs: (Long) -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val activeIndex = lyrics.activeLineIndexAt(elapsedMs)

    var autoScrollEnabled by remember(lyrics.trackId) { mutableStateOf(true) }
    var isProgrammaticScroll by remember { mutableStateOf(false) }

    // Detect when user manually scrolls away so we don't fight their gesture
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress && !isProgrammaticScroll) {
            autoScrollEnabled = false
        }
    }

    // Smoothly keep the active line in the comfortable upper-center reading zone
    LaunchedEffect(activeIndex, autoScrollEnabled, lyrics.trackId) {
        if (!autoScrollEnabled || activeIndex < 0) return@LaunchedEffect
        val targetIndex = (activeIndex - 2).coerceAtLeast(0)
        isProgrammaticScroll = true
        try {
            listState.animateScrollToItem(index = targetIndex)
        } finally {
            isProgrammaticScroll = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("synced_lyrics_list"),
            contentPadding = PaddingValues(
                top = if (isFullscreen) 40.dp else 18.dp,
                bottom = 72.dp + bottomPadding,
            ),
        ) {
            itemsIndexed(
                items = lyrics.syncedLines,
                key = { idx, line -> "${lyrics.trackId}-${line.timeMs}-$idx" },
            ) { index, line ->
                val isActive = index == activeIndex
                val isPast = activeIndex >= 0 && index < activeIndex
                SyncedLyricRow(
                    line = line,
                    isActive = isActive,
                    isPast = isPast,
                    darkBackdrop = darkBackdrop,
                    isFullscreen = isFullscreen,
                    onClick = {
                        autoScrollEnabled = true
                        onSeekToMs(line.timeMs)
                    },
                )
            }
        }

        // Floating Re-sync pill when user scrolled manually
        AnimatedVisibility(
            visible = !autoScrollEnabled && activeIndex >= 0,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 14.dp + bottomPadding),
        ) {
            Row(
                modifier = Modifier
                    .background(FgPrimary, RectangleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.22f), RectangleShape)
                    .clickable {
                        autoScrollEnabled = true
                        scope.launch {
                            val target = (activeIndex - 2).coerceAtLeast(0)
                            isProgrammaticScroll = true
                            try {
                                listState.animateScrollToItem(target)
                            } finally {
                                isProgrammaticScroll = false
                            }
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("lyrics_resync_button"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.MyLocation,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.lyrics_sync_button),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun SyncedLyricRow(
    line: LyricLine,
    isActive: Boolean,
    isPast: Boolean,
    darkBackdrop: Boolean,
    isFullscreen: Boolean,
    onClick: () -> Unit,
) {
    val activeAccent = if (darkBackdrop) FgAccentOnDark else FgPrimary
    val targetTextColor = when {
        isActive && darkBackdrop -> Color.White
        isActive -> FgPrimary
        isPast && darkBackdrop -> Color.White.copy(alpha = 0.42f)
        isPast -> FgTextMuted.copy(alpha = 0.62f)
        darkBackdrop -> Color.White.copy(alpha = 0.68f)
        else -> FgText.copy(alpha = 0.78f)
    }
    val animatedTextColor by animateColorAsState(
        targetValue = targetTextColor,
        animationSpec = tween(durationMillis = 220),
        label = "lyric_line_color",
    )

    val rowBg = when {
        isActive && darkBackdrop -> Color.White.copy(alpha = 0.09f)
        isActive -> FgPlayingTint
        else -> Color.Transparent
    }

    val fontSize = when {
        isFullscreen && isActive -> 23.sp
        isFullscreen -> 18.sp
        isActive -> 19.sp
        else -> 15.5.sp
    }
    val lineHeight = when {
        isFullscreen && isActive -> 31.sp
        isFullscreen -> 26.sp
        isActive -> 26.sp
        else -> 22.sp
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg)
            .clickable(onClick = onClick)
            .padding(
                start = if (isActive) 14.dp else 18.dp,
                end = 20.dp,
                top = if (isFullscreen) 12.dp else 9.dp,
                bottom = if (isFullscreen) 12.dp else 9.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isActive) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(if (isFullscreen) 26.dp else 22.dp)
                    .background(activeAccent),
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(
            text = line.text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = fontSize,
                lineHeight = lineHeight,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            ),
            color = animatedTextColor,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PlainLyricsView(
    plainText: String,
    darkBackdrop: Boolean,
    bottomPadding: Dp,
) {
    val scrollState = rememberScrollState()
    val textColor = if (darkBackdrop) Color.White.copy(alpha = 0.92f) else FgText
    val mutedColor = if (darkBackdrop) Color.White.copy(alpha = 0.62f) else FgTextMuted
    val bannerBg = if (darkBackdrop) Color.White.copy(alpha = 0.08f) else FgPlayingTint

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 32.dp + bottomPadding),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(bannerBg)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.lyrics_unsynced_notice),
                style = MaterialTheme.typography.bodySmall,
                color = mutedColor,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = plainText,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                lineHeight = 27.sp,
            ),
            color = textColor,
        )
    }
}

@Composable
private fun LyricsEmptyState(
    title: String,
    subtitle: String,
    showActions: Boolean,
    darkBackdrop: Boolean,
    onRetry: () -> Unit,
    onImportLrc: () -> Unit,
) {
    val titleColor = if (darkBackdrop) Color.White else FgText
    val subtitleColor = if (darkBackdrop) Color.White.copy(alpha = 0.7f) else FgTextMuted

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Lyrics,
            contentDescription = null,
            tint = subtitleColor.copy(alpha = 0.6f),
            modifier = Modifier.size(40.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = titleColor,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = subtitleColor,
            textAlign = TextAlign.Center,
        )
        if (showActions) {
            Spacer(modifier = Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onRetry,
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = FgPrimary),
                    modifier = Modifier.testTag("lyrics_retry_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_retry), color = Color.White, style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = onImportLrc,
                    shape = RectangleShape,
                    modifier = Modifier.testTag("lyrics_import_empty_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.UploadFile,
                        contentDescription = null,
                        tint = if (darkBackdrop) Color.White else FgPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.lyrics_import_lrc),
                        color = if (darkBackdrop) Color.White else FgPrimary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}
