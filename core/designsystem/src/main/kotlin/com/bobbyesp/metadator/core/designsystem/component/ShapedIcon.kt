/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon

/**
 * An icon on one of Material's shapes, turning slowly: the app's way of saying "nothing here, and
 * nothing is stuck". The icon stays upright; only the shape under it turns.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ShapedIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 112.dp,
    polygon: RoundedPolygon = MaterialShapes.Cookie9Sided,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    turnMillis: Int = 12_000,
) {
    val rotation by
        rememberInfiniteTransition(label = "ShapedIcon")
            .animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec =
                    infiniteRepeatable(
                        tween(turnMillis, easing = LinearEasing),
                        RepeatMode.Restart,
                    ),
                label = "Rotation",
            )
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            Modifier.fillMaxSize()
                .graphicsLayer { rotationZ = rotation }
                .clip(polygon.toShape())
                .background(containerColor)
        )
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(0.43f),
            tint = contentColor,
        )
    }
}
