package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AccentColorOption
import com.example.model.AcrylicDensity
import com.example.model.AppLanguage
import com.example.model.AppThemeMode
import com.example.model.EqualizerGains
import com.example.model.EqualizerPresetName
import com.example.ui.components.ConfirmDangerDialog
import com.example.ui.labelRes
import com.example.ui.theme.FgBorder
import com.example.ui.theme.FgDanger
import com.example.ui.theme.FgPrimary
import com.example.ui.theme.FgSurface
import com.example.ui.theme.FgSurfaceAlt
import com.example.ui.theme.FgText
import com.example.ui.theme.FgTextMuted
import com.example.ui.theme.TabularNumStyle
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    themeMode: AppThemeMode,
    accentColor: AccentColorOption,
    appLanguage: AppLanguage,
    acrylicDensity: AcrylicDensity,
    eqPreset: EqualizerPresetName,
    eqGains: EqualizerGains,
    showTitleBar: Boolean,
    trackCount: Int,
    albumCount: Int,
    playlistCount: Int,
    onChangeThemeMode: (AppThemeMode) -> Unit,
    onChangeAccentColor: (AccentColorOption) -> Unit,
    onChangeAppLanguage: (AppLanguage) -> Unit,
    onChangeAcrylicDensity: (AcrylicDensity) -> Unit,
    onSelectEqPreset: (EqualizerPresetName) -> Unit,
    onChangeEqGains: (EqualizerGains) -> Unit,
    onToggleTitleBar: () -> Unit,
    onImportFiles: () -> Unit,
    onImportFolder: () -> Unit,
    onLoadDemoTrack: () -> Unit,
    onResetLibrary: () -> Unit,
) {
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        ConfirmDangerDialog(
            title = stringResource(R.string.settings_clear_library_title),
            message = stringResource(R.string.settings_clear_library_message),
            confirmLabel = stringResource(R.string.action_clear_all),
            onDismiss = { showClearDialog = false },
            onConfirm = {
                showClearDialog = false
                onResetLibrary()
            },
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(FgSurface),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                Text(
                    text = stringResource(R.string.nav_settings),
                    style = MaterialTheme.typography.headlineLarge,
                    color = FgText,
                )
                Text(
                    text = stringResource(R.string.settings_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = FgTextMuted,
                )
            }
            HorizontalDivider(color = FgBorder)
        }

        // Section 1: Theme Mode & Accent Colors
        item {
            SettingsSection(title = stringResource(R.string.settings_section_theme)) {
                Text(
                    text = stringResource(R.string.settings_app_theme),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = FgText,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (themeMode) {
                        AppThemeMode.SYSTEM -> stringResource(R.string.settings_theme_desc_auto)
                        AppThemeMode.LIGHT -> stringResource(R.string.settings_theme_desc_light)
                        AppThemeMode.DARK -> stringResource(R.string.settings_theme_desc_dark)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = FgTextMuted,
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AppThemeMode.entries.forEach { mode ->
                        val selected = mode == themeMode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .background(if (selected) FgPrimary else FgSurfaceAlt)
                                .border(1.dp, if (selected) FgPrimary else FgBorder)
                                .clickable { onChangeThemeMode(mode) }
                                .testTag("theme_mode_${mode.name.lowercase()}"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(mode.labelRes()),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) Color.White else FgText,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.settings_accent_color),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = FgText,
                    )
                    Text(
                        text = stringResource(accentColor.labelRes()),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = FgPrimary,
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AccentColorOption.entries.forEach { option ->
                        val selected = option == accentColor
                        val swatchColor = Color(option.primaryHex)
                        val optionLabel = stringResource(option.labelRes())
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .then(
                                    if (selected) {
                                        Modifier
                                            .border(2.dp, FgText)
                                            .padding(3.dp)
                                    } else {
                                        Modifier.border(1.dp, FgBorder)
                                    }
                                )
                                .background(swatchColor)
                                .clickable { onChangeAccentColor(option) }
                                .semantics { contentDescription = optionLabel }
                                .testTag("accent_color_${option.name.lowercase()}"),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Language
        item {
            SettingsSection(title = stringResource(R.string.settings_section_language)) {
                Text(
                    text = stringResource(R.string.settings_language_label),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = FgText,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (appLanguage) {
                        AppLanguage.SYSTEM -> stringResource(R.string.settings_language_desc_system)
                        AppLanguage.ENGLISH -> stringResource(R.string.settings_language_desc_english)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = FgTextMuted,
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AppLanguage.entries.forEach { language ->
                        val selected = language == appLanguage
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .background(if (selected) FgPrimary else FgSurfaceAlt)
                                .border(1.dp, if (selected) FgPrimary else FgBorder)
                                .clickable { onChangeAppLanguage(language) }
                                .testTag("language_${language.name.lowercase()}"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(language.labelRes()),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) Color.White else FgText,
                            )
                        }
                    }
                }
            }
        }

        // Section 3: Fluent Acrylic Material & Chrome
        item {
            SettingsSection(title = stringResource(R.string.settings_section_acrylic)) {
                Text(
                    text = stringResource(R.string.settings_acrylic_density),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = FgText,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AcrylicDensity.entries.forEach { density ->
                        val selected = density == acrylicDensity
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .background(if (selected) FgPrimary else FgSurfaceAlt)
                                .border(1.dp, if (selected) FgPrimary else FgBorder)
                                .clickable { onChangeAcrylicDensity(density) }
                                .testTag("acrylic_${density.name.lowercase()}"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(density.labelRes()),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) Color.White else FgText,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_show_top_bar),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = FgText,
                        )
                        Text(
                            text = stringResource(R.string.settings_show_top_bar_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = FgTextMuted,
                        )
                    }
                    Switch(
                        checked = showTitleBar,
                        onCheckedChange = { onToggleTitleBar() },
                        colors = SwitchDefaults.colors(checkedTrackColor = FgPrimary),
                    )
                }
            }
        }

        // Section 4: Native 3-Band Hardware Equalizer
        item {
            SettingsSection(title = stringResource(R.string.settings_section_eq)) {
                Text(
                    text = stringResource(R.string.settings_eq_preset),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = FgText,
                )
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        EqualizerPresetName.FLAT,
                        EqualizerPresetName.WARM,
                        EqualizerPresetName.BASS_BOOST,
                        EqualizerPresetName.VOCAL,
                        EqualizerPresetName.ELECTRONIC,
                    ).forEach { preset ->
                        val selected = preset == eqPreset
                        Box(
                            modifier = Modifier
                                .background(if (selected) FgPrimary else FgSurfaceAlt)
                                .border(1.dp, if (selected) FgPrimary else FgBorder)
                                .clickable { onSelectEqPreset(preset) }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag("eq_preset_${preset.name.lowercase()}"),
                        ) {
                            Text(
                                text = stringResource(preset.labelRes()),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) Color.White else FgText,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                EqSliderRow(
                    label = stringResource(R.string.eq_band_low),
                    valueDb = eqGains.low,
                    onChange = { onChangeEqGains(eqGains.copy(low = it)) },
                )
                Spacer(modifier = Modifier.height(10.dp))
                EqSliderRow(
                    label = stringResource(R.string.eq_band_mid),
                    valueDb = eqGains.mid,
                    onChange = { onChangeEqGains(eqGains.copy(mid = it)) },
                )
                Spacer(modifier = Modifier.height(10.dp))
                EqSliderRow(
                    label = stringResource(R.string.eq_band_high),
                    valueDb = eqGains.high,
                    onChange = { onChangeEqGains(eqGains.copy(high = it)) },
                )
            }
        }

        // Section 5: Local Music Library Management
        item {
            SettingsSection(title = stringResource(R.string.settings_section_library)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    LibraryStatTile(label = stringResource(R.string.tab_songs), value = trackCount.toString(), modifier = Modifier.weight(1f))
                    LibraryStatTile(label = stringResource(R.string.tab_albums), value = albumCount.toString(), modifier = Modifier.weight(1f))
                    LibraryStatTile(label = stringResource(R.string.nav_playlists), value = playlistCount.toString(), modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(16.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = onImportFiles,
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = FgPrimary),
                        modifier = Modifier.testTag("settings_import_files_button"),
                    ) {
                        Icon(Icons.Filled.FileUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_import_files), color = Color.White)
                    }
                    OutlinedButton(
                        onClick = onImportFolder,
                        shape = RectangleShape,
                        modifier = Modifier.testTag("settings_import_folder_button"),
                    ) {
                        Icon(Icons.Filled.CreateNewFolder, contentDescription = null, tint = FgPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_import_folder_short), color = FgPrimary)
                    }
                    OutlinedButton(
                        onClick = onLoadDemoTrack,
                        shape = RectangleShape,
                        modifier = Modifier.testTag("settings_load_demo_button"),
                    ) {
                        Icon(Icons.Filled.MusicNote, contentDescription = null, tint = FgPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_add_demo), color = FgPrimary)
                    }
                    OutlinedButton(
                        onClick = { showClearDialog = true },
                        shape = RectangleShape,
                        modifier = Modifier.testTag("settings_clear_library_button"),
                    ) {
                        Icon(Icons.Filled.DeleteForever, contentDescription = null, tint = FgDanger, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_clear_library), color = FgDanger)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .widthIn(max = 680.dp)
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = FgPrimary,
        )
        Spacer(modifier = Modifier.height(14.dp))
        content()
    }
    HorizontalDivider(color = FgBorder)
}

@Composable
private fun EqSliderRow(
    label: String,
    valueDb: Int,
    onChange: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = FgText)
            Text(
                text = if (valueDb > 0) "+$valueDb dB" else "$valueDb dB",
                style = TabularNumStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                color = FgPrimary,
            )
        }
        Slider(
            value = valueDb.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = -12f..12f,
            steps = 23,
            colors = SliderDefaults.colors(
                thumbColor = FgPrimary,
                activeTrackColor = FgPrimary,
                inactiveTrackColor = FgBorder,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun LibraryStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(FgSurfaceAlt)
            .border(1.dp, FgBorder)
            .padding(12.dp),
    ) {
        Text(
            text = value,
            style = TabularNumStyle.copy(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
            color = FgText,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = FgTextMuted,
        )
    }
}
