package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.RepeatMode
import com.example.model.Track
import com.example.model.formatDuration
import com.example.ui.theme.FgAccentOnDark
import com.example.ui.theme.FgDarkBackdrop
import com.example.ui.theme.TabularNumStyle

@Composable
fun MiniPlayerView(
    currentTrack: Track?,
    isPlaying: Boolean,
    elapsedSeconds: Int,
    durationSeconds: Int,
    isShuffled: Boolean,
    repeatMode: RepeatMode,
    hasNextTrack: Boolean = true,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Int) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onRestoreFull: () -> Unit,
) {
    val totalDur = durationSeconds.coerceAtLeast(currentTrack?.duration ?: 0).coerceAtLeast(1)
    val safeElapsed = elapsedSeconds.coerceIn(0, totalDur)
    var isDraggingSeek by remember { mutableStateOf(false) }
    var dragSeekSeconds by remember { mutableFloatStateOf(0f) }
    val displayedElapsed = if (isDraggingSeek) dragSeekSeconds.toInt() else safeElapsed

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FgDarkBackdrop)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 380.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
                .border(1.dp, Color.White.copy(alpha = 0.14f)),
        ) {
            AlbumArtwork(
                coverUri = currentTrack?.coverUri,
                artDesign = currentTrack?.artDesign,
                modifier = Modifier.fillMaxSize(),
            )

            // Dark translucent Fluent overlay for crisp controls
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.58f),
                                Color.Black.copy(alpha = 0.20f),
                                Color.Black.copy(alpha = 0.88f),
                            )
                        )
                    ),
            )

            // Top bar: Restore full view
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.mini_player_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.78f),
                )
                IconButton(
                    onClick = onRestoreFull,
                    modifier = Modifier.testTag("mini_player_restore_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.OpenInFull,
                        contentDescription = stringResource(R.string.cd_restore_player),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // Bottom minimalist transport deck matching the main Now Playing player
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Text(
                    text = currentTrack?.title ?: stringResource(R.string.mini_player_empty_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = currentTrack?.artist ?: stringResource(R.string.mini_player_empty_subtitle),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.5.sp),
                    color = Color.White.copy(alpha = 0.86f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(10.dp))

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
                        seekBarTestTag = "mini_player_seek_bar",
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp),
                    )
                    Text(
                        text = formatDuration(currentTrack?.duration ?: durationSeconds),
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
                            .testTag("mini_player_prev_button"),
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
                            .testTag("mini_player_play_pause"),
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
                            .testTag("mini_player_next_button"),
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
                            .testTag("mini_player_shuffle_button"),
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
                            .testTag("mini_player_repeat_button"),
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

