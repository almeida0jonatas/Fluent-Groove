package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddToQueue
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.Album
import com.example.model.Track
import com.example.model.formatDuration
import com.example.ui.components.AlbumArtwork
import com.example.ui.components.TrackRow
import com.example.ui.songCountLabel
import com.example.ui.theme.FgBorder
import com.example.ui.theme.FgPrimary
import com.example.ui.theme.FgSurface
import com.example.ui.theme.FgSurfaceAlt
import com.example.ui.theme.FgText
import com.example.ui.theme.FgTextMuted

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlbumDetailScreen(
    album: Album,
    tracks: List<Track>,
    currentTrackId: String?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onPlayAlbum: () -> Unit,
    onShuffleAlbum: () -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onTogglePlay: () -> Unit,
    onAddToQueue: (Track) -> Unit,
    onAddAlbumToQueue: () -> Unit,
    onRequestAddTrackToPlaylist: (Track) -> Unit,
    onRequestAddAlbumToPlaylist: () -> Unit,
    onOpenArtist: (String) -> Unit,
) {
    BackHandler(onBack = onBack)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(FgSurface),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FgSurfaceAlt.copy(alpha = 0.7f))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("album_back_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = FgText,
                        )
                    }
                    Text(
                        text = stringResource(R.string.tab_albums),
                        style = MaterialTheme.typography.titleSmall,
                        color = FgTextMuted,
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    AlbumArtwork(
                        coverUri = album.coverUri,
                        artDesign = album.artDesign,
                        modifier = Modifier
                            .size(136.dp)
                            .border(1.dp, FgBorder),
                    )
                    Spacer(modifier = Modifier.width(18.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = album.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = FgText,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = album.artist,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = FgPrimary,
                            modifier = Modifier
                                .clickable { onOpenArtist(album.artistId) }
                                .padding(vertical = 2.dp),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val songsCountText = songCountLabel(tracks.size)
                        val meta = buildList {
                            if (album.year > 0) add(album.year.toString())
                            if (album.genre.isNotBlank()) add(album.genre)
                            add(songsCountText)
                            add(formatDuration(album.totalDuration))
                        }.joinToString("  •  ")
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.bodySmall,
                            color = FgTextMuted,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onPlayAlbum,
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = FgPrimary),
                        modifier = Modifier.testTag("album_play_button"),
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_play), color = Color.White)
                    }
                    OutlinedButton(
                        onClick = onShuffleAlbum,
                        shape = RectangleShape,
                    ) {
                        Icon(Icons.Filled.Shuffle, contentDescription = null, tint = FgText, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_shuffle), color = FgText)
                    }
                    OutlinedButton(
                        onClick = onAddAlbumToQueue,
                        shape = RectangleShape,
                    ) {
                        Icon(Icons.Filled.AddToQueue, contentDescription = null, tint = FgText, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_queue), color = FgText)
                    }
                    OutlinedButton(
                        onClick = onRequestAddAlbumToPlaylist,
                        shape = RectangleShape,
                    ) {
                        Icon(Icons.Filled.PlaylistAdd, contentDescription = null, tint = FgText, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_add_to_playlist), color = FgText)
                    }
                }
            }
            HorizontalDivider(color = FgBorder)
        }

        itemsIndexed(tracks, key = { _, t -> t.id }) { index, track ->
            val isCurrent = track.id == currentTrackId
            TrackRow(
                track = track,
                index = index,
                isCurrent = isCurrent,
                isPlaying = isPlaying,
                showArtwork = false,
                showAlbum = false,
                onPlay = { if (isCurrent) onTogglePlay() else onPlayTrack(track, tracks) },
                onAddToQueue = { onAddToQueue(track) },
                onAddToPlaylist = { onRequestAddTrackToPlaylist(track) },
                onOpenArtist = { onOpenArtist(track.artistId) },
            )
        }
    }
}
