/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.designsystem.blur

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Where the app blurs, and how much. Material has no blur tokens, so these stand in for elevation
 * where a surface floats over content that moves beneath it: frosting what is behind the surface
 * tells the two apart better than a shadow, and keeps the content's colors in view. The surface
 * keeps its Material color role, only translucent.
 *
 * Three kinds of blur, for three jobs, each a modifier:
 * - **Frosted surfaces** ([frosted]): a bar, a search field or a menu, over content recorded with
 *   [blurSource].
 * - **Halos** ([blurHalo]): the content around a floating element out of focus, where a shadow
 *   would darken it.
 * - **Content out of focus** ([outOfFocus]): the screen behind a sheet or a dialog.
 *
 * How much of it a device gets is its [BlurLevel]; every modifier falls back on its own.
 */
object BlurDefaults {

    /**
     * Behind a frosted surface: enough that text beneath cannot be read, not so much that its
     * colors turn to mud.
     */
    val SurfaceRadius: Dp = 24.dp

    /**
     * The screen behind a sheet or a dialog: out of focus but still recognizable, so it is clear
     * what the overlay was opened from.
     */
    val BehindOverlayRadius: Dp = 12.dp

    /**
     * How far a [blurHalo] reaches past its element. Wider than the shadow it replaces: a shadow
     * shows over any background, a blur only over detail, so it needs the room to be seen.
     */
    val HaloSpread: Dp = 32.dp

    /**
     * A menu's halo: wider than [HaloSpread], because a menu opens over more of the content, and
     * its halo is all that separates the two.
     */
    val MenuHaloSpread: Dp = 36.dp

    /**
     * What is [dissolved], at its faintest: clearly out of focus, but its shapes still there to
     * sharpen into place.
     */
    val DissolveRadius: Dp = 12.dp

    /**
     * How dark content out of focus gets. Where it is also blurred a touch is enough; where it
     * cannot be, the dim is all that sets it back, and has to be Material's scrim.
     */
    internal const val BlurredScrimOpacity = 0.16f
    internal const val SharpScrimOpacity = 0.48f

    /**
     * The halo's blur at the element's edge, easing to none at the end of its spread. Enough to
     * soften what is beside the element, not to smear it: past this the halo stops reading as depth
     * and starts reading as an effect.
     */
    val HaloRadius: Dp = 12.dp

    /**
     * How much further below its element the halo reaches than above it, as a fraction of its
     * spread: Material's key light casts its shadows downwards, and the halo keeps that direction.
     */
    private const val HaloDropFraction = 0.25f

    /** How far down a halo of [spread] is moved; see [HaloDropFraction]. */
    fun haloDrop(spread: Dp): Dp = spread * HaloDropFraction

    /** The room a halo of [spread] needs on every side of its element. */
    fun haloMargin(spread: Dp = HaloSpread): Dp = spread + haloDrop(spread)

    /**
     * Whether [blurHalo] draws here: at [BlurLevel.Full], from Android 13, where Haze can vary a
     * blur's radius. Where it does not, an element keeps the shadow the halo replaces.
     */
    val isHaloAvailable: Boolean
        @Composable
        @ReadOnlyComposable
        get() =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                LocalBlurLevel.current == BlurLevel.Full

    /**
     * A breath of the surface color over the halo's blur, fading with it: over a flat area, where
     * blur alone changes nothing, the element still has a faint rim around it. Kept low, or the rim
     * reads as a band with an edge of its own.
     */
    internal const val HaloVeilOpacity = 0.18f

    /** The halo's grain: the same reason as [NoiseFactor], over a narrower blur. */
    internal const val HaloNoiseFactor = 0.04f

    /**
     * How much of the container color covers the blur. Enough for the content colors on it to keep
     * their contrast whatever scrolls beneath; a dark container needs more, because light content
     * shows through it more.
     */
    internal const val LightContainerOpacity = 0.76f
    internal const val DarkContainerOpacity = 0.82f

    /**
     * A frosted surface where nothing blurs: its container color, nearly opaque. Nothing beneath
     * can be read through it, as through the frost, and the surface still reads as the same one.
     */
    internal const val FallbackOpacity = 0.96f

    /**
     * A little grain, which keeps a wide blur of flat colors from banding. Less than Haze's
     * default: Material surfaces are flat, and more reads as texture.
     */
    internal const val NoiseFactor = 0.06f
}
