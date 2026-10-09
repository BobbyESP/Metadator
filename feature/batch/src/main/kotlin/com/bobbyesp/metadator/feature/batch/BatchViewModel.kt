/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.feature.batch

import androidx.lifecycle.viewModelScope
import com.bobbyesp.metadator.core.common.StringProvider
import com.bobbyesp.metadator.core.domain.batch.BatchEdit
import com.bobbyesp.metadator.core.domain.batch.BatchItem
import com.bobbyesp.metadator.core.domain.batch.BatchProgress
import com.bobbyesp.metadator.core.domain.batch.BatchRunner
import com.bobbyesp.metadator.core.domain.batch.FileNamePattern
import com.bobbyesp.metadator.core.domain.batch.TrackNumbering
import com.bobbyesp.metadator.core.domain.editor.withMultiValueMode
import com.bobbyesp.metadator.core.domain.save.RestoreBackupUseCase
import com.bobbyesp.metadator.core.domain.save.RestoreOutcome
import com.bobbyesp.metadator.core.domain.settings.SettingsRepository
import com.bobbyesp.metadator.core.model.ContentRef
import com.bobbyesp.metadator.core.ui.R as CoreUiR
import com.bobbyesp.metadator.core.ui.viewmodel.BaseViewModel
import com.bobbyesp.metadator.core.ui.viewmodel.UiMessage
import com.bobbyesp.metadator.library.api.AccessResult
import com.bobbyesp.metadator.library.api.AudioFileInfo
import com.bobbyesp.metadator.library.api.AudioLibrary
import com.bobbyesp.metadator.library.api.WriteAccess
import com.bobbyesp.metadator.tags.api.ArtworkChange
import com.bobbyesp.metadator.tags.api.EmbeddedPicture
import com.bobbyesp.metadator.tags.api.PictureType
import com.bobbyesp.metadator.tags.api.TagField
import com.bobbyesp.metadator.tags.api.TagMap
import com.bobbyesp.metadator.tags.api.TagReadResult
import com.bobbyesp.metadator.tags.api.TagReader
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** What the selected files have in a field: one shared value, different ones, or nothing. */
sealed interface CommonValue {
    data class Same(val values: List<String>) : CommonValue

    data object Mixed : CommonValue

    data object Empty : CommonValue
}

data class BatchFile(val item: BatchItem, val title: String, val tags: TagMap)

data class BatchState(
    val loading: Boolean = true,
    val files: List<BatchFile> = emptyList(),
    val common: Map<String, CommonValue> = emptyMap(),
    /** The fields the user set, and to what. Untouched fields keep each file's own value. */
    val edits: Map<String, List<String>> = emptyMap(),
    val cover: ArtworkChange = ArtworkChange.Unchanged,
    val numbering: TrackNumbering? = null,
    val pattern: String = FileNamePattern.Suggestions.first(),
    val usePattern: Boolean = false,
    val progress: BatchProgress? = null,
    val separator: String = "; ",
) {
    val fileNamePattern: FileNamePattern?
        get() = FileNamePattern(pattern).takeIf { usePattern && it.isValid }

    val edit: BatchEdit
        get() =
            BatchEdit(
                fields = edits,
                artwork = cover,
                numbering = numbering,
                fileNamePattern = fileNamePattern,
            )

    /** How the first files would read with the pattern, for a live preview. */
    val patternPreview: List<Pair<String, Map<String, List<String>>?>>
        get() {
            val parsed = FileNamePattern(pattern).takeIf { it.isValid } ?: return emptyList()
            return files.take(3).map { it.item.displayName to parsed.parse(it.item.displayName) }
        }

    val canApply: Boolean
        get() = !edit.isEmpty && files.isNotEmpty() && progress?.isRunning != true
}

sealed interface BatchIntent {
    data class SetField(val key: String, val values: List<String>) : BatchIntent

    data class ResetField(val key: String) : BatchIntent

    data class SetCoverFromUri(val uri: String) : BatchIntent

    data object RemoveCovers : BatchIntent

    data object KeepCovers : BatchIntent

    data class SetNumbering(val numbering: TrackNumbering?) : BatchIntent

    data class SetPattern(val pattern: String) : BatchIntent

    data class UsePattern(val enabled: Boolean) : BatchIntent

    data object Apply : BatchIntent

    data object Cancel : BatchIntent

    data object UndoAll : BatchIntent

    data object Dismiss : BatchIntent
}

sealed interface BatchEffect {
    data object Close : BatchEffect
}

