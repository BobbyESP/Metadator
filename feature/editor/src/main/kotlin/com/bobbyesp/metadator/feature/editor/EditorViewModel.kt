package com.bobbyesp.metadator.feature.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bobbyesp.metadator.core.common.StringProvider
import com.bobbyesp.metadator.core.domain.editor.LoadTrackResult
import com.bobbyesp.metadator.core.domain.editor.LoadTrackUseCase
import com.bobbyesp.metadator.core.domain.editor.LoadedTrack
import com.bobbyesp.metadator.core.domain.editor.Position
import com.bobbyesp.metadator.core.domain.editor.PositionStyle
import com.bobbyesp.metadator.core.domain.editor.TagDraft
import com.bobbyesp.metadator.core.domain.editor.TrackPositions
import com.bobbyesp.metadator.core.domain.editor.withMultiValueMode
import com.bobbyesp.metadator.core.domain.lyrics.FindLyricsUseCase
import com.bobbyesp.metadator.core.domain.save.RestoreBackupUseCase
import com.bobbyesp.metadator.core.domain.save.RestoreOutcome
import com.bobbyesp.metadator.core.domain.save.SaveOutcome
import com.bobbyesp.metadator.core.domain.save.SaveTagChangesUseCase
import com.bobbyesp.metadator.core.domain.settings.SettingsRepository
import com.bobbyesp.metadator.core.model.ContentRef
import com.bobbyesp.metadator.core.model.UserSettings
import com.bobbyesp.metadator.core.ui.viewmodel.BaseViewModel
import com.bobbyesp.metadator.core.ui.viewmodel.UiMessage
import com.bobbyesp.metadator.library.api.AccessResult
import com.bobbyesp.metadator.library.api.WriteAccess
import com.bobbyesp.metadator.lookup.api.ArtworkDownloader
import com.bobbyesp.metadator.lyrics.api.LyricsQuery
import com.bobbyesp.metadator.lyrics.api.LyricsResult
import com.bobbyesp.metadator.player.api.PlayerController
import com.bobbyesp.metadator.tags.api.BackupId
import com.bobbyesp.metadator.tags.api.EmbeddedPicture
import com.bobbyesp.metadator.tags.api.PictureType
import com.bobbyesp.metadator.tags.api.TagField
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

sealed interface EditorStatus {
    data object Loading : EditorStatus

    data object Ready : EditorStatus

    data object NotFound : EditorStatus

    data object AccessDenied : EditorStatus

    data object Unsupported : EditorStatus

    data class Failed(val message: String) : EditorStatus
}

data class EditorState(
    val ref: ContentRef,
    val status: EditorStatus = EditorStatus.Loading,
    val loaded: LoadedTrack? = null,
    val draft: TagDraft? = null,
    val positionStyle: PositionStyle = PositionStyle.Combined,
    val coverInfo: ImageInfo? = null,
    val saving: Boolean = false,
    val findingLyrics: Boolean = false,
    val loadingCover: Boolean = false,
    val settings: UserSettings = UserSettings(),
    val isPlayingThis: Boolean = false,
) {
    val isDirty: Boolean
        get() = draft?.isDirty == true

    val title: String
        get() =
            draft?.tags?.first(TagField.Title.key)?.takeIf { it.isNotBlank() }
                ?: loaded?.track?.title
                ?: loaded?.file?.displayName.orEmpty()

    val artist: String?
        get() = draft?.tags?.get(TagField.Artist.key)?.joinToString(", ")?.ifBlank { null }

    fun position(field: TagField): Position =
        draft?.let { TrackPositions.read(it.tags, field) } ?: Position()
}

sealed interface EditorIntent {
    data object Reload : EditorIntent

    data class SetField(val key: String, val values: List<String>) : EditorIntent

    data class SetPosition(val field: TagField, val position: Position) : EditorIntent

    data class Revert(val keys: List<String>) : EditorIntent

    data object RevertAll : EditorIntent

    data class SetCoverFromUri(val uri: String) : EditorIntent

