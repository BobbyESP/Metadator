package com.bobbyesp.metadator.presentation.pages.utilities.tageditor

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bobbyesp.ext.joinOrNullToString
import com.bobbyesp.ext.toMinutes
import com.bobbyesp.metadator.R
import com.bobbyesp.metadator.model.ParcelableSong
import com.bobbyesp.metadator.presentation.common.LocalNavController
import com.bobbyesp.metadator.presentation.components.image.ArtworkAsyncImage
import com.bobbyesp.ui.components.button.CloseButton
import com.bobbyesp.ui.components.others.MetadataTag
import com.bobbyesp.ui.components.text.LargeCategoryTitle
import com.bobbyesp.ui.components.text.MarqueeText
import com.bobbyesp.ui.components.text.PreConfiguredOutlinedTextField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ID3MetadataEditorPage(
    viewModel: ID3MetadataEditorPageViewModel, parcelableSong: ParcelableSong
) {
    val viewState = viewModel.pageViewState.collectAsStateWithLifecycle().value
    val navController = LocalNavController.current

    val path = parcelableSong.localSongPath
    val scope = rememberCoroutineScope()

    var propertiesCopy = viewModel.propertiesCopy.value

    var newArtworkAddress by remember {
        mutableStateOf<Uri?>(null)
    }

    LaunchedEffect(parcelableSong.localSongPath) {
        newArtworkAddress = null
        viewModel.loadTrackMetadata(
            path = parcelableSong.localSongPath!!
        )
    }

    val sendActivityIntent =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                scope.launch(Dispatchers.IO) {
                    viewModel.saveMetadata(
                        newMetadata = viewState.metadata?.copy(
                            propertyMap = propertiesCopy!!.toPropertyMap()
                        )!!, path = path!!, imageUri = newArtworkAddress
                    )
                }
                navController.popBackStack()
            }
        }

    var showNotSavedChangesDialog by remember { mutableStateOf(false) }

    fun saveInMediaStore(): Boolean = viewModel.saveMetadata(
        newMetadata = viewState.metadata?.copy(
            propertyMap = propertiesCopy!!.toPropertyMap()
        )!!, path = path!!, imageUri = newArtworkAddress
    ) {
        val intent = IntentSenderRequest.Builder(it).build()
        sendActivityIntent.launch(intent)
    }

    val singleImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            newArtworkAddress = uri
        })

    Scaffold(
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
                IconButton(onClick = {
                    scope.launch(Dispatchers.IO) {
                        val results = viewModel.getSpotifyResults(
                            propertiesCopy?.title?.joinToString() + propertiesCopy?.artist?.joinToString()
                        )
                        //TODO: Implement the search results
                    }
                }) {
                    Icon(
                        imageVector = Icons.Rounded.Downloading,
                        contentDescription = stringResource(
                            id = R.string.retrieve_song_info
                        )
                    )
                }
                TextButton(onClick = {
                    if (saveInMediaStore()) {
                        navController.popBackStack()
                    }
                }) {
                    Text(text = stringResource(id = R.string.save))
                }
            })
        }, modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Crossfade(
            targetState = viewState.state,
            animationSpec = tween(175),
            label = "Fade between pages (ID3MetadataEditorPage)",
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { actualPageState ->
            when (actualPageState) {
                is ID3MetadataEditorPageViewModel.Companion.ID3MetadataEditorPageState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(
                            8.dp, alignment = Alignment.CenterVertically
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(id = R.string.loading_metadata),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(0.7f)
                        )
                    }
                }

                is ID3MetadataEditorPageViewModel.Companion.ID3MetadataEditorPageState.Success -> {
                    var showMediaStoreInfoDialog by remember { mutableStateOf(false) }

                    val artworkUri = newArtworkAddress ?: parcelableSong.artworkPath

                    val songProperties = viewState.audioFileMetadata!!
                    val audioStats = viewState.audioProperties!!

                    val scrollState = rememberScrollState()

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
                            ArtworkAsyncImage(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(MaterialTheme.shapes.small)
                                    .align(Alignment.Center),
                                artworkPath = artworkUri,
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
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = stringResource(id = R.string.edit_image)
                                    )
                                }
                            }
                        }
                        LargeCategoryTitle(
                            modifier = Modifier.padding(vertical = 6.dp),
                            text = stringResource(id = R.string.audio_details)
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
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
                                    metadata = audioStats.bitrate.toString() + " kbps"
                                )
                                MetadataTag(
                                    modifier = Modifier.weight(0.5f),
                                    typeOfMetadata = stringResource(id = R.string.sample_rate),
                                    metadata = audioStats.sampleRate.toString() + " Hz"
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
                                    metadata = audioStats.channels.toString()
                                )
                                MetadataTag(
                                    modifier = Modifier.weight(0.5f),
                                    typeOfMetadata = stringResource(id = R.string.duration),
                                    metadata = audioStats.length.toMinutes()
                                )
                            }
                        }


                        LargeCategoryTitle(
                            modifier = Modifier.padding(vertical = 6.dp),
                            text = stringResource(id = R.string.general_tags)
                        )
                        PreConfiguredOutlinedTextField(
                            value = songProperties.title.joinOrNullToString(),
                            label = stringResource(id = R.string.title),
                            modifier = Modifier.fillMaxWidth()
                        ) { title ->
                            propertiesCopy?.title = arrayOf(title)
                        }

                        PreConfiguredOutlinedTextField(
                            value = songProperties.artist.joinOrNullToString(),
                            label = stringResource(id = R.string.artist),
                            modifier = Modifier.fillMaxWidth()
                        ) { artists ->
                            propertiesCopy?.artist =
                                artists.split(",").map { it.trim() }.toTypedArray()

                        }

                        PreConfiguredOutlinedTextField(
                            value = songProperties.album.joinOrNullToString(),
                            label = stringResource(id = R.string.album),
                            modifier = Modifier.fillMaxWidth()
                        ) { album ->
                            propertiesCopy?.album = arrayOf(album)
                        }

                        PreConfiguredOutlinedTextField(
                            value = songProperties.albumArtist.joinOrNullToString(),
                            label = stringResource(id = R.string.album_artist),
                            modifier = Modifier.fillMaxWidth()
                        ) { artists ->
                            propertiesCopy?.albumArtist =
                                artists.split(",").map { it.trim() }.toTypedArray()

                        }

                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                PreConfiguredOutlinedTextField(
                                    value = songProperties.trackNumber.joinOrNullToString(),
                                    label = stringResource(id = R.string.track_number),
                                    modifier = Modifier.weight(0.5f)
                                ) { trackNumber ->
                                    propertiesCopy?.trackNumber = arrayOf(trackNumber)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                PreConfiguredOutlinedTextField(
                                    value = songProperties.discNumber.joinOrNullToString(),
                                    label = stringResource(id = R.string.disc_number),
                                    modifier = Modifier.weight(0.5f)
                                ) { discNumber ->
                                    propertiesCopy?.discNumber = arrayOf(discNumber)

                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                PreConfiguredOutlinedTextField(
                                    value = songProperties.date.joinOrNullToString(),
                                    label = stringResource(id = R.string.date),
                                    modifier = Modifier.weight(0.5f)
                                ) { date ->
                                    propertiesCopy?.date = arrayOf(date)

                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                PreConfiguredOutlinedTextField(
                                    value = songProperties.genre.joinOrNullToString(),
                                    label = stringResource(id = R.string.genre),
                                    modifier = Modifier.weight(0.5f)
                                ) { genre ->
                                    propertiesCopy?.genre = arrayOf(genre)
                                }
                            }
                        }
                        LargeCategoryTitle(
                            modifier = Modifier.padding(vertical = 6.dp),
                            text = stringResource(id = R.string.credits)
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                PreConfiguredOutlinedTextField(
                                    value = songProperties.composer.joinOrNullToString(),
                                    label = stringResource(id = R.string.composer),
                                    modifier = Modifier.weight(0.5f)
                                ) { composer ->
                                    propertiesCopy?.composer = arrayOf(composer)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                PreConfiguredOutlinedTextField(
                                    value = songProperties.lyricist.joinOrNullToString(),
                                    label = stringResource(id = R.string.lyricist),
                                    modifier = Modifier.weight(0.5f)
                                ) { lyricist ->
                                    propertiesCopy?.lyricist = arrayOf(lyricist)
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                PreConfiguredOutlinedTextField(
                                    value = songProperties.conductor.joinOrNullToString(),
                                    label = stringResource(id = R.string.conductor),
                                    modifier = Modifier.weight(0.5f)
                                ) { conductor ->
                                    propertiesCopy?.conductor = arrayOf(conductor)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                PreConfiguredOutlinedTextField(
                                    value = songProperties.remixer.joinOrNullToString(),
                                    label = stringResource(id = R.string.remixer),
                                    modifier = Modifier.weight(0.5f)
                                ) { remixer ->
                                    propertiesCopy = propertiesCopy?.copy(
                                        remixer = arrayOf(remixer)
                                    )
                                }
                            }
                            PreConfiguredOutlinedTextField(
                                value = songProperties.performer.joinOrNullToString(),
                                label = stringResource(id = R.string.performer),
                                modifier = Modifier.fillMaxWidth()
                            ) { performer ->
                                propertiesCopy?.performer =
                                    performer.split(",").map { it.trim() }.toTypedArray()
                            }
                        }
                    }

                    if (showMediaStoreInfoDialog) {
                        MediaStoreInfoDialog(onDismissRequest = {
                            showMediaStoreInfoDialog = false
                        })
                    }
                }

                is ID3MetadataEditorPageViewModel.Companion.ID3MetadataEditorPageState.Error -> Text(
                    text = actualPageState.throwable.message ?: "Unknown error"
                )
            }
        }
    }

    if (showNotSavedChangesDialog) {
        NotSavedChanges(onDismissChanges = {
            showNotSavedChangesDialog = false
            navController.popBackStack()
        }, onReturnToPage = {
            showNotSavedChangesDialog = false
        })
    }
}

@Composable
private fun NotSavedChanges(
    onDismissChanges: () -> Unit = {}, onReturnToPage: () -> Unit = {}
) {
    AlertDialog(onDismissRequest = onReturnToPage, icon = {
        Icon(
            imageVector = Icons.Rounded.Warning,
            contentDescription = stringResource(id = R.string.warning)
        )
    }, title = {
        Text(text = stringResource(id = R.string.unsaved_changes))
    }, text = {
        Text(
            text = stringResource(id = R.string.unsaved_changes_info),
            style = MaterialTheme.typography.bodyMedium
        )
    }, dismissButton = {
        TextButton(
            onClick = onReturnToPage,
        ) {
            Text(text = stringResource(id = R.string.return_str))
        }
    }, confirmButton = {
        Button(
            onClick = onDismissChanges,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text(text = stringResource(id = R.string.discard_changes))
        }
    })
}