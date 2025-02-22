package com.bobbyesp.mediaplayer.data.service

import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

class PlaybackService : KoinComponent, MediaLibraryService() {

  // We inject the ExoPlayer instance
  private val player: ExoPlayer by inject()

  // Callback for MediaLibrarySession
  private val callback =
      object : MediaLibrarySession.Callback {
        // Whatever we need...
      }

  // MediaLibrarySession has to be lazily created because it needs the callback
  private var mediaLibrarySession: MediaLibrarySession? = null

  override fun onCreate() {
    super.onCreate()
    mediaLibrarySession =
        getKoin().get {
          parametersOf(callback)
        } // We pass the callback to the MediaLibrarySession constructor
  }

  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
      mediaLibrarySession

  override fun onDestroy() {
    mediaLibrarySession?.run {
      release()
      mediaLibrarySession = null
    }
    player.release()
    super.onDestroy()
  }
}
