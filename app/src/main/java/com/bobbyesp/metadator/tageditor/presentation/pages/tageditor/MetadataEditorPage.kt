package com.bobbyesp.metadator.tageditor.presentation.pages.tageditor

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bobbyesp.metadator.R
import com.bobbyesp.metadator.core.domain.model.ParcelableSong
import com.bobbyesp.metadator.core.presentation.common.LocalNavController
import com.bobbyesp.metadator.core.presentation.common.LocalSonner
import com.bobbyesp.metadator.core.presentation.components.image.AsyncImage
import com.bobbyesp.metadator.tageditor.presentation.components.textfield.MetadataOutlinedTextField
import com.bobbyesp.ui.common.pages.ErrorPage
import com.bobbyesp.ui.common.pages.LoadingPage
import com.bobbyesp.ui.components.button.CloseButton
import com.bobbyesp.ui.components.others.MetadataTag
import com.bobbyesp.ui.components.text.LargeCategoryTitle
import com.bobbyesp.utilities.ext.fromMillisToMinutes
import com.bobbyesp.utilities.ext.isNeitherNullNorBlank
import com.bobbyesp.utilities.states.ResourceState
import com.bobbyesp.utilities.states.ScreenState
import com.dokar.sonner.ToastType
import com.kyant.taglib.AudioProperties
import com.materialkolor.ktx.harmonize
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetadataEditorPage(
    pageState: MetadataEditorViewModel.PageViewState,
    receivedAudio: ParcelableSong,
    onEvent: (MetadataEditorViewModel.Event) -> Unit,
) {
    val navController = LocalNavController.current
    val sonner = LocalSonner.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(receivedAudio) {
        onEvent(MetadataEditorViewModel.Event.LoadMetadata(receivedAudio.localPath))
    }

    var newArtworkAddress by rememberSaveable { mutableStateOf<Uri?>(null) }
    var showInstallSongSyncDialog by remember { mutableStateOf(false) }

    val singleImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> newArtworkAddress = uri }
    )

    val lyricsActivityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val receivedLyrics = result.data?.getStringExtra("lyrics")
                if (receivedLyrics.isNeitherNullNorBlank()) {
                    onEvent(MetadataEditorViewModel.Event.UpdateProperty("LYRICS", receivedLyrics!!))
                    scope.launch {
                        sonner.show(
                            message = context.getString(R.string.lyrics_received),
                            type = ToastType.Success
                        )
                    }
                } else {
                    scope.launch {
                        sonner.show(
                            message = context.getString(R.string.empty_lyrics_received),
                            type = ToastType.Error
                        )
                    }
                }
            }
            Activity.RESULT_CANCELED -> scope.launch {
                sonner.show(
                    message = context.getString(R.string.lyrics_retrieve_cancelled),
                    type = ToastType.Info
                )
            }
            else -> scope.launch {
                sonner.show(
                    message = context.getString(R.string.something_unexpected_occurred),
                    type = ToastType.Error
                )
            }
        }
    }

    val lyricsRetrieveIntent = Intent("android.intent.action.SEND").apply {
        putExtra("songName", pageState.properties["TITLE"])
        putExtra("artistName", pageState.properties["ARTIST"])
        type = "text/plain"
        setPackage("pl.lambada.songsync")
    }

    fun launchLyricsRetrieveIntent() {
        try {
            lyricsActivityLauncher.launch(lyricsRetrieveIntent)
        } catch (e: Exception) {
            if (e is ActivityNotFoundException) {
                showInstallSongSyncDialog = true
            } else {
                scope.launch {
                    sonner.show(
                        message = context.getString(R.string.something_unexpected_occurred),
                        type = ToastType.Error
                    )
                }
            }
        }
    }

    if (showInstallSongSyncDialog) {
        SongSyncNeededDialog(onDismissRequest = { showInstallSongSyncDialog = false })
    }

    Scaffold(
        topBar = {
            EditorTopBar(
                onClose = { navController.popBackStack() },
                onSave = {
                    onEvent(
                        MetadataEditorViewModel.Event.SaveAll(
                            receivedAudio.localPath,
                            listOf(newArtworkAddress ?: receivedAudio.artworkPath ?: Uri.EMPTY)
                        )
                    )
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            EditorContent(
                pageState = pageState,
                artworkUri = newArtworkAddress ?: receivedAudio.artworkPath,
                onEditArtwork = {
                    singleImagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onRetrieveLyrics = { launchLyricsRetrieveIntent() },
                onEvent = onEvent
            )
        }
    }
}

/** Composable para la TopAppBar del editor */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTopBar(
    onClose: () -> Unit,
    onSave: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(id = R.string.viewing_metadata),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp)
            )
        },
        navigationIcon = { CloseButton(onClick = onClose) },
        actions = {
            TextButton(onClick = onSave) {
                Text(text = stringResource(id = R.string.save))
            }
        },
        scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    )
}

