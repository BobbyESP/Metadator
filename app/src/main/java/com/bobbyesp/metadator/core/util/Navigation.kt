package com.bobbyesp.metadator.core.util

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController

/**
 * Determines whether the navigation controller can navigate back.
 *
 * This property checks if the current back stack entry's lifecycle state is RESUMED. If the current
 * entry is in the RESUMED state, it indicates that it's currently visible and interacting with the
 * user, and therefore it's safe to navigate back from it. If the current entry is not in the
 * RESUMED state (e.g., it's in CREATED, STARTED, or DESTROYED), navigating back might lead to
 * unexpected behavior or UI inconsistencies.
 *
 * @return `true` if the navigation controller can go back, `false` otherwise.
 */
val NavHostController.canGoBack: Boolean
    get() = this.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED

/**
 * Navigates back to the previous destination in the navigation stack, if possible.
 *
 * This function checks if the navigation controller can go back using
 * [NavHostController.canGoBack]. If it can, it pops the current destination off the stack using
 * [NavHostController.popBackStack], effectively navigating to the previous destination. If there's
 * no previous destination to go back to, this function does nothing.
 *
 * @receiver The [NavHostController] instance that manages the navigation stack.
 */
fun NavHostController.navigateBack() {
    if (canGoBack) {
        popBackStack()
    }
}

/**
 * Extension function for NavHostController to navigate to a destination while cleaning up the back
 * stack.
 *
 * @param T The type of the destination, which must be a subclass of Any.
 * @param destination The destination to navigate to.
 */
fun <T : Any> NavHostController.cleanNavigate(destination: T) =
    navigate(destination) {
        // Pop up to the start destination of the graph, saving the state
        popUpTo(graph.startDestinationId) { saveState = true }
        // Launch the destination as a single top instance
        launchSingleTop = true
        // Restore the state if possible
        restoreState = true
    }
