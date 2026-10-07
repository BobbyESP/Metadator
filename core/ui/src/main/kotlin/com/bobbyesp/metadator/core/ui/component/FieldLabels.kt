/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
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
