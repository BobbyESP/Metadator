package com.bobbyesp.utilities.mediastore

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import com.bobbyesp.utilities.R
import com.bobbyesp.utilities.mediastore.advanced.advancedQuery
import com.bobbyesp.utilities.mediastore.advanced.observe
import com.bobbyesp.utilities.mediastore.model.Song
import kotlinx.coroutines.flow.map
import java.io.FileNotFoundException

object MediaStoreReceiver {

    private val audioUri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    private val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.ALBUM_ID
    )
    private const val isMusicSelection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

    private fun buildSelection(searchTerm: String?, filterType: MediaStoreFilterType?): String {
        return when {
            !searchTerm.isNullOrEmpty() && filterType != null -> {
                "$isMusicSelection AND ${filterType.column} LIKE '%$searchTerm%'"
            }

            //Search everywhere
            !searchTerm.isNullOrEmpty() -> {
                "$isMusicSelection AND (${MediaStore.Audio.Media.TITLE} LIKE '%$searchTerm%' OR ${MediaStore.Audio.Media.ARTIST} LIKE '%$searchTerm%' OR ${MediaStore.Audio.Media.ALBUM} LIKE '%$searchTerm%')"
            }

            else -> isMusicSelection
        }
    }

    private fun buildSelectionArgs(
        searchTerm: String?,
        filterType: MediaStoreFilterType?
    ): Array<String>? {
        return when {
            !searchTerm.isNullOrEmpty() && filterType != null -> arrayOf("%$searchTerm%")
            !searchTerm.isNullOrEmpty() -> arrayOf("%$searchTerm%", "%$searchTerm%")
            else -> null
        }
    }

    private fun parseCursorToSongs(cursor: android.database.Cursor): List<Song> {
        val songs = mutableListOf<Song>()
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
        val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

        while (cursor.moveToNext()) {
            val song = Song(
                id = cursor.getLong(idColumn),
                title = cursor.getString(titleColumn),
                artist = cursor.getString(artistColumn),
                album = cursor.getString(albumColumn),
                artworkPath = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    cursor.getLong(albumIdColumn)
                ),
                duration = cursor.getDouble(durationColumn),
                path = cursor.getString(pathColumn),
                fileName = cursor.getString(pathColumn).substringAfterLast("/")
            )
            songs.add(song)
        }
        cursor.close()
        return songs
    }

    fun getSongs(
        context: Context,
        searchTerm: String? = null,
        filterType: MediaStoreFilterType? = null
    ): List<Song> {
        val resolver = context.contentResolver
        val selection = buildSelection(searchTerm, filterType)
        val selectionArgs = buildSelectionArgs(searchTerm, filterType)
        val sortOrder = MediaStore.Audio.Media.TITLE

        return resolver.query(audioUri, projection, selection, selectionArgs, sortOrder)
            ?.use { cursor ->
                parseCursorToSongs(cursor)
            } ?: emptyList()
    }

    @SuppressLint("Range")
    fun getFileDescriptorFromPath(
        context: Context,
        filePath: String,
        mode: String = "r"
    ): ParcelFileDescriptor? {
        val resolver = context.contentResolver
        val selection = "${MediaStore.Files.FileColumns.DATA} = ?"
        val selectionArgs = arrayOf(filePath)

        resolver.query(
            audioUri,
            arrayOf(MediaStore.Files.FileColumns._ID),
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val fileId = cursor.getInt(cursor.getColumnIndex(MediaStore.Files.FileColumns._ID))
                val fileUri = ContentUris.withAppendedId(audioUri, fileId.toLong())
                return try {
                    resolver.openFileDescriptor(fileUri, mode)
                } catch (e: FileNotFoundException) {
                    Log.e("MediaStoreReceiver", "File not found: ${e.message}")
                    null
                }
            }
            cursor.close()
        }
        return null
    }

    object Advanced {
        suspend fun ContentResolver.getSongs(
            searchTerm: String? = null,
            filterType: MediaStoreFilterType? = null
        ): List<Song> {
            val selection = buildSelection(searchTerm, filterType)
            val selectionArgs = buildSelectionArgs(searchTerm, filterType)
            val sortOrder = MediaStore.Audio.Media.TITLE //Order by title

            return advancedQuery(
                uri = audioUri,
                projection = projection,
                selection = selection,
                args = selectionArgs,
                order = sortOrder,
                ascending = true
            )?.use { cursor ->
                parseCursorToSongs(cursor)
            } ?: emptyList()
        }

        fun ContentResolver.observeSongs(
            searchTerm: String? = null,
            filterType: MediaStoreFilterType? = null
        ) =
            observe(audioUri).map {
                getSongs(searchTerm, filterType)
            }
    }
}

enum class MediaStoreFilterType(val column: String) {
    TITLE(MediaStore.Audio.Media.TITLE),
    ARTIST(MediaStore.Audio.Media.ARTIST),
    ALBUM(MediaStore.Audio.Media.ALBUM);

    fun toString(context: Context): String {
        return when (this) {
            TITLE -> context.getString(R.string.title)
            ARTIST -> context.getString(R.string.artist)
            ALBUM -> context.getString(R.string.album)
        }
    }
}
