package com.example.model

data class LyricLine(
    val timeMs: Long,
    val text: String,
)

data class TrackLyrics(
    val trackId: String,
    val syncedLines: List<LyricLine>,
    val plainLyrics: String?,
    val isInstrumental: Boolean = false,
    val source: String = "LRCLIB",
) {
    val hasSyncedLyrics: Boolean
        get() = syncedLines.isNotEmpty()

    val hasAnyLyrics: Boolean
        get() = hasSyncedLyrics || !plainLyrics.isNullOrBlank() || isInstrumental

    fun activeLineIndexAt(positionMs: Long): Int {
        if (syncedLines.isEmpty()) return -1
        var low = 0
        var high = syncedLines.lastIndex
        var best = -1
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (syncedLines[mid].timeMs <= positionMs) {
                best = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return best
    }
}

sealed interface LyricsUiState {
    data object Idle : LyricsUiState
    data class Loading(val trackId: String) : LyricsUiState
    data class Loaded(val lyrics: TrackLyrics) : LyricsUiState
    data class NotFound(val trackId: String) : LyricsUiState
    data class Error(val trackId: String, val message: String) : LyricsUiState
}

object LrcParser {

    private val TIMESTAMP_REGEX = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")
    private val OFFSET_REGEX = Regex("""(?i)\[offset:\s*([+-]?\d+)\s*]""")

    fun parse(rawLrc: String?): List<LyricLine> {
        if (rawLrc.isNullOrBlank()) return emptyList()

        var offsetMs = 0L
        OFFSET_REGEX.find(rawLrc)?.groupValues?.getOrNull(1)?.toLongOrNull()?.let {
            offsetMs = it
        }

        val parsed = mutableListOf<LyricLine>()
        rawLrc.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEach

            val matches = TIMESTAMP_REGEX.findAll(line).toList()
            if (matches.isEmpty()) return@forEach

            val lyricText = TIMESTAMP_REGEX.replace(line, "").trim()
            for (match in matches) {
                val minutes = match.groupValues[1].toLongOrNull() ?: continue
                val seconds = match.groupValues[2].toLongOrNull() ?: continue
                val fracStr = match.groupValues.getOrNull(3).orEmpty()
                val millis = when (fracStr.length) {
                    0 -> 0L
                    1 -> (fracStr.toLongOrNull() ?: 0L) * 100L
                    2 -> (fracStr.toLongOrNull() ?: 0L) * 10L
                    else -> fracStr.take(3).toLongOrNull() ?: 0L
                }
                val timeMs = (minutes * 60_000L + seconds * 1_000L + millis + offsetMs).coerceAtLeast(0L)
                parsed.add(LyricLine(timeMs = timeMs, text = lyricText))
            }
        }

        if (parsed.isEmpty()) return emptyList()

        val sorted = parsed.sortedBy { it.timeMs }
        // Drop leading empty lines and consecutive empty spacer lines
        val cleaned = mutableListOf<LyricLine>()
        for (item in sorted) {
            if (item.text.isBlank()) {
                if (cleaned.isNotEmpty() && cleaned.last().text.isNotBlank()) {
                    cleaned.add(item.copy(text = "♪"))
                }
            } else {
                cleaned.add(item)
            }
        }
        if (cleaned.isNotEmpty() && cleaned.last().text == "♪") {
            cleaned.removeAt(cleaned.lastIndex)
        }
        return cleaned
    }
}
