package com.bobbyesp.metadator.core.data.local.preferences.datastore

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bobbyesp.metadator.App.Companion.PREFERENCES_NAME
import com.bobbyesp.metadator.core.data.local.preferences.PreferencesKey
import com.bobbyesp.metadator.core.presentation.common.LocalAppPreferencesController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = PREFERENCES_NAME,
    corruptionHandler = ReplaceFileCorruptionHandler(
        produceNewData = { emptyPreferences() }
    ),
    //migrations = emptyList(),
    scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
)

@Composable
fun <T> rememberPreferenceState(
    key: PreferencesKey<T>,
    defaultValue: T = key.defaultValue
): Pair<androidx.compose.runtime.State<T>, (T) -> Unit> {
    val appPreferences = LocalAppPreferencesController.current
    val coroutineScope = rememberCoroutineScope()

    val preferenceFlow =
        remember { appPreferences.getSettingFlow(key, defaultValue).distinctUntilChanged() }
    val valueState = preferenceFlow.collectAsStateWithLifecycle(initialValue = defaultValue)

    val updatePreference: (T) -> Unit = { newValue ->
        if (valueState.value != newValue) {
            coroutineScope.launch(Dispatchers.IO) {
                appPreferences.saveSetting(key, newValue)
            }
        }
    }

    return valueState to updatePreference
}