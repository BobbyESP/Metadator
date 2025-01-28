package com.bobbyesp.ui.components.preferences

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed

@Composable
fun SettingSegmentOptions(
    title: String,
    supportingText: String,
    icon: ImageVector,
    options: List<SettingSegmentOption>,
    selectedOptionIndex: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(ShapeDefaults.Large),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            tint = MaterialTheme.colorScheme.onSurface,
            contentDescription = null
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            modifier = modifier
                .width(IntrinsicSize.Min)
                .height(IntrinsicSize.Min)
                .clip(ShapeDefaults.ExtraLarge)
                .background(color = MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            var midPoint by remember {
                mutableStateOf(0.dp)
            }
            val density = LocalDensity.current
            val capsuleOffset by animateDpAsState(
                targetValue = if (selectedOptionIndex == 0) 0.dp else midPoint,
                label = "capsule-offset-animation"
            )

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(.5f)
                    .offset(x = capsuleOffset)
                    .clip(ShapeDefaults.ExtraLarge)
                    .background(color = MaterialTheme.colorScheme.primary)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .onGloballyPositioned {
                        midPoint = with(density) { (it.size.width / 2).toDp() }
                    }
            ) {
                options.fastForEachIndexed { index, option ->
                    Icon(
                        imageVector = option.icon,
                        contentDescription = option.contentDescription,
                        tint = if (selectedOptionIndex == index) {
                            MaterialTheme.colorScheme.onPrimary
                        } else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(8.dp)
                            .clip(CircleShape)
                            .clickable {
                                option.onClick()
                            }
                    )
                }
            }
        }
    }
}