    data object RemoveCover : EditorIntent

    data object RevertCover : EditorIntent

    data object Save : EditorIntent

    data object FindLyrics : EditorIntent

    data class ApplyLookup(val fields: Map<String, List<String>>, val coverUrl: String?) : EditorIntent

    data object Play : EditorIntent
}

sealed interface EditorEffect {
    data object Close : EditorEffect
}

class EditorViewModel(
    private val ref: ContentRef,
    private val loadTrack: LoadTrackUseCase,
    private val saveChanges: SaveTagChangesUseCase,
    private val restoreBackup: RestoreBackupUseCase,
    private val writeAccess: WriteAccess,
    private val findLyrics: FindLyricsUseCase,
    private val artworkDownloader: ArtworkDownloader,
    private val images: ImageSource,
    private val settingsRepository: SettingsRepository,
    private val player: PlayerController,
    private val strings: StringProvider,
    private val savedState: SavedStateHandle,
) : BaseViewModel<EditorIntent, EditorState, EditorEffect>(EditorState(ref)) {

    init {
        settingsRepository.settings.onEach { setState { copy(settings = it) } }.launchIn(viewModelScope)
        player.state
            .onEach { playback ->
                setState { copy(isPlayingThis = playback.isPlaying && playback.current?.ref == ref) }
            }
            .launchIn(viewModelScope)
        load(restorePending = true)
    }

    override fun handleIntent(intent: EditorIntent) {
        when (intent) {
            EditorIntent.Reload -> load(restorePending = false)
            is EditorIntent.SetField -> editDraft { set(intent.key, intent.values) }
            is EditorIntent.SetPosition -> {
                val draft = currentState.draft ?: return
                val values =
                    TrackPositions.write(draft.tags, intent.field, intent.position, currentState.positionStyle)
                editDraft { setAll(values) }
            }
            is EditorIntent.Revert -> editDraft { intent.keys.fold(this) { draft, key -> draft.revert(key) } }
            EditorIntent.RevertAll -> editDraft { revertAll() }
            is EditorIntent.SetCoverFromUri -> setCover { images.read(intent.uri)?.let { it.bytes to it.mimeType } }
            EditorIntent.RemoveCover -> editDraft { removeCover() }
            EditorIntent.RevertCover -> editDraft { revertCover() }
            EditorIntent.Save -> save()
            EditorIntent.FindLyrics -> findLyrics()
            is EditorIntent.ApplyLookup -> applyLookup(intent)
            EditorIntent.Play -> play()
        }
    }

    private fun load(restorePending: Boolean) =
        launch(onError = { setState { copy(status = EditorStatus.Failed(it.message.orEmpty())) } }) {
            setState { copy(status = EditorStatus.Loading) }
            when (val result = loadTrack(ref)) {
                is LoadTrackResult.Loaded -> {
                    val loaded = result.track
                    var draft = TagDraft(loaded.snapshot)
                    if (restorePending) restorePendingFields()?.let { draft = draft.setAll(it) }
                    setState {
                        copy(
                            status = EditorStatus.Ready,
                            loaded = loaded,
                            draft = draft,
                            positionStyle =
                                TrackPositions.detectStyle(loaded.snapshot.tags, loaded.fileExtension),
                        )
                    }
                    inspectCover()
                }
                LoadTrackResult.NotFound -> setState { copy(status = EditorStatus.NotFound) }
                LoadTrackResult.AccessDenied -> setState { copy(status = EditorStatus.AccessDenied) }
                LoadTrackResult.Unsupported -> setState { copy(status = EditorStatus.Unsupported) }
                is LoadTrackResult.Failed -> setState { copy(status = EditorStatus.Failed(result.message)) }
            }
        }

    private fun editDraft(transform: TagDraft.() -> TagDraft) {
        val draft = currentState.draft ?: return
        val coverBefore = draft.cover
        val edited = draft.transform()
        setState { copy(draft = edited) }
        persistPendingFields(edited)
        if (edited.cover !== coverBefore) inspectCover()
    }

    private fun setCover(source: suspend () -> Pair<ByteArray, String>?) =
        launch(onError = { setState { copy(loadingCover = false) } }) {
            setState { copy(loadingCover = true) }
            val picked = source()
            setState { copy(loadingCover = false) }
            if (picked == null) {
                showMessage(UiMessage(strings.get(R.string.cover_unreadable)))
                return@launch
            }
            val (bytes, mime) = picked
            editDraft { replaceCover(EmbeddedPicture(bytes, mime, type = PictureType.FrontCover)) }
        }

    private fun inspectCover() =
        launch {
            val cover = currentState.draft?.cover
            setState { copy(coverInfo = null) }
            if (cover != null) {
                val info = images.inspect(cover.data)
                if (currentState.draft?.cover === cover) setState { copy(coverInfo = info) }
            }
        }

    private fun save() {
        val draft = currentState.draft ?: return
        if (!draft.isDirty || currentState.saving) return
        launch(
            onError = {
                setState { copy(saving = false) }
                showMessage(UiMessage(strings.get(R.string.save_failed, it.message.orEmpty()), long = true))
            }
        ) {
            setState { copy(saving = true) }
            val settings = settingsRepository.settings.first()
            val changes = draft.changes.withMultiValueMode(settings.multiValueMode, settings.multiValueSeparator)

            if (!writeAccess.canWrite(listOf(ref)) && writeAccess.request(listOf(ref)) != AccessResult.Granted) {
                setState { copy(saving = false) }
                showMessage(UiMessage(strings.get(R.string.write_access_denied), long = true))
                return@launch
            }

            var outcome = saveChanges(ref, changes)
            // Android 10 only offers its prompt after refusing a write.
            if (outcome == SaveOutcome.NeedsAccess && writeAccess.request(listOf(ref)) == AccessResult.Granted) {
                outcome = saveChanges(ref, changes)
            }
            handleSaveOutcome(outcome)
        }
    }

    private suspend fun handleSaveOutcome(outcome: SaveOutcome) {
        when (outcome) {
            is SaveOutcome.Saved -> {
                clearPendingFields()
                reloadAfterWrite()
                settingsRepository.update { it.copy(successfulSaves = it.successfulSaves + 1) }
                val text =
                    if (outcome.notStored.isEmpty()) strings.get(R.string.saved)
                    else strings.get(R.string.saved_some_unsupported, outcome.notStored.joinToString(", "))
                showMessage(
                    UiMessage(
                        text = text,
                        actionLabel = strings.get(R.string.undo),
                        onAction = { undo(outcome.backupId) },
                    )
                )
            }
            SaveOutcome.NothingToSave -> setState { copy(saving = false) }
            SaveOutcome.NeedsAccess -> {
                setState { copy(saving = false) }
                showMessage(UiMessage(strings.get(R.string.write_access_denied), long = true))
            }
            SaveOutcome.FileGone -> {
                setState { copy(saving = false, status = EditorStatus.NotFound) }
            }
            SaveOutcome.Unsupported -> {
                setState { copy(saving = false) }
                showMessage(UiMessage(strings.get(R.string.format_unsupported), long = true))
            }
            is SaveOutcome.Failed -> {
                setState { copy(saving = false) }
                showMessage(
                    UiMessage(
                        strings.get(
                            if (outcome.restored) R.string.save_failed_restored else R.string.save_failed,
                            outcome.message,
                        ),
                        long = true,
                    )
                )
            }
        }
    }

    /** After a write, the editor shows what the file really holds now. */
    private suspend fun reloadAfterWrite() {
        when (val result = loadTrack(ref)) {
            is LoadTrackResult.Loaded ->
                setState {
                    copy(
                        saving = false,
                        loaded = result.track,
                        draft = TagDraft(result.track.snapshot),
                    )
                }
            else -> setState { copy(saving = false) }
        }
        inspectCover()
    }

    private fun undo(backupId: BackupId) =
        launch {
            val message =
                when (restoreBackup(backupId)) {
                    RestoreOutcome.Restored -> {
                        reloadAfterWrite()
                        R.string.undone
                    }
                    RestoreOutcome.BackupGone -> R.string.undo_unavailable
                    RestoreOutcome.NeedsAccess -> R.string.write_access_denied
                    RestoreOutcome.FileGone -> R.string.file_gone_title
                    is RestoreOutcome.Failed -> R.string.undo_failed
                }
            showMessage(UiMessage(strings.get(message)))
        }

    private fun findLyrics() {
        val state = currentState
        val draft = state.draft ?: return
        val title = draft.tags.first(TagField.Title.key).orEmpty()
        val artist = draft.tags[TagField.Artist.key].firstOrNull().orEmpty()
        if (title.isBlank() || artist.isBlank()) {
            showMessage(UiMessage(strings.get(R.string.lyrics_need_title_artist)))
            return
        }
        launch(onError = { setState { copy(findingLyrics = false) } }) {
            setState { copy(findingLyrics = true) }
            val result =
                findLyrics(
                    LyricsQuery(
                        title = title,
                        artist = artist,
                        album = draft.tags.first(TagField.Album.key),
                        durationMs = state.loaded?.audio?.durationMs ?: state.loaded?.track?.durationMs,
                    )
                )
            setState { copy(findingLyrics = false) }
            when (result) {
                is LyricsResult.Found -> {
                    val text = result.lyrics.preferred
                    if (text == null) {
                        showMessage(UiMessage(strings.get(R.string.lyrics_instrumental)))
                    } else {
                        editDraft { set(TagField.Lyrics.key, listOf(text)) }
                        showMessage(
                            UiMessage(
                                strings.get(
                                    if (result.lyrics.synced.isNullOrBlank()) R.string.lyrics_found_plain
                                    else R.string.lyrics_found_synced
                                ),
                                actionLabel = strings.get(R.string.undo),
                                onAction = { handleIntent(EditorIntent.Revert(listOf(TagField.Lyrics.key))) },
                            )
                        )
                    }
                }
                LyricsResult.NotFound -> showMessage(UiMessage(strings.get(R.string.lyrics_not_found)))
                LyricsResult.Offline -> showMessage(UiMessage(strings.get(R.string.offline)))
                is LyricsResult.Failed -> showMessage(UiMessage(strings.get(R.string.lyrics_failed)))
            }
        }
    }

    private fun applyLookup(intent: EditorIntent.ApplyLookup) {
        if (intent.fields.isNotEmpty()) editDraft { setAll(intent.fields) }
        val url = intent.coverUrl
        if (url != null) {
            setCover { artworkDownloader.download(url)?.let { it.bytes to it.mimeType } }
        }
        showMessage(UiMessage(strings.get(R.string.lookup_applied)))
    }

    private fun play() {
        val track = currentState.loaded?.track
        if (track == null) {
            showMessage(UiMessage(strings.get(R.string.play_unavailable)))
            return
        }
        if (currentState.isPlayingThis) player.togglePlayPause() else player.play(listOf(track))
    }

    /*
     * The text changes survive process death: the user can leave mid-edit and come back to them.
     * A new cover does not; its bytes are too large for saved state.
     */

    private val pendingSerializer = MapSerializer(String.serializer(), ListSerializer(String.serializer()))

    private fun persistPendingFields(draft: TagDraft) {
        savedState[KEY_PENDING] =
            if (draft.changes.fields.isEmpty()) null else Json.encodeToString(pendingSerializer, draft.changes.fields)
    }

    private fun restorePendingFields(): Map<String, List<String>>? =
        savedState.get<String>(KEY_PENDING)?.let { runCatching { Json.decodeFromString(pendingSerializer, it) }.getOrNull() }

    private fun clearPendingFields() {
        savedState[KEY_PENDING] = null
    }

    private companion object {
        const val KEY_PENDING = "pending_fields"
    }
}
