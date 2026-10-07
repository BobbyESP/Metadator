/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.feature.editor.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.bobbyesp.metadator.core.common.formatDuration
import com.bobbyesp.metadator.core.common.formatFileSize
import com.bobbyesp.metadator.core.designsystem.theme.Spacing
import com.bobbyesp.metadator.core.domain.editor.LoadedTrack
import com.bobbyesp.metadator.feature.editor.R
import java.util.Locale

/** What the file is: name, format, size and the audio stream. Read-only. */
@Composable
fun FileInfoCard(loaded: LoadedTrack, modifier: Modifier = Modifier) {
    val audio = loaded.audio
    val rows = buildList {
        add(R.string.file_name to (loaded.track?.displayName ?: loaded.file?.displayName.orEmpty()))
        loaded.fileExtension?.let { add(R.string.file_format to it.uppercase(Locale.ROOT)) }
        (loaded.track?.sizeBytes ?: loaded.file?.sizeBytes)
            ?.takeIf { it > 0 }
            ?.let {
                add(R.string.file_size to formatFileSize(it))
            }
        (audio?.durationMs ?: loaded.track?.durationMs)
            ?.takeIf { it > 0 }
            ?.let {
                add(R.string.file_duration to formatDuration(it))
            }
        audio
            ?.bitrateKbps
            ?.takeIf { it > 0 }
            ?.let { add(R.string.file_bitrate to stringResource(R.string.file_bitrate_value, it)) }
        audio
            ?.sampleRateHz
            ?.takeIf { it > 0 }
            ?.let {
                add(
                    R.string.file_sample_rate to
                        stringResource(
                            R.string.file_sample_rate_value,
                            (it / 1000.0).toString().removeSuffix(".0"),
                        )
                )
            }
        audio?.channels?.takeIf { it > 0 }?.let { add(R.string.file_channels to it.toString()) }
        (loaded.track?.folder ?: loaded.file?.location)?.let { add(R.string.file_location to it) }
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier.padding(Spacing.large),
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            rows.forEach { (label, value) ->
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.large)) {
                    Text(
                        stringResource(label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        value,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
