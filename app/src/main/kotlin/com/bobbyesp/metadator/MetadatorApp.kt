/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import com.bobbyesp.metadator.core.designsystem.theme.Spacing
import com.bobbyesp.metadator.core.designsystem.theme.outOfFocus
import com.bobbyesp.metadator.core.navigation.Collection
import com.bobbyesp.metadator.core.navigation.Editor
import com.bobbyesp.metadator.core.navigation.Library
import com.bobbyesp.metadator.core.navigation.MetadatorNavDisplay
import com.bobbyesp.metadator.core.navigation.overlay.rememberOverlaySceneStrategy
import com.bobbyesp.metadator.core.navigation.pane.sharingTheWindow
import com.bobbyesp.metadator.core.navigation.rememberNavigator
import com.bobbyesp.metadator.core.ui.viewmodel.LocalSnackbarHostState
import com.bobbyesp.metadator.feature.batch.batchSection
import com.bobbyesp.metadator.feature.editor.editorSection
import com.bobbyesp.metadator.feature.library.librarySection
import com.bobbyesp.metadator.feature.player.PlayerLayout
import com.bobbyesp.metadator.feature.player.PlayerSheet
import com.bobbyesp.metadator.feature.player.PlayerSheetDefaults
import com.bobbyesp.metadator.feature.player.rememberPlayerSheetExpansion
import com.bobbyesp.metadator.feature.settings.settingsSection
import com.bobbyesp.metadator.player.api.PlayerController
import org.koin.compose.koinInject

/**
 * The app shell: one back stack, one display. Strategies decide the layout: overlays first, then
 * list-detail, which puts the editor beside the library on a wide window. The shell owns what
 * floats over every screen: the snackbar and the player, which is a bar that opens to full screen.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun MetadatorApp(onExit: () -> Unit) {
    val backStack = rememberNavBackStack(Library)
    val navigator = rememberNavigator(backStack, onExit)
    val overlay = rememberOverlaySceneStrategy<NavKey>()
    val listDetail = rememberListDetailSceneStrategy<NavKey>()
    val strategies =
        remember(overlay, listDetail) { listOf(overlay, listDetail.sharingTheWindow()) }
    val snackbarHostState = LocalSnackbarHostState.current

    // The player belongs to browsing; the editors have their own controls and need the room.
    val top = backStack.lastOrNull()
    val showPlayer = top == Library || top is Collection
    val playback by koinInject<PlayerController>().state.collectAsStateWithLifecycle()
    val snackbarClearance by
        animateDpAsState(
            targetValue =
                if (showPlayer && playback.isActive) PlayerSheetDefaults.CollapsedClearance
                else Spacing.medium,
            label = "SnackbarClearance",
        )

    // The player is not a destination, so it is not told how much room it has as one is: the shell
    // measures the window for it.
    val window = LocalWindowInfo.current.containerSize
    val playerLayout =
        with(LocalDensity.current) {
            PlayerLayout.forWindow(width = window.width.toDp(), height = window.height.toDp())
        }
    val playerExpansion = rememberPlayerSheetExpansion()
    // Nothing once the player covers the screen: it is opaque, and what is behind it would be
    // blurred again on every frame for nobody to see.
    val behindPlayer =
        remember(playerExpansion) { { playerExpansion.fraction.let { if (it < 1f) it else 0f } } }

    Surface(color = MaterialTheme.colorScheme.surface) {
        Box(Modifier.fillMaxSize()) {
            // Everything the player opens over goes out of focus as it does.
            Box(Modifier.fillMaxSize().outOfFocus(behindPlayer, MaterialTheme.colorScheme.scrim)) {
                MetadatorNavDisplay(
                    backStack = backStack,
                    navigator = navigator,
                    sceneStrategies = strategies,
                    entryProvider =
                        entryProvider {
                            librarySection(navigator)
                            editorSection(navigator)
                            batchSection(navigator)
                            settingsSection(navigator)
                        },
                )
                SnackbarHost(
                    snackbarHostState,
                    modifier =
                        Modifier.align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = snackbarClearance),
                )
            }
            // Last, so that opened it covers everything, the snackbar included.
            PlayerSheet(
                visible = showPlayer,
                onEdit = { uri -> navigator.goTo(Editor(uri)) },
                layout = playerLayout,
                expansion = playerExpansion,
            )
        }
    }
}
