package com.bobbyesp.ui.components.bottomsheet.draggable

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Bottom Sheet Improved version from [ViMusic](https://github.com/vfsfitvnm/ViMusic) */
@Composable
fun DraggableBottomSheet(
    state: DraggableBottomSheetState,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    onDismiss: (() -> Unit)? = null,
    collapsedContent: @Composable BoxScope.() -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
  Box(
      modifier =
          modifier
              .fillMaxSize()
              .offset {
                val y = (state.expandedBound - state.value).roundToPx().coerceAtLeast(0)
                IntOffset(x = 0, y = y)
              }
              .pointerInput(state) {
                val velocityTracker = VelocityTracker()

                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                      velocityTracker.addPointerInputChange(change)
                      state.dispatchRawDelta(dragAmount)
                    },
                    onDragCancel = {
                      velocityTracker.resetTracking()
                      state.snapTo(state.collapsedBound)
                    },
                    onDragEnd = {
                      val velocity = -velocityTracker.calculateVelocity().y
                      velocityTracker.resetTracking()
                      state.performFling(velocity, onDismiss)
                    })
              }
              .clip(
                  RoundedCornerShape(
                      topStart = if (!state.isExpanded) 8.dp else 0.dp,
                      topEnd = if (!state.isExpanded) 8.dp else 0.dp))
              .background(backgroundColor)) {
        if (!state.isCollapsed && !state.isDismissed) {
          BackHandler(onBack = state::collapseSoft)
        }

        if (!state.isCollapsed) {
          BoxWithConstraints(
              modifier =
                  Modifier.fillMaxSize().graphicsLayer {
                    alpha = ((state.progress - 0.25f) * 4).coerceIn(0f, 1f)
                  },
              content = content)
        }

        if (!state.isExpanded && (onDismiss == null || !state.isDismissed)) {
          Box(
              modifier =
                  Modifier.graphicsLayer { alpha = 1f - (state.progress * 4).coerceAtMost(1f) }
                      .clickable(
                          interactionSource = remember { MutableInteractionSource() },
                          indication = null,
                          onClick = state::expandSoft)
                      .fillMaxWidth()
                      .height(state.collapsedBound),
              content = collapsedContent)
        }
      }
}

@Composable
fun rememberDraggableBottomSheetState(
    dismissedBound: Dp,
    expandedBound: Dp,
    collapsedBound: Dp = dismissedBound,
    initialAnchor: DraggableBottomSheetAnchor = DraggableBottomSheetAnchor.DISMISSED,
    animationSpec: AnimationSpec<Dp>
): DraggableBottomSheetState {
  val density = LocalDensity.current
  val coroutineScope = rememberCoroutineScope()

  var previousAnchor by
      rememberSaveable(key = "previousAnchorDraggableBs") { mutableStateOf(initialAnchor) }
  val animatable = remember { Animatable(0.dp, Dp.VectorConverter) }

  return remember(dismissedBound, expandedBound, collapsedBound, coroutineScope) {
    val initialValue =
        when (previousAnchor) {
          DraggableBottomSheetAnchor.EXPANDED -> expandedBound
          DraggableBottomSheetAnchor.COLLAPSED -> collapsedBound
          DraggableBottomSheetAnchor.DISMISSED -> dismissedBound
        }

    animatable.updateBounds(dismissedBound.coerceAtMost(expandedBound), expandedBound)
    coroutineScope.launch { animatable.animateTo(initialValue, animationSpec) }

    DraggableBottomSheetState(
        draggableState =
            DraggableState { delta ->
              coroutineScope.launch {
                animatable.snapTo(animatable.value - with(density) { delta.toDp() })
              }
            },
        onAnchorChanged = { previousAnchor = it },
        coroutineScope = coroutineScope,
        animatable = animatable,
        collapsedBound = collapsedBound)
  }
}
