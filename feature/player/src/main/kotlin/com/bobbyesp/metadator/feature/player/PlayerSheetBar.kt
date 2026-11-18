/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.feature.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import com.bobbyesp.metadator.core.designsystem.blur.BlurDefaults
import com.bobbyesp.metadator.core.designsystem.blur.BlurSource
import com.bobbyesp.metadator.core.designsystem.blur.blurHalo
import com.bobbyesp.metadator.core.designsystem.theme.Spacing

/**
 * Where the bar rests: at the bottom, clear of the navigation bar, as wide as the window up to
 * [BarMaxWidth]. The bar and its halo both take this place, so that they cannot disagree on it.
 */
internal fun Modifier.barPlace(): Modifier =
    navigationBarsPadding()
        .padding(bottom = Spacing.medium)
        .widthIn(max = BarMaxWidth)
        .fillMaxWidth()
        .padding(horizontal = Spacing.medium)

private val BarShape = RoundedCornerShape(BarCorner)

/**
 * Lifts the bar off what is behind it: the content around it goes out of focus. It is a sibling
 * behind the sheet and not part of the bar, on purpose:
 * - Haze lays its effect out against the window. Inside the bar it would be composed, moved and
 *   kept alive by the shared element transition, which draws its content in an overlay and keeps
 *   the leaving content around; a halo there can be left behind, or drawn in two places at once
 *   when the sheet is let go halfway and turned back.
 * - It follows the bar's place, not the bar: it has no part in its enter and exit animation, which
 *   would scale, slide and clip it with the bar. Its [strength] is animated instead.
 *
 * It is there only while the bar is at rest. As soon as the sheet moves it fades, and the bar lifts
 * off with the surface it turns into.
 *
 * @param barHeight the bar's height at rest, in pixels; 0 while it has not been laid out
 * @param backdrop what is recorded of the screens under the player; without it there is no halo
 */
@Composable
internal fun BarHalo(
    sheet: PlayerSheetState,
    available: Boolean,
    backdrop: BlurSource?,
    barHeight: Int,
    modifier: Modifier = Modifier,
) {
    if (backdrop == null || !BlurDefaults.isHaloAvailable) return

    val wanted by rememberUpdatedState(available && barHeight > 0)
    val atRest by remember(sheet) { derivedStateOf { sheet.fraction == 0f } }
    val strength =
        animateFloatAsState(
            targetValue = if (wanted && atRest) 1f else 0f,
            animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
            label = "BarHalo",
        )
    // Whether there is a halo at all changes twice per fade; its strength, every frame, is read
    // while drawing. A source records its content only while something blurs it, so a halo that is
    // not showing is taken off rather than left at zero.
    val drawn by remember { derivedStateOf { wanted || strength.value > 0f } }
    if (!drawn) return

    val height = with(LocalDensity.current) { barHeight.toDp() }
    Box(
        modifier
            .barPlace()
            .height(height)
            .blurHalo(source = backdrop, shape = BarShape, strength = { strength.value })
    )
}
