package com.bobbyesp.metadator.presentation.pages.utilities.tageditor

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material.icons.rounded.Edit
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bobbyesp.metadator.R
import com.bobbyesp.metadator.model.ParcelableSong
import com.bobbyesp.metadator.presentation.common.LocalNavController
import com.bobbyesp.metadator.presentation.common.LocalOrientation
import com.bobbyesp.metadator.presentation.components.image.AsyncImage
import com.bobbyesp.metadator.presentation.pages.utilities.tageditor.spotify.MetadataBsVM
import com.bobbyesp.metadator.presentation.pages.utilities.tageditor.spotify.SpMetadataBottomSheetContent
import com.bobbyesp.ui.common.pages.ErrorPage
import com.bobbyesp.ui.common.pages.LoadingPage
import com.bobbyesp.ui.components.button.CloseButton
import com.bobbyesp.ui.components.others.MetadataTag
import com.bobbyesp.ui.components.text.LargeCategoryTitle
import com.bobbyesp.ui.components.text.MarqueeText
import com.bobbyesp.ui.components.text.PreConfiguredOutlinedTextField
import com.bobbyesp.utilities.ext.toMinutes
import com.bobbyesp.utilities.states.ResourceState
import com.bobbyesp.utilities.states.ScreenState
import com.kyant.taglib.AudioProperties
import com.materialkolor.ktx.harmonize
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetadataEditorPage(
    state: State<MetadataEditorVM.PageViewState>,
    bsViewState: State<MetadataBsVM.ViewState>,
    receivedAudio: ParcelableSong,
    onBsEvent: (MetadataBsVM.Event) -> Unit,
    onEvent: (MetadataEditorVM.Event) -> Unit
) {
    val navController = LocalNavController.current
    val scope = rememberCoroutineScope()
    val pageState = state.value

    LaunchedEffect(receivedAudio) {
        onEvent(MetadataEditorVM.Event.LoadMetadata(receivedAudio.localPath))
    }

    var newArtworkAddress by rememberSaveable(key = "newArtworkAddress") {
        mutableStateOf<Uri?>(null)
    }

    val singleImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            newArtworkAddress = uri
        })

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.Hidden, skipHiddenState = false
        )
    )

    BottomSheetScaffold(
        topBar = {
            TopAppBar(title = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    MarqueeText(
                        text = stringResource(id = R.string.viewing_metadata),
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }, navigationIcon = {
                CloseButton { navController.popBackStack() }
            }, actions = {
                IconButton(
                    onClick = {
                        scope.launch {
                            scaffoldState.bottomSheetState.partialExpand()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Downloading,
                        contentDescription = stringResource(
                            id = R.string.retrieve_song_info
                        )
                    )
                }
                TextButton(
                    onClick = {
                        onEvent(
                            MetadataEditorVM.Event.SaveAll(
                                receivedAudio.localPath,
                                listOf(newArtworkAddress ?: receivedAudio.artworkPath ?: Uri.EMPTY)
                            )
                        )
                    }
                ) {
                    Text(text = stringResource(id = R.string.save))
                }
            }, scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior())
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
                scope.launch {
                    scaffoldState.bottomSheetState.hide()
                }
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
                is ScreenState.Error -> ErrorPage(error = state.exception.stackTrace.toString()) {
                    onEvent(
                        MetadataEditorVM.Event.LoadMetadata(receivedAudio.localPath)
                    )
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
                                        .padding(bottom = 8.dp)
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
                                        IconButton(colors = IconButtonDefaults.iconButtonColors(
                                            containerColor = Color.Black.copy(alpha = 0.5f)
                                        ), onClick = {
                                            singleImagePickerLauncher.launch(
                                                PickVisualMediaRequest(
                                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                                )
                                            )
                                        }) {
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
                                    SongProperties(pageState.mutablePropertiesMap)
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
                                        IconButton(colors = IconButtonDefaults.iconButtonColors(
                                            containerColor = Color.Black.copy(alpha = 0.5f)
                                        ), onClick = {
                                            singleImagePickerLauncher.launch(
                                                PickVisualMediaRequest(
                                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                                )
                                            )
                                        }) {
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
                                        SongProperties(pageState.mutablePropertiesMap)
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
                metadata = audioProperties.length.toMinutes()
            )
        }
    }
}

@Composable
fun SongProperties(mutablePropertiesMap: SnapshotStateMap<String, String>) {
    LargeCategoryTitle(
        modifier = Modifier.padding(vertical = 6.dp),
        text = stringResource(id = R.string.general_tags)
    )

    PreConfiguredOutlinedTextField(
        value = mutablePropertiesMap["TITLE"],
        label = stringResource(id = R.string.title),
        modifier = Modifier.fillMaxWidth()
    ) { title ->
        mutablePropertiesMap["TITLE"] = title
    }

    PreConfiguredOutlinedTextField(
        value = mutablePropertiesMap["ARTIST"],
        label = stringResource(id = R.string.artist),
        modifier = Modifier.fillMaxWidth()
    ) { artists ->
        mutablePropertiesMap["ARTIST"] = artists
    }

    PreConfiguredOutlinedTextField(
        value = mutablePropertiesMap["ALBUM"],
        label = stringResource(id = R.string.album),
        modifier = Modifier.fillMaxWidth()
    ) { album ->
        mutablePropertiesMap["ALBUM"] = album
    }

    PreConfiguredOutlinedTextField(
        value = mutablePropertiesMap["ALBUMARTIST"],
        label = stringResource(id = R.string.album_artist),
        modifier = Modifier.fillMaxWidth()
    ) { albumArtist ->
        mutablePropertiesMap["ALBUMARTIST"] = albumArtist
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            PreConfiguredOutlinedTextField(
                value = mutablePropertiesMap["TRACKNUMBER"],
                label = stringResource(id = R.string.track_number),
                modifier = Modifier.weight(0.5f)
            ) { trackNumber ->
                mutablePropertiesMap["TRACKNUMBER"] = trackNumber
            }
            Spacer(modifier = Modifier.width(8.dp))
            PreConfiguredOutlinedTextField(
                value = mutablePropertiesMap["DISCNUMBER"],
                label = stringResource(id = R.string.disc_number),
                modifier = Modifier.weight(0.5f)
            ) { discNumber ->
                mutablePropertiesMap["DISCNUMBER"] = discNumber
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            PreConfiguredOutlinedTextField(
                value = mutablePropertiesMap["DATE"],
                label = stringResource(id = R.string.date),
                modifier = Modifier.weight(0.5f)
            ) { date ->
                mutablePropertiesMap["DATE"] = date
            }
            Spacer(modifier = Modifier.width(8.dp))
            PreConfiguredOutlinedTextField(
                value = mutablePropertiesMap["GENRE"],
                label = stringResource(id = R.string.genre),
                modifier = Modifier.weight(0.5f)
            ) { genre ->
                mutablePropertiesMap["GENRE"] = genre
            }
        }
    }

    LargeCategoryTitle(
        modifier = Modifier.padding(vertical = 6.dp), text = stringResource(id = R.string.credits)
    )

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            PreConfiguredOutlinedTextField(
                value = mutablePropertiesMap["COMPOSER"],
                label = stringResource(id = R.string.composer),
                modifier = Modifier.weight(0.5f)
            ) { composer ->
                mutablePropertiesMap["COMPOSER"] = composer
            }
            Spacer(modifier = Modifier.width(8.dp))
            PreConfiguredOutlinedTextField(
                value = mutablePropertiesMap["LYRICIST"],
                label = stringResource(id = R.string.lyricist),
                modifier = Modifier.weight(0.5f)
            ) { lyricist ->
                mutablePropertiesMap["LYRICIST"] = lyricist
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            PreConfiguredOutlinedTextField(
                value = mutablePropertiesMap["CONDUCTOR"],
                label = stringResource(id = R.string.conductor),
                modifier = Modifier.weight(0.5f)
            ) { conductor ->
                mutablePropertiesMap["CONDUCTOR"] = conductor
            }
            Spacer(modifier = Modifier.width(8.dp))
            PreConfiguredOutlinedTextField(
                value = mutablePropertiesMap["REMIXER"],
                label = stringResource(id = R.string.remixer),
                modifier = Modifier.weight(0.5f)
            ) { remixer ->
                mutablePropertiesMap["REMIXER"] = remixer
            }
        }
        PreConfiguredOutlinedTextField(
            value = mutablePropertiesMap["PERFORMER"],
            label = stringResource(id = R.string.performer),
            modifier = Modifier.fillMaxWidth()
        ) { performer ->
            mutablePropertiesMap["PERFORMER"] = performer
        }

        LargeCategoryTitle(
            modifier = Modifier.padding(vertical = 6.dp),
            text = stringResource(id = R.string.others)
        )

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            PreConfiguredOutlinedTextField(
                value = mutablePropertiesMap["COMMENT"],
                label = stringResource(id = R.string.comment),
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            ) { comment ->
                mutablePropertiesMap["COMMENT"] = comment
            }
            PreConfiguredOutlinedTextField(
                value = mutablePropertiesMap["USLT"],
                label = stringResource(id = R.string.lyrics) + (" (USLT)"),
                modifier = Modifier.fillMaxWidth(),
                maxLines = 20
            ) { uslt_lyrics ->
                mutablePropertiesMap["USLT"] = uslt_lyrics
            }
            PreConfiguredOutlinedTextField(
                value = mutablePropertiesMap["SYLT"],
                label = stringResource(id = R.string.lyrics) + (" (SYLT)"),
                modifier = Modifier.fillMaxWidth(),
                maxLines = 20
            ) { sylt_lyrics ->
                mutablePropertiesMap["SYLT"] = sylt_lyrics
            }
        }
    }
}