/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.lyrics.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SongLyricsTest {
    @Test
    fun `nothing to show is null`() {
        assertNull(SongLyrics.parse(""))
        assertNull(SongLyrics.parse("  \n "))
    }

    @Test
    fun `text without times is plain`() {
        assertEquals(
            SongLyrics.Plain("Just words\nMore words"),
            SongLyrics.parse("Just words\nMore words\n"),
        )
    }

    @Test
    fun `a line lasts until the next one, and the last a while`() {
        val lyrics = SongLyrics.parse("[00:01.00]One\n[00:04.00]Two") as SongLyrics.Synced

        assertEquals(
            listOf(TimedLine(1_000, 4_000, "One"), TimedLine(4_000, 14_000, "Two")),
            lyrics.lines,
        )
        assertFalse(lyrics.isWordByWord)
    }

    @Test
    fun `an empty line is not shown but ends the one before`() {
        val lyrics =
            SongLyrics.parse("[00:01.00]One\n[00:03.00]\n[00:09.00]Two") as SongLyrics.Synced

        assertEquals(listOf("One", "Two"), lyrics.lines.map { it.text })
        assertEquals(3_000L, lyrics.lines.first().endMs)
    }

    @Test
    fun `a word lasts until the next one, and the last until its line ends`() {
        val lyrics =
            SongLyrics.parse("[00:01.00]<00:01.00>One <00:01.50>two\n[00:04.00]Next")
                as SongLyrics.Synced

        assertTrue(lyrics.isWordByWord)
        assertEquals(
            listOf(TimedWord(1_000, 1_500, 0, 3), TimedWord(1_500, 4_000, 4, 7)),
            lyrics.lines.first().words,
        )
    }

    @Test
    fun `an explicit end wins over the next start`() {
        val lyrics =
            SongLyrics.parse("[00:01.00]<00:01.00>One<00:02.00>\n[00:04.00]Next")
                as SongLyrics.Synced

        assertEquals(2_000L, lyrics.lines.first().endMs)
        assertEquals(2_000L, lyrics.lines.first().words.single().endMs)
    }

    @Test
    fun `reads TTML`() {
        val ttml =
            """<tt xmlns="http://www.w3.org/ns/ttml"><body><div>""" +
                """<p begin="1.0s" end="3.0s"><span begin="1.0s" end="1.5s">Hi</span> """ +
                """<span begin="2.0s" end="3.0s">there</span></p></div></body></tt>"""
        val lyrics = SongLyrics.parse(ttml) as SongLyrics.Synced

        assertEquals(
            listOf(
                TimedLine(
                    1_000,
                    3_000,
                    "Hi there",
                    listOf(TimedWord(1_000, 1_500, 0, 2), TimedWord(2_000, 3_000, 3, 8)),
                )
            ),
            lyrics.lines,
        )
    }

    @Test
    fun `XML that cannot be read is shown as it is`() {
        assertEquals(
            SongLyrics.Plain("<tt><body><p>broken"),
            SongLyrics.parse("<tt><body><p>broken"),
        )
    }
}
