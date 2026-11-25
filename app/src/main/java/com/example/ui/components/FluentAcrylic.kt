package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.model.AcrylicDensity
import com.example.ui.theme.LocalGrooveColors

/**
 * Windows 10 Fluent Acrylic material with atmospheric accent-tinted light/dark field
 * and subtle frosted noise grain matching `.fluent-acrylic`.
 */
@Composable
fun Modifier.fluentAcrylic(
    density: AcrylicDensity,
    verticalGradient: Boolean = true,
): Modifier {
    val palette = LocalGrooveColors.current
    val skyLight = palette.acrylicSkyLight
    val skyMid = palette.acrylicSkyMid
    val skyDeep = palette.acrylicSkyDeep
    val baseColor = palette.acrylicBase
    val isDark = palette.isDark

    return this.drawBehind {
        val skyBrush = if (verticalGradient) {
            Brush.linearGradient(
                colors = listOf(skyLight, skyMid, skyDeep),
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height),
            )
        } else {
            Brush.horizontalGradient(
                colors = listOf(skyLight, skyMid, skyDeep),
            )
        }
        drawRect(brush = skyBrush)

        // Translucent frosted acrylic tint layer calibrated by density and light/dark mode
        if (isDark) {
            drawRect(color = Color.White.copy(alpha = (1f - density.alpha) * 0.10f))
            drawRect(color = baseColor.copy(alpha = density.alpha * 0.55f))
        } else {
            drawRect(color = Color.White.copy(alpha = (1f - density.alpha) * 0.55f))
            drawRect(color = baseColor.copy(alpha = density.alpha * 0.62f))
        }

        // Subtle procedural Fluent micro-grain sheen
        val step = 18f
        var y = 6f
        var row = 0
        while (y < size.height) {
            var x = if (row % 2 == 0) 6f else 15f
            while (x < size.width) {
                val light = ((x.toInt() * 31 + y.toInt() * 17) and 1) == 0
                drawCircle(
                    color = if (isDark) {
                        if (light) Color.White.copy(alpha = 0.035f) else Color.Black.copy(alpha = 0.06f)
                    } else {
                        if (light) Color.White.copy(alpha = 0.045f) else Color.Black.copy(alpha = 0.025f)
                    },
                    radius = 0.9f,
                    center = Offset(x, y),
                )
                x += step
            }
            y += step
            row++
        }
    }
}
