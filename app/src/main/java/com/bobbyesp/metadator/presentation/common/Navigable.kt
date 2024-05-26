package com.bobbyesp.metadator.presentation.common

interface Navigable {
    fun getDestinationTitle(): Int
}

//We can use this to simplify some things in the future
//Example:
/**
 * val destination = MainHost
 * val title = destination.getDestinationTitle()
 */

/**
 * @Serializable
 * object MainHost : Navigable {
 *     override fun getDestinationTitle() = R.string.app_name
 * }
 */