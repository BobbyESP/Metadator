/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.model

/**
 * Keys for lazy list items, built from a namespace and the values that make an item what it is.
 *
 * Every part is escaped before the parts are joined, so two different lists of parts never give the
 * same key: an album "B" by "A:" stays apart from an album ":B" by "A". A key is a plain `String`
 * because a lazy list saves its keys in a `Bundle`.
 *
 * A key must come from the same values the item is told apart by elsewhere, such as the values a
 * list is grouped by: then two items share a key only when they are the same item.
 */
object ItemKey {
    private const val SEPARATOR = ':'
    private const val ESCAPE = '\\'
    private const val NULL = "$ESCAPE" + "0"

    fun of(namespace: String, vararg parts: String?): String = buildString {
        append(namespace.escaped())
        for (part in parts) {
            append(SEPARATOR)
            append(part?.escaped() ?: NULL)
        }
    }

    private fun String.escaped(): String =
        if (none { it == SEPARATOR || it == ESCAPE }) this
        else
            buildString(length + 4) {
                for (char in this@escaped) {
                    if (char == SEPARATOR || char == ESCAPE) append(ESCAPE)
                    append(char)
                }
            }
}
