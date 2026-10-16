/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ItemKeyTest {
    @Test
    fun `parts without separators read as written`() {
        assertEquals(
            "album:queen:a night at the opera",
            ItemKey.of("album", "queen", "a night at the opera"),
        )
    }

    @Test
    fun `a separator inside a part does not move the boundary between parts`() {
        assertNotEquals(ItemKey.of("album", "A:", "B"), ItemKey.of("album", "A", ":B"))
        assertNotEquals(ItemKey.of("album", "A\\", ":B"), ItemKey.of("album", "A", "\\:B"))
    }

    @Test
    fun `the namespace keeps kinds apart`() {
        assertNotEquals(ItemKey.of("artist", "Queen"), ItemKey.of("folder", "Queen"))
        assertNotEquals(ItemKey.of("a:b", "c"), ItemKey.of("a", "b:c"))
    }

    @Test
    fun `a missing part differs from an empty one and from the text of the marker`() {
        assertNotEquals(ItemKey.of("album", null), ItemKey.of("album", ""))
        assertNotEquals(ItemKey.of("album", null), ItemKey.of("album", "\\0"))
    }

    @Test
    fun `the number of parts is part of the key`() {
        assertNotEquals(ItemKey.of("album", "A"), ItemKey.of("album", "A", ""))
    }
}
