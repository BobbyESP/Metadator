package com.bobbyesp.metadator.core.navigation.overlay

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import androidx.window.core.layout.WindowSizeClass

/** How an overlay destination is being presented. */
enum class OverlayPresentation {
    /** A modal bottom sheet, reachable with a thumb. */
    Sheet,

    /** A centred dialog, which is what a wide window has the room for. */
    Dialog,
}

/** What a destination asks for, which is not always what the window can give it. */
enum class OverlayPreference {
    /** A sheet whatever the window, for content that is a list of choices. */
    AlwaysSheet,

    /** A sheet where the window is narrow, a dialog where it is wide. */
    SheetOrDialog,
}

/**
 * What an overlay destination is allowed to know about the container it landed in — told, not
 * measured. Reading the window for itself gets a different answer (the device's orientation, say)
 * that stops agreeing the moment the container is not the whole window.
 */
@Immutable
class OverlayContext
internal constructor(
    /** Which container the destination got, so its body can suit it. */
    val presentation: OverlayPresentation,

    /**
     * Whether the container is tall enough to stack content vertically. What matters is the height
     * available, not which way the device is held — the two only agree on a phone.
     */
    val hasRoomToStack: Boolean,
) {

    override fun equals(other: Any?): Boolean =
        this === other ||
            (other is OverlayContext &&
                presentation == other.presentation &&
                hasRoomToStack == other.hasRoomToStack)

    override fun hashCode(): Int = presentation.hashCode() * 31 + hasRoomToStack.hashCode()

    override fun toString(): String =
        "OverlayContext(presentation=$presentation, hasRoomToStack=$hasRoomToStack)"
}

/** Defaults to what a composable rendered outside any overlay scene has: a full, tall window. */
val LocalOverlayContext = staticCompositionLocalOf {
    OverlayContext(OverlayPresentation.Dialog, hasRoomToStack = true)
}

/**
 * Renders destinations marked with [OverlaySceneStrategy.overlay] above whatever else is on the
 * back stack, as a sheet or a dialog depending on the window.
 *
 * Which container a destination gets follows from the window and from the destination, and from
 * nothing about whoever navigated to it. Must be listed before the layout strategies — the first to
 * claim the topmost entry wins.
 */
class OverlaySceneStrategy<T : Any>(
    private val windowIsWide: Boolean,
    private val windowIsShort: Boolean,
) : SceneStrategy<T> {

    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val last = entries.lastOrNull() ?: return null
        val preference = last.metadata[OverlayKey] ?: return null

        // An overlay has to sit above something. A back stack whose only entry is an overlay has
        // nothing to overlay, so it is left to the layout strategies to render as an ordinary
        // destination — `overlaidEntries` may not be empty.
        val overlaid = entries.dropLast(1)
        if (overlaid.isEmpty()) return null

        val presentation =
            when (preference) {
                OverlayPreference.AlwaysSheet -> OverlayPresentation.Sheet
                OverlayPreference.SheetOrDialog ->
                    if (windowIsWide) OverlayPresentation.Dialog else OverlayPresentation.Sheet
            }

        val context = OverlayContext(presentation, hasRoomToStack = !windowIsShort)

        return when (presentation) {
            OverlayPresentation.Sheet -> SheetScene(last, overlaid, context, onBack)
            OverlayPresentation.Dialog -> DialogScene(last, overlaid, context)
        }
    }

    companion object {

        /** Marks an entry as an overlay, and says which container it would like. */
        fun overlay(
            preference: OverlayPreference = OverlayPreference.SheetOrDialog
        ): Map<String, Any> = metadata { put(OverlayKey, preference) }

        /**
         * Whether [top], the last of [stackSize] entries, is shown as an overlay: the answer
         * [calculateScene] gives, for whoever draws what is behind it.
         */
        fun isShownAsOverlay(top: NavEntry<*>, stackSize: Int): Boolean =
            stackSize > 1 && top.metadata[OverlayKey] != null

        internal object OverlayKey : NavMetadataKey<OverlayPreference>
    }
}

