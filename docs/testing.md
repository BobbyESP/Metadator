# Testing

## What is tested where

| Level | What | Where |
|---|---|---|
| JVM unit | Tag merge and diff, pictures, multi-values (`TagChangesTest`) | `tags/api` |
| JVM unit | Save, verify, restore, failure paths, with in-memory fakes of TagLib's behaviour (`SaveTagChangesUseCaseTest`) | `core/domain` |
| JVM unit | Track positions, drafts, multi-value mode, library search/sort/grouping, completing the library from the files, file-name patterns, batch numbering, lookup merging and proposals | `core/domain` |
| JVM unit | MusicBrainz, Deezer and LRCLIB parsing and error mapping, with Ktor's `MockEngine` | `lookup/*`, `lyrics/lrclib` |
| JVM unit | Ranking, LRC parsing, text normalization | `lookup/api`, `lyrics/api`, `core/common` |
| JVM unit | The order of the queue, shuffled and back (`PlayQueueTest`) | `player/media3` |

Run them all with `./gradlew test`. CI runs them on every pull request (see [ci.md](ci.md)).

## Conventions

- Ports are tested with fakes, not mocks of the framework. `FakeTagFiles` behaves like TagLib:
  a write replaces the whole property map, and keys a format cannot hold are dropped.
- A bug gets a failing test before its fix.

## Not yet verified

The Android layers and the UI were written without a device. Before release, check on a phone
and a tablet, on Android 8, 10 and the latest version:

- Round trip per format (MP3, FLAC, M4A, Ogg, Opus, WAV): edit one field, save, and check with
  another tag editor that every other tag and picture is unchanged. This is the most important
  check and the next automated test to write (instrumented, `tags/taglib/src/androidTest`).
- Write access: a song from the library on Android 10 and 11+, a batch of 20 songs (one prompt on
  11+), a file opened from a file manager, a file shared read-only.
- Undo after a save and after a batch.
- Process death in the editor with unsaved changes ("Don't keep activities").
- The list-detail layout on a tablet or a foldable, TalkBack on the library and the editor.
- Playback in the background, the media notification, and stopping when the app is swiped away
  while paused.
