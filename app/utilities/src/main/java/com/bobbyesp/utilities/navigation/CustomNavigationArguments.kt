package com.bobbyesp.utilities.navigation

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.navigation.NavType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Creates a custom NavType for Parcelable types.
 *
 * @param T The type of Parcelable.
 * @param isNullableAllowed Whether null values are allowed.
 * @param json The Json instance used for serialization and deserialization.
 * @return A NavType for the specified Parcelable type.
 */
inline fun <reified T : Parcelable> parcelableType(
    isNullableAllowed: Boolean = false,
    json: Json = Json,
) =
    object : NavType<T>(isNullableAllowed = isNullableAllowed) {
        /**
         * Retrieves the Parcelable from the Bundle.
         *
         * @param bundle The Bundle containing the Parcelable.
         * @param key The key associated with the Parcelable.
         * @return The Parcelable object.
         */
        override fun get(bundle: Bundle, key: String) =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bundle.getParcelable(key, T::class.java)
            } else {
                @Suppress("DEPRECATION") bundle.getParcelable(key)
            }

        /**
         * Parses a Parcelable from a String.
         *
         * @param value The String to parse.
         * @return The parsed Parcelable object.
         */
        override fun parseValue(value: String): T = json.decodeFromString(value)

        /**
         * Serializes a Parcelable to a String.
         *
         * @param value The Parcelable to serialize.
         * @return The serialized String.
         */
        override fun serializeAsValue(value: T): String = Uri.encode(json.encodeToString(value))

        /**
         * Puts a Parcelable into a Bundle.
         *
         * @param bundle The Bundle to put the Parcelable into.
         * @param key The key associated with the Parcelable.
         * @param value The Parcelable to put into the Bundle.
         */
        override fun put(bundle: Bundle, key: String, value: T) = bundle.putParcelable(key, value)
    }

/**
 * Creates a custom NavType for serializable types.
 *
 * @param T The type of the serializable object.
 * @param isNullableAllowed Whether null values are allowed.
 * @param json The Json instance used for serialization and deserialization.
 * @return A NavType for the specified serializable type.
 */
inline fun <reified T : Any> serializableType(
    isNullableAllowed: Boolean = false,
    json: Json = Json,
) =
    object : NavType<T>(isNullableAllowed = isNullableAllowed) {
        /**
         * Retrieves the serializable object from the Bundle.
         *
         * @param bundle The Bundle containing the serializable object.
         * @param key The key associated with the serializable object.
         * @return The serializable object.
         */
        override fun get(bundle: Bundle, key: String) =
            bundle.getString(key)?.let<String, T>(json::decodeFromString)

        /**
         * Parses a serializable object from a String.
         *
         * @param value The String to parse.
         * @return The parsed serializable object.
         */
        override fun parseValue(value: String): T = json.decodeFromString(value)

        /**
         * Serializes a serializable object to a String.
         *
         * @param value The serializable object to serialize.
         * @return The serialized String.
         */
        override fun serializeAsValue(value: T): String = Uri.encode(json.encodeToString(value))

        /**
         * Puts a serializable object into a Bundle.
         *
         * @param bundle The Bundle to put the serializable object into.
         * @param key The key associated with the serializable object.
         * @param value The serializable object to put into the Bundle.
         */
        override fun put(bundle: Bundle, key: String, value: T) {
            bundle.putString(key, json.encodeToString(value))
        }
    }
