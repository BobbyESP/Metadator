/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.lyrics.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcTest {
    private val lrc =
        """
        [ar:Someone]
        [00:12.50]First line
        [00:05.00][01:00.00]Chorus
        [00:20.1]Third
        """
            .trimIndent()

    @Test
    fun `parses timed lines in order, repeating multi-stamp lines`() {
        val lines = Lrc.parse(lrc)
        assertEquals(
            listOf(
                LrcLine(5_000, "Chorus"),
                LrcLine(12_500, "First line"),
                LrcLine(20_100, "Third"),
                LrcLine(60_000, "Chorus"),
            ),
            lines,
        )
    }

    @Test
    fun `detects synced text`() {
        assertTrue(Lrc.isSynced(lrc))
        assertFalse(Lrc.isSynced("Just words\nMore words"))
    }

    @Test
    fun `writes timestamps with hundredths`() {
        assertEquals("[01:02.34]Hi", Lrc.write(listOf(LrcLine(62_340, "Hi"))))
    }

    @Test
    fun `reads the words an enhanced line times, out of its text`() {
        val line = Lrc.parse("[00:12.00] <00:12.00>Hello <00:12.50>world<00:13.10>").single()

        assertEquals("Hello world", line.text)
        assertEquals(
            listOf(LrcWord(12_000, null, 0, 5), LrcWord(12_500, 13_100, 6, 11)),
            line.words,
        )
        assertEquals(13_100L, line.endMs)
        assertEquals("Hello", line.text.substring(line.words[0].start, line.words[0].end))
    }

    @Test
    fun `a mark with nothing after it ends the word before`() {
        val line = Lrc.parse("[00:01.00]Oh <00:01.00>one<00:01.40> <00:02.00>two").single()

        assertEquals("Oh one two", line.text)
        assertEquals(
            listOf(LrcWord(1_000, 1_400, 3, 6), LrcWord(2_000, null, 7, 10)),
            line.words,
        )
        assertEquals(null, line.endMs)
    }

    @Test
    fun `enhanced lines survive a round trip`() {
        val text =
            """
            [00:12.00]<00:12.00>Hello <00:12.50>world<00:13.10>
            [00:14.00]Oh <00:14.00>one<00:14.40> <00:15.00>two
            [00:20.00]No words here
            """
                .trimIndent()

        assertEquals(text, Lrc.write(Lrc.parse(text)))
    }

    @Test
    fun `plain text drops the word marks too`() {
        assertEquals("Hello world", Lrc.toPlain("[00:12.00]<00:12.00>Hello <00:12.50>world"))
    }

    @Test
    fun `a repeated line keeps its text and not its words`() {
        val lines = Lrc.parse("[00:01.00][00:30.00]<00:01.00>La <00:01.50>la")

        assertEquals(listOf(LrcLine(1_000, "La la"), LrcLine(30_000, "La la")), lines)
    }

    @Test
    fun `prefers synced lyrics`() {
        assertEquals("[00:01.00]a", Lyrics(plain = "a", synced = "[00:01.00]a").preferred)
        assertEquals("a", Lyrics(plain = "a", synced = " ").preferred)
    }
}
