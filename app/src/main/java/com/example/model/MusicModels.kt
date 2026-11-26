package com.example.model

enum class AppThemeMode(val label: String) {
    SYSTEM("Auto"),
    LIGHT("Light"),
    DARK("Dark"),
}

enum class AppLanguage {
    SYSTEM,
    ENGLISH,
}

enum class NowPlayingArtworkStyle {
    ARTIST_FULLSCREEN,
    ALBUM_ART_NORMAL,
}

enum class AccentColorOption(
    val label: String,
    val primaryHex: Long,
    val primaryDeepHex: Long,
    val accentOnDarkHex: Long,
    val lightAcrylicLightHex: Long,
    val lightAcrylicMidHex: Long,
    val lightAcrylicDeepHex: Long,
    val darkAcrylicLightHex: Long,
    val darkAcrylicMidHex: Long,
    val darkAcrylicDeepHex: Long,
) {
    BLUE(
        label = "Groove Blue",
        primaryHex = 0xFF2B78C5,
        primaryDeepHex = 0xFF005A9E,
        accentOnDarkHex = 0xFF7EC4DC,
        lightAcrylicLightHex = 0xFFBFE7F4,
        lightAcrylicMidHex = 0xFF7EC4DC,
        lightAcrylicDeepHex = 0xFF56A8C7,
        darkAcrylicLightHex = 0xFF1F2D3B,
        darkAcrylicMidHex = 0xFF18222E,
        darkAcrylicDeepHex = 0xFF131A24,
    ),
    ORANGE(
        label = "Groove Orange",
        primaryHex = 0xFFD65522,
        primaryDeepHex = 0xFFA33B12,
        accentOnDarkHex = 0xFFFF9A6C,
        lightAcrylicLightHex = 0xFFFAD8C7,
        lightAcrylicMidHex = 0xFFF0AD8C,
        lightAcrylicDeepHex = 0xFFE08960,
        darkAcrylicLightHex = 0xFF36221A,
        darkAcrylicMidHex = 0xFF291B16,
        darkAcrylicDeepHex = 0xFF1E1512,
    ),
    PURPLE(
        label = "Iris Purple",
        primaryHex = 0xFF8A5FD1,
        primaryDeepHex = 0xFF5E36A3,
        accentOnDarkHex = 0xFFC5A6FF,
        lightAcrylicLightHex = 0xFFE2D6F7,
        lightAcrylicMidHex = 0xFFC0A6EB,
        lightAcrylicDeepHex = 0xFFA080DB,
        darkAcrylicLightHex = 0xFF2B213B,
        darkAcrylicMidHex = 0xFF211A2E,
        darkAcrylicDeepHex = 0xFF181322,
    ),
    MAGENTA(
        label = "Orchid Pink",
        primaryHex = 0xFFC93BA6,
        primaryDeepHex = 0xFF942078,
        accentOnDarkHex = 0xFFF38CD9,
        lightAcrylicLightHex = 0xFFF5D0EC,
        lightAcrylicMidHex = 0xFFE49CD3,
        lightAcrylicDeepHex = 0xFFD172BB,
        darkAcrylicLightHex = 0xFF361E30,
        darkAcrylicMidHex = 0xFF2A1826,
        darkAcrylicDeepHex = 0xFF1F131D,
    ),
    TEAL(
        label = "Seafoam Teal",
        primaryHex = 0xFF008575,
        primaryDeepHex = 0xFF005C51,
        accentOnDarkHex = 0xFF5ED4C5,
        lightAcrylicLightHex = 0xFFC2ECE6,
        lightAcrylicMidHex = 0xFF84D4C9,
        lightAcrylicDeepHex = 0xFF56B8AB,
        darkAcrylicLightHex = 0xFF172E2C,
        darkAcrylicMidHex = 0xFF142423,
        darkAcrylicDeepHex = 0xFF101B1B,
    ),
    EMERALD(
        label = "Emerald",
        primaryHex = 0xFF168449,
        primaryDeepHex = 0xFF0D5E32,
        accentOnDarkHex = 0xFF6BD99B,
        lightAcrylicLightHex = 0xFFC7EBD6,
        lightAcrylicMidHex = 0xFF8AD4AA,
        lightAcrylicDeepHex = 0xFF5BB883,
        darkAcrylicLightHex = 0xFF182E22,
        darkAcrylicMidHex = 0xFF14241C,
        darkAcrylicDeepHex = 0xFF101C16,
    ),
    CRIMSON(
        label = "Crimson",
        primaryHex = 0xFFD13852,
        primaryDeepHex = 0xFF9B1E34,
        accentOnDarkHex = 0xFFFF889C,
        lightAcrylicLightHex = 0xFFF8CFD6,
        lightAcrylicMidHex = 0xFFEC9AA8,
        lightAcrylicDeepHex = 0xFFDC7284,
        darkAcrylicLightHex = 0xFF381D23,
        darkAcrylicMidHex = 0xFF2B171C,
        darkAcrylicDeepHex = 0xFF201216,
    ),
    AMBER(
        label = "Golden Amber",
        primaryHex = 0xFFBD7000,
        primaryDeepHex = 0xFF8A5000,
        accentOnDarkHex = 0xFFF7BC5C,
        lightAcrylicLightHex = 0xFFF7E2BC,
        lightAcrylicMidHex = 0xFFEBC37F,
        lightAcrylicDeepHex = 0xFFDCA64E,
        darkAcrylicLightHex = 0xFF362815,
        darkAcrylicMidHex = 0xFF291F12,
        darkAcrylicDeepHex = 0xFF1E170F,
    ),
}

