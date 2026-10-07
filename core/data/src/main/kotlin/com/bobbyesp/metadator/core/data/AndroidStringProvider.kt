/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.data

import android.content.Context
import com.bobbyesp.metadator.core.common.StringProvider

class AndroidStringProvider(private val context: Context) : StringProvider {
    override fun get(id: Int, vararg args: Any): String = context.getString(id, *args)

    override fun plural(id: Int, quantity: Int, vararg args: Any): String =
        context.resources.getQuantityString(id, quantity, *args)
}
