package com.bobbyesp.metadator.core.data.local.preferences

import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow

interface AppPreferencesController {
    val TAG get() = "AppPreferencesController"

    val userPreferencesFlow: Flow<UserPreferences>

    suspend fun <T> saveSetting(key: Preferences.Key<T>, value: T)
    fun <T> getSettingFlow(key: Preferences.Key<T>, defaultValue: T?): Flow<T>
    suspend fun <T> getSetting(key: Preferences.Key<T>, defaultValue: T?): T
}