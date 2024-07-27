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

/**
 * Extension function for NavHostController to navigate to a destination while cleaning up the back stack.
 *
 * @param T The type of the destination, which must be a subclass of Any.
 * @param destination The destination to navigate to.
 */
fun <T : Any> NavHostController.cleanNavigate(destination: T) = navigate(destination) {
    // Pop up to the start destination of the graph, saving the state
    popUpTo(graph.startDestinationId) {
        saveState = true
    }
    // Launch the destination as a single top instance
    launchSingleTop = true
    // Restore the state if possible
    restoreState = true
}