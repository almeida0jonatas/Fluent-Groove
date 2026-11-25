package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.model.Album
import com.example.model.Artist
import com.example.model.LrcParser
import com.example.model.Playlist
import com.example.model.RecentPlayEntry
import com.example.model.Track
import com.example.model.TrackLyrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

data class CatalogSummary(
    val albums: List<Album>,
    val artists: List<Artist>,
)

data class ImportResult(
    val trackCount: Int,
    val albumCount: Int,
    val artistCount: Int,
)

class MusicRepository(
    private val context: Context,
    private val dao: MusicDao,
    private val artistImageFetcher: ArtistImageFetcher = ArtistImageFetcher(),
    private val lyricsFetcher: LyricsFetcher = LyricsFetcher(),
) {
    val tracksFlow: Flow<List<Track>> = dao.observeTracks().map { list ->
        PrimaryArtistResolver.normalizeTrackGrouping(list.map { it.toModel() })
    }

    val playlistsFlow: Flow<List<Playlist>> = dao.observePlaylists().map { list ->
        list.map { it.toModel() }
    }

    val recentPlaysFlow: Flow<List<RecentPlayEntry>> = dao.observeRecentPlays().map { list ->
        list.map { it.toModel() }
    }

    val artistImageMapFlow: Flow<Map<String, String>> = dao.observeArtistCache().map { list ->
        buildMap {
            for (item in list) {
                if (!item.imageUrl.isNullOrBlank()) {
                    put(item.artistKey, item.imageUrl)
                }
            }
        }
    }

    fun buildCatalogFromTracks(
        tracks: List<Track>,
        artistImageMap: Map<String, String>,
    ): CatalogSummary {
        if (tracks.isEmpty()) return CatalogSummary(emptyList(), emptyList())

        val knownBaseKeys = tracks
            .map { PrimaryArtistResolver.extractPrimaryArtist(it.artist).lowercase(Locale.US) }
            .toSet()

        val primaryArtistNameById = tracks
            .groupBy { it.artistId }
            .mapValues { (_, artistTracks) ->
                PrimaryArtistResolver.resolveDisplayNameForGroup(artistTracks, knownBaseKeys)
            }

        val albums = tracks
            .groupBy { it.albumId }
            .map { (albumId, albumTracks) ->
                val sorted = albumTracks.sortedBy { it.trackNumber }
                val first = sorted.first()
                val canonicalArtist = primaryArtistNameById[first.artistId]
                    ?: PrimaryArtistResolver.extractPrimaryArtist(first.artist, knownBaseKeys)
                val coverUri = sorted.firstOrNull { !it.coverUri.isNullOrBlank() }?.coverUri
                val bestYear = sorted.maxOfOrNull { it.year } ?: 0
                Album(
                    id = albumId,
                    title = first.album,
                    artist = canonicalArtist,
                    artistId = first.artistId,
                    year = bestYear,
                    genre = first.genre,
                    trackCount = sorted.size,
                    totalDuration = sorted.sumOf { it.duration },
                    coverUri = coverUri,
                    artDesign = first.artDesign,
                )
            }
            .sortedBy { it.title.lowercase(Locale.US) }

        val artists = tracks
            .groupBy { it.artistId }
            .map { (artistId, artistTracks) ->
                val first = artistTracks.first()
                val canonicalName = primaryArtistNameById[artistId]
                    ?: PrimaryArtistResolver.extractPrimaryArtist(first.artist, knownBaseKeys)
                val key = canonicalName.trim().lowercase(Locale.US)
                val albumCount = artistTracks.map { it.albumId }.distinct().size
                val fallbackCover = artistTracks.firstOrNull { !it.coverUri.isNullOrBlank() }?.coverUri
                Artist(
                    id = artistId,
                    name = canonicalName,
                    albumCount = albumCount,
                    trackCount = artistTracks.size,
                    imageUrl = artistImageMap[key],
                    fallbackCoverUri = fallbackCover,
                    artDesign = first.artDesign,
                )
            }
            .sortedBy { it.name.lowercase(Locale.US) }

        return CatalogSummary(albums = albums, artists = artists)
    }

    suspend fun importAudioUris(uris: List<Uri>): ImportResult = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val parsedTracks = mutableListOf<Track>()

        uris.forEachIndexed { index, uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            val parsed = AudioMetadataParser.parseUri(context, uri, now + index)
            if (parsed != null) {
                parsedTracks.add(parsed)
            }
        }

        if (parsedTracks.isNotEmpty()) {
            dao.upsertTracks(parsedTracks.map { TrackEntity.fromModel(it) })
        }

        ImportResult(
            trackCount = parsedTracks.size,
            albumCount = parsedTracks.map { it.albumId }.distinct().size,
            artistCount = parsedTracks.map { it.artistId }.distinct().size,
        )
    }

    suspend fun importFolderTree(treeUri: Uri): ImportResult = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        val audioUris = AudioMetadataParser.collectAudioUrisFromTree(context, treeUri)
        if (audioUris.isEmpty()) return@withContext ImportResult(0, 0, 0)

        val now = System.currentTimeMillis()
        val parsedTracks = mutableListOf<Track>()
        audioUris.forEachIndexed { index, uri ->
            val parsed = AudioMetadataParser.parseUri(context, uri, now + index)
            if (parsed != null) {
                parsedTracks.add(parsed)
            }
        }

        if (parsedTracks.isNotEmpty()) {
            dao.upsertTracks(parsedTracks.map { TrackEntity.fromModel(it) })
        }

        ImportResult(
            trackCount = parsedTracks.size,
            albumCount = parsedTracks.map { it.albumId }.distinct().size,
            artistCount = parsedTracks.map { it.artistId }.distinct().size,
        )
    }

    suspend fun fetchMissingArtistImages(artistNames: List<String>) = withContext(Dispatchers.IO) {
        for (rawName in artistNames.distinct()) {
            val key = rawName.trim().lowercase(Locale.US)
            if (key.isEmpty() || key == "unknown artist" || key == "local audio") continue
            val existing = dao.getArtistCache(key)
            if (existing != null) continue
            val url = artistImageFetcher.fetchArtistImageUrl(rawName)
            dao.upsertArtistCache(
                ArtistCacheEntity(
                    artistKey = key,
                    imageUrl = url,
                    fetchedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    suspend fun refreshArtistImage(artistName: String): Boolean = withContext(Dispatchers.IO) {
        val key = artistName.trim().lowercase(Locale.US)
        if (key.isEmpty()) return@withContext false
        val url = artistImageFetcher.fetchArtistImageUrl(artistName)
        dao.upsertArtistCache(
            ArtistCacheEntity(
                artistKey = key,
                imageUrl = url,
                fetchedAt = System.currentTimeMillis(),
            )
        )
        !url.isNullOrBlank()
    }

    suspend fun recordRecentPlay(trackId: String) = withContext(Dispatchers.IO) {
        val entry = RecentPlayEntity(
            id = "rp-${System.currentTimeMillis()}-${trackId.takeLast(6)}",
            trackId = trackId,
            playedAt = System.currentTimeMillis(),
        )
        dao.insertRecentPlay(entry)
        dao.incrementPlayCount(trackId)
        dao.trimRecentPlays()
    }

    suspend fun savePlaylist(playlist: Playlist) = withContext(Dispatchers.IO) {
        dao.upsertPlaylist(PlaylistEntity.fromModel(playlist))
    }

    suspend fun deletePlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        dao.deletePlaylist(playlistId)
    }

    suspend fun seedDemoTrack(): Track = withContext(Dispatchers.IO) {
        val (demoTrack, artistPhotoUri) = DemoTrackProvider.ensureDemoTrack(context)
        dao.upsertTracks(listOf(TrackEntity.fromModel(demoTrack)))
        dao.upsertArtistCache(
            ArtistCacheEntity(
                artistKey = demoTrack.artist.trim().lowercase(Locale.US),
                imageUrl = artistPhotoUri,
                fetchedAt = System.currentTimeMillis(),
            )
        )
        demoTrack
    }

    suspend fun getLyricsForTrack(
        track: Track,
        forceRefresh: Boolean = false,
    ): TrackLyrics? = withContext(Dispatchers.IO) {
        if (!forceRefresh) {
            val cached = dao.getLyricsCache(track.id)
            if (cached != null) {
                val parsedSynced = LrcParser.parse(cached.syncedLrc)
                val hasContent = parsedSynced.isNotEmpty() || !cached.plainLyrics.isNullOrBlank() || cached.isInstrumental
                if (hasContent) {
                    return@withContext TrackLyrics(
                        trackId = track.id,
                        syncedLines = parsedSynced,
                        plainLyrics = cached.plainLyrics,
                        isInstrumental = cached.isInstrumental,
                        source = cached.source,
                    )
                }
                // Respect negative cache for 12 hours to avoid repeated network hits on offline/obscure tracks
                val ageMs = System.currentTimeMillis() - cached.fetchedAt
                if (ageMs < NEGATIVE_LYRICS_CACHE_TTL_MS) {
                    return@withContext null
                }
            }
        }

        val fetched = lyricsFetcher.fetchForTrack(track)
        if (fetched != null) {
            dao.upsertLyricsCache(
                LyricsCacheEntity(
                    trackId = track.id,
                    syncedLrc = fetched.syncedLrc,
                    plainLyrics = fetched.plainLyrics,
                    isInstrumental = fetched.isInstrumental,
                    source = fetched.source,
                    fetchedAt = System.currentTimeMillis(),
                )
            )
            return@withContext TrackLyrics(
                trackId = track.id,
                syncedLines = LrcParser.parse(fetched.syncedLrc),
                plainLyrics = fetched.plainLyrics,
                isInstrumental = fetched.isInstrumental,
                source = fetched.source,
            )
        }

        // Cache negative result so rapid track skipping does not spam LRCLIB
        dao.upsertLyricsCache(
            LyricsCacheEntity(
                trackId = track.id,
                syncedLrc = null,
                plainLyrics = null,
                isInstrumental = false,
                source = "",
                fetchedAt = System.currentTimeMillis(),
            )
        )
        null
    }

    suspend fun importLocalLyrics(trackId: String, uri: Uri): TrackLyrics? = withContext(Dispatchers.IO) {
        val result = lyricsFetcher.parseLocalLrcUri(context, uri, trackId) ?: return@withContext null
        val (raw, model) = result
        dao.upsertLyricsCache(
            LyricsCacheEntity(
                trackId = trackId,
                syncedLrc = raw.syncedLrc,
                plainLyrics = raw.plainLyrics,
                isInstrumental = raw.isInstrumental,
                source = raw.source,
                fetchedAt = System.currentTimeMillis(),
            )
        )
        model
    }

    suspend fun clearAllLibraryData() = withContext(Dispatchers.IO) {
        dao.clearTracks()
        dao.clearPlaylists()
        dao.clearRecentPlays()
        dao.clearLyricsCache()
        runCatching {
            File(context.filesDir, "album_art").deleteRecursively()
        }
    }

    companion object {
        private const val NEGATIVE_LYRICS_CACHE_TTL_MS = 12L * 60L * 60L * 1000L
    }
}
