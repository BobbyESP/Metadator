/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.designsystem.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Three bars that dance while a song plays and rest when it is paused: which row is the one you are
 * hearing, and whether you are hearing it, without reading anything.
 */
@Composable
fun PlayingBars(
    playing: Boolean,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
) {
    val transition = rememberInfiniteTransition(label = "PlayingBars")
    // Three periods with no common beat, so the bars never fall into step.
    val heights = BarPeriods.map { period ->
        transition.animateFloat(
            initialValue = 0.25f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(tween(period, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "Bar$period",
        )
    }
    val liveliness by animateFloatAsState(if (playing) 1f else 0f, label = "Liveliness")

    Canvas(modifier.size(20.dp)) {
        val barWidth = size.width / (BarPeriods.size * 2 - 1)
        heights.forEachIndexed { index, height ->
            val fraction = RestingHeight + (height.value - RestingHeight) * liveliness
            val barHeight = size.height * fraction
            drawRoundRect(
                color = color,
                topLeft = Offset(barWidth * index * 2, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2),
            )
        }
    }
}

private val BarPeriods = listOf(430, 610, 520)
private const val RestingHeight = 0.25f
