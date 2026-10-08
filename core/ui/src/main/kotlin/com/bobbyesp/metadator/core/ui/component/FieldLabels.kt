/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.bobbyesp.metadator.core.model.TrackField
import com.bobbyesp.metadator.core.ui.R
import com.bobbyesp.metadator.tags.api.TagField

/** The user-facing name of a tag; unknown keys show as they are in the file. */
@Composable
fun fieldLabel(key: String): String =
    when (TagField.forKey(key)) {
        TagField.Title -> stringResource(R.string.field_title)
        TagField.Artist -> stringResource(R.string.field_artist)
        TagField.Album -> stringResource(R.string.field_album)
        TagField.AlbumArtist -> stringResource(R.string.field_album_artist)
        TagField.Date -> stringResource(R.string.field_date)
        TagField.Genre -> stringResource(R.string.field_genre)
        TagField.TrackNumber -> stringResource(R.string.field_track)
        TagField.DiscNumber -> stringResource(R.string.field_disc)
        TagField.Composer -> stringResource(R.string.field_composer)
        TagField.Lyricist -> stringResource(R.string.field_lyricist)
        TagField.Conductor -> stringResource(R.string.field_conductor)
        TagField.Remixer -> stringResource(R.string.field_remixer)
        TagField.Performer -> stringResource(R.string.field_performer)
        TagField.Comment -> stringResource(R.string.field_comment)
        TagField.Lyrics -> stringResource(R.string.field_lyrics)
        TagField.Bpm -> stringResource(R.string.field_bpm)
        TagField.Isrc -> stringResource(R.string.field_isrc)
        TagField.Copyright -> stringResource(R.string.field_copyright)
        TagField.Label -> stringResource(R.string.field_label)
        TagField.Grouping -> stringResource(R.string.field_grouping)
        null -> key
    }

/**
 * Why a song needs attention, as a sentence: "Missing: album artist, year". Only the tags that make
 * it need it are named, in the order the editor has them. Null when none of them is missing.
 */
@Composable
fun missingTagsText(missing: Set<TrackField>): String? {
    val names =
        TrackField.entries
            .filter { it in missing && it in TrackField.Essential }
            .map {
                stringResource(
                    when (it) {
                        TrackField.Artist -> R.string.missing_artist
                        TrackField.Album -> R.string.missing_album
                        TrackField.AlbumArtist -> R.string.missing_album_artist
                        TrackField.Year -> R.string.missing_year
                        TrackField.TrackNumber -> R.string.missing_track_number
                        // Never essential, so never asked for.
                        TrackField.Title,
                        TrackField.Genre -> return@map ""
                    }
                )
            }
    if (names.isEmpty()) return null
    return pluralStringResource(R.plurals.missing_tags, names.size, names.joinToString(", "))
}
