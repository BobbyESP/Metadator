package com.bobbyesp.metadator.presentation.common

import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.navigation.NavType
import com.bobbyesp.metadator.model.ParcelableSong
import kotlinx.serialization.json.Json

@Suppress("DEPRECATION")
val ParcelableSongParamType = object : NavType<ParcelableSong>(isNullableAllowed = false) {
    override fun get(bundle: Bundle, key: String): ParcelableSong? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bundle.getParcelable(NavArgs.SelectedSong.key, ParcelableSong::class.java)
        } else {
            bundle.getParcelable(NavArgs.SelectedSong.key)
        }
    }

    override fun put(bundle: Bundle, key: String, value: ParcelableSong) {
        bundle.putParcelable(key, value)
    }

    override fun parseValue(value: String): ParcelableSong {
        return Json.decodeFromString(Uri.decode(value))
    }
}

@Suppress("DEPRECATION")
val TagEditorParcelableSongParamType = object : NavType<ParcelableSong>(isNullableAllowed = false) {
    override fun get(bundle: Bundle, key: String): ParcelableSong? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bundle.getParcelable(NavArgs.TagEditorSelectedSong.key, ParcelableSong::class.java)
        } else {
            bundle.getParcelable(NavArgs.TagEditorSelectedSong.key)
        }
    }

    override fun put(bundle: Bundle, key: String, value: ParcelableSong) {
        bundle.putParcelable(key, value)
    }

    override fun parseValue(value: String): ParcelableSong {
        return Json.decodeFromString(Uri.decode(value))
    }
}