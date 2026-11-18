/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.designsystem.blur

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.blur.BlurRadiusSpec
import androidx.compose.ui.graphics.graphicsLayer

/*
 * What Metadator blurs besides surfaces: the screen behind something that opens over it, and what
 * comes and goes as one surface turns into another. Like every modifier in this package they follow
 * LocalBlurLevel on their own, and read what animates while drawing.
 */

/**
 * What is recorded of the screen for whatever floats over all of it to blur: the menus, and the
 * shell's own player bar. The shell records it and provides it; a screen never does.
 *
 * `null` where nothing is recorded (a preview, the player, an overlay in a window of its own):
 * there a menu keeps Material's container and shadow. Never frost with it something drawn inside
 * the screen itself, which is inside what it records: that takes a source of the screen's own.
 */
val LocalBackdropSource = staticCompositionLocalOf<BlurSource?> { null }

/**
 * Takes this out of focus as something opens over it: blurred, as [outOfFocus] does, and dimmed
 * with [scrim] in step with [fraction], from 0 (as it is) to 1. Where it cannot blur (below
 * [BlurLevel.Full]) it is only dimmed, and more.
 *
 * @param fraction read while drawing, so a gesture can drive it without recomposing anything
 */
@Composable
fun Modifier.outOfFocus(fraction: () -> Float, scrim: Color): Modifier {
    val scrimOpacity =
        if (LocalBlurLevel.current == BlurLevel.Full) BlurDefaults.BlurredScrimOpacity
        else BlurDefaults.SharpScrimOpacity
    return outOfFocus { BlurDefaults.BehindOverlayRadius * fraction().coerceIn(0f, 1f) }
        .drawWithContent {
            drawContent()
            val dim = fraction().coerceIn(0f, 1f) * scrimOpacity
            if (dim > 0f) drawRect(scrim, alpha = dim)
        }
}

/**
 * Brings this in, or takes it away, through focus: as faint and as blurred as it is absent, by
 * [presence] from 0 (gone) to 1 (as it is). On the way in it sharpens as it shows. For what one
 * surface has and the surface it turns into has not, which a plain fade would lay, half
 * see-through, over what is still there. Where it cannot blur it only fades.
 *
 * `Modifier.blur` and not Haze, so it can run during a shared element transition. What travels in
 * that transition is drawn in its overlay, outside this, and stays sharp.
 *
 * @param presence read while drawing, so a gesture can drive it without recomposing anything
 */
@Composable
fun Modifier.dissolved(presence: () -> Float): Modifier {
    val absence = remember(presence) { { 1f - presence() } }
    return defocused(absence).graphicsLayer { alpha = presence().coerceIn(0f, 1f) }
}

/**
 * Takes this out of focus by [amount], from 0 (sharp) to 1, and nothing else: it stays as solid as
 * it was. For what something else is taking the place of, and must not be seen through meanwhile.
 * Below [BlurLevel.Full] it does nothing.
 *
 * @param amount read while drawing, so an animation can drive it without recomposing anything
 * @param bounded whether the blur stops at the edges of this, which is right for what fills its
 *   rectangle (a picture); left unbounded it is right for what does not (a pill, a line of text),
 *   which a blur cut to a rectangle would show the corners of
 */
@Composable
fun Modifier.defocused(amount: () -> Float, bounded: Boolean = false): Modifier {
    if (LocalBlurLevel.current != BlurLevel.Full) return this
    // Only on the way: sharp there is nothing to blur, and no layer to pay for.
    val blurring by remember(amount) { derivedStateOf { amount() > 0f } }
    if (!blurring) return this
    return blur {
        blurRadiusSpec =
            BlurRadiusSpec.uniform(BlurDefaults.DissolveRadius * amount().coerceIn(0f, 1f))
        edgeTreatment =
            if (bounded) BlurredEdgeTreatment.Rectangle else BlurredEdgeTreatment.Unbounded
    }
}