/**
 * Two facts, not one written twice — and neither is the list-detail breakpoint. Room for one
 * centred dialog ([WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND]) is a different question from room
 * for a list beside a detail, which `calculatePaneScaffoldDirective` only grants at expanded.
 * Between them an overlay is a dialog over a single pane, deliberately.
 */
private object OverlayBreakpoints {

    /** Wide enough that a centred dialog reads better than a sheet reached with a thumb. */
    const val DIALOG_WIDTH_DP: Int = WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND

    /** Tall enough that an overlay can put a header above its content and have both fit. */
    const val STACKING_HEIGHT_DP: Int = WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun <T : Any> rememberOverlaySceneStrategy(): OverlaySceneStrategy<T> {
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass

    val windowIsWide = windowSizeClass.isWidthAtLeastBreakpoint(OverlayBreakpoints.DIALOG_WIDTH_DP)
    val windowIsShort =
        !windowSizeClass.isHeightAtLeastBreakpoint(OverlayBreakpoints.STACKING_HEIGHT_DP)

    return remember(windowIsWide, windowIsShort) {
        OverlaySceneStrategy(windowIsWide, windowIsShort)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private data class SheetScene<T : Any>(
    val entry: NavEntry<T>,
    override val overlaidEntries: List<NavEntry<T>>,
    val overlayContext: OverlayContext,
    val onDismiss: () -> Unit,
) : OverlayScene<T> {

    override val key: Any = OverlaySceneKey(entry.contentKey, overlayContext)

    override val entries: List<NavEntry<T>> = listOf(entry)

    override val previousEntries: List<NavEntry<T>> = overlaidEntries

    /**
     * Held so the sheet can be slid away before it leaves composition — see [onRemove]. Assigned
     * from the composition because a `SheetState` must be remembered and a scene is built outside
     * one; `lateinit` is what the platform's own `AnimatedBottomSheetSample` does here.
     */
    private lateinit var sheetState: SheetState

    override val content: @Composable () -> Unit = {
        val state =
            rememberBottomSheetState(
                initialValue = SheetValue.Hidden,
                enabledValues = SheetHeights,
            )
        sheetState = state

        ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state) {
            CompositionLocalProvider(LocalOverlayContext provides overlayContext) {
                entry.Content()
            }
        }
    }

    /**
     * Slides the sheet away before it leaves composition; `NavDisplay` keeps a popped overlay
     * composed until this returns. Without it only a drag animated, because that is
     * `ModalBottomSheet` animating itself — a button or system back took it off screen in one
     * frame. Guarded because a scene can be popped before it ever composed.
     */
    override suspend fun onRemove() {
        if (::sheetState.isInitialized) sheetState.hide()
    }

    private companion object {

        /**
         * No half-open state: the sheet opens at the height of its content and back closes it.
         * `ModalBottomSheet` otherwise treats back as "collapse, then dismiss", costing two presses
         * to leave one destination — and the first hides content that did not fit collapsed anyway.
         * An overlay is one entry, so back has one job.
         */
        val SheetHeights = setOf(SheetValue.Hidden, SheetValue.Expanded)
    }
}

/**
 * A destination shown as a dialog renders its own `AlertDialog`, which is already a window of its
 * own, so nothing is wrapped around it here: one dialog inside another would draw two scrims.
 */
private data class DialogScene<T : Any>(
    val entry: NavEntry<T>,
    override val overlaidEntries: List<NavEntry<T>>,
    val overlayContext: OverlayContext,
) : OverlayScene<T> {

    override val key: Any = OverlaySceneKey(entry.contentKey, overlayContext)

    override val entries: List<NavEntry<T>> = listOf(entry)

    override val previousEntries: List<NavEntry<T>> = overlaidEntries

    override val content: @Composable () -> Unit = {
        CompositionLocalProvider(LocalOverlayContext provides overlayContext) { entry.Content() }
    }
}

/**
 * Keeps one destination's containers from being mistaken for each other — the whole context, not
 * just the container: `NavDisplay` keeps the first overlay scene it sees for a key, so a window
 * that changed shape mid-overlay would go on rendering what it was told before.
 */
private data class OverlaySceneKey(val contentKey: Any, val overlayContext: OverlayContext)
