package com.bobbyesp.metadator.core.data.local

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.res.stringResource
import com.bobbyesp.metadator.core.data.local.preferences.PreferencesKey
import com.bobbyesp.metadator.core.data.local.preferences.PreferencesKey.DARK_THEME_VALUE
import com.bobbyesp.metadator.core.data.local.preferences.PreferencesKey.HIGH_CONTRAST
import com.bobbyesp.utilities.R

@Stable
data class DarkThemePreference(
    val darkThemeValue: DarkThemeValue = DarkThemeValue.valueOf(DARK_THEME_VALUE.defaultValue),
    val isHighContrastModeEnabled: Boolean = HIGH_CONTRAST.defaultValue
) {
    companion object {
        enum class DarkThemeValue {
            FOLLOW_SYSTEM,
            ON,
            OFF
        }

        const val FOLLOW_SYSTEM = 1
        const val ON = 2
        const val OFF = 3 // Non used
    }

    @Composable
    fun isDarkTheme(): Boolean {
        return if (darkThemeValue == DarkThemeValue.FOLLOW_SYSTEM)
            isSystemInDarkTheme()
        else darkThemeValue == DarkThemeValue.ON
    }

    @Composable
    fun getDarkThemeDescription(): String {
        return when (darkThemeValue) {
            DarkThemeValue.FOLLOW_SYSTEM -> stringResource(R.string.follow_system)
            DarkThemeValue.ON -> stringResource(R.string.on)
            else -> stringResource(R.string.off)
        }
    }
}