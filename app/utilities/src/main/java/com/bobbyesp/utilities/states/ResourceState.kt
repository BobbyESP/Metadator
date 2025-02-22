package com.bobbyesp.utilities.states

/**
 * A sealed class representing the state of a resource.
 *
 * @param T The type of data held by this state.
 * @property data The data associated with the state, if any.
 * @property message The message associated with the state, if any.
 */
sealed class ResourceState<T>(val data: T? = null, val message: String? = null) {
    /**
     * Represents a loading state with optional partial data.
     *
     * @param T The type of data held by this state.
     * @property partialData The partial data associated with the loading state, if any.
     */
    data class Loading<T>(val partialData: T? = null) : ResourceState<T>(partialData)

    /**
     * Represents a successful state with required data.
     *
     * @param T The type of data held by this state.
     * @property result The result data associated with the successful state.
     */
    data class Success<T>(val result: T) : ResourceState<T>(result)

    /**
     * Represents an error state with a message and optional data.
     *
     * @param T The type of data held by this state.
     * @property errorMessage The error message associated with the error state.
     * @property errorData The data associated with the error state, if any.
     */
    data class Error<T>(val errorMessage: String, val errorData: T? = null) :
        ResourceState<T>(errorData, errorMessage)

    /**
     * Returns a string representation of the resource state.
     *
     * @return A string describing the current state.
     */
    override fun toString(): String {
        return when (this) {
            is Loading -> "Loading(data=$partialData)"
            is Success -> "Success(data=$result)"
            is Error -> "Error(message=$errorMessage, data=$errorData)"
        }
    }
}
