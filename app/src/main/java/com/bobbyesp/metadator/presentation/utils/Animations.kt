package com.bobbyesp.metadator.presentation.utils

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.tween
import com.bobbyesp.ui.motion.MotionConstants.DURATION
import com.bobbyesp.ui.motion.emphasizeEasing

@OptIn(ExperimentalSharedTransitionApi::class)
val SongCardBoundsTransformation = BoundsTransform { _, _ ->
    tween(easing = emphasizeEasing, durationMillis = DURATION)
}