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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Slim minimalist Groove Music seekbar shared across the main Now Playing player,
 * fullscreen synced lyrics view, and Mini Player: hairline translucent track,
 * crisp white active fill, and pure white circular thumb.
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
) {
    var widthPx by remember { mutableFloatStateOf(1f) }
    var lastFraction by remember { mutableFloatStateOf(progress) }

    Box(
        modifier = modifier
            .height(36.dp)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    val frac = (offset.x / widthPx).coerceIn(0f, 1f)
                    lastFraction = frac
                    onScrubEnd(frac)
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        val frac = (offset.x / widthPx).coerceIn(0f, 1f)
                        lastFraction = frac
                        onScrubStartOrMove(frac)
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val frac = (change.position.x / widthPx).coerceIn(0f, 1f)
                        lastFraction = frac
                        onScrubStartOrMove(frac)
                    },
                    onDragEnd = {
                        onScrubEnd(lastFraction)
                    },
                    onDragCancel = {
                        onScrubEnd(lastFraction)
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
                color = Color.White.copy(alpha = 0.34f),
                start = Offset(startX, centerY),
                end = Offset(endX, centerY),
                strokeWidth = trackHeight,
                cap = StrokeCap.Round,
            )

            // Active track
            if (thumbX > startX) {
                drawLine(
                    color = Color.White.copy(alpha = 0.90f),
                    start = Offset(startX, centerY),
                    end = Offset(thumbX, centerY),
                    strokeWidth = trackHeight,
                    cap = StrokeCap.Round,
                )
            }

            // Pure white circular thumb
            if (enabled) {
                drawCircle(
                    color = Color.White,
                    radius = thumbRadius,
                    center = Offset(thumbX, centerY),
                )
            }
        }
    }
}
