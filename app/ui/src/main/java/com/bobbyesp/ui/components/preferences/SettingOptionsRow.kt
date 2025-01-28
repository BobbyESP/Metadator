package com.bobbyesp.ui.components.preferences

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun <T : SettingOption> SettingOptionsRow(
    title: String,
    options: List<T>,
    modifier: Modifier = Modifier,
    optionContent: @Composable (T) -> Unit
) {
    Column(
        modifier = modifier
            .clip(ShapeDefaults.ExtraLarge)
            .background(color = MaterialTheme.colorScheme.surfaceContainer)
            .padding(vertical = 16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 20.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow {
            item {
                Spacer(modifier = Modifier.width(8.dp))
            }
            items(
                items = options,
                key = { it.title }
            ) { option ->
                optionContent(option)
            }
            item {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}