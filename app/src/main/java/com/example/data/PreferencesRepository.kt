package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AccentColorOption
import com.example.model.AcrylicDensity
import com.example.model.AppLanguage
import com.example.model.AppThemeMode
import com.example.model.EqualizerGains
import com.example.model.EqualizerPresetName
import com.example.model.RepeatMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray

private val Context.groovePrefsDataStore by preferencesDataStore(name = "fluent_groove_prefs")

data class UserPrefs(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val accentColor: AccentColorOption = AccentColorOption.BLUE,
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val acrylicDensity: AcrylicDensity = AcrylicDensity.STANDARD,
    val eqPreset: EqualizerPresetName = EqualizerPresetName.FLAT,
    val eqGains: EqualizerGains = EqualizerGains(),
    val showTitleBar: Boolean = true,
    val volume: Float = 0.78f,
    val isShuffled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.ALL,
    val demoSeeded: Boolean = false,
)

data class SavedPlaybackState(
    val queueTrackIds: List<String> = emptyList(),
    val currentIndex: Int = 0,
    val positionMs: Long = 0L,
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val ACRYLIC_DENSITY = stringPreferencesKey("acrylic_density")
        val EQ_PRESET = stringPreferencesKey("eq_preset")
        val EQ_LOW = intPreferencesKey("eq_low")
        val EQ_MID = intPreferencesKey("eq_mid")
        val EQ_HIGH = intPreferencesKey("eq_high")
        val SHOW_TITLE_BAR = booleanPreferencesKey("show_title_bar")
        val VOLUME = floatPreferencesKey("volume")
        val SHUFFLED = booleanPreferencesKey("shuffled")
        val REPEAT_MODE = stringPreferencesKey("repeat_mode")
        val DEMO_SEEDED = booleanPreferencesKey("demo_seeded")
        // Saved playback resume state
        val SAVED_QUEUE_IDS = stringPreferencesKey("saved_queue_ids")
        val SAVED_QUEUE_INDEX = intPreferencesKey("saved_queue_index")
        val SAVED_POSITION_MS = longPreferencesKey("saved_position_ms")
    }

    val prefsFlow: Flow<UserPrefs> = context.groovePrefsDataStore.data.map { prefs ->
        val themeMode = prefs[Keys.THEME_MODE]
            ?.let { runCatching { AppThemeMode.valueOf(it) }.getOrNull() }
            ?: AppThemeMode.SYSTEM
        val accentColor = prefs[Keys.ACCENT_COLOR]
            ?.let { runCatching { AccentColorOption.valueOf(it) }.getOrNull() }
            ?: AccentColorOption.BLUE
        val appLanguage = prefs[Keys.APP_LANGUAGE]
            ?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() }
            ?: AppLanguage.SYSTEM
        val density = prefs[Keys.ACRYLIC_DENSITY]
            ?.let { runCatching { AcrylicDensity.valueOf(it) }.getOrNull() }
            ?: AcrylicDensity.STANDARD
        val preset = prefs[Keys.EQ_PRESET]
            ?.let { runCatching { EqualizerPresetName.valueOf(it) }.getOrNull() }
            ?: EqualizerPresetName.FLAT
        val repeat = prefs[Keys.REPEAT_MODE]
            ?.let { runCatching { RepeatMode.valueOf(it) }.getOrNull() }
            ?: RepeatMode.ALL
        UserPrefs(
            themeMode = themeMode,
            accentColor = accentColor,
            appLanguage = appLanguage,
            acrylicDensity = density,
            eqPreset = preset,
            eqGains = EqualizerGains(
                low = prefs[Keys.EQ_LOW] ?: preset.gains.low,
                mid = prefs[Keys.EQ_MID] ?: preset.gains.mid,
                high = prefs[Keys.EQ_HIGH] ?: preset.gains.high,
            ),
            showTitleBar = prefs[Keys.SHOW_TITLE_BAR] ?: true,
            volume = prefs[Keys.VOLUME] ?: 0.78f,
            isShuffled = prefs[Keys.SHUFFLED] ?: false,
            repeatMode = repeat,
            demoSeeded = prefs[Keys.DEMO_SEEDED] ?: false,
        )
    }.distinctUntilChanged()

    suspend fun getSavedPlaybackState(): SavedPlaybackState {
        val prefs = context.groovePrefsDataStore.data.first()
        val rawIds = prefs[Keys.SAVED_QUEUE_IDS].orEmpty()
        val ids = if (rawIds.isBlank()) {
            emptyList()
        } else {
            runCatching {
                val arr = JSONArray(rawIds)
                buildList(arr.length()) {
                    for (i in 0 until arr.length()) {
                        val id = arr.optString(i)
                        if (id.isNotBlank()) add(id)
                    }
                }
            }.getOrDefault(emptyList())
        }
        return SavedPlaybackState(
            queueTrackIds = ids,
            currentIndex = (prefs[Keys.SAVED_QUEUE_INDEX] ?: 0).coerceAtLeast(0),
            positionMs = (prefs[Keys.SAVED_POSITION_MS] ?: 0L).coerceAtLeast(0L),
        )
    }

    suspend fun savePlaybackState(
        queueTrackIds: List<String>,
        currentIndex: Int,
        positionMs: Long,
    ) {
        val arr = JSONArray()
        queueTrackIds.forEach { arr.put(it) }
        context.groovePrefsDataStore.edit {
            it[Keys.SAVED_QUEUE_IDS] = arr.toString()
            it[Keys.SAVED_QUEUE_INDEX] = currentIndex.coerceAtLeast(0)
            it[Keys.SAVED_POSITION_MS] = positionMs.coerceAtLeast(0L)
        }
    }

    suspend fun clearSavedPlaybackState() {
        context.groovePrefsDataStore.edit {
            it.remove(Keys.SAVED_QUEUE_IDS)
            it.remove(Keys.SAVED_QUEUE_INDEX)
            it.remove(Keys.SAVED_POSITION_MS)
        }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.groovePrefsDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setAccentColor(accent: AccentColorOption) {
        context.groovePrefsDataStore.edit { it[Keys.ACCENT_COLOR] = accent.name }
    }

    suspend fun setAppLanguage(language: AppLanguage) {
        context.groovePrefsDataStore.edit { it[Keys.APP_LANGUAGE] = language.name }
    }

    suspend fun setDemoSeeded(seeded: Boolean) {
        context.groovePrefsDataStore.edit { it[Keys.DEMO_SEEDED] = seeded }
    }

    suspend fun setAcrylicDensity(density: AcrylicDensity) {
        context.groovePrefsDataStore.edit { it[Keys.ACRYLIC_DENSITY] = density.name }
    }

    suspend fun setEqualizer(preset: EqualizerPresetName, gains: EqualizerGains) {
        context.groovePrefsDataStore.edit {
            it[Keys.EQ_PRESET] = preset.name
            it[Keys.EQ_LOW] = gains.low
            it[Keys.EQ_MID] = gains.mid
            it[Keys.EQ_HIGH] = gains.high
        }
    }

    suspend fun setShowTitleBar(show: Boolean) {
        context.groovePrefsDataStore.edit { it[Keys.SHOW_TITLE_BAR] = show }
    }

    suspend fun setVolume(volume: Float) {
        context.groovePrefsDataStore.edit { it[Keys.VOLUME] = volume.coerceIn(0f, 1f) }
    }

    suspend fun setShuffled(shuffled: Boolean) {
        context.groovePrefsDataStore.edit { it[Keys.SHUFFLED] = shuffled }
    }

    suspend fun setRepeatMode(mode: RepeatMode) {
        context.groovePrefsDataStore.edit { it[Keys.REPEAT_MODE] = mode.name }
    }

    suspend fun resetAll() {
        context.groovePrefsDataStore.edit {
            val savedTheme = it[Keys.THEME_MODE]
            val savedAccent = it[Keys.ACCENT_COLOR]
            val savedLanguage = it[Keys.APP_LANGUAGE]
            it.clear()
            it[Keys.DEMO_SEEDED] = true
            if (savedTheme != null) it[Keys.THEME_MODE] = savedTheme
            if (savedAccent != null) it[Keys.ACCENT_COLOR] = savedAccent
            if (savedLanguage != null) it[Keys.APP_LANGUAGE] = savedLanguage
        }
    }
}
