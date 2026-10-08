/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.lyrics.api

/**
 * One line of synced lyrics.
 *
 * @param words the words that are timed on their own, when the file has them; empty otherwise
 * @param endMs when the line ends, when the file says so; otherwise it lasts until the next one
 */
data class LrcLine(
    val timeMs: Long,
    val text: String,
    val words: List<LrcWord> = emptyList(),
    val endMs: Long? = null,
)

/**
 * A word timed on its own: the characters from [start] until [end] of its line's text.
 *
 * @param endMs when it ends, when the file says so; otherwise it lasts until the next word
 */
data class LrcWord(val timeMs: Long, val endMs: Long?, val start: Int, val end: Int)

/**
 * Reads and writes the LRC format: `[mm:ss.xx] text`, metadata tags ignored. Also its enhanced
 * form, where marks inside the line time each word: `[00:12.00]<00:12.00>Hello <00:12.50>world`.
 */
object Lrc {
    private val TIMESTAMP = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?\]""")
    private val WORD_MARK = Regex("""<(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?>""")

    /** Whether [text] has at least one timed line, i.e. is synced. */
    fun isSynced(text: String): Boolean = text.lineSequence().any { TIMESTAMP.containsMatchIn(it) }

    /** Timed lines in order. A line with several timestamps repeats at each of them. */
    fun parse(text: String): List<LrcLine> =
        text
            .lineSequence()
            .flatMap { line ->
                val stamps = TIMESTAMP.findAll(line).toList()
                if (stamps.isEmpty()) return@flatMap emptySequence()
                val lyric = readWords(line.substring(stamps.last().range.last + 1))
                // The word marks are times in the song, so they can only belong to one of the
                // places a repeated line is sung at: a repeated line keeps its text alone.
                val shared = if (stamps.size > 1) LrcLine(0, lyric.text) else lyric
                stamps.asSequence().map { shared.copy(timeMs = it.toMillis()) }
            }
            .sortedBy { it.timeMs }
            .toList()

    fun write(lines: List<LrcLine>): String =
        lines.joinToString("\n") { "[${formatTimestamp(it.timeMs)}]${it.markedText()}" }

    /** The lines without their timestamps. */
    fun toPlain(text: String): String =
        if (!isSynced(text)) text else parse(text).joinToString("\n") { it.text }

    fun formatTimestamp(millis: Long): String {
        val total = millis.coerceAtLeast(0)
        val minutes = total / 60_000
        val seconds = (total % 60_000) / 1000
        val hundredths = (total % 1000) / 10
        return "%02d:%02d.%02d".format(java.util.Locale.ROOT, minutes, seconds, hundredths)
    }

    /**
     * The text of a line with its word marks taken out, and the words they timed. A mark with
     * nothing after it is not a word: it is where the word before it ends, and the last one where
     * the line does.
     */
    private fun readWords(raw: String): LrcLine {
        val marks = WORD_MARK.findAll(raw).toList()
        if (marks.isEmpty()) return LrcLine(0, raw.trim())

        val text = StringBuilder(raw.substring(0, marks.first().range.first).trimStart())
        val words = mutableListOf<LrcWord>()
        var endMs: Long? = null
        marks.forEachIndexed { index, mark ->
            val until = marks.getOrNull(index + 1)?.range?.first ?: raw.length
            val piece =
                raw.substring(mark.range.last + 1, until).let {
                    if (text.isEmpty()) it.trimStart() else it
                }
            val time = mark.toMillis()
            if (piece.isBlank()) {
                val previous = words.lastOrNull()
                if (previous != null && previous.endMs == null) {
                    words[words.lastIndex] = previous.copy(endMs = time)
                }
                if (index == marks.lastIndex) endMs = time
            } else {
                // The range is the word alone: the space after it stays in the text, untimed.
                val start = text.length + (piece.length - piece.trimStart().length)
                words += LrcWord(time, null, start, text.length + piece.trimEnd().length)
            }
            text.append(piece)
        }
        return LrcLine(0, text.toString().trimEnd(), words, endMs)
    }

    private fun LrcLine.markedText(): String {
        if (words.isEmpty()) return text
        return buildString {
            append(text, 0, words.first().start)
            words.forEachIndexed { index, word ->
                val next = words.getOrNull(index + 1)
                append('<').append(formatTimestamp(word.timeMs)).append('>')
                append(text, word.start, word.end)
                // An end is only written where the next mark does not already say it.
                val close = word.endMs ?: endMs.takeIf { next == null }
                if (close != null && close != next?.timeMs) {
                    append('<').append(formatTimestamp(close)).append('>')
                }
                append(text, word.end, next?.start ?: text.length)
            }
        }
    }

    private fun MatchResult.toMillis(): Long {
        val (minutes, seconds, fraction) = destructured
        val fractionMs =
            when (fraction.length) {
                0 -> 0
                1 -> fraction.toInt() * 100
                2 -> fraction.toInt() * 10
                else -> fraction.take(3).toInt()
            }
        return minutes.toLong() * 60_000 + seconds.toLong() * 1000 + fractionMs
    }
}
