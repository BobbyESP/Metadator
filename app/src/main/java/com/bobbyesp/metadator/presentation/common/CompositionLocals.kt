package com.bobbyesp.metadator.presentation.common

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.bobbyesp.mediaplayer.service.ConnectionHandler
import com.bobbyesp.utilities.preferences.Preferences.AppMainSettingsStateFlow
import com.bobbyesp.utilities.theme.DarkThemePreference
import com.bobbyesp.utilities.ui.DEFAULT_SEED_COLOR
import com.materialkolor.DynamicMaterialThemeState
import com.materialkolor.rememberDynamicMaterialThemeState
import com.skydoves.landscapist.coil.LocalCoilImageLoader
import kotlinx.coroutines.Dispatchers

val LocalDarkTheme = compositionLocalOf { DarkThemePreference() }
val LocalSeedColor = compositionLocalOf { DEFAULT_SEED_COLOR }
val LocalDynamicColoringSwitch = compositionLocalOf { false }
val LocalDynamicThemeState =
    compositionLocalOf<DynamicMaterialThemeState> { error("No theme state provided") }
val LocalOrientation = compositionLocalOf<Int> { error("No orientation provided") }
val LocalNavController =
    compositionLocalOf<NavHostController> { error("No nav controller provided") }
val LocalWindowWidthState =
    staticCompositionLocalOf { WindowWidthSizeClass.Compact } //This value probably will never change, that's why it is static
val LocalSnackbarHostState =
    compositionLocalOf<SnackbarHostState> { error("No snackbar host state provided") }
val LocalDrawerState =
    compositionLocalOf<DrawerState> { error("No Drawer State has been provided") }
val LocalMediaplayerConnection =
    compositionLocalOf<ConnectionHandler> { error("No Media Player Service Connection handler has been provided") }
val LocalPlayerAwareWindowInsets =
    compositionLocalOf<WindowInsets> { error("No WindowInsets provided") }

@Composable
fun AppLocalSettingsProvider(
    windowWidthSize: WindowWidthSizeClass,
    playerConnectionHandler: ConnectionHandler,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val appSettingsState = AppMainSettingsStateFlow.collectAsStateWithLifecycle().value
    val navController = rememberNavController()

    val imageLoader = ImageLoader.Builder(context)
        .memoryCache {
            MemoryCache.Builder(context)
                .maxSizePercent(0.35)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache"))
                .maxSizeBytes(7 * 1024 * 1024)
                .build()
        }
        .respectCacheHeaders(false)
        .allowHardware(true)
        .crossfade(true)
        .bitmapFactoryMaxParallelism(12)
        .dispatcher(Dispatchers.IO)
        .build()

    val config = LocalConfiguration.current
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    appSettingsState.run {
        val themeState = rememberDynamicMaterialThemeState(
            seedColor = Color(seedColor),
            isDark = darkTheme.isDarkTheme(),
            isAmoled = darkTheme.isHighContrastModeEnabled
        )

        CompositionLocalProvider(
            LocalDarkTheme provides darkTheme, //Tells the app if it should use dark theme or not
            LocalSeedColor provides seedColor, //Tells the app what color to use as seed for the palette
            LocalDynamicColoringSwitch provides useDynamicColoring, //Tells the app if it should use dynamic colors or not (Android 12+ feature)
            LocalDynamicThemeState provides themeState, //Provides the theme state to the app
            LocalNavController provides navController,
            LocalWindowWidthState provides windowWidthSize,
            LocalOrientation provides config.orientation,
            LocalSnackbarHostState provides snackbarHostState,
            LocalCoilImageLoader provides imageLoader,
            LocalDrawerState provides drawerState,
            LocalMediaplayerConnection provides playerConnectionHandler,
        ) {
            content() //The content of the app
        }
    }
}