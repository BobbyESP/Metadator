/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.feature.batch

import android.content.Context
import androidx.core.net.toUri
import com.bobbyesp.metadator.core.common.AppDispatchers
import kotlinx.coroutines.withContext

/** Reads a picked image into bytes and a MIME type. */
interface CoverReader {
    suspend fun read(uri: String): Pair<ByteArray, String>?
}

class AndroidCoverReader(private val context: Context, private val dispatchers: AppDispatchers) :
    CoverReader {
    override suspend fun read(uri: String): Pair<ByteArray, String>? =
        withContext(dispatchers.io) {
            runCatching {
                val parsed = uri.toUri()
                val bytes =
                    context.contentResolver.openInputStream(parsed)?.use { it.readBytes() }
                        ?: return@runCatching null
                val mime = context.contentResolver.getType(parsed) ?: "image/jpeg"
                bytes to mime
            }
                .getOrNull()
        }
}
