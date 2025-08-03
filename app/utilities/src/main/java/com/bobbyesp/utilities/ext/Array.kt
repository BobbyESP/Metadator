package com.bobbyesp.utilities.ext

fun Array<String>?.joinToStringOrNull(separator: String = ", "): String? {
    return this?.joinToString(separator = separator)
}
