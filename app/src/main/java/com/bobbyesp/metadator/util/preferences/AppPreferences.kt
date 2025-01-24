package com.bobbyesp.metadator.util.preferences

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.bobbyesp.metadator.domain.enums.LayoutType
import com.bobbyesp.metadator.presentation.components.cards.songs.compact.CompactCardSize
import com.bobbyesp.metadator.util.theme.DarkThemePreference
import com.bobbyesp.utilities.ui.DEFAULT_SEED_COLOR
import com.materialkolor.PaletteStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class AppPreferences(
    private val dataStore: DataStore<Preferences>,
    scope: CoroutineScope
) {
    var desiredLayout by PreferenceDelegate<String, LayoutType>(
        dataStore = dataStore,
        key = DESIRED_LAYOUT,
        defaultValue = LayoutType.Grid,
        transform = {
            Log.d("AppPreferences", "Transforming desired layout: $it")
            LayoutType.valueOf(it as String)
        }
    )

    var reduceShadows by PreferenceDelegate(
        dataStore = dataStore,
        key = REDUCE_SHADOWS,
        defaultValue = false
    )

    var marqueeTextEnabled by PreferenceDelegate(
        dataStore = dataStore,
        key = MARQUEE_TEXT_ENABLED,
        defaultValue = true
    )

    var songCardSize by PreferenceDelegate<String, CompactCardSize>(
        dataStore = dataStore,
        key = SONG_CARD_SIZE,
        defaultValue = CompactCardSize.LARGE,
        transform = {
            Log.d("AppPreferences", "Transforming song card size: $it")
            CompactCardSize.valueOf(it as String)
        }
    )

    var darkMode by PreferenceDelegate(
        dataStore = dataStore,
        key = DARK_THEME_VALUE,
        defaultValue = DarkThemePreference.FOLLOW_SYSTEM
    )

    val highContrast by PreferenceDelegate(
        dataStore = dataStore,
        key = HIGH_CONTRAST,
        defaultValue = false
    )

    var useDynamicColoring by PreferenceDelegate(
        dataStore = dataStore,
        key = USE_DYNAMIC_COLORING,
        defaultValue = true
    )

    var themeColor by PreferenceDelegate(
        dataStore = dataStore,
        key = THEME_COLOR,
        defaultValue = DEFAULT_SEED_COLOR
    )

    var paletteStyle by PreferenceDelegate<String, PaletteStyle>(
        dataStore = dataStore,
        key = PALETTE_STYLE,
        defaultValue = PaletteStyle.Vibrant,
        transform = {
            Log.d("AppPreferences", "Transforming palette style: $it")
            PaletteStyle.valueOf(it as String)
        }
    )

    suspend fun <T> saveSetting(key: Preferences.Key<T>, value: T) {
        Log.d("AppPreferences", "Saving setting: $key = $value")
        dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    fun <T> getSettingFlow(key: Preferences.Key<T>, defaultValue: T?): Flow<T> {
        return dataStore.data
            .map { preferences ->
                preferences[key] ?: defaultValue ?: error("No default value found for $key")
            }
    }

    suspend fun <T> getSetting(key: Preferences.Key<T>, defaultValue: T?): T {
        val preferences = dataStore.data.firstOrNull()
        return preferences?.get(key) ?: defaultValue ?: error("No default value found for $key")
    }

    companion object {

        // --> UI
        val DESIRED_LAYOUT = stringPreferencesKey("desired_layout")
        val REDUCE_SHADOWS = booleanPreferencesKey("reduce_shadows")
        val MARQUEE_TEXT_ENABLED = booleanPreferencesKey("marquee_text_enabled")
        val SONG_CARD_SIZE = stringPreferencesKey("song_card_size")

        // --> Theming
        val DARK_THEME_VALUE = intPreferencesKey("dark_theme_value")
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
        val USE_DYNAMIC_COLORING = booleanPreferencesKey("dynamic_coloring")
        val THEME_COLOR = intPreferencesKey("theme_color")
        val PALETTE_STYLE = stringPreferencesKey("palette_style")

        val StringPreferenceDefaults: List<Preferences.Pair<String>> = listOf(
            DESIRED_LAYOUT to LayoutType.Grid.name,
            SONG_CARD_SIZE to CompactCardSize.LARGE.name,
            PALETTE_STYLE to PaletteStyle.Vibrant.name
        )

        val BooleanPreferenceDefaults: List<Preferences.Pair<Boolean>> = listOf(
            MARQUEE_TEXT_ENABLED to true,
            REDUCE_SHADOWS to false,
            HIGH_CONTRAST to false,
            USE_DYNAMIC_COLORING to true
        )

        val IntPreferenceDefaults: List<Preferences.Pair<Int>> = listOf(
            DARK_THEME_VALUE to DarkThemePreference.FOLLOW_SYSTEM,
            THEME_COLOR to DEFAULT_SEED_COLOR
        )
    }
}