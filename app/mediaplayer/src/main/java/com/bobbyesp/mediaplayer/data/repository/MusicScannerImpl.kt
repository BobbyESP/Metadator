package com.bobbyesp.mediaplayer.data.repository

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.core.net.toUri
import com.bobbyesp.coreutilities.observe
import com.bobbyesp.mediaplayer.domain.enums.MediaStoreSearchFilter
import com.bobbyesp.mediaplayer.domain.model.MusicTrack
import com.bobbyesp.mediaplayer.domain.repository.MusicScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MusicScannerImpl(
    private val context: Context
) : MusicScanner {

    override suspend fun getMusicLibrary(
        searchQuery: String?,
        filters: List<MediaStoreSearchFilter>?,
    ): List<MusicTrack> {
        val musicList = mutableListOf<MusicTrack>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                plus(MediaStore.Audio.Media.GENRE)
            }
        }

        val selection = buildSelection(searchQuery, filters)
        val selectionArgs = buildSelectionArgs(searchQuery, filters)
        val sortOrder = MediaStore.Audio.Media.TITLE

        context.contentResolver.advancedQuery(
            uri = this.musicUri,
            projection = projection,
            selection = selection,
            args = selectionArgs,
            order = sortOrder,
            ascending = true
        )?.use { cursor ->
            musicList.addAll(parseCursor(cursor))
        }

        return musicList
    }

    override fun observeMusicLibrary(
        searchQuery: String?,
        filters: List<MediaStoreSearchFilter>?
    ): Flow<List<MusicTrack>> {
        return context.contentResolver.observe(musicUri).map {
            getMusicLibrary(searchQuery, filters)
        }.flowOn(Dispatchers.IO)
    }

    private fun buildSelection(searchTerm: String?, filters: List<MediaStoreSearchFilter>?): String {
        val selection = StringBuilder(MUSIC_SELECTION)

        searchTerm?.let {
            selection.append(" AND (")
            if (!filters.isNullOrEmpty()) {
                filters.joinToString(" OR ") { "${it.column} LIKE ?" }.also { selection.append(it) }
            } else {
                selection.append(
                    "${MediaStore.Audio.Media.TITLE} LIKE ? OR " +
                            "${MediaStore.Audio.Media.ARTIST} LIKE ? OR " +
                            "${MediaStore.Audio.Media.ALBUM} LIKE ?"
                )
            }
            selection.append(")")
        }

        return selection.toString()
    }

    private fun buildSelectionArgs(searchTerm: String?, filters: List<MediaStoreSearchFilter>?): Array<String>? {
        return searchTerm?.let {
            Array(if (!filters.isNullOrEmpty()) filters.size else 3) { "%$searchTerm%" }
        }
    }

    private fun parseCursor(cursor: Cursor): List<MusicTrack> {
        val musicList = mutableListOf<MusicTrack>()
        val idColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
        val titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
        val artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
        val albumColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
        val durationColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
        val trackNumberColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK)
        val discNumberColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DISC_NUMBER)
        val yearColumn = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)
        val pathColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
        val addedTimestampColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
        val modifiedTimestampColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)
        val genreColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            cursor.getColumnIndex(MediaStore.Audio.Media.GENRE)
        } else null
        val sizeColumn = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val title = cursor.getString(titleColumn)
            val artist = cursor.getString(artistColumn)
            val album = cursor.getString(albumColumn)
            val duration = cursor.getLong(durationColumn)
            val trackNumber = cursor.getInt(trackNumberColumn)
            val discNumber = cursor.getInt(discNumberColumn)
            val year = cursor.getInt(yearColumn)
            val path = cursor.getString(pathColumn)
            val addedTimestamp = cursor.getLong(addedTimestampColumn)
            val modifiedTimestamp = cursor.getLong(modifiedTimestampColumn)
            val genre = genreColumn?.let { cursor.getString(it) }
            val size = sizeColumn.let { cursor.getLong(it) }
            val artworkUrl = ContentUris.withAppendedId(
                "content://media/external/audio/albumart".toUri(),
                cursor.getLong(idColumn)
            )

            musicList.add(
                MusicTrack(
                    id = id,
                    title = title,
                    artist = artist,
                    album = album,
                    duration = duration,
                    trackNumber = trackNumber,
                    discNumber = discNumber,
                    year = year,
                    path = path,
                    addedTimestamp = addedTimestamp,
                    modifiedTimestamp = modifiedTimestamp,
                    genre = genre,
                    size = size,
                    artworkUri = artworkUrl.toString()
                )
            )
        }

        return musicList
    }

    /**
     * Performs an advanced query on the content resolver.
     *
     * @param uri The URI to query.
     * @param projection The list of columns to put into the cursor.
     * @param selection A filter declaring which rows to return.
     * @param args You may include ?s in selection, which will be replaced by the values from selectionArgs.
     * @param order How to order the rows, formatted as an SQL ORDER BY clause.
     * @param ascending Whether the results should be in ascending order.
     * @param offset The offset of the first row to return.
     * @param limit The maximum number of rows to return.
     * @return A Cursor object, which is positioned before the first entry.
     */
    @SuppressLint("Recycle")
    private suspend fun ContentResolver.advancedQuery(
        uri: Uri,
        projection: Array<String>? = null,
        selection: String,
        args: Array<String>? = null,
        order: String = MediaStore.MediaColumns._ID,
        ascending: Boolean = true,
        offset: Int = 0,
        limit: Int = Int.MAX_VALUE
    ): Cursor? {
        return withContext(Dispatchers.IO) {
            // use only above android 10
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // compose the args
                val args2 = Bundle().apply {
                    // Limit & Offset
                    putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
                    putInt(ContentResolver.QUERY_ARG_OFFSET, offset)

                    // order
                    putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(order))
                    putInt(
                        ContentResolver.QUERY_ARG_SORT_DIRECTION,
                        if (ascending) ContentResolver.QUERY_SORT_DIRECTION_ASCENDING else ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                    )
                    // Selection and groupBy
                    if (args != null) putStringArray(
                        ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                        args
                    )
                    // add selection.
                    // TODO: Consider adding group by.
                    // currently I experienced errors in android 10 for groupBy and arg groupBy is supported
                    // above android 10.
                    putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                }
                query(uri, projection, args2, null)
            }
            // below android O
            else {
                //language=SQL
                val order2 =
                    order + (if (ascending) " ASC" else " DESC") + " LIMIT $limit OFFSET $offset"
                // compose the selection.
                query(uri, projection, selection, args, order2)
            }
        }
    }

    companion object {
        private const val MUSIC_SELECTION = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
    }
}