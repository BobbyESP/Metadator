package com.bobbyesp.metadator.core.presentation.common

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.bobbyesp.metadator.R

@Immutable
enum class DestinationInfo(
    val icon: ImageVector,
    @StringRes val title: Int,
) {
  HOME(icon = Icons.Rounded.Home, title = R.string.home),
  MEDIAPLAYER(icon = Icons.Rounded.PlayArrow, title = R.string.mediaplayer);

  companion object {
    fun fromRoute(route: Route): DestinationInfo? {
      return when (route) {
        is Route.MetadatorNavigator -> HOME
        is Route.MediaplayerNavigator -> MEDIAPLAYER
        else -> null
      }
    }
  }
}
