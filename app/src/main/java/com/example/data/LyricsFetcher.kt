package com.example.data

import android.content.Context
import android.net.Uri
import com.example.model.LrcParser
import com.example.model.Track
import com.example.model.TrackLyrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.math.abs

data class RawLyricsResult(
    val syncedLrc: String?,
    val plainLyrics: String?,
    val isInstrumental: Boolean,
    val source: String,
)

class LyricsFetcher {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val titleCleanupRegex = Regex(
        pattern = """(?i)\s*[\(\[](?:official\s*(?:music\s*)?video|official\s*audio|lyric\s*video|lyrics|remastered(?:\s*\d{4})?|radio\s*edit|hd|hq|explicit|clean)[\)\]]""",
    )

    suspend fun fetchForTrack(track: Track): RawLyricsResult? = withContext(Dispatchers.IO) {
        // Built-in synced lyrics for the synthesized demo track
        if (track.title.equals(DemoTrackProvider.DEMO_TRACK_TITLE, ignoreCase = true) &&
            track.artist.equals(DemoTrackProvider.DEMO_ARTIST_NAME, ignoreCase = true)
        ) {
            return@withContext RawLyricsResult(
                syncedLrc = DEMO_TRACK_LRC,
                plainLyrics = null,
                isInstrumental = false,
                source = "Demo LRC",
            )
        }

        val primaryArtist = PrimaryArtistResolver.extractPrimaryArtist(track.artist)
            .takeIf { !it.equals("Unknown Artist", ignoreCase = true) && !it.equals("Local Audio", ignoreCase = true) }
            ?: ""
        val rawTitle = track.title.trim()
        val cleanTitle = cleanTrackTitle(rawTitle)
        val album = track.album.trim()
            .takeIf { !it.equals("Unknown Album", ignoreCase = true) && !it.equals("Single", ignoreCase = true) }
            .orEmpty()

        if (cleanTitle.isEmpty()) return@withContext null

        // 1. Try exact LRCLIB /api/get match first (highest sync accuracy)
        if (primaryArtist.isNotEmpty()) {
            val exact = fetchExactFromLrcLib(
                artist = primaryArtist,
                title = cleanTitle,
                album = album,
                durationSec = track.duration,
            )
            if (exact != null && !exact.syncedLrc.isNullOrBlank()) {
                return@withContext exact
            }

            // 2. Fallback to LRCLIB /api/search for best synced match across releases/editions
            val searched = searchLrcLib(
                artist = primaryArtist,
                title = cleanTitle,
                targetDurationSec = track.duration,
            )
            if (searched != null && !searched.syncedLrc.isNullOrBlank()) {
                return@withContext searched
            }

            // 3. Return plain lyrics or instrumental result if available from either call
            if (exact != null && (!exact.plainLyrics.isNullOrBlank() || exact.isInstrumental)) {
                return@withContext exact
            }
            if (searched != null) {
                return@withContext searched
            }
        }

        // 4. Last resort: general search with combined query
        searchLrcLibByQuery(
            query = "$primaryArtist $cleanTitle".trim(),
            targetDurationSec = track.duration,
        )
    }

    suspend fun parseLocalLrcUri(context: Context, uri: Uri, trackId: String): Pair<RawLyricsResult, TrackLyrics>? =
        withContext(Dispatchers.IO) {
            runCatching {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?.trim()
                    .orEmpty()
                if (text.isEmpty()) return@runCatching null

                val syncedLines = LrcParser.parse(text)
                val raw = if (syncedLines.isNotEmpty()) {
                    RawLyricsResult(
                        syncedLrc = text,
                        plainLyrics = null,
                        isInstrumental = false,
                        source = "Local .lrc",
                    )
                } else {
                    RawLyricsResult(
                        syncedLrc = null,
                        plainLyrics = text,
                        isInstrumental = false,
                        source = "Local file",
                    )
                }
                val model = TrackLyrics(
                    trackId = trackId,
                    syncedLines = syncedLines,
                    plainLyrics = raw.plainLyrics,
                    isInstrumental = false,
                    source = raw.source,
                )
                raw to model
            }.getOrNull()
        }

