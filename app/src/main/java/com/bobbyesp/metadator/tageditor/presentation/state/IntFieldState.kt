package com.bobbyesp.metadator.tageditor.presentation.state

class IntFieldState(
    key: String,
    labelRes: Int,
    original: Int
) : FieldState<Int>(key, labelRes, original, original) {
    override val errorMessageRes: Int? = null
}