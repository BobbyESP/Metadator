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
    fun `prefers synced lyrics`() {
        assertEquals("[00:01.00]a", Lyrics(plain = "a", synced = "[00:01.00]a").preferred)
        assertEquals("a", Lyrics(plain = "a", synced = " ").preferred)
    }
}
