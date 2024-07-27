package com.bobbyesp.metadator.presentation.pages.mediaplayer.player.views

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bobbyesp.metadator.R
import com.bobbyesp.metadator.presentation.components.image.AsyncImage
import com.bobbyesp.metadator.presentation.pages.mediaplayer.MediaplayerViewModel
import com.bobbyesp.metadator.presentation.pages.mediaplayer.player.MediaplayerSheetView
import com.bobbyesp.metadator.presentation.pages.mediaplayer.player.PlayerControls
import com.bobbyesp.ui.components.bottomsheet.draggable.DraggableBottomSheetState
import com.bobbyesp.ui.motion.MotionConstants
import com.bobbyesp.ui.motion.tweenEnter
import com.bobbyesp.ui.motion.tweenExit
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MediaplayerExpandedContent(
    modifier: Modifier = Modifier,
    viewModel: MediaplayerViewModel,
    sheetState: DraggableBottomSheetState
) {
    var view by remember {
        mutableStateOf(MediaplayerSheetView.FULL_PLAYER)
    }
    val scope = rememberCoroutineScope()
    val playingSong = viewModel.songBeingPlayed.collectAsStateWithLifecycle().value?.mediaMetadata

    val config = LocalConfiguration.current

    BackHandler {
        sheetState.collapseSoft()
    }

    SharedTransitionLayout(
        modifier = Modifier.fillMaxSize()
    ) {
        Surface(
            modifier = modifier
                .fillMaxSize()
                .padding(
                    WindowInsets.systemBars
                        .only(WindowInsetsSides.Horizontal)
                        .asPaddingValues()
                ),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            when (config.orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> {
                    Row(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            AsyncImage(
                                imageModel = playingSong?.artworkUri,
                                modifier = Modifier
                                    .fillMaxHeight(0.9f)
                                    .aspectRatio(1f)
                                    .clip(MaterialTheme.shapes.small)
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .statusBarsPadding()
                                    .padding(start = 12.dp),
                            ) {
                                IconButton(onClick = {
                                    scope.launch {
                                        sheetState.collapseSoft()
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowBackIosNew,
                                        contentDescription = stringResource(id = R.string.close),
                                        modifier = Modifier.rotate(-90f)
                                    )
                                }
                                IconButton(onClick = {

                                }) {
                                    Icon(
                                        imageVector = Icons.Rounded.MoreVert,
                                        contentDescription = stringResource(
                                            id = R.string.more
                                        )
                                    )
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            PlayerControls(
                                modifier = Modifier.fillMaxWidth(), viewModel = viewModel
                            )
                        }
                    }
                }

                else -> {
                    AnimatedContent(targetState = view, label = "", transitionSpec = {
                        fadeIn(
                            tweenEnter(delayMillis = MotionConstants.DURATION_EXIT_SHORT)
                        ) togetherWith fadeOut(
                            tweenExit(durationMillis = MotionConstants.DURATION_EXIT_SHORT)
                        )
                    }) {
                        when (it) {
                            MediaplayerSheetView.FULL_PLAYER -> {
                                Column(
                                    modifier = Modifier.statusBarsPadding()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(horizontal = 12.dp)
                                            .padding(top = 12.dp, bottom = 12.dp)
                                    ) {
                                        IconButton(onClick = {
                                            scope.launch {
                                                sheetState.collapseSoft()
                                            }
                                        }) {
                                            Icon(
                                                imageVector = Icons.Rounded.ArrowBackIosNew,
                                                contentDescription = stringResource(id = R.string.close),
                                                modifier = Modifier.rotate(-90f)
                                            )

                                        }
                                        Spacer(modifier = Modifier.weight(1f))
                                        IconButton(onClick = {

                                        }) {
                                            Icon(
                                                imageVector = Icons.Rounded.MoreVert,
                                                contentDescription = stringResource(id = R.string.more)
                                            )
                                        }
                                    }
                                    Column(
                                        modifier = Modifier,
                                        verticalArrangement = Arrangement.spacedBy(24.dp)
                                    ) {
                                        AsyncImage(
                                            imageModel = playingSong?.artworkUri,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(1f)
                                                .padding(horizontal = 24.dp, vertical = 16.dp)
                                                .clip(MaterialTheme.shapes.small)
                                        )
                                        PlayerControls(
                                            modifier = Modifier.fillMaxWidth(),
                                            viewModel = viewModel
                                        )
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
//                                    PlayerOptions(
//                                        modifier = Modifier.fillMaxWidth(),
//                                        onOpenQueue = {
//                                            view = MediaplayerSheetView.QUEUE
//                                        }
//                                    )
                                }

                            }

                            MediaplayerSheetView.QUEUE -> {
                                PlayerQueue(
                                    imageModifier = Modifier,
                                    nowPlaying = playingSong,
                                    queue = emptyList(), onBackPressed = {
                                        view = MediaplayerSheetView.FULL_PLAYER
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}