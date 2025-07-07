package com.bobbyesp.metadator.tageditor.presentation.state

import androidx.annotation.StringRes

class IntFieldState(
    key: String,
    @StringRes labelRes: Int,
    initial: Int? = 0,
    validator: (Int) -> Boolean = { it >= 0 },
) : FieldState<Int>(key, labelRes, initial ?: 0, validator)
