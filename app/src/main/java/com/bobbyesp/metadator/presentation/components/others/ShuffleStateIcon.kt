package com.bobbyesp.metadator.presentation.components.others

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.ShuffleOn
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

@Composable
fun ShuffleStateIcon(
    modifier: Modifier = Modifier,
    isShuffleEnabled: Boolean
) {
    when (isShuffleEnabled) {
        true -> {
            Icon(
                imageVector = Icons.Rounded.ShuffleOn,
                contentDescription = stringResource(id = com.bobbyesp.mediaplayer.R.string.action_shuffle_on),
                modifier = modifier
            )
        }

        false -> {
            Icon(
                imageVector = Icons.Rounded.Shuffle,
                contentDescription = stringResource(id = com.bobbyesp.mediaplayer.R.string.action_shuffle_off),
                modifier = modifier
            )
        }
    }
}