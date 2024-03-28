package com.bobbyesp.metadator.presentation.common

import android.os.Build
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.rememberDrawerState
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
import com.bobbyesp.metadator.App.Companion.context
import com.bobbyesp.utilities.DarkThemePreference
import com.bobbyesp.utilities.Theme.paletteStyles
import com.bobbyesp.utilities.preferences.Preferences.AppSettingsStateFlow
import com.bobbyesp.utilities.ui.DEFAULT_SEED_COLOR
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.rememberBottomSheetNavigator
import com.kyant.monet.LocalTonalPalettes
import com.kyant.monet.PaletteStyle
import com.kyant.monet.TonalPalettes.Companion.toTonalPalettes
import com.skydoves.landscapist.coil.LocalCoilImageLoader

val LocalDarkTheme = compositionLocalOf { DarkThemePreference() }
val LocalSeedColor = compositionLocalOf { DEFAULT_SEED_COLOR }
val LocalDynamicColorSwitch = compositionLocalOf { false }
val LocalIndexOfPaletteStyle = compositionLocalOf { 0 }
val LocalOrientation = compositionLocalOf<Int> { error("No orientation provided") }
val LocalNavController =
    compositionLocalOf<NavHostController> { error("No nav controller provided") }
val LocalWindowWidthState =
    staticCompositionLocalOf { WindowWidthSizeClass.Compact } //This value probably will never change, that's why it is static
val LocalSnackbarHostState = compositionLocalOf<SnackbarHostState> { error("No snackbar host state provided") }
val LocalDrawerState =
    compositionLocalOf<DrawerState> { error("No Drawer State has been provided") }

@OptIn(ExperimentalMaterialNavigationApi::class)
@Composable
fun AppLocalSettingsProvider(
    windowWidthSize: WindowWidthSizeClass,
    content: @Composable () -> Unit
) {
    val appSettingsState = AppSettingsStateFlow.collectAsStateWithLifecycle().value
    val bottomSheetNavigator = rememberBottomSheetNavigator()
    val navController = rememberNavController(bottomSheetNavigator)
    val imageLoader = ImageLoader.Builder(context).build()
    val config = LocalConfiguration.current
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    appSettingsState.run {
        CompositionLocalProvider(
            LocalDarkTheme provides darkTheme, //Tells the app if it should use dark theme or not
            LocalSeedColor provides seedColor, //Tells the app what color to use as seed for the palette
            LocalDynamicColorSwitch provides isDynamicColorEnabled, //Tells the app if it should use dynamic colors or not (Android 12+ feature)
            LocalIndexOfPaletteStyle provides paletteStyleIndex, //Tells the app what palette style to use depending on the index
            LocalNavController provides navController,
            LocalWindowWidthState provides windowWidthSize,
            LocalTonalPalettes provides if (isDynamicColorEnabled && Build.VERSION.SDK_INT >= 31) dynamicDarkColorScheme(
                LocalContext.current
            ).toTonalPalettes()
            else Color(seedColor).toTonalPalettes(
                paletteStyles.getOrElse(paletteStyleIndex) { PaletteStyle.TonalSpot }
            ), // Tells the app what is the current palette to use
            LocalOrientation provides config.orientation,
            LocalSnackbarHostState provides snackbarHostState,
            LocalCoilImageLoader provides imageLoader,
            LocalDrawerState provides drawerState
        ) {
            content() //The content of the app
        }
    }
}