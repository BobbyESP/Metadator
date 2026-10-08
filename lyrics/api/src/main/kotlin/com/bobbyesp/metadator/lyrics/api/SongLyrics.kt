/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.lyrics.api

/**
 * The lyrics embedded in a song, ready to be shown while it plays: every line and word knows when
 * it starts and when it ends, whatever format they were written in.
 */
sealed interface SongLyrics {
    data class Synced(val lines: List<TimedLine>) : SongLyrics {
        /** Whether words are timed on their own, and not only whole lines. */
        val isWordByWord: Boolean
            get() = lines.any { it.words.isNotEmpty() }
    }

    data class Plain(val text: String) : SongLyrics

    companion object {
        /**
         * Reads what a lyrics tag holds: TTML, LRC (plain or enhanced) or just text. Null when
         * there is nothing to show.
         */
        fun parse(text: String): SongLyrics? {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) return null
            val lines =
                when {
                    Ttml.looksLikeTtml(trimmed) -> Ttml.parse(trimmed).orEmpty()
                    Lrc.isSynced(trimmed) -> Lrc.parse(trimmed)
                    else -> emptyList()
                }.toTimedLines()
            // Text that could not be read as what it looks like is still better shown than not.
            return if (lines.isEmpty()) Plain(trimmed) else Synced(lines)
        }
    }
}

data class TimedLine(
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val words: List<TimedWord> = emptyList(),
)

/** A word of a [TimedLine]: the characters from [start] until [end] of its text. */
data class TimedWord(val startMs: Long, val endMs: Long, val start: Int, val end: Int)

/**
 * Gives every line and word an end: its own, or else where the next one starts. A line with no text
 * is not shown, but it still ends the one before it, which is how a file marks an instrumental
 * break.
 */
internal fun List<LrcLine>.toTimedLines(): List<TimedLine> {
    val ordered = sortedBy { it.timeMs }
    return ordered.mapIndexedNotNull { index, line ->
        if (line.text.isBlank()) return@mapIndexedNotNull null
        val lineEnd =
            (line.endMs ?: ordered.getOrNull(index + 1)?.timeMs ?: (line.timeMs + LAST_LINE_MS))
                .coerceAtLeast(line.timeMs)
        TimedLine(
            startMs = line.timeMs,
            endMs = lineEnd,
            text = line.text,
            words =
                line.words.mapIndexed { wordIndex, word ->
                    val wordEnd =
                        word.endMs ?: line.words.getOrNull(wordIndex + 1)?.timeMs ?: lineEnd
                    TimedWord(
                        word.timeMs,
                        wordEnd.coerceAtLeast(word.timeMs),
                        word.start,
                        word.end,
                    )
                },
        )
    }
}

/** How long the last line stays, when nothing says where it ends. */
private const val LAST_LINE_MS = 10_000L
