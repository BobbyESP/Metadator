package com.bobbyesp.ui.components.others

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LoadingPlaceholder(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    colorful: Boolean,
) {

  val color = if (colorful) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
  val onColor =
      if (colorful) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
  val elevation = if (colorful) 0.dp else 8.dp

  Surface(tonalElevation = elevation, color = color, modifier = modifier) {
    if (progress == null) {
      CircularProgressIndicator(
          modifier = Modifier.fillMaxSize().padding(8.dp),
          color = onColor,
      )
    } else {
      CircularProgressIndicator(
          progress = { progress },
          modifier = Modifier.fillMaxSize().padding(8.dp),
          color = onColor,
      )
    }
  }
}
