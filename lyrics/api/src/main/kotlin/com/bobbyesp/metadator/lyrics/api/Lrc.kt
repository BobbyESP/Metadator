/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.lyrics.api

/** One line of synced lyrics. */
data class LrcLine(val timeMs: Long, val text: String)

/** Reads and writes the LRC format: `[mm:ss.xx] text`, metadata tags ignored. */
object Lrc {
    private val TIMESTAMP = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")

    /** Whether [text] has at least one timed line, i.e. is synced. */
    fun isSynced(text: String): Boolean = text.lineSequence().any { TIMESTAMP.containsMatchIn(it) }

    /** Timed lines in order. A line with several timestamps repeats at each of them. */
    fun parse(text: String): List<LrcLine> =
        text
            .lineSequence()
            .flatMap { line ->
                val stamps = TIMESTAMP.findAll(line).toList()
                if (stamps.isEmpty()) return@flatMap emptySequence()
                val lyric = line.substring(stamps.last().range.last + 1).trim()
                stamps.asSequence().map { LrcLine(it.toMillis(), lyric) }
            }
            .sortedBy { it.timeMs }
            .toList()

    fun write(lines: List<LrcLine>): String =
        lines.joinToString("\n") { "[${formatTimestamp(it.timeMs)}]${it.text}" }

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
