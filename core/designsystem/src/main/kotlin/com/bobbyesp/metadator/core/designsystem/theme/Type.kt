package com.bobbyesp.metadator.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
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
