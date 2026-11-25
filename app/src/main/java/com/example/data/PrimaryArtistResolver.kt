package com.example.data

import com.example.model.Track
import java.util.Locale

/**
 * Normalizes multi-artist / featuring tags (e.g. "Muse/Ellie Goulding",
 * "Muse feat. Ellie Goulding", "Muse; Ellie Goulding") to their primary artist
 * so collaborations stay grouped under the main artist in the Artists and Albums tabs
 * while preserving the full credited artist string on each song.
 */
object PrimaryArtistResolver {

    private val PRESERVED_SLASH_NAMES = setOf(
        "ac/dc",
        "m/a/r/r/s",
        "k/da",
    )

    private val EXPLICIT_FEAT_REGEX = Regex(
        pattern = """(?i)\s*(?:[\(\[\{]\s*)?(?:feat\.?|ft\.?|featuring|part\.?|participa[çc][ãa]o|with)\b.*$"""
    )

    private val AMBIGUOUS_COLLAB_REGEX = Regex(
        pattern = """(?i)\s*(?:,|&|\+|(?:\s+x\s+)|(?:\s+and\s+)|(?:\s+vs\.?\s+)).*$"""
    )

    fun extractPrimaryArtist(rawArtist: String, knownBaseArtists: Set<String> = emptySet()): String {
        var current = rawArtist.trim()
        if (current.isEmpty()) return "Unknown Artist"

        val lowered = current.lowercase(Locale.US)
        if (lowered in PRESERVED_SLASH_NAMES) return current

        // 1. Strip explicit featuring / guest markers (feat., ft., featuring, part., with)
        current = EXPLICIT_FEAT_REGEX.replace(current, "").trim()

        // 2. Split on semicolon (standard FLAC/Vorbis/MP4 multi-artist separator)
        if (current.contains(';')) {
            current = current.substringBefore(';').trim()
        }

        // 3. Split on slash (standard ID3v2.3 multi-artist separator like "Muse/Ellie Goulding")
        if (current.contains('/') && current.lowercase(Locale.US) !in PRESERVED_SLASH_NAMES) {
            val parts = current.split('/').map { it.trim() }.filter { it.isNotEmpty() }
            val looksLikeShortAcronymBand = parts.size == 2 && parts[0].length <= 2 && parts[1].length <= 2
            if (!looksLikeShortAcronymBand && parts.isNotEmpty()) {
                current = parts.first()
            }
        }

        // 4. Handle ambiguous delimiters (",", "&", "+", " x ", " and ", " vs ")
        // Only split when the primary artist already exists standalone in the library,
        // so bands like "Simon & Garfunkel" or "Tyler, The Creator" stay intact.
        if (knownBaseArtists.isNotEmpty()) {
            val candidate = AMBIGUOUS_COLLAB_REGEX.replace(current, "").trim()
            if (candidate.isNotEmpty() &&
                !candidate.equals(current, ignoreCase = true) &&
                candidate.lowercase(Locale.US) in knownBaseArtists
            ) {
                current = candidate
            }
        }

        return current.ifEmpty { rawArtist.trim().ifEmpty { "Unknown Artist" } }
    }

    /**
     * Canonicalizes artistId and albumId across a track list so featured/collaborative
     * tracks unify under their primary artist while keeping each track's full credited
     * `artist` text and `id` unchanged.
     */
    fun normalizeTrackGrouping(tracks: List<Track>): List<Track> {
        if (tracks.isEmpty()) return tracks

        // Pass 1: collect base primary artist names after explicit delimiter splitting
        val baseArtistNames = mutableMapOf<String, String>() // lower -> preferred casing
        for (track in tracks) {
            val base = extractPrimaryArtist(track.artist)
            val key = base.lowercase(Locale.US)
            if (key !in baseArtistNames) {
                baseArtistNames[key] = base
            }
        }
        val knownBaseKeys = baseArtistNames.keys

        // Pass 2: resolve every track with library-aware matching and unify artistId & albumId
        return tracks.map { track ->
            val primaryRaw = extractPrimaryArtist(track.artist, knownBaseKeys)
            val canonicalName = baseArtistNames[primaryRaw.lowercase(Locale.US)] ?: primaryRaw
            val canonicalArtistId = AudioMetadataParser.makeSlugId("art", canonicalName)
            val canonicalAlbumId = AudioMetadataParser.makeSlugId("alb", "$canonicalName-${track.album}")
            if (track.artistId == canonicalArtistId && track.albumId == canonicalAlbumId) {
                track
            } else {
                track.copy(
                    artistId = canonicalArtistId,
                    albumId = canonicalAlbumId,
                )
            }
        }
    }

    fun resolveDisplayNameForGroup(artistTracks: List<Track>, knownBaseKeys: Set<String>): String {
        val primaryNames = artistTracks.map { extractPrimaryArtist(it.artist, knownBaseKeys) }
        // Prefer the cleanest shortest casing (e.g., "Muse" over "MUSE")
        return primaryNames
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
            ?: primaryNames.firstOrNull()
            ?: "Unknown Artist"
    }
}
