/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.domain.library

import com.bobbyesp.metadator.core.common.normalizedForComparison
import com.bobbyesp.metadator.core.model.SortOrder
import com.bobbyesp.metadator.core.model.Track
import com.bobbyesp.metadator.core.model.TrackCollection
import com.bobbyesp.metadator.core.model.TrackSort
import java.text.Collator
import java.util.Locale

/** What the library is narrowed to. */
data class LibraryFilter(
    val search: String = "",
    val needsAttention: Boolean = false,
    /** File extensions, lower case. Empty means every format. */
    val formats: Set<String> = emptySet(),
) {
    val isActive: Boolean
        get() = search.isNotBlank() || needsAttention || formats.isNotEmpty()
}

fun List<Track>.filtered(filter: LibraryFilter): List<Track> {
    val terms = filter.search.normalizedForComparison().split(' ').filter { it.isNotEmpty() }
    return filter { track ->
        (!filter.needsAttention || track.needsAttention) &&
            (filter.formats.isEmpty() || track.fileExtension in filter.formats) &&
            (terms.isEmpty() || track.matches(terms))
    }
}

/** Every term must appear in one of the fields: "beatles abbey" finds Come Together. */
private fun Track.matches(terms: List<String>): Boolean {
    val haystack =
        listOfNotNull(title, artist, album, albumArtist, displayName, genre)
            .joinToString(" ")
            .normalizedForComparison()
    return terms.all { it in haystack }
}

fun List<Track>.sorted(order: SortOrder, locale: Locale = Locale.getDefault()): List<Track> {
    val collator = Collator.getInstance(locale).apply { strength = Collator.PRIMARY }
    val text =
        Comparator<String?> { a, b ->
            when {
                a.isNullOrBlank() && b.isNullOrBlank() -> 0
                // Missing values last, whichever the direction, so they never crowd the top.
                a.isNullOrBlank() -> if (order.ascending) 1 else -1
                b.isNullOrBlank() -> if (order.ascending) -1 else 1
                else -> collator.compare(a, b)
            }
        }
    val comparator: Comparator<Track> =
        when (order.sort) {
            TrackSort.Title -> compareBy(text) { it.title }
            TrackSort.Artist ->
                compareBy<Track, String?>(text) { it.artist }
                    .thenBy(text) { it.album }
                    .thenBy { it.discNumber ?: 0 }
                    .thenBy { it.trackNumber ?: 0 }
            TrackSort.Album ->
                compareBy<Track, String?>(text) { it.album }
                    .thenBy { it.discNumber ?: 0 }
                    .thenBy { it.trackNumber ?: 0 }
            TrackSort.DateAdded -> compareBy { it.dateAddedEpochSeconds }
            TrackSort.DateModified -> compareBy { it.dateModifiedEpochSeconds }
            TrackSort.Duration -> compareBy { it.durationMs }
        }
    return sortedWith(if (order.ascending) comparator else comparator.reversed())
}

/** Albums by album artist and title, so two albums called "Greatest Hits" stay apart. */
fun List<Track>.albums(): List<TrackCollection.Album> = filter {
    !it.album.isNullOrBlank()
}
    .groupBy { (it.albumArtist ?: it.artist).orEmpty().lowercase() to it.album!!.lowercase() }
    .map { (_, tracks) ->
        val ordered = tracks.sortedWith(compareBy({ it.discNumber ?: 0 }, { it.trackNumber ?: 0 }))
        val first = ordered.first()
        TrackCollection.Album(
            key = "album:${first.albumId}:${first.album}",
            title = first.album!!,
            artist = first.albumArtist ?: first.artist,
            year = ordered.firstNotNullOfOrNull { it.year },
            tracks = ordered,
        )
    }
    .sortedWith(compareBy(Collator.getInstance()) { it.title })

/** Artists by album artist when there is one, so features do not split an album's artist. */
fun List<Track>.artists(): List<TrackCollection.Artist> = filter {
    !(it.albumArtist ?: it.artist).isNullOrBlank()
}
    .groupBy { (it.albumArtist ?: it.artist)!!.trim() }
    .map { (name, tracks) ->
        TrackCollection.Artist(
            key = "artist:$name",
            title = name,
            tracks = tracks.sorted(SortOrder(TrackSort.Album)),
        )
    }
    .sortedWith(compareBy(Collator.getInstance()) { it.title })

fun List<Track>.folders(): List<TrackCollection.Folder> = groupBy {
    it.folder.orEmpty()
}
    .map { (folder, tracks) ->
        TrackCollection.Folder(
            key = "folder:$folder",
            title = folder.trimEnd('/').substringAfterLast('/').ifEmpty { folder.ifEmpty { "/" } },
            tracks = tracks.sorted(SortOrder(TrackSort.Title)),
        )
    }
    .sortedWith(compareBy(Collator.getInstance()) { it.title })
