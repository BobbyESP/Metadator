package com.bobbyesp.utilities

import com.tencent.mmkv.MMKV

class Preferences(
    preferencesName: String,
    intPreferenceDefaults: Map<String, Int>,
    stringPreferenceDefaults: Map<String, String>,
    booleanPreferenceDefaults: Map<String, Boolean>
) {
    val kv: MMKV = MMKV.mmkvWithID(preferencesName)

    init {
        Preferences.kv = kv
        setDefaults(intPreferenceDefaults, stringPreferenceDefaults, booleanPreferenceDefaults)
    }

    fun String.updateString(newString: String) = kv.encode(this, newString)

    fun String.updateInt(newInt: Int) = kv.encode(this, newInt)

    fun String.updateBoolean(newValue: Boolean) = kv.encode(this, newValue)
    fun updateValue(key: String, b: Boolean) = key.updateBoolean(b)
    fun encodeInt(key: String, int: Int) = key.updateInt(int)
    fun getValue(key: String): Boolean = key.getBoolean()
    fun encodeString(key: String, string: String) = key.updateString(string)
    fun containsKey(key: String) = kv.containsKey(key)

    object Enumerations {
        inline fun <reified T : Enum<T>> encodeValue(
            key: String,
            value: T
        ) {
            kv.encode(key, value.ordinal)
        }

        inline fun <reified T : Enum<T>> getValue(
            key: String,
            defaultValue: T
        ): T {
            val ordinal = kv.decodeInt(key, defaultValue.ordinal)
            return enumValues<T>().getOrNull(ordinal) ?: defaultValue
        }
    }

    companion object {
        lateinit var kv: MMKV
        private lateinit var intPreferenceDefaults: Map<String, Int>
        private lateinit var stringPreferenceDefaults: Map<String, String>
        private lateinit var booleanPreferenceDefaults: Map<String, Boolean>

        fun setDefaults(
            intDefaults: Map<String, Int>,
            stringDefaults: Map<String, String>,
            booleanDefaults: Map<String, Boolean>
        ) {
            intPreferenceDefaults = intDefaults
            stringPreferenceDefaults = stringDefaults
            booleanPreferenceDefaults = booleanDefaults
        }

        fun String.getInt(default: Int = intPreferenceDefaults.getOrElse(this) { 0 }): Int =
            kv.decodeInt(this, default)

        fun String.getString(default: String = stringPreferenceDefaults.getOrElse(this) { "" }): String =
            kv.decodeString(this) ?: default

        fun String.getBoolean(default: Boolean = booleanPreferenceDefaults.getOrElse(this) { false }): Boolean =
            kv.decodeBool(this, default)
    }
}