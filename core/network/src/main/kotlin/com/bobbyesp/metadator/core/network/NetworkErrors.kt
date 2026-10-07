package com.bobbyesp.metadator.core.network

import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/** Whether [error] means "no connection" rather than "the service answered badly". */
fun isOfflineError(error: Throwable): Boolean =
    error is IOException ||
        error.cause is IOException ||
        error::class.simpleName in setOf("UnresolvedAddressException", "HttpRequestTimeoutException")

/** Runs [block], letting cancellation through and handing every other failure to [onError]. */
inline fun <T> networkCall(onError: (Throwable) -> T, block: () -> T): T =
    try {
        block()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        onError(error)
    }
