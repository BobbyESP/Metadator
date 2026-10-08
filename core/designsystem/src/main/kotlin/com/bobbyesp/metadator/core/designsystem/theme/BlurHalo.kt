/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.designsystem.theme

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.offset
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.blur.material3.Material3

/**
 * Lifts this element off the content beneath it with blur instead of a shadow: the content around
 * it goes out of focus, most at its edge, and is sharp again [spread] away. Where a shadow darkens,
 * this softens, so the element reads as floating without a dark rim.
 *
 * The halo follows [shape], the element's own, and its blur radius truly decreases with distance
 * rather than a blurred copy fading over a sharp one, so there is no double image in between. It
 * blurs what [state] records, which must not contain this element.
 *
 * Drawn behind everything this modifier's element draws, and outside its bounds without changing
 * its layout. In a popup, whose window ends at its content, pass [reserveSpace]: the halo then
 * takes room around the element, and whoever places the popup must make up for it.
 *
 * Android 13 and up, where Haze can vary the radius. Elsewhere this does nothing: check
 * [MetadatorBlurDefaults.isHaloSupported] and keep the element's shadow there.
 *
 * Keep it out of the element's own enter and exit animation, and animate [strength] instead: in a
 * scaled or fading layer the halo would spring with the element and be cut to its bounds.
 *
 * @param strength How far the halo has come into focus, from 0 (none) to 1. It scales the blur
 *   radius and the halo's opacity together, so the content around the element blurs progressively
 *   rather than a full blur fading in over it. At 0 the halo still keeps its place in the layout.
 */
@Composable
fun Modifier.blurHalo(
    state: HazeState,
    shape: Shape,
    spread: Dp = MetadatorBlurDefaults.HaloSpread,
    strength: Float = 1f,
    reserveSpace: Boolean = false,
): Modifier {
    // The version again, though isHaloSupported has it: spelled out is how lint knows the shader
    // below is only reached on Android 13.
    if (
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            !MetadatorBlurDefaults.isHaloSupported
    ) {
        return this
    }

    val density = LocalDensity.current
    val spreadPx = with(density) { spread.roundToPx() }
    val dropPx = with(density) { MetadatorBlurDefaults.haloDrop(spread).roundToPx() }
    // The halo's room on every side: its spread, plus the drop it reaches further by below.
    val marginPx = spreadPx + dropPx
    val geometry = remember { HaloGeometry() }
    val progressive = remember(geometry) { HazeProgressive.forShader { geometry.shader(it) } }
    val style =
        MetadatorBlurDefaults.haloStyle(
            surface = MaterialTheme.colorScheme.surface,
            progressive = progressive,
            strength = strength.coerceIn(0f, 1f),
        )

    return this
        // Outer: the halo's area, the element grown by the margin on every side. Reported to the
        // parent as the element alone unless the room is reserved.
        .layout { measurable, constraints ->
            val grow = 2 * marginPx
            // Room that is reserved is already in the constraints: the element gets what is left.
            val placeable =
                measurable.measure(
                    if (reserveSpace) constraints else constraints.offset(grow, grow)
                )
            val element =
                Size(
                    (placeable.width - grow).toFloat(),
                    (placeable.height - grow).toFloat(),
                )
            geometry.update(
                nodeWidth = placeable.width.toFloat(),
                margin = marginPx.toFloat(),
                spread = spreadPx.toFloat(),
                drop = dropPx.toFloat(),
                outline = shape.createOutline(element, layoutDirection, this),
            )
            if (reserveSpace) {
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            } else {
                layout(placeable.width - grow, placeable.height - grow) {
                    placeable.place(-marginPx, -marginPx)
                }
            }
        }
        // The halo's own intensity reaches zero at its edge, so it never needs the extra room Haze
        // would otherwise blur beyond it.
        .hazeBlur(input = HazeInput.Sources(state), style = style, expandLayerBounds = false)
        // Inner: the element, back at its own size, in the middle of the halo's area.
        .layout { measurable, constraints ->
            val grow = 2 * marginPx
            val placeable = measurable.measure(constraints.offset(-grow, -grow))
            layout(placeable.width + grow, placeable.height + grow) {
                placeable.place(marginPx, marginPx)
            }
        }
}

/**
 * Where the halo is, read by the shader when Haze builds the effect. Written from layout and read
 * in draw, never in composition, so a moving element does not recompose anything.
 */
