/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.designsystem.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.bobbyesp.metadator.core.model.PaletteStyle
import com.bobbyesp.metadator.core.model.ThemeMode
import com.bobbyesp.metadator.core.model.UserSettings
import com.materialkolor.PaletteStyle as KolorPaletteStyle
import com.materialkolor.blend.Blend
import com.materialkolor.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Whether the theme is dark, as the screens that draw their own surfaces need to know. */
val LocalDarkTheme = staticCompositionLocalOf { false }

@Composable
fun ThemeMode.isDark(): Boolean =
    when (this) {
        ThemeMode.FollowSystem -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

fun isDynamicColorSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * The app's theme: Material 3 Expressive, with the expressive motion scheme, colored from the
 * wallpaper or from the user's seed color.
 *
 * Color schemes are built only when their inputs change, off the main thread, and cached: every new
 * scheme recomposes everything under the theme. The first one is built in composition, because the
 * first frame needs it.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MetadatorTheme(settings: UserSettings = UserSettings(), content: @Composable () -> Unit) {
    val isDark = settings.themeMode.isDark()
    val cache = remember { ColorSchemeCache() }
    val built =
        rememberColorScheme(
            source =
                ColorSchemeSource(
                    isDark = isDark,
                    useWallpaperColors = settings.useDynamicColor,
                    seedColor = settings.seedColor,
                    paletteStyle = settings.paletteStyle,
                    pureBlack = settings.pureBlack,
                ),
            cache = cache,
        )
    // One instance for the app's lifetime: Material keys running shape morphs by their spec, and a
    // new scheme per recomposition made buttons jump to their pressed shape.
    val motionScheme = remember { MotionScheme.expressive() }

    CompositionLocalProvider(
        LocalDarkTheme provides isDark,
        LocalThemeColors provides ThemeColors(built, cache),
    ) {
        MaterialExpressiveTheme(
            colorScheme = built.scheme,
            motionScheme = motionScheme,
            typography = MetadatorTypography,
            content = content,
        )
    }
}

/**
 * A theme seeded from [accent] (a cover's color) for one destination, harmonized with the app's.
 * Only what is inside recomposes; the app's theme is untouched.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MetadatorAccentTheme(accent: Color?, content: @Composable () -> Unit) {
    val appScheme = MaterialTheme.colorScheme
    val themeColors = LocalThemeColors.current
    val scheme =
        if (accent != null && themeColors != null) {
            val primary = themeColors.built.scheme.primary
            val seed =
                remember(accent, primary) { Blend.harmonize(accent.toArgb(), primary.toArgb()) }
            rememberColorScheme(
                    source =
                        themeColors.built.source.copy(useWallpaperColors = false, seedColor = seed),
                    cache = themeColors.cache,
                    placeholder = appScheme,
                )
                .scheme
        } else appScheme

    MaterialExpressiveTheme(
        colorScheme = scheme,
        motionScheme = MaterialTheme.motionScheme,
        shapes = MaterialTheme.shapes,
        typography = MaterialTheme.typography,
        content = content,
    )
}

@Immutable
internal data class ColorSchemeSource(
    val isDark: Boolean,
    val useWallpaperColors: Boolean,
    val seedColor: Int,
    val paletteStyle: PaletteStyle,
    val pureBlack: Boolean,
)

@Immutable internal class BuiltColorScheme(val source: ColorSchemeSource, val scheme: ColorScheme)

@Immutable internal class ThemeColors(val built: BuiltColorScheme, val cache: ColorSchemeCache)

private val LocalThemeColors = staticCompositionLocalOf<ThemeColors?> { null }

private fun ColorSchemeSource.build(context: Context): ColorScheme =
    if (useWallpaperColors && isDynamicColorSupported()) {
        if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        dynamicColorScheme(
            seedColor = Color(seedColor),
            isDark = isDark,
            isAmoled = pureBlack,
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
            style = paletteStyle.toKolor(),
        )
    }

private fun PaletteStyle.toKolor(): KolorPaletteStyle =
    when (this) {
        PaletteStyle.TonalSpot -> KolorPaletteStyle.TonalSpot
        PaletteStyle.Neutral -> KolorPaletteStyle.Neutral
        PaletteStyle.Vibrant -> KolorPaletteStyle.Vibrant
        PaletteStyle.Expressive -> KolorPaletteStyle.Expressive
        PaletteStyle.Rainbow -> KolorPaletteStyle.Rainbow
        PaletteStyle.FruitSalad -> KolorPaletteStyle.FruitSalad
        PaletteStyle.Monochrome -> KolorPaletteStyle.Monochrome
        PaletteStyle.Fidelity -> KolorPaletteStyle.Fidelity
        PaletteStyle.Content -> KolorPaletteStyle.Content
    }

@Composable
private fun rememberColorScheme(
    source: ColorSchemeSource,
    cache: ColorSchemeCache,
    placeholder: ColorScheme? = null,
): BuiltColorScheme {
    val context = LocalContext.current
    var built by
        remember(cache) {
            val scheme =
                cache[source] ?: placeholder ?: source.build(context).also { cache[source] = it }
            mutableStateOf(BuiltColorScheme(source, scheme))
        }
    LaunchedEffect(cache, source) {
        val scheme =
            cache[source]
                ?: withContext(Dispatchers.Default) { source.build(context) }
                    .also { cache[source] = it }
        if (built.source != source || built.scheme !== scheme)
            built = BuiltColorScheme(source, scheme)
    }
    return built
}

internal class ColorSchemeCache :
    LinkedHashMap<ColorSchemeSource, ColorScheme>(CACHED_SCHEMES, 0.75f, true) {
    override fun removeEldestEntry(
        eldest: MutableMap.MutableEntry<ColorSchemeSource, ColorScheme>
    ) = size > CACHED_SCHEMES
}

private const val CACHED_SCHEMES = 8
