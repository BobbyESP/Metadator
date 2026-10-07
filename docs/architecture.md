# Architecture

## Modules

Metadator is split by responsibility, not by kind. Each engine has a contract module (`*:api`,
plain Kotlin) and one or more implementations; features see the contracts only.

```
app ──────────────► every module (the only place implementations are named)
feature:* ────────► core:*, *:api
core:domain ──────► core:model, core:common, tags:api, library:api, lookup:api, lyrics:api
tags:taglib ──────► tags:api, library:api (opens files through AudioFileOpener)
lookup:* , lyrics:lrclib ─► their api, core:network
```

Why more modules than a single app module: the engines are interchangeable (TagLib today, maybe
another tag library tomorrow; several metadata sources), and the API/implementation split is what
lets one be swapped in one line of DI (`app/.../di/AppModules.kt`) without touching features. It
also keeps the domain plain Kotlin, so the logic that protects users' files is tested on the JVM
in seconds.

`AndroidFeatureConventionPlugin` enforces the rule: a feature that depends on an implementation
or on another feature fails the build.

## Layers

- **Domain** (`core:domain`): use cases and pure logic. `SaveTagChangesUseCase`,
  `RestoreBackupUseCase`, `LoadTrackUseCase`, `LookupService`, `BatchRunner`, `TrackPositions`,
  `FileNamePattern`, library filtering and grouping.
- **Ports** (`*:api`): `TagReader`, `TagWriter`, `TagBackupStore`, `AudioLibrary`,
  `AudioFileOpener`, `AudioFileInfo`, `WriteAccess`, `MediaIndexer`, `MetadataProvider`,
  `ArtworkDownloader`, `LyricsProvider`, `PlayerController`.
- **Implementations**: TagLib, MediaStore, Room, DataStore, Ktor, Media3.
- **Presentation** (`feature:*`): screens and ViewModels.

Results, not exceptions: everything the user can cause or fix (no permission, file gone, format
not supported, offline) is a sealed result the UI can explain.

## ViewModels

`BaseViewModel<Intent, State, Effect>` (`core:ui`):
- `state`: one immutable `StateFlow`.
- `effects`: commands for whoever is on screen now; dropped if nobody listens, since a command
  carried out later would act on whatever the user is doing by then.
- `messages`: snackbars; buffered until shown, since losing one is a silent failure.

ViewModels never hold a `Context`. `StringProvider` gives them text; ports give them Android work.
System prompts (permissions, MediaStore's write request) are shown by `ActivityResultHost`, to
which each activity lends its launchers in `onCreate`; a use case suspends until the user answers.

## Navigation

One back stack, one Navigation 3 `NavDisplay` (`MetadatorNavDisplay`), keys in
`core:navigation/Keys.kt`. Scene strategies decide the layout: overlays first, then
list-detail, which puts the editor beside the library on wide windows. Destinations are told
whether they share the window (`LocalPaneContext`), never measure it.

"Open with Metadator" runs `ExternalEditorActivity` in its own task with only the editor, so
closing it returns to the app that opened the file.

## Theme

`MetadatorTheme` is `MaterialExpressiveTheme` with `MotionScheme.expressive()` (one instance:
Material keys running shape morphs by their spec) and a color scheme from the wallpaper or a seed
(MaterialKolor). Schemes are cached and built off the main thread. The editor nests
`MetadatorAccentTheme`, seeded from the cover's dominant color and harmonized with the app's.

Settings keep the 1.x DataStore file name and theme keys, so an update keeps the user's theme.

### Shared components

`:core:designsystem` holds what more than one screen uses, so the expressive vocabulary is the
same everywhere:

- `ConnectedChoices`: a few exclusive options as a connected button group (the library's tabs, the
  theme mode). Used instead of a tab row or a short radio list.
- `ActionMenu`, `PopupMenu`, `PopupMenuGroup`: menus as floating groups. `ActionMenu` takes a list
  of `MenuAction`s and closes itself before running one.
- `ShapedIcon`: an icon on a slowly turning Material shape, for empty states and the About page.
- `PlayingBars`: the three bars on the row of the song that is playing; still while paused.
- `SectionHeader`, `NavigationItem`, `SwitchItem`, `RadioItem`, `PlaceholderCard`: grouped lists
  and what a screen shows instead of content.

Animations use the theme's motion scheme (`MaterialTheme.motionScheme`): spatial specs for what
moves or changes size, effects specs for fades. They are kept for changes of state the user caused
or should notice: a selection, a song starting or pausing, a tab switching.

## The player

`PlaybackService` is a Media3 `MediaSessionService` with ExoPlayer: background playback, the
system's media notification and lock-screen controls. `Media3PlayerController` connects to it on
first use and exposes a `StateFlow<PlaybackState>`. It is driven from the main thread, as Media3
requires. The mini player floats over the library; the full player is a destination. Both show
the position as a wave that lies flat while paused; in the full player it is the slider's track.
