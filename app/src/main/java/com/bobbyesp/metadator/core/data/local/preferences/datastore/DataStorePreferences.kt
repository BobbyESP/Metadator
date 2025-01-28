package com.bobbyesp.metadator.core.data.local.preferences.datastore

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bobbyesp.metadator.App.Companion.APP_PACKAGE_NAME
import com.bobbyesp.metadator.presentation.common.LocalAppPreferencesController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "${APP_PACKAGE_NAME}_preferences",
)

@Composable
fun <T> rememberPreference(
    key: Preferences.Key<T>
): MutableState<T> {
    val appPreferences = LocalAppPreferencesController.current
    val coroutineScope = rememberCoroutineScope()

    val defaultValue = remember { appPreferences.getDefaultForKey(key) }

    val preferenceFlow = remember { appPreferences.getSettingFlow(key, defaultValue).distinctUntilChanged() }

    val valueState = preferenceFlow.collectAsStateWithLifecycle(initialValue = defaultValue)

    return remember(valueState, coroutineScope) {
        object : MutableState<T> {
            override var value: T
                get() = valueState.value
                set(newValue) {
                    if (valueState.value != newValue) {
                        coroutineScope.launch(Dispatchers.IO) {
                            appPreferences.saveSetting(key, newValue)
                        }
                    }
                }

            override fun component1() = value
            override fun component2(): (T) -> Unit = { value = it }
        }
    }
}