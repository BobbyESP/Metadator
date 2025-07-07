package com.bobbyesp.metadator.tageditor.presentation.state

import androidx.annotation.StringRes

class StringFieldState(
    key: String,
    @StringRes labelRes: Int,
    initial: String? = "",
    validator: (String) -> Boolean = { it.isNotBlank() },
) : FieldState<String>(key, labelRes, initial ?: "", validator)
