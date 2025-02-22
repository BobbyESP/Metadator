package com.bobbyesp.ui.ext

import androidx.compose.ui.graphics.Color

fun Color.applyAlpha(enabled: Boolean, alpha: Float = 0.62f): Color {
  return if (enabled) this else this.copy(alpha = alpha)
}
