package com.bobbyesp.metadator.presentation.components.others

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bobbyesp.ui.motion.materialSharedAxisXIn
import com.bobbyesp.ui.motion.materialSharedAxisXOut

val CollapsedPlayerHeight = 84.dp
val SeekToButtonSize = 42.dp

val PlayerAnimationSpec: AnimationSpec<Dp> = tween(
    durationMillis = 750,
    delayMillis = 0,
    easing = EaseInOutSine
)

val AnimatedTextContentTransformation = ContentTransform(
    materialSharedAxisXIn(initialOffsetX = { it / 10 }),
    materialSharedAxisXOut(targetOffsetX = { -it / 10 }),
    sizeTransform = SizeTransform(clip = false)
)