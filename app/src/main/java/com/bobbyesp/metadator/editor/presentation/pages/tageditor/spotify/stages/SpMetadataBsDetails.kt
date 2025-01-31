package com.bobbyesp.metadator.editor.presentation.pages.tageditor.spotify.stages

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamratzman.spotify.models.Track
import com.bobbyesp.metadator.R
import com.bobbyesp.metadator.core.ext.TagLib.toImageVector
import com.bobbyesp.metadator.core.ext.TagLib.toLocalizedName
import com.bobbyesp.metadator.core.ext.format
import com.bobbyesp.metadator.core.ext.formatArtists
import com.bobbyesp.metadator.editor.presentation.pages.tageditor.spotify.MetadataBottomSheetViewModel
import com.bobbyesp.metadator.core.presentation.components.image.AsyncImage
import com.bobbyesp.metadator.core.presentation.components.text.ConditionedMarqueeText
import com.bobbyesp.ui.components.button.BackButton
import com.bobbyesp.ui.components.others.SelectableSurface
import com.bobbyesp.ui.util.rememberVolatileSaveable

@Composable
fun SpMetadataBsDetails(
    modifier: Modifier = Modifier,
    pageViewState: State<MetadataBottomSheetViewModel.ViewState>,
    onEvent: (MetadataBottomSheetViewModel.Event) -> Unit,
    onCloseSheet: () -> Unit,
) {
    BackHandler {
        onEvent(MetadataBottomSheetViewModel.Event.SelectTrack(null))
    }

    val chosenMetadata = rememberSaveable(key = "chosenMetadata") {
        mutableMapOf<String, String>()
    }

    val lazyGirdState = rememberLazyGridState()

    Column(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackButton(
                onClick = {
                    onEvent(MetadataBottomSheetViewModel.Event.SelectTrack(null))
                }
            )
            Text(
                text = stringResource(id = R.string.spotify_metadata),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                ),
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(
                onClick = {
                    onEvent(MetadataBottomSheetViewModel.Event.UpdateMetadataFields(chosenMetadata.toMap()))
                    onCloseSheet()
                }
            ) {
                Text(text = stringResource(id = R.string.save))
            }
        }
        pageViewState.value.selectedTrack?.let { track ->
            val context = LocalContext.current
            val metadataMap = createMetadataMap(context, track)

            TrackInfo(
                modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
                track = track
            )

            LazyVerticalGrid(
                state = lazyGirdState,
                columns = GridCells.Adaptive(200.dp),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(8.dp)
            ) {
                val metadataList = metadataMap.toList()
                val isTheListOdd = metadataList.size % 2 != 0

                items(metadataList.size) { index ->
                    // Skip the last item if the size of metadataMap is odd
                    if (isTheListOdd && index == metadataList.size - 1) {
                        return@items
                    }

                    val (field, retrievedValue) = metadataList[index]
                    SelectableMetadataField(
                        modifier = Modifier
                            .padding(4.dp)
                            .heightIn(min = 100.dp),
                        title = field,
                        value = retrievedValue,
                        onSelectMetadata = { title, value -> chosenMetadata[title] = value },
                        onDeleteMetadata = { title -> chosenMetadata.remove(title) }
                    )
                }

                if (isTheListOdd) {
                    val lastElement = metadataList.last()
                    val (field, retrievedValue) = lastElement
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SelectableMetadataField(
                            modifier = Modifier
                                .padding(4.dp)
                                .heightIn(min = 100.dp),
                            title = field,
                            value = retrievedValue,
                            onSelectMetadata = { title, value -> chosenMetadata[title] = value },
                            onDeleteMetadata = { title -> chosenMetadata.remove(title) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun createMetadataMap(context: Context, track: Track) = rememberSaveable {
    mutableMapOf(
        "TITLE" to track.name,
        "ARTIST" to track.artists.formatArtists(),
        "ALBUM" to track.album.name,
        "ALBUMARTIST" to track.album.artists.formatArtists(),
        "TRACKNUMBER" to track.trackNumber.toString(),
        "DISCNUMBER" to track.discNumber.toString(),
        "DATE" to (track.album.releaseDate?.format(track.album.releaseDatePrecisionString)
            ?: context.getString(R.string.unknown)),
    )
}

@Composable
private fun TrackInfo(
    modifier: Modifier = Modifier,
    track: Track,
) {
    val albumArtPath = track.album.images?.getOrNull(0)?.url

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            modifier = Modifier.size(64.dp),
            imageModel = albumArtPath,
            shape = MaterialTheme.shapes.extraSmall
        )
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier
                    .padding(8.dp)
                    .weight(1f)
            ) {
                ConditionedMarqueeText(
                    text = track.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = track.artists.formatArtists(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SelectableMetadataField(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    useIcon: Boolean = true,
    onSelectMetadata: (title: String, value: String) -> Unit,
    onDeleteMetadata: (title: String) -> Unit
) {
    var isSelected by rememberVolatileSaveable(initialValue = false)
    SelectableSurface(
        modifier = modifier,
        isSelected = isSelected,
        tonalElevation = 2.dp,
        borderStroke = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        onSelected = {
            isSelected = !isSelected
            if (isSelected) onSelectMetadata(title, value) else onDeleteMetadata(title)
        },
        shape = MaterialTheme.shapes.medium,
    ) {
        Box {
            Column(
                modifier = Modifier
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Top),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    modifier = Modifier,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start)
                ) {
                    if (useIcon) {
                        Icon(imageVector = title.toImageVector(), contentDescription = null)
                    }
                    Text(
                        text = title.toLocalizedName(),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            RadioButton(
                selected = isSelected,
                onClick = {
                    isSelected = !isSelected
                    if (isSelected) onSelectMetadata(title, value) else onDeleteMetadata(title)
                },
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

@Preview
@Composable
fun SelectableMetadataFieldNoIconPreview() {
    SelectableMetadataField(
        modifier = Modifier.size(200.dp),
        title = "TITLE",
        value = "Title",
        onSelectMetadata = { _, _ -> },
        onDeleteMetadata = { },
        useIcon = false
    )
}

@Preview
@Composable
fun SelectableMetadataFieldWithIconPreview() {
    SelectableMetadataField(
        modifier = Modifier.size(200.dp),
        title = "TITLE",
        value = "Title",
        onSelectMetadata = { _, _ -> },
        onDeleteMetadata = { },
        useIcon = true
    )
}