package com.bobbyesp.metadator.tageditor.presentation.state

import androidx.annotation.StringRes
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

sealed class FieldState<T : Any>(
    val key: String,
    @StringRes val labelRes: Int,
    initial: T,
    private val validator: (T) -> Boolean = { true },
) {
    var initialValue: T by mutableStateOf(initial)
    var value: T by mutableStateOf(initial)
    val isModified: Boolean
        get() = value != initialValue

    val errorMessage: String? by derivedStateOf { if (validator(value)) null else "Valor inválido" }
}
