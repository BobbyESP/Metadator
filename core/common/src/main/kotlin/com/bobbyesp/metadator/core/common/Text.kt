package com.bobbyesp.metadator.core.common

import java.text.Normalizer
import java.util.Locale

/**
 * Lower case, without accents or punctuation, with single spaces: the form two titles are
 * compared in. "Beyoncé – Halo (Remastered)" and "beyonce halo remastered" are the same here.
 */
fun String.normalizedForComparison(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(DIACRITICS, "")
        .lowercase(Locale.ROOT)
        .replace(NON_ALPHANUMERIC, " ")
        .trim()
        .replace(WHITESPACE, " ")

private val DIACRITICS = Regex("\\p{Mn}+")
private val NON_ALPHANUMERIC = Regex("[^\\p{L}\\p{N}]+")
private val WHITESPACE = Regex("\\s+")

/** Similarity between 0 (nothing in common) and 1 (equal), on normalized text. */
fun similarity(a: String, b: String): Double {
    val x = a.normalizedForComparison()
    val y = b.normalizedForComparison()
    if (x.isEmpty() && y.isEmpty()) return 1.0
    if (x.isEmpty() || y.isEmpty()) return 0.0
    if (x == y) return 1.0
    val distance = levenshtein(x, y)
    return 1.0 - distance.toDouble() / maxOf(x.length, y.length)
}

private fun levenshtein(a: String, b: String): Int {
    var previous = IntArray(b.length + 1) { it }
    var current = IntArray(b.length + 1)
    for (i in 1..a.length) {
        current[0] = i
        for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            current[j] = minOf(current[j - 1] + 1, previous[j] + 1, previous[j - 1] + cost)
        }
        val swap = previous
        previous = current
        current = swap
    }
    return previous[b.length]
}

/** "3:07", or "1:02:03" past an hour. */
fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    else String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
}

/** "4.2 MB". */
fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = listOf("KB", "MB", "GB")
    var value = bytes / 1024.0
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return String.format(Locale.getDefault(), "%.1f %s", value, units[unit])
}
