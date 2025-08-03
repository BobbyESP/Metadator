package com.bobbyesp.ui.components.bottomsheet.draggable

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Stable
class DraggableBottomSheetState(
    draggableState: DraggableState,
    private val coroutineScope: CoroutineScope,
    private val animatable: Animatable<Dp, AnimationVector1D>,
    private val onAnchorChanged: (DraggableBottomSheetAnchor) -> Unit,
    val collapsedBound: Dp,
) : DraggableState by draggableState {
    val dismissedBound: Dp
        get() = animatable.lowerBound!!

    val expandedBound: Dp
        get() = animatable.upperBound!!

    val value by animatable.asState()

    val isDismissed by derivedStateOf { value == animatable.lowerBound!! }

    val isCollapsed by derivedStateOf { value == collapsedBound }

    val isExpanded by derivedStateOf { value == animatable.upperBound }

    val progress by derivedStateOf {
        1f -
            (animatable.upperBound!! - animatable.value) /
                (animatable.upperBound!! - collapsedBound)
    }

    fun collapse(animationSpec: AnimationSpec<Dp>) {
        onAnchorChanged(DraggableBottomSheetAnchor.COLLAPSED)
        coroutineScope.launch { animatable.animateTo(collapsedBound, animationSpec) }
    }

    fun expand(animationSpec: AnimationSpec<Dp>) {
        onAnchorChanged(DraggableBottomSheetAnchor.EXPANDED)
        coroutineScope.launch { animatable.animateTo(animatable.upperBound!!, animationSpec) }
    }

    private fun collapse() {
        collapse(SpringSpec())
    }

    private fun expand() {
        expand(SpringSpec())
    }

    fun collapseSoft() {
        collapse(spring(stiffness = Spring.StiffnessMediumLow))
    }

    fun expandSoft() {
        expand(spring(stiffness = Spring.StiffnessMediumLow))
    }

    fun dismiss() {
        onAnchorChanged(DraggableBottomSheetAnchor.DISMISSED)
        coroutineScope.launch { animatable.animateTo(animatable.lowerBound!!) }
    }

    fun snapTo(value: Dp) {
        coroutineScope.launch { animatable.snapTo(value) }
    }

    fun performFling(velocity: Float, onDismiss: (() -> Unit)?) {
        if (velocity > 250) {
            expand()
        } else if (velocity < -250) {
            if (value < collapsedBound && onDismiss != null) {
                dismiss()
                onDismiss.invoke()
            } else {
                collapse()
            }
        } else {
            val l0 = dismissedBound
            val l1 = (collapsedBound - dismissedBound) / 2
            val l2 = (expandedBound - collapsedBound) / 2
            val l3 = expandedBound

            when (value) {
                in l0..l1 -> {
                    if (onDismiss != null) {
                        dismiss()
                        onDismiss.invoke()
                    } else {
                        collapse()
                    }
                }

                in l1..l2 -> collapse()
                in l2..l3 -> expand()
                else -> Unit
            }
        }
    }

    val preUpPostDownNestedScrollConnection
        get() =
            object : NestedScrollConnection {
                var isTopReached = false

                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (isExpanded && available.y < 0) {
                        isTopReached = false
                    }

                    return if (
                        isTopReached &&
                            available.y < 0 &&
                            source == NestedScrollSource.Companion.UserInput
                    ) {
                        dispatchRawDelta(available.y)
                        available
                    } else {
                        Offset.Companion.Zero
                    }
                }

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (!isTopReached) {
                        isTopReached = consumed.y == 0f && available.y > 0
                    }

                    return if (isTopReached && source == NestedScrollSource.Companion.UserInput) {
                        dispatchRawDelta(available.y)
                        available
                    } else {
                        Offset.Companion.Zero
                    }
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    return if (isTopReached) {
                        val velocity = -available.y
                        performFling(velocity, null)

                        available
                    } else {
                        Velocity.Companion.Zero
                    }
                }

                override suspend fun onPostFling(
                    consumed: Velocity,
                    available: Velocity,
                ): Velocity {
                    isTopReached = false
                    return Velocity.Companion.Zero
                }
            }
}