private class HaloGeometry {
    private var nodeWidth = 1f
    private var margin = 0f
    private var spread = 0f
    private var drop = 0f

    /** The element's corners, clockwise from the top-left, in the halo's pixels. */
    private val radii = FloatArray(4)

    private var shader: RuntimeShader? = null

    fun update(nodeWidth: Float, margin: Float, spread: Float, drop: Float, outline: Outline) {
        this.nodeWidth = nodeWidth.coerceAtLeast(1f)
        this.margin = margin
        this.spread = spread
        this.drop = drop
        when (outline) {
            is Outline.Rounded -> {
                val rect = outline.roundRect
                radii[0] = rect.topLeftCornerRadius.x
                radii[1] = rect.topRightCornerRadius.x
                radii[2] = rect.bottomRightCornerRadius.x
                radii[3] = rect.bottomLeftCornerRadius.x
            }
            // A rectangle, or a path the distance cannot follow: its bounds, square.
            else -> radii.fill(0f)
        }
    }

    /**
     * Haze asks at the size it blurs at, which is smaller than the layout when it samples down, so
     * every length is scaled to it.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun shader(size: Size): RuntimeShader {
        val shader = shader ?: haloShader().also { shader = it }
        val scale = size.width / nodeWidth
        shader.setFloatUniform("size", size.width, size.height)
        shader.setFloatUniform("margin", margin * scale)
        shader.setFloatUniform("spread", (spread * scale).coerceAtLeast(1f))
        shader.setFloatUniform("drop", drop * scale)
        shader.setFloatUniform(
            "radii",
            radii[0] * scale,
            radii[1] * scale,
            radii[2] * scale,
            radii[3] * scale,
        )
        return shader
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun haloShader(): RuntimeShader = RuntimeShader(HaloShaderSource)

/**
 * The halo's intensity at each point: the signed distance to the element's rounded rectangle, which
 * sits `margin` in from every edge, eased from 1 at its edge to 0 `spread` away. Inside the element
 * it is 1; the element covers it anyway.
 *
 * The distance is measured from the element moved down by `drop`, as Material's key light casts a
 * shadow further below an element than above it: the halo reaches further under it, and less over
 * whatever it was opened from.
 *
 * The ease is an exponential decay, rescaled to run from 1 to 0. How blurred text looks is far from
 * linear in the radius: it only visibly changes while the radius is a few pixels, being unreadable
 * above that and sharp below. A decay spends the same distance on every halving of the radius, so
 * that narrow range is spread over a good part of the halo instead of a few pixels of it, and the
 * content eases back into focus. A smoothstep kept the radius high until the very end, and read as
 * a cut. At the edge the radius is under a pixel, so the decay needs no flat end.
 */
private const val HaloShaderSource =
    """
uniform float2 size;
uniform float margin;
uniform float spread;
uniform float drop;
uniform float4 radii;

half4 main(float2 position) {
    float2 center = size * 0.5;
    float2 halfExtent = max(center - margin, float2(0.0));
    float2 fromCenter = position - center - float2(0.0, drop);

    float radius = fromCenter.x < 0.0
        ? (fromCenter.y < 0.0 ? radii.x : radii.w)
        : (fromCenter.y < 0.0 ? radii.y : radii.z);
    radius = min(radius, min(halfExtent.x, halfExtent.y));

    float2 q = abs(fromCenter) - halfExtent + radius;
    float distance = min(max(q.x, q.y), 0.0) + length(max(q, float2(0.0))) - radius;

    // e^(-3t), rescaled so that it is exactly 1 at the element and 0 at the halo's edge.
    float t = clamp(distance / spread, 0.0, 1.0);
    float intensity = (exp(-3.0 * t) - 0.049787) / (1.0 - 0.049787);
    return half4(0.0, 0.0, 0.0, clamp(intensity, 0.0, 1.0));
}
"""

/**
 * The halo's look: blur, grain and a faint veil of the surface, all fading with the intensity, and
 * all scaled by [strength] while the halo comes into focus.
 */
@Composable
internal fun MetadatorBlurDefaults.haloStyle(
    surface: Color,
    progressive: HazeProgressive,
    strength: Float,
): HazeBlurStyle =
    HazeBlurStyle.Material3(surface) {
        blurRadius(HaloRadius * strength)
        noiseFactor(HaloNoiseFactor)
        colorEffects(listOf(HazeColorEffect.tint(surface.copy(alpha = HaloVeilOpacity))))
        progressive(progressive)
        alpha(strength)
    }
