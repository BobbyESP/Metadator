# Saving tags

Saving is where 1.x lost users' data, so this path has the strictest rules and the most tests
(`core/domain/src/test/.../save/SaveTagChangesUseCaseTest.kt`).

## What 1.x got wrong

- It wrote a property map built from 15 known fields. TagLib's `setProperties` removes every key
  missing from the map, so every save deleted ReplayGain, MusicBrainz ids, BPM, ISRC and any custom
  tag.
- Every save re-embedded a cover, from MediaStore's (often downscaled) album-art cache, replacing
  every picture in the file.
- Multi-value fields were joined with ", " and split on ",", so "Tyler, The Creator" became two
  artists.

## How 2.0 saves

The editor keeps a `TagDraft`: the snapshot it read and the user's edits. What gets saved is the
difference, `TagChanges`: changed keys (an empty list removes a key) and an `ArtworkChange`
(`Unchanged`, `Replace`, `Remove`).

`SaveTagChangesUseCase`:
1. Reads the file as it is now, so changes made elsewhere since the editor opened survive.
2. Applies the changed keys to that map. Untouched keys keep their exact values.
3. Pictures: not written at all when the cover didn't change. Replacing or removing the cover
   touches only the front cover; back covers, artist pictures and the rest stay.
4. Backs up the file's tags and pictures (`TagBackupStore`, Room plus picture files, the newest
   100 kept).
5. Writes. If the write fails, it writes the backup back; if that fails too, the backup is kept.
6. Reads again and reports changed keys the format did not store (`SaveOutcome.Saved.notStored`).
7. Asks MediaStore to rescan the file, so the library and other players see the new tags.

Undo (`RestoreBackupUseCase`) writes a backup back and rescans.

Multi-value fields are lists from the file to the chips in the editor and back. The only place
values are joined is the user's choice in Settings ("Join them into one value"), applied to the
fields they changed, just before writing.

Track and disc totals follow the format: `3/12` in one field for ID3 and MP4, a separate
`TRACKTOTAL` for Vorbis comments (FLAC, Ogg, Opus), or whatever key the file already uses
(`TrackPositions`).

## Write access

Android asks differently on each version (`MediaStoreWriteAccess`):
- up to 9: the storage permission, once;
- 10: a prompt per file, which MediaStore only offers after refusing a write
  (`RecoverableAccessCache` keeps it until shown);
- 11 and later: `MediaStore.createWriteRequest`, one prompt for every file of a batch, before
  anything is written.

A file opened from another app with write access needs nothing; one shared read-only cannot be
written, and the editor says so.

## Process death

The editor's text changes are kept in the `SavedStateHandle` as JSON and reapplied on a freshly
read file. A new cover is not: its bytes are too large for saved state.
