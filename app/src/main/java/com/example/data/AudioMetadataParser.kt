package com.example.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.example.model.ArtDesign
import com.example.model.Track
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import kotlin.math.abs

object AudioMetadataParser {

    private val AUDIO_EXTENSIONS = setOf(
        "mp3", "flac", "m4a", "mp4", "wav", "ogg", "aac", "opus", "wma", "webm", "3gp", "mid"
    )

    private val PALETTES = listOf(
        Triple(0xFF1D4E89L, 0xFF00B2CAL, 0xFF7DCFB6L),
        Triple(0xFF2B78C5L, 0xFF56A8C7L, 0xFFBFE7F4L),
        Triple(0xFF0F2027L, 0xFF2C5364L, 0xFF7EC4DCL),
        Triple(0xFF3A1C71L, 0xFFD76D77L, 0xFFFFAF7BL),
        Triple(0xFF134E5EL, 0xFF71B280L, 0xFFD7F2BAL),
        Triple(0xFF42275AL, 0xFF734B6DL, 0xFFEAC8CAL),
        Triple(0xFF1F4037L, 0xFF99F2C8L, 0xFF56A8C7L),
        Triple(0xFF283C86L, 0xFF45A247L, 0xFFA8E6CFL),
        Triple(0xFF8E2DE2L, 0xFF4A00E0L, 0xFF7EC4DCL),
        Triple(0xFFCB356BL, 0xFFBD3F32L, 0xFFFFB88CL),
        Triple(0xFF0B486BL, 0xFFF56217L, 0xFFFFD194L),
        Triple(0xFF232526L, 0xFF414345L, 0xFF7EC4DCL),
    )

    fun isSupportedAudio(name: String?, mimeType: String?): Boolean {
        if (mimeType != null && mimeType.startsWith("audio/", ignoreCase = true)) return true
        val ext = name?.substringAfterLast('.', "")?.lowercase(Locale.US) ?: return false
        return ext in AUDIO_EXTENSIONS
    }

    fun makeSlugId(prefix: String, raw: String): String {
        val cleaned = raw.trim()
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifEmpty { "unknown" }
        return "$prefix-$cleaned"
    }

    fun createTrackIdFromTags(artist: String, album: String, title: String, trackNumber: Int): String {
        val raw = "${artist.trim().lowercase(Locale.US)}|${album.trim().lowercase(Locale.US)}|$trackNumber|${title.trim().lowercase(Locale.US)}"
        val digest = MessageDigest.getInstance("SHA-1").digest(raw.toByteArray())
        val hex = digest.take(8).joinToString("") { "%02x".format(it) }
        return "trk-$hex"
    }

    fun createArtDesignForKey(key: String): ArtDesign {
        val hash = abs(key.lowercase(Locale.US).hashCode())
        val palette = PALETTES[hash % PALETTES.size]
        return ArtDesign(
            colorStart = palette.first,
            colorEnd = palette.second,
            accent = palette.third,
            pattern = (hash / PALETTES.size) % 4,
        )
    }

    fun collectAudioUrisFromTree(context: Context, treeUri: Uri): List<Uri> {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
        val result = mutableListOf<Uri>()
        fun walk(dir: DocumentFile, depth: Int) {
            if (depth > 6) return
            for (child in dir.listFiles()) {
                if (child.isDirectory) {
                    walk(child, depth + 1)
                } else if (child.isFile && isSupportedAudio(child.name, child.type)) {
                    result.add(child.uri)
                }
            }
        }
        walk(root, 0)
        return result
    }

    fun parseUri(context: Context, uri: Uri, addedAt: Long): Track? {
        val displayName = resolveDisplayName(context, uri)
        val mimeType = runCatching { context.contentResolver.getType(uri) }.getOrNull()
        if (!isSupportedAudio(displayName, mimeType)) return null

        val retriever = MediaMetadataRetriever()
        var title: String? = null
        var artist: String? = null
        var album: String? = null
        var trackNum = 1
        var durationSec = 0
        var genre = "Music"
        var year = 0
        var embeddedArt: ByteArray? = null

        try {
            retriever.setDataSource(context, uri)
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.trim()
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.trim()
                ?.ifEmpty { null }
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)?.trim()
            album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.trim()

            val rawTrack = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
            trackNum = rawTrack?.substringBefore('/')?.trim()?.toIntOrNull()?.coerceAtLeast(1) ?: 1

            val rawDurationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            durationSec = if (rawDurationMs != null && rawDurationMs > 0L) {
                (rawDurationMs / 1000L).toInt().coerceAtLeast(1)
            } else {
                0
            }

            genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
                ?.trim()
                ?.ifEmpty { "Music" }
                ?: "Music"

            val rawYear = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
            year = Regex("(19|20)\\d{2}").find(rawYear ?: "")?.value?.toIntOrNull() ?: 0

            embeddedArt = retriever.embeddedPicture
        } catch (_: Exception) {
            // Fall back to filename heuristics if metadata cannot be extracted
        } finally {
            runCatching { retriever.release() }
        }

        val cleanBaseName = displayName
            .substringBeforeLast('.')
            .replace(Regex("^\\d{1,3}[\\s._-]+"), "")
            .trim()
            .ifEmpty { "Untitled Track" }

        // Parse "Artist - Title" filename fallback when ID3 tags are missing
        val inferredParts = if (title.isNullOrBlank() && artist.isNullOrBlank() && cleanBaseName.contains(" - ")) {
            val parts = cleanBaseName.split(" - ", limit = 2)
            parts[0].trim() to parts[1].trim()
        } else {
            null
        }

        val finalArtist = artist?.ifEmpty { null } ?: inferredParts?.first?.ifEmpty { null } ?: "Unknown Artist"
        val finalTitle = title?.ifEmpty { null } ?: inferredParts?.second?.ifEmpty { null } ?: cleanBaseName
        val finalAlbum = album?.ifEmpty { null } ?: "Unknown Album"

        val artistId = makeSlugId("art", finalArtist)
        val albumId = makeSlugId("alb", "$finalArtist-$finalAlbum")
        val trackId = createTrackIdFromTags(finalArtist, finalAlbum, finalTitle, trackNum)
        val artDesign = createArtDesignForKey("$finalArtist::$finalAlbum")

        val coverPath = embeddedArt?.let { bytes ->
            saveCoverArtToInternalStorage(context, albumId, bytes)
        }

        return Track(
            id = trackId,
            title = finalTitle,
            artist = finalArtist,
            artistId = artistId,
            album = finalAlbum,
            albumId = albumId,
            trackNumber = trackNum,
            duration = durationSec,
            genre = genre,
            year = year,
            addedAt = addedAt,
            fileName = displayName,
            contentUri = uri.toString(),
            coverUri = coverPath,
            artDesign = artDesign,
        )
    }

    private fun resolveDisplayName(context: Context, uri: Uri): String {
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) {
                            val name = cursor.getString(idx)
                            if (!name.isNullOrBlank()) return name
                        }
                    }
                }
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "audio_track.mp3"
    }

    private fun saveCoverArtToInternalStorage(context: Context, albumId: String, bytes: ByteArray): String? {
        return runCatching {
            val dir = File(context.filesDir, "album_art")
            if (!dir.exists()) dir.mkdirs()
            val outFile = File(dir, "$albumId.jpg")
            if (!outFile.exists() || outFile.length() == 0L) {
                outFile.writeBytes(bytes)
            }
            Uri.fromFile(outFile).toString()
        }.getOrNull()
    }
}
