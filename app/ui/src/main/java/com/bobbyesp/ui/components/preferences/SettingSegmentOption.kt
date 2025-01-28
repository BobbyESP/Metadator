package com.bobbyesp.ui.components.preferences

import androidx.compose.ui.graphics.vector.ImageVector

data class SettingSegmentOption(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit
)