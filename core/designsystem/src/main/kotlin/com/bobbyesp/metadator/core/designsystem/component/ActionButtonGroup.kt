/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.designsystem.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ButtonGroupMenuState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * One of the things an [ActionButtonGroup] does.
 *
 * @param labeled whether it shows its label where there is room for it; one that does not is only
 *   ever its icon, and [label] is then what a screen reader says
 */
@Immutable
class GroupAction(
    val label: String,
    val icon: ImageVector,
    val labeled: Boolean = true,
    val onClick: () -> Unit,
)

/**
 * What to do with everything a screen shows, as Material's expressive button group: one row of tall
 * buttons that fills the width, where the one being pressed grows and squeezes its neighbours.
 *
 * The [primary] action is filled and always keeps its label. The [secondary] ones are tonal, and
 * fall back to their icon where the row is too narrow for every label: a label cut short says less
 * than an icon. For the header of a collection; a screen whose actions float over a list uses
 * [FloatingActionToolbar].
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ActionButtonGroup(
    primary: GroupAction,
    secondary: List<GroupAction>,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val roomForLabels = maxWidth >= LabelsMinWidth
        ButtonGroup(
            // Never shown at the widths the labels are dropped for, but the group asks for one.
            overflowIndicator = { menuState ->
                ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
            },
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            (listOf(primary) + secondary).forEachIndexed { index, action ->
                val isPrimary = index == 0
                val showLabel = isPrimary || (action.labeled && roomForLabels)
                customItem(
                    buttonGroupContent = {
                        val interactions = remember { MutableInteractionSource() }
                        if (showLabel) {
                            LabeledAction(
                                action = action,
                                filled = isPrimary,
                                interactions = interactions,
                                modifier = Modifier.weight(1f).animateWidth(interactions),
                            )
                        } else {
                            IconAction(
                                action = action,
                                interactions = interactions,
                                modifier = Modifier.animateWidth(interactions),
                            )
                        }
                    },
                    menuContent = { menuState -> MenuAction(action, menuState) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LabeledAction(
    action: GroupAction,
    filled: Boolean,
    interactions: MutableInteractionSource,
    modifier: Modifier = Modifier,
) {
    val shapes = ButtonDefaults.shapesFor(Height)
    val padding = ButtonDefaults.contentPaddingFor(Height, hasStartIcon = true)
    val content: @Composable () -> Unit = {
        Icon(
            action.icon,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.iconSizeFor(Height)),
        )
        Spacer(Modifier.width(ButtonDefaults.iconSpacingFor(Height)))
        Text(
            action.label,
            style = ButtonDefaults.textStyleFor(Height),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    if (filled) {
        Button(
            onClick = action.onClick,
            shapes = shapes,
            modifier = modifier.heightIn(min = Height),
            contentPadding = padding,
            interactionSource = interactions,
        ) {
            content()
        }
    } else {
        FilledTonalButton(
            onClick = action.onClick,
            shapes = shapes,
            modifier = modifier.heightIn(min = Height),
            contentPadding = padding,
            interactionSource = interactions,
        ) {
            content()
        }
    }
}

/** Square where the labeled buttons are round: told apart by shape, as an expressive group is. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun IconAction(
    action: GroupAction,
    interactions: MutableInteractionSource,
    modifier: Modifier = Modifier,
) {
    FilledTonalIconButton(
        onClick = action.onClick,
        shapes =
            IconButtonDefaults.shapes(
                shape = IconButtonDefaults.mediumSquareShape,
                pressedShape = IconButtonDefaults.mediumPressedShape,
            ),
        modifier = modifier.size(IconButtonDefaults.mediumContainerSize()),
        interactionSource = interactions,
    ) {
        Icon(
            action.icon,
            contentDescription = action.label,
            modifier = Modifier.size(IconButtonDefaults.mediumIconSize),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MenuAction(action: GroupAction, menuState: ButtonGroupMenuState) {
    DropdownMenuItem(
        text = { Text(action.label) },
        leadingIcon = { Icon(action.icon, contentDescription = null) },
        onClick = {
            menuState.dismiss()
            action.onClick()
        },
    )
}

/** Material's medium button: tall enough to be the first thing on the screen under its title. */
private val Height = ButtonDefaults.MediumContainerHeight

/**
 * The width from which two labeled buttons and an icon fit with their labels whole, in the app's
 * longer language. Below it only the primary action keeps its label.
 */
private val LabelsMinWidth = 400.dp
