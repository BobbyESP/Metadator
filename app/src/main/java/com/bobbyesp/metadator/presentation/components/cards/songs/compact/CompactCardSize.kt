package com.bobbyesp.metadator.presentation.components.cards.songs.compact

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class CompactCardSize(val value: Dp) {
    SMALL(96.dp),
    MEDIUM(120.dp),
    LARGE(144.dp),
    EXTRA_LARGE(168.dp);

    companion object {

        fun Int.toCompactCardSize(): CompactCardSize =
            CompactCardSize.entries.first { it.ordinal == this }

        @Composable
        fun CompactCardSize.toShape(): CornerBasedShape = when (this) {
            SMALL -> MaterialTheme.shapes.small
            MEDIUM -> MaterialTheme.shapes.medium
            LARGE -> MaterialTheme.shapes.large
            EXTRA_LARGE -> MaterialTheme.shapes.extraLarge
        }
    }
}