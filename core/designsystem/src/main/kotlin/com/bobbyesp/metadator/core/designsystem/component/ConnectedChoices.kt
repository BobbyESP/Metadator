/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize

/** One of the options of a [ConnectedChoices]. */
@Immutable class Choice<T>(val value: T, val label: String, val icon: ImageVector)

/**
 * A few exclusive options as Material's connected button group: the chosen one rounds out and shows
 * its icon, its neighbours keep the tight inner corners. For what a tab row or a short radio list
 * would otherwise be.
 *
 * @param fill whether the options share the width; otherwise each is as wide as its label, for a
 *   row that scrolls.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> ConnectedChoices(
    choices: List<Choice<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    fill: Boolean = false,
) {
    val spatial = MaterialTheme.motionScheme.fastSpatialSpec<IntSize>()
    Row(
        modifier = modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        choices.forEachIndexed { index, choice ->
            val checked = choice.value == selected
            ToggleButton(
                checked = checked,
                onCheckedChange = { if (it) onSelect(choice.value) },
                modifier =
                    (if (fill) Modifier.weight(1f) else Modifier).semantics {
                        role = Role.RadioButton
                    },
                shapes =
                    when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        choices.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
            ) {
                AnimatedVisibility(
                    visible = checked,
                    enter = expandHorizontally(spatial) + fadeIn(),
                    exit = shrinkHorizontally(spatial) + fadeOut(),
                ) {
                    Row {
                        Icon(
                            choice.icon,
                            contentDescription = null,
                            modifier = Modifier.size(ToggleButtonDefaults.IconSize),
                        )
                        Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
                    }
                }
                Text(choice.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
