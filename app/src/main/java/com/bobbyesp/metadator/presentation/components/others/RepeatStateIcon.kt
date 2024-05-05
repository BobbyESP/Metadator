package com.bobbyesp.metadator.presentation.components.others

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

@Composable
fun RepeatStateIcon(
    modifier: Modifier = Modifier,
    repeatMode: Int
) {
    when (repeatMode) {
        REPEAT_MODE_OFF -> {
            Icon(
                imageVector = Icons.Rounded.Repeat,
                contentDescription = stringResource(id = com.bobbyesp.mediaplayer.R.string.repeat_mode_off),
                modifier = modifier.alpha(0.5f)
            )
        }

        REPEAT_MODE_ONE -> {
            Icon(
                imageVector = Icons.Rounded.RepeatOne,
                contentDescription = stringResource(id = com.bobbyesp.mediaplayer.R.string.repeat_mode_one),
                modifier = modifier
            )
        }

        REPEAT_MODE_ALL -> {
            Icon(
                imageVector = Icons.Rounded.Repeat,
                contentDescription = stringResource(id = com.bobbyesp.mediaplayer.R.string.repeat_mode_all),
                modifier = modifier
            )
        }
    }
}