package com.bobbyesp.metadator.presentation.common

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController

val NavHostController.canGoBack: Boolean
    get() = this.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED

fun NavHostController.navigateBack() {
    if (canGoBack) {
        popBackStack()
    }
}