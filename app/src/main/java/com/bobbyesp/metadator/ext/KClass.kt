package com.bobbyesp.metadator.ext

import kotlin.reflect.KClass

fun KClass<*>.formatToRoute(): String = this.qualifiedName.toString()