/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.lyrics.api

import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.roundToLong
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource

/**
 * Reads lyrics written as TTML, the format that times every word or syllable: a `<p>` is a line and
 * a `<span>` with a `begin` is a word of it.
 */
object Ttml {
    private val DECIMAL = Regex("""\d+(?:\.\d+)?""")
    private val WHITESPACE = Regex("""\s+""")

    /** Spans that repeat the line in another language or script, rather than being sung. */
    private val NOT_SUNG = setOf("x-translation", "x-roman")

    fun looksLikeTtml(text: String): Boolean {
        val start = text.trimStart()
        return start.startsWith("<?xml") || start.startsWith("<tt")
    }

    /** The lines, as LRC would give them; null when [text] is not XML that can be read. */
    fun parse(text: String): List<LrcLine>? = runCatching {
        val root = builder().parse(InputSource(StringReader(text))).documentElement
        buildList { collectLines(root, this) }.sortedBy { it.timeMs }
    }
        .getOrNull()

    /**
     * A time expression: clock time (`hh:mm:ss.fff`, `mm:ss.fff`) or an offset (`12.5s`, `300ms`,
     * `2m`, `1h`, or bare seconds). Frames and ticks need the document's rates and are not read.
     */
    internal fun parseTime(value: String): Long? {
        val trimmed = value.trim()
        if (':' in trimmed) {
            val parts = trimmed.split(':')
            if (parts.size !in 2..3) return null
            val seconds =
                parts.fold(0.0) { total, part -> total * 60 + (number(part) ?: return null) }
            return (seconds * 1000).roundToLong()
        }
        val unit = trimmed.takeLastWhile { it.isLetter() }
        val amount = number(trimmed.dropLast(unit.length)) ?: return null
        val millis =
            when (unit) {
                "",
                "s" -> 1000.0
                "ms" -> 1.0
                "m" -> 60_000.0
                "h" -> 3_600_000.0
                else -> return null
            }
        return (amount * millis).roundToLong()
    }

    private fun number(text: String): Double? = text.takeIf { DECIMAL.matches(it) }?.toDouble()

    /**
     * A parser that reads the document and nothing else: a lyrics tag is text from anywhere, and a
     * DTD or an external entity in it would have the parser open files or URLs. Each parser knows
     * different features and throws on the ones it does not, Android's included.
     */
    private fun builder() =
        DocumentBuilderFactory.newInstance()
            .apply {
                isNamespaceAware = true
                isExpandEntityReferences = false
                listOf(
                        "http://apache.org/xml/features/disallow-doctype-decl" to true,
                        "http://xml.org/sax/features/external-general-entities" to false,
                        "http://xml.org/sax/features/external-parameter-entities" to false,
                    )
                    .forEach { (feature, enabled) -> runCatching { setFeature(feature, enabled) } }
            }
            .newDocumentBuilder()
            // Unreadable text is an answer here, not something to print.
            .apply { setErrorHandler(null) }

    private fun collectLines(element: Element, into: MutableList<LrcLine>) {
        if (element.name == "p") {
            readLine(element)?.let(into::add)
            return
        }
        element.children().filterIsInstance<Element>().forEach { collectLines(it, into) }
    }

    private fun readLine(paragraph: Element): LrcLine? {
        val begin = paragraph.time("begin") ?: return null
        val line = LineBuilder()
        line.read(paragraph)
        return LrcLine(begin, line.text.toString().trimEnd(), line.words, paragraph.time("end"))
    }

    private class LineBuilder {
        val text = StringBuilder()
        val words = mutableListOf<LrcWord>()

        fun read(element: Element) {
            element.children().forEach { child ->
                when {
                    child.nodeType == Node.TEXT_NODE || child.nodeType == Node.CDATA_SECTION_NODE ->
                        append(child.nodeValue.orEmpty())
                    child !is Element -> Unit
                    child.name == "br" -> append(" ")
                    child.attribute("role") in NOT_SUNG -> Unit
                    else -> {
                        val begin = child.time("begin")
                        // A span with no time of its own only groups the ones inside it.
                        if (child.name != "span" || begin == null) {
                            read(child)
                        } else {
                            val range = append(child.textContent.orEmpty())
                            if (range != null) {
                                words +=
                                    LrcWord(begin, child.time("end"), range.first, range.second)
                            }
                        }
                    }
                }
            }
        }

        /**
         * Adds [raw] with its whitespace collapsed, as XML that was indented needs, and returns
         * where its words went: start, and end exclusive. Null when it was only whitespace.
         */
        private fun append(raw: String): Pair<Int, Int>? {
            var piece = raw.replace(WHITESPACE, " ")
            if (text.isEmpty() || text.last() == ' ') piece = piece.trimStart()
            val start = text.length + (piece.length - piece.trimStart().length)
            val end = text.length + piece.trimEnd().length
            text.append(piece)
            return if (end > start) start to end else null
        }
    }

    private val Element.name: String
        get() = localName ?: tagName.substringAfter(':')

    private fun Element.children(): List<Node> = List(childNodes.length) { childNodes.item(it) }

    /** An attribute by its local name, whatever prefix the document gave its namespace. */
    private fun Element.attribute(name: String): String? {
        for (index in 0 until attributes.length) {
            val attribute = attributes.item(index)
            val local = attribute.localName ?: attribute.nodeName.substringAfter(':')
            if (local == name) return attribute.nodeValue
        }
        return null
    }

    private fun Element.time(name: String): Long? = attribute(name)?.let(::parseTime)
}
