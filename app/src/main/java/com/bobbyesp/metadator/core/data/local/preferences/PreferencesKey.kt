package com.bobbyesp.metadator.core.data.local.preferences

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.bobbyesp.metadator.core.data.local.DarkThemePreference.Companion.DarkThemeValue
import com.bobbyesp.metadator.domain.enums.LayoutType
import com.bobbyesp.metadator.presentation.components.cards.songs.compact.CompactCardSize
import com.bobbyesp.utilities.ui.DEFAULT_SEED_COLOR
import com.materialkolor.PaletteStyle

sealed class PreferencesKey<T>(val key: Preferences.Key<T>, val defaultValue: T) {
    // --> UI
    data object SONGS_LAYOUT :
        PreferencesKey<String>(stringPreferencesKey("songs_layout"), LayoutType.Grid.name)

    data object REDUCE_SHADOWS :
        PreferencesKey<Boolean>(booleanPreferencesKey("reduce_shadows"), false)

    data object MARQUEE_TEXT_ENABLED :
        PreferencesKey<Boolean>(booleanPreferencesKey("marquee_text_enabled"), true)

    data object SONG_CARD_SIZE :
        PreferencesKey<String>(stringPreferencesKey("song_card_size"), CompactCardSize.LARGE.name)

    // --> Theming
    data object DARK_THEME_VALUE : PreferencesKey<String>(
        stringPreferencesKey("dark_theme_value"),
        DarkThemeValue.FOLLOW_SYSTEM.name
    )

    data object HIGH_CONTRAST :
        PreferencesKey<Boolean>(booleanPreferencesKey("high_contrast"), false)

    data object USE_DYNAMIC_COLORING :
        PreferencesKey<Boolean>(booleanPreferencesKey("dynamic_coloring"), true)

    data object THEME_COLOR :
        PreferencesKey<Int>(intPreferencesKey("theme_color"), DEFAULT_SEED_COLOR)

    data object PALETTE_STYLE :
        PreferencesKey<String>(stringPreferencesKey("palette_style"), PaletteStyle.Vibrant.name)
}