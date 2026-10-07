/*
 * Copyright (C) 2026  Gabriel Fontán (BobbyESP)
 */
package com.bobbyesp.metadator.core.model

enum class TrackSort {
    Title,
    Artist,
    Album,
    DateAdded,
    DateModified,
    Duration,
}

data class SortOrder(val sort: TrackSort = TrackSort.Title, val ascending: Boolean = true)
