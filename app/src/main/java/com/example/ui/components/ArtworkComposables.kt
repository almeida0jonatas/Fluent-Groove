package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.ArtDesign
import com.example.model.Track
import com.example.ui.theme.FgPrimary
import com.example.ui.theme.FgSurfaceAlt
import com.example.ui.theme.WorkSansFontFamily

@Composable
fun AlbumArtwork(
    coverUri: String?,
    artDesign: ArtDesign?,
    modifier: Modifier = Modifier,
    contentDescription: String = stringResource(R.string.cd_album_art),
) {
    Box(
        modifier = modifier
            .clip(RectangleShape)
            .background(FgSurfaceAlt),
        contentAlignment = Alignment.Center,
    ) {
        if (artDesign != null) {
            ProceduralFluentArtwork(
                artDesign = artDesign,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = Color(0xFF999999),
                modifier = Modifier.size(28.dp),
            )
        }

        if (!coverUri.isNullOrBlank()) {
            AsyncImage(
                model = coverUri,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ProceduralFluentArtwork(
    artDesign: ArtDesign,
    modifier: Modifier = Modifier,
) {
    val start = Color(artDesign.colorStart)
    val end = Color(artDesign.colorEnd)
    val accent = Color(artDesign.accent)

    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(start, end),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            )
        )

        when (artDesign.pattern % 4) {
            0 -> {
                // Concentric vinyl rings
                val center = Offset(size.width * 0.5f, size.height * 0.5f)
                drawCircle(
                    color = accent.copy(alpha = 0.24f),
                    radius = size.minDimension * 0.36f,
                    center = center,
                    style = Stroke(width = size.minDimension * 0.045f),
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.18f),
                    radius = size.minDimension * 0.16f,
                    center = center,
                )
                drawCircle(
                    color = start,
                    radius = size.minDimension * 0.055f,
                    center = center,
                )
            }
            1 -> {
                // Bold Fluent diagonal architectural bars
                drawRect(
                    color = accent.copy(alpha = 0.22f),
                    topLeft = Offset(0f, size.height * 0.62f),
                    size = Size(size.width, size.height * 0.38f),
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.20f),
                    radius = size.minDimension * 0.24f,
                    center = Offset(size.width * 0.72f, size.height * 0.32f),
                )
            }
            2 -> {
                // Soundwave columns
                val barCount = 5
                val barWidth = size.width / (barCount * 2.2f)
                val heights = listOf(0.32f, 0.62f, 0.45f, 0.75f, 0.38f)
                heights.forEachIndexed { idx, h ->
                    val x = size.width * 0.18f + idx * (barWidth * 1.65f)
                    val barH = size.height * h
                    drawRect(
                        color = if (idx % 2 == 0) Color.White.copy(alpha = 0.24f) else accent.copy(alpha = 0.32f),
                        topLeft = Offset(x, (size.height - barH) / 2f),
                        size = Size(barWidth, barH),
                    )
                }
            }
            else -> {
                // Crisp overlapping squares
                drawRect(
                    color = accent.copy(alpha = 0.28f),
                    topLeft = Offset(size.width * 0.15f, size.height * 0.15f),
                    size = Size(size.width * 0.48f, size.height * 0.48f),
                )
                drawRect(
                    color = Color.White.copy(alpha = 0.16f),
                    topLeft = Offset(size.width * 0.38f, size.height * 0.38f),
                    size = Size(size.width * 0.47f, size.height * 0.47f),
                )
            }
        }
    }
}

@Composable
fun ArtistAvatar(
    name: String,
    imageUrl: String?,
    fallbackCoverUri: String?,
    artDesign: ArtDesign,
    modifier: Modifier = Modifier,
) {
    val initials = name.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifEmpty { "?" }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(artDesign.colorStart), Color(artDesign.colorEnd))
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontFamily = WorkSansFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
        )

        val resolvedModel = imageUrl?.takeIf { it.isNotBlank() } ?: fallbackCoverUri?.takeIf { it.isNotBlank() }
        if (resolvedModel != null) {
            AsyncImage(
                model = resolvedModel,
                contentDescription = stringResource(R.string.cd_artist_photo),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun PlaylistMosaicArtwork(
    tracks: List<Track>,
    modifier: Modifier = Modifier,
) {
    val uniqueAlbums = tracks.distinctBy { it.albumId }.take(4)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RectangleShape)
            .background(FgSurfaceAlt),
        contentAlignment = Alignment.Center,
    ) {
        when {
            uniqueAlbums.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF2B78C5), Color(0xFF56A8C7))
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.QueueMusic,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
            uniqueAlbums.size < 4 -> {
                val first = uniqueAlbums.first()
                AlbumArtwork(
                    coverUri = first.coverUri,
                    artDesign = first.artDesign,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.weight(1f)) {
                        AlbumArtwork(
                            coverUri = uniqueAlbums[0].coverUri,
                            artDesign = uniqueAlbums[0].artDesign,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                        AlbumArtwork(
                            coverUri = uniqueAlbums[1].coverUri,
                            artDesign = uniqueAlbums[1].artDesign,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                    Row(modifier = Modifier.weight(1f)) {
                        AlbumArtwork(
                            coverUri = uniqueAlbums[2].coverUri,
                            artDesign = uniqueAlbums[2].artDesign,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                        AlbumArtwork(
                            coverUri = uniqueAlbums[3].coverUri,
                            artDesign = uniqueAlbums[3].artDesign,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Animated 3-bar equalizer matching `.animate-eq-1/2/3` in index.css.
 */
@Composable
fun PlayingEqualizerIndicator(
    isPlaying: Boolean,
    color: Color = FgPrimary,
    barWidth: Dp = 2.5.dp,
    maxHeight: Dp = 14.dp,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "eq")
    val bar1 by transition.animateFloat(
        initialValue = 0.30f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 360, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "eq1",
    )
    val bar2 by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 290, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "eq2",
    )
    val bar3 by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 420, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "eq3",
    )

    val h1 = if (isPlaying) bar1 else 0.35f
    val h2 = if (isPlaying) bar2 else 0.65f
    val h3 = if (isPlaying) bar3 else 0.45f

    Row(
        modifier = modifier.height(maxHeight),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Box(
            modifier = Modifier
                .width(barWidth)
                .fillMaxHeight(h1)
                .background(color),
        )
        Box(
            modifier = Modifier
                .width(barWidth)
                .fillMaxHeight(h2)
                .background(color),
        )
        Box(
            modifier = Modifier
                .width(barWidth)
                .fillMaxHeight(h3)
                .background(color),
        )
    }
}
