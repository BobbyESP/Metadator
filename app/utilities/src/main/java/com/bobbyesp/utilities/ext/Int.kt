package com.bobbyesp.utilities.ext

fun Int.bigQuantityFormatter(): String {
    return when (this) {
        in 0..999 -> this.toString()
        in 1000..999999 -> "${this / 1000} K"
        in 1000000..999999999 -> "${this / 1000000} M"
        else -> "${this / 1000000000} B"
    }
}

fun Int.toMinutes(): String {
    val minutes = this / 60
    val seconds = this % 60
    return "%02d:%02d".format(minutes, seconds)
}