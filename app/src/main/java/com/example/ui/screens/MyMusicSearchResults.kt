package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddToQueue
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Shuffle
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.annotation.StringRes
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.Album
import com.example.model.Artist
import com.example.model.Track
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
import java.text.Normalizer

enum class SearchFilterCategory(@StringRes val labelRes: Int) {
    ALL(R.string.filter_all),
    SONGS(R.string.tab_songs),
    ALBUMS(R.string.tab_albums),
    ARTISTS(R.string.tab_artists),
}

private fun normalizeForSearch(text: String): String {
    return Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        .lowercase()
}

@Composable
fun MyMusicSearchResults(
    query: String,
    tracks: List<Track>,
    albums: List<Album>,
    artists: List<Artist>,
    currentTrackId: String?,
    isPlaying: Boolean,
    onClearSearch: () -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onPlayAlbum: (Album) -> Unit,
    onPlayArtist: (String) -> Unit,
    onTogglePlay: () -> Unit,
    onShuffleTracks: (List<Track>) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onAddAlbumToQueue: (Album) -> Unit,
    onRequestAddTrackToPlaylist: (Track) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
) {
    val normalizedQuery = remember(query) { normalizeForSearch(query.trim()) }
    var activeFilter by remember(query) { mutableStateOf(SearchFilterCategory.ALL) }

    val matchingTracks = remember(normalizedQuery, tracks) {
        if (normalizedQuery.isEmpty()) emptyList()
        else tracks.filter {
            normalizeForSearch(it.title).contains(normalizedQuery) ||
                normalizeForSearch(it.artist).contains(normalizedQuery) ||
                normalizeForSearch(it.album).contains(normalizedQuery) ||
                normalizeForSearch(it.genre).contains(normalizedQuery)
        }
    }

    val matchingAlbums = remember(normalizedQuery, albums) {
        if (normalizedQuery.isEmpty()) emptyList()
        else albums.filter {
            normalizeForSearch(it.title).contains(normalizedQuery) ||
                normalizeForSearch(it.artist).contains(normalizedQuery)
        }
    }

    val matchingArtists = remember(normalizedQuery, artists) {
        if (normalizedQuery.isEmpty()) emptyList()
        else artists.filter { normalizeForSearch(it.name).contains(normalizedQuery) }
    }

    val totalCount = matchingTracks.size + matchingAlbums.size + matchingArtists.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FgSurface)
            .testTag("my_music_search_results"),
    ) {
        // Result category filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val counts = mapOf(
                SearchFilterCategory.ALL to totalCount,
                SearchFilterCategory.SONGS to matchingTracks.size,
                SearchFilterCategory.ALBUMS to matchingAlbums.size,
                SearchFilterCategory.ARTISTS to matchingArtists.size,
            )
            SearchFilterCategory.entries.forEach { category ->
                val count = counts[category] ?: 0
                val selected = activeFilter == category
                Box(
                    modifier = Modifier
                        .background(if (selected) FgPrimary else FgSurfaceAlt)
                        .border(1.dp, if (selected) FgPrimary else FgBorder)
                        .clickable { activeFilter = category }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                        .testTag("search_filter_${category.name.lowercase()}"),
                ) {
                    Text(
                        text = "${stringResource(category.labelRes)} ($count)",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        ),
                        color = if (selected) Color.White else FgText,
                    )
                }
            }
        }

        HorizontalDivider(color = FgBorder)

        val showArtists = (activeFilter == SearchFilterCategory.ALL || activeFilter == SearchFilterCategory.ARTISTS) && matchingArtists.isNotEmpty()
        val showAlbums = (activeFilter == SearchFilterCategory.ALL || activeFilter == SearchFilterCategory.ALBUMS) && matchingAlbums.isNotEmpty()
        val showSongs = (activeFilter == SearchFilterCategory.ALL || activeFilter == SearchFilterCategory.SONGS) && matchingTracks.isNotEmpty()

        if (!showArtists && !showAlbums && !showSongs) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.SearchOff,
                        contentDescription = null,
                        tint = FgTextMuted.copy(alpha = 0.55f),
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.search_no_results, query.trim()),
                        style = MaterialTheme.typography.titleMedium,
                        color = FgText,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.search_no_results_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = FgTextMuted,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = onClearSearch,
                        shape = RectangleShape,
                        modifier = Modifier.testTag("search_empty_clear_button"),
                    ) {
                        Text(stringResource(R.string.search_clear), color = FgPrimary)
                    }
                }
            }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            if (showArtists) {
                item(key = "search_artists_section") {
                    Text(
                        text = "${stringResource(R.string.tab_artists)} (${matchingArtists.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = FgText,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 10.dp),
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(matchingArtists, key = { it.id }) { artist ->
                            Column(
                                modifier = Modifier
                                    .width(104.dp)
                                    .clickable { onOpenArtist(artist.id) }
                                    .testTag("search_artist_${artist.id}"),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(modifier = Modifier.size(92.dp)) {
                                    ArtistAvatar(
                                        name = artist.name,
                                        imageUrl = artist.imageUrl,
                                        fallbackCoverUri = artist.fallbackCoverUri,
                                        artDesign = artist.artDesign,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(FgPrimary)
                                            .clickable { onPlayArtist(artist.id) },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.PlayArrow,
                                            contentDescription = stringResource(R.string.cd_play_artist),
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = artist.name,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = FgText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                )
                                Text(
                                    text = songCountLabel(artist.trackCount),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = FgTextMuted,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }

            if (showAlbums) {
                item(key = "search_albums_section") {
                    Text(
                        text = "${stringResource(R.string.tab_albums)} (${matchingAlbums.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = FgText,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 10.dp),
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(matchingAlbums, key = { it.id }) { album ->
                            Column(
                                modifier = Modifier
                                    .width(136.dp)
                                    .clickable { onOpenAlbum(album.id) }
                                    .testTag("search_album_${album.id}"),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(136.dp)
                                        .border(0.5.dp, FgBorder),
                                ) {
                                    AlbumArtwork(
                                        coverUri = album.coverUri,
                                        artDesign = album.artDesign,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .background(Color.Black.copy(alpha = 0.68f))
                                                .clickable { onAddAlbumToQueue(album) },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.AddToQueue,
                                                contentDescription = stringResource(R.string.cd_add_album_queue),
                                                tint = Color.White,
                                                modifier = Modifier.size(15.dp),
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
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

            if (showSongs) {
                item(key = "search_songs_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "${stringResource(R.string.tab_songs)} (${matchingTracks.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = FgText,
                        )
                        Row(
                            modifier = Modifier
                                .clickable { onShuffleTracks(matchingTracks) }
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                                .testTag("search_shuffle_songs_button"),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shuffle,
                                contentDescription = null,
                                tint = FgPrimary,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.action_shuffle),
                                style = MaterialTheme.typography.labelMedium,
                                color = FgPrimary,
                            )
                        }
                    }
                    HorizontalDivider(color = FgBorder)
                }

                itemsIndexed(matchingTracks, key = { _, t -> "search_track_${t.id}" }) { idx, track ->
                    val isCurrent = track.id == currentTrackId
                    TrackRow(
                        track = track,
                        index = idx,
                        isCurrent = isCurrent,
                        isPlaying = isPlaying,
                        showArtwork = true,
                        showAlbum = true,
                        onPlay = { if (isCurrent) onTogglePlay() else onPlayTrack(track, matchingTracks) },
                        onAddToQueue = { onAddToQueue(track) },
                        onAddToPlaylist = { onRequestAddTrackToPlaylist(track) },
                        onOpenAlbum = { onOpenAlbum(track.albumId) },
                        onOpenArtist = { onOpenArtist(track.artistId) },
                    )
                }
            }
        }
    }
}
