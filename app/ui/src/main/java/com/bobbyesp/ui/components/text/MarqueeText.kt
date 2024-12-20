package com.bobbyesp.ui.components.text

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SubtextOverline(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        letterSpacing = 2.sp,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/**
 * A highly performant and customizable marquee text composable for Jetpack Compose.
 *
 * @param text The text to be displayed in the marquee.
 * @param modifier General modifier for the entire marquee component.
 * @param textModifier Specific modifier for the text content.
 * @param color Text color.
 * @param fontSize Text size.
 * @param fontStyle Text font style (italic, normal).
 * @param fontWeight Text font weight.
 * @param fontFamily Custom font family.
 * @param letterSpacing Spacing between characters.
 * @param textDecoration Text decoration (underline, line-through).
 * @param textAlign Text alignment.
 * @param lineHeight Height of each text line.
 * @param maxLines Maximum number of lines to display.
 * @param overflow How to handle text overflow.
 * @param softWrap Whether to soft wrap text.
 * @param onTextLayout Callback when text layout is complete.
 * @param style Combined text style.
 * @param sideGradient Gradient options for text edges.
 * @param customEasing Custom animation easing function.
 * @param animationDuration Duration multiplier for animation speed.
 * @param delayBetweenAnimations Delay between marquee cycles.
 */
@Composable
fun MarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    textModifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current.plus(TextStyle()),
    sideGradient: MarqueeTextGradientOptions = MarqueeTextGradientOptions(),
    customEasing: Easing? = null,
    animationDuration: Float = 4000f,
    delayBetweenAnimations: Long = 500L
) {
    // State to track text layout information
    var textLayoutInfo by remember { mutableStateOf<TextLayoutInfo?>(null) }

    // Offset state with improved performance optimization
    var offset by remember(text) { mutableIntStateOf(0) }

    // Composable to create the text with consistent styling
    val createText = @Composable { localModifier: Modifier ->
        Text(
            text = text,
            textAlign = textAlign,
            modifier = localModifier,
            color = color,
            fontSize = fontSize,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing,
            textDecoration = textDecoration,
            lineHeight = lineHeight,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            onTextLayout = onTextLayout,
            style = style,
        )
    }

    LaunchedEffect(textLayoutInfo) {
        val layoutInfo = textLayoutInfo ?: return@LaunchedEffect

        // Skip animation if text fits within container
        if (layoutInfo.textWidth <= layoutInfo.containerWidth) return@LaunchedEffect

        val duration = 4000 * layoutInfo.textWidth / layoutInfo.containerWidth

        // Simple, declarative animation approach
        animate(
            initialValue = 0f,
            targetValue = -layoutInfo.textWidth.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = duration,
                    delayMillis = 1000,
                    easing = customEasing ?: LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            )
        ) { value, _ ->
            offset = value.toInt()
        }
    }

    // SubcomposeLayout for flexible text rendering
    SubcomposeLayout(
        modifier = modifier.clipToBounds()
    ) { constraints ->
        // Allow infinite width for text measurement
        val infiniteWidthConstraints = constraints.copy(maxWidth = Int.MAX_VALUE)

        // Measure main text
        val mainText = subcompose(MarqueeLayers.MainText) {
            createText(textModifier)
        }.first().measure(infiniteWidthConstraints)

        // Initialize placeholders
        var gradient: Placeable? = null
        var secondPlaceableWithOffset: Pair<Placeable, Int>? = null

        // Determine marquee behavior
        if (mainText.width <= constraints.maxWidth) {
            // Text fits, reset offset
            offset = 0
            textLayoutInfo = null
        } else {
            // Calculate spacing and layout info
            val spacing = constraints.maxWidth * 2 / 3
            textLayoutInfo = TextLayoutInfo(
                textWidth = mainText.width + spacing,
                containerWidth = constraints.maxWidth
            )

            // Prepare secondary text placement
            val secondTextOffset = mainText.width + offset + spacing
            val secondTextSpace = constraints.maxWidth - secondTextOffset

            if (secondTextSpace > 0) {
                secondPlaceableWithOffset = subcompose(MarqueeLayers.SecondaryText) {
                    createText(textModifier)
                }.first().measure(infiniteWidthConstraints) to secondTextOffset
            }

            // Create gradient edges
            gradient = subcompose(MarqueeLayers.EdgesGradient) {
                Row {
                    if (sideGradient.left) {
                        GradientEdge(
                            startColor = sideGradient.color,
                            endColor = Color.Transparent
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (sideGradient.right) {
                        GradientEdge(
                            startColor = Color.Transparent,
                            endColor = sideGradient.color
                        )
                    }
                }
            }.first().measure(constraints.copy(maxHeight = mainText.height))
        }

        // Final layout placement
        layout(
            width = if (mainText.width > constraints.maxWidth) constraints.maxWidth else mainText.width,
            height = mainText.height
        ) {
            mainText.place(offset, 0)
            secondPlaceableWithOffset?.let {
                it.first.place(it.second, 0)
            }
            gradient?.place(0, 0)
        }
    }
}

@Composable
private fun GradientEdge(
    startColor: Color, endColor: Color,
) {
    Box(
        modifier = Modifier
            .width(10.dp)
            .fillMaxHeight()
            .background(
                brush = Brush.horizontalGradient(
                    listOf(startColor, endColor)
                )
            )
    )
}

data class MarqueeTextGradientOptions(
    val color: Color = Color.Transparent,
    val right: Boolean = true,
    val left: Boolean = true
)

private enum class MarqueeLayers { MainText, SecondaryText, EdgesGradient }
private data class TextLayoutInfo(val textWidth: Int, val containerWidth: Int)