class BatchViewModel(
    private val refs: List<String>,
    private val reader: TagReader,
    private val library: AudioLibrary,
    private val fileInfo: AudioFileInfo,
    private val runner: BatchRunner,
    private val restore: RestoreBackupUseCase,
    private val writeAccess: WriteAccess,
    private val settings: SettingsRepository,
    private val images: CoverReader,
    private val strings: StringProvider,
) : BaseViewModel<BatchIntent, BatchState, BatchEffect>(BatchState()) {

    init {
        runner.clear()
        runner.progress.onEach { setState { copy(progress = it) } }.launchIn(viewModelScope)
        settings.settings
            .onEach { setState { copy(separator = it.multiValueSeparator) } }
            .launchIn(viewModelScope)
        load()
    }

    override fun handleIntent(intent: BatchIntent) {
        when (intent) {
            is BatchIntent.SetField ->
                setState { copy(edits = edits + (intent.key to intent.values)) }
            is BatchIntent.ResetField -> setState { copy(edits = edits - intent.key) }
            is BatchIntent.SetCoverFromUri ->
                launch {
                    val image = images.read(intent.uri)
                    if (image == null)
                        showMessage(UiMessage(strings.get(CoreUiR.string.cover_unreadable)))
                    else
                        setState {
                            copy(
                                cover =
                                    ArtworkChange.Replace(
                                        EmbeddedPicture(
                                            image.first,
                                            image.second,
                                            type = PictureType.FrontCover,
                                        )
                                    )
                            )
                        }
                }
            BatchIntent.RemoveCovers -> setState { copy(cover = ArtworkChange.Remove) }
            BatchIntent.KeepCovers -> setState { copy(cover = ArtworkChange.Unchanged) }
            is BatchIntent.SetNumbering -> setState { copy(numbering = intent.numbering) }
            is BatchIntent.SetPattern -> setState { copy(pattern = intent.pattern) }
            is BatchIntent.UsePattern -> setState { copy(usePattern = intent.enabled) }
            BatchIntent.Apply -> apply()
            BatchIntent.Cancel -> runner.cancel()
            BatchIntent.UndoAll -> undoAll()
            BatchIntent.Dismiss -> {
                runner.clear()
                sendEffect(BatchEffect.Close)
            }
        }
    }

    private fun load() =
        launch(onError = { setState { copy(loading = false) } }) {
            val gate = Semaphore(PARALLEL_READS)
            val files =
                kotlinx.coroutines.coroutineScope {
                    refs
                        .map { uri ->
                            async {
                                gate.withPermit {
                                    val ref = ContentRef(uri)
                                    val track = library.findByRef(ref)
                                    val name =
                                        track?.displayName
                                            ?: fileInfo.describe(ref)?.displayName
                                            ?: uri.substringAfterLast('/')
                                    val tags =
                                        (reader.read(ref, includePictures = false)
                                                as? TagReadResult.Success)
                                            ?.snapshot
                                            ?.tags
                                    tags?.let {
                                        BatchFile(BatchItem(ref, name), track?.title ?: name, it)
                                    }
                                }
                            }
                        }
                        .awaitAll()
                        .filterNotNull()
                }
            setState { copy(loading = false, files = files, common = commonValues(files)) }
        }

    private fun commonValues(files: List<BatchFile>): Map<String, CommonValue> =
        EditableFields.associate { field ->
            val values = files.map { it.tags[field.key] }.distinct()
            field.key to
                when {
                    values.size == 1 && values.single().isEmpty() -> CommonValue.Empty
                    values.size == 1 -> CommonValue.Same(values.single())
                    else -> CommonValue.Mixed
                }
        }

    private fun apply() {
        val state = currentState
        if (!state.canApply) return
        launch {
            val refs = state.files.map { it.item.ref }
            if (!writeAccess.canWrite(refs) && writeAccess.request(refs) != AccessResult.Granted) {
                showMessage(UiMessage(strings.get(R.string.batch_access_denied), long = true))
                return@launch
            }
            val current = settings.settings.first()
            runner.start(state.files.map { it.item }, state.edit) { changes ->
                changes.withMultiValueMode(current.multiValueMode, current.multiValueSeparator)
            }
        }
    }

    private fun undoAll() {
        val backups = currentState.progress?.backups.orEmpty()
        launch {
            val restored = backups.count { restore(it) == RestoreOutcome.Restored }
            showMessage(UiMessage(strings.plural(R.plurals.batch_undone, restored, restored)))
            runner.clear()
            sendEffect(BatchEffect.Close)
        }
    }

    companion object {
        val EditableFields =
            listOf(
                TagField.Artist,
                TagField.Album,
                TagField.AlbumArtist,
                TagField.Date,
                TagField.Genre,
                TagField.Composer,
                TagField.Comment,
            )
        private const val PARALLEL_READS = 4
    }
}
