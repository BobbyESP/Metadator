/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/**
 * The only thing a feature is given of the back stack. A feature says where it wants to go; it
 * never reads, reorders or replaces the stack.
 */
@Stable
interface Navigator {
    /** Goes to [key]. Asking for the destination already on top does nothing. */
    fun goTo(key: NavKey)

    /** Leaves the current destination; at the root, leaves the host ([onExit]). */
    fun goBack()

    /** Takes [key] off the stack wherever it sits, for a destination whose subject is gone. */
    fun removeDestination(key: NavKey)
}

@Composable
fun rememberNavigator(backStack: NavBackStack<NavKey>, onExit: () -> Unit = {}): Navigator =
    remember(backStack, onExit) { BackStackNavigator(backStack, onExit) }

internal class BackStackNavigator(
    private val backStack: NavBackStack<NavKey>,
    private val onExit: () -> Unit,
) : Navigator {

    override fun goTo(key: NavKey) {
        if (backStack.lastOrNull() == key) return
        backStack.add(key)
    }

    /** `NavDisplay` needs a non-empty stack, so the root is never popped; the host is left. */
    override fun goBack() {
        if (backStack.size <= 1) onExit() else backStack.removeLastOrNull()
    }

    override fun removeDestination(key: NavKey) {
        if (backStack.size == 1 && backStack.single() == key) {
            onExit()
            return
        }
        while (backStack.size > 1 && backStack.contains(key)) backStack.remove(key)
    }
}
