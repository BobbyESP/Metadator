package com.bobbyesp.metadator.util.preferences

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlin.reflect.KProperty

class PreferenceDelegate<K, V>(
    private val dataStore: DataStore<Preferences>,
    private val key: Preferences.Key<K>,
    defaultValue: V,
    val onValueChange: ((V) -> Unit)? = null
) : MutableState<V> {

    private var state = mutableStateOf(defaultValue)

    init {
        CoroutineScope(Dispatchers.IO).launch {
            val storedValue = dataStore.data.firstOrNull()?.get(key)
            val convertedValue = when {
                storedValue is String && defaultValue is Enum<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    (defaultValue::class.java as Class<Enum<*>>)
                        .enumConstants
                        .find { (it as Enum<*>).name == storedValue } as V
                }
                storedValue == null -> defaultValue
                else -> storedValue as V
            }
            state.value = convertedValue
        }
    }

    override var value: V
        get() = state.value
        set(newValue) {
            state.value = newValue
            onValueChange?.invoke(newValue)
        }

    override fun component1(): V = value
    override fun component2(): (V) -> Unit = { value = it }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): V = value
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: V) {
        this.value = value
    }
}