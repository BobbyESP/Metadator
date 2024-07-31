package com.bobbyesp.crashhandler.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun CrashHandlerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    //if is android higher than 12, use dynamic color scheme, else use static color scheme based on dark theme
    if (android.os.Build.VERSION.SDK_INT >= 31) {
        MaterialTheme(
            colorScheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(
                context
            )
        ) {
            content()
        }
    } else {
        MaterialTheme(
            colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
        ) {
            content()
        }
    }
}