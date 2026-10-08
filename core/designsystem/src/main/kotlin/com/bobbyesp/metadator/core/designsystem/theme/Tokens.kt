/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.designsystem.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** The 8 dp spacing scale. Screens use these instead of numbers. */
object Spacing {
    val extraSmall = 4.dp
    val small = 8.dp
    val medium = 12.dp
    val large = 16.dp
    val extraLarge = 24.dp
    val huge = 32.dp

    /** The side margin of a screen's content. */
    val screen = 16.dp

    /** Room left at the bottom of a list for whatever floats over it (toolbar, mini player). */
    val floatingClearance = 112.dp

    /** Readable width for forms and settings on wide windows. */
    val contentMaxWidth = 720.dp
}

/** Shapes for grouped lists: rounded outer corners, tight inner ones, as Material groups rows. */
object GroupShapes {
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    private val top: RoundedCornerShape
        @Composable
        get() =
            RoundedCornerShape(
                topStart = shapes.largeIncreased.topStart,
                topEnd = shapes.largeIncreased.topEnd,
                bottomStart = shapes.extraSmall.bottomStart,
                bottomEnd = shapes.extraSmall.bottomEnd,
            )

    private val middle: RoundedCornerShape
        @Composable get() = RoundedCornerShape(shapes.extraSmall.topStart)

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    private val bottom: RoundedCornerShape
        @Composable
        get() =
            RoundedCornerShape(
                topStart = shapes.extraSmall.topStart,
                topEnd = shapes.extraSmall.topEnd,
                bottomStart = shapes.largeIncreased.bottomStart,
                bottomEnd = shapes.largeIncreased.bottomEnd,
            )

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    private val single: RoundedCornerShape
        @Composable get() = RoundedCornerShape(shapes.largeIncreased.topStart)

    /** The shape of the item at [index] of [count] in a group. */
    @Composable
    fun itemShape(index: Int, count: Int): RoundedCornerShape =
        when {
            count == 1 -> single
            index == 0 -> top
            index == count - 1 -> bottom
            else -> middle
        }

    /**
     * [itemShape] for a group that has columns too: a corner is round where it is one of the
     * group's own, and tight where it meets another cell.
     *
     * @param first whether the cell is in the group's first row, and [last] in its last one
     * @param start whether it is the first cell of its row, and [end] the last one
     */
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    fun cellShape(
        first: Boolean = false,
        last: Boolean = false,
        start: Boolean = true,
        end: Boolean = true,
    ): RoundedCornerShape {
        val outer = shapes.largeIncreased.topStart
        val inner = shapes.extraSmall.topStart
        fun corner(isOuter: Boolean): CornerSize = if (isOuter) outer else inner
        return RoundedCornerShape(
            topStart = corner(first && start),
            topEnd = corner(first && end),
            bottomEnd = corner(last && end),
            bottomStart = corner(last && start),
        )
    }

    /**
     * [shape] at rest, and round on every side while [focused]: the item of a group being worked on
     * comes out of it by its shape, as Material's selected list items do. For what is not a list
     * item and so gets none of [listItemShapes]: a text field.
     */
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    fun focusedShape(shape: RoundedCornerShape, focused: Boolean): RoundedCornerShape {
        val fraction by
            animateFloatAsState(
                targetValue = if (focused) 1f else 0f,
                animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                label = "GroupFocus",
            )
        if (fraction == 0f) return shape
        val density = LocalDensity.current
        val round = shapes.largeIncreased
        // The spring overshoots, and a corner cannot be less than square.
        fun corner(from: CornerSize, to: CornerSize): CornerSize {
            val rest = from.toPx(Size.Zero, density)
            val target = to.toPx(Size.Zero, density)
            return CornerSize((rest + (target - rest) * fraction).coerceAtLeast(0f))
        }
        return RoundedCornerShape(
            topStart = corner(shape.topStart, round.topStart),
            topEnd = corner(shape.topEnd, round.topEnd),
            bottomEnd = corner(shape.bottomEnd, round.bottomEnd),
            bottomStart = corner(shape.bottomStart, round.bottomStart),
        )
    }

    /** [itemShape] with the list's pressed, selected and focused shapes on top. */
    @Composable
    fun listItemShapes(index: Int, count: Int): ListItemShapes =
        ListItemDefaults.shapes(shape = itemShape(index, count))
}