/** Composable que muestra el contenido principal según el estado de la página */
@Composable
private fun EditorContent(
    pageState: MetadataEditorViewModel.PageViewState,
    artworkUri: Uri?,
    onEditArtwork: () -> Unit,
    onRetrieveLyrics: () -> Unit,
    onEvent: (MetadataEditorViewModel.Event) -> Unit
) {
    val scrollState = rememberScrollState()
    val configuration = LocalConfiguration.current

    Crossfade(
        targetState = pageState.pageState,
        animationSpec = tween(175)
    ) { state ->
        when (state) {
            is ScreenState.Error -> ErrorPage(
                modifier = Modifier.fillMaxSize(),
                throwable = state.exception
            ) {
                // Acción de reintento, según convenga
            }
            ScreenState.Loading -> LoadingPage(
                modifier = Modifier.fillMaxSize(),
                text = stringResource(id = R.string.loading_metadata)
            )
            is ScreenState.Success -> {
                if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    PortraitContent(
                        pageState = pageState,
                        artworkUri = artworkUri,
                        onEditArtwork = onEditArtwork,
                        onRetrieveLyrics = onRetrieveLyrics,
                        scrollState = scrollState,
                        onEvent = onEvent
                    )
                } else {
                    LandscapeContent(
                        pageState = pageState,
                        artworkUri = artworkUri,
                        onEditArtwork = onEditArtwork,
                        onRetrieveLyrics = onRetrieveLyrics,
                        scrollState = scrollState,
                        onEvent = onEvent
                    )
                }
            }
        }
    }
}

/** Contenido en orientación vertical */
@Composable
private fun PortraitContent(
    pageState: MetadataEditorViewModel.PageViewState,
    artworkUri: Uri?,
    onEditArtwork: () -> Unit,
    onRetrieveLyrics: () -> Unit,
    scrollState: ScrollState,
    onEvent: (MetadataEditorViewModel.Event) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
    ) {
        ArtworkSection(
            artworkUri = artworkUri,
            onEditArtwork = onEditArtwork,
            modifier = Modifier
                .size(250.dp)
                .padding(8.dp)
                .align(Alignment.CenterHorizontally)
        )
        if (pageState.audioProperties is ResourceState.Success && pageState.audioProperties.data != null) {
            AudioPropertiesSection(audioProperties = pageState.audioProperties.data!!)
        }
        if (pageState.metadata is ResourceState.Success) {
            SongPropertiesSection(
                properties = pageState.properties,
                modifiedKeys = pageState.modifiedKeys,
                onUpdateProperty = { key, value ->
                    onEvent(MetadataEditorViewModel.Event.UpdateProperty(key, value))
                },
                onRetrieveLyrics = onRetrieveLyrics
            )
        }
    }
}

