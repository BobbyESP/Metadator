/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.designsystem.blur

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.LocalHazePerformanceMode
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.LocalHazeBlurStyle
import java.util.function.Consumer

/**
 * How much this device blurs, right now. Every blur in the app reads it, so none decides alone
 * whether it can afford to: the app's theme provides it once, from [rememberBlurLevel], and it
 * changes while the app runs (battery saver, a hot phone, the user's choice).
 *
 * Ordered from least to most blur, so `minOf` of two levels is the stricter one.
 */
enum class BlurLevel {
    /**
     * No blur. A frosted surface draws its container color, nearly opaque; a halo and content out
     * of focus draw nothing, and the element keeps its shadow or its scrim. Below Android 12, on
     * Android Go, in battery saver and on a phone too hot to spend the GPU.
     */
    Off,

    /**
     * Frosted surfaces only, blurred at a quarter of their pixels. No halos, and nothing goes out
     * of focus behind an overlay: those blur a whole screen, or every frame of an animation. On an
     * entry-level phone, a warm one, or where the system itself turned window blur off.
     */
    Reduced,

    /** Every blur, at Haze's balanced resolution. */
    Full,
}

/**
 * The [BlurLevel] in effect. Read it to choose between a blurred and a solid look (a halo or a
 * shadow); provide it to force one (a preview, a screenshot test, a future setting), through
 * [ProvideBlurLevel] so that Haze follows too.
 */
val LocalBlurLevel = compositionLocalOf {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) BlurLevel.Full else BlurLevel.Off
}

/**
 * Provides [level] to [content], and to Haze: at [BlurLevel.Off] its blur is disabled everywhere
 * below, at [BlurLevel.Reduced] it samples at its lowest resolution. A `hazeBlur` written without
 * the modifiers in this package still follows the level.
 */
@Composable
fun ProvideBlurLevel(level: BlurLevel, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalBlurLevel provides level,
        LocalHazePerformanceMode provides level.performanceMode,
        LocalHazeBlurStyle provides level.hazeStyle,
        content = content,
    )
}

private val BlurLevel.performanceMode: HazePerformanceMode
    get() =
        when (this) {
            BlurLevel.Full -> HazePerformanceMode.Balanced
            BlurLevel.Reduced,
            BlurLevel.Off -> HazePerformanceMode.Performance
        }

/** Hoisted: a Haze style compares by identity, and a new one makes every node resolve again. */
private val BlurDisabled = HazeBlurStyle { blurEnabled(false) }

private val BlurLevel.hazeStyle: HazeBlurStyle
    get() = if (this == BlurLevel.Off) BlurDisabled else HazeBlurStyle

/**
 * The level to provide with [ProvideBlurLevel]: what the device can afford
 * ([rememberDeviceBlurLevel]), and never more than [preferred], the user's choice. A user can ask
 * for less blur than the device allows, never for more: a phone in battery saver or too hot still
 * stops blurring.
 */
@Composable
fun rememberBlurLevel(preferred: BlurLevel = BlurLevel.Full): BlurLevel =
    minOf(rememberDeviceBlurLevel(), preferred)

/**
 * The [BlurLevel] this device can afford, kept up to date as battery saver, the phone's temperature
 * and the system's own window blur setting change. Full in a preview.
 */
@Composable
fun rememberDeviceBlurLevel(): BlurLevel {
    if (LocalInspectionMode.current) return BlurLevel.Full
    val context = LocalContext.current
    val signals = remember(context) { DeviceBlurSignals(context) }
    DisposableEffect(signals) {
        signals.start()
        onDispose { signals.stop() }
    }
    // A signal that moves without moving the level (a phone getting a little warm) recomposes
    // nothing.
    val level by remember(signals) { derivedStateOf { signals.level } }
    return level
}

/**
 * What decides the level, as numbers, so the decision can be tested on the JVM.
 *
 * @param thermalStatus One of `PowerManager.THERMAL_STATUS_*`.
 * @param isWindowBlurEnabled `WindowManager.isCrossWindowBlurEnabled`: false where the GPU cannot
 *   afford the compositor's blur, in battery saver, while video tunnels, or when turned off in the
 *   developer options. This blur is drawn by the app, not the compositor, but the reasons are the
 *   same.
 */
