package com.bobbyesp.metadator.core.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * MVI for every screen: intents in, one immutable [state] out.
 * - [effects] are commands for whoever is on screen now (close, navigate) and are dropped when
 *   nobody listens: a command nobody took no longer applies.
 * - [messages] are for the user (snackbars) and wait until somebody shows them: losing one is a
 *   silent failure.
 */
abstract class BaseViewModel<Intent : Any, State : Any, Effect : Any>(initialState: State) :
    ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<State> = _state.asStateFlow()

    protected val currentState: State
        get() = _state.value

    private val _effects =
        MutableSharedFlow<Effect>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val effects: SharedFlow<Effect> = _effects.asSharedFlow()

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages: Flow<UiMessage> = _messages.receiveAsFlow()

    fun onIntent(intent: Intent) = handleIntent(intent)

    protected abstract fun handleIntent(intent: Intent)

    protected fun setState(reducer: State.() -> State) = _state.update { it.reducer() }

    protected fun sendEffect(effect: Effect) {
        _effects.tryEmit(effect)
    }

    protected fun showMessage(message: UiMessage) {
        viewModelScope.launch { _messages.send(message) }
    }

    /** Launches [block]; a failure goes to [onError] instead of crashing the app. */
    protected fun launch(onError: (Throwable) -> Unit = {}, block: suspend CoroutineScope.() -> Unit): Job =
        viewModelScope.launch {
            try {
                block()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                onError(error)
            }
        }
}
