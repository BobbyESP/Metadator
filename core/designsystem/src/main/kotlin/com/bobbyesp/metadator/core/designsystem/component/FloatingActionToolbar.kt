/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.designsystem.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.bobbyesp.metadator.core.designsystem.theme.MetadatorBlurDefaults

/**
 * What a screen can do with what it shows, floating over its list: Material's vibrant toolbar, with
 * the one thing the screen is for as the button beside it.
 *
 * Lifted off the list by a blur halo rather than a shadow, which its caller draws around it
 * (`Modifier.blurHalo` with [FloatingActionToolbarDefaults.HaloShape]), since only the caller knows
 * what is beneath and whether the toolbar is inside an animation the halo must stay out of. Where
 * the device cannot draw a halo, the toolbar keeps Material's shadows.
 *
 * @param primaryAction the content of the button, typically an icon
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FloatingActionToolbar(
    onPrimaryAction: () -> Unit,
    primaryAction: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit,
) {
    val flat = MetadatorBlurDefaults.isHaloSupported
    val colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors()
    HorizontalFloatingToolbar(
        expanded = true,
        modifier = modifier,
        colors = colors,
        expandedShadowElevation =
            if (flat) 0.dp else FloatingToolbarDefaults.ContainerExpandedElevationWithFab,
        floatingActionButton = {
            if (flat) {
                // The toolbar's own button, which cannot be told to cast no shadow.
                FloatingActionButton(
                    onClick = onPrimaryAction,
                    modifier = Modifier.fillMaxSize(),
                    containerColor = colors.fabContainerColor,
                    contentColor = colors.fabContentColor,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                    content = primaryAction,
                )
            } else {
                FloatingToolbarDefaults.VibrantFloatingActionButton(
                    onClick = onPrimaryAction,
                    content = primaryAction,
                )
            }
        },
        content = actions,
    )
}

object FloatingActionToolbarDefaults {
    /** The outline of the toolbar and its button together, for the halo around the pair. */
    val HaloShape: Shape = CircleShape
}
