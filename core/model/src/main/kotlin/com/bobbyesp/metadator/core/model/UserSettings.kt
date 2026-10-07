/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.model

/** Every user preference, read as one value so a screen never sees half an update. */
data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.FollowSystem,
    val useDynamicColor: Boolean = true,
    val seedColor: Int = DEFAULT_SEED_COLOR,
    val paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    val pureBlack: Boolean = false,
    val colorEditorFromArtwork: Boolean = true,
    val multiValueMode: MultiValueMode = MultiValueMode.Separate,
    val multiValueSeparator: String = "; ",
    val warnSmallArtwork: Boolean = true,
    val enabledProviders: Set<String> = setOf(PROVIDER_MUSICBRAINZ, PROVIDER_DEEZER),
    val successfulSaves: Int = 0,
) {
    companion object {
        /** The 1.x seed, so a user who never touched it keeps the same colors. */
        const val DEFAULT_SEED_COLOR: Int = 0xFF415F76.toInt()

        const val PROVIDER_MUSICBRAINZ = "musicbrainz"
        const val PROVIDER_DEEZER = "deezer"
    }
}

enum class ThemeMode {
    FollowSystem,
    Light,
    Dark,
}

enum class PaletteStyle {
    TonalSpot,
    Neutral,
    Vibrant,
    Expressive,
    Rainbow,
    FruitSalad,
    Monochrome,
    Fidelity,
    Content,
}

/**
 * How fields that hold several values (artists, genres…) are written.
 *
 * Players disagree: most read every value, some only the first. [Separate] keeps the values apart,
 * as the formats intend; [Joined] writes one value with a separator, for players that would
 * otherwise show only the first artist.
 */
enum class MultiValueMode {
    Separate,
    Joined,
}
