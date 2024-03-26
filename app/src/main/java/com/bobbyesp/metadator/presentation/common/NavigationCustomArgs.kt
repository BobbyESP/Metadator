package com.bobbyesp.metadator.presentation.common

import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.navigation.NavType
import com.bobbyesp.metadator.model.SelectedSong
import kotlinx.serialization.json.Json

@Suppress("DEPRECATION")
val SelectedSongParamType = object : NavType<SelectedSong>(isNullableAllowed = false) {
    override fun get(bundle: Bundle, key: String): SelectedSong? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bundle.getParcelable(NavArgs.SelectedSong.key, SelectedSong::class.java)
        } else {
            bundle.getParcelable(NavArgs.SelectedSong.key)
        }
    }

    override fun put(bundle: Bundle, key: String, value: SelectedSong) {
        bundle.putParcelable(key, value)
    }

    override fun parseValue(value: String): SelectedSong {
        return Json.decodeFromString(Uri.decode(value))
    }
}

@Suppress("DEPRECATION")
val TagEditorSelectedSongParamType = object : NavType<SelectedSong>(isNullableAllowed = false) {
    override fun get(bundle: Bundle, key: String): SelectedSong? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bundle.getParcelable(NavArgs.TagEditorSelectedSong.key, SelectedSong::class.java)
        } else {
            bundle.getParcelable(NavArgs.TagEditorSelectedSong.key)
        }
    }

    override fun put(bundle: Bundle, key: String, value: SelectedSong) {
        bundle.putParcelable(key, value)
    }

    override fun parseValue(value: String): SelectedSong {
        return Json.decodeFromString(Uri.decode(value))
    }
}