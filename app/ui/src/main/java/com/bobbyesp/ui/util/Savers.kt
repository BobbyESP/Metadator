package com.bobbyesp.ui.util

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
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

object AnimatableSaver : Saver<Animatable<Float, AnimationVector1D>, Float> {
  override fun restore(value: Float): Animatable<Float, AnimationVector1D>? {
    return Animatable(value)
  }

  override fun SaverScope.save(value: Animatable<Float, AnimationVector1D>): Float? {
    return value.value
  }
}
