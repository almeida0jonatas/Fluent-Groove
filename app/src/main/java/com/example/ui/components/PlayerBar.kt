package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.EqualizerPresetName
import com.example.model.RepeatMode
import com.example.model.Track
import com.example.model.formatDuration
import com.example.ui.labelRes
import com.example.ui.theme.FgPrimary
import com.example.ui.theme.FgPrimaryDeep
import com.example.ui.theme.TabularNumStyle

/**
 * Signature Windows 10 Groove Music persistent bottom Accent Blue (#2B78C5) transport deck.
 * Compact minimalist layout on phones, full 3-zone deck on medium/expanded screens.
 */
@Composable
fun PlayerBar(
    currentTrack: Track?,
    isPlaying: Boolean,
    elapsedSeconds: Int,
    durationSeconds: Int,
    volume: Float,
    isMuted: Boolean,
    isShuffled: Boolean,
    repeatMode: RepeatMode,
    eqPreset: EqualizerPresetName,
    isWideLayout: Boolean,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Int) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenSettingsEq: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val totalDur = durationSeconds.coerceAtLeast(currentTrack?.duration ?: 0).coerceAtLeast(1)
    val safeElapsed = elapsedSeconds.coerceIn(0, totalDur)
    val progress = safeElapsed.toFloat() / totalDur.toFloat()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(FgPrimary)
            .navigationBarsPadding(),
    ) {
        if (!isWideLayout) {
            // Sleek full-width progress bar pinned to top of mobile Groove bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color.White.copy(alpha = 0.22f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (currentTrack != null) progress else 0f)
                        .height(3.dp)
                        .background(Color.White),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .clickable(onClick = onOpenNowPlaying)
                    .padding(horizontal = 10.dp)
                    .testTag("player_bar_mobile"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlbumArtwork(
                    coverUri = currentTrack?.coverUri,
                    artDesign = currentTrack?.artDesign,
                    modifier = Modifier.size(48.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentTrack?.title ?: stringResource(R.string.not_playing),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = currentTrack?.artist ?: stringResource(R.string.import_music_to_begin),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.82f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.testTag("player_prev_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = stringResource(R.string.cd_previous),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }

                // Signature circular white-outlined Groove Play/Pause button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color.White, CircleShape)
                        .background(if (isPlaying) Color.White.copy(alpha = 0.14f) else Color.Transparent)
                        .clickable(onClick = onTogglePlay)
                        .testTag("player_play_pause_button"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = stringResource(if (isPlaying) R.string.cd_pause else R.string.cd_play),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }

                IconButton(
                    onClick = onNext,
                    modifier = Modifier.testTag("player_next_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = stringResource(R.string.cd_next),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        } else {
            // Full 3-zone Desktop/Tablet Windows 10 Groove Transport Deck
            var isDragging by remember { mutableStateOf(false) }
            var dragValue by remember { mutableFloatStateOf(0f) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp)
                    .padding(horizontal = 16.dp)
                    .testTag("player_bar_wide"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Zone 1: Current Track Info
                Row(
                    modifier = Modifier
                        .weight(0.30f)
                        .clickable(onClick = onOpenNowPlaying),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AlbumArtwork(
                        coverUri = currentTrack?.coverUri,
                        artDesign = currentTrack?.artDesign,
                        modifier = Modifier.size(56.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentTrack?.title ?: stringResource(R.string.not_playing),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = currentTrack?.artist ?: stringResource(R.string.select_song_to_play),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.82f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable(enabled = currentTrack != null) {
                                currentTrack?.let { onOpenArtist(it.artistId) }
                            },
                        )
                    }
                }

                // Zone 2: Center Transport + Timeline
                Column(
                    modifier = Modifier
                        .weight(0.44f)
                        .padding(horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        IconButton(
                            onClick = onToggleShuffle,
                            modifier = Modifier.size(36.dp).testTag("player_shuffle_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shuffle,
                                contentDescription = stringResource(R.string.cd_shuffle),
                                tint = if (isShuffled) Color.White else Color.White.copy(alpha = 0.52f),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        IconButton(
                            onClick = onPrevious,
                            modifier = Modifier.size(38.dp).testTag("player_prev_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipPrevious,
                                contentDescription = stringResource(R.string.cd_previous),
                                tint = Color.White,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, Color.White, CircleShape)
                                .clickable(onClick = onTogglePlay)
                                .testTag("player_play_pause_button"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = stringResource(if (isPlaying) R.string.cd_pause else R.string.cd_play),
                                tint = Color.White,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        IconButton(
                            onClick = onNext,
                            modifier = Modifier.size(38.dp).testTag("player_next_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipNext,
                                contentDescription = stringResource(R.string.cd_next),
                                tint = Color.White,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        IconButton(
                            onClick = onCycleRepeat,
                            modifier = Modifier.size(36.dp).testTag("player_repeat_button"),
                        ) {
                            Icon(
                                imageVector = if (repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                                contentDescription = stringResource(R.string.cd_repeat),
                                tint = if (repeatMode != RepeatMode.OFF) Color.White else Color.White.copy(alpha = 0.52f),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = formatDuration(if (isDragging) dragValue.toInt() else safeElapsed),
                            style = TabularNumStyle.copy(fontSize = 11.sp),
                            color = Color.White.copy(alpha = 0.88f),
                        )
                        GrooveSeekBar(
                            progress = ((if (isDragging) dragValue else safeElapsed.toFloat()) / totalDur.toFloat()).coerceIn(0f, 1f),
                            enabled = currentTrack != null,
                            isDragging = isDragging,
                            onScrubStartOrMove = { fraction ->
                                isDragging = true
                                dragValue = fraction * totalDur.toFloat()
                            },
                            onScrubEnd = { fraction ->
                                isDragging = false
                                val target = (fraction * totalDur.toFloat()).toInt()
                                dragValue = target.toFloat()
                                onSeek(target)
                            },
                            barHeight = 28.dp,
                            seekBarTestTag = "player_bar_seek_bar",
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                        )
                        Text(
                            text = formatDuration(currentTrack?.duration ?: durationSeconds),
                            style = TabularNumStyle.copy(fontSize = 11.sp),
                            color = Color.White.copy(alpha = 0.88f),
                        )
                    }
                }

                // Zone 3: Volume, EQ badge & Mini-Player
                Row(
                    modifier = Modifier.weight(0.26f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                ) {
                    Row(
                        modifier = Modifier
                            .background(FgPrimaryDeep.copy(alpha = 0.55f))
                            .clickable(onClick = onOpenSettingsEq)
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Equalizer,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(eqPreset.labelRes()),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = onToggleMute,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = if (isMuted || volume == 0f) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                            contentDescription = stringResource(R.string.cd_mute),
                            tint = Color.White,
                            modifier = Modifier.size(19.dp),
                        )
                    }

                    var isDraggingVolume by remember { mutableStateOf(false) }
                    GrooveSeekBar(
                        progress = if (isMuted) 0f else volume.coerceIn(0f, 1f),
                        enabled = true,
                        isDragging = isDraggingVolume,
                        onScrubStartOrMove = { fraction ->
                            isDraggingVolume = true
                            onVolumeChange(fraction)
                        },
                        onScrubEnd = { fraction ->
                            isDraggingVolume = false
                            onVolumeChange(fraction)
                        },
                        barHeight = 32.dp,
                        seekBarTestTag = "player_bar_volume_slider",
                        modifier = Modifier.width(88.dp),
                    )
                }
            }
        }
    }
}
