package com.bobbyesp.metadator.presentation.utils

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.tween
import com.bobbyesp.ui.motion.EmphasizedEasing
import com.bobbyesp.ui.motion.MotionConstants.DURATION

@OptIn(ExperimentalSharedTransitionApi::class)
val SongCardBoundsTransformation = BoundsTransform { _, _ ->
    tween(easing = EmphasizedEasing, durationMillis = DURATION)
}