package com.bobbyesp.ui.components.others

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bobbyesp.ui.R

@Composable
fun AdditionalInformation(modifier: Modifier = Modifier, text: AnnotatedString) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
            imageVector = Icons.Rounded.Info,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            contentDescription = stringResource(id = R.string.additional_information),
        )

        Text(text = text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun AdditionalInformation(
    modifier: Modifier = Modifier,
    text: String,
    fontFamily: FontFamily = FontFamily.Default,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
            modifier = Modifier.size(32.dp),
            imageVector = Icons.Rounded.Info,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            contentDescription = stringResource(id = R.string.additional_information),
        )

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = fontFamily,
        )
    }
}

@Preview
@Composable
private fun Preview() {
    MaterialTheme {
        AdditionalInformation(
            text =
                "This is a preview text preview text preview text preview text preview text preview text " +
                    "preview text preview text"
        )
    }
}
