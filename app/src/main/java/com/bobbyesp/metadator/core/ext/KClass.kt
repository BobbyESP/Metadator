package com.bobbyesp.metadator.core.ext

import kotlin.reflect.KClass

fun KClass<*>.qualifiedName(): String = this.qualifiedName.toString()
