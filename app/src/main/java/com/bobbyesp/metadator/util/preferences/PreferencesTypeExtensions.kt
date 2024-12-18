package com.bobbyesp.metadator.util.preferences

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.bobbyesp.utilities.Preferences.Companion.getBoolean
import com.bobbyesp.utilities.Preferences.Companion.getInt
import com.bobbyesp.utilities.Preferences.Companion.getString

inline val String.booleanState
    @Composable get() = remember("${this}_boolean_state") {
        mutableStateOf(this.getBoolean())
    }

inline val String.stringState
    @Composable get() = remember("${this}_string_state") {
        mutableStateOf(this.getString())
    }

inline val String.intState
    @Composable get() = remember("${this}_int_state") {
        mutableIntStateOf(this.getInt())
    }