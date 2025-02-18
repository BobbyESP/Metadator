package com.bobbyesp.metadator.core.presentation.common

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil.ImageLoader
import com.bobbyesp.mediaplayer.service.ConnectionHandler
import com.bobbyesp.metadator.core.data.local.DarkThemePreference
import com.bobbyesp.metadator.core.data.local.preferences.AppPreferences
import com.bobbyesp.metadator.core.data.local.preferences.UserPreferences.Companion.emptyUserPreferences
import com.bobbyesp.utilities.ui.DEFAULT_SEED_COLOR
import com.dokar.sonner.ToasterState
import com.dokar.sonner.rememberToasterState
import com.materialkolor.DynamicMaterialThemeState
import com.materialkolor.rememberDynamicMaterialThemeState
import com.skydoves.landscapist.coil.LocalCoilImageLoader

val LocalDarkTheme =
    compositionLocalOf<DarkThemePreference> { error("No Dark Theme preferences provided") }
val LocalSeedColor = compositionLocalOf { DEFAULT_SEED_COLOR }
val LocalDynamicColoringSwitch = compositionLocalOf { false }
val LocalDynamicThemeState =
    compositionLocalOf<DynamicMaterialThemeState> { error("No theme state provided") }
val LocalOrientation = compositionLocalOf<Int> { error("No orientation provided") }

val LocalAppPreferencesController =
    staticCompositionLocalOf<AppPreferences> { error("No settings controller provided") }

val LocalNavController =
    compositionLocalOf<NavHostController> { error("No nav controller provided") }

val LocalWindowWidthState =
    staticCompositionLocalOf { WindowWidthSizeClass.Compact } //This value probably will never change, that's why it is static

val LocalSonner = compositionLocalOf<ToasterState> { error("No sonner toaster state provided") }
val LocalMediaplayerConnection =
    compositionLocalOf<ConnectionHandler> { error("No Media Player Service Connection handler has been provided") }

@Composable
fun AppLocalSettingsProvider(
    windowWidthSize: WindowWidthSizeClass,
    playerConnectionHandler: ConnectionHandler,
    sonner: ToasterState = rememberToasterState(),
    appPreferences: AppPreferences,
    imageLoader: ImageLoader,
    content: @Composable () -> Unit
) {
    val settingsFlow =
        appPreferences.userPreferencesFlow.collectAsStateWithLifecycle(initialValue = emptyUserPreferences())

    val seedColor = settingsFlow.value.themeColor
    val darkTheme = settingsFlow.value.darkThemePreference
    val themeStyle = settingsFlow.value.paletteStyle

    val config = LocalConfiguration.current

    val themeState = rememberDynamicMaterialThemeState(
        seedColor = Color(seedColor),
        isDark = darkTheme.isDarkTheme(),
        style = themeStyle,
        isAmoled = darkTheme.isHighContrastModeEnabled
    )

    CompositionLocalProvider(
        LocalDarkTheme provides darkTheme, //Tells the app what dark theme to use
        //TODO: Modify to handle multiple colors (like based on images)
        LocalSeedColor provides seedColor, //Tells the app what color to use as seed for the palette
        LocalDynamicColoringSwitch provides settingsFlow.value.useDynamicColoring, //Tells the app if it should use dynamic colors or not (Android 12+ feature)
        LocalDynamicThemeState provides themeState, //Provides the theme state to the app
        LocalAppPreferencesController provides appPreferences,
        LocalWindowWidthState provides windowWidthSize,
        LocalOrientation provides config.orientation,
        LocalSonner provides sonner,
        LocalCoilImageLoader provides imageLoader,
        LocalMediaplayerConnection provides playerConnectionHandler,
    ) {
        content() //The content of the app
    }
}