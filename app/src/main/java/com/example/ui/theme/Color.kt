package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.model.AccentColorOption

@Immutable
data class GrooveColorPalette(
    val isDark: Boolean,
    val primary: Color,
    val primaryDeep: Color,
    val primaryDark: Color,
    val primaryHover: Color,
    val accentOnDark: Color,
    val acrylicBase: Color,
    val acrylicSkyLight: Color,
    val acrylicSkyMid: Color,
    val acrylicSkyDeep: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val surfaceHover: Color,
    val border: Color,
    val borderStrong: Color,
    val text: Color,
    val textMuted: Color,
    val textSubtle: Color,
    val toastBg: Color,
    val playingTint: Color,
    val danger: Color,
    val darkBackdrop: Color,
    val darkSurface: Color,
)

fun buildGrooveColorPalette(
    darkTheme: Boolean,
    accent: AccentColorOption,
): GrooveColorPalette {
    val primary = Color(accent.primaryHex)
    val primaryDeep = Color(accent.primaryDeepHex)
    val accentOnDark = Color(accent.accentOnDarkHex)

    return if (darkTheme) {
        GrooveColorPalette(
            isDark = true,
            primary = primary,
            primaryDeep = accentOnDark,
            primaryDark = primaryDeep,
            primaryHover = primary.copy(alpha = 0.88f),
            accentOnDark = accentOnDark,
            acrylicBase = Color(accent.darkAcrylicMidHex),
            acrylicSkyLight = Color(accent.darkAcrylicLightHex),
            acrylicSkyMid = Color(accent.darkAcrylicMidHex),
            acrylicSkyDeep = Color(accent.darkAcrylicDeepHex),
            surface = Color(0xFF121316),
            surfaceAlt = Color(0xFF1B1D22),
            surfaceHover = Color(0xFF252830),
            border = Color(0x1FFFFFFF), // rgba(255, 255, 255, 0.12)
            borderStrong = Color(0x33FFFFFF),
            text = Color(0xFFF3F5F8),
            textMuted = Color(0xFFA6ADB8),
            textSubtle = Color(0xFF7D8590),
            toastBg = Color(0xFF252830),
            playingTint = primary.copy(alpha = 0.22f),
            danger = Color(0xFFE5534B),
            darkBackdrop = Color(0xFF0E1014),
            darkSurface = Color(0xFF191C22),
        )
    } else {
        GrooveColorPalette(
            isDark = false,
            primary = primary,
            primaryDeep = primaryDeep,
            primaryDark = primaryDeep,
            primaryHover = primary.copy(alpha = 0.88f),
            accentOnDark = accentOnDark,
            acrylicBase = Color(accent.lightAcrylicMidHex),
            acrylicSkyLight = Color(accent.lightAcrylicLightHex),
            acrylicSkyMid = Color(accent.lightAcrylicMidHex),
            acrylicSkyDeep = Color(accent.lightAcrylicDeepHex),
            surface = Color(0xFFFFFFFF),
            surfaceAlt = Color(0xFFF2F2F2),
            surfaceHover = Color(0xFFE9E9E9),
            border = Color(0x14000000), // rgba(0, 0, 0, 0.08)
            borderStrong = Color(0x24000000),
            text = Color(0xFF111111),
            textMuted = Color(0xFF666666),
            textSubtle = Color(0xFF888888),
            toastBg = Color(0xFF1C1B1B),
            playingTint = primary.copy(alpha = 0.09f),
            danger = Color(0xFFC42B1C),
            darkBackdrop = Color(0xFF14181F),
            darkSurface = Color(0xFF1E242D),
        )
    }
}

val LocalGrooveColors = staticCompositionLocalOf {
    buildGrooveColorPalette(darkTheme = false, accent = AccentColorOption.BLUE)
}

// Dynamic Compose color tokens backed by LocalGrooveColors so all screens react automatically
val FgIsDark: Boolean
    @Composable @ReadOnlyComposable get() = LocalGrooveColors.current.isDark

val FgPrimary: Color
    @Composable @ReadOnlyComposable get() = LocalGrooveColors.current.primary

val FgPrimaryDeep: Color
    @Composable @ReadOnlyComposable get() = LocalGrooveColors.current.primaryDeep

val FgPrimaryDark: Color
    @Composable @ReadOnlyComposable get() = LocalGrooveColors.current.primaryDark

val FgPrimaryHover: Color
    @Composable @ReadOnlyComposable get() = LocalGrooveColors.current.primaryHover

val FgAccentOnDark: Color
    @Composable @ReadOnlyComposable get() = LocalGrooveColors.current.accentOnDark

val FgAcrylicBase: Color
    @Composable @ReadOnlyComposable get() = LocalGrooveColors.current.acrylicBase

val FgAcrylicSkyLight: Color
    @Composable @ReadOnlyComposable get() = LocalGrooveColors.current.acrylicSkyLight

val FgAcrylicSkyMid: Color
    @Composable @ReadOnlyComposable get() = LocalGrooveColors.current.acrylicSkyMid

val FgAcrylicSkyDeep: Color
    @Composable @ReadOnlyComposable get() = LocalGrooveColors.current.acrylicSkyDeep

val FgSurface: Color
    @Composable @ReadOnlyComposable get() = LocalGrooveColors.current.surface

val FgSurfaceAlt: Color
    @Composable @ReadOnlyComposable get() = LocalGrooveColors.current.surfaceAlt

val FgSurfaceHover: Color
    @Composable @ReadOnlyComposable get() = LocalGrooveColors.current.surfaceHover

val FgBorder: Color
    @Composable @ReadOnlyComposable get() = LocalGrooveColors.current.border

val FgBorderStrong: Color
    @Composable @ReadOnlyComposable get() = LocalGrooveColors.current.borderStrong

val FgText: Color
    @Composable @ReadOnlyComposable get() = LocalGrooveColors.current.text

val FgTextMuted: Color
    @Composable @ReadOnlyComposable get() = LocalGrooveColors.current.textMuted

val FgTextSubtle: Color
    @Composable @ReadOnlyComposable get() = LocalGrooveColors.current.textSubtle

val FgToastBg: Color
    @Composable @ReadOnlyComposable get() = LocalGrooveColors.current.toastBg

val FgPlayingTint: Color
    @Composable @ReadOnlyComposable get() = LocalGrooveColors.current.playingTint

val FgDanger: Color
    @Composable @ReadOnlyComposable get() = LocalGrooveColors.current.danger

val FgDarkBackdrop: Color
    @Composable @ReadOnlyComposable get() = LocalGrooveColors.current.darkBackdrop

val FgDarkSurface: Color
    @Composable @ReadOnlyComposable get() = LocalGrooveColors.current.darkSurface
