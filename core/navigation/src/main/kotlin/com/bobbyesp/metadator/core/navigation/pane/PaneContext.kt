/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.navigation.pane

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import kotlin.reflect.KClass

/**
 * What a destination is allowed to know about the layout it landed in.
 *
 * A destination cannot see whether anything is beside it, only how wide the window is, and the two
 * stop agreeing the moment it fills a wide window alone. The strategy put it there, so the strategy
 * answers — see [sharingTheWindow].
 */
@Immutable
class PaneContext
internal constructor(
    /** Whether this destination has the window to itself. */
    val isSolePane: Boolean
) {

    /**
     * Whether the destination has to offer its own way back. Sharing the window means the list it
     * shares with is still on screen and reachable; filling it means there is nothing else to
     * touch.
     */
    val providesOwnBackAffordance: Boolean
        get() = isSolePane

    override fun equals(other: Any?): Boolean =
        this === other || (other is PaneContext && isSolePane == other.isSolePane)

    override fun hashCode(): Int = isSolePane.hashCode()

    override fun toString(): String = "PaneContext(isSolePane=$isSolePane)"
}

/**
 * Defaults to filling the window: the single-pane fallback, `ExternalEditorActivity`, a preview.
 */
val LocalPaneContext = staticCompositionLocalOf { PaneContext(isSolePane = true) }

/**
 * Wraps [this] so the destinations in every scene it produces are told they share the window.
 *
 * Only correct for a strategy that claims the stack **only** when it really does lay them side by
 * side. `ListDetailSceneStrategy` qualifies while `shouldHandleSinglePaneLayout` stays false: it
 * returns null unless `paneCount` came out above one, and what it declines falls through to the
 * single-pane fallback, where [LocalPaneContext]'s default is already right.
 *
 * Asking the scene how many entries it holds would be a guess, and a wrong one: a list alone on a
 * wide window is one entry in a two-pane scaffold, the other pane being a placeholder. The library
 * scene types that could answer honestly are `internal`, so the question goes to whoever decided.
 */
fun <T : Any> SceneStrategy<T>.sharingTheWindow(): SceneStrategy<T> =
    SharedWindowSceneStrategy(this)

internal class SharedWindowSceneStrategy<T : Any>(private val delegate: SceneStrategy<T>) :
    SceneStrategy<T> {

    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val scope = this

        return with(delegate) { scope.calculateScene(entries) }
            ?.let { PaneAwareScene(it, SharedPane) }
    }

    private companion object {
        val SharedPane = PaneContext(isSolePane = false)
    }
}

/**
 * A scene's identity is `(its class, its key)`, so wrapping would make every wrapped scene the same
 * class and `NavDisplay` would stop animating between them. Folding the delegate's class into the
 * key keeps identity as precise as it was.
 */
internal class PaneAwareScene<T : Any>(
    private val delegate: Scene<T>,
    private val paneContext: PaneContext,
) : Scene<T> by delegate {

    /** Exposed for tests: what this scene will tell the destinations it renders. */
    internal val paneContextForTest: PaneContext
        get() = paneContext

    override val key: Any = WrappedKey(delegate::class, delegate.key)

    override val content: @Composable () -> Unit = {
        CompositionLocalProvider(LocalPaneContext provides paneContext) { delegate.content() }
    }

    override fun equals(other: Any?): Boolean =
        this === other ||
            (other is PaneAwareScene<*> &&
                delegate == other.delegate &&
                paneContext == other.paneContext)

    override fun hashCode(): Int = delegate.hashCode() * 31 + paneContext.hashCode()

    override fun toString(): String = "PaneAwareScene(paneContext=$paneContext, scene=$delegate)"

    internal data class WrappedKey(val sceneClass: KClass<*>, val sceneKey: Any)
}
