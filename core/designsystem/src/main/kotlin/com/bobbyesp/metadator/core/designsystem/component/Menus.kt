/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.designsystem.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/** One thing a menu can do. */
@Immutable
class MenuAction(
    val text: String,
    val icon: ImageVector,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

/**
 * A menu in Material's expressive form: a floating group whose items take its corners. Groups are
 * the unit, so a menu with two kinds of things (choices, then an action) is two groups with a gap.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PopupMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenuPopup(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        content = content,
    )
}

/** The group at [index] of [count] in a [PopupMenu]. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PopupMenuGroup(index: Int = 0, count: Int = 1, content: @Composable ColumnScope.() -> Unit) {
    DropdownMenuGroup(shapes = MenuDefaults.groupShape(index, count), content = content)
}

/** A menu of plain actions. Choosing one closes the menu first, so it never outlives its screen. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ActionMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    actions: List<MenuAction>,
    modifier: Modifier = Modifier,
) {
    PopupMenu(expanded, onDismissRequest, modifier) {
        PopupMenuGroup {
            actions.forEachIndexed { index, action ->
                DropdownMenuItem(
                    onClick = {
                        onDismissRequest()
                        action.onClick()
                    },
                    text = { Text(action.text) },
                    shape = MenuDefaults.itemShape(index, actions.size).shape,
                    leadingIcon = { Icon(action.icon, contentDescription = null) },
                    enabled = action.enabled,
                )
            }
        }
    }
}