enum class AcrylicDensity(val label: String, val alpha: Float) {
    SUBTLE("Subtle", 0.56f),
    STANDARD("Standard", 0.74f),
    DENSE("Dense", 0.88f),
}

enum class RepeatMode {
    OFF,
    ALL,
    ONE,
}

enum class MyMusicTab(val label: String) {
    HOME("Home"),
    SONGS("Songs"),
    ARTISTS("Artists"),
    ALBUMS("Albums"),
    PLAYLISTS("Playlists"),
}

enum class SongSortOrder(val label: String) {
    DATE_ADDED("Date added"),
    TITLE("A to Z"),
    ARTIST("Artist"),
    ALBUM("Album"),
}

data class EqualizerGains(
    val low: Int = 0,
    val mid: Int = 0,
    val high: Int = 0,
)

enum class EqualizerPresetName(val displayName: String, val gains: EqualizerGains) {
    FLAT("Flat", EqualizerGains(0, 0, 0)),
    WARM("Warm", EqualizerGains(3, 1, -2)),
    BASS_BOOST("Bass Boost", EqualizerGains(7, 0, 1)),
    VOCAL("Vocal", EqualizerGains(-2, 5, 3)),
    ELECTRONIC("Electronic", EqualizerGains(5, -1, 4)),
    CUSTOM("Custom", EqualizerGains(0, 0, 0)),
}

data class ArtDesign(
    val colorStart: Long,
    val colorEnd: Long,
    val accent: Long,
    val pattern: Int,
)

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String,
    val album: String,
    val albumId: String,
    val trackNumber: Int,
    val duration: Int,
    val genre: String,
    val year: Int,
    val addedAt: Long,
    val fileName: String,
    val contentUri: String,
    val coverUri: String?,
    val artDesign: ArtDesign,
    val playCount: Int = 0,
)

data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String,
    val year: Int,
    val genre: String,
    val trackCount: Int,
    val totalDuration: Int,
    val coverUri: String?,
    val artDesign: ArtDesign,
)

data class Artist(
    val id: String,
    val name: String,
    val albumCount: Int,
    val trackCount: Int,
    val imageUrl: String?,
    val fallbackCoverUri: String?,
    val artDesign: ArtDesign,
)

data class Playlist(
    val id: String,
    val name: String,
    val description: String,
    val trackIds: List<String>,
    val createdAt: Long,
)

data class RecentPlayEntry(
    val id: String,
    val trackId: String,
    val playedAt: Long,
)

sealed interface ScreenRoute {
    data class MyMusic(val tab: MyMusicTab = MyMusicTab.HOME) : ScreenRoute
    data class AlbumDetail(val albumId: String) : ScreenRoute
    data class ArtistDetail(val artistId: String) : ScreenRoute
    data object RecentPlays : ScreenRoute
    data object NowPlaying : ScreenRoute
    data class PlaylistDetail(val playlistId: String) : ScreenRoute
    data object Settings : ScreenRoute
}

fun formatDuration(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    val hrs = safe / 3600
    val mins = (safe % 3600) / 60
    val secs = safe % 60
    return if (hrs > 0) {
        "%d:%02d:%02d".format(hrs, mins, secs)
    } else {
        "%d:%02d".format(mins, secs)
    }
}

fun formatRelativeTime(timestampMs: Long, nowMs: Long = System.currentTimeMillis()): String {
    val diffSec = ((nowMs - timestampMs) / 1000L).coerceAtLeast(0L)
    return when {
        diffSec < 60 -> "Just now"
        diffSec < 3600 -> "${diffSec / 60}m ago"
        diffSec < 86400 -> "${diffSec / 3600}h ago"
        diffSec < 604800 -> "${diffSec / 86400}d ago"
        else -> "${diffSec / 604800}w ago"
    }
}