/** Contenido en orientación horizontal */
@Composable
private fun LandscapeContent(
    pageState: MetadataEditorViewModel.PageViewState,
    artworkUri: Uri?,
    onEditArtwork: () -> Unit,
    onRetrieveLyrics: () -> Unit,
    scrollState: ScrollState,
    onEvent: (MetadataEditorViewModel.Event) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ArtworkSection(
            artworkUri = artworkUri,
            onEditArtwork = onEditArtwork,
            modifier = Modifier
                .padding(8.dp)
                .aspectRatio(1f)
                .align(Alignment.CenterVertically)
        )
        Column(
            modifier = Modifier
                .weight(0.5f)
                .verticalScroll(scrollState)
        ) {
            if (pageState.audioProperties is ResourceState.Success && pageState.audioProperties.data != null) {
                AudioPropertiesSection(audioProperties = pageState.audioProperties.data!!)
            }
            if (pageState.metadata is ResourceState.Success) {
                SongPropertiesSection(
                    properties = pageState.properties,
                    modifiedKeys = pageState.modifiedKeys,
                    onUpdateProperty = { key, value ->
                        onEvent(MetadataEditorViewModel.Event.UpdateProperty(key, value))
                    },
                    onRetrieveLyrics = onRetrieveLyrics
                )
            }
        }
    }
}

/** Sección para mostrar y editar la imagen de portada */
@Composable
private fun ArtworkSection(
    artworkUri: Uri?,
    onEditArtwork: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        AsyncImage(
            modifier = Modifier
                .fillMaxSize()
                .clip(MaterialTheme.shapes.small),
            imageModel = artworkUri
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            IconButton(
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.5f)
                ),
                onClick = onEditArtwork
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    tint = Color.White.harmonize(Color.Black.copy(alpha = 0.5f)),
                    contentDescription = stringResource(id = R.string.edit_image)
                )
            }
        }
    }
}

@Composable
private fun AudioPropertiesSection(
    audioProperties: AudioProperties
) {
    LargeCategoryTitle(
        modifier = Modifier.padding(vertical = 6.dp),
        text = stringResource(id = R.string.audio_details)
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            MetadataTag(
                modifier = Modifier.weight(0.5f),
                typeOfMetadata = stringResource(id = R.string.bitrate),
                metadata = "${audioProperties.bitrate} kbps"
            )
            MetadataTag(
                modifier = Modifier.weight(0.5f),
                typeOfMetadata = stringResource(id = R.string.sample_rate),
                metadata = "${audioProperties.sampleRate} Hz"
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            MetadataTag(
                modifier = Modifier.weight(0.5f),
                typeOfMetadata = stringResource(id = R.string.channels),
                metadata = audioProperties.channels.toString(),
            )
            MetadataTag(
                modifier = Modifier.weight(0.5f),
                typeOfMetadata = stringResource(id = R.string.duration),
                metadata = audioProperties.length.fromMillisToMinutes()
            )
        }
    }
}

