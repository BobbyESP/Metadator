/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.tags.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TagChangesTest {

    private val original =
        TagMap.of(
            "TITLE" to listOf("Halo"),
            "ARTIST" to listOf("Beyoncé"),
            "REPLAYGAIN_TRACK_GAIN" to listOf("-7.2 dB"),
            "MUSICBRAINZ_TRACKID" to listOf("abc"),
            "BPM" to listOf("80"),
        )

    @Test
    fun `applying changes keeps every untouched key exactly`() {
        val result = original.applying(mapOf("TITLE" to listOf("Halo (Live)")))

        assertEquals(listOf("Halo (Live)"), result["TITLE"])
        assertEquals(listOf("-7.2 dB"), result["REPLAYGAIN_TRACK_GAIN"])
        assertEquals(listOf("abc"), result["MUSICBRAINZ_TRACKID"])
        assertEquals(listOf("80"), result["BPM"])
        assertEquals(original.size, result.size)
    }

    @Test
    fun `an empty list removes only that key`() {
        val result = original.applying(mapOf("BPM" to emptyList()))

        assertFalse("BPM" in result)
        assertEquals(original.size - 1, result.size)
    }

    @Test
    fun `multi values are kept apart, commas and all`() {
        val result =
            original.applying(mapOf("ARTIST" to listOf("Tyler, The Creator", "Kali Uchis")))

        assertEquals(listOf("Tyler, The Creator", "Kali Uchis"), result["ARTIST"])
    }

    @Test
    fun `keys are case insensitive`() {
        val map = TagMap.of("title" to listOf("a"))
        assertEquals(listOf("a"), map["TITLE"])
        assertEquals(listOf("b"), map.with("Title", listOf("b"))["title"])
    }

    @Test
    fun `diff lists only keys whose values differ`() {
        val edited =
            original.with("TITLE", listOf("Other")).without("BPM").with("MOOD", listOf("calm"))

        val changes = diff(original, edited)

        assertEquals(
            mapOf(
                "TITLE" to listOf("Other"),
                "BPM" to emptyList(),
                "MOOD" to listOf("calm"),
            ),
            changes,
        )
    }

    @Test
    fun `typing a value back is no change`() {
        val edited = original.with("TITLE", listOf("x")).with("TITLE", listOf("Halo"))
        assertTrue(diff(original, edited).isEmpty())
    }

    private val front = EmbeddedPicture(byteArrayOf(1), "image/jpeg", type = PictureType.FrontCover)
    private val back = EmbeddedPicture(byteArrayOf(2), "image/jpeg", type = PictureType.BackCover)
    private val newCover = EmbeddedPicture(byteArrayOf(3), "image/png", type = PictureType.Other)

    @Test
    fun `unchanged artwork is not written at all`() {
        assertNull(listOf(front, back).applying(ArtworkChange.Unchanged))
    }

    @Test
    fun `replacing the cover keeps the other pictures`() {
        val result = listOf(front, back).applying(ArtworkChange.Replace(newCover))!!

        assertEquals(2, result.size)
        assertEquals(PictureType.FrontCover, result[0].type)
        assertTrue(result[0].data.contentEquals(byteArrayOf(3)))
        assertEquals(back, result[1])
    }

    @Test
    fun `removing the cover keeps the other pictures`() {
        assertEquals(listOf(back), listOf(front, back).applying(ArtworkChange.Remove))
    }

    @Test
    fun `a lone untyped picture is treated as the cover`() {
        val untyped = front.copy(type = PictureType.Other)
        val result = listOf(untyped).applying(ArtworkChange.Replace(newCover))!!
        assertEquals(1, result.size)
    }

    @Test
    fun `picture types parse leniently`() {
        assertEquals(PictureType.FrontCover, PictureType.parse("Front Cover"))
        assertEquals(PictureType.FrontCover, PictureType.parse("front cover"))
        assertEquals(PictureType.BackCover, PictureType.parse("BackCover"))
        assertEquals(PictureType.Other, PictureType.parse("Something else"))
    }

    @Test
    fun `pictures compare by content`() {
        assertEquals(front, front.copy(data = byteArrayOf(1)))
    }
}
