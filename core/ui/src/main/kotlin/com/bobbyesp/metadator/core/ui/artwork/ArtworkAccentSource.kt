/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.ui.artwork

import android.content.Context
import android.graphics.BitmapFactory
import androidx.core.net.toUri
import androidx.palette.graphics.Palette
import com.bobbyesp.metadator.core.common.AppDispatchers
import kotlinx.coroutines.withContext

/**
 * The color a cover gives the screen that shows it, as an ARGB int for `MetadatorAccentTheme`.
 * Keeps `Context` and bitmaps out of ViewModels.
 */
interface ArtworkAccentSource {
    /** From a picture's bytes, as embedded in a file. Null when they are not a picture. */
    suspend fun fromBytes(bytes: ByteArray): Int?

    /** From a picture behind a `content://` URI, as the library's covers are. */
    suspend fun fromUri(uri: String): Int?
}

class AndroidArtworkAccentSource(
    private val context: Context,
    private val dispatchers: AppDispatchers,
) : ArtworkAccentSource {

    override suspend fun fromBytes(bytes: ByteArray): Int? =
        withContext(dispatchers.default) { runCatching { accentOf(bytes) }.getOrNull() }

    override suspend fun fromUri(uri: String): Int? {
        val bytes =
            withContext(dispatchers.io) {
                runCatching {
                    context.contentResolver.openInputStream(uri.toUri())?.use { it.readBytes() }
                }
                    .getOrNull()
            }
        return bytes?.let { fromBytes(it) }
    }

    private fun accentOf(bytes: ByteArray): Int? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0) return null
        // A small copy is plenty to find a color.
        val sample =
            BitmapFactory.decodeByteArray(
                bytes,
                0,
                bytes.size,
                BitmapFactory.Options().apply {
                    inSampleSize =
                        (maxOf(bounds.outWidth, bounds.outHeight) / SAMPLE_EDGE_PX).coerceAtLeast(1)
                },
            ) ?: return null
        val palette = Palette.from(sample).generate()
        sample.recycle()
        return (palette.vibrantSwatch ?: palette.dominantSwatch)?.rgb
    }

    private companion object {
        const val SAMPLE_EDGE_PX = 128
    }
}
