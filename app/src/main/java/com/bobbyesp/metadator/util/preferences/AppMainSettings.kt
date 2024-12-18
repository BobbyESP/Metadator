package com.bobbyesp.metadator.util.preferences

import com.bobbyesp.metadator.util.preferences.PreferencesKeys.DARK_THEME_VALUE
import com.bobbyesp.metadator.util.preferences.PreferencesKeys.DYNAMIC_COLOR
import com.bobbyesp.metadator.util.preferences.PreferencesKeys.HIGH_CONTRAST
import com.bobbyesp.metadator.util.preferences.PreferencesKeys.THEME_COLOR
import com.bobbyesp.metadator.util.theme.DarkThemePreference
import com.bobbyesp.utilities.ui.DEFAULT_SEED_COLOR
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class CoreSettings(
    kv: MMKV,
) {
    data class AppMainSettings(
        val darkTheme: DarkThemePreference = DarkThemePreference(),
        val useDynamicColoring: Boolean = false,
        val seedColor: Int = DEFAULT_SEED_COLOR,
    )

    val mutableAppMainSettingsStateFlow = MutableStateFlow(
        AppMainSettings(
            DarkThemePreference(
                darkThemeValue = kv.decodeInt(
                    DARK_THEME_VALUE,
                    DarkThemePreference.FOLLOW_SYSTEM
                ), isHighContrastModeEnabled = kv.decodeBool(HIGH_CONTRAST, false)
            ),
            useDynamicColoring = kv.decodeBool(
                DYNAMIC_COLOR,
                true
            ),
            seedColor = kv.decodeInt(THEME_COLOR, DEFAULT_SEED_COLOR),
        )
    )

    val appMainSettingsStateFlow = mutableAppMainSettingsStateFlow.asStateFlow()

    companion object {
        lateinit var instance: CoreSettings

        fun initialize(kv: MMKV): CoreSettings {
            instance = CoreSettings(kv)
            return instance
        }

        val mutableAppMainSettingsStateFlow get() = instance.mutableAppMainSettingsStateFlow
        val appMainSettingsStateFlow get() = instance.appMainSettingsStateFlow
    }
}