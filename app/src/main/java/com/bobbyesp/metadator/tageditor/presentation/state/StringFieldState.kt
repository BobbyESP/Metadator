package com.bobbyesp.metadator.tageditor.presentation.state

class StringFieldState(
    key: String,
    labelRes: Int,
    original: String
) : FieldState<String>(key, labelRes, original, original) {
    override val errorMessageRes: Int? = null
}