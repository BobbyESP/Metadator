package com.bobbyesp.metadator.util.preferences

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlin.reflect.KProperty

class PreferenceDelegate<K, V>(
    private val dataStore: DataStore<Preferences>,
    private val key: Preferences.Key<K>,
    defaultValue: V,
    private val transform: (Any) -> V = { it as V }
) : MutableState<V> {

    private var state = mutableStateOf(defaultValue)

    init {
        CoroutineScope(Dispatchers.IO).launch {
            val value = dataStore.data.firstOrNull()?.get(key) ?: defaultValue
            // Safely applying the transform function to the value
            Log.d("PreferenceDelegate", "Loading value: $value for key: $key")
            state.value = transform(
                value ?: throw IllegalArgumentException("Cannot cast value to the expected type")
            )
        }
    }

    override var value: V
        get() = state.value
        set(newValue) {
            state.value = newValue
            CoroutineScope(Dispatchers.IO).launch {
                Log.d("PreferenceDelegate", "Saving value: $newValue")
                dataStore.edit { preferences ->
                    val valueToStore = when (newValue) {
                        is Enum<*> -> newValue.name as? K // Store the enum name (String) in the DataStore
                        else -> newValue as? K
                    } ?: throw IllegalArgumentException("Cannot cast newValue to the expected type")
                    preferences[key] = valueToStore
                }
            }
        }

    override fun component1(): V = value
    override fun component2(): (V) -> Unit = { value = it }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): V = value
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: V) {
        this.value = value
    }
}