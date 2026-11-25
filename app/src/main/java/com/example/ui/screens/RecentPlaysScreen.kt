package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.Album
import com.example.model.RecentPlayEntry
import com.example.model.Track
import com.example.ui.components.AlbumArtwork
import com.example.ui.components.TrackRow
import com.example.ui.formatLocalizedRelativeTime
import com.example.ui.theme.FgBorder
import com.example.ui.theme.FgPrimary
import com.example.ui.theme.FgSurface
import com.example.ui.theme.FgText
import com.example.ui.theme.FgTextMuted

@Composable
fun RecentPlaysScreen(
    recentPlays: List<RecentPlayEntry>,
    tracks: List<Track>,
    albums: List<Album>,
    currentTrackId: String?,
    isPlaying: Boolean,
    onPlayAlbum: (Album) -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onTogglePlay: () -> Unit,
    onAddToQueue: (Track) -> Unit,
    onRequestAddTrackToPlaylist: (Track) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
) {
    val trackMap = remember(tracks) { tracks.associateBy { it.id } }
    val albumMap = remember(albums) { albums.associateBy { it.id } }

    val resolvedRecent = remember(recentPlays, trackMap) {
        recentPlays.mapNotNull { entry ->
            trackMap[entry.trackId]?.let { entry to it }
        }
    }

    val recentAlbums = remember(resolvedRecent, albumMap) {
        resolvedRecent.mapNotNull { (_, track) -> albumMap[track.albumId] }.distinctBy { it.id }.take(12)
    }

    val recentTrackList = remember(resolvedRecent) { resolvedRecent.map { it.second } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FgSurface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.nav_recent_plays),
                style = MaterialTheme.typography.headlineLarge,
                color = FgText,
            )
            Text(
                text = stringResource(R.string.recent_plays_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = FgTextMuted,
            )
        }
        HorizontalDivider(color = FgBorder)

        if (resolvedRecent.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = null,
                        tint = FgTextMuted.copy(alpha = 0.55f),
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(stringResource(R.string.recent_plays_empty_title), style = MaterialTheme.typography.titleMedium, color = FgText)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.recent_plays_empty_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = FgTextMuted,
                    )
                }
            }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            if (recentAlbums.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.recent_albums),
                        style = MaterialTheme.typography.titleMedium,
                        color = FgText,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 10.dp),
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(recentAlbums, key = { it.id }) { album ->
                            Column(
                                modifier = Modifier
                                    .width(132.dp)
                                    .clickable { onOpenAlbum(album.id) },
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(132.dp)
                                        .border(0.5.dp, FgBorder),
                                ) {
                                    AlbumArtwork(
                                        coverUri = album.coverUri,
                                        artDesign = album.artDesign,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(6.dp)
                                            .size(32.dp)
                                            .background(FgPrimary)
                                            .clickable { onPlayAlbum(album) },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.PlayArrow,
                                            contentDescription = stringResource(R.string.cd_play_album),
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = album.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = FgText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = album.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FgTextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.recent_played_songs),
                    style = MaterialTheme.typography.titleMedium,
                    color = FgText,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 8.dp),
                )
                HorizontalDivider(color = FgBorder)
            }

            itemsIndexed(resolvedRecent, key = { _, pair -> pair.first.id }) { index, (entry, track) ->
                val isCurrent = track.id == currentTrackId
                TrackRow(
                    track = track,
                    index = index,
                    isCurrent = isCurrent,
                    isPlaying = isPlaying,
                    showArtwork = true,
                    showAlbum = true,
                    secondaryTrailingText = formatLocalizedRelativeTime(entry.playedAt),
                    onPlay = { if (isCurrent) onTogglePlay() else onPlayTrack(track, recentTrackList) },
                    onAddToQueue = { onAddToQueue(track) },
                    onAddToPlaylist = { onRequestAddTrackToPlaylist(track) },
                    onOpenAlbum = { onOpenAlbum(track.albumId) },
                    onOpenArtist = { onOpenArtist(track.artistId) },
                )
            }
        }
    }
}
