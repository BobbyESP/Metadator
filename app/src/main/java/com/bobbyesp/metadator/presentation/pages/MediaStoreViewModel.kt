package com.bobbyesp.metadator.presentation.pages

import android.content.Context
import androidx.lifecycle.ViewModel
import com.bobbyesp.utilities.mediastore.MediaStoreReceiver.Advanced.observeSongs
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class MediaStorePageViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Context
) : ViewModel() {
    val songsFlow = applicationContext.contentResolver.observeSongs()
}