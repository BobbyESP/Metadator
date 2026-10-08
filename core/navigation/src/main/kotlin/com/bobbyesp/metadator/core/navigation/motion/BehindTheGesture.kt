/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.navigation.motion

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigationevent.NavigationEventTransitionState.InProgress
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.bobbyesp.metadator.core.designsystem.theme.outOfFocus
import kotlinx.coroutines.flow.collectLatest

/**
 * Keeps the screen behind out of focus while a back gesture holds the one over it: most at the
 * start, when little of it shows, and sharp by the time the gesture has uncovered it.
 */
@Composable
internal fun <T : Any> rememberBehindTheGestureNavEntryDecorator(): NavEntryDecorator<T> =
    remember {
        NavEntryDecorator { entry -> BehindTheGesture { entry.Content() } }
    }

@Composable
private fun BehindTheGesture(content: @Composable () -> Unit) {
    val dispatcher = LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher
    val transition = LocalNavAnimatedContentScope.current.transition
    val defocus = remember { Animatable(0f) }
    val refocus = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    LaunchedEffect(dispatcher, transition) {
        dispatcher?.transitionState?.collectLatest { gesture ->
            // The screen a gesture is bringing back is the one still on its way in.
            if (gesture is InProgress && transition.currentState == EnterExitState.PreEnter) {
                defocus.snapTo(MOST * (1f - gesture.latestEvent.progress))
            } else {
                defocus.animateTo(0f, refocus)
            }
        }
    }
    Box(
        modifier = Modifier.outOfFocus({ defocus.value }, MaterialTheme.colorScheme.scrim),
        propagateMinConstraints = true,
    ) {
        content()
    }
}

private const val MOST = 0.6f
