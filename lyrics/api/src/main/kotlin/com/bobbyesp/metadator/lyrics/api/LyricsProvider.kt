package com.bobbyesp.metadator.lyrics.api

/** Finds the lyrics of a song online. */
interface LyricsProvider {
    val displayName: String

    suspend fun find(query: LyricsQuery): LyricsResult
}

data class LyricsQuery(
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationMs: Long? = null,
)

sealed interface LyricsResult {
    data class Found(val lyrics: Lyrics) : LyricsResult

    data object NotFound : LyricsResult

    data object Offline : LyricsResult

    data class Failed(val message: String) : LyricsResult
}

/** Plain and synced (LRC) lyrics; a provider may have either or both. */
data class Lyrics(val plain: String?, val synced: String?, val instrumental: Boolean = false) {
    /** What to embed: synced when there is a version, since players that sync show it better. */
    val preferred: String?
        get() = synced?.takeIf { it.isNotBlank() } ?: plain?.takeIf { it.isNotBlank() }
}