internal fun blurLevelOf(
    sdkInt: Int,
    isLowRamDevice: Boolean,
    totalMemoryBytes: Long,
    isPowerSaveMode: Boolean,
    thermalStatus: Int,
    isWindowBlurEnabled: Boolean,
): BlurLevel =
    when {
        // Haze would fall back to a scrim here anyway; RenderScript blurs a frame late, which a
        // scrolling list shows.
        sdkInt < Build.VERSION_CODES.S -> BlurLevel.Off
        isLowRamDevice -> BlurLevel.Off
        isPowerSaveMode -> BlurLevel.Off
        thermalStatus >= PowerManager.THERMAL_STATUS_SEVERE -> BlurLevel.Off
        thermalStatus >= PowerManager.THERMAL_STATUS_MODERATE -> BlurLevel.Reduced
        !isWindowBlurEnabled -> BlurLevel.Reduced
        totalMemoryBytes in 1 until EntryLevelMemoryBytes -> BlurLevel.Reduced
        else -> BlurLevel.Full
    }

/** Under this, a phone is entry-level: its GPU is too, and blur goes [BlurLevel.Reduced]. */
private const val EntryLevelMemoryBytes = 3L * 1024 * 1024 * 1024

/**
 * The device's signals, each a snapshot state, so a change recomposes whoever read [level] and
 * nothing else. Listens only between [start] and [stop].
 */
@Stable
private class DeviceBlurSignals(private val context: Context) {
    private val powerManager = context.getSystemService(PowerManager::class.java)
    private val activityManager = context.getSystemService(ActivityManager::class.java)
    private val windowManager = context.getSystemService(WindowManager::class.java)

    private val isLowRamDevice = activityManager?.isLowRamDevice ?: false
    private val totalMemoryBytes =
        activityManager?.let { manager ->
            ActivityManager.MemoryInfo().also(manager::getMemoryInfo).totalMem
        } ?: 0L

    private var isPowerSaveMode by mutableStateOf(powerManager?.isPowerSaveMode ?: false)
    private var thermalStatus by mutableIntStateOf(PowerManager.THERMAL_STATUS_NONE)
    private var isWindowBlurEnabled by mutableStateOf(true)

    val level: BlurLevel
        get() =
            blurLevelOf(
                sdkInt = Build.VERSION.SDK_INT,
                isLowRamDevice = isLowRamDevice,
                totalMemoryBytes = totalMemoryBytes,
                isPowerSaveMode = isPowerSaveMode,
                thermalStatus = thermalStatus,
                isWindowBlurEnabled = isWindowBlurEnabled,
            )

    private val powerSaveReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                isPowerSaveMode = powerManager?.isPowerSaveMode ?: false
            }
        }

    private val thermalListener = PowerManager.OnThermalStatusChangedListener { status ->
        thermalStatus = status
    }

    // Called at once with the current value, then on every change.
    private val windowBlurListener = Consumer<Boolean> { enabled -> isWindowBlurEnabled = enabled }

    /** Below Android 12, or on Android Go, the level is Off whatever they say: none is heard. */
    fun start() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || isLowRamDevice) return
        isPowerSaveMode = powerManager?.isPowerSaveMode ?: false
        val filter = IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        // A system broadcast, which reaches a receiver that is not exported.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(powerSaveReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(powerSaveReceiver, filter)
        }
        powerManager?.let {
            thermalStatus = it.currentThermalStatus
            it.addThermalStatusListener(context.mainExecutor, thermalListener)
        }
        windowManager?.addCrossWindowBlurEnabledListener(context.mainExecutor, windowBlurListener)
    }

    fun stop() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || isLowRamDevice) return
        context.unregisterReceiver(powerSaveReceiver)
        powerManager?.removeThermalStatusListener(thermalListener)
        windowManager?.removeCrossWindowBlurEnabledListener(windowBlurListener)
    }
}
