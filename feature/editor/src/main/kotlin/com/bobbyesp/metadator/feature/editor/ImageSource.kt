package com.bobbyesp.metadator.feature.editor

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.palette.graphics.Palette
import com.bobbyesp.metadator.core.common.AppDispatchers
import kotlinx.coroutines.withContext

/** Facts about a picture, for the editor to show and warn about. */
data class ImageInfo(val width: Int, val height: Int, val sizeBytes: Int, val dominantColor: Int?) {
    val isSmall: Boolean
        get() = minOf(width, height) in 1 until SMALL_EDGE_PX

    companion object {
        /** Below this, covers look blurry on a phone's lock screen. */
        const val SMALL_EDGE_PX = 500
    }
}

class PickedImage(val bytes: ByteArray, val mimeType: String)

/** Reads pictures the user picks, and measures any picture. Keeps `Context` out of ViewModels. */
interface ImageSource {
    suspend fun read(uri: String): PickedImage?

    suspend fun inspect(bytes: ByteArray): ImageInfo?
}

class AndroidImageSource(
    private val context: Context,
    private val dispatchers: AppDispatchers,
) : ImageSource {

    override suspend fun read(uri: String): PickedImage? =
        withContext(dispatchers.io) {
            runCatching {
                    val parsed = Uri.parse(uri)
                    val resolver = context.contentResolver
                    val bytes = resolver.openInputStream(parsed)?.use { it.readBytes() } ?: return@runCatching null
                    if (bytes.size > MAX_BYTES) return@runCatching null
                    val mime = resolver.getType(parsed) ?: sniff(bytes) ?: return@runCatching null
                    PickedImage(bytes, mime)
                }
                .getOrNull()
        }

    override suspend fun inspect(bytes: ByteArray): ImageInfo? =
        withContext(dispatchers.default) {
            runCatching {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                    if (bounds.outWidth <= 0) return@runCatching null
                    // A small copy is plenty to find a color.
                    val sample =
                        BitmapFactory.decodeByteArray(
                            bytes,
                            0,
                            bytes.size,
                            BitmapFactory.Options().apply {
                                inSampleSize = (maxOf(bounds.outWidth, bounds.outHeight) / 128).coerceAtLeast(1)
                            },
                        )
                    val color =
                        sample?.let { bitmap ->
                            val palette = Palette.from(bitmap).generate()
                            bitmap.recycle()
                            (palette.vibrantSwatch ?: palette.dominantSwatch)?.rgb
                        }
                    ImageInfo(bounds.outWidth, bounds.outHeight, bytes.size, color)
                }
                .getOrNull()
        }

    private fun sniff(bytes: ByteArray): String? =
        when {
            bytes.size > 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() -> "image/jpeg"
            bytes.size > 8 && bytes[0] == 0x89.toByte() && bytes[1] == 'P'.code.toByte() -> "image/png"
            else -> null
        }

    private companion object {
        const val MAX_BYTES = 20 * 1024 * 1024
    }
}
