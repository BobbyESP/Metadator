/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
@file:OptIn(ExperimentalHazeApi::class)

package com.bobbyesp.metadator.core.designsystem.blur

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.blur.BlurRadiusSpec
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeInputSharing
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource

/*
 * The app's blur, in four calls. Haze stays behind them: no other module names it.
 *
 *   val behind = rememberBlurSource()
 *   List(Modifier.blurSource(behind))                                  // what passes beneath
 *   Bar(Modifier.frosted(behind, containerColor = surfaceContainer))   // a surface over it
 *   Screen(Modifier.outOfFocus { radius })                             // content itself, blurred
 *
 * Each follows LocalBlurLevel on its own, so a caller never checks the device. What a caller may
 * animate (a frosted surface's amount, a radius) is a lambda read while drawing: an animation then
 * redraws, and never recomposes.
 */

/**
 * Content that a [frosted] surface or a [blurHalo] can blur, recorded with [blurSource]. One per
 * screen is enough: every surface over it shares it.
 */
@Stable class BlurSource internal constructor(internal val state: HazeState)

/** A [BlurSource] for this composition. */
@Composable fun rememberBlurSource(): BlurSource = remember { BlurSource(HazeState()) }

/**
 * Records this content for the surfaces that float over it. Must not contain them.
 *
 * At [BlurLevel.Off] it records nothing, and costs nothing: the surfaces draw their color alone.
 * Where two surfaces blur it (a bar and a halo), it renders once for both rather than once each.
 *
 * @param zIndex Where it stacks among the other sources of [source]: a higher one is recorded over
 *   a lower one, as it is drawn over it.
 */
@Composable
fun Modifier.blurSource(source: BlurSource, zIndex: Float = 0f): Modifier {
    if (LocalBlurLevel.current == BlurLevel.Off) return this
    return hazeSource(source.state, zIndex = zIndex, inputSharing = HazeInputSharing.Auto)
}

/**
 * Frosts this surface: what [source] records beneath it, blurred, under [containerColor] made
 * translucent. Pass the color role the surface would have as a solid one, so the frosted surface
 * keeps its place in the color scheme. The surface's own container must be transparent, and its
 * shadow gone: the frost is what sets it apart.
 *
 * At [BlurLevel.Off] the surface is [containerColor], nearly opaque, as it would be without blur.
 *
 * [HazeInput.Sources] rather than the window's backdrop, because it reaches across windows: a menu
 * is a popup of its own, and still frosts the list it opened over.
 *
 * @param shape The surface's shape; the frost is clipped to it.
 * @param radius How far what is beneath is blurred. Fixed for the surface's life: a new radius
 *   builds the blur again.
 * @param containerOpacity How much of [containerColor] covers the blur, where the default (see
 *   [BlurDefaults.LightContainerOpacity]) hides too much of what is beneath for a surface that is
 *   meant to be seen through. Lower is more see-through; contrast of what is on it suffers.
 * @param amount How much of the frost shows, from 0 (none: what is beneath shows as it is) to 1.
 *   Read while drawing, so animate it freely: unlike the color or the radius, it changes nothing
 *   the blur has to be built again for.
 */
@Composable
fun Modifier.frosted(
    source: BlurSource,
    containerColor: Color,
    shape: Shape = RectangleShape,
    radius: Dp = BlurDefaults.SurfaceRadius,
    containerOpacity: Float? = null,
    amount: () -> Float = FullAmount,
): Modifier {
    val style =
        remember(containerColor, radius, containerOpacity, amount) {
            frostStyle(containerColor, radius, containerOpacity, amount)
        }
    val clipped = if (shape === RectangleShape) this else clip(shape)
    if (LocalBlurLevel.current == BlurLevel.Off) {
        // Not through Haze: it draws an effect only over the layers a source has recorded, and at
        // Off nothing records any, so even its fallback color would never show.
        val solid = containerColor.copy(alpha = BlurDefaults.FallbackOpacity)
        return clipped.drawBehind { drawRect(solid, alpha = amount().coerceIn(0f, 1f)) }
    }
    return clipped.hazeBlur(input = HazeInput.Sources(source.state), style = style)
}

private val FullAmount = { 1f }

private fun frostStyle(
    containerColor: Color,
    radius: Dp,
    containerOpacity: Float?,
    amount: () -> Float,
): HazeBlurStyle {
    val opacity =
        containerOpacity?.coerceIn(0f, 1f)
            ?: if (containerColor.luminance() >= 0.5f) BlurDefaults.LightContainerOpacity
            else BlurDefaults.DarkContainerOpacity
    // Built once here, not in the block: Haze runs it on every frame the amount moves.
    val tint = listOf(HazeColorEffect.tint(containerColor.copy(alpha = opacity)))
    val fallback = HazeColorEffect.tint(containerColor.copy(alpha = BlurDefaults.FallbackOpacity))
    return HazeBlurStyle {
        blurRadius(radius)
        noiseFactor(BlurDefaults.NoiseFactor)
        backgroundColor(containerColor)
        colorEffects(tint)
        fallbackColorEffect(fallback)
        alpha(amount())
    }
}

/**
 * Takes this content out of focus by [radius], read while drawing, so it can be animated without
 * recomposing. While the radius is zero the content is drawn as it is, with no layer of its own.
 *
 * For whole screens and pictures, which cost the most to blur: only at [BlurLevel.Full]. Below it,
 * and below Android 12, the content stays sharp, so pair it with what sets the thing apart without
 * blur (a scrim, a solid surface).
 */
@Composable
fun Modifier.outOfFocus(radius: () -> Dp): Modifier {
    if (LocalBlurLevel.current != BlurLevel.Full) return this
    val blurring by remember(radius) { derivedStateOf { radius() > 0.dp } }
    return if (blurring) blur { blurRadiusSpec = BlurRadiusSpec.uniform(radius()) } else this
}
