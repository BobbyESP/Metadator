package com.bobbyesp.metadator.presentation.theme

import android.os.Build
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextDirection
import com.bobbyesp.metadator.core.presentation.common.LocalDynamicColoringSwitch
import com.bobbyesp.metadator.core.presentation.common.LocalDynamicThemeState
import com.materialkolor.DynamicMaterialTheme

fun isDynamicColoringSupported(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}

@Composable
fun MetadatorTheme(
    content: @Composable () -> Unit
) {
    val themeState = LocalDynamicThemeState.current
    val dynamicColoring = LocalDynamicColoringSwitch.current
    val context = LocalContext.current
    val canUseDynamicColor = dynamicColoring && isDynamicColoringSupported()

    val dynamicColorScheme = if (canUseDynamicColor) {
        if (themeState.isDark) {
            dynamicDarkColorScheme(context).let {
                if (themeState.isAmoled) it.copy(
                    surface = Color.Black,
                    background = Color.Black
                ) else it
            }
        } else {
            dynamicLightColorScheme(context)
        }
    } else null

    ProvideTextStyle(
        value = LocalTextStyle.current.copy(
            lineBreak = LineBreak.Paragraph,
            textDirection = TextDirection.Content
        )
    ) {
        if (dynamicColorScheme != null) {
            MaterialTheme(colorScheme = dynamicColorScheme, shapes = AppShapes, content = content)
        } else {
            DynamicMaterialTheme(
                state = themeState,
                animate = true,
                shapes = AppShapes,
                content = content
            )
        }
    }
}