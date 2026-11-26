package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Slim minimalist Groove Music slider shared across the main Now Playing player,
 * fullscreen synced lyrics view, Mini Player, PlayerBar, and Equalizer controls:
 * hairline track, crisp active fill, and clean circular thumb.
 */
@Composable
fun GrooveSeekBar(
    progress: Float,
    enabled: Boolean,
    isDragging: Boolean,
    onScrubStartOrMove: (Float) -> Unit,
    onScrubEnd: (Float) -> Unit,
    modifier: Modifier = Modifier,
    seekBarTestTag: String = "np_seek_bar",
    activeTrackColor: Color = Color.White.copy(alpha = 0.90f),
    inactiveTrackColor: Color = Color.White.copy(alpha = 0.34f),
    thumbColor: Color = Color.White,
    barHeight: Dp = 36.dp,
) {
    var widthPx by remember { mutableFloatStateOf(1f) }
    var lastFraction by remember { mutableFloatStateOf(progress) }
    val currentOnScrubStartOrMove by rememberUpdatedState(onScrubStartOrMove)
    val currentOnScrubEnd by rememberUpdatedState(onScrubEnd)
    val density = LocalDensity.current
    val thumbInsetPx = with(density) { 8.5.dp.toPx() }

    fun fractionForX(x: Float): Float {
        val usableWidth = (widthPx - thumbInsetPx * 2f).coerceAtLeast(1f)
        return ((x - thumbInsetPx) / usableWidth).coerceIn(0f, 1f)
    }

    Box(
        modifier = modifier
            .height(barHeight)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(enabled, thumbInsetPx) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    val frac = fractionForX(offset.x)
                    lastFraction = frac
                    currentOnScrubEnd(frac)
                }
            }
            .pointerInput(enabled, thumbInsetPx) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        val frac = fractionForX(offset.x)
                        lastFraction = frac
                        currentOnScrubStartOrMove(frac)
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val frac = fractionForX(change.position.x)
                        lastFraction = frac
                        currentOnScrubStartOrMove(frac)
                    },
                    onDragEnd = {
                        currentOnScrubEnd(lastFraction)
                    },
                    onDragCancel = {
                        currentOnScrubEnd(lastFraction)
                    },
                )
            }
            .testTag(seekBarTestTag),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val clamped = progress.coerceIn(0f, 1f)
            val thumbRadius = if (isDragging) 10.5.dp.toPx() else 8.5.dp.toPx()
            val trackHeight = 2.dp.toPx()
            val centerY = size.height / 2f
            val startX = thumbRadius
            val endX = (size.width - thumbRadius).coerceAtLeast(startX)
            val thumbX = startX + (endX - startX) * clamped

            // Inactive track
            drawLine(
                color = inactiveTrackColor,
                start = Offset(startX, centerY),
                end = Offset(endX, centerY),
                strokeWidth = trackHeight,
                cap = StrokeCap.Round,
            )

            // Active track
            if (thumbX > startX) {
                drawLine(
                    color = activeTrackColor,
                    start = Offset(startX, centerY),
                    end = Offset(thumbX, centerY),
                    strokeWidth = trackHeight,
                    cap = StrokeCap.Round,
                )
            }

            // Circular thumb
            if (enabled) {
                drawCircle(
                    color = thumbColor,
                    radius = thumbRadius,
                    center = Offset(thumbX, centerY),
                )
            }
        }
    }
}
