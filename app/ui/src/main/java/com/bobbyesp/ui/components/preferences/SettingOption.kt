
package com.bobbyesp.ui.components.preferences

sealed class SettingOption(
    val title: String,
    val onSelection: () -> Unit,
)
