/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.designsystem.component

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow

/**
 * Something a list is narrowed down by, or one option of a few: Material's filter chip in its
 * expressive form, which changes shape as it is pressed and once chosen, and ticks under the
 * finger.
 *
 * Chosen, it shows a check where its [icon] was: the color alone would not say it to everyone.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ToggleChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val haptics = LocalHapticFeedback.current
    val leading = if (selected) Icons.Rounded.Check else icon
    FilterChip(
        selected = selected,
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            onClick()
        },
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        shapes = FilterChipDefaults.shapes(),
        modifier = modifier,
        leadingIcon =
            leading?.let {
                {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                    )
                }
            },
    )
}

/**
 * A chip that does something rather than being chosen: it never looks selected, so it is not
 * mistaken for a filter that is on.
 *
 * @param opensMenu whether it opens a menu, which an arrow at its end then says
 */
@Composable
fun ActionChip(
    onClick: () -> Unit,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    opensMenu: Boolean = false,
) {
    AssistChip(
        onClick = onClick,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        modifier = modifier,
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(AssistChipDefaults.IconSize),
            )
        },
        trailingIcon =
            if (opensMenu) {
                {
                    Icon(
                        imageVector = Icons.Rounded.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                    )
                }
            } else null,
    )
}
