package com.bobbyesp.metadator.onboarding.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OnboardingScreenHeader(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary)
                .padding(32.dp)
        )

        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.headlineLarge,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 5.sp
        )
    }

}

@Preview
@Composable
private fun Preview() {
    MaterialTheme {
        OnboardingScreenHeader(
            modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp),
            title = "Permissions",
            icon = Icons.Rounded.Security
        )
    }

}