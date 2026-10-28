/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.designsystem.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.bobbyesp.metadator.core.designsystem.R

/**
 * Instrument Sans, the brand's typeface. Its two variable files hold every weight from 400 to 700,
 * upright and italic; each weight here picks its point on the weight axis, so nothing is
 * synthesized.
 */
val InstrumentSans: FontFamily =
    FontFamily(
        listOf(400, 500, 600, 700).flatMap { weight ->
            listOf(
                Font(
                    R.font.instrument_sans,
                    weight = FontWeight(weight),
                    style = FontStyle.Normal,
                    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
                ),
                Font(
                    R.font.instrument_sans_italic,
                    weight = FontWeight(weight),
                    style = FontStyle.Italic,
                    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
                ),
            )
        }
    )

/**
 * The wordmark's style: "Metadator" is set in semibold italic, slightly tight, as in the logo. For
 * the app's name where it stands as a brand, never for running text.
 */
val LogotypeStyle: TextStyle =
    TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.SemiBold,
        fontStyle = FontStyle.Italic,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.02).em,
    )

/**
 * The M3 type scale in Instrument Sans, weighted as the brand guide sets it: Material's regular
 * headlines look flat in this face, so the hierarchy is carried by weight. Display is bold italic,
 * the two largest headlines semibold italic, titles semibold; each emphasized role is one step
 * heavier than its plain one (bold for headlines and titles, semibold for body and labels).
 *
 * Italic is for headlines, as in the brand: screen titles in a large top bar read as the brand
 * does. Everything a user reads at length (lyrics, song titles, fields, the emphasized roles) stays
 * upright.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal val MetadatorTypography: Typography =
    Typography().run {
        copy(
            displayLarge = displayLarge.brand(FontWeight.Bold, italic = true),
            displayMedium = displayMedium.brand(FontWeight.Bold, italic = true),
            displaySmall = displaySmall.brand(FontWeight.Bold, italic = true),
            headlineLarge = headlineLarge.brand(FontWeight.SemiBold, italic = true),
            headlineMedium = headlineMedium.brand(FontWeight.SemiBold, italic = true),
            headlineSmall = headlineSmall.brand(FontWeight.SemiBold),
            titleLarge = titleLarge.brand(FontWeight.SemiBold),
            titleMedium = titleMedium.brand(FontWeight.SemiBold),
            titleSmall = titleSmall.brand(),
            bodyLarge = bodyLarge.brand().copy(letterSpacing = 0.sp),
            bodyMedium = bodyMedium.brand().copy(letterSpacing = 0.sp),
            bodySmall = bodySmall.brand(),
            labelLarge = labelLarge.brand(),
            labelMedium = labelMedium.brand(),
            labelSmall = labelSmall.brand(),
            displayLargeEmphasized = displayLargeEmphasized.brand(FontWeight.Bold, italic = true),
            displayMediumEmphasized = displayMediumEmphasized.brand(FontWeight.Bold, italic = true),
            displaySmallEmphasized = displaySmallEmphasized.brand(FontWeight.Bold, italic = true),
            headlineLargeEmphasized = headlineLargeEmphasized.brand(FontWeight.Bold),
            headlineMediumEmphasized = headlineMediumEmphasized.brand(FontWeight.Bold),
            headlineSmallEmphasized = headlineSmallEmphasized.brand(FontWeight.Bold),
            titleLargeEmphasized = titleLargeEmphasized.brand(FontWeight.Bold),
            titleMediumEmphasized = titleMediumEmphasized.brand(FontWeight.Bold),
            titleSmallEmphasized = titleSmallEmphasized.brand(FontWeight.Bold),
            bodyLargeEmphasized = bodyLargeEmphasized.brand(FontWeight.SemiBold),
            bodyMediumEmphasized = bodyMediumEmphasized.brand(FontWeight.SemiBold),
            bodySmallEmphasized = bodySmallEmphasized.brand(FontWeight.SemiBold),
            labelLargeEmphasized = labelLargeEmphasized.brand(FontWeight.SemiBold),
            labelMediumEmphasized = labelMediumEmphasized.brand(FontWeight.SemiBold),
            labelSmallEmphasized = labelSmallEmphasized.brand(FontWeight.SemiBold),
        )
    }

/** This role in Instrument Sans, at [weight] if given, upright unless [italic]. */
private fun TextStyle.brand(weight: FontWeight? = null, italic: Boolean = false): TextStyle =
    copy(
        fontFamily = InstrumentSans,
        fontWeight = weight ?: fontWeight,
        fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
    )

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
