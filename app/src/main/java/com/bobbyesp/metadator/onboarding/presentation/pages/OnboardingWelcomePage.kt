package com.bobbyesp.metadator.onboarding.presentation.pages

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bobbyesp.metadator.R
import com.bobbyesp.metadator.core.presentation.AppDetails

@Composable
fun OnboardingWelcomePage(
    onGetStarted: () -> Unit,
    subtitle: String? = null
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            Button(
                modifier = Modifier
                    .padding(16.dp),
                onClick = onGetStarted,
            ) {
                Text(text = stringResource(id = R.string.get_started))
                Icon(
                    modifier = Modifier,
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = stringResource(id = R.string.get_started)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            AppDetails(
                modifier = Modifier.align(Alignment.Center), subtitle = subtitle
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun OnboardingPreview() {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    ) {
        OnboardingWelcomePage(
            subtitle = "1.0.0-beta09",
            onGetStarted = {}
        )
    }
}