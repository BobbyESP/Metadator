package com.bobbyesp.metadator.util.preferences

import com.bobbyesp.metadator.util.theme.DarkThemePreference

object PreferencesKeys {
    //------------GENERAL--------------
    const val DESIRED_LAYOUT = "desired_overlay"
    const val SONG_CARD_SIZE = "song_card_size"
    const val MARQUEE_TEXT = "marquee_text"
    const val REDUCE_SHADOWS = "reduce_shadows"

    //------------THEME----------------
    const val DARK_THEME_VALUE = "dark_theme_value"
    const val HIGH_CONTRAST = "high_contrast"
    const val THEME_COLOR = "theme_color"
    const val PALETTE_STYLE = "palette_style"
    const val DYNAMIC_COLOR = "dynamic_color"

    const val MMKV_PREFERENCES_NAME = "metadator_preferences"

    val StringPreferenceDefaults: Map<String, String> =
        mapOf()

    val BooleanPreferenceDefaults: Map<String, Boolean> =
        mapOf(
            MARQUEE_TEXT to true,
            REDUCE_SHADOWS to false
        )

    val IntPreferenceDefaults: Map<String, Int> =
        mapOf(
            DARK_THEME_VALUE to DarkThemePreference.FOLLOW_SYSTEM,
            SONG_CARD_SIZE to 2 //CompactCardSize.LARGE
        )
}