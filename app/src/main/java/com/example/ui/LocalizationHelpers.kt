package com.example.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.model.AccentColorOption
import com.example.model.AcrylicDensity
import com.example.model.AppLanguage
import com.example.model.AppThemeMode
import com.example.model.EqualizerPresetName
import com.example.model.MyMusicTab
import com.example.model.NowPlayingArtworkStyle
import com.example.model.SongSortOrder

@StringRes
fun AppThemeMode.labelRes(): Int = when (this) {
    AppThemeMode.SYSTEM -> R.string.settings_theme_auto
    AppThemeMode.LIGHT -> R.string.settings_theme_light
    AppThemeMode.DARK -> R.string.settings_theme_dark
}

@StringRes
fun AppLanguage.labelRes(): Int = when (this) {
    AppLanguage.SYSTEM -> R.string.settings_language_system
    AppLanguage.ENGLISH -> R.string.settings_language_english
}

@StringRes
fun NowPlayingArtworkStyle.labelRes(): Int = when (this) {
    NowPlayingArtworkStyle.ARTIST_FULLSCREEN -> R.string.settings_player_art_artist_fullscreen
    NowPlayingArtworkStyle.ALBUM_ART_NORMAL -> R.string.settings_player_art_album_normal
}

@StringRes
fun AccentColorOption.labelRes(): Int = when (this) {
    AccentColorOption.BLUE -> R.string.accent_blue
    AccentColorOption.ORANGE -> R.string.accent_orange
    AccentColorOption.PURPLE -> R.string.accent_purple
    AccentColorOption.MAGENTA -> R.string.accent_magenta
    AccentColorOption.TEAL -> R.string.accent_teal
    AccentColorOption.EMERALD -> R.string.accent_emerald
    AccentColorOption.CRIMSON -> R.string.accent_crimson
    AccentColorOption.AMBER -> R.string.accent_amber
}

@StringRes
fun AcrylicDensity.labelRes(): Int = when (this) {
    AcrylicDensity.SUBTLE -> R.string.acrylic_subtle
    AcrylicDensity.STANDARD -> R.string.acrylic_standard
    AcrylicDensity.DENSE -> R.string.acrylic_dense
}

@StringRes
fun MyMusicTab.labelRes(): Int = when (this) {
    MyMusicTab.HOME -> R.string.tab_home
    MyMusicTab.SONGS -> R.string.tab_songs
    MyMusicTab.ARTISTS -> R.string.tab_artists
    MyMusicTab.ALBUMS -> R.string.tab_albums
}

@StringRes
fun SongSortOrder.labelRes(): Int = when (this) {
    SongSortOrder.DATE_ADDED -> R.string.sort_date_added
    SongSortOrder.TITLE -> R.string.sort_title
    SongSortOrder.ARTIST -> R.string.sort_artist
    SongSortOrder.ALBUM -> R.string.sort_album
}

@StringRes
fun EqualizerPresetName.labelRes(): Int = when (this) {
    EqualizerPresetName.FLAT -> R.string.eq_flat
    EqualizerPresetName.WARM -> R.string.eq_warm
    EqualizerPresetName.BASS_BOOST -> R.string.eq_bass_boost
    EqualizerPresetName.VOCAL -> R.string.eq_vocal
    EqualizerPresetName.ELECTRONIC -> R.string.eq_electronic
    EqualizerPresetName.CUSTOM -> R.string.eq_custom
}

@Composable
fun songCountLabel(count: Int): String = pluralStringResource(R.plurals.song_count, count, count)

@Composable
fun albumCountLabel(count: Int): String = pluralStringResource(R.plurals.album_count, count, count)

@Composable
fun artistCountLabel(count: Int): String = pluralStringResource(R.plurals.artist_count, count, count)

@Composable
fun playCountLabel(count: Int): String = pluralStringResource(R.plurals.play_count, count, count)

@Composable
fun formatLocalizedRelativeTime(
    timestampMs: Long,
    nowMs: Long = System.currentTimeMillis(),
): String {
    val diffSec = ((nowMs - timestampMs) / 1000L).coerceAtLeast(0L)
    return when {
        diffSec < 60 -> stringResource(R.string.time_just_now)
        diffSec < 3600 -> stringResource(R.string.time_minutes_ago, (diffSec / 60).toInt())
        diffSec < 86400 -> stringResource(R.string.time_hours_ago, (diffSec / 3600).toInt())
        diffSec < 604800 -> stringResource(R.string.time_days_ago, (diffSec / 86400).toInt())
        else -> stringResource(R.string.time_weeks_ago, (diffSec / 604800).toInt())
    }
}
