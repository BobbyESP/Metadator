package com.bobbyesp.metadator.ext

import kotlin.reflect.KClass

fun KClass<*>.qualifiedName(): String = this.qualifiedName.toString()