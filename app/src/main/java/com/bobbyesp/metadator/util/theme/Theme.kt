package com.bobbyesp.metadator.util.theme

import com.bobbyesp.metadator.util.preferences.CoreSettings
import com.bobbyesp.metadator.util.preferences.PreferencesKeys.DARK_THEME_VALUE
import com.bobbyesp.metadator.util.preferences.PreferencesKeys.DYNAMIC_COLOR
import com.bobbyesp.metadator.util.preferences.PreferencesKeys.HIGH_CONTRAST
import com.bobbyesp.metadator.util.preferences.PreferencesKeys.THEME_COLOR
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppTheme(
    private val kv: MMKV,
    private val coreSettings: CoreSettings,
    private val scope: CoroutineScope
) {
    private val appSettingsFlow = coreSettings.appMainSettingsStateFlow.value

    private val theme = appSettingsFlow.darkTheme

    fun modifyThemePreferences(
        darkTheme: Int = theme.darkThemeValue,
        highContrast: Boolean = theme.isHighContrastModeEnabled
    ) {
        scope.launch(Dispatchers.IO) {
            coreSettings.mutableAppMainSettingsStateFlow.update {
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
            coreSettings.mutableAppMainSettingsStateFlow.update {
                it.copy(seedColor = argbColor)
            }

            kv.encode(THEME_COLOR, argbColor)
        }
    }

    fun switchDynamicColoring(enabled: Boolean = !appSettingsFlow.useDynamicColoring) {
        scope.launch(Dispatchers.IO) {
            coreSettings.mutableAppMainSettingsStateFlow.update {
                it.copy(useDynamicColoring = enabled)
            }
        }
        kv.encode(DYNAMIC_COLOR, enabled)
    }
}
