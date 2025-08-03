package com.bobbyesp.ui.components.button.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.NavigationDrawerItemColors
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun HorizontalFloatingAppBarItem(
    label: @Composable () -> Unit,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    badge: (@Composable () -> Unit)? = null,
    shape: Shape = CircleShape,
    colors: NavigationDrawerItemColors = NavigationDrawerItemDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val animatedSurfaceColor by
        animateColorAsState(
            targetValue = colors.containerColor(selected).value,
            label = "Animated Surface Color",
        )

    Surface(
        onClick = onClick,
        modifier = modifier.semantics { role = Role.Companion.Tab },
        shape = shape,
        color = animatedSurfaceColor,
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier.Companion.padding(8.dp),
            verticalAlignment = Alignment.Companion.CenterVertically,
        ) {
            icon?.let {
                Box(contentAlignment = Alignment.Companion.Center) {
                    ProvideColor(colors.iconColor(selected).value) { icon() }
                    if (badge != null) {
                        Box(
                            modifier =
                                Modifier.Companion.align(Alignment.Companion.BottomEnd)
                                    .padding(bottom = 4.dp, end = 4.dp)
                        ) {
                            ProvideColor(colors.badgeColor(selected).value) { badge() }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = expanded && !selected, modifier = Modifier.Companion) {
                ProvideColor(colors.textColor(selected).value) { label() }
            }
        }
    }
}

@Composable
private fun ProvideColor(color: Color, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalContentColor provides color, content = content)
}

@Preview
@Composable
private fun HorizontalFloatingAppBarItemPreview() {
    var expanded by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf(true) }
    HorizontalFloatingAppBarItem(
        label = {
            Text(
                modifier = Modifier.Companion.padding(horizontal = 12.dp),
                text = "Home",
                fontWeight = FontWeight.Companion.Bold,
            )
        },
        selected = selected,
        expanded = expanded,
        onClick = { selected = !selected },
        icon = { Icon(imageVector = Icons.Rounded.Home, contentDescription = "Home") },
        badge = null,
    )
}
