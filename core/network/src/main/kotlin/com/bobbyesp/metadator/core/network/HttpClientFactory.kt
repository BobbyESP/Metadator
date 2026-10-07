/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object HttpClientFactory {
    /**
     * Identifies the app to every service, as MusicBrainz requires: anonymous clients get throttled
     * or blocked.
     */
    fun userAgent(versionName: String) =
        "Metadator/$versionName ( https://github.com/BobbyESP/Metadator )"

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        coerceInputValues = true
    }

    fun create(userAgent: String, engine: HttpClientEngine? = null): HttpClient {
        val configure: io.ktor.client.HttpClientConfig<*>.() -> Unit = {
            expectSuccess = false
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) {
                connectTimeoutMillis = 10_000
                requestTimeoutMillis = 20_000
                socketTimeoutMillis = 20_000
            }
            defaultRequest { headers.append(HttpHeaders.UserAgent, userAgent) }
        }
        return if (engine != null) HttpClient(engine, configure) else HttpClient(OkHttp, configure)
    }
}
