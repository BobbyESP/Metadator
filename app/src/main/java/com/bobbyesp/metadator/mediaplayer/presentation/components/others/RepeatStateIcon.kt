package com.bobbyesp.metadator.mediaplayer.presentation.components.others

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import com.bobbyesp.mediaplayer.R

@Composable
fun RepeatStateIcon(modifier: Modifier = Modifier, repeatMode: Int) {
  val (icon, description, alpha) =
      when (repeatMode) {
        REPEAT_MODE_OFF -> Triple(Icons.Rounded.Repeat, R.string.repeat_mode_off, 0.5f)
        REPEAT_MODE_ONE -> Triple(Icons.Rounded.RepeatOne, R.string.repeat_mode_one, 1f)
        REPEAT_MODE_ALL -> Triple(Icons.Rounded.Repeat, R.string.repeat_mode_all, 1f)
        else -> Triple(Icons.Rounded.Repeat, R.string.repeat_mode_off, 0.5f)
      }

  Icon(
      imageVector = icon,
      contentDescription = stringResource(id = description),
      modifier = modifier.alpha(alpha))
}
