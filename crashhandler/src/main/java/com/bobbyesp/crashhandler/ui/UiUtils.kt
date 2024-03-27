package com.bobbyesp.crashhandler.ui

import androidx.compose.ui.graphics.Color

object UiUtils {
    fun Color.applyAlpha(alpha: Float): Color {
        return this.copy(alpha = alpha)
    }
}