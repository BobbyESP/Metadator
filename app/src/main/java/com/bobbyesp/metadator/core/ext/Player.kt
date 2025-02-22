package com.bobbyesp.metadator.core.ext

import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Timeline

fun Player.getQueueWindows(): List<Timeline.Window> {
  val timeline = currentTimeline
  if (timeline.isEmpty) {
    return emptyList()
  }
  val queue = ArrayDeque<Timeline.Window>()
  val queueSize = timeline.windowCount

  val currentMediaItemIndex: Int = currentMediaItemIndex
  queue.add(timeline.getWindow(currentMediaItemIndex, Timeline.Window()))

  var firstMediaItemIndex = currentMediaItemIndex
  var lastMediaItemIndex = currentMediaItemIndex
  val shuffleModeEnabled = shuffleModeEnabled
  while ((firstMediaItemIndex != C.INDEX_UNSET || lastMediaItemIndex != C.INDEX_UNSET) &&
      queue.size < queueSize) {
    if (lastMediaItemIndex != C.INDEX_UNSET) {
      lastMediaItemIndex =
          timeline.getNextWindowIndex(lastMediaItemIndex, REPEAT_MODE_OFF, shuffleModeEnabled)
      if (lastMediaItemIndex != C.INDEX_UNSET) {
        queue.add(timeline.getWindow(lastMediaItemIndex, Timeline.Window()))
      }
    }
    if (firstMediaItemIndex != C.INDEX_UNSET && queue.size < queueSize) {
      firstMediaItemIndex =
          timeline.getPreviousWindowIndex(firstMediaItemIndex, REPEAT_MODE_OFF, shuffleModeEnabled)
      if (firstMediaItemIndex != C.INDEX_UNSET) {
        queue.addFirst(timeline.getWindow(firstMediaItemIndex, Timeline.Window()))
      }
    }
  }
  return queue.toList()
}
