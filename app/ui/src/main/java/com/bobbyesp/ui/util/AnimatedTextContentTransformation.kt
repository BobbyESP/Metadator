package com.bobbyesp.ui.util

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import com.bobbyesp.ui.motion.materialSharedAxisXIn
import com.bobbyesp.ui.motion.materialSharedAxisXOut

val AnimatedTextContentTransformation =
    ContentTransform(
        materialSharedAxisXIn(initialOffsetX = { it / 10 }),
        materialSharedAxisXOut(targetOffsetX = { -it / 10 }),
        sizeTransform = SizeTransform(clip = false))
