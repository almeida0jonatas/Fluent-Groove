package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddToQueue
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.Album
import com.example.model.Artist
import com.example.model.MyMusicTab
import com.example.model.RecentPlayEntry
import com.example.model.SongSortOrder
import com.example.model.Track
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.sp
import com.example.ui.albumCountLabel
import com.example.ui.artistCountLabel
import com.example.ui.components.AlbumArtwork
import com.example.ui.components.ArtistAvatar
import com.example.ui.components.JumpToLetterDialog
import com.example.ui.components.TrackRow
import com.example.ui.labelRes
import com.example.ui.songCountLabel
import com.example.ui.theme.FgBorder
import com.example.ui.theme.FgBorderStrong
import com.example.ui.theme.FgPrimary
import com.example.ui.theme.FgPrimaryDeep
import com.example.ui.theme.FgSurface
import com.example.ui.theme.FgSurfaceAlt
import com.example.ui.theme.FgText
import com.example.ui.theme.FgTextMuted
import kotlinx.coroutines.launch
import java.text.Normalizer

@Composable
fun MyMusicScreen(
    activeTab: MyMusicTab,
    searchQuery: String,
    albums: List<Album>,
    artists: List<Artist>,
    tracks: List<Track>,
    recentPlays: List<RecentPlayEntry>,
    songSortOrder: SongSortOrder,
    currentTrackId: String?,
    isPlaying: Boolean,
    isImporting: Boolean,
    importingCount: Int,
    onChangeTab: (MyMusicTab) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onChangeSortOrder: (SongSortOrder) -> Unit,
    onPlayAlbum: (Album) -> Unit,
    onPlayArtist: (String) -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onTogglePlay: () -> Unit,
    onShuffleAll: (List<Track>) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenRecentPlays: () -> Unit,
    onAddToQueue: (Track) -> Unit,
    onAddAlbumToQueue: (Album) -> Unit,
    onRequestAddTrackToPlaylist: (Track) -> Unit,
    onImportFiles: () -> Unit,
    onImportFolder: () -> Unit,
    onLoadDemoTrack: () -> Unit,
) {
    val isSearching = searchQuery.isNotBlank()
    val focusManager = LocalFocusManager.current
    var searchFocused by remember { mutableStateOf(false) }
    val sortedTracks = remember(tracks, songSortOrder) {
        when (songSortOrder) {
            SongSortOrder.DATE_ADDED -> tracks.sortedByDescending { it.addedAt }
            SongSortOrder.TITLE -> tracks.sortedBy { it.title.lowercase() }
            SongSortOrder.ARTIST -> tracks.sortedWith(compareBy<Track> { it.artist.lowercase() }.thenBy { it.album.lowercase() }.thenBy { it.trackNumber })
            SongSortOrder.ALBUM -> tracks.sortedWith(compareBy<Track> { it.album.lowercase() }.thenBy { it.trackNumber })
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FgSurface),
    ) {
        // Groove Music Header + Tabs
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            Text(
                text = stringResource(R.string.nav_my_music),
                style = MaterialTheme.typography.headlineLarge,
                color = FgText,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Persistent Global Library Search Bar (exclusively inside My Music)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(FgSurfaceAlt)
                    .border(
                        width = if (searchFocused || isSearching) 1.5.dp else 1.dp,
                        color = if (searchFocused || isSearching) FgPrimary else FgBorderStrong,
                    )
                    .padding(start = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = if (searchFocused || isSearching) FgPrimary else FgTextMuted,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = stringResource(R.string.search_placeholder),
                            style = MaterialTheme.typography.bodyMedium,
                            color = FgTextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = FgText),
                        cursorBrush = SolidColor(FgPrimary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = { focusManager.clearFocus() },
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { searchFocused = it.isFocused }
                            .testTag("my_music_search_input"),
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            onSearchQueryChange("")
                            focusManager.clearFocus()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("my_music_search_clear"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.cd_clear_search),
                            tint = FgTextMuted,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            if (!isSearching) {
                Spacer(modifier = Modifier.height(12.dp))

                // Windows 10 Groove Tabs: Home | Songs | Artists | Albums
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(22.dp),
                ) {
                    MyMusicTab.entries.forEach { tab ->
                        val selected = tab == activeTab
                        Column(
                            modifier = Modifier
                                .clickable { onChangeTab(tab) }
                                .padding(vertical = 4.dp)
                                .testTag("tab_${tab.name.lowercase()}"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = stringResource(tab.labelRes()),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                ),
                                color = if (selected) FgText else FgTextMuted,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .width(44.dp)
                                    .height(2.5.dp)
                                    .background(if (selected) FgPrimary else Color.Transparent),
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = FgBorder)

        // Importing banner
        if (isImporting) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FgSurfaceAlt)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = FgPrimary,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (importingCount > 0) {
                        stringResource(R.string.importing_files, importingCount)
                    } else {
                        stringResource(R.string.importing_folder)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = FgText,
                )
            }
        }

        if (tracks.isEmpty()) {
            EmptyLibraryState(
                onImportFiles = onImportFiles,
                onImportFolder = onImportFolder,
                onLoadDemoTrack = onLoadDemoTrack,
            )
            return
        }

        if (isSearching) {
            MyMusicSearchResults(
                query = searchQuery,
                tracks = tracks,
                albums = albums,
                artists = artists,
                currentTrackId = currentTrackId,
                isPlaying = isPlaying,
                onClearSearch = {
                    onSearchQueryChange("")
                    focusManager.clearFocus()
                },
                onPlayTrack = onPlayTrack,
                onPlayAlbum = onPlayAlbum,
                onPlayArtist = onPlayArtist,
                onTogglePlay = onTogglePlay,
                onShuffleTracks = onShuffleAll,
                onAddToQueue = onAddToQueue,
                onAddAlbumToQueue = onAddAlbumToQueue,
                onRequestAddTrackToPlaylist = onRequestAddTrackToPlaylist,
                onOpenAlbum = onOpenAlbum,
                onOpenArtist = onOpenArtist,
            )
            return
        }

        // Action Bar: Shuffle all + Sort (hidden on Home since Home has its own Quick Access header)
        if (activeTab != MyMusicTab.HOME) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier
                        .clickable { onShuffleAll(tracks) }
                        .padding(vertical = 6.dp, horizontal = 4.dp)
                        .testTag("shuffle_all_button"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = null,
                        tint = FgPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.action_shuffle_all_count, tracks.size),
                        style = MaterialTheme.typography.labelLarge,
                        color = FgPrimary,
                    )
                }

                if (activeTab == MyMusicTab.SONGS) {
                    var sortExpanded by remember { mutableStateOf(false) }
                    Box {
                        Row(
                            modifier = Modifier
                                .clickable { sortExpanded = true }
                                .padding(vertical = 6.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Sort,
                                contentDescription = null,
                                tint = FgTextMuted,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.sort_by_prefix, stringResource(songSortOrder.labelRes())),
                                style = MaterialTheme.typography.bodySmall,
                                color = FgPrimary,
                            )
                        }
                        DropdownMenu(
                            expanded = sortExpanded,
                            onDismissRequest = { sortExpanded = false },
                            containerColor = FgSurface,
                        ) {
                            SongSortOrder.entries.forEach { order ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(order.labelRes())) },
                                    onClick = {
                                        sortExpanded = false
                                        onChangeSortOrder(order)
                                    },
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = when (activeTab) {
                            MyMusicTab.ALBUMS -> albumCountLabel(albums.size)
                            MyMusicTab.ARTISTS -> artistCountLabel(artists.size)
                            else -> songCountLabel(tracks.size)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = FgTextMuted,
                    )
                }
            }
        }

        // Main Tab Content
        val scope = rememberCoroutineScope()
        when (activeTab) {
            MyMusicTab.HOME -> {
                MyMusicHomeSection(
                    tracks = tracks,
                    albums = albums,
                    artists = artists,
                    recentPlays = recentPlays,
                    currentTrackId = currentTrackId,
                    isPlaying = isPlaying,
                    onPlayTrack = onPlayTrack,
                    onPlayAlbum = onPlayAlbum,
                    onPlayArtist = onPlayArtist,
                    onTogglePlay = onTogglePlay,
                    onShuffleAll = onShuffleAll,
                    onAddToQueue = onAddToQueue,
                    onRequestAddTrackToPlaylist = onRequestAddTrackToPlaylist,
                    onOpenAlbum = onOpenAlbum,
                    onOpenArtist = onOpenArtist,
                    onChangeTab = onChangeTab,
                    onOpenRecentPlays = onOpenRecentPlays,
                )
            }

            MyMusicTab.ALBUMS -> {
                val albumsGridState = rememberLazyGridState()
                val groupedAlbums = remember(albums) { groupAlphabetically(albums) { it.title } }
                val letterHeaderIndices = remember(groupedAlbums) { computeGridHeaderIndices(groupedAlbums) }
                var jumpDialogActiveLetter by remember { mutableStateOf<String?>(null) }

                jumpDialogActiveLetter?.let { activeLetter ->
                    JumpToLetterDialog(
                        availableLetters = letterHeaderIndices.keys,
                        activeLetter = activeLetter,
                        onSelectLetter = { selected ->
                            jumpDialogActiveLetter = null
                            letterHeaderIndices[selected]?.let { targetIdx ->
                                scope.launch { albumsGridState.animateScrollToItem(targetIdx) }
                            }
                        },
                        onDismiss = { jumpDialogActiveLetter = null },
                    )
                }

                LazyVerticalGrid(
                    state = albumsGridState,
                    columns = GridCells.Adaptive(minSize = 152.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    groupedAlbums.forEach { (letter, groupItems) ->
                        item(
                            key = "album_header_$letter",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            AlphabetSectionHeader(
                                letter = letter,
                                testTag = "album_letter_header_$letter",
                                onClick = { jumpDialogActiveLetter = letter },
                            )
                        }
                        items(groupItems, key = { it.id }) { album ->
                            AlbumGridCard(
                                album = album,
                                onOpen = { onOpenAlbum(album.id) },
                                onPlay = { onPlayAlbum(album) },
                                onAddToQueue = { onAddAlbumToQueue(album) },
                            )
                        }
                    }
                }
            }

            MyMusicTab.ARTISTS -> {
                val artistsGridState = rememberLazyGridState()
                val groupedArtists = remember(artists) { groupAlphabetically(artists) { it.name } }
                val letterHeaderIndices = remember(groupedArtists) { computeGridHeaderIndices(groupedArtists) }
                var jumpDialogActiveLetter by remember { mutableStateOf<String?>(null) }

                jumpDialogActiveLetter?.let { activeLetter ->
                    JumpToLetterDialog(
                        availableLetters = letterHeaderIndices.keys,
                        activeLetter = activeLetter,
                        onSelectLetter = { selected ->
                            jumpDialogActiveLetter = null
                            letterHeaderIndices[selected]?.let { targetIdx ->
                                scope.launch { artistsGridState.animateScrollToItem(targetIdx) }
                            }
                        },
                        onDismiss = { jumpDialogActiveLetter = null },
                    )
                }

                LazyVerticalGrid(
                    state = artistsGridState,
                    columns = GridCells.Adaptive(minSize = 140.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    groupedArtists.forEach { (letter, groupItems) ->
                        item(
                            key = "artist_header_$letter",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            AlphabetSectionHeader(
                                letter = letter,
                                testTag = "artist_letter_header_$letter",
                                onClick = { jumpDialogActiveLetter = letter },
                            )
                        }
                        items(groupItems, key = { it.id }) { artist ->
                            ArtistGridCard(
                                artist = artist,
                                onOpen = { onOpenArtist(artist.id) },
                                onPlay = { onPlayArtist(artist.id) },
                            )
                        }
                    }
                }
            }

            MyMusicTab.SONGS -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    itemsIndexed(sortedTracks, key = { _, t -> t.id }) { index, track ->
                        val isCurrent = track.id == currentTrackId
                        TrackRow(
                            track = track,
                            index = index,
                            isCurrent = isCurrent,
                            isPlaying = isPlaying,
                            showArtwork = true,
                            showAlbum = true,
                            onPlay = {
                                if (isCurrent) onTogglePlay() else onPlayTrack(track, sortedTracks)
                            },
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
}

@Composable
private fun AlbumGridCard(
    album: Album,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    onAddToQueue: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .testTag("album_card_${album.id}"),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .border(0.5.dp, FgBorder),
        ) {
            AlbumArtwork(
                coverUri = album.coverUri,
                artDesign = album.artDesign,
                modifier = Modifier.fillMaxSize(),
            )

            // Crisp Groove bottom-right play & queue action cluster
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color.Black.copy(alpha = 0.68f))
                        .clickable(onClick = onAddToQueue),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddToQueue,
                        contentDescription = stringResource(R.string.cd_add_album_queue),
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(FgPrimary)
                        .clickable(onClick = onPlay),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = stringResource(R.string.cd_play_album),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
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

@Composable
private fun ArtistGridCard(
    artist: Artist,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .testTag("artist_card_${artist.id}"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .aspectRatio(1f),
        ) {
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
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(FgPrimary)
                    .clickable(onClick = onPlay),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.cd_play_artist),
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = artist.name,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = FgText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "${albumCountLabel(artist.albumCount)} • ${songCountLabel(artist.trackCount)}",
            style = MaterialTheme.typography.bodySmall,
            color = FgTextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun EmptyLibraryState(
    onImportFiles: () -> Unit,
    onImportFolder: () -> Unit,
    onLoadDemoTrack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(FgSurfaceAlt)
                    .border(1.dp, FgBorder),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.LibraryMusic,
                    contentDescription = null,
                    tint = FgPrimary,
                    modifier = Modifier.size(36.dp),
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = stringResource(R.string.empty_library_title),
                style = MaterialTheme.typography.titleLarge,
                color = FgText,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.empty_library_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = FgTextMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(22.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onImportFiles,
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = FgPrimary),
                    modifier = Modifier.testTag("empty_import_files_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.FileUpload,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.action_import_files_short), color = Color.White)
                }
                OutlinedButton(
                    onClick = onImportFolder,
                    shape = RectangleShape,
                    modifier = Modifier.testTag("empty_import_folder_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.CreateNewFolder,
                        contentDescription = null,
                        tint = FgPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.action_import_folder_short), color = FgPrimary)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onLoadDemoTrack,
                shape = RectangleShape,
                modifier = Modifier.testTag("empty_load_demo_button"),
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = FgPrimary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.action_load_demo), color = FgPrimary)
            }
        }
    }
}

