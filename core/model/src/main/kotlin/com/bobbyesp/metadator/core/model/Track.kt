/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.model

/** A song in the device's library, as MediaStore indexes it. */
@JvmInline value class TrackId(val value: Long)

/**
 * What the library knows about a song. The tags in the file are the truth; this is MediaStore's
 * copy, which can lag behind until the file is rescanned, with the fields MediaStore has no value
 * for read from the file.
 */
data class Track(
    val id: TrackId,
    val ref: ContentRef,
    val title: String,
    /** Null when MediaStore has no artist (it reports `<unknown>`). */
    val artist: String?,
    val album: String?,
    val albumArtist: String?,
    val albumId: Long,
    val durationMs: Long,
    val trackNumber: Int?,
    val discNumber: Int?,
    val year: Int?,
    val genre: String?,
    val mimeType: String?,
    val sizeBytes: Long,
    val displayName: String,
    /** The folder, relative to the storage volume (`Music/Album/`), when MediaStore knows it. */
    val folder: String?,
    val dateAddedEpochSeconds: Long,
    val dateModifiedEpochSeconds: Long,
    /** MediaStore's cached album art. Only for display: it is often a downscaled copy. */
    val artworkRef: ContentRef?,
) {
    /** The fields neither MediaStore nor the file has a value for. */
    val missingFields: Set<TrackField>
        get() = buildSet {
            if (artist.isNullOrBlank()) add(TrackField.Artist)
            if (album.isNullOrBlank()) add(TrackField.Album)
            if (albumArtist.isNullOrBlank()) add(TrackField.AlbumArtist)
            if (year == null) add(TrackField.Year)
            if (trackNumber == null) add(TrackField.TrackNumber)
            if (genre.isNullOrBlank()) add(TrackField.Genre)
        }

    val needsAttention: Boolean
        get() = missingFields.any { it in TrackField.Essential }

    val fileExtension: String
        get() = displayName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
}

/** Library-level fields, for sorting, filtering and health checks. */
enum class TrackField {
    Title,
    Artist,
    Album,
    AlbumArtist,
    Year,
    TrackNumber,
    Genre;

    companion object {
        /** Missing any of these makes a song show up under "Needs attention". */
        val Essential = setOf(Artist, Album, AlbumArtist, Year, TrackNumber)
    }
}
