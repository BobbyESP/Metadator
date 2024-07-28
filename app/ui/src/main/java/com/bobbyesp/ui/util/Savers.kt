package com.bobbyesp.ui.util

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.text.input.TextFieldValue

object TextFieldValueSaver : Saver<TextFieldValue, String> {
    override fun restore(value: String): TextFieldValue {
        return TextFieldValue(value)
    }

    override fun SaverScope.save(value: TextFieldValue): String {
        return value.text
    }
}