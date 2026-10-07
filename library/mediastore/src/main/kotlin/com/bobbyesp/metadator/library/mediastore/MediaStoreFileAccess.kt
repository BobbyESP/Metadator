/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.library.mediastore

import android.app.RecoverableSecurityException
import android.content.Context
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.bobbyesp.metadator.core.common.AppDispatchers
import com.bobbyesp.metadator.core.model.ContentRef
import com.bobbyesp.metadator.library.api.AccessMode
import com.bobbyesp.metadator.library.api.AudioFileInfo
import com.bobbyesp.metadator.library.api.AudioFileOpener
import com.bobbyesp.metadator.library.api.FileDescription
import com.bobbyesp.metadator.library.api.OpenResult
import com.bobbyesp.metadator.library.api.OpenedAudioFile
import java.io.FileNotFoundException
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.withContext
import androidx.core.net.toUri

/**
 * The prompts Android 10 hands out when a write is refused, kept until [MediaStoreWriteAccess]
 * shows them. Android 10 has no way to ask in advance; the refusal is the only way to get one.
 */
class RecoverableAccessCache {
    private val prompts = ConcurrentHashMap<String, Any>()

    fun put(ref: ContentRef, exception: Any) {
        prompts[ref.uri] = exception
    }

    fun take(ref: ContentRef): Any? = prompts.remove(ref.uri)
}

class ContentResolverFileOpener(
    private val context: Context,
    private val recoverable: RecoverableAccessCache,
) : AudioFileOpener {

    override fun open(ref: ContentRef, mode: AccessMode): OpenResult {
        val uri = ref.uri.toUri()
        return try {
            val pfd =
                context.contentResolver.openFileDescriptor(
                    uri,
                    if (mode == AccessMode.Read) "r" else "rw",
                ) ?: return OpenResult.NotFound
            OpenResult.Opened(ParcelAudioFile(pfd))
        } catch (_: FileNotFoundException) {
            OpenResult.NotFound
        } catch (security: SecurityException) {
            if (
                Build.VERSION.SDK_INT == Build.VERSION_CODES.Q &&
                    security is RecoverableSecurityException
            ) {
                recoverable.put(ref, security)
            }
            OpenResult.AccessDenied
        } catch (unsupported: UnsupportedOperationException) {
            // A provider that only serves streams cannot hand out a descriptor to write.
            if (mode == AccessMode.ReadWrite) OpenResult.AccessDenied
            else OpenResult.Failed(unsupported.message ?: "Unsupported")
        } catch (error: IllegalArgumentException) {
            OpenResult.Failed(error.message ?: "Invalid location")
        }
    }
}

private class ParcelAudioFile(private val pfd: ParcelFileDescriptor) : OpenedAudioFile {
    override fun detachDuplicateFd(): Int = pfd.dup().detachFd()

    override fun close() = pfd.close()
}

class ContentResolverFileInfo(
    private val context: Context,
    private val dispatchers: AppDispatchers,
) : AudioFileInfo {

    override suspend fun describe(ref: ContentRef): FileDescription? =
        withContext(dispatchers.io) {
            val uri = ref.uri.toUri()
            val resolver = context.contentResolver
            runCatching {
                val projection = buildList {
                    add(OpenableColumns.DISPLAY_NAME)
                    add(OpenableColumns.SIZE)
                    if (isMediaStoreUri(uri)) {
                        add(MediaStore.MediaColumns.DATE_MODIFIED)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            add(MediaStore.MediaColumns.RELATIVE_PATH)
                        } else add(MediaStoreAudioLibrary.DATA_COLUMN)
                    }
                }
                resolver.query(uri, projection.toTypedArray(), null, null, null)?.use { cursor ->
                    if (!cursor.moveToFirst()) return@use null
                    fun string(column: String) =
                        cursor
                            .getColumnIndex(column)
                            .takeIf { it >= 0 && !cursor.isNull(it) }
                            ?.let(cursor::getString)
                    fun long(column: String) =
                        cursor
                            .getColumnIndex(column)
                            .takeIf { it >= 0 && !cursor.isNull(it) }
                            ?.let(cursor::getLong)
                    FileDescription(
                        displayName =
                            string(OpenableColumns.DISPLAY_NAME) ?: uri.lastPathSegment.orEmpty(),
                        sizeBytes = long(OpenableColumns.SIZE),
                        mimeType = resolver.getType(uri),
                        lastModifiedMillis =
                            long(MediaStore.MediaColumns.DATE_MODIFIED)?.times(1000),
                        location =
                            string(MediaStore.MediaColumns.RELATIVE_PATH)
                                ?: string(MediaStoreAudioLibrary.DATA_COLUMN),
                    )
                }
            }
                .getOrNull()
        }
}
