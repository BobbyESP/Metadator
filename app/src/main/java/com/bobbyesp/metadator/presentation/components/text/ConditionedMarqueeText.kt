package com.bobbyesp.metadator.presentation.components.text

import androidx.compose.animation.core.Easing
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.bobbyesp.ui.components.text.MarqueeText
import com.bobbyesp.ui.components.text.MarqueeTextGradientOptions
import com.bobbyesp.metadator.util.preferences.PreferencesKeys.MARQUEE_TEXT
import com.bobbyesp.metadator.util.preferences.booleanState

@Composable
fun ConditionedMarqueeText(
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
    val useMarqueeText = MARQUEE_TEXT.booleanState

    if(useMarqueeText.value) {
        MarqueeText(
            text,
            modifier,
            textModifier,
            color,
            fontSize,
            fontStyle,
            fontWeight,
            fontFamily,
            letterSpacing,
            textDecoration,
            textAlign,
            lineHeight,
            maxLines,
            overflow,
            softWrap,
            onTextLayout,
            style,
            sideGradient,
            customEasing,
            animationDuration,
            delayBetweenAnimations
        )
    } else {
        Text(
            text = text,
            modifier = textModifier,
            color = color,
            fontSize = fontSize,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing,
            textDecoration = textDecoration,
            textAlign = textAlign,
            lineHeight = lineHeight,
            maxLines = maxLines,
            overflow = overflow,
            softWrap = softWrap,
            onTextLayout = onTextLayout,
            style = style
        )
    }
}