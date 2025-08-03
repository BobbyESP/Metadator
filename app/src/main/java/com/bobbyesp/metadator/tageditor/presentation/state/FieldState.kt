package com.bobbyesp.metadator.tageditor.presentation.state

sealed class FieldState<T>(
    val key: String,
    val labelRes: Int,
    val original: T,
    var current: T
) {
    val isModified: Boolean
        get() = original != current

    abstract val errorMessageRes: Int?

    private var _error: Int? = null
    val errorMessage: Int?
        get() = _error

    fun validate(validator: (T) -> Int?): FieldState<T> = apply {
        _error = validator(current)
    }
}