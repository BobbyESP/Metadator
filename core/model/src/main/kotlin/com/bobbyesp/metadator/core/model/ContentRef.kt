package com.bobbyesp.metadator.core.model

/**
 * Where an audio file is, as a `content://` URI string.
 *
 * Never a file path: since scoped storage a path is neither stable nor something the app may open,
 * and the same file can be reached through MediaStore or through a document another app handed
 * over. Never `android.net.Uri` either, so that domains stay plain Kotlin.
 */
@JvmInline
value class ContentRef(val uri: String) {
    override fun toString(): String = uri
}
