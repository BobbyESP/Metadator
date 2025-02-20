package com.bobbyesp.coreutilities

import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

/**
 * Register an observer class that gets callbacks when data identified by a given content URI
 * changes.
 *
 * @param uri The content URI to observe.
 * @return A Flow that emits a Boolean indicating whether the content has changed.
 */
fun ContentResolver.observe(uri: Uri) = callbackFlow {
    val observer = object : ContentObserver(null) {
        /**
         * Called when a change occurs to the content URI.
         *
         * @param selfChange A boolean indicating if the change was made by the observer itself.
         */
        /**
         * Called when a change occurs to the content URI.
         *
         * @param selfChange A boolean indicating if the change was made by the observer itself.
         */
        override fun onChange(selfChange: Boolean) {
            trySend(selfChange)
        }
    }
    registerContentObserver(uri, true, observer)
    // Trigger the first emission.
    trySend(false)
    awaitClose {
        unregisterContentObserver(observer)
    }
}