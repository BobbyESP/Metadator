/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.lyrics.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TtmlTest {
    private val ttml =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
          <head><metadata><ttm:agent type="person" xml:id="v1"/></metadata></head>
          <body>
            <div>
              <p begin="00:12.000" end="00:15.500" ttm:agent="v1">
                <span begin="00:12.000" end="00:12.400">Hel</span><span begin="00:12.400" end="00:12.900">lo</span>
                <span begin="00:13.000" end="00:14.000">world</span>
                <span ttm:role="x-translation">Hola mundo</span>
                <span ttm:role="x-bg">(<span begin="00:14.200" end="00:15.000">oh</span>)</span>
              </p>
              <p begin="1:00:01.5">Line<br/>only</p>
              <p>Never timed</p>
            </div>
          </body>
        </tt>
        """
            .trimIndent()

    @Test
    fun `a paragraph is a line and a timed span a word of it`() {
        val line = Ttml.parse(ttml)!!.first()

        assertEquals(12_000L, line.timeMs)
        assertEquals(15_500L, line.endMs)
        assertEquals("Hello world (oh)", line.text)
        assertEquals(
            listOf(
                LrcWord(12_000, 12_400, 0, 3),
                LrcWord(12_400, 12_900, 3, 5),
                LrcWord(13_000, 14_000, 6, 11),
                LrcWord(14_200, 15_000, 13, 15),
            ),
            line.words,
        )
    }

    @Test
    fun `a paragraph without spans is a line with no words, and one without a time is skipped`() {
        val lines = Ttml.parse(ttml)!!

        assertEquals(2, lines.size)
        assertEquals(LrcLine(3_601_500, "Line only"), lines[1])
    }

    @Test
    fun `reads clock times and offsets`() {
        assertEquals(3_723_500L, Ttml.parseTime("01:02:03.5"))
        assertEquals(62_250L, Ttml.parseTime("1:02.25"))
        assertEquals(12_500L, Ttml.parseTime("12.5"))
        assertEquals(12_500L, Ttml.parseTime("12.5s"))
        assertEquals(300L, Ttml.parseTime("300ms"))
        assertEquals(120_000L, Ttml.parseTime("2m"))
        assertEquals(3_600_000L, Ttml.parseTime("1h"))
        assertNull(Ttml.parseTime("24f"))
        assertNull(Ttml.parseTime("soon"))
        assertNull(Ttml.parseTime("NaN"))
    }

    @Test
    fun `refuses a document type, and what it could bring in`() {
        val hostile =
            """
            <?xml version="1.0"?>
            <!DOCTYPE tt [<!ENTITY secret SYSTEM "file:///etc/passwd">]>
            <tt><body><p begin="1s">&secret;</p></body></tt>
            """
                .trimIndent()

        assertNull(Ttml.parse(hostile))
    }

    @Test
    fun `text that is not XML is not read`() {
        assertNull(Ttml.parse("<tt><body><p>broken"))
    }
}
