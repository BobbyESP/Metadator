package com.bobbyesp.utilities.theme

import com.bobbyesp.utilities.preferences.Preferences.AppMainSettingsStateFlow
import com.bobbyesp.utilities.preferences.Preferences.mutableAppMainSettingsStateFlow
import com.bobbyesp.utilities.preferences.PreferencesKeys.DARK_THEME_VALUE
import com.bobbyesp.utilities.preferences.PreferencesKeys.DYNAMIC_COLOR
import com.bobbyesp.utilities.preferences.PreferencesKeys.HIGH_CONTRAST
import com.bobbyesp.utilities.preferences.PreferencesKeys.THEME_COLOR
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppTheme(
    private val kv: MMKV,
    private val scope: CoroutineScope
) {
    private val appSettingsFlow = AppMainSettingsStateFlow.value

    private val theme = appSettingsFlow.darkTheme

    fun modifyThemePreferences(
        darkTheme: Int = theme.darkThemeValue,
        highContrast: Boolean = theme.isHighContrastModeEnabled
    ) {
        scope.launch(Dispatchers.IO) {
            mutableAppMainSettingsStateFlow.update {
                it.copy(
                    darkTheme = it.darkTheme.copy(
                        darkThemeValue = darkTheme,
                        isHighContrastModeEnabled = highContrast
                    )
                )
            }

            kv.encode(DARK_THEME_VALUE, darkTheme)
            kv.encode(HIGH_CONTRAST, highContrast) //AMOLED mode
        }
    }

    fun modifySeedColor(argbColor: Int) {
        scope.launch(Dispatchers.IO) {
            mutableAppMainSettingsStateFlow.update {
                it.copy(seedColor = argbColor)
            }

            kv.encode(THEME_COLOR, argbColor)
        }
    }

    fun switchDynamicColoring(enabled: Boolean = !appSettingsFlow.useDynamicColoring) {
        scope.launch(Dispatchers.IO) {
            mutableAppMainSettingsStateFlow.update {
                it.copy(useDynamicColoring = enabled)
            }
        }
        kv.encode(DYNAMIC_COLOR, enabled)
    }
}
