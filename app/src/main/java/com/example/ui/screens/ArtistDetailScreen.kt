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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.Album
import com.example.model.Artist
import com.example.model.Track
import com.example.ui.albumCountLabel
import com.example.ui.components.AlbumArtwork
import com.example.ui.components.ArtistAvatar
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
fun ArtistDetailScreen(
    artist: Artist,
    albums: List<Album>,
    tracks: List<Track>,
    currentTrackId: String?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onPlayAll: (List<Track>) -> Unit,
    onShuffleAll: (List<Track>) -> Unit,
    onPlayAlbum: (Album) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onTogglePlay: () -> Unit,
    onAddToQueue: (Track) -> Unit,
    onRequestAddTrackToPlaylist: (Track) -> Unit,
) {
    BackHandler(onBack = onBack)

    val sortedAlbums = remember(albums) {
        albums.sortedWith(
            compareByDescending<Album> { it.year }
                .thenBy { it.title.lowercase() }
        )
    }

    val sortedTracks = remember(tracks, sortedAlbums) {
        val albumOrder = sortedAlbums.withIndex().associate { (index, album) -> album.id to index }
        tracks.sortedWith(
            compareBy<Track> { albumOrder[it.albumId] ?: Int.MAX_VALUE }
                .thenByDescending { it.year }
                .thenBy { it.album.lowercase() }
                .thenBy { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE }
                .thenBy { it.title.lowercase() }
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
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("artist_back_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = FgText,
                        )
                    }
                    Text(
                        text = stringResource(R.string.cd_back),
                        style = MaterialTheme.typography.titleSmall,
                        color = FgTextMuted,
                        modifier = Modifier.clickable(onClick = onBack),
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    ArtistAvatar(
                        name = artist.name,
                        imageUrl = artist.imageUrl,
                        fallbackCoverUri = artist.fallbackCoverUri,
                        artDesign = artist.artDesign,
                        modifier = Modifier.size(118.dp),
                    )
                    Spacer(modifier = Modifier.width(18.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = artist.name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = FgText,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${albumCountLabel(artist.albumCount)}  •  ${songCountLabel(artist.trackCount)}",
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
                        onClick = { onPlayAll(sortedTracks) },
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = FgPrimary),
                        modifier = Modifier.testTag("artist_play_all_button"),
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_play_all), color = Color.White)
                    }
                    OutlinedButton(
                        onClick = { onShuffleAll(sortedTracks) },
                        shape = RectangleShape,
                    ) {
                        Icon(Icons.Filled.Shuffle, contentDescription = null, tint = FgText, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_shuffle), color = FgText)
                    }
                }
            }
            HorizontalDivider(color = FgBorder)
        }

        if (sortedAlbums.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.tab_albums),
                    style = MaterialTheme.typography.titleMedium,
                    color = FgText,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 10.dp),
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(sortedAlbums, key = { it.id }) { album ->
                        Column(
                            modifier = Modifier
                                .width(136.dp)
                                .clickable { onOpenAlbum(album.id) },
                        ) {
                            AlbumArtwork(
                                coverUri = album.coverUri,
                                artDesign = album.artDesign,
                                modifier = Modifier
                                    .size(136.dp)
                                    .border(0.5.dp, FgBorder),
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = album.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = FgText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            val songLabel = songCountLabel(album.trackCount)
                            Text(
                                text = if (album.year > 0) "${album.year} • $songLabel" else songLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = FgTextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clickable { onPlayAlbum(album) },
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = stringResource(R.string.tab_songs),
                style = MaterialTheme.typography.titleMedium,
                color = FgText,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 8.dp),
            )
            HorizontalDivider(color = FgBorder)
        }

        itemsIndexed(sortedTracks, key = { _, t -> t.id }) { index, track ->
            val isCurrent = track.id == currentTrackId
            TrackRow(
                track = track,
                index = index,
                isCurrent = isCurrent,
                isPlaying = isPlaying,
                showArtwork = true,
                showAlbum = true,
                onPlay = { if (isCurrent) onTogglePlay() else onPlayTrack(track, sortedTracks) },
                onAddToQueue = { onAddToQueue(track) },
                onAddToPlaylist = { onRequestAddTrackToPlaylist(track) },
                onOpenAlbum = { onOpenAlbum(track.albumId) },
            )
        }
    }
}
