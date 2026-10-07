package com.bobbyesp.metadator

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Asks for a Play review after the third save, and again after the twenty-fifth: never after an
 * error, never before the app has proved itself. Play decides whether the dialog really shows.
 */
@Composable
fun ReviewPrompt(successfulSaves: Int) {
    val activity = LocalActivity.current ?: return
    // Only a save made now counts: reopening the app with three saves behind it is not a moment.
    val atLaunch = remember { successfulSaves }
    LaunchedEffect(successfulSaves) {
        if (successfulSaves == atLaunch) return@LaunchedEffect
        if (successfulSaves != 3 && successfulSaves != 25) return@LaunchedEffect
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnCompleteListener { request ->
            if (request.isSuccessful) manager.launchReviewFlow(activity, request.result)
        }
    }
}
