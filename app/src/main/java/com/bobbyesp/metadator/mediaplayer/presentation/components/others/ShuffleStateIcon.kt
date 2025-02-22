package com.bobbyesp.metadator.mediaplayer.presentation.components.others

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.ShuffleOn
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.bobbyesp.mediaplayer.R

@Composable
fun ShuffleStateIcon(modifier: Modifier = Modifier, isShuffleEnabled: Boolean) {
    val icon = if (isShuffleEnabled) Icons.Rounded.ShuffleOn else Icons.Rounded.Shuffle
    val description =
        stringResource(
            id = if (isShuffleEnabled) R.string.action_shuffle_on else R.string.action_shuffle_off
        )

    Icon(imageVector = icon, contentDescription = description, modifier = modifier)
}