/** Sección para mostrar y editar las propiedades de la canción */
@Composable
private fun SongPropertiesSection(
    properties: Map<String, String>,
    modifiedKeys: Set<String>,
    onUpdateProperty: (String, String) -> Unit,
    onRetrieveLyrics: () -> Unit,
) {
    SectionHeader(title = stringResource(id = R.string.general_tags))
    MetadataField(
        key = "TITLE",
        properties = properties,
        modifiedKeys = modifiedKeys,
        label = stringResource(id = R.string.title),
        onUpdateProperty = onUpdateProperty,
        modifier = Modifier.fillMaxWidth()
    )
    MetadataField(
        key = "ARTIST",
        properties = properties,
        modifiedKeys = modifiedKeys,
        label = stringResource(id = R.string.artist),
        onUpdateProperty = onUpdateProperty,
        modifier = Modifier.fillMaxWidth()
    )
    MetadataField(
        key = "ALBUM",
        properties = properties,
        modifiedKeys = modifiedKeys,
        label = stringResource(id = R.string.album),
        onUpdateProperty = onUpdateProperty,
        modifier = Modifier.fillMaxWidth()
    )
    MetadataField(
        key = "ALBUMARTIST",
        properties = properties,
        modifiedKeys = modifiedKeys,
        label = stringResource(id = R.string.album_artist),
        onUpdateProperty = onUpdateProperty,
        modifier = Modifier.fillMaxWidth()
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MetadataField(
            key = "TRACKNUMBER",
            properties = properties,
            modifiedKeys = modifiedKeys,
            label = stringResource(id = R.string.track_number),
            onUpdateProperty = onUpdateProperty,
            modifier = Modifier.weight(0.5f)
        )
        MetadataField(
            key = "DISCNUMBER",
            properties = properties,
            modifiedKeys = modifiedKeys,
            label = stringResource(id = R.string.disc_number),
            onUpdateProperty = onUpdateProperty,
            modifier = Modifier.weight(0.5f)
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MetadataField(
            key = "DATE",
            properties = properties,
            modifiedKeys = modifiedKeys,
            label = stringResource(id = R.string.date),
            onUpdateProperty = onUpdateProperty,
            modifier = Modifier.weight(0.5f)
        )
        MetadataField(
            key = "GENRE",
            properties = properties,
            modifiedKeys = modifiedKeys,
            label = stringResource(id = R.string.genre),
            onUpdateProperty = onUpdateProperty,
            modifier = Modifier.weight(0.5f)
        )
    }
    SectionHeader(title = stringResource(id = R.string.credits))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MetadataField(
            key = "COMPOSER",
            properties = properties,
            modifiedKeys = modifiedKeys,
            label = stringResource(id = R.string.composer),
            onUpdateProperty = onUpdateProperty,
            modifier = Modifier.weight(0.5f)
        )
        MetadataField(
            key = "LYRICIST",
            properties = properties,
            modifiedKeys = modifiedKeys,
            label = stringResource(id = R.string.lyricist),
            onUpdateProperty = onUpdateProperty,
            modifier = Modifier.weight(0.5f)
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MetadataField(
            key = "CONDUCTOR",
            properties = properties,
            modifiedKeys = modifiedKeys,
            label = stringResource(id = R.string.conductor),
            onUpdateProperty = onUpdateProperty,
            modifier = Modifier.weight(0.5f)
        )
        MetadataField(
            key = "REMIXER",
            properties = properties,
            modifiedKeys = modifiedKeys,
            label = stringResource(id = R.string.remixer),
            onUpdateProperty = onUpdateProperty,
            modifier = Modifier.weight(0.5f)
        )
    }
    MetadataField(
        key = "PERFORMER",
        properties = properties,
        modifiedKeys = modifiedKeys,
        label = stringResource(id = R.string.performer),
        onUpdateProperty = onUpdateProperty,
        modifier = Modifier.fillMaxWidth()
    )
    SectionHeader(title = stringResource(id = R.string.others))
    MetadataField(
        key = "COMMENT",
        properties = properties,
        modifiedKeys = modifiedKeys,
        label = stringResource(id = R.string.comment),
        onUpdateProperty = onUpdateProperty,
        modifier = Modifier.fillMaxWidth(),
        maxLines = 3
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        TextButton(
            modifier = Modifier.align(Alignment.End),
            onClick = onRetrieveLyrics
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lyrics,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Text(text = stringResource(id = R.string.retrieve_lyrics))
            }
        }
        MetadataField(
            key = "LYRICS",
            properties = properties,
            modifiedKeys = modifiedKeys,
            label = stringResource(id = R.string.lyrics),
            onUpdateProperty = onUpdateProperty,
            modifier = Modifier.fillMaxWidth(),
            maxLines = 20
        )
    }
}

/** Encabezado de sección */
@Composable
private fun SectionHeader(title: String) {
    LargeCategoryTitle(
        modifier = Modifier.padding(vertical = 12.dp),
        text = title
    )
}

/** Campo editable de metadata */
@Composable
private fun MetadataField(
    key: String,
    properties: Map<String, String>,
    modifiedKeys: Set<String>,
    label: String,
    onUpdateProperty: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    maxLines: Int = 1
) {
    val value = properties[key] ?: ""
    val isModified = key in modifiedKeys

    MetadataOutlinedTextField(
        value = value,
        label = label,
        isModified = isModified,
        modifier = modifier,
        maxLines = maxLines,
        onValueChange = { onUpdateProperty(key, it) }
    )
}
