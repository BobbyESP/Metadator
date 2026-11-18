/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.designsystem.blur

import android.os.Build
import android.os.PowerManager
import org.junit.Assert.assertEquals
import org.junit.Test

class BlurLevelTest {

    private fun level(
        sdkInt: Int = Build.VERSION_CODES.VANILLA_ICE_CREAM,
        isLowRamDevice: Boolean = false,
        totalMemoryBytes: Long = 8 * GiB,
        isPowerSaveMode: Boolean = false,
        thermalStatus: Int = PowerManager.THERMAL_STATUS_NONE,
        isWindowBlurEnabled: Boolean = true,
    ) =
        blurLevelOf(
            sdkInt = sdkInt,
            isLowRamDevice = isLowRamDevice,
            totalMemoryBytes = totalMemoryBytes,
            isPowerSaveMode = isPowerSaveMode,
            thermalStatus = thermalStatus,
            isWindowBlurEnabled = isWindowBlurEnabled,
        )

    @Test fun `a capable phone blurs fully`() = assertEquals(BlurLevel.Full, level())

    @Test
    fun `nothing blurs below Android 12`() =
        assertEquals(BlurLevel.Off, level(sdkInt = Build.VERSION_CODES.R))

    @Test
    fun `Android 12 blurs`() = assertEquals(BlurLevel.Full, level(sdkInt = Build.VERSION_CODES.S))

    @Test
    fun `Android Go does not blur`() = assertEquals(BlurLevel.Off, level(isLowRamDevice = true))

    @Test
    fun `battery saver turns blur off`() =
        assertEquals(BlurLevel.Off, level(isPowerSaveMode = true))

    @Test
    fun `a warm phone reduces blur, a hot one turns it off`() {
        assertEquals(BlurLevel.Full, level(thermalStatus = PowerManager.THERMAL_STATUS_LIGHT))
        assertEquals(BlurLevel.Reduced, level(thermalStatus = PowerManager.THERMAL_STATUS_MODERATE))
        assertEquals(BlurLevel.Off, level(thermalStatus = PowerManager.THERMAL_STATUS_SEVERE))
        assertEquals(BlurLevel.Off, level(thermalStatus = PowerManager.THERMAL_STATUS_SHUTDOWN))
    }

    @Test
    fun `where the system turned window blur off, blur is reduced`() =
        assertEquals(BlurLevel.Reduced, level(isWindowBlurEnabled = false))

    @Test
    fun `an entry-level phone reduces blur`() {
        assertEquals(BlurLevel.Reduced, level(totalMemoryBytes = 2 * GiB))
        assertEquals(BlurLevel.Full, level(totalMemoryBytes = 3 * GiB))
    }

    @Test
    fun `unknown memory is not taken for little`() =
        assertEquals(BlurLevel.Full, level(totalMemoryBytes = 0))

    @Test
    fun `the stricter signal wins`() =
        assertEquals(
            BlurLevel.Off,
            level(isWindowBlurEnabled = false, isPowerSaveMode = true, totalMemoryBytes = 2 * GiB),
        )

    @Test
    fun `levels order from least to most blur`() {
        assertEquals(BlurLevel.Off, minOf(BlurLevel.Full, BlurLevel.Off))
        assertEquals(BlurLevel.Reduced, minOf(BlurLevel.Full, BlurLevel.Reduced))
    }

    private companion object {
        const val GiB = 1024L * 1024 * 1024
    }
}
