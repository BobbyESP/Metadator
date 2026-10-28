/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.bobbyesp.metadator.core.designsystem.R

/**
 * The brand's colors. The app is colored by its theme (the wallpaper, a seed, a cover); these are
 * for where Metadator shows itself as a brand, and for the default seed.
 */
object BrandColors {
    val Violet = Color(0xFF381080)
    val Lavender = Color(0xFFF1E9FF)
    /** An accent for marketing and small touches only: it is not readable on lavender. */
    val Acid = Color(0xFFDFFF00)
}

/** The brand glyph, a file's frame around five beats of a waveform, to tint like any icon. */
val MetadatorGlyph: ImageVector
    @Composable get() = ImageVector.vectorResource(R.drawable.ic_metadator_glyph)
