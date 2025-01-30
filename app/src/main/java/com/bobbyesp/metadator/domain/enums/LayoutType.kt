package com.bobbyesp.metadator.domain.enums

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.ui.graphics.vector.ImageVector

enum class LayoutType(val icon: ImageVector) {
    Grid(icon = Icons.Rounded.GridView),
    List(icon = Icons.AutoMirrored.Rounded.List);

    companion object {
        fun Int.toListType(): LayoutType = LayoutType.entries.first { it.ordinal == this }
    }
}