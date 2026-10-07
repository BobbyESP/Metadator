package com.bobbyesp.metadator.library.mediastore

import android.app.Activity
import android.content.IntentSender
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Lets code without an activity show a system prompt and wait for the answer: the write-access
 * dialog, or a permission. Activities lend their launchers by calling [attach] in `onCreate`; the
 * most recently attached one that is still alive shows the prompt.
 *
 * A prompt survives the activity being recreated under it: the registry delivers the result to
 * the new activity's launcher, which completes the same pending request.
 */
class ActivityResultHost {
    private class Launchers(
        val activity: Activity,
        val intentSender: ActivityResultLauncher<IntentSenderRequest>,
        val permission: ActivityResultLauncher<String>,
    )

    private val attached = ArrayDeque<Launchers>()
    private var pending: CompletableDeferred<Boolean>? = null
    private val mutex = Mutex()

    fun attach(activity: ComponentActivity) {
        val launchers =
            Launchers(
                activity = activity,
                intentSender =
                    activity.registerForActivityResult(
                        ActivityResultContracts.StartIntentSenderForResult()
                    ) { result ->
                        pending?.complete(result.resultCode == Activity.RESULT_OK)
                    },
                permission =
                    activity.registerForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { granted ->
                        pending?.complete(granted)
                    },
            )
        attached.addLast(launchers)
        activity.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    attached.remove(launchers)
                }
            }
        )
    }

    /** Shows [sender] and returns whether the user accepted, or null if nothing could show it. */
    suspend fun launch(sender: IntentSender): Boolean? =
        prompt { it.intentSender.launch(IntentSenderRequest.Builder(sender).build()) }

    /** Asks for [permission] and returns whether it was granted, or null if nothing could ask. */
    suspend fun requestPermission(permission: String): Boolean? =
        prompt { it.permission.launch(permission) }

    private suspend fun prompt(show: (Launchers) -> Unit): Boolean? =
        mutex.withLock {
            withContext(Dispatchers.Main.immediate) {
                val launchers = attached.lastOrNull { !it.activity.isFinishing } ?: return@withContext null
                val answer = CompletableDeferred<Boolean>()
                pending = answer
                try {
                    show(launchers)
                    answer.await()
                } finally {
                    pending = null
                }
            }
        }
}
