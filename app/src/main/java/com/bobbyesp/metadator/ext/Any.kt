package com.bobbyesp.metadator.ext

fun Any.formatAsClassToRoute(): String = this::class.qualifiedName.toString()