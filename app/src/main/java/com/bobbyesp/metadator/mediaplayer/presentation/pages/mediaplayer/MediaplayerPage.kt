package com.bobbyesp.metadator.mediaplayer.presentation.pages.mediaplayer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowUp
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bobbyesp.metadator.R
import com.bobbyesp.metadator.core.presentation.common.LocalNavController
import com.bobbyesp.metadator.core.util.navigateBack
import com.bobbyesp.metadator.mediaplayer.presentation.pages.mediaplayer.player.CollapsedPlayerHeight
import com.bobbyesp.metadator.mediaplayer.presentation.pages.mediaplayer.player.MediaplayerSheet
import com.bobbyesp.metadator.mediaplayer.presentation.pages.mediaplayer.player.PlayerAnimationSpec
import com.bobbyesp.metadator.mediastore.presentation.components.card.songs.HorizontalSongCard
import com.bobbyesp.ui.components.bottomsheet.draggable.rememberDraggableBottomSheetState
import com.bobbyesp.ui.components.button.BackButton
import com.bobbyesp.ui.util.isDeviceInLandscape
import kotlinx.coroutines.launch
import my.nanihadesuka.compose.LazyColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSelectionActionable
import my.nanihadesuka.compose.ScrollbarSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaplayerPage(
    viewModel: MediaplayerViewModel,
) {
    val mediaStoreLazyColumnState = rememberLazyListState()
    val isFirstItemVisible by remember { derivedStateOf { mediaStoreLazyColumnState.firstVisibleItemIndex == 0 } }

    val navController = LocalNavController.current

    val songs = viewModel.songsFlow.collectAsStateWithLifecycle(initialValue = emptyList()).value

    val scope = rememberCoroutineScope()

    val density = LocalDensity.current
    val windowsInsets = WindowInsets.systemBars

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val bottomInset = with(density) { windowsInsets.getBottom(density).toDp() }
        val mediaPlayerSheetState = rememberDraggableBottomSheetState(
            dismissedBound = 0.dp,
            collapsedBound = bottomInset + CollapsedPlayerHeight,
            expandedBound = this.maxHeight,
            animationSpec = PlayerAnimationSpec,
        )

        val targetBottom by remember {
            derivedStateOf {
                if (!mediaPlayerSheetState.isDismissed) {
                    CollapsedPlayerHeight + bottomInset
                } else {
                    bottomInset
                }
            }
        }

        val animatedBottom by animateDpAsState(
            targetValue = targetBottom, label = "Animated bottom insets for player sheet"
        )

        val playerAwareWindowInsets by remember {
            derivedStateOf {
                windowsInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                    .add(WindowInsets(bottom = animatedBottom))
            }
        }
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(navigationIcon = {
                    BackButton {
                        navController.navigateBack()
                    }
                }, title = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(id = R.string.mediaplayer).uppercase(),
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.titleLarge.copy(
                                letterSpacing = 4.sp,
                            ),
                        )
                    }
                })
            },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = !isFirstItemVisible,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    FloatingActionButton(onClick = {
                        scope.launch {
                            mediaStoreLazyColumnState.animateScrollToItem(0)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardDoubleArrowUp,
                            contentDescription = stringResource(
                                id = R.string.scroll_to_top
                            )
                        )
                    }
                }
            },
            contentWindowInsets = playerAwareWindowInsets,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if(isDeviceInLandscape()) Modifier.displayCutoutPadding() else Modifier
                    )
                    .padding(it)
            ) {
                LazyColumnScrollbar(
                    state = mediaStoreLazyColumnState,
                    settings = ScrollbarSettings(
                        thumbUnselectedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        thumbSelectedColor = MaterialTheme.colorScheme.primary,
                        selectionActionable = ScrollbarSelectionActionable.WhenVisible,
                    ),
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        state = mediaStoreLazyColumnState,
                    ) {
                        items(
                            count = songs.size,
                            key = { index -> songs[index].id },
                            contentType = { index -> songs[index].id.toString() }) { index ->
                            val song = songs[index]
                            HorizontalSongCard(
                                song = song, modifier = Modifier.animateItem(
                                    fadeInSpec = null, fadeOutSpec = null
                                ), onClick = {
                                    viewModel.playOrderedQueue(song)

                                    if (mediaPlayerSheetState.isDismissed) {
                                        mediaPlayerSheetState.collapseSoft()
                                    }
                                })
                        }
                    }
                }
            }
        }

        MediaplayerSheet(
            state = mediaPlayerSheetState, viewModel = viewModel
        )
    }
}