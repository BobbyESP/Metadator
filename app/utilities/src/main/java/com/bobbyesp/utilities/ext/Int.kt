package com.bobbyesp.utilities.ext

fun Int.bigQuantityFormatter(): String {
    return when (this) {
        in 0..999 -> this.toString()
        in 1000..999999 -> "${this / 1000} K"
        in 1000000..999999999 -> "${this / 1000000} M"
        else -> "${this / 1000000000} B"
    }
}

/**
 * Extension function to convert an integer representing seconds into a formatted string of minutes and seconds.
 *
 * @receiver Int The number of seconds to be converted.
 * @return String The formatted string in "MM:SS" format.
 */
fun Int.toMinutes(): String {
    val minutes = this / 60
    val seconds = this % 60
    return "%02d:%02d".format(minutes, seconds)
}

/**
 * Extension function to convert an integer representing milliseconds into a formatted string of minutes and seconds.
 *
 * @receiver Int The number of milliseconds to be converted.
 * @return String The formatted string in "MM:SS" format.
 */
fun Int.fromMillisToMinutes(): String {
    val totalSeconds = this / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}