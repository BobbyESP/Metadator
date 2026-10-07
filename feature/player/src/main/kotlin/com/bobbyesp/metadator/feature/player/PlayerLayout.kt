/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.feature.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * How the full player is laid out. The player is not a destination and does not measure the window
 * it is in: the shell does, and says which of these fits.
 */
enum class PlayerLayout {
    /** One column: the cover, the controls, then the queue. A phone held upright. */
    Stacked,

    /** The song and its controls on the left, its lyrics or the queue on the right. */
    SideBySide,

    /**
     * [SideBySide] where there is no height for the controls under the cover: they float in a bar
     * over the lyrics instead. A phone on its side.
     */
    SideBySideCompact;

    companion object {
        /** The layout for a window of this size. */
        fun forWindow(width: Dp, height: Dp): PlayerLayout =
            when {
                width < SideBySideMinWidth || width <= height -> Stacked
                height < ControlsMinHeight -> SideBySideCompact
                else -> SideBySide
            }

        private val SideBySideMinWidth = 600.dp
        private val ControlsMinHeight = 600.dp
    }
}

/**
 * How far open the player is, from 0 (the bar) to 1 (the whole screen), for what is behind it to
 * step back as it opens. Read [fraction] while drawing: it follows the finger.
 */
@Stable
class PlayerSheetExpansion internal constructor() {
    internal var source: (() -> Float)? by mutableStateOf(null)

    val fraction: Float
        get() = source?.invoke() ?: 0f
}

@Composable
fun rememberPlayerSheetExpansion(): PlayerSheetExpansion = remember { PlayerSheetExpansion() }
