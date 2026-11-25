package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.Playlist
import com.example.model.Track
import com.example.model.formatDuration
import com.example.ui.components.ConfirmDangerDialog
import com.example.ui.components.PlaylistMosaicArtwork
import com.example.ui.components.PlaylistNameDialog
import com.example.ui.components.TrackRow
import com.example.ui.songCountLabel
import com.example.ui.theme.FgBorder
import com.example.ui.theme.FgDanger
import com.example.ui.theme.FgPrimary
import com.example.ui.theme.FgSurface
import com.example.ui.theme.FgSurfaceAlt
import com.example.ui.theme.FgText
import com.example.ui.theme.FgTextMuted

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlaylistScreen(
    playlist: Playlist,
    allTracks: List<Track>,
    currentTrackId: String?,
    isPlaying: Boolean,
    onPlayPlaylist: (List<Track>) -> Unit,
    onShufflePlaylist: (List<Track>) -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onTogglePlay: () -> Unit,
    onAddToQueue: (Track) -> Unit,
    onRequestAddTrackToPlaylist: (Track) -> Unit,
    onRemoveFromPlaylist: (String) -> Unit,
    onRenamePlaylist: (String) -> Unit,
    onDeletePlaylist: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
) {
    val trackMap = remember(allTracks) { allTracks.associateBy { it.id } }
    val playlistTracks = remember(playlist.trackIds, trackMap) {
        playlist.trackIds.mapNotNull { trackMap[it] }
    }
    val totalDuration = remember(playlistTracks) { playlistTracks.sumOf { it.duration } }

    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showRenameDialog) {
        PlaylistNameDialog(
            title = stringResource(R.string.playlist_rename_title),
            confirmLabel = stringResource(R.string.action_rename),
            initialValue = playlist.name,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                showRenameDialog = false
                onRenamePlaylist(newName)
            },
        )
    }

    if (showDeleteDialog) {
        ConfirmDangerDialog(
            title = stringResource(R.string.playlist_delete_title),
            message = stringResource(R.string.playlist_delete_message, playlist.name),
            confirmLabel = stringResource(R.string.action_delete),
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                onDeletePlaylist()
            },
        )
    }

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
                    .padding(20.dp),
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    PlaylistMosaicArtwork(
                        tracks = playlistTracks,
                        modifier = Modifier
                            .size(132.dp)
                            .border(1.dp, FgBorder),
                    )
                    Spacer(modifier = Modifier.width(18.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.playlist_badge),
                            style = MaterialTheme.typography.labelSmall,
                            color = FgPrimary,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = FgText,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${songCountLabel(playlistTracks.size)}  •  ${formatDuration(totalDuration)}",
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
                        onClick = { onPlayPlaylist(playlistTracks) },
                        enabled = playlistTracks.isNotEmpty(),
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = FgPrimary),
                        modifier = Modifier.testTag("playlist_play_button"),
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_play), color = Color.White)
                    }
                    OutlinedButton(
                        onClick = { onShufflePlaylist(playlistTracks) },
                        enabled = playlistTracks.isNotEmpty(),
                        shape = RectangleShape,
                    ) {
                        Icon(Icons.Filled.Shuffle, contentDescription = null, tint = FgText, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_shuffle), color = FgText)
                    }
                    OutlinedButton(
                        onClick = { showRenameDialog = true },
                        shape = RectangleShape,
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, tint = FgText, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_rename), color = FgText)
                    }
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        shape = RectangleShape,
                    ) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = FgDanger, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_delete), color = FgDanger)
                    }
                }
            }
            HorizontalDivider(color = FgBorder)
        }

        if (playlistTracks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(36.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.QueueMusic,
                            contentDescription = null,
                            tint = FgTextMuted.copy(alpha = 0.5f),
                            modifier = Modifier.size(44.dp),
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(stringResource(R.string.playlist_empty_title), style = MaterialTheme.typography.titleMedium, color = FgText)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.playlist_empty_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = FgTextMuted,
                        )
                    }
                }
            }
        } else {
            itemsIndexed(playlistTracks, key = { idx, t -> "${t.id}-$idx" }) { index, track ->
                val isCurrent = track.id == currentTrackId
                TrackRow(
                    track = track,
                    index = index,
                    isCurrent = isCurrent,
                    isPlaying = isPlaying,
                    showArtwork = true,
                    showAlbum = true,
                    onPlay = { if (isCurrent) onTogglePlay() else onPlayTrack(track, playlistTracks) },
                    onAddToQueue = { onAddToQueue(track) },
                    onAddToPlaylist = { onRequestAddTrackToPlaylist(track) },
                    onOpenAlbum = { onOpenAlbum(track.albumId) },
                    onOpenArtist = { onOpenArtist(track.artistId) },
                    onRemoveFromPlaylist = { onRemoveFromPlaylist(track.id) },
                )
            }
        }
    }
}
