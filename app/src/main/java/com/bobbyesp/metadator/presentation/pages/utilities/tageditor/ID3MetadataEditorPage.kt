package com.bobbyesp.metadator.presentation.pages.utilities.tageditor

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
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
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImagePainter
import com.bobbyesp.ext.joinOrNullToString
import com.bobbyesp.ext.toMinutes
import com.bobbyesp.metadator.R
import com.bobbyesp.metadator.model.SelectedSong
import com.bobbyesp.metadator.presentation.common.LocalNavController
import com.bobbyesp.metadator.presentation.components.image.AsyncImageImpl
import com.bobbyesp.ui.components.button.CloseButton
import com.bobbyesp.ui.components.others.MetadataTag
import com.bobbyesp.ui.components.others.PlaceholderCreator
import com.bobbyesp.ui.components.text.LargeCategoryTitle
import com.bobbyesp.ui.components.text.MarqueeText
import com.bobbyesp.ui.components.text.PreConfiguredOutlinedTextField
import com.bobbyesp.utilities.mediastore.AudioFileMetadata
import com.bobbyesp.utilities.mediastore.AudioFileMetadata.Companion.toFileMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ID3MetadataEditorPage(viewModel: ID3MetadataEditorPageViewModel, selectedSong: SelectedSong) {
    val viewState = viewModel.pageViewState.collectAsStateWithLifecycle().value
    val navController = LocalNavController.current

    val path = selectedSong.localSongPath

    var propertiesCopy: AudioFileMetadata? by remember { mutableStateOf(null) }

    LaunchedEffect(true) {
        viewModel.viewModelScope.launch(Dispatchers.IO) {
            viewModel.loadTrackMetadata(
                path = selectedSong.localSongPath!!
            )
        }
    }


    val sendActivityIntent =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.viewModelScope.launch(Dispatchers.IO) {
                    viewModel.loadTrackMetadata(
                        path = selectedSong.localSongPath!!
                    )
                }
                navController.popBackStack()
            }
        }


    fun saveInMediaStore(): Boolean = viewModel.saveMetadata(
        newMetadata = viewState.metadata?.copy(
            propertyMap = propertiesCopy!!.toPropertyMap()
        )!!,
        path = path!!
    ) {
        val intent = IntentSenderRequest.Builder(it).build()
        sendActivityIntent.launch(intent)
    }

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
                TextButton(
                    onClick = {
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
                is ID3MetadataEditorPageViewModel.Companion.ID3MetadataEditorPageState.Loading -> CircularProgressIndicator()
                is ID3MetadataEditorPageViewModel.Companion.ID3MetadataEditorPageState.Success -> {

                    SideEffect {
                        propertiesCopy = actualPageState.metadata.propertyMap.toFileMetadata()
                    }

                    val artworkUri = selectedSong.artworkPath
                    var showArtwork by remember { mutableStateOf(true) }
                    var showMediaStoreInfoDialog by remember { mutableStateOf(false) }

                    val audioStats by remember(actualPageState.metadata) {
                        mutableStateOf(actualPageState.metadata.audioProperties)
                    }

                    val songProperties by remember(actualPageState.metadata) {
                        mutableStateOf(actualPageState.metadata.propertyMap.toFileMetadata())
                    }

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
                            if (artworkUri != null && showArtwork) {
                                AsyncImageImpl(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(MaterialTheme.shapes.small)
                                        .align(Alignment.Center),
                                    model = artworkUri,
                                    onState = { state ->
                                        //if it was successful, don't show the placeholder, else show it
                                        showArtwork =
                                            state !is AsyncImagePainter.State.Error && state !is AsyncImagePainter.State.Empty
                                    },
                                    contentDescription = "Song Artwork"
                                )
                            } else {
                                PlaceholderCreator(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(MaterialTheme.shapes.small)
                                        .align(Alignment.Center),
                                    icon = Icons.Default.MusicNote,
                                    colorful = false
                                )
                            }
                        }
                        LargeCategoryTitle(
                            modifier = Modifier.padding(vertical = 6.dp),
                            text = stringResource(id = R.string.audio_features)
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
}