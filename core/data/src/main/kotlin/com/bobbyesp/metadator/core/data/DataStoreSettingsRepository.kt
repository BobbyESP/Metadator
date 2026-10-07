package com.bobbyesp.metadator.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.bobbyesp.metadator.core.domain.settings.SettingsRepository
import com.bobbyesp.metadator.core.model.MultiValueMode
import com.bobbyesp.metadator.core.model.PaletteStyle
import com.bobbyesp.metadator.core.model.ThemeMode
import com.bobbyesp.metadator.core.model.UserSettings
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Preferences in DataStore. The file name and the theme keys are the ones 1.x used, so an
 * update keeps the user's theme with no migration.
 */
class DataStoreSettingsRepository(private val dataStore: DataStore<Preferences>) :
    SettingsRepository {

    override val settings: Flow<UserSettings> =
        dataStore.data
            .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
            .map { it.toSettings() }
            .distinctUntilChanged()

    override suspend fun update(transform: (UserSettings) -> UserSettings) {
        dataStore.edit { preferences ->
            val updated = transform(preferences.toSettings())
            preferences[Keys.ThemeMode] =
                when (updated.themeMode) {
                    ThemeMode.FollowSystem -> "FOLLOW_SYSTEM"
                    ThemeMode.Dark -> "ON"
                    ThemeMode.Light -> "OFF"
                }
            preferences[Keys.DynamicColor] = updated.useDynamicColor
            preferences[Keys.SeedColor] = updated.seedColor
            preferences[Keys.PaletteStyle] = updated.paletteStyle.name
            preferences[Keys.PureBlack] = updated.pureBlack
            preferences[Keys.ColorEditorFromArtwork] = updated.colorEditorFromArtwork
            preferences[Keys.MultiValueMode] = updated.multiValueMode.name
            preferences[Keys.MultiValueSeparator] = updated.multiValueSeparator
            preferences[Keys.WarnSmallArtwork] = updated.warnSmallArtwork
            preferences[Keys.EnabledProviders] = updated.enabledProviders
            preferences[Keys.SuccessfulSaves] = updated.successfulSaves
        }
    }

    private fun Preferences.toSettings(): UserSettings {
        val defaults = UserSettings()
        return UserSettings(
            themeMode =
                when (this[Keys.ThemeMode]) {
                    "ON" -> ThemeMode.Dark
                    "OFF" -> ThemeMode.Light
                    else -> ThemeMode.FollowSystem
                },
            useDynamicColor = this[Keys.DynamicColor] ?: defaults.useDynamicColor,
            seedColor = this[Keys.SeedColor] ?: defaults.seedColor,
            paletteStyle = enumOrDefault(this[Keys.PaletteStyle], defaults.paletteStyle),
            pureBlack = this[Keys.PureBlack] ?: defaults.pureBlack,
            colorEditorFromArtwork = this[Keys.ColorEditorFromArtwork] ?: defaults.colorEditorFromArtwork,
            multiValueMode = enumOrDefault(this[Keys.MultiValueMode], defaults.multiValueMode),
            multiValueSeparator = this[Keys.MultiValueSeparator] ?: defaults.multiValueSeparator,
            warnSmallArtwork = this[Keys.WarnSmallArtwork] ?: defaults.warnSmallArtwork,
            enabledProviders = this[Keys.EnabledProviders] ?: defaults.enabledProviders,
            successfulSaves = this[Keys.SuccessfulSaves] ?: defaults.successfulSaves,
        )
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    private object Keys {
        // Shared with 1.x.
        val ThemeMode = stringPreferencesKey("dark_theme_value")
        val DynamicColor = booleanPreferencesKey("dynamic_coloring")
        val SeedColor = intPreferencesKey("theme_color")
        val PaletteStyle = stringPreferencesKey("palette_style")
        val PureBlack = booleanPreferencesKey("high_contrast")

        // New in 2.0.
        val ColorEditorFromArtwork = booleanPreferencesKey("color_editor_from_artwork")
        val MultiValueMode = stringPreferencesKey("multi_value_mode")
        val MultiValueSeparator = stringPreferencesKey("multi_value_separator")
        val WarnSmallArtwork = booleanPreferencesKey("warn_small_artwork")
        val EnabledProviders = stringSetPreferencesKey("enabled_providers")
        val SuccessfulSaves = intPreferencesKey("successful_saves")
    }

    companion object {
        /** 1.x's file name, kept so settings carry over. */
        const val FILE_NAME = "com.bobbyesp.metadator_preferences"
    }
}

/** The preferences file. Create it once per process: DataStore allows one instance per file. */
fun createSettingsDataStore(context: Context): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        produceFile = { context.preferencesDataStoreFile(DataStoreSettingsRepository.FILE_NAME) },
    )