    private fun fetchExactFromLrcLib(
        artist: String,
        title: String,
        album: String,
        durationSec: Int,
    ): RawLyricsResult? {
        return runCatching {
            val params = buildList {
                add("artist_name=${urlEncode(artist)}")
                add("track_name=${urlEncode(title)}")
                if (album.isNotEmpty()) add("album_name=${urlEncode(album)}")
                if (durationSec > 0) add("duration=$durationSec")
            }.joinToString("&")

            val request = Request.Builder()
                .url("https://lrclib.net/api/get?$params")
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                parseLrcLibObject(JSONObject(body))
            }
        }.getOrNull()
    }

    private fun searchLrcLib(
        artist: String,
        title: String,
        targetDurationSec: Int,
    ): RawLyricsResult? {
        return runCatching {
            val url = "https://lrclib.net/api/search?artist_name=${urlEncode(artist)}&track_name=${urlEncode(title)}"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                pickBestCandidate(JSONArray(body), targetDurationSec)
            }
        }.getOrNull()
    }

    private fun searchLrcLibByQuery(
        query: String,
        targetDurationSec: Int,
    ): RawLyricsResult? {
        if (query.isBlank()) return null
        return runCatching {
            val url = "https://lrclib.net/api/search?q=${urlEncode(query)}"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                pickBestCandidate(JSONArray(body), targetDurationSec)
            }
        }.getOrNull()
    }

    private fun pickBestCandidate(array: JSONArray, targetDurationSec: Int): RawLyricsResult? {
        if (array.length() == 0) return null

        var bestSynced: Pair<Int, RawLyricsResult>? = null
        var bestPlain: Pair<Int, RawLyricsResult>? = null
        var instrumentalFallback: RawLyricsResult? = null

        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val candidate = parseLrcLibObject(obj) ?: continue
            val itemDur = obj.optDouble("duration", -1.0).toInt()
            val diff = if (targetDurationSec > 0 && itemDur > 0) abs(itemDur - targetDurationSec) else 999

            when {
                !candidate.syncedLrc.isNullOrBlank() -> {
                    if (bestSynced == null || diff < bestSynced.first) {
                        bestSynced = diff to candidate
                    }
                }
                !candidate.plainLyrics.isNullOrBlank() -> {
                    if (bestPlain == null || diff < bestPlain.first) {
                        bestPlain = diff to candidate
                    }
                }
                candidate.isInstrumental && instrumentalFallback == null -> {
                    instrumentalFallback = candidate
                }
            }
        }

        return bestSynced?.second ?: bestPlain?.second ?: instrumentalFallback
    }

    private fun parseLrcLibObject(json: JSONObject): RawLyricsResult? {
        val synced = json.optString("syncedLyrics").takeIf { it.isNotBlank() && it != "null" }
        val plain = json.optString("plainLyrics").takeIf { it.isNotBlank() && it != "null" }
        val instrumental = json.optBoolean("instrumental", false)
        if (synced == null && plain == null && !instrumental) return null
        return RawLyricsResult(
            syncedLrc = synced,
            plainLyrics = plain,
            isInstrumental = instrumental,
            source = "LRCLIB",
        )
    }

    private fun cleanTrackTitle(title: String): String {
        var cleaned = title
            .replace(titleCleanupRegex, "")
            .replace(Regex("""^\d{1,3}\s*[-._]\s*"""), "")
            .trim()
        if (cleaned.isEmpty()) cleaned = title.trim()
        return cleaned
    }

    private fun urlEncode(value: String): String = URLEncoder.encode(value, "UTF-8")

    companion object {
        private const val USER_AGENT = "FluentGroove/1.0 (Android Music Player)"

        private val DEMO_TRACK_LRC = """
            [00:00.20]Midnight settles softly over neon glass
            [00:03.50]Chasing distant echoes as the highways pass
            [00:07.00]Silver synthesizer rising in the night
            [00:10.50]Every quiet heartbeat turns to electric light
            [00:14.00]Hold onto the rhythm floating in the air
            [00:17.50]Shadows fade away beyond the skyline glare
            [00:21.00]Drifting past the stars until the morning gleams
            [00:24.50]On the midnight horizon of our neon dreams
        """.trimIndent()
    }
}
