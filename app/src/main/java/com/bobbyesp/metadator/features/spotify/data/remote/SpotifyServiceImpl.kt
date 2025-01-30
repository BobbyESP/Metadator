package com.bobbyesp.metadator.features.spotify.data.remote

import android.util.Log
import com.adamratzman.spotify.SpotifyAppApi
import com.adamratzman.spotify.SpotifyException
import com.adamratzman.spotify.models.Token
import com.adamratzman.spotify.spotifyAppApi
import com.bobbyesp.metadator.core.util.executeIfDebugging
import com.bobbyesp.metadator.features.spotify.domain.services.SpotifyService
import com.bobbyesp.utilities.Logging.isDebug
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

class SpotifyServiceImpl : SpotifyService, KoinComponent {

    private val clientId: String by inject(named("client_id"))
    private val clientSecret: String by inject(named("client_secret"))

    private var token: Token? = null
    private var api: SpotifyAppApi? = null

    private var recursionDepth = 0

    /**
     * Retrieves the Spotify API instance.
     *
     * This function returns a [SpotifyAppApi] instance, allowing interaction with the Spotify API.
     * It ensures that the API is built before returning it. If the API has not been built yet, it will call the [buildApi] function to initialize it.
     *
     * @return A [SpotifyAppApi] instance, ready for use.
     * @throws IllegalStateException If the connection to the Spotify API was not established. This can occur due to network issues or server outages.
     *
     */
    override suspend fun getSpotifyApi(): SpotifyAppApi {
        if (api == null) {
            buildApi()
        }
        return api
            ?: throw IllegalStateException(
                "The connection to the Spotify API was not established." +
                        " This may be due to a network error or a servers outage."
            )
    }

    /**
     * Retrieves the Spotify access token.
     *
     * This function fetches the Spotify access token, ensuring it's available
     * before returning it. If the token is null, it attempts to build/refresh
     * the token via the `buildApi()` method.
     *
     * @return A [Token] object representing the Spotify access token.
     * @throws IllegalStateException if the Spotify token is still null after
     * attempting to build/refresh it. This indicates a failure in the token
     * acquisition process.
     */
    override suspend fun getSpotifyToken(): Token {
        if (token == null) {
            buildApi()
        }
        return token ?: throw IllegalStateException("Spotify Token is null")
    }

    /**
     * This method is responsible for building the Spotify API.
     * It first checks if the application is in debug mode, and if so, logs the client ID and secret.
     * Then, it attempts to build the Spotify API with the provided client ID and secret.
     * If the API is successfully built, it retrieves the token from the API and stores it.
     *
     * If an exception occurs during the building of the API, it logs the error.
     * If a BadRequestException occurs, it checks if the token should be refreshed and if the recursion depth is less than the maximum allowed.
     * If both conditions are met, it logs the information, clears the API, and attempts to build the API again, incrementing the recursion depth.
     *
     * @throws Exception if there is an error building the API.
     * @throws SpotifyException.BadRequestException if a bad request is made to the Spotify API.
     */
    private suspend fun buildApi() {
        try {
            executeIfDebugging {
                Log.d(
                    "SpotifyApiRequests",
                    "Building API with client ID: $clientId and client secret: $clientSecret"
                )
            }
            api = spotifyAppApi(clientId, clientSecret).build().apply {
                with(this.spotifyApiOptions) {
                    automaticRefresh = true
                    enableDebugMode = isDebug
                }
            }
            token = api?.token
        } catch (e: Exception) {
            executeIfDebugging { Log.e("SpotifyApiRequests", "Error building API", e) }
        } catch (e: SpotifyException.BadRequestException) {
            Log.e("SpotifyApiRequests", "Bad request exception", e)
            token?.let {
                if (it.shouldRefresh() && recursionDepth < MAX_RECURSION_DEPTH) {
                    Log.i(
                        "SpotifyApiRequests",
                        "Token expired, refreshing token; recursion depth: $recursionDepth of $MAX_RECURSION_DEPTH"
                    )
                    clearApi()
                    buildApi()
                    recursionDepth++
                    return@let
                }
            }
        }
    }

    private fun clearApi() {
        api = null
        token = null
    }

    companion object {
        private const val MAX_RECURSION_DEPTH = 3
    }
}