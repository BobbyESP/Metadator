package com.bobbyesp.metadator.core.presentation

import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.navigation
import com.bobbyesp.metadator.core.presentation.common.Route
import com.bobbyesp.metadator.core.presentation.pages.settings.SettingsPage
import com.bobbyesp.metadator.core.presentation.pages.settings.modules.GeneralSettingsPage
import com.bobbyesp.ui.motion.animatedComposable

fun NavGraphBuilder.settingsRouting(
    onNavigateBack: () -> Unit
) {
    navigation<Route.SettingsNavigator>(
        startDestination = Route.SettingsNavigator.Settings,
    ) {
        animatedComposable<Route.SettingsNavigator.Settings> {
            SettingsPage(
                onBackPressed = onNavigateBack
            )
        }

        animatedComposable<Route.SettingsNavigator.Settings.General> {
            GeneralSettingsPage()
        }

        animatedComposable<Route.SettingsNavigator.Settings.Appearance> {
            Scaffold(
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = {}
                    ) { }
                }
            ) { padding ->

            }
        }

        animatedComposable<Route.SettingsNavigator.Settings.About> {
            Text("About")
        }
    }
}