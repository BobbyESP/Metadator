/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.domain.library

import com.bobbyesp.metadator.core.domain.editor.TrackPositions
import com.bobbyesp.metadator.core.model.ContentRef
import com.bobbyesp.metadator.core.model.Track
import com.bobbyesp.metadator.core.model.TrackId
import com.bobbyesp.metadator.library.api.AudioLibrary
import com.bobbyesp.metadator.library.api.CachedTrackTags
import com.bobbyesp.metadator.library.api.FileTrackTags
import com.bobbyesp.metadator.library.api.TrackTagCache
import com.bobbyesp.metadator.tags.api.TagField
import com.bobbyesp.metadator.tags.api.TagMap
import com.bobbyesp.metadator.tags.api.TagReadResult
import com.bobbyesp.metadator.tags.api.TagReader
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A library whose gaps are filled from the files themselves. The system's scanner does not index
 * every tag of every format (a FLAC's `DATE` never becomes a year), so a field the library has no
 * value for only counts as missing once the file has none either.
 */
class TagCompletedAudioLibrary(
    private val source: AudioLibrary,
    private val reader: TagReader,
    private val cache: TrackTagCache,
) : AudioLibrary {

    private val known = ConcurrentHashMap<TrackId, CachedTrackTags>()
    private val restoring = Mutex()
    private var restored = false

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeTracks(): Flow<List<Track>> =
        source.observeTracks().transformLatest { tracks ->
            restore()
            // An empty library is more often a permission not granted yet than every song gone.
            if (tracks.isNotEmpty()) forget(known.keys - tracks.mapTo(HashSet()) { it.id })
            emit(tracks.map(::completed))
            tracks.filter(::isUnread).chunked(READ_CHUNK).forEach { chunk ->
                read(chunk)
                emit(tracks.map(::completed))
            }
        }

    override suspend fun track(id: TrackId): Track? = source.track(id)?.let { completedNow(it) }

    override suspend fun findByRef(ref: ContentRef): Track? =
        source.findByRef(ref)?.let { completedNow(it) }

    override suspend fun refresh() = source.refresh()

    private suspend fun completedNow(track: Track): Track {
        restore()
        if (isUnread(track)) read(listOf(track))
        return completed(track)
    }

    private suspend fun restore() = restoring.withLock {
        if (!restored) {
            cache.all().forEach { known.putIfAbsent(it.id, it) }
            restored = true
        }
    }

    private fun isUnread(track: Track): Boolean =
        track.missingFields.isNotEmpty() && tagsOf(track) == null

    private fun tagsOf(track: Track): FileTrackTags? =
        known[track.id]
            ?.takeIf {
                it.dateModifiedEpochSeconds == track.dateModifiedEpochSeconds &&
                    it.sizeBytes == track.sizeBytes
            }
            ?.tags

    private fun completed(track: Track): Track {
        val tags = tagsOf(track) ?: return track
        return track.copy(
            artist = track.artist ?: tags.artist,
            album = track.album ?: tags.album,
            albumArtist = track.albumArtist ?: tags.albumArtist,
            year = track.year ?: tags.year,
            trackNumber = track.trackNumber ?: tags.trackNumber,
            discNumber = track.discNumber ?: tags.discNumber,
            genre = track.genre ?: tags.genre,
        )
    }

    private suspend fun read(tracks: List<Track>) {
        val settled = mutableListOf<CachedTrackTags>()
        for (track in tracks) {
            val result = reader.read(track.ref, includePictures = false)
            val tags = (result as? TagReadResult.Success)?.snapshot?.tags?.toFileTrackTags()
            val entry =
                CachedTrackTags(
                    id = track.id,
                    dateModifiedEpochSeconds = track.dateModifiedEpochSeconds,
                    sizeBytes = track.sizeBytes,
                    tags = tags ?: FileTrackTags(),
                )
            known[track.id] = entry
            // A file that could not be opened is only remembered until the app restarts.
            if (tags != null || result == TagReadResult.Unsupported) settled += entry
        }
        if (settled.isNotEmpty()) cache.put(settled)
    }

    private suspend fun forget(ids: Set<TrackId>) {
        if (ids.isEmpty()) return
        ids.forEach(known::remove)
        cache.remove(ids)
    }

    private companion object {
        const val READ_CHUNK = 50
    }
}

internal fun TagMap.toFileTrackTags(): FileTrackTags =
    FileTrackTags(
        artist = joined(TagField.Artist),
        album = first(TagField.Album.key)?.trim()?.ifEmpty { null },
        albumArtist = joined(TagField.AlbumArtist),
        year =
            (get(TagField.Date.key) + get(YEAR_KEY)).firstNotNullOfOrNull { date ->
                YearPattern.find(date)?.value?.toIntOrNull()?.takeIf { it > 0 }
            },
        trackNumber = position(TagField.TrackNumber),
        discNumber = position(TagField.DiscNumber),
        genre = joined(TagField.Genre),
    )

/** One line for a list row, the way MediaStore reports several values. */
private fun TagMap.joined(field: TagField): String? =
    get(field.key).map { it.trim() }.filter { it.isNotEmpty() }.joinToString(", ").ifEmpty { null }

private fun TagMap.position(field: TagField): Int? =
    TrackPositions.read(this, field).number.toIntOrNull()?.takeIf { it > 0 }

private const val YEAR_KEY = "YEAR"
private val YearPattern = Regex("""\d{4}""")
