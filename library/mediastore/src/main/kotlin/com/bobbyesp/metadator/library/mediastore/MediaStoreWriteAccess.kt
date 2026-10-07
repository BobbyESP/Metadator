/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.library.mediastore

import android.Manifest
import android.app.RecoverableSecurityException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.bobbyesp.metadator.core.model.ContentRef
import com.bobbyesp.metadator.library.api.AccessResult
import com.bobbyesp.metadator.library.api.WriteAccess

/**
 * Write access to other apps' files, the way each Android version asks for it:
 * - up to 9, the storage permission, once;
 * - 10, a prompt per file, which only a refused write produces ([RecoverableAccessCache]);
 * - 11 and later, one prompt for any number of files, asked before writing.
 *
 * A document another app handed over with write access needs nothing; one handed over read-only
 * cannot be asked for, and is reported as denied.
 */
class MediaStoreWriteAccess(
    private val context: Context,
    private val host: ActivityResultHost,
    private val recoverable: RecoverableAccessCache,
) : WriteAccess {

    override suspend fun canWrite(refs: List<ContentRef>): Boolean = missing(refs).isEmpty()

    override suspend fun request(refs: List<ContentRef>): AccessResult {
        val missing = missing(refs)
        if (missing.isEmpty()) return AccessResult.Granted

        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                val mediaUris = missing.map { Uri.parse(it.uri) }.filter(::isMediaStoreUri)
                if (mediaUris.size < missing.size) return AccessResult.Denied
                val request = MediaStore.createWriteRequest(context.contentResolver, mediaUris)
                host.launch(request.intentSender).toAccessResult()
            }
            Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> {
                for (ref in missing) {
                    val exception =
                        recoverable.take(ref) as? RecoverableSecurityException
                            ?: return AccessResult.Denied
                    val answer = host.launch(exception.userAction.actionIntent.intentSender)
                    if (answer != true) return answer.toAccessResult()
                }
                AccessResult.Granted
            }
            else ->
                host.requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE).toAccessResult()
        }
    }

    private fun missing(refs: List<ContentRef>): List<ContentRef> = refs.filterNot { ref ->
        val uri = Uri.parse(ref.uri)
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || !isMediaStoreUri(uri) ->
                context.checkUriPermission(
                    uri,
                    Process.myPid(),
                    Process.myUid(),
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                ) == PackageManager.PERMISSION_GRANTED
            // Android 10 only says no when writing; until then, assume yes.
            Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> true
            else ->
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun Boolean?.toAccessResult(): AccessResult =
        when (this) {
            true -> AccessResult.Granted
            false -> AccessResult.Denied
            null -> AccessResult.Unavailable
        }
}
