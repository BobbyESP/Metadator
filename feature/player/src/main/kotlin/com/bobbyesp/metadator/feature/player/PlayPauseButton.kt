/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.feature.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp

/**
 * Play and pause, in both players. A toggle only so that its shape can say what is happening: round
 * while paused, squared off while playing. It stays in the primary color either way, because it is
 * the main thing to press whichever state it is in.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun PlayPauseButton(
    isPlaying: Boolean,
    onToggle: () -> Unit,
    iconSize: Dp,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource? = null,
) {
    val colors = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    FilledIconToggleButton(
        checked = isPlaying,
        onCheckedChange = { onToggle() },
        shapes = IconButtonDefaults.toggleableShapes(),
        modifier = modifier,
        colors =
            IconButtonDefaults.filledIconToggleButtonColors(
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
                checkedContainerColor = colors.primary,
                checkedContentColor = colors.onPrimary,
            ),
        interactionSource = interactionSource,
    ) {
        AnimatedContent(
            targetState = isPlaying,
            transitionSpec = {
                (scaleIn(motion.fastSpatialSpec(), initialScale = 0.6f) +
                    fadeIn(motion.fastEffectsSpec())) togetherWith
                    (scaleOut(motion.fastEffectsSpec(), targetScale = 0.6f) +
                        fadeOut(motion.fastEffectsSpec()))
            },
            label = "PlayPause",
        ) { playing ->
            Icon(
                if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = stringResource(if (playing) R.string.pause else R.string.play),
                modifier = Modifier.size(iconSize),
            )
        }
    }
}
