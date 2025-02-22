package com.bobbyesp.metadator.mediaplayer.presentation.components.buttons

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bobbyesp.metadator.R

@Composable
fun PlayPauseAnimatedButton(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState()
    val cornerRadius =
        animateDpAsState(
            targetValue = if (isPlaying || isPressed.value) 40.dp else 24.dp,
            label = "Animated button shape",
        )

    Surface(
        tonalElevation = 10.dp,
        modifier = modifier.clip(RoundedCornerShape(cornerRadius.value)),
    ) {
        Box(
            modifier =
                Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                    .size(72.dp)
                    .clip(RoundedCornerShape(cornerRadius.value))
                    .clickable(
                        interactionSource = interactionSource,
                        indication = ripple(bounded = false),
                        onClick = onClick,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            val icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow
            val contentDescription =
                stringResource(id = if (isPlaying) R.string.pause else R.string.play)

            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}
