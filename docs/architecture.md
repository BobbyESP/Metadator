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
requires. Both players show the position as a wave that lies flat while paused; in the full player
it is the slider's track.

### One surface, two sizes

The player is not a destination. `PlayerSheet` sits in the app shell over the navigation display
and has two states: a bar over the library and collections, and the full screen. Tapping the bar
opens it, and so does dragging it up: the bar's edges follow the finger out to the edges of the
screen while its corners straighten. Pulling the full player down, or back, closes it.

- It is a shared element transition (`SharedTransitionLayout`) between the two contents of an
  `AnimatedContent`: the bar and the full player.
- The bar's surface and the screen's are one `sharedBounds`, the container, in
  `RemeasureToBounds` mode: its bounds are what animates. What is inside is told to ignore that,
  as the library's own clip-reveal sample does: `skipToLookaheadSize()` so it is laid out once, as
  it will be at rest, and for the full player `skipToLookaheadPosition()` too, so it stays where
  it will be and the container opens over it like a window. Without them everything inside is
  laid out again every frame in bounds that are changing, and the shared elements in it start the
  transition by jumping.
- The cover is a `sharedElement`: the same picture in both, so only the arriving one is drawn.
  Both ask Coil for it under one cache key (`ArtworkImage`'s `cacheKey`), so the one arriving
  starts from the picture already loaded.
- The song's name and the play button are `sharedBounds` in the default scale mode: they differ
  between the two, and one hands over to the other halfway. The name's bounds are the text's own
  (centered by its parent, not by `TextAlign`), so it scales without wrapping again.
- Shapes are not animated by shared elements. The container's corner and the cover's are values
  of the same transition (`animateDp`), given to the shape and to the container's overlay clip,
  which shared elements inside inherit.
- `PlayerSheetState` holds a `progress` (0 the bar, 1 the screen) and a `SeekableTransitionState`
  that follows it. A drag sets the progress; a release hands it to a spring with the finger's
  velocity. A navigation transition cannot be held halfway like this, which is why the full player
  stopped being a `NavKey`.
- The bounce is the spring running past either end. The transition cannot go past its ends, so
  that excess (`overshoot`) is drawn on the whole sheet: the screen swells slightly, the bar dips.
- The container moves linearly, so its edge stays under the finger. What it carries eases in and
  out. The corners stay round most of the way and straighten late.
- The surface is never see-through: the content leaving keeps its place while the one arriving
  fades in over it.
- The full player's list shares its gesture through nested scroll: at its top, pulling further
  down pulls the player down. The back gesture previews the collapse through `NavigationEvent`.
