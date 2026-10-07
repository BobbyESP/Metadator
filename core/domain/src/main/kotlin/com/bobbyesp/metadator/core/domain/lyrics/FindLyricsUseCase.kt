/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.domain.lyrics

import com.bobbyesp.metadator.lyrics.api.LyricsProvider
import com.bobbyesp.metadator.lyrics.api.LyricsQuery
import com.bobbyesp.metadator.lyrics.api.LyricsResult

class FindLyricsUseCase(private val provider: LyricsProvider) {
    val providerName: String
        get() = provider.displayName

    suspend operator fun invoke(query: LyricsQuery): LyricsResult {
        if (query.title.isBlank() || query.artist.isBlank()) return LyricsResult.NotFound
        return provider.find(query)
    }
}
