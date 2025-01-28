package com.bobbyesp.metadator.core.data.local.preferences

import com.bobbyesp.metadator.core.data.local.DarkThemePreference
import com.bobbyesp.metadator.domain.enums.LayoutType
import com.bobbyesp.metadator.presentation.components.cards.songs.compact.CompactCardSize
import com.bobbyesp.utilities.ui.DEFAULT_SEED_COLOR
import com.materialkolor.PaletteStyle

data class UserPreferences(
    val desiredLayout: LayoutType,
    val reduceShadows: Boolean,
    val marqueeTextEnabled: Boolean,
    val songCardSize: CompactCardSize,
    val darkThemePreference: DarkThemePreference,
    val useDynamicColoring: Boolean,
    val themeColor: Int,
    val paletteStyle: PaletteStyle
) {
    companion object {
        fun emptyUserPreferences(): UserPreferences =
            UserPreferences(
                desiredLayout = LayoutType.Grid,
                reduceShadows = false,
                marqueeTextEnabled = true,
                songCardSize = CompactCardSize.LARGE,
                darkThemePreference = DarkThemePreference(),
                useDynamicColoring = true,
                themeColor = DEFAULT_SEED_COLOR,
                paletteStyle = PaletteStyle.Vibrant
            )
    }
}
