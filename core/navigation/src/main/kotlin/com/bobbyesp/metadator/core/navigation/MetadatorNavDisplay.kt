package com.bobbyesp.metadator.core.navigation

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.bobbyesp.metadator.core.navigation.motion.LocalNavSharedTransitionScope
import com.bobbyesp.metadator.core.navigation.motion.NavigationMotion

/**
 * How every host renders its back stack: the main activity and the external editor. They differ
 * in their destinations and strategies, never in how entries keep state or move.
 *
 * Saveable state goes before the ViewModel store: the library needs that order for a
 * `SavedStateHandle` to survive process death.
 */
@Composable
fun MetadatorNavDisplay(
    backStack: NavBackStack<NavKey>,
    navigator: Navigator,
    sceneStrategies: List<SceneStrategy<NavKey>>,
    entryProvider: (NavKey) -> NavEntry<NavKey>,
    modifier: Modifier = Modifier,
) {
    SharedTransitionLayout(modifier = modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalNavSharedTransitionScope provides this) {
            NavDisplay(
                backStack = backStack,
                modifier = Modifier.fillMaxSize(),
                onBack = navigator::goBack,
                entryDecorators =
                    listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                sceneStrategies = sceneStrategies,
                entryProvider = entryProvider,
                transitionSpec = { NavigationMotion.forward() },
                popTransitionSpec = { NavigationMotion.backward() },
                predictivePopTransitionSpec = { NavigationMotion.predictiveBack() },
            )
        }
    }
}
