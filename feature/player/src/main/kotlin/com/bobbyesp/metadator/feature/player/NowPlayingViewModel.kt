/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.feature.player

import com.bobbyesp.metadator.core.domain.lyrics.LoadLyricsUseCase
import com.bobbyesp.metadator.core.domain.lyrics.TrackLyrics
import com.bobbyesp.metadator.core.model.ContentRef
import com.bobbyesp.metadator.core.model.Track
import com.bobbyesp.metadator.core.ui.artwork.ArtworkAccentSource
import com.bobbyesp.metadator.core.ui.viewmodel.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val playerModule = module { viewModelOf(::NowPlayingViewModel) }

/**
 * What the player shows of a song beyond what the library knows: read from its file.
 *
 * @param ref the song this is about
 * @param lyrics null while they are being read
 * @param accent the cover's color, as an ARGB int; null while there is none
 */
internal data class NowPlayingState(
    val ref: ContentRef? = null,
    val lyrics: TrackLyrics? = null,
    val accent: Int? = null,
)

internal sealed interface NowPlayingIntent {
    /** [track] is the one playing, or the player was opened on it again. */
    data class Show(val track: Track) : NowPlayingIntent
}

internal class NowPlayingViewModel(
    private val loadLyrics: LoadLyricsUseCase,
    private val accents: ArtworkAccentSource,
) : BaseViewModel<NowPlayingIntent, NowPlayingState, Nothing>(NowPlayingState()) {

    private var loading: Job? = null

    override fun handleIntent(intent: NowPlayingIntent) {
        when (intent) {
            is NowPlayingIntent.Show -> show(intent.track)
        }
    }

    private fun show(track: Track) {
        loading?.cancel()
        // The same song again is read again, since its tags may have been edited meanwhile, but
        // keeps what it shows until then. The color stays until the next one is known either
        // way: going through the app's own between two covers would flash.
        if (currentState.ref != track.ref) setState { copy(ref = track.ref, lyrics = null) }
        loading =
            launch(onError = { setState { copy(lyrics = TrackLyrics.Unreadable) } }) {
                this.launch {
                    val color = track.artworkRef?.let { accents.fromUri(it.uri) }
                    setState { copy(accent = color) }
                }
                val lyrics = loadLyrics(track.ref)
                setState { copy(lyrics = lyrics) }
            }
    }
}
