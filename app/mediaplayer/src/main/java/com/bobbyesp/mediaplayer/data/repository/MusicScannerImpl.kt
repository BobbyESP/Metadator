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
import android.util.Log
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

private const val TAG = "MusicScannerImpl"

class MusicScannerImpl(private val context: Context) : MusicScanner {

  override suspend fun getMusicLibrary(
      searchQuery: String?,
      filters: List<MediaStoreSearchFilter>?,
  ): List<MusicTrack> =
      withContext(Dispatchers.IO) {
        val musicList = mutableListOf<MusicTrack>()
        val projection = projectionBasedOnVersion()
        val selection = buildSelection(searchQuery, filters)
        val selectionArgs = buildSelectionArgs(searchQuery, filters)
        val sortOrder = MediaStore.Audio.Media.TITLE

        context.contentResolver
            .advancedQuery(
                uri = musicUri,
                projection = projection,
                selection = selection,
                args = selectionArgs,
                order = sortOrder,
                ascending = true)
            ?.use { cursor -> musicList.addAll(parseCursor(cursor)) }
        musicList.toList() // Convert to immutable list
      }

  override fun observeMusicLibrary(
      searchQuery: String?,
      filters: List<MediaStoreSearchFilter>?
  ): Flow<List<MusicTrack>> =
      context.contentResolver
          .observe(musicUri)
          .map { getMusicLibrary(searchQuery, filters) }
          .flowOn(Dispatchers.IO)

  private fun projectionBasedOnVersion(): Array<String> =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DISC_NUMBER,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.GENRE)
      } else {
        arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DISC_NUMBER,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED)
      }

  private fun buildSelection(searchTerm: String?, filters: List<MediaStoreSearchFilter>?): String {
    val selection = StringBuilder(MUSIC_SELECTION)

    searchTerm?.let { term ->
      selection.append(" AND (")
      if (!filters.isNullOrEmpty()) {
        val filterSelection = filters.joinToString(" OR ") { "${it.column} LIKE '%$term%'" }
        selection.append(filterSelection)
      } else {
        selection.append(
            "${MediaStore.Audio.Media.TITLE} LIKE '%$term%' OR " +
                "${MediaStore.Audio.Media.ARTIST} LIKE '%$term%' OR " +
                "${MediaStore.Audio.Media.ALBUM} LIKE '%$term%'")
      }
      selection.append(")")
    }
    return selection.toString()
  }

  private fun buildSelectionArgs(
      searchTerm: String?,
      filters: List<MediaStoreSearchFilter>?
  ): Array<String>? =
      searchTerm?.let { term ->
        Array(if (filters.isNullOrEmpty()) 3 else filters.size) { "%$term%" }
      }

  private fun parseCursor(cursor: Cursor): List<MusicTrack> {
    val musicList = mutableListOf<MusicTrack>()

    val columnIndices = MusicColumnIndices(cursor)

    while (cursor.moveToNext()) {
      val id = getLong(cursor, columnIndices.id) ?: throw IllegalStateException("ID cannot be null")
      val albumId = getLong(cursor, columnIndices.albumId)
      val path =
          getString(cursor, columnIndices.path)
              ?: throw IllegalStateException("Path cannot be null")
      val title = getString(cursor, columnIndices.title) ?: path.substringAfterLast("/")
      val artist = getString(cursor, columnIndices.artist)
      val album = getString(cursor, columnIndices.album)
      val duration = getLong(cursor, columnIndices.duration)
      val trackNumber = getInt(cursor, columnIndices.trackNumber)
      val discNumber = getInt(cursor, columnIndices.discNumber)
      val year = getInt(cursor, columnIndices.year)
      val addedTimestamp = getLong(cursor, columnIndices.addedTimestamp)
      val modifiedTimestamp = getLong(cursor, columnIndices.modifiedTimestamp)
      val genre = columnIndices.genre?.let { getString(cursor, it) }
      val size = getLong(cursor, columnIndices.size)
      val artworkUrl =
          ContentUris.withAppendedId(
              "content://media/external/audio/albumart".toUri(), albumId ?: 0)

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
              artworkUri = artworkUrl.toString()))
    }
    return musicList.toList() // Convert to immutable list
  }

  private data class MusicColumnIndices(val cursor: Cursor) {
    val id = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
    val title = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
    val artist = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
    val album = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
    val albumId = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
    val duration = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
    val trackNumber = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
    val discNumber = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISC_NUMBER)
    val year = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
    val path = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
    val addedTimestamp = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
    val modifiedTimestamp = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
    val genre =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          cursor.getColumnIndex(
              MediaStore.Audio.Media.GENRE) // Use getColumnIndex to avoid exception if not present
        } else null
    val size = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
  }

  private fun getString(cursor: Cursor, columnIndex: Int): String? {
    return try {
      cursor.getString(columnIndex)
    } catch (e: Exception) {
      Log.e(TAG, "Error getting String from cursor at index $columnIndex", e)
      null
    }
  }

  private fun getLong(cursor: Cursor, columnIndex: Int): Long? {
    return try {
      cursor.getLong(columnIndex)
    } catch (e: Exception) {
      Log.e(TAG, "Error getting Long from cursor at index $columnIndex", e)
      null
    }
  }

  private fun getInt(cursor: Cursor, columnIndex: Int): Int? {
    return try {
      cursor.getInt(columnIndex)
    } catch (e: Exception) {
      Log.e(TAG, "Error getting Int from cursor at index $columnIndex", e)
      null
    }
  }

  /**
   * Performs an advanced query on the content resolver.
   *
   * @param uri The URI to query.
   * @param projection The list of columns to put into the cursor.
   * @param selection A filter declaring which rows to return.
   * @param args You may include ?s in selection, which will be replaced by the values from
   *   selectionArgs.
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
  ): Cursor? =
      withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          val queryArgs =
              Bundle().apply {
                putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
                putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
                putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(order))
                putInt(
                    ContentResolver.QUERY_ARG_SORT_DIRECTION,
                    if (ascending) ContentResolver.QUERY_SORT_DIRECTION_ASCENDING
                    else ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
                args?.let { putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, it) }
                putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
              }
          query(uri, projection, queryArgs, null)
        } else {
          val orderClause = "$order ${if (ascending) "ASC" else "DESC"} LIMIT $limit OFFSET $offset"
          query(uri, projection, selection, args, orderClause)
        }
      }

  companion object {
    private const val MUSIC_SELECTION = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
  }
}
