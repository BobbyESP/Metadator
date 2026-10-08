/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/** The M3 type scale with heavier titles, which carry most of the hierarchy in lists and forms. */
internal val MetadatorTypography: Typography =
    Typography().run {
        copy(
            headlineLarge = headlineLarge.copy(fontWeight = FontWeight.SemiBold),
            headlineMedium = headlineMedium.copy(fontWeight = FontWeight.SemiBold),
            headlineSmall = headlineSmall.copy(fontWeight = FontWeight.Medium),
            titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
            titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
            bodyLarge = bodyLarge.copy(letterSpacing = 0.sp),
            bodyMedium = bodyMedium.copy(letterSpacing = 0.sp),
        )
    }

/**
 * This style at another size, with every length in it scaled by as much: text set in one is then
 * the text set in the other, only larger or smaller. For a text that grows from one place to
 * another, whose two ends must match glyph for glyph on the way; two roles of the type scale do
 * not, since each has a weight, a line height and a tracking of its own.
 */
fun TextStyle.resizedTo(fontSize: TextUnit): TextStyle {
    val scale = fontSize.value / this.fontSize.value
    return copy(
        fontSize = fontSize,
        // Lengths in em already follow the font size.
        lineHeight = if (lineHeight.isSp) lineHeight * scale else lineHeight,
        letterSpacing = if (letterSpacing.isSp) letterSpacing * scale else letterSpacing,
    )
}
