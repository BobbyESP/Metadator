package com.bobbyesp.metadator.tageditor

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import com.bobbyesp.metadator.core.domain.model.ParcelableSong
import com.bobbyesp.metadator.core.presentation.common.Route
import com.bobbyesp.metadator.tageditor.presentation.pages.tageditor.MetadataEditorPage
import com.bobbyesp.metadator.tageditor.presentation.pages.tageditor.MetadataEditorViewModel
import com.bobbyesp.metadator.tageditor.presentation.pages.tageditor.spotify.MetadataBottomSheetViewModel
import com.bobbyesp.ui.motion.slideInVerticallyComposable
import com.bobbyesp.utilities.navigation.parcelableType
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import kotlin.reflect.typeOf

fun NavGraphBuilder.tagEditorRouting(
    onNavigateBack: () -> Unit
) {
    navigation<Route.UtilitiesNavigator>(
        startDestination = Route.UtilitiesNavigator.TagEditor::class,
    ) {
        slideInVerticallyComposable<Route.UtilitiesNavigator.TagEditor>(
            typeMap = mapOf(typeOf<ParcelableSong>() to parcelableType<ParcelableSong>()),
        ) {
            val song = it.toRoute<Route.UtilitiesNavigator.TagEditor>()

            val viewModel = koinViewModel<MetadataEditorViewModel>()
            val bsViewModel = koinViewModel<MetadataBottomSheetViewModel>()

            val state = viewModel.state.collectAsStateWithLifecycle()
            val bsState = bsViewModel.viewStateFlow.collectAsStateWithLifecycle()
            val securityErrorHandler =
                rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartIntentSenderForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        viewModel.savePropertyMap(
                            audioPath = song.selectedSong.localPath
                        )
                        onNavigateBack()
                    }
                }

            LaunchedEffect(true) {
                viewModel.eventFlow.collectLatest { event ->
                    when (event) {
                        is MetadataEditorViewModel.UiEvent.RequestPermission -> {
                            val intent =
                                IntentSenderRequest.Builder(event.intent)
                                    .build()
                            securityErrorHandler.launch(intent)
                        }

                        is MetadataEditorViewModel.UiEvent.SaveSuccess -> {
                            onNavigateBack()
                        }
                    }
                }
            }

            LaunchedEffect(true) {
                bsViewModel.outerEventsFlow.collectLatest { event ->
                    when (event) {
                        is MetadataBottomSheetViewModel.OuterEvent.SaveMetadata -> {
                            event.modifiedFields.forEach { field ->
                                viewModel.onEvent(
                                    MetadataEditorViewModel.Event.UpdateProperty(
                                        field.key,
                                        field.value
                                    )
                                )
                            }
                        }
                    }
                }
            }

            MetadataEditorPage(
                state = state,
                bsViewState = bsState,
                receivedAudio = song.selectedSong,
                onBsEvent = bsViewModel::onEvent,
                onEvent = viewModel::onEvent
            )
        }
    }
}