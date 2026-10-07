package com.bobbyesp.metadator.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.runtime.Composable
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

    /** [itemShape] with the list's pressed, selected and focused shapes on top. */
    @Composable
    fun listItemShapes(index: Int, count: Int): ListItemShapes =
        ListItemDefaults.shapes(shape = itemShape(index, count))
}
