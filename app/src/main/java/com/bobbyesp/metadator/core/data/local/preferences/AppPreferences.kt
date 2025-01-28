package com.bobbyesp.metadator.core.data.local.preferences

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.bobbyesp.metadator.core.data.local.DarkThemePreference
import com.bobbyesp.metadator.core.data.local.DarkThemePreference.Companion.DarkThemeValue
import com.bobbyesp.metadator.core.data.local.preferences.PreferencesKey.DARK_THEME_VALUE
import com.bobbyesp.metadator.core.data.local.preferences.PreferencesKey.HIGH_CONTRAST
import com.bobbyesp.metadator.core.data.local.preferences.PreferencesKey.MARQUEE_TEXT_ENABLED
import com.bobbyesp.metadator.core.data.local.preferences.PreferencesKey.PALETTE_STYLE
import com.bobbyesp.metadator.core.data.local.preferences.PreferencesKey.REDUCE_SHADOWS
import com.bobbyesp.metadator.core.data.local.preferences.PreferencesKey.SONGS_LAYOUT
import com.bobbyesp.metadator.core.data.local.preferences.PreferencesKey.SONG_CARD_SIZE
import com.bobbyesp.metadator.core.data.local.preferences.PreferencesKey.THEME_COLOR
import com.bobbyesp.metadator.core.data.local.preferences.PreferencesKey.USE_DYNAMIC_COLORING
import com.bobbyesp.metadator.domain.enums.LayoutType
import com.bobbyesp.metadator.presentation.components.cards.songs.compact.CompactCardSize
import com.bobbyesp.metadator.presentation.theme.isDynamicColoringSupported
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
) : AppPreferencesController {

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

    override suspend fun getUserPreferences(): UserPreferences {
        val preferences = dataStore.data.firstOrNull()
        return mapUserPreferences(preferences ?: emptyPreferences())
    }

    // --> UI
    suspend fun updateSongsLayout(layoutType: LayoutType) {
        saveSetting(SONGS_LAYOUT, layoutType.name)
    }

    suspend fun updateReduceShadows(reduceShadows: Boolean) {
        saveSetting(REDUCE_SHADOWS, reduceShadows)
    }

    suspend fun updateMarqueeTextEnabled(marqueeTextEnabled: Boolean) {
        saveSetting(MARQUEE_TEXT_ENABLED, marqueeTextEnabled)
    }

    suspend fun updateSongCardSize(songCardSize: CompactCardSize) {
        saveSetting(SONG_CARD_SIZE, songCardSize.name)
    }

    // --> Theming
    suspend fun updateDarkThemeValue(darkThemeValue: DarkThemeValue) {
        saveSetting(DARK_THEME_VALUE, darkThemeValue.name)
    }

    suspend fun updateHighContrast(highContrast: Boolean) {
        saveSetting(HIGH_CONTRAST, highContrast)
    }

    suspend fun updateDarkThemePreferences(darkThemePreference: DarkThemePreference) {
        saveSetting(DARK_THEME_VALUE, darkThemePreference.darkThemeValue.name)
        saveSetting(HIGH_CONTRAST, darkThemePreference.isHighContrastModeEnabled)
    }

    suspend fun updateDynamicColoring(dynamicColoring: Boolean, onCantEnable: () -> Unit) {
        if (dynamicColoring && !isDynamicColoringSupported()) {
            onCantEnable()
            saveSetting(USE_DYNAMIC_COLORING, false)
            return
        } else {
            saveSetting(USE_DYNAMIC_COLORING, dynamicColoring)
        }
    }

    suspend fun updateThemeColor(themeColor: Int) {
        saveSetting(THEME_COLOR, themeColor)
    }

    suspend fun updatePaletteStyle(paletteStyle: PaletteStyle) {
        saveSetting(PALETTE_STYLE, paletteStyle.name)
    }

    private fun mapUserPreferences(
        preferences: Preferences
    ): UserPreferences {
        val desiredLayout: LayoutType =
            LayoutType.valueOf(preferences[SONGS_LAYOUT.key] ?: SONGS_LAYOUT.defaultValue)
        val reduceShadows: Boolean = preferences[REDUCE_SHADOWS.key] ?: REDUCE_SHADOWS.defaultValue
        val marqueeTextEnabled: Boolean =
            preferences[MARQUEE_TEXT_ENABLED.key] ?: MARQUEE_TEXT_ENABLED.defaultValue
        val songCardSize: CompactCardSize =
            CompactCardSize.valueOf(preferences[SONG_CARD_SIZE.key] ?: SONG_CARD_SIZE.defaultValue)
        val useDynamicColoring: Boolean =
            preferences[USE_DYNAMIC_COLORING.key] ?: USE_DYNAMIC_COLORING.defaultValue
        val themeColor: Int = preferences[THEME_COLOR.key] ?: THEME_COLOR.defaultValue
        val paletteStyle: PaletteStyle =
            PaletteStyle.valueOf(preferences[PALETTE_STYLE.key] ?: PALETTE_STYLE.defaultValue)

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
        val currentDarkThemeValue: DarkThemeValue = DarkThemeValue.valueOf(
            preferences[DARK_THEME_VALUE.key] ?: DARK_THEME_VALUE.defaultValue
        )
        val highContrast: Boolean = preferences[HIGH_CONTRAST.key] ?: HIGH_CONTRAST.defaultValue
        return DarkThemePreference(currentDarkThemeValue, highContrast)
    }

    override suspend fun <T> saveSetting(key: PreferencesKey<T>, value: T) {
        dataStore.edit { preferences ->
            when (value) {
                is String -> preferences[key.key] = value
                is Boolean -> preferences[key.key] = value
                is Int -> preferences[key.key] = value
                else -> throw IllegalArgumentException("Unsupported type: ${value!!::class.simpleName}")
            }
        }
    }


    override fun <T> getSettingFlow(key: PreferencesKey<T>, defaultValue: T?): Flow<T> {
        return dataStore.data
            .map { preferences ->
                preferences[key.key] ?: defaultValue ?: key.defaultValue
            }
    }

    override suspend fun <T> getSetting(key: PreferencesKey<T>, defaultValue: T?): T {
        val preferences = dataStore.data.firstOrNull()
        return preferences?.get(key.key) ?: defaultValue ?: key.defaultValue
    }

}