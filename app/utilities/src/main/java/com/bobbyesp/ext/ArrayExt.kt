package com.bobbyesp.ext

fun Array<String>?.joinOrNullToString(): String? {
    return this?.joinToString(separator = ", ")
}