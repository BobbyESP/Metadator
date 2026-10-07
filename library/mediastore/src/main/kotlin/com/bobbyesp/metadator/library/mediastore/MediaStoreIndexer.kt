package com.bobbyesp.metadator.library.mediastore

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import com.bobbyesp.metadator.core.common.AppDispatchers
import com.bobbyesp.metadator.core.model.ContentRef
import com.bobbyesp.metadator.library.api.MediaIndexer
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Asks MediaStore to read written files again. Without it the library, and every music player,
 * keeps showing the old title until the system happens to rescan.
 */
class MediaStoreIndexer(
    private val context: Context,
    private val dispatchers: AppDispatchers,
) : MediaIndexer {

    override suspend fun rescan(refs: List<ContentRef>) {
        val paths =
            withContext(dispatchers.io) {
                refs.mapNotNull { ref -> pathOf(Uri.parse(ref.uri)) }
            }
        refs.forEach { context.contentResolver.notifyChange(Uri.parse(it.uri), null) }
        if (paths.isEmpty()) return
        withTimeoutOrNull(SCAN_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                var pending = paths.size
                MediaScannerConnection.scanFile(context, paths.toTypedArray(), null) { _, _ ->
                    pending--
                    if (pending == 0 && continuation.isActive) continuation.resume(Unit)
                }
            }
        }
    }

    /**
     * The scanner takes paths. MediaStore still reports them for its own files, and this is the
     * one use left for them: nothing is opened by path.
     */
    private fun pathOf(uri: Uri): String? {
        val mediaId = mediaIdOf(context, uri) ?: return null
        val mediaUri = MediaStoreAudioLibrary.AudioCollection.buildUpon().appendPath(mediaId.toString()).build()
        return runCatching {
                context.contentResolver
                    .query(mediaUri, arrayOf(MediaStoreAudioLibrary.DATA_COLUMN), null, null, null)
                    ?.use { if (it.moveToFirst()) it.getString(0) else null }
            }
            .getOrNull()
    }

    private companion object {
        const val SCAN_TIMEOUT_MS = 10_000L
    }
}
