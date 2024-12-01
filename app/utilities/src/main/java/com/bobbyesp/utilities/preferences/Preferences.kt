package com.bobbyesp.utilities.preferences

import com.bobbyesp.utilities.preferences.PreferencesKeys.DARK_THEME_VALUE
import com.bobbyesp.utilities.preferences.PreferencesKeys.DYNAMIC_COLOR
import com.bobbyesp.utilities.preferences.PreferencesKeys.HIGH_CONTRAST
import com.bobbyesp.utilities.preferences.PreferencesKeys.MARQUEE_TEXT
import com.bobbyesp.utilities.preferences.PreferencesKeys.MMKV_PREFERENCES_NAME
import com.bobbyesp.utilities.preferences.PreferencesKeys.SONG_CARD_SIZE
import com.bobbyesp.utilities.preferences.PreferencesKeys.THEME_COLOR
import com.bobbyesp.utilities.preferences.settings.AppMainSettings
import com.bobbyesp.utilities.theme.DarkThemePreference
import com.bobbyesp.utilities.ui.DEFAULT_SEED_COLOR
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

private val StringPreferenceDefaults: Map<String, String> =
    mapOf()

private val BooleanPreferenceDefaults: Map<String, Boolean> =
    mapOf(
        MARQUEE_TEXT to true
    )

private val IntPreferenceDefaults: Map<String, Int> =
    mapOf(
        DARK_THEME_VALUE to DarkThemePreference.FOLLOW_SYSTEM,
        SONG_CARD_SIZE to 2 //CompactCardSize.LARGE (com.bobbyesp.metadator.presentation.components.cards.songs.compact.CompactCardSize)
    )

object Preferences {
    val kv: MMKV = MMKV.mmkvWithID(MMKV_PREFERENCES_NAME)

    fun String.getInt(default: Int = IntPreferenceDefaults.getOrElse(this) { 0 }): Int =
        kv.decodeInt(this, default)

    fun String.getString(default: String = StringPreferenceDefaults.getOrElse(this) { "" }): String =
        kv.decodeString(this) ?: default

    fun String.getBoolean(default: Boolean = BooleanPreferenceDefaults.getOrElse(this) { false }): Boolean =
        kv.decodeBool(this, default)

    fun String.updateString(newString: String) = kv.encode(this, newString)

    fun String.updateInt(newInt: Int) = kv.encode(this, newInt)

    fun String.updateBoolean(newValue: Boolean) = kv.encode(this, newValue)
    fun updateValue(key: String, b: Boolean) = key.updateBoolean(b)
    fun encodeInt(key: String, int: Int) = key.updateInt(int)
    fun getValue(key: String): Boolean = key.getBoolean()
    fun encodeString(key: String, string: String) = key.updateString(string)
    fun containsKey(key: String) = kv.containsKey(key)

    object Enumerations {
        inline fun <reified T : Enum<T>> encodeValue(
            key: String,
            value: T
        ) {
            kv.encode(key, value.ordinal)
        }

        inline fun <reified T : Enum<T>> getValue(
            key: String,
            defaultValue: T
        ): T {
            val ordinal = kv.decodeInt(key, defaultValue.ordinal)
            return enumValues<T>().getOrNull(ordinal) ?: defaultValue
        }
    }

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
    val AppMainSettingsStateFlow = mutableAppMainSettingsStateFlow.asStateFlow()
}