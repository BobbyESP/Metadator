/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.tags.api

/**
 * Every tag of a file, as TagLib's property map names them: upper-case keys, each with a list of
 * values. A key with no values is not in the map.
 *
 * Immutable on purpose. The editor never changes the map it read; it builds a new one, which is
 * what lets a save write back exactly what it read for every key the user did not touch.
 */
class TagMap private constructor(private val entries: Map<String, List<String>>) {

    val keys: Set<String>
        get() = entries.keys

    val size: Int
        get() = entries.size

    fun isEmpty(): Boolean = entries.isEmpty()

    operator fun get(key: String): List<String> = entries[normalizeKey(key)].orEmpty()

    operator fun contains(key: String): Boolean = normalizeKey(key) in entries

    /** The first value, for fields that hold one. */
    fun first(key: String): String? = get(key).firstOrNull()

    /** A copy with [key] set to [values]; an empty list removes it. */
    fun with(key: String, values: List<String>): TagMap {
        val normalized = normalizeKey(key)
        val cleaned = cleanValues(values)
        val next = entries.toMutableMap()
        if (cleaned.isEmpty()) next.remove(normalized) else next[normalized] = cleaned
        return TagMap(next)
    }

    fun with(key: String, value: String?): TagMap = with(key, listOfNotNull(value))

    fun without(key: String): TagMap = with(key, emptyList())

    fun toMap(): Map<String, List<String>> = entries

    override fun equals(other: Any?): Boolean = other is TagMap && entries == other.entries

    override fun hashCode(): Int = entries.hashCode()

    override fun toString(): String = "TagMap($entries)"

    companion object {
        val Empty = TagMap(emptyMap())

        fun of(map: Map<String, List<String>>): TagMap =
            TagMap(
                buildMap {
                    map.forEach { (key, values) ->
                        val cleaned = cleanValues(values)
                        if (key.isNotBlank() && cleaned.isNotEmpty()) {
                            // Two spellings of one key merge rather than overwrite each other.
                            val normalized = normalizeKey(key)
                            put(normalized, get(normalized).orEmpty() + cleaned)
                        }
                    }
                }
            )

        fun of(vararg pairs: Pair<String, List<String>>): TagMap = of(pairs.toMap())

        fun normalizeKey(key: String): String = key.trim().uppercase(java.util.Locale.ROOT)

        /** Values are kept verbatim, inner spaces and all; only empty ones are dropped. */
        private fun cleanValues(values: List<String>): List<String> = values.filter {
            it.isNotEmpty()
        }
    }
}
