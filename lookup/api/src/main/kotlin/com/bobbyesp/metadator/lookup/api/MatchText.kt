/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.lookup.api

import com.bobbyesp.metadator.core.common.normalizedForComparison
import com.bobbyesp.metadator.core.common.similarity

/** Like [similarity], but word order does not matter: "Knowles Beyoncé" is "Beyoncé Knowles". */
internal fun textSimilarity(a: String, b: String): Double {
    val direct = similarity(a, b)
    if (direct == 1.0) return 1.0
    return maxOf(direct, similarity(a.sortedWords(), b.sortedWords()))
}

private fun String.sortedWords() = normalizedForComparison().split(' ').sorted().joinToString(" ")

internal fun artistSimilarity(a: String, b: String): Double =
    textSimilarity(a.withoutArticle(), b.withoutArticle())

private fun String.withoutArticle() = normalizedForComparison().removePrefix("the ")

/** "Gracie Abrams feat. Taylor Swift" is two artists. */
internal fun splitArtists(text: String): List<String> =
    text.split(ARTIST_SEPARATORS).map { it.trim() }.filter { it.isNotEmpty() }

private val ARTIST_SEPARATORS =
    Regex(
        """\s*(?:[,;/&+×]|\s(?:x|and|y|e|feat\.?|ft\.?|featuring|with|vs\.?)\s)\s*""",
        RegexOption.IGNORE_CASE,
    )

/** A kind of recording that is not the one a plain title names, and how far it is from it. */
internal enum class Variant(val penalty: Double, val phrases: List<String>) {
    Karaoke(0.5, listOf("karaoke", "backing track", "minus one")),
    Cover(
        0.55,
        listOf(
            "cover",
            "tribute",
            "made famous by",
            "as made famous",
            "in the style of",
            "originally performed by",
        ),
    ),
    Altered(
        0.6,
        listOf(
            "nightcore",
            "daycore",
            "slowed",
            "sped up",
            "speed up",
            "reverb",
            "8d",
            "bass boosted",
        ),
    ),
    Instrumental(0.6, listOf("instrumental", "off vocal")),
    ACappella(0.65, listOf("a cappella", "acapella", "acappella", "vocals only")),
    Live(0.75, listOf("live", "en vivo", "en directo", "ao vivo", "dal vivo", "concert")),
    Remix(
        0.75,
        listOf("remix", "remixed", "rmx", "club mix", "dub mix", "bootleg", "rework", "mix"),
    ),
    Acoustic(0.8, listOf("acoustic", "acustico", "acustica", "unplugged", "stripped")),
    Demo(
        0.8,
        listOf(
            "demo",
            "rehearsal",
            "outtake",
            "alternate take",
            "alternate version",
            "early version",
        ),
    ),
    Orchestral(0.8, listOf("orchestral", "symphonic", "orchestra")),
    Reprise(0.85, listOf("reprise")),
    Edit(
        0.9,
        listOf(
            "edit",
            "radio edit",
            "single edit",
            "radio version",
            "radio mix",
            "extended",
            "extended mix",
            "extended version",
            "short version",
        ),
    ),
    Mono(0.95, listOf("mono")),
    Remaster(
        0.97,
        listOf("remaster", "remastered", "remasterizado", "remasterizada", "remasterise"),
    ),
    /** A version the app cannot name, like "Taylor's Version". */
    Other(0.9, emptyList()),
}

private val NEUTRAL_PHRASES =
    listOf(
        "original",
        "original mix",
        "original version",
        "album version",
        "single version",
        "lp version",
        "main version",
        "explicit",
        "explicit version",
        "clean",
        "clean version",
        "bonus track",
    )

/** Longest first, so "radio edit" is read before "edit" and "original mix" before "mix". */
private val PHRASES: List<Pair<String, Variant?>> =
    (Variant.entries.flatMap { variant -> variant.phrases.map { it to variant } } +
            NEUTRAL_PHRASES.map { it to null })
        .sortedByDescending { it.first.length }

private val STRONG_MARKERS = setOf(Variant.Karaoke, Variant.Cover, Variant.Altered)

/**
 * The variants a decoration ("Live at Wembley", "2011 Remaster") names, or null when it is not a
 * decoration but part of the title, like "(Don't Fear)".
 */
