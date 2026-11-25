package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Track
import com.example.model.formatDuration
import com.example.ui.theme.FgBorder
import com.example.ui.theme.FgPlayingTint
import com.example.ui.theme.FgPrimary
import com.example.ui.theme.FgSurface
import com.example.ui.theme.FgSurfaceAlt
import com.example.ui.theme.FgText
import com.example.ui.theme.FgTextMuted
import com.example.ui.theme.TabularNumStyle

@Composable
fun TrackRow(
    track: Track,
    index: Int,
    isCurrent: Boolean,
    isPlaying: Boolean,
    showArtwork: Boolean = true,
    showAlbum: Boolean = true,
    secondaryTrailingText: String? = null,
    onPlay: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onOpenAlbum: (() -> Unit)? = null,
    onOpenArtist: (() -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val rowBg = when {
        isCurrent -> FgPlayingTint
        index % 2 == 1 -> FgSurfaceAlt.copy(alpha = 0.52f)
        else -> FgSurface
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(rowBg)
                .clickable(onClick = onPlay)
                .padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
                .testTag("track_row_${track.id}"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left index / Playing EQ / Thumbnail
            if (showArtwork) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AlbumArtwork(
                        coverUri = track.coverUri,
                        artDesign = track.artDesign,
                        modifier = Modifier.size(40.dp),
                    )
                    if (isCurrent) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.Black.copy(alpha = 0.52f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            PlayingEqualizerIndicator(isPlaying = isPlaying, color = Color.White)
                        }
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
            } else {
                Box(
                    modifier = Modifier.width(30.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (isCurrent) {
                        PlayingEqualizerIndicator(isPlaying = isPlaying, color = FgPrimary)
                    } else {
                        Text(
                            text = track.trackNumber.toString(),
                            style = TabularNumStyle.copy(fontSize = 13.sp),
                            color = FgTextMuted,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Title & Artist/Album
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
                val subtitle = buildString {
                    append(track.artist)
                    if (showAlbum && track.album.isNotBlank()) {
                        append("  •  ")
                        append(track.album)
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = FgTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Duration or relative timestamp
            Text(
                text = secondaryTrailingText ?: formatDuration(track.duration),
                style = TabularNumStyle.copy(fontSize = 12.sp),
                color = if (isCurrent) FgPrimary else FgTextMuted,
            )

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.cd_more_options),
                        tint = FgTextMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    containerColor = FgSurface,
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(if (isCurrent && isPlaying) R.string.action_pause else R.string.action_play)) },
                        onClick = {
                            menuExpanded = false
                            onPlay()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_add_to_queue)) },
                        onClick = {
                            menuExpanded = false
                            onAddToQueue()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_add_to_playlist)) },
                        onClick = {
                            menuExpanded = false
                            onAddToPlaylist()
                        },
                    )
                    if (onOpenAlbum != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_go_to_album)) },
                            onClick = {
                                menuExpanded = false
                                onOpenAlbum()
                            },
                        )
                    }
                    if (onOpenArtist != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_go_to_artist)) },
                            onClick = {
                                menuExpanded = false
                                onOpenArtist()
                            },
                        )
                    }
                    if (onRemoveFromPlaylist != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_remove_from_playlist)) },
                            onClick = {
                                menuExpanded = false
                                onRemoveFromPlaylist()
                            },
                        )
                    }
                }
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = FgBorder)
    }
}
