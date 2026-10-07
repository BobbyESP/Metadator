package com.bobbyesp.metadator.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextTest {
    @Test
    fun `normalization drops accents, case and punctuation`() {
        assertEquals("beyonce halo remastered", "Beyoncé – Halo (Remastered)".normalizedForComparison())
    }

    @Test
    fun `similarity is 1 for equal text and lower for different text`() {
        assertEquals(1.0, similarity("Halo", "halo"), 0.0)
        assertTrue(similarity("Halo", "Hallo") > 0.7)
        assertTrue(similarity("Halo", "Crazy in Love") < 0.3)
    }

    @Test
    fun `durations format as minutes and seconds`() {
        assertEquals("3:07", formatDuration(187_000))
        assertEquals("1:02:03", formatDuration(3_723_000))
        assertEquals("0:00", formatDuration(-5))
    }
}
