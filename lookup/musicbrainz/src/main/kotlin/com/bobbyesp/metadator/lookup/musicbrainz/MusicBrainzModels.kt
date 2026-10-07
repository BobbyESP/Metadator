/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.lookup.musicbrainz

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable internal data class RecordingSearch(val recordings: List<Recording> = emptyList())

@Serializable
internal data class Recording(
    val id: String,
    val title: String,
    val length: Long? = null,
    @SerialName("first-release-date") val firstReleaseDate: String? = null,
    @SerialName("artist-credit") val artistCredit: List<ArtistCredit> = emptyList(),
    val releases: List<Release> = emptyList(),
    val isrcs: List<String> = emptyList(),
    val tags: List<Tag> = emptyList(),
)

@Serializable
internal data class ArtistCredit(
    val name: String,
    val joinphrase: String = "",
    val artist: Artist? = null,
)

@Serializable internal data class Artist(val id: String, val name: String)

@Serializable
internal data class Release(
    val id: String,
    val title: String,
    val status: String? = null,
    val date: String? = null,
    val country: String? = null,
    @SerialName("track-count") val trackCount: Int? = null,
    @SerialName("artist-credit") val artistCredit: List<ArtistCredit> = emptyList(),
    @SerialName("release-group") val releaseGroup: ReleaseGroup? = null,
    val media: List<Medium> = emptyList(),
)

@Serializable
internal data class ReleaseGroup(
    val id: String,
    @SerialName("primary-type") val primaryType: String? = null,
    @SerialName("secondary-types") val secondaryTypes: List<String> = emptyList(),
)

@Serializable
internal data class Medium(
    val position: Int? = null,
    val format: String? = null,
    @SerialName("track-count") val trackCount: Int? = null,
    @SerialName("track-offset") val trackOffset: Int? = null,
    val track: List<MediumTrack> = emptyList(),
)

@Serializable
internal data class MediumTrack(
    val id: String? = null,
    val number: String? = null,
    val title: String? = null,
    val length: Long? = null,
)

@Serializable internal data class Tag(val name: String, val count: Int = 0)
