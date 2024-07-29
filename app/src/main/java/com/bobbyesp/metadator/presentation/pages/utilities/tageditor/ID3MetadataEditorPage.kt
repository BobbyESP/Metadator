package com.bobbyesp.metadator.presentation.pages.utilities.tageditor
//
//import android.app.Activity
//import android.net.Uri
//import androidx.activity.compose.BackHandler
//import androidx.activity.compose.rememberLauncherForActivityResult
//import androidx.activity.result.IntentSenderRequest
//import androidx.activity.result.PickVisualMediaRequest
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.compose.animation.Crossfade
//import androidx.compose.animation.core.animateDpAsState
//import androidx.compose.animation.core.tween
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.aspectRatio
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.navigationBarsPadding
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.layout.width
//import androidx.compose.foundation.rememberScrollState
//import androidx.compose.foundation.verticalScroll
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.rounded.Downloading
//import androidx.compose.material.icons.rounded.Edit
//import androidx.compose.material.icons.rounded.Warning
//import androidx.compose.material3.AlertDialog
//import androidx.compose.material3.BottomSheetScaffold
//import androidx.compose.material3.Button
//import androidx.compose.material3.ButtonDefaults
//import androidx.compose.material3.ExperimentalMaterial3Api
//import androidx.compose.material3.Icon
//import androidx.compose.material3.IconButton
//import androidx.compose.material3.IconButtonDefaults
//import androidx.compose.material3.LinearProgressIndicator
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.SheetValue
//import androidx.compose.material3.Text
//import androidx.compose.material3.TextButton
//import androidx.compose.material3.TopAppBar
//import androidx.compose.material3.TopAppBarDefaults
//import androidx.compose.material3.rememberBottomSheetScaffoldState
//import androidx.compose.material3.rememberStandardBottomSheetState
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.rememberCoroutineScope
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.res.stringResource
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.lifecycle.compose.collectAsStateWithLifecycle
//import com.bobbyesp.metadator.R
//import com.bobbyesp.metadator.model.ParcelableSong
//import com.bobbyesp.metadator.presentation.common.LocalNavController
//import com.bobbyesp.metadator.presentation.components.image.AsyncImage
//import com.bobbyesp.metadator.presentation.pages.utilities.tageditor.spotify.SpMetadataBottomSheetContent
//import com.bobbyesp.ui.components.button.CloseButton
//import com.bobbyesp.ui.components.others.MetadataTag
//import com.bobbyesp.ui.components.text.LargeCategoryTitle
//import com.bobbyesp.ui.components.text.MarqueeText
//import com.bobbyesp.ui.components.text.PreConfiguredOutlinedTextField
//import com.bobbyesp.utilities.ext.toAudioFileMetadata
//import com.bobbyesp.utilities.ext.toMinutes
//import com.bobbyesp.utilities.ext.toModifiableMap
//import com.bobbyesp.utilities.mediastore.AudioFileMetadata.Companion.toAudioFileMetadata
//import com.bobbyesp.utilities.mediastore.AudioFileMetadata.Companion.toPropertyMap
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ID3MetadataEditorPage(
//    viewModel: ID3MetadataEditorPageViewModel, parcelableSong: ParcelableSong
//) {
//    val viewState = viewModel.pageViewState.collectAsStateWithLifecycle().value
//    val navController = LocalNavController.current
//
//    val path = parcelableSong.localPath
//
//    val scope = rememberCoroutineScope()
//    val scaffoldState = rememberBottomSheetScaffoldState(
//        bottomSheetState = rememberStandardBottomSheetState(
//            initialValue = SheetValue.Hidden, skipHiddenState = false
//        )
//    )
//
//    val metadata = viewState.metadata
//    val modifiablePropertyMap = viewState.metadata?.propertyMap?.toModifiableMap()
//
//    var newArtworkAddress by remember {
//        mutableStateOf<Uri?>(null)
//    }
//
//    LaunchedEffect(parcelableSong.localPath) {
//        newArtworkAddress = null
//        viewModel.loadTrackMetadata(
//            path = parcelableSong.localPath
//        )
//    }
//
//    val sendActivityIntent =
//        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartIntentSenderForResult()) { result ->
//            if (result.resultCode == Activity.RESULT_OK) {
//                scope.launch(Dispatchers.IO) {
//                    modifiablePropertyMap?.let { newMetadata ->
//                        viewModel.saveMetadata(
//                            newMetadata = viewState.metadata.copy(
//                                propertyMap = newMetadata.toAudioFileMetadata().toPropertyMap()
//                            ), path = path, imageUri = newArtworkAddress
//                        )
//                    }
//                }
//                navController.popBackStack()
//            }
//        }
//
//    var showNotSavedChangesDialog by remember { mutableStateOf(false) }
//
//    fun saveInMediaStore(): Boolean = viewModel.saveMetadata(
//        newMetadata = viewState.metadata?.copy(
//            propertyMap = modifiablePropertyMap!!.toAudioFileMetadata().toPropertyMap()
//        )!!, path = path, imageUri = newArtworkAddress
//    ) {
//        val intent = IntentSenderRequest.Builder(it).build()
//        sendActivityIntent.launch(intent)
//    }
//
//    val singleImagePickerLauncher =
//        rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia(),
//            onResult = { uri ->
//                newArtworkAddress = uri
//            })
//
//
//    BackHandler {
//        if (scaffoldState.bottomSheetState.isVisible) {
//            scope.launch {
//                scaffoldState.bottomSheetState.hide()
//            }
//        } else {
//            navController.popBackStack()
//        }
//    }
//
//
//    BottomSheetScaffold(
//        topBar = {
//            TopAppBar(title = {
//                Column(modifier = Modifier.fillMaxWidth()) {
//                    MarqueeText(
//                        text = stringResource(id = R.string.viewing_metadata),
//                        style = MaterialTheme.typography.bodyLarge,
//                        fontSize = 20.sp,
//                        fontWeight = FontWeight.Bold
//                    )
//                }
//            }, navigationIcon = {
//                CloseButton { navController.popBackStack() }
//            }, actions = {
//                IconButton(onClick = {
//                    scope.launch {
//                        scaffoldState.bottomSheetState.partialExpand()
//                    }
//                }) {
//                    Icon(
//                        imageVector = Icons.Rounded.Downloading,
//                        contentDescription = stringResource(
//                            id = R.string.retrieve_song_info
//                        )
//                    )
//                }
//                TextButton(onClick = {
//                    val isInfoSavedInMediaStore = saveInMediaStore()
//                    if (isInfoSavedInMediaStore) {
//                        navController.popBackStack()
//                    }
//                }) {
//                    Text(text = stringResource(id = R.string.save))
//                }
//            }, scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior())
//        },
//        modifier = Modifier.fillMaxSize(),
//        scaffoldState = scaffoldState,
//        sheetPeekHeight = 148.dp,
//        sheetShadowElevation = 8.dp,
//        sheetContent = {
//            SpMetadataBottomSheetContent(
//                name = modifiablePropertyMap?.get("TITLE") ?: "",
//                artist = modifiablePropertyMap?.get("ARTIST") ?: "",
//                sheetState = scaffoldState.bottomSheetState,
//                onCloseSheet = {
//                    scope.launch {
//                        scaffoldState.bottomSheetState.hide()
//                    }
//                }
//            )
//        }) { innerPadding ->
//        Crossfade(
//            targetState = viewState.state,
//            animationSpec = tween(175),
//            label = "Fade between pages (ID3MetadataEditorPage)",
//            modifier = Modifier
//                .fillMaxSize()
//                .navigationBarsPadding()
//        ) { actualPageState ->
//            when (actualPageState) {
//                is ID3MetadataEditorPageViewModel.Companion.ID3MetadataEditorPageState.Loading -> {
//                    Column(
//                        modifier = Modifier.fillMaxSize(),
//                        verticalArrangement = Arrangement.spacedBy(
//                            8.dp, alignment = Alignment.CenterVertically
//                        ),
//                        horizontalAlignment = Alignment.CenterHorizontally
//                    ) {
//                        Text(
//                            text = stringResource(id = R.string.loading_metadata),
//                            style = MaterialTheme.typography.bodyMedium,
//                            fontWeight = FontWeight.SemiBold
//                        )
//                        LinearProgressIndicator(
//                            modifier = Modifier.fillMaxWidth(0.7f)
//                        )
//                    }
//                }
//
//                is ID3MetadataEditorPageViewModel.Companion.ID3MetadataEditorPageState.Success -> {
//                    var showMediaStoreInfoDialog by remember { mutableStateOf(false) }
//
//                    val artworkUri = newArtworkAddress ?: parcelableSong.artworkPath
//
//                    val songProperties by remember {
//                        mutableStateOf(metadata!!.propertyMap.toAudioFileMetadata())
//                    }
//                    val audioStats by remember {
//                        mutableStateOf(viewState.audioProperties!!)
//                    }
//
//                    val scrollState = rememberScrollState()
//
//                    Column(
//                        modifier = Modifier
//                            .fillMaxSize()
//                            .verticalScroll(scrollState)
//                            .padding(horizontal = 16.dp)
//                    ) {
//                        Box(
//                            modifier = Modifier
//                                .size(250.dp)
//                                .padding(8.dp)
//                                .padding(bottom = 8.dp)
//                                .aspectRatio(1f)
//                                .align(Alignment.CenterHorizontally),
//                        ) {
//                            AsyncImage(
//                                modifier = Modifier
//                                    .fillMaxSize()
//                                    .clip(MaterialTheme.shapes.small)
//                                    .align(Alignment.Center),
//                                imageModel = artworkUri,
//                            )
//                            Box(
//                                modifier = Modifier
//                                    .fillMaxSize()
//                                    .padding(8.dp),
//                                contentAlignment = Alignment.BottomEnd
//                            ) {
//                                IconButton(colors = IconButtonDefaults.iconButtonColors(
//                                    containerColor = Color.Black.copy(alpha = 0.5f)
//                                ), onClick = {
//                                    singleImagePickerLauncher.launch(
//                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
//                                    )
//                                }) {
//                                    Icon(
//                                        imageVector = Icons.Rounded.Edit,
//                                        contentDescription = stringResource(id = R.string.edit_image)
//                                    )
//                                }
//                            }
//                        }
//                        LargeCategoryTitle(
//                            modifier = Modifier.padding(vertical = 6.dp),
//                            text = stringResource(id = R.string.audio_details)
//                        )
//
//                        Column(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(horizontal = 12.dp),
//                            verticalArrangement = Arrangement.spacedBy(8.dp),
//                            horizontalAlignment = Alignment.CenterHorizontally
//                        ) {
//                            Row(
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .padding(bottom = 8.dp),
//                            ) {
//                                MetadataTag(
//                                    modifier = Modifier.weight(0.5f),
//                                    typeOfMetadata = stringResource(id = R.string.bitrate),
//                                    metadata = audioStats.bitrate.toString() + " kbps"
//                                )
//                                MetadataTag(
//                                    modifier = Modifier.weight(0.5f),
//                                    typeOfMetadata = stringResource(id = R.string.sample_rate),
//                                    metadata = audioStats.sampleRate.toString() + " Hz"
//                                )
//                            }
//                            Row(
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .padding(bottom = 8.dp),
//                            ) {
//                                MetadataTag(
//                                    modifier = Modifier.weight(0.5f),
//                                    typeOfMetadata = stringResource(id = R.string.channels),
//                                    metadata = audioStats.channels.toString()
//                                )
//                                MetadataTag(
//                                    modifier = Modifier.weight(0.5f),
//                                    typeOfMetadata = stringResource(id = R.string.duration),
//                                    metadata = audioStats.length.toMinutes()
//                                )
//                            }
//                        }
//
//
//                        LargeCategoryTitle(
//                            modifier = Modifier.padding(vertical = 6.dp),
//                            text = stringResource(id = R.string.general_tags)
//                        )
//                        PreConfiguredOutlinedTextField(
//                            value = songProperties.title,
//                            label = stringResource(id = R.string.title),
//                            modifier = Modifier.fillMaxWidth()
//                        ) { title ->
//                            modifiablePropertyMap?.put("TITLE", title)
//                        }
//
//                        PreConfiguredOutlinedTextField(
//                            value = songProperties.artist,
//                            label = stringResource(id = R.string.artist),
//                            modifier = Modifier.fillMaxWidth()
//                        ) { artists ->
//                            modifiablePropertyMap?.put("ARTIST", artists)
//                        }
//
//                        PreConfiguredOutlinedTextField(
//                            value = songProperties.album,
//                            label = stringResource(id = R.string.album),
//                            modifier = Modifier.fillMaxWidth()
//                        ) { album ->
//                            modifiablePropertyMap?.put("ALBUM", album)
//                        }
//
//                        PreConfiguredOutlinedTextField(
//                            value = songProperties.albumArtist,
//                            label = stringResource(id = R.string.album_artist),
//                            modifier = Modifier.fillMaxWidth()
//                        ) { artists ->
//                            modifiablePropertyMap?.put("ALBUMARTIST", artists)
//                        }
//
//                        Column(
//                            modifier = Modifier.fillMaxWidth()
//                        ) {
//                            Row(
//                                modifier = Modifier.fillMaxWidth(),
//                            ) {
//                                PreConfiguredOutlinedTextField(
//                                    value = songProperties.trackNumber,
//                                    label = stringResource(id = R.string.track_number),
//                                    modifier = Modifier.weight(0.5f)
//                                ) { trackNumber ->
//                                    modifiablePropertyMap?.put("TRACKNUMBER", trackNumber)
//                                }
//                                Spacer(modifier = Modifier.width(8.dp))
//                                PreConfiguredOutlinedTextField(
//                                    value = songProperties.discNumber,
//                                    label = stringResource(id = R.string.disc_number),
//                                    modifier = Modifier.weight(0.5f)
//                                ) { discNumber ->
//                                    modifiablePropertyMap?.put("DISCNUMBER", discNumber)
//                                }
//                            }
//                            Row(
//                                modifier = Modifier.fillMaxWidth(),
//                            ) {
//                                PreConfiguredOutlinedTextField(
//                                    value = songProperties.date,
//                                    label = stringResource(id = R.string.date),
//                                    modifier = Modifier.weight(0.5f)
//                                ) { date ->
//                                    modifiablePropertyMap?.put("DATE", date)
//                                }
//                                Spacer(modifier = Modifier.width(8.dp))
//                                PreConfiguredOutlinedTextField(
//                                    value = songProperties.genre,
//                                    label = stringResource(id = R.string.genre),
//                                    modifier = Modifier.weight(0.5f)
//                                ) { genre ->
//                                    modifiablePropertyMap?.put("GENRE", genre)
//                                }
//                            }
//                        }
//                        LargeCategoryTitle(
//                            modifier = Modifier.padding(vertical = 6.dp),
//                            text = stringResource(id = R.string.credits)
//                        )
//
//                        Column(
//                            modifier = Modifier.fillMaxWidth()
//                        ) {
//                            Row(
//                                modifier = Modifier.fillMaxWidth(),
//                            ) {
//                                PreConfiguredOutlinedTextField(
//                                    value = songProperties.composer,
//                                    label = stringResource(id = R.string.composer),
//                                    modifier = Modifier.weight(0.5f)
//                                ) { composer ->
//                                    modifiablePropertyMap?.put("COMPOSER", composer)
//                                }
//                                Spacer(modifier = Modifier.width(8.dp))
//                                PreConfiguredOutlinedTextField(
//                                    value = songProperties.lyricist,
//                                    label = stringResource(id = R.string.lyricist),
//                                    modifier = Modifier.weight(0.5f)
//                                ) { lyricist ->
//                                    modifiablePropertyMap?.put("LYRICIST", lyricist)
//                                }
//                            }
//                            Row(
//                                modifier = Modifier.fillMaxWidth(),
//                            ) {
//                                PreConfiguredOutlinedTextField(
//                                    value = songProperties.conductor,
//                                    label = stringResource(id = R.string.conductor),
//                                    modifier = Modifier.weight(0.5f)
//                                ) { conductor ->
//                                    modifiablePropertyMap?.put("CONDUCTOR", conductor)
//                                }
//                                Spacer(modifier = Modifier.width(8.dp))
//                                PreConfiguredOutlinedTextField(
//                                    value = songProperties.remixer,
//                                    label = stringResource(id = R.string.remixer),
//                                    modifier = Modifier.weight(0.5f)
//                                ) { remixer ->
//                                    modifiablePropertyMap?.put("REMIXER", remixer)
//                                }
//                            }
//                            PreConfiguredOutlinedTextField(
//                                value = songProperties.performer,
//                                label = stringResource(id = R.string.performer),
//                                modifier = Modifier.fillMaxWidth()
//                            ) { performer ->
//                                modifiablePropertyMap?.put("PERFORMER", performer)
//                            }
//                        }
//                        LargeCategoryTitle(
//                            modifier = Modifier.padding(vertical = 6.dp),
//                            text = stringResource(id = R.string.others)
//                        )
//
//                        Column(
//                            modifier = Modifier.fillMaxWidth()
//                        ) {
//                            PreConfiguredOutlinedTextField(
//                                value = songProperties.comment,
//                                label = stringResource(id = R.string.comment),
//                                modifier = Modifier.fillMaxWidth(),
//                                maxLines = 3
//                            ) { comment ->
//                                modifiablePropertyMap?.put("COMMENT", comment)
//                            }
//                        }
//
//                        val animatedBottomPadding by animateDpAsState(
//                            targetValue = if (scaffoldState.bottomSheetState.isVisible) innerPadding.calculateBottomPadding() + 6.dp else 0.dp,
//                            label = "animatedBottomPadding"
//                        )
//                        Spacer(modifier = Modifier.height(animatedBottomPadding))
//                    }
//
//                    if (showMediaStoreInfoDialog) {
//                        MediaStoreInfoDialog(onDismissRequest = {
//                            showMediaStoreInfoDialog = false
//                        })
//                    }
//                }
//
//                is ID3MetadataEditorPageViewModel.Companion.ID3MetadataEditorPageState.Error -> Text(
//                    text = actualPageState.throwable.message ?: "Unknown error"
//                )
//            }
//        }
//    }
//
//    if (showNotSavedChangesDialog) {
//        NotSavedChanges(onDismissChanges = {
//            showNotSavedChangesDialog = false
//            navController.popBackStack()
//        }, onReturnToPage = {
//            showNotSavedChangesDialog = false
//        })
//    }
//}
//
//@Composable
//private fun NotSavedChanges(
//    onDismissChanges: () -> Unit = {}, onReturnToPage: () -> Unit = {}
//) {
//    AlertDialog(onDismissRequest = onReturnToPage, icon = {
//        Icon(
//            imageVector = Icons.Rounded.Warning,
//            contentDescription = stringResource(id = R.string.warning)
//        )
//    }, title = {
//        Text(text = stringResource(id = R.string.unsaved_changes))
//    }, text = {
//        Text(
//            text = stringResource(id = R.string.unsaved_changes_info),
//            style = MaterialTheme.typography.bodyMedium
//        )
//    }, dismissButton = {
//        TextButton(
//            onClick = onReturnToPage,
//        ) {
//            Text(text = stringResource(id = R.string.return_str))
//        }
//    }, confirmButton = {
//        Button(
//            onClick = onDismissChanges,
//            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
//        ) {
//            Text(text = stringResource(id = R.string.discard_changes))
//        }
//    })
//}