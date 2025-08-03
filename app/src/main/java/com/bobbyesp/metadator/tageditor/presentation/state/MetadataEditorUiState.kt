package com.bobbyesp.metadator.tageditor.presentation.state

import com.bobbyesp.metadator.R
import com.bobbyesp.metadator.tageditor.domain.AudioEditableMetadata

data class MetadataEditorUiState(
    val fields: List<FieldState<*>> = emptyList()
) {

    fun loadFrom(editable: AudioEditableMetadata): MetadataEditorUiState =
        copy(
            fields = listOf(
                StringFieldState("TITLE", R.string.title, editable.title),
                StringFieldState("ARTIST", R.string.artist, editable.artist),
                StringFieldState("ALBUM", R.string.album, editable.album),
                IntFieldState("TRACKNUMBER", R.string.track_number, editable.trackNumber),
                IntFieldState("DISCNUMBER", R.string.disc_number, editable.discNumber),
                StringFieldState("DATE", R.string.date, editable.date),
                StringFieldState("GENRE", R.string.genre, editable.genre),
                StringFieldState("COMMENT", R.string.comment, editable.comment),
                StringFieldState("LYRICS", R.string.lyrics, editable.lyrics),
            )
        )

    fun toDomain(): AudioEditableMetadata {
        val map = fields.associate { it.key to it.current.toString() }
        return AudioEditableMetadata.fromMap(map)
    }

    val modifiedKeys: Set<String>
        get() = fields.filter { it.isModified }.map { it.key }.toSet()

    /** Update a single field's current value based on its key */
    fun updateField(key: String, value: String): MetadataEditorUiState =
        copy(fields = fields.map { field ->
            if (field.key == key) {
                when (field) {
                    is StringFieldState -> StringFieldState(field.key, field.labelRes, field.current).apply { current = value }
                    is IntFieldState -> IntFieldState(field.key, field.labelRes, field.original).apply { current = value.toIntOrNull() ?: original }
                }
            } else field
        })

    /** Reset the original values to the current ones, clearing modification state */
    fun clearModified(): MetadataEditorUiState =
        copy(fields = fields.map { field ->
            when (field) {
                is StringFieldState -> StringFieldState(field.key, field.labelRes, field.current)
                is IntFieldState -> IntFieldState(field.key, field.labelRes, field.current)
            }
        })
}