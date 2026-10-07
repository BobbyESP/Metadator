/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.common

/**
 * Text for ViewModels, which never hold a `Context`. The ids are Android string resources; this
 * module only carries them.
 */
interface StringProvider {
    fun get(id: Int, vararg args: Any): String

    fun plural(id: Int, quantity: Int, vararg args: Any): String
}
