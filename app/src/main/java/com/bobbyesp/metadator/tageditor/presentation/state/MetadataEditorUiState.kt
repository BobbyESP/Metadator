package com.bobbyesp.metadator.tageditor.presentation.state

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.bobbyesp.metadator.R
import com.bobbyesp.metadator.tageditor.domain.AudioEditableMetadata

data class MetadataEditorUiState(
    val fields: SnapshotStateList<FieldState<*>> = mutableStateListOf()
) {
    fun loadFrom(domain: AudioEditableMetadata) {
        fields.clear()
        fields += StringFieldState("TITLE", R.string.title, domain.title)
        fields += StringFieldState("ARTIST", R.string.artist, domain.artist)
        fields += StringFieldState("ALBUM", R.string.album, domain.album)
        fields += IntFieldState("TRACKNUMBER", R.string.track_number, domain.trackNumber)
        fields += IntFieldState("DISCNUMBER", R.string.disc_number, domain.discNumber)
        fields += StringFieldState("DATE", R.string.date, domain.date)
        fields += StringFieldState("GENRE", R.string.genre, domain.genre)
        fields += StringFieldState("COMMENT", R.string.comment, domain.comment)
        fields += StringFieldState("LYRICS", R.string.lyrics, domain.lyrics) { it.length <= 5000 }
    }

    fun toDomain(): AudioEditableMetadata =
        AudioEditableMetadata(
            title = (fields.first { it.key == "TITLE" } as StringFieldState).value,
            artist = (fields.first { it.key == "ARTIST" } as StringFieldState).value,
            album = (fields.first { it.key == "ALBUM" } as StringFieldState).value,
            trackNumber = (fields.first { it.key == "TRACKNUMBER" } as IntFieldState).value,
            discNumber = (fields.first { it.key == "DISCNUMBER" } as IntFieldState).value,
            date = (fields.first { it.key == "DATE" } as StringFieldState).value,
            genre = (fields.first { it.key == "GENRE" } as StringFieldState).value,
            comment = (fields.first { it.key == "COMMENT" } as StringFieldState).value,
            lyrics = (fields.first { it.key == "LYRICS" } as StringFieldState).value,
        )
}
