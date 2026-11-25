package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.model.AccentColorOption

// Crisp Windows 10 Fluent sharp geometry (rounded-none aesthetic)
private val FluentShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(2.dp),
    large = RoundedCornerShape(2.dp),
    extraLarge = RoundedCornerShape(2.dp),
)

@Composable
fun FluentGrooveTheme(
    darkTheme: Boolean = false,
    accentColor: AccentColorOption = AccentColorOption.BLUE,
    content: @Composable () -> Unit,
) {
    val palette = remember(darkTheme, accentColor) {
        buildGrooveColorPalette(darkTheme = darkTheme, accent = accentColor)
    }

    val colorScheme = remember(palette) {
        if (palette.isDark) {
            darkColorScheme(
                primary = palette.primary,
                onPrimary = Color.White,
                primaryContainer = palette.primaryDark,
                onPrimaryContainer = Color.White,
                secondary = palette.accentOnDark,
                onSecondary = Color(0xFF0E1014),
                secondaryContainer = palette.acrylicSkyLight,
                onSecondaryContainer = palette.text,
                tertiary = palette.accentOnDark,
                onTertiary = Color(0xFF0E1014),
                background = palette.surface,
                onBackground = palette.text,
                surface = palette.surface,
                onSurface = palette.text,
                surfaceVariant = palette.surfaceAlt,
                onSurfaceVariant = palette.textMuted,
                outline = palette.borderStrong,
                outlineVariant = palette.border,
                error = palette.danger,
                onError = Color.White,
            )
        } else {
            lightColorScheme(
                primary = palette.primary,
                onPrimary = Color.White,
                primaryContainer = palette.acrylicSkyLight,
                onPrimaryContainer = palette.primaryDeep,
                secondary = palette.acrylicBase,
                onSecondary = palette.text,
                secondaryContainer = palette.acrylicSkyLight,
                onSecondaryContainer = palette.text,
                tertiary = palette.primaryDeep,
                onTertiary = Color.White,
                background = palette.surface,
                onBackground = palette.text,
                surface = palette.surface,
                onSurface = palette.text,
                surfaceVariant = palette.surfaceAlt,
                onSurfaceVariant = palette.textMuted,
                outline = palette.borderStrong,
                outlineVariant = palette.border,
                error = palette.danger,
                onError = Color.White,
            )
        }
    }

    CompositionLocalProvider(LocalGrooveColors provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = FluentTypography,
            shapes = FluentShapes,
            content = content,
        )
    }
}
