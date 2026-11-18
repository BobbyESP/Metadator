/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.ui.component

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A cover: an embedded picture's bytes, a `content://` URI or a URL. While there is none, or it
 * fails, a note on a tonal surface takes its place, the same size, so lists never jump.
 *
 * @param cacheKey a key shared by the places that show this same picture at different sizes. One
 *   that appears while another is on screen (a shared element) then starts from the picture already
 *   loaded instead of from nothing.
 */
@Composable
fun ArtworkImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    placeholderIconFraction: Float = 0.45f,
    cacheKey: String? = null,
) {
    var failed by remember(model) { mutableStateOf(false) }
    val context = LocalContext.current
    val request =
        remember(context, model, cacheKey) {
            ImageRequest.Builder(context)
                .data(model)
                .crossfade(true)
                .memoryCacheKey(cacheKey)
                .placeholderMemoryCacheKey(cacheKey)
                .build()
        }
    Box(
        modifier =
            modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        if (model == null || failed) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxSize(placeholderIconFraction),
            )
        }
        if (model != null && !failed) {
            AsyncImage(
                model = request,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                onError = { failed = true },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * A cover reduced to a few colors: each pixel of the result is the average of a patch of the cover,
 * so stretched over a screen the scaling alone turns it into a soft wash of the cover's colors,
 * with no blur. `null` until it is made, and for a cover that cannot be read.
 *
 * Reduced here, by halving, and not by asking the decoder for a tiny size: a decoder skips pixels
 * to get there rather than averaging them, and a cover with fine detail then comes out as noise.
 */
@Composable
fun rememberArtworkWash(model: Any?, cacheKey: String? = null): ImageBitmap? {
    val context = LocalContext.current
    val wash by
        produceState<ImageBitmap?>(initialValue = null, model, cacheKey) {
            value =
                if (model == null) null
                else withContext(Dispatchers.Default) { loadWash(context, model, cacheKey) }
        }
    return wash
}

private suspend fun loadWash(context: Context, model: Any, cacheKey: String?): ImageBitmap? {
    val request =
        ImageRequest.Builder(context)
            .data(model)
            .size(WASH_SOURCE_SIDE)
            .allowHardware(false)
            .apply { cacheKey?.let { memoryCacheKey("$it#wash") } }
            .build()
    val image = (context.imageLoader.execute(request) as? SuccessResult)?.image ?: return null
    var bitmap = image.toBitmap()
    // Halving with a bilinear filter averages each 2x2 patch exactly; a bigger step would skip.
    while (maxOf(bitmap.width, bitmap.height) > WASH_SIDE) {
        bitmap =
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width / 2).coerceAtLeast(1),
                (bitmap.height / 2).coerceAtLeast(1),
                true,
            )
    }
    // And back up by doubling, each time with a bilinear filter: every doubling smooths the
    // creases that stretching so few pixels in one go would leave, visible as diamonds.
    while (maxOf(bitmap.width, bitmap.height) < WASH_SMOOTH_SIDE) {
        bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.width * 2, bitmap.height * 2, true)
    }
    return bitmap.asImageBitmap()
}

/** What the cover is decoded at before it is averaged down: enough that skipped pixels are few. */
private const val WASH_SOURCE_SIDE = 128

/**
 * The longest side of the wash before it is smoothed, in pixels: how much of the cover's shape is
 * kept.
 */
private const val WASH_SIDE = 16

/** The longest side of the wash once smoothed. */
private const val WASH_SMOOTH_SIDE = 128
