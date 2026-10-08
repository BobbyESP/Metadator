/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.lookup.api

/** An online catalogue songs can be matched against. */
interface MetadataProvider {
    /** Stable id, as stored in the settings. */
    val id: String

    val displayName: String

    /** Whether results come with cover art worth offering. */
    val providesArtwork: Boolean

    suspend fun search(query: LookupQuery): LookupResult
}

data class LookupQuery(
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val durationMs: Long? = null,
) {
    val isBlank: Boolean
        get() = title.isBlank() && artist.isNullOrBlank() && album.isNullOrBlank()
}

sealed interface LookupResult {
    data class Success(val candidates: List<LookupCandidate>) : LookupResult

    data object Offline : LookupResult

    data object RateLimited : LookupResult

    data class Failed(val message: String) : LookupResult
}

/**
 * One match proposed by a provider. Plain values: mapping them to tags is the domain's job, so
 * every provider is mapped the same way.
 */
data class LookupCandidate(
    val providerId: String,
    val providerName: String,
    /** The provider's id for the recording, unique within that provider. */
    val id: String,
    val title: String,
    val artists: List<String>,
    val album: String? = null,
    val albumArtists: List<String> = emptyList(),
    /** As precise as the provider knows it: "2019", "2019-05" or "2019-05-17". */
    val date: String? = null,
    val trackNumber: Int? = null,
    val trackTotal: Int? = null,
    val discNumber: Int? = null,
    val discTotal: Int? = null,
    val genres: List<String> = emptyList(),
    val isrc: String? = null,
    val label: String? = null,
    val durationMs: Long? = null,
    val artworkUrl: String? = null,
    val artworkThumbnailUrl: String? = null,
    /** Provider ids worth keeping in the file, by tag key (MUSICBRAINZ_TRACKID…). */
    val identifiers: Map<String, String> = emptyMap(),
    /** How well this matches the query, from 0 to 1. Filled by [rank]. */
    val score: Double = 0.0,
) {
    val year: String?
        get() = date?.take(4)
}

/** Downloads cover art proposed by a provider. */
interface ArtworkDownloader {
    suspend fun download(url: String): DownloadedImage?
}

class DownloadedImage(val bytes: ByteArray, val mimeType: String)
