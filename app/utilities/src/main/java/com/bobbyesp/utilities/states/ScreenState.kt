package com.bobbyesp.utilities.states

/**
 * A sealed interface representing the state of a screen.
 *
 * @param T The type of data held by this state.
 */
sealed interface ScreenState<out T> {
    /** Represents a loading state. */
    data object Loading : ScreenState<Nothing>

    /**
     * Represents a successful state with data.
     *
     * @param T The type of data held by this state.
     * @property data The data associated with the successful state.
     */
    data class Success<T>(val data: T?) : ScreenState<T>

    /**
     * Represents an error state with an exception.
     *
     * @property exception The exception associated with the error state.
     */
    data class Error(val exception: Throwable) : ScreenState<Nothing>
}
