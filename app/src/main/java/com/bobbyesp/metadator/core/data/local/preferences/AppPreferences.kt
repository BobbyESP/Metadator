package com.bobbyesp.metadator.core.data.local.preferences

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.bobbyesp.metadator.core.data.local.DarkThemePreference
import com.bobbyesp.metadator.domain.enums.LayoutType
import com.bobbyesp.metadator.presentation.components.cards.songs.compact.CompactCardSize
import com.bobbyesp.utilities.ui.DEFAULT_SEED_COLOR
import com.materialkolor.PaletteStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.io.IOException

class AppPreferences(
    private val dataStore: DataStore<Preferences>,
    scope: CoroutineScope //May be used in the future
): AppPreferencesController {

    override val userPreferencesFlow: Flow<UserPreferences>
        get() = dataStore.data
            .catch { exception ->
                // dataStore.data throws an IOException when an error is encountered when reading data
                if (exception is IOException) {
                    Log.e(TAG, "Error reading preferences.", exception)
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences ->
                mapUserPreferences(preferences)
            }

    private fun mapUserPreferences(
        preferences: Preferences
    ): UserPreferences {
        val desiredLayout: LayoutType = LayoutType.valueOf(preferences[DESIRED_LAYOUT] ?: getDefaultForKey(DESIRED_LAYOUT))
        val reduceShadows: Boolean = preferences[REDUCE_SHADOWS] ?: getDefaultForKey(REDUCE_SHADOWS)
        val marqueeTextEnabled: Boolean = preferences[MARQUEE_TEXT_ENABLED] ?: getDefaultForKey(MARQUEE_TEXT_ENABLED)
        val songCardSize: CompactCardSize = CompactCardSize.valueOf(preferences[SONG_CARD_SIZE] ?: getDefaultForKey(SONG_CARD_SIZE))
        val useDynamicColoring: Boolean = preferences[USE_DYNAMIC_COLORING] ?: getDefaultForKey(USE_DYNAMIC_COLORING)
        val themeColor: Int = preferences[THEME_COLOR] ?: getDefaultForKey(THEME_COLOR)
        val paletteStyle: PaletteStyle = PaletteStyle.valueOf(preferences[PALETTE_STYLE] ?: getDefaultForKey(PALETTE_STYLE))

        val darkThemePreference = mapDarkThemePreferences(preferences)

        return UserPreferences(
            desiredLayout,
            reduceShadows,
            marqueeTextEnabled,
            songCardSize,
            darkThemePreference,
            useDynamicColoring,
            themeColor,
            paletteStyle
        )
    }

    private fun mapDarkThemePreferences(preferences: Preferences): DarkThemePreference {
        val darkThemeValue: Int = preferences[DARK_THEME_VALUE] ?: getDefaultForKey(DARK_THEME_VALUE)
        val highContrast: Boolean = preferences[HIGH_CONTRAST] ?: getDefaultForKey(HIGH_CONTRAST)
        return DarkThemePreference(darkThemeValue, highContrast)
    }

    override suspend fun <T> saveSetting(key: Preferences.Key<T>, value: T) {
        dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    override fun <T> getSettingFlow(key: Preferences.Key<T>, defaultValue: T?): Flow<T> {
        return dataStore.data
            .map { preferences ->
                preferences[key] ?: defaultValue ?: getDefaultForKey(key)
            }
    }

    override suspend fun <T> getSetting(key: Preferences.Key<T>, defaultValue: T?): T {
        val preferences = dataStore.data.firstOrNull()
        return preferences?.get(key) ?: defaultValue ?: getDefaultForKey(key)
    }

    fun <T> getDefaultForKey(key: Preferences.Key<T>): T {
        return when (key) {
            DESIRED_LAYOUT -> LayoutType.Grid.name
            REDUCE_SHADOWS -> false
            MARQUEE_TEXT_ENABLED -> true
            SONG_CARD_SIZE -> CompactCardSize.LARGE.name

            DARK_THEME_VALUE -> DarkThemePreference.FOLLOW_SYSTEM
            HIGH_CONTRAST -> false
            USE_DYNAMIC_COLORING -> true
            THEME_COLOR -> DEFAULT_SEED_COLOR
            PALETTE_STYLE -> PaletteStyle.Vibrant.name

            else -> error("No default value found for $key")
        } as T
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
    }
}