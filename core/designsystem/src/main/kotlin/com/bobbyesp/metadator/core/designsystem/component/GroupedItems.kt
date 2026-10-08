/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.bobbyesp.metadator.core.designsystem.theme.GroupShapes

object GroupedItemDefaults {
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    fun colors(): ListItemColors =
        ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        )
}

/** An icon in a tinted circle, leading a grouped row. */
@Composable
fun ItemIcon(icon: ImageVector, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.size(40.dp),
        shape = CircleShape,
        color = if (enabled) colors.primaryContainer else colors.onSurface.copy(alpha = 0.12f),
        contentColor =
            if (enabled) colors.onPrimaryContainer else colors.onSurface.copy(alpha = 0.38f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
        }
    }
}

/** A row that opens something: a settings page, a picker. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NavigationItem(
    title: String,
    supportingText: String?,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shapes: ListItemShapes = GroupShapes.listItemShapes(0, 1),
    showChevron: Boolean = true,
) {
    SegmentedListItem(
        onClick = onClick,
        shapes = shapes,
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = { ItemIcon(icon) },
        trailingContent =
            if (showChevron) {
                { Icon(Icons.Rounded.ChevronRight, contentDescription = null) }
            } else null,
        supportingContent = supportingText?.let { { Text(it) } },
        colors = GroupedItemDefaults.colors(),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLargeEmphasized)
    }
}

/** A setting turned on and off. The whole row is the control; the switch only shows it. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SwitchItem(
    title: String,
    supportingText: String?,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shapes: ListItemShapes = GroupShapes.listItemShapes(0, 1),
) {
    SegmentedListItem(
        onClick = { onCheckedChange(!checked) },
        shapes = shapes,
        modifier =
            modifier.semantics {
                role = Role.Switch
                toggleableState = ToggleableState(checked)
            },
        enabled = enabled,
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = { ItemIcon(icon, enabled = enabled) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                thumbContent =
                    if (checked) {
                        { Icon(Icons.Rounded.Check, null, Modifier.size(SwitchDefaults.IconSize)) }
                    } else null,
            )
        },
        supportingContent = supportingText?.let { { Text(it) } },
        colors = GroupedItemDefaults.colors(),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLargeEmphasized)
    }
}

/** One choice among several. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RadioItem(
    title: String,
    supportingText: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shapes: ListItemShapes = GroupShapes.listItemShapes(0, 1),
) {
    SegmentedListItem(
        onClick = onClick,
        shapes = shapes,
        modifier = modifier.semantics { role = Role.RadioButton },
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = { RadioButton(selected = selected, onClick = null) },
        supportingContent = supportingText?.let { { Text(it) } },
        colors = GroupedItemDefaults.colors(),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
    }
}
