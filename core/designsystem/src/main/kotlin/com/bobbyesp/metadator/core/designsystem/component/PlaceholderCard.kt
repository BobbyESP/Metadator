package com.bobbyesp.metadator.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * What a screen shows instead of content: empty, needs a permission, or failed. The slowly
 * turning cookie shape is the expressive cue that the app is not frozen, only empty.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlaceholderCard(
    title: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    actionText: String? = null,
    actionIcon: ImageVector? = null,
    onAction: (() -> Unit)? = null,
    secondaryActionText: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val shapeColor = if (isError) colors.errorContainer else colors.primaryContainer
    val onShapeColor = if (isError) colors.onErrorContainer else colors.onPrimaryContainer

    val rotation by
        rememberInfiniteTransition(label = "PlaceholderRotation")
            .animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec =
                    infiniteRepeatable(
                        tween(if (isError) 20_000 else 12_000, easing = LinearEasing),
                        RepeatMode.Restart,
                    ),
                label = "Rotation",
            )

    Surface(
        modifier = modifier.widthIn(max = 480.dp).fillMaxWidth(),
        shape = MaterialTheme.shapes.largeIncreased,
        color = colors.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.size(112.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier =
                        Modifier.matchParentSize()
                            .graphicsLayer { rotationZ = rotation }
                            .clip(MaterialShapes.Cookie9Sided.toShape())
                            .background(shapeColor)
                )
                Icon(icon, contentDescription = null, modifier = Modifier.size(48.dp), tint = onShapeColor)
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmallEmphasized,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = colors.onSurfaceVariant,
                )
            }
            if (actionText != null && onAction != null) {
                val height = ButtonDefaults.MediumContainerHeight
                Button(
                    onClick = onAction,
                    modifier = Modifier.padding(top = 8.dp).heightIn(min = height),
                    shapes = ButtonDefaults.shapes(),
                    contentPadding = ButtonDefaults.contentPaddingFor(height, hasStartIcon = actionIcon != null),
                ) {
                    if (actionIcon != null) {
                        Icon(actionIcon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.iconSizeFor(height)))
                        Spacer(Modifier.width(ButtonDefaults.iconSpacingFor(height)))
                    }
                    Text(text = actionText, style = ButtonDefaults.textStyleFor(height))
                }
            }
            if (secondaryActionText != null && onSecondaryAction != null) {
                TextButton(onClick = onSecondaryAction, shapes = ButtonDefaults.shapes()) {
                    Text(secondaryActionText)
                }
            }
        }
    }
}
