package com.bobbyesp.metadator.editor.presentation.pages.tageditor

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bobbyesp.metadator.R
import com.bobbyesp.metadator.core.presentation.common.LocalNavController
import com.bobbyesp.metadator.core.presentation.common.LocalOrientation
import com.bobbyesp.metadator.core.presentation.common.LocalSonner
import com.bobbyesp.metadator.core.domain.model.ParcelableSong
import com.bobbyesp.metadator.core.presentation.components.image.AsyncImage
import com.bobbyesp.metadator.editor.presentation.pages.tageditor.spotify.MetadataBottomSheetViewModel
import com.bobbyesp.metadator.editor.presentation.pages.tageditor.spotify.SpMetadataBottomSheetContent
import com.bobbyesp.ui.common.pages.ErrorPage
import com.bobbyesp.ui.common.pages.LoadingPage
import com.bobbyesp.ui.components.button.CloseButton
import com.bobbyesp.ui.components.others.MetadataTag
import com.bobbyesp.ui.components.text.LargeCategoryTitle
import com.bobbyesp.ui.components.text.PreConfiguredOutlinedTextField
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
    state: State<MetadataEditorViewModel.PageViewState>,
    bsViewState: State<MetadataBottomSheetViewModel.ViewState>,
    receivedAudio: ParcelableSong,
    onBsEvent: (MetadataBottomSheetViewModel.Event) -> Unit,
    onEvent: (MetadataEditorViewModel.Event) -> Unit
) {
    val navController = LocalNavController.current
    val sonner = LocalSonner.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pageState = state.value

    LaunchedEffect(receivedAudio) {
        onEvent(MetadataEditorViewModel.Event.LoadMetadata(receivedAudio.localPath))
    }

    var newArtworkAddress by rememberSaveable { mutableStateOf<Uri?>(null) }

    val singleImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> newArtworkAddress = uri }
    )

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.Hidden,
            skipHiddenState = false
        )
    )

    var showInstallSongSyncDialog by remember {
        mutableStateOf(false)
    }

    val lyricsActivityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val receivedLyrics = result.data?.getStringExtra("lyrics")
                if (receivedLyrics.isNeitherNullNorBlank()) {
                    pageState.mutablePropertiesMap["LYRICS"] = receivedLyrics!!
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

            Activity.RESULT_CANCELED -> {
                scope.launch {
                    sonner.show(
                        message = context.getString(R.string.lyrics_retrieve_cancelled),
                        type = ToastType.Info
                    )
                }
            }

            else -> {
                scope.launch {
                    sonner.show(
                        message = context.getString(R.string.something_unexpected_occurred),
                        type = ToastType.Error
                    )
                }
            }
        }
    }

    val lyricsRetrieveIntent = Intent("android.intent.action.SEND").apply {
        putExtra("songName", pageState.mutablePropertiesMap["TITLE"])
        putExtra("artistName", pageState.mutablePropertiesMap["ARTIST"])
        type = "text/plain"
        setPackage("pl.lambada.songsync")
    }

    fun launchLyricsRetrieveIntent() {
        try {
            lyricsActivityLauncher.launch(lyricsRetrieveIntent)
        } catch (e: Exception) {
            when (e) {
                is ActivityNotFoundException -> showInstallSongSyncDialog = true
                else -> scope.launch {
                    sonner.show(
                        message = context.getString(R.string.something_unexpected_occurred),
                        type = ToastType.Error
                    )
                }
            }
        }
    }

    if (showInstallSongSyncDialog) {
        SongSyncNeededDialog(
            onDismissRequest = { showInstallSongSyncDialog = false }
        )
    }

    BottomSheetScaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(id = R.string.viewing_metadata),
                            style = MaterialTheme.typography.bodyLarge,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = { CloseButton { navController.popBackStack() } },
                actions = {
                    IconButton(
                        onClick = {
                            if (scaffoldState.bottomSheetState.isVisible) {
                                onBsEvent(MetadataBottomSheetViewModel.Event.Search(receivedAudio.name + " " + receivedAudio.mainArtist))
                            } else {
                                scope.launch { scaffoldState.bottomSheetState.partialExpand() }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Downloading,
                            contentDescription = stringResource(id = R.string.retrieve_song_info)
                        )
                    }
                    TextButton(
                        onClick = {
                            onEvent(
                                MetadataEditorViewModel.Event.SaveAll(
                                    receivedAudio.localPath,
                                    listOf(
                                        newArtworkAddress ?: receivedAudio.artworkPath ?: Uri.EMPTY
                                    )
                                )
                            )
                        }
                    ) {
                        Text(text = stringResource(id = R.string.save))
                    }
                },
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
            )
        },
        modifier = Modifier.fillMaxSize(),
        scaffoldState = scaffoldState,
        sheetPeekHeight = 148.dp,
        sheetShadowElevation = 8.dp,
        sheetContent = {
            SpMetadataBottomSheetContent(
                name = receivedAudio.name,
                artist = receivedAudio.mainArtist,
                sheetState = scaffoldState.bottomSheetState,
                bsViewState = bsViewState,
                onEvent = onBsEvent,
            ) {
                scope.launch { scaffoldState.bottomSheetState.hide() }
            }
        },
    ) { innerPadding ->
        val animatedBottomPadding by animateDpAsState(
            targetValue = if (scaffoldState.bottomSheetState.isVisible) innerPadding.calculateBottomPadding() + 6.dp else 0.dp,
            label = "animatedBottomPadding"
        )

        Crossfade(
            targetState = pageState.pageState,
            animationSpec = tween(175),
            label = "Fade between pages (ID3MetadataEditorPage)",
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) { state ->
            when (state) {
                is ScreenState.Error -> ErrorPage(
                    modifier = Modifier.fillMaxSize(),
                    throwable = state.exception
                ) {
                    onEvent(MetadataEditorViewModel.Event.LoadMetadata(receivedAudio.localPath))
                }

                ScreenState.Loading -> LoadingPage(
                    modifier = Modifier.fillMaxSize(),
                    text = stringResource(id = R.string.loading_metadata)
                )

                is ScreenState.Success -> {
                    val scrollState = rememberScrollState()
                    val orientation = LocalOrientation.current
                    val artworkUri = newArtworkAddress ?: receivedAudio.artworkPath

                    when (orientation) {
                        Configuration.ORIENTATION_PORTRAIT -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(scrollState)
                                    .padding(horizontal = 16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(250.dp)
                                        .padding(8.dp)
                                        .aspectRatio(1f)
                                        .align(Alignment.CenterHorizontally),
                                ) {
                                    AsyncImage(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(MaterialTheme.shapes.small)
                                            .align(Alignment.Center),
                                        imageModel = artworkUri,
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
                                            onClick = {
                                                singleImagePickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Edit,
                                                tint = Color.White.harmonize(Color.Black.copy(alpha = 0.5f)),
                                                contentDescription = stringResource(id = R.string.edit_image)
                                            )
                                        }
                                    }
                                }

                                if (pageState.audioProperties is ResourceState.Success && pageState.audioProperties.data != null) {
                                    AudioProperties(
                                        modifier = Modifier,
                                        audioProperties = pageState.audioProperties.data!!
                                    )
                                }

                                if (pageState.metadata is ResourceState.Success) {
                                    SongProperties(
                                        mutablePropertiesMap = pageState.mutablePropertiesMap,
                                        retrieveLyrics = { launchLyricsRetrieveIntent() }
                                    )
                                    Spacer(modifier = Modifier.height(animatedBottomPadding))
                                }
                            }
                        }

                        Configuration.ORIENTATION_LANDSCAPE -> {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .aspectRatio(1f)
                                        .align(Alignment.CenterVertically),
                                ) {
                                    AsyncImage(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(MaterialTheme.shapes.small)
                                            .align(Alignment.Center),
                                        imageModel = artworkUri,
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
                                            onClick = {
                                                singleImagePickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Edit,
                                                tint = Color.White.harmonize(Color.Black.copy(alpha = 0.5f)),
                                                contentDescription = stringResource(id = R.string.edit_image)
                                            )
                                        }
                                    }
                                }
                                Column(
                                    modifier = Modifier
                                        .weight(0.5f)
                                        .verticalScroll(scrollState)
                                ) {
                                    if (pageState.audioProperties is ResourceState.Success && pageState.audioProperties.data != null) {
                                        AudioProperties(
                                            modifier = Modifier,
                                            audioProperties = pageState.audioProperties.data!!
                                        )
                                    }
                                    if (pageState.metadata is ResourceState.Success) {
                                        SongProperties(
                                            mutablePropertiesMap = pageState.mutablePropertiesMap,
                                            retrieveLyrics = { launchLyricsRetrieveIntent() }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioProperties(modifier: Modifier = Modifier, audioProperties: AudioProperties) {
    LargeCategoryTitle(
        modifier = Modifier.padding(vertical = 6.dp),
        text = stringResource(id = R.string.audio_details)
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        ) {
            MetadataTag(
                modifier = Modifier.weight(0.5f),
                typeOfMetadata = stringResource(id = R.string.bitrate),
                metadata = audioProperties.bitrate.toString() + " kbps"
            )
            MetadataTag(
                modifier = Modifier.weight(0.5f),
                typeOfMetadata = stringResource(id = R.string.sample_rate),
                metadata = audioProperties.sampleRate.toString() + " Hz"
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        ) {
            MetadataTag(
                modifier = Modifier.weight(0.5f),
                typeOfMetadata = stringResource(id = R.string.channels),
                metadata = audioProperties.channels.toString()
            )
            MetadataTag(
                modifier = Modifier.weight(0.5f),
                typeOfMetadata = stringResource(id = R.string.duration),
                metadata = audioProperties.length.fromMillisToMinutes()
            )
        }
    }
}

@Composable
private fun SongProperties(
    mutablePropertiesMap: SnapshotStateMap<String, String>,
    retrieveLyrics: () -> Unit
) {
    LargeCategoryTitle(
        modifier = Modifier.padding(vertical = 6.dp),
        text = stringResource(id = R.string.general_tags)
    )

    PreConfiguredOutlinedTextField(
        value = mutablePropertiesMap["TITLE"],
        label = stringResource(id = R.string.title),
        modifier = Modifier.fillMaxWidth()
    ) { title -> mutablePropertiesMap["TITLE"] = title }

    PreConfiguredOutlinedTextField(
        value = mutablePropertiesMap["ARTIST"],
        label = stringResource(id = R.string.artist),
        modifier = Modifier.fillMaxWidth()
    ) { artists -> mutablePropertiesMap["ARTIST"] = artists }

    PreConfiguredOutlinedTextField(
        value = mutablePropertiesMap["ALBUM"],
        label = stringResource(id = R.string.album),
        modifier = Modifier.fillMaxWidth()
    ) { album -> mutablePropertiesMap["ALBUM"] = album }

    PreConfiguredOutlinedTextField(
        value = mutablePropertiesMap["ALBUMARTIST"],
        label = stringResource(id = R.string.album_artist),
        modifier = Modifier.fillMaxWidth()
    ) { albumArtist -> mutablePropertiesMap["ALBUMARTIST"] = albumArtist }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PreConfiguredOutlinedTextField(
                value = mutablePropertiesMap["TRACKNUMBER"],
                label = stringResource(id = R.string.track_number),
                modifier = Modifier.weight(0.5f)
            ) { trackNumber -> mutablePropertiesMap["TRACKNUMBER"] = trackNumber }
            PreConfiguredOutlinedTextField(
                value = mutablePropertiesMap["DISCNUMBER"],
                label = stringResource(id = R.string.disc_number),
                modifier = Modifier.weight(0.5f)
            ) { discNumber -> mutablePropertiesMap["DISCNUMBER"] = discNumber }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PreConfiguredOutlinedTextField(
                value = mutablePropertiesMap["DATE"],
                label = stringResource(id = R.string.date),
                modifier = Modifier.weight(0.5f)
            ) { date -> mutablePropertiesMap["DATE"] = date }
            PreConfiguredOutlinedTextField(
                value = mutablePropertiesMap["GENRE"],
                label = stringResource(id = R.string.genre),
                modifier = Modifier.weight(0.5f)
            ) { genre -> mutablePropertiesMap["GENRE"] = genre }
        }
    }

    LargeCategoryTitle(
        modifier = Modifier.padding(vertical = 6.dp),
        text = stringResource(id = R.string.credits)
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PreConfiguredOutlinedTextField(
                value = mutablePropertiesMap["COMPOSER"],
                label = stringResource(id = R.string.composer),
                modifier = Modifier.weight(0.5f)
            ) { composer -> mutablePropertiesMap["COMPOSER"] = composer }
            PreConfiguredOutlinedTextField(
                value = mutablePropertiesMap["LYRICIST"],
                label = stringResource(id = R.string.lyricist),
                modifier = Modifier.weight(0.5f)
            ) { lyricist -> mutablePropertiesMap["LYRICIST"] = lyricist }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PreConfiguredOutlinedTextField(
                value = mutablePropertiesMap["CONDUCTOR"],
                label = stringResource(id = R.string.conductor),
                modifier = Modifier.weight(0.5f)
            ) { conductor -> mutablePropertiesMap["CONDUCTOR"] = conductor }
            PreConfiguredOutlinedTextField(
                value = mutablePropertiesMap["REMIXER"],
                label = stringResource(id = R.string.remixer),
                modifier = Modifier.weight(0.5f)
            ) { remixer -> mutablePropertiesMap["REMIXER"] = remixer }
        }
        PreConfiguredOutlinedTextField(
            value = mutablePropertiesMap["PERFORMER"],
            label = stringResource(id = R.string.performer),
            modifier = Modifier.fillMaxWidth()
        ) { performer -> mutablePropertiesMap["PERFORMER"] = performer }

        LargeCategoryTitle(
            modifier = Modifier.padding(vertical = 6.dp),
            text = stringResource(id = R.string.others)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PreConfiguredOutlinedTextField(
                value = mutablePropertiesMap["COMMENT"],
                label = stringResource(id = R.string.comment),
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            ) { comment -> mutablePropertiesMap["COMMENT"] = comment }
            Column(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    modifier = Modifier.align(Alignment.End),
                    onClick = retrieveLyrics
                ) {
                    Row(
                        modifier = Modifier,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lyrics,
                            contentDescription = stringResource(id = R.string.retrieve_lyrics)
                        )

                        Text(text = stringResource(id = R.string.retrieve_lyrics))
                    }
                }
                PreConfiguredOutlinedTextField(
                    value = mutablePropertiesMap["LYRICS"],
                    label = stringResource(id = R.string.lyrics),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 20
                ) { lyrics -> mutablePropertiesMap["LYRICS"] = lyrics }
            }
        }
    }
}