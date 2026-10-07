package com.bobbyesp.metadator.library.mediastore

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.bobbyesp.metadator.core.common.AppDispatchers
import com.bobbyesp.metadator.core.model.ContentRef
import com.bobbyesp.metadator.core.model.Track
import com.bobbyesp.metadator.core.model.TrackId
import com.bobbyesp.metadator.library.api.AudioLibrary
import kotlin.coroutines.resume
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** The device's songs, from MediaStore. */
class MediaStoreAudioLibrary(
    private val context: Context,
    private val dispatchers: AppDispatchers,
) : AudioLibrary {

    private val resolver: ContentResolver
        get() = context.contentResolver

    @OptIn(FlowPreview::class)
    override fun observeTracks(): Flow<List<Track>> =
        callbackFlow {
                val observer =
                    object : ContentObserver(Handler(Looper.getMainLooper())) {
                        override fun onChange(selfChange: Boolean) {
                            trySend(Unit)
                        }
                    }
                resolver.registerContentObserver(AudioCollection, true, observer)
                trySend(Unit)
                awaitClose { resolver.unregisterContentObserver(observer) }
            }
            // A save or a scan changes many rows at once: one query for the burst is enough.
            .debounce(DEBOUNCE_MS)
            .conflate()
            .map { queryTracks(selection = IS_MUSIC, args = null) }
            .flowOn(dispatchers.io)

    override suspend fun track(id: TrackId): Track? =
        withContext(dispatchers.io) {
            queryTracks("${MediaStore.Audio.Media._ID} = ?", arrayOf(id.value.toString()))
                .firstOrNull()
        }

    override suspend fun findByRef(ref: ContentRef): Track? =
        withContext(dispatchers.io) {
            val id = mediaIdOf(context, Uri.parse(ref.uri)) ?: return@withContext null
            track(TrackId(id))
        }

    override suspend fun refresh() {
        val folders =
            listOf(Environment.DIRECTORY_MUSIC, Environment.DIRECTORY_DOWNLOADS)
                .map { Environment.getExternalStoragePublicDirectory(it).absolutePath }
                .toTypedArray()
        withTimeoutOrNull(SCAN_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                var pending = folders.size
                MediaScannerConnection.scanFile(context, folders, null) { _, _ ->
                    pending--
                    if (pending == 0 && continuation.isActive) continuation.resume(Unit)
                }
            }
        }
    }

    private fun queryTracks(selection: String, args: Array<String>?): List<Track> {
        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.TITLE)
            add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.ALBUM)
            add(MediaStore.Audio.Media.ALBUM_ID)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.TRACK)
            add(MediaStore.Audio.Media.YEAR)
            add(MediaStore.Audio.Media.MIME_TYPE)
            add(MediaStore.Audio.Media.SIZE)
            add(MediaStore.Audio.Media.DISPLAY_NAME)
            add(MediaStore.Audio.Media.DATE_ADDED)
            add(MediaStore.Audio.Media.DATE_MODIFIED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Audio.Media.RELATIVE_PATH)
            } else {
                add(DATA_COLUMN)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(MediaStore.Audio.Media.ALBUM_ARTIST)
                add(MediaStore.Audio.Media.GENRE)
                add(MediaStore.Audio.Media.DISC_NUMBER)
            }
        }
        return resolver
            .query(AudioCollection, projection.toTypedArray(), selection, args, null)
            ?.use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.toTrack()) } }
            .orEmpty()
    }

    private fun Cursor.toTrack(): Track {
        val id = long(MediaStore.Audio.Media._ID)!!
        val albumId = long(MediaStore.Audio.Media.ALBUM_ID) ?: 0
        // MediaStore packs the disc into the thousands: 2005 is disc 2, track 5.
        val packedTrack = int(MediaStore.Audio.Media.TRACK)
        val discColumn = string(MediaStore.Audio.Media.DISC_NUMBER)?.substringBefore('/')?.toIntOrNull()
        val displayName = string(MediaStore.Audio.Media.DISPLAY_NAME).orEmpty()
        return Track(
            id = TrackId(id),
            ref = ContentRef(ContentUris.withAppendedId(AudioCollection, id).toString()),
            title = string(MediaStore.Audio.Media.TITLE)?.ifBlank { null } ?: displayName.substringBeforeLast('.'),
            artist = string(MediaStore.Audio.Media.ARTIST).known(),
            album = string(MediaStore.Audio.Media.ALBUM).known(),
            albumArtist = string(MediaStore.Audio.Media.ALBUM_ARTIST).known(),
            albumId = albumId,
            durationMs = long(MediaStore.Audio.Media.DURATION) ?: 0,
            trackNumber = packedTrack?.rem(1000)?.takeIf { it > 0 },
            discNumber = discColumn ?: packedTrack?.div(1000)?.takeIf { it > 0 },
            year = int(MediaStore.Audio.Media.YEAR)?.takeIf { it > 0 },
            genre = string(MediaStore.Audio.Media.GENRE).known(),
            mimeType = string(MediaStore.Audio.Media.MIME_TYPE),
            sizeBytes = long(MediaStore.Audio.Media.SIZE) ?: 0,
            displayName = displayName,
            folder =
                string(MediaStore.Audio.Media.RELATIVE_PATH)
                    ?: string(DATA_COLUMN)?.let(::relativeFolder),
            dateAddedEpochSeconds = long(MediaStore.Audio.Media.DATE_ADDED) ?: 0,
            dateModifiedEpochSeconds = long(MediaStore.Audio.Media.DATE_MODIFIED) ?: 0,
            artworkRef =
                ContentRef(ContentUris.withAppendedId(AlbumArtCollection, albumId).toString()),
        )
    }

    private fun Cursor.index(column: String): Int? =
        getColumnIndex(column).takeIf { it >= 0 && !isNull(it) }

    private fun Cursor.string(column: String): String? = index(column)?.let(::getString)

    private fun Cursor.long(column: String): Long? = index(column)?.let(::getLong)

    private fun Cursor.int(column: String): Int? = index(column)?.let(::getInt)

    /** MediaStore spells "no value" as `<unknown>`. */
    private fun String?.known(): String? = this?.takeIf { it.isNotBlank() && it != MediaStore.UNKNOWN_STRING }

    private fun relativeFolder(path: String): String {
        val root = Environment.getExternalStorageDirectory().absolutePath.trimEnd('/') + "/"
        return path.removePrefix(root).substringBeforeLast('/', "") + "/"
    }

    companion object {
        val AudioCollection: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        private val AlbumArtCollection: Uri = Uri.parse("content://media/external/audio/albumart")
        /** The file path column: deprecated, and only read where nothing replaces it (API < 29). */
        internal const val DATA_COLUMN = "_data"
        private const val IS_MUSIC = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        private const val DEBOUNCE_MS = 300L
        private const val SCAN_TIMEOUT_MS = 30_000L
    }
}

/**
 * The MediaStore id of [uri]: its own for a MediaStore URI, and the one of the same file for a
 * document another app handed over, when the system can tell (Android 10+).
 */
internal fun mediaIdOf(context: Context, uri: Uri): Long? {
    if (uri.authority == MediaStore.AUTHORITY) {
        return runCatching { ContentUris.parseId(uri) }.getOrNull()?.takeIf { it >= 0 }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val mediaUri = runCatching { MediaStore.getMediaUri(context, uri) }.getOrNull() ?: return null
        return runCatching { ContentUris.parseId(mediaUri) }.getOrNull()
    }
    return null
}

internal fun isMediaStoreUri(uri: Uri): Boolean = uri.authority == MediaStore.AUTHORITY
