package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.model.Album
import com.example.model.Artist
import com.example.model.MyMusicTab
import com.example.model.RecentPlayEntry
import com.example.model.Track
import com.example.ui.components.AlbumArtwork
import com.example.ui.components.ArtistAvatar
import com.example.ui.components.PlayingEqualizerIndicator
import com.example.ui.components.TrackRow
import com.example.ui.formatLocalizedRelativeTime
import com.example.ui.playCountLabel
import com.example.ui.theme.FgBorder
import com.example.ui.theme.FgPlayingTint
import com.example.ui.theme.FgPrimary
import com.example.ui.theme.FgSurface
import com.example.ui.theme.FgSurfaceAlt
import com.example.ui.theme.FgText
import com.example.ui.theme.FgTextMuted
import com.example.ui.theme.TabularNumStyle

private sealed interface QuickAccessItem {
    val key: String
    data class AlbumItem(val album: Album) : QuickAccessItem {
        override val key: String = "qa_album_${album.id}"
    }
    data class ArtistItem(val artist: Artist) : QuickAccessItem {
        override val key: String = "qa_artist_${artist.id}"
    }
    data class TrackItem(val track: Track) : QuickAccessItem {
        override val key: String = "qa_track_${track.id}"
    }
}

@Composable
fun MyMusicHomeSection(
    tracks: List<Track>,
    albums: List<Album>,
    artists: List<Artist>,
    recentPlays: List<RecentPlayEntry>,
    currentTrackId: String?,
    isPlaying: Boolean,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onPlayAlbum: (Album) -> Unit,
    onPlayArtist: (String) -> Unit,
    onTogglePlay: () -> Unit,
    onShuffleAll: (List<Track>) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onRequestAddTrackToPlaylist: (Track) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onChangeTab: (MyMusicTab) -> Unit,
    onOpenRecentPlays: () -> Unit,
) {
    val trackMap = remember(tracks) { tracks.associateBy { it.id } }

    // Curated Quick Access mix of albums, artists, and tracks (up to 6 crisp tiles)
    val quickAccessItems = remember(albums, artists, tracks) {
        buildList<QuickAccessItem> {
            albums.take(2).forEach { add(QuickAccessItem.AlbumItem(it)) }
            artists.take(2).forEach { add(QuickAccessItem.ArtistItem(it)) }
            tracks.sortedByDescending { it.playCount }.take(2).forEach { add(QuickAccessItem.TrackItem(it)) }
        }.take(6)
    }

    // Top 8 Most Played tracks
    val topMostPlayed = remember(tracks) {
        tracks
            .filter { it.playCount > 0 }
            .sortedWith(compareByDescending<Track> { it.playCount }.thenByDescending { it.addedAt })
            .take(8)
    }

    // Recently Played (distinct tracks, latest first)
    val recentlyPlayedTracks = remember(recentPlays, trackMap) {
        val seen = HashSet<String>()
        buildList {
            for (entry in recentPlays) {
                if (seen.add(entry.trackId)) {
                    val track = trackMap[entry.trackId]
                    if (track != null) add(entry to track)
                }
                if (size >= 10) break
            }
        }
    }
    val recentTrackList = remember(recentlyPlayedTracks) { recentlyPlayedTracks.map { it.second } }

    // Recently Added tracks
    val recentlyAddedTracks = remember(tracks) {
        tracks.sortedByDescending { it.addedAt }.take(8)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(FgSurface)
            .testTag("home_tab_list"),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        // Section 1: Quick Access (Albums, Artists & Tracks)
        item {
            HomeSectionHeader(
                title = stringResource(R.string.home_quick_access),
                actionLabel = stringResource(R.string.action_shuffle_all),
                actionIcon = {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = null,
                        tint = FgPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                },
                onAction = { onShuffleAll(tracks) },
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                quickAccessItems.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        rowItems.forEach { item ->
                            QuickAccessTile(
                                item = item,
                                isCurrentTrack = item is QuickAccessItem.TrackItem && item.track.id == currentTrackId,
                                isPlaying = isPlaying,
                                onClick = {
                                    when (item) {
                                        is QuickAccessItem.AlbumItem -> onOpenAlbum(item.album.id)
                                        is QuickAccessItem.ArtistItem -> onOpenArtist(item.artist.id)
                                        is QuickAccessItem.TrackItem -> {
                                            if (item.track.id == currentTrackId) onTogglePlay()
                                            else onPlayTrack(item.track, tracks)
                                        }
                                    }
                                },
                                onQuickPlay = {
                                    when (item) {
                                        is QuickAccessItem.AlbumItem -> onPlayAlbum(item.album)
                                        is QuickAccessItem.ArtistItem -> onPlayArtist(item.artist.id)
                                        is QuickAccessItem.TrackItem -> {
                                            if (item.track.id == currentTrackId) onTogglePlay()
                                            else onPlayTrack(item.track, tracks)
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Section 2: Top 8 Most Played
        item {
            HomeSectionHeader(
                title = stringResource(R.string.home_top_most_played),
                actionLabel = if (topMostPlayed.isNotEmpty()) stringResource(R.string.action_play_all) else null,
                onAction = if (topMostPlayed.isNotEmpty()) {
                    { onPlayTrack(topMostPlayed.first(), topMostPlayed) }
                } else null,
            )
            if (topMostPlayed.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .background(FgSurfaceAlt)
                        .border(0.5.dp, FgBorder)
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.home_top_most_played_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = FgTextMuted,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.action_play_now),
                        style = MaterialTheme.typography.labelLarge,
                        color = FgPrimary,
                        modifier = Modifier
                            .clickable { tracks.firstOrNull()?.let { onPlayTrack(it, tracks) } }
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                    )
                }
            } else {
                HorizontalDivider(color = FgBorder)
            }
        }

        if (topMostPlayed.isNotEmpty()) {
            itemsIndexed(topMostPlayed, key = { _, t -> "top_${t.id}" }) { index, track ->
                val isCurrent = track.id == currentTrackId
                MostPlayedTrackRow(
                    rank = index + 1,
                    track = track,
                    isCurrent = isCurrent,
                    isPlaying = isPlaying,
                    onClick = {
                        if (isCurrent) onTogglePlay() else onPlayTrack(track, topMostPlayed)
                    },
                )
            }
        }

        // Section 3: Recently Played
        item {
            Spacer(modifier = Modifier.height(8.dp))
            HomeSectionHeader(
                title = stringResource(R.string.home_recently_played),
                actionLabel = if (recentlyPlayedTracks.isNotEmpty()) stringResource(R.string.action_see_all) else null,
                onAction = if (recentlyPlayedTracks.isNotEmpty()) onOpenRecentPlays else null,
            )

            if (recentlyPlayedTracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .background(FgSurfaceAlt)
                        .border(0.5.dp, FgBorder)
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                ) {
                    Text(
                        text = stringResource(R.string.home_recently_played_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = FgTextMuted,
                    )
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(recentlyPlayedTracks, key = { (entry, _) -> "home_rp_${entry.id}" }) { (entry, track) ->
                        val isCurrent = track.id == currentTrackId
                        Column(
                            modifier = Modifier
                                .width(128.dp)
                                .clickable {
                                    if (isCurrent) onTogglePlay() else onPlayTrack(track, recentTrackList)
                                },
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(128.dp)
                                    .border(0.5.dp, FgBorder),
                            ) {
                                AlbumArtwork(
                                    coverUri = track.coverUri,
                                    artDesign = track.artDesign,
                                    modifier = Modifier.fillMaxSize(),
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(6.dp)
                                        .size(32.dp)
                                        .background(FgPrimary),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (isCurrent) {
                                        PlayingEqualizerIndicator(isPlaying = isPlaying, color = Color.White)
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.PlayArrow,
                                            contentDescription = stringResource(R.string.cd_play),
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isCurrent) FgPrimary else FgText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${track.artist} • ${formatLocalizedRelativeTime(entry.playedAt)}",
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

        // Section 4: Recently Added Songs
        item {
            Spacer(modifier = Modifier.height(10.dp))
            HomeSectionHeader(
                title = stringResource(R.string.home_recently_added),
                actionLabel = stringResource(R.string.action_all_songs),
                onAction = { onChangeTab(MyMusicTab.SONGS) },
            )
            HorizontalDivider(color = FgBorder)
        }

        itemsIndexed(recentlyAddedTracks, key = { _, t -> "recent_add_${t.id}" }) { index, track ->
            val isCurrent = track.id == currentTrackId
            TrackRow(
                track = track,
                index = index,
                isCurrent = isCurrent,
                isPlaying = isPlaying,
                showArtwork = true,
                showAlbum = true,
                onPlay = {
                    if (isCurrent) onTogglePlay() else onPlayTrack(track, recentlyAddedTracks)
                },
                onAddToQueue = { onAddToQueue(track) },
                onAddToPlaylist = { onRequestAddTrackToPlaylist(track) },
                onOpenAlbum = { onOpenAlbum(track.albumId) },
                onOpenArtist = { onOpenArtist(track.artistId) },
            )
        }
    }
}

@Composable
private fun HomeSectionHeader(
    title: String,
    actionLabel: String? = null,
    actionIcon: (@Composable () -> Unit)? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = FgText,
        )
        if (actionLabel != null && onAction != null) {
            Row(
                modifier = Modifier
                    .clickable(onClick = onAction)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (actionIcon != null) {
                    actionIcon()
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = FgPrimary,
                )
            }
        }
    }
}

@Composable
private fun QuickAccessTile(
    item: QuickAccessItem,
    isCurrentTrack: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onQuickPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(58.dp)
            .background(if (isCurrentTrack) FgPlayingTint else FgSurfaceAlt)
            .border(0.5.dp, FgBorder)
            .clickable(onClick = onClick)
            .testTag(item.key),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (item) {
            is QuickAccessItem.AlbumItem -> {
                AlbumArtwork(
                    coverUri = item.album.coverUri,
                    artDesign = item.album.artDesign,
                    modifier = Modifier.size(58.dp),
                )
            }
            is QuickAccessItem.ArtistItem -> {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .padding(6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    ArtistAvatar(
                        name = item.artist.name,
                        imageUrl = item.artist.imageUrl,
                        fallbackCoverUri = item.artist.fallbackCoverUri,
                        artDesign = item.artist.artDesign,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            is QuickAccessItem.TrackItem -> {
                Box(modifier = Modifier.size(58.dp), contentAlignment = Alignment.Center) {
                    AlbumArtwork(
                        coverUri = item.track.coverUri,
                        artDesign = item.track.artDesign,
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (isCurrentTrack) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.50f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            PlayingEqualizerIndicator(isPlaying = isPlaying, color = Color.White)
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
        ) {
            val title = when (item) {
                is QuickAccessItem.AlbumItem -> item.album.title
                is QuickAccessItem.ArtistItem -> item.artist.name
                is QuickAccessItem.TrackItem -> item.track.title
            }
            val tagLabel = when (item) {
                is QuickAccessItem.AlbumItem -> stringResource(R.string.label_album_tag)
                is QuickAccessItem.ArtistItem -> stringResource(R.string.label_artist_tag)
                is QuickAccessItem.TrackItem -> stringResource(R.string.label_song_tag)
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (isCurrentTrack) FgPrimary else FgText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = tagLabel,
                style = MaterialTheme.typography.labelSmall,
                color = FgTextMuted,
                maxLines = 1,
            )
        }

        Box(
            modifier = Modifier
                .padding(end = 8.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(FgPrimary.copy(alpha = 0.12f))
                .clickable(onClick = onQuickPlay),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = stringResource(R.string.cd_play),
                tint = FgPrimary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun MostPlayedTrackRow(
    rank: Int,
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isCurrent) FgPlayingTint else if (rank % 2 == 0) FgSurfaceAlt.copy(alpha = 0.5f) else FgSurface)
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 9.dp)
                .testTag("most_played_rank_$rank"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.width(26.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (isCurrent) {
                    PlayingEqualizerIndicator(isPlaying = isPlaying, color = FgPrimary)
                } else {
                    Text(
                        text = "$rank",
                        style = TabularNumStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                        color = FgPrimary,
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            AlbumArtwork(
                coverUri = track.coverUri,
                artDesign = track.artDesign,
                modifier = Modifier.size(38.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
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
                    text = "${track.artist}  •  ${track.album}",
                    style = MaterialTheme.typography.bodySmall,
                    color = FgTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = playCountLabel(track.playCount),
                style = TabularNumStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                color = FgPrimary,
            )
        }
        HorizontalDivider(thickness = 0.5.dp, color = FgBorder)
    }
}
