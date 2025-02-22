package com.bobbyesp.mediaplayer.domain.enums

import android.provider.MediaStore

enum class MediaStoreSearchFilter(val column: String) {
  TITLE(MediaStore.Audio.Media.TITLE),
  ARTIST(MediaStore.Audio.Media.ARTIST),
  YEAR(MediaStore.Audio.Media.YEAR),
  ALBUM(MediaStore.Audio.Media.ALBUM)
}
