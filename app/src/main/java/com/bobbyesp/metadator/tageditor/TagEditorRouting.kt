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
import com.bobbyesp.ui.motion.slideInVerticallyComposable
import com.bobbyesp.utilities.navigation.parcelableType
import kotlin.reflect.typeOf
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

fun NavGraphBuilder.tagEditorRouting(onNavigateBack: () -> Unit) {
    navigation<Route.UtilitiesNavigator>(
        startDestination = Route.UtilitiesNavigator.TagEditor::class
    ) {
        slideInVerticallyComposable<Route.UtilitiesNavigator.TagEditor>(
            typeMap = mapOf(typeOf<ParcelableSong>() to parcelableType<ParcelableSong>())
        ) {
            val viewModel = koinViewModel<MetadataEditorViewModel>()

            val song = it.toRoute<Route.UtilitiesNavigator.TagEditor>()

            LaunchedEffect(song) {
                viewModel.onEvent(
                    MetadataEditorViewModel.Event.LoadMetadata(song.selectedSong.localPath)
                )
            }

            val state = viewModel.state.collectAsStateWithLifecycle()
            val securityErrorHandler =
                rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartIntentSenderForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        viewModel.onEvent(
                            MetadataEditorViewModel.Event.SaveProperties(
                                song.selectedSong.localPath
                            )
                        )
                        onNavigateBack()
                    }
                }

            LaunchedEffect(true) {
                viewModel.eventFlow.collectLatest { event ->
                    when (event) {
                        is MetadataEditorViewModel.UiEvent.RequestPermission -> {
                            val intent = IntentSenderRequest.Builder(event.intent).build()
                            securityErrorHandler.launch(intent)
                        }

                        is MetadataEditorViewModel.UiEvent.SaveSuccess -> {
                            onNavigateBack()
                        }
                    }
                }
            }

            MetadataEditorPage(
                pageState = state.value,
                receivedAudio = song.selectedSong,
                onEvent = viewModel::onEvent,
            )
        }
    }
}
