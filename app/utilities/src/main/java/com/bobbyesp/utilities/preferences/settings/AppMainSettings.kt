package com.bobbyesp.utilities.preferences.settings

import com.bobbyesp.utilities.theme.DarkThemePreference
import com.bobbyesp.utilities.ui.DEFAULT_SEED_COLOR

data class AppMainSettings(
    val darkTheme: DarkThemePreference = DarkThemePreference(),
    val useDynamicColoring: Boolean = false,
    val seedColor: Int = DEFAULT_SEED_COLOR,
)