private fun classify(decoration: String): Set<Variant>? {
    var rest = " ${decoration.normalizedForComparison()} "
    if (rest.startsWith(" from ")) return emptySet()
    val found = mutableSetOf<Variant>()
    var recognized = false
    for ((phrase, variant) in PHRASES) {
        val bounded = " $phrase "
        if (bounded in rest) {
            rest = rest.replace(bounded, " ")
            recognized = true
            variant?.let(found::add)
        }
    }
    if (found.isEmpty() && " version " in rest) found += Variant.Other
    return if (found.isEmpty() && !recognized) null else found
}

/**
 * Karaoke, tribute and sped-up uploads often say so only in the album or the artist ("Ameritz
 * Karaoke"); a live album's name is too often a title ("Live Through This") to count.
 */
internal fun markersIn(text: String?): Set<Variant> {
    if (text.isNullOrBlank()) return emptySet()
    val padded = " ${text.normalizedForComparison()} "
    return PHRASES.mapNotNullTo(mutableSetOf()) { (phrase, variant) ->
        variant?.takeIf { it in STRONG_MARKERS && " $phrase " in padded }
    }
}

/** A title split into the song's name, the version it names and the artists it features. */
internal class TitleParts(
    val full: String,
    val core: String,
    val variants: Set<Variant>,
    val featured: List<String>,
)

internal fun parseTitle(title: String): TitleParts {
    val variants = mutableSetOf<Variant>()
    val featured = mutableListOf<String>()

    fun strip(decoration: String): Boolean {
        FEATURING.matchEntire(decoration.trim())?.let {
            featured += splitArtists(it.groupValues[1])
            return true
        }
        variants += classify(decoration) ?: return false
        return true
    }

    var core =
        BRACKETS.replace(title) {
            if (strip(it.groupValues[1] + it.groupValues[2])) " " else it.value
        }
    DASH.find(core)?.let {
        if (strip(core.substring(it.range.last + 1))) core = core.substring(0, it.range.first)
    }
    INLINE_FEATURING.find(core)?.let {
        featured += splitArtists(it.groupValues[1])
        core = core.substring(0, it.range.first)
    }
    return TitleParts(title, core.trim().ifEmpty { title }, variants, featured)
}

/** The album's name without its edition: "MAYHEM (Deluxe)" is "MAYHEM". */
internal fun albumCore(album: String): String {
    fun isEdition(text: String) =
        classify(text) != null ||
            text.normalizedForComparison().split(' ').any { it in EDITION_WORDS }

    var core =
        BRACKETS.replace(album) {
            if (isEdition(it.groupValues[1] + it.groupValues[2])) " " else it.value
        }
    DASH.find(core)?.let {
        if (isEdition(core.substring(it.range.last + 1))) core = core.substring(0, it.range.first)
    }
    return core.trim().ifEmpty { album }
}

internal fun isCompilation(album: String?, albumArtists: List<String>): Boolean {
    if (albumArtists.any { it.normalizedForComparison() in VARIOUS_ARTISTS }) return true
    if (album.isNullOrBlank()) return false
    val padded = " ${album.normalizedForComparison()} "
    return COMPILATION_PHRASES.any { " $it " in padded }
}

private val BRACKETS = Regex("""\(([^()]*)\)|\[([^\[\]]*)\]""")
internal val DASH = Regex("""\s+[-–—]\s+""")
private val FEATURING =
    Regex("""^(?:feat\.?|ft\.?|featuring|with|con)\s+(.+)$""", RegexOption.IGNORE_CASE)
private val INLINE_FEATURING =
    Regex("""\s+(?:feat\.?|ft\.?|featuring)\s+(.+)$""", RegexOption.IGNORE_CASE)

private val EDITION_WORDS =
    setOf(
        "deluxe",
        "edition",
        "edicion",
        "expanded",
        "anniversary",
        "bonus",
        "special",
        "collector",
        "collectors",
        "remaster",
        "remastered",
        "reissue",
        "ep",
        "single",
        "version",
        "explicit",
        "clean",
        "platinum",
        "super",
    )

private val VARIOUS_ARTISTS =
    setOf(
        "various artists",
        "various",
        "va",
        "varios artistas",
        "artistes divers",
        "verschiedene interpreten",
        "artisti vari",
    )

private val COMPILATION_PHRASES =
    listOf(
        "greatest hits",
        "best of",
        "the best",
        "very best",
        "hits",
        "collection",
        "anthology",
        "essential",
        "essentials",
        "compilation",
        "now that s what i call",
    )
