package com.bobbyesp.metadator.core.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bobbyesp.metadator.R
import com.bobbyesp.metadator.core.util.getAppVersionName
import com.bobbyesp.metadator.presentation.theme.MetadatorLogoBackground
import com.bobbyesp.metadator.presentation.theme.MetadatorLogoForeground

@Composable
fun AppDetails(
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    val context = LocalContext.current
    val isDarkMode = isSystemInDarkTheme()

    val animatedColor by animateColorAsState(
        targetValue = if(isDarkMode) MetadatorLogoForeground else MetadatorLogoBackground
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.metadator_logo_foreground),
            colorFilter = ColorFilter.tint(animatedColor),
            contentDescription = null,
            modifier = Modifier
                .size(200.dp)
                .graphicsLayer {
                    scaleX = 2f
                    scaleY = 2f
                }
        )

        Text(
            text = stringResource(R.string.app_name).uppercase(),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )

        Text(
            text = subtitle ?: context.getAppVersionName(),
            color = MaterialTheme.colorScheme.outline,
            fontFamily = FontFamily.Monospace
        )
    }
}