@Composable
private fun AlphabetSectionHeader(
    letter: String,
    testTag: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .size(width = 48.dp, height = 40.dp)
                .clickable(onClick = onClick)
                .testTag(testTag),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = letter,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                ),
                color = FgPrimaryDeep,
            )
        }
    }
}

private fun alphabetBucket(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return "#"
    val normalized = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    val first = normalized.firstOrNull()?.uppercaseChar() ?: return "#"
    return if (first in 'A'..'Z') first.toString() else "#"
}

private fun <T> groupAlphabetically(
    items: List<T>,
    selector: (T) -> String,
): List<Pair<String, List<T>>> {
    val grouped = items.groupBy { alphabetBucket(selector(it)) }
    val keys = grouped.keys.sortedWith(
        compareBy<String> { if (it == "#") 0 else 1 }.thenBy { it }
    )
    return keys.map { key ->
        key to (grouped[key].orEmpty().sortedBy { selector(it).lowercase() })
    }
}

private fun <T> computeGridHeaderIndices(
    grouped: List<Pair<String, List<T>>>,
): Map<String, Int> {
    val map = linkedMapOf<String, Int>()
    var cursor = 0
    for ((letter, groupItems) in grouped) {
        map[letter] = cursor
        cursor += 1 + groupItems.size
    }
    return map
}

