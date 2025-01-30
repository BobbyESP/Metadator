package com.bobbyesp.metadator.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.navigation
import com.bobbyesp.metadator.core.presentation.common.Route
import com.bobbyesp.metadator.core.util.getNeededStoragePermissions
import com.bobbyesp.metadator.onboarding.presentation.pages.OnboardingPermissionsPage
import com.bobbyesp.metadator.onboarding.presentation.pages.OnboardingWelcomePage
import com.bobbyesp.ui.motion.animatedComposable
import com.bobbyesp.utilities.ui.permission.PermissionType.Companion.toPermissionType

fun NavGraphBuilder.onboardingNavigation(
    onNavigate: (Route) -> Unit,
    onCompletedOnboarding: () -> Unit
) {
    navigation<Route.OnboardingNavigator>(
        startDestination = Route.OnboardingNavigator.Welcome::class,
    ) {
        animatedComposable<Route.OnboardingNavigator.Welcome> {
            OnboardingWelcomePage(
                onGetStarted = {
                    onNavigate(Route.OnboardingNavigator.Permissions)
                }
            )
        }

        animatedComposable<Route.OnboardingNavigator.Permissions> {

            val neededPermissions by remember { mutableStateOf(getNeededStoragePermissions().map { it.toPermissionType() }) }

            OnboardingPermissionsPage(
                neededPermissions = neededPermissions,
                onNextClick = {
                    onCompletedOnboarding()
                }
            )
        }
    }
}