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
- **Ports** (`*:api`): `TagReader`, `TagWriter`, `TagBackupStore`, `AudioLibrary`, `TrackTagCache`,
  `AudioFileOpener`, `AudioFileInfo`, `WriteAccess`, `MediaIndexer`, `MetadataProvider`,
  `ArtworkDownloader`, `LyricsProvider`, `PlayerController`.
- **Implementations**: TagLib, MediaStore, Room, DataStore, Ktor, Media3.
- **Presentation** (`feature:*`): screens and ViewModels.

The library is MediaStore's, completed from the files (`TagCompletedAudioLibrary`). Android's
scanner does not index every tag of every format: a FLAC's `DATE` never becomes a year, and before
Android 11 there is no album artist or genre at all. A song with a field MediaStore has no value
for has its tags read once, and what was read is cached in Room (`track_tags`) against the file's
modification date and size. MediaStore's values always win; the file only fills the gaps, so
"Needs attention" means the file itself lacks the field.

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
whether they share the window (`LocalPaneContext`), never measure it. The list-detail scaffold is
told not to move the focus to the pane it shows (`paneDirective` in `MetadatorApp`): it would give
it to the library's search field, and open the keyboard, on every rotation to a wide window.

Every transition between screens is in `NavigationMotion`:

- **Forward**: the new screen springs in from the end, over the old one, which moves a quarter of
  the way, shrinks a little and fades. **Backward** is the reverse. Both run on the theme's motion
  scheme: the default spatial spring for what moves and the effects springs for the fades. Not the
  slow spring Material suggests for a full screen: a screen is changed too often to wait for it.
  The springs are given a threshold of one pixel, or the transition, with both screens composed,
  would outlast the motion.
- **Predictive back**: the screen shrinks under the finger and shows the one behind, then leaves
  the way the finger goes. `NavDisplay` seeks this transition by the gesture's progress and plays
  what is left of it once released, so it is tweens of one length: each spring would run through
  its own time under the same finger. With gesture navigation every back goes through it.
  Meanwhile the screen behind is out of focus (`BehindTheGesture`, an entry decorator over
  `Modifier.outOfFocus`): most at the start, sharp once uncovered. The blur is a layer that exists
  only during the gesture, and is driven while drawing.

The entry provider is remembered in `MetadatorApp`. Entries are equal only while their content is
the same instance, and `NavDisplay` finishes a back gesture that was let go only if the scene it
was showing equals the one the stack ends on: otherwise the leaving screen is gone at once.

"Open with Metadator" runs `ExternalEditorActivity` in its own task with only the editor, so
closing it returns to the app that opened the file.

## Theme

`MetadatorTheme` is `MaterialExpressiveTheme` with `MotionScheme.expressive()` (one instance:
Material keys running shape morphs by their spec) and a color scheme from the wallpaper or a seed
(MaterialKolor). Schemes are cached and built off the main thread. The editor and the player nest
`MetadatorAccentTheme`, seeded from the cover's dominant color and harmonized with the app's. That
color comes from `ArtworkAccentSource` (`:core:ui`), which keeps bitmaps out of ViewModels.

A new scheme costs one recomposition of what is under its theme, and nothing else does: what the
theme provides keeps its instance until the scheme changes, so saving an unrelated setting
recomposes nothing. Schemes are swapped, not faded: `ColorScheme` is immutable and its local is
static, so a fade would recompose everything under the theme on every frame. Both activities wait
for the settings before composing, so the default theme is never shown in place of the user's.

Settings keep the 1.x DataStore file name and theme keys, so an update keeps the user's theme.

### Shared components

`:core:designsystem` holds what more than one screen uses, so the expressive vocabulary is the
same everywhere:

- `ConnectedChoices`: a few exclusive options as a connected button group (the library's tabs, the
  theme mode). Used instead of a tab row or a short radio list.
- `ActionMenu`, `PopupMenu`, `PopupMenuGroup`: menus as floating groups. `ActionMenu` takes a list
  of `MenuAction`s and closes itself before running one. Over a screen they are frosted and lifted
  by a blur halo (see Blur).
- `TonalTextField`: the app's text field, a rounded block of tone with its label inside and neither
  an outline nor an indicator line. Stacked outlines turn a form into a grid of boxes; tone keeps
  the fields apart quietly. Focus raises the tone, `emphasized` tints it with the primary color (the
  editor: what will be written on saving), and `TonalFieldDefaults` has the tones for a field in a
  dialog or a sheet. Stacked fields are a group like the app's lists: each takes its `shape` from
  `GroupShapes` (`itemShape`, or `cellShape` where the group has columns too), and
  `GroupShapes.focusedShape` rounds off the one with the focus, on the theme's spring.
- `ToggleChip`, `ActionChip`: a filter or one option of a few, and a chip that does something. The
  first is Material's filter chip with its expressive shapes, a tick under the finger, and a check
  once chosen; the second never looks selected, so it is not read as a filter that is on.
- `FloatingActionToolbar`: the vibrant toolbar with the screen's main action beside it (the editor,
  the library's selection).
- `ActionButtonGroup`: what to do with everything a screen shows, as an expressive button group of
  medium buttons across the width (an album, an artist, a folder: play, edit all, shuffle). The
  primary action keeps its label; the others fall back to their icon where the row is narrow. The
  primary one is filled unless the screen's main action is elsewhere (`prominent = false`: the
  editor's cover, on a screen that saves from its toolbar).
- `ShapedIcon`: an icon on a slowly turning Material shape, for empty states and the About page.
- `PlayingBars`: the three bars on the row of the song that is playing; still while paused.
- `SectionHeader`, `NavigationItem`, `SwitchItem`, `RadioItem`, `PlaceholderCard`: grouped lists
  and what a screen shows instead of content.

- `RolledIn`: something that turns up by rolling into its place through focus, and leaves the same
  way, at the pace of `ThroughFocus`: the album's chip in the player, the pills of the editor's
  "needs attention" notice.

Animations use the theme's motion scheme (`MaterialTheme.motionScheme`): spatial specs for what
moves or changes size, effects specs for fades. They are kept for changes of state the user caused
or should notice: a selection, a song starting or pausing, a tab switching.

### Blur

Material has no blur tokens; `MetadatorBlurDefaults` (`:core:designsystem`) is where the app decides
where it blurs, and by how much. Blur stands in for elevation: where Material would cast a shadow
under something that floats over content, the app blurs instead. There are three kinds of blur, for
three jobs.

**A surface floating over content that moves beneath it is frosted, not shadowed.** What is behind
it shows through, blurred, under its Material container color made translucent. It is
[Haze](https://github.com/chrisbanes/haze): the content is recorded with `Modifier.hazeSource`, and
the surface applies `Modifier.frosted` with `MetadatorBlurDefaults.surfaceStyle(containerRole)`.
The surface's own container becomes transparent and loses its shadow. That is the wide player's
floating controls, and the menus.

- **The source is a sibling of what frosts it, never an ancestor.** A surface inside its own source
  would blur itself.
- **Content passes beneath the surface padded, not inset**, or there is nothing there to frost.
- **Not while a shared element transition runs.** What travels is drawn in the transition's
  overlay, where a frosted surface flickers: it is its solid color until the transition settles.

**What floats is lifted by a halo, not a shadow** (`Modifier.blurHalo`): the content around the
element goes out of focus, most at its edge, and is sharp again a little further out. Where a
shadow darkens, this softens. The halo follows the element's shape, reaches a little further below
it than above, as Material's light does, and is drawn outside the element without changing its
layout. It takes Android 13; below it `MetadatorBlurDefaults.isHaloSupported` is false and the
element keeps its shadow. The player's bar, the floating toolbars and the menus have one.

- **Out of the element's own enter and exit animation**, which would scale, slide and clip the halo
  with the element. Animate its `strength` instead.
- **Only while there is one to draw.** A source records its content only while something blurs it,
  so a halo that is not showing is taken off the chain rather than left at strength zero.

Two things are recorded. The shell records every screen (`LocalBackdropHaze`) for what floats over
all of them: the player's bar, which is its sibling there, and the menus, which are windows of
their own. A screen with a toolbar of its own records its lists itself, since the toolbar is inside
what the shell records.

**What something opens over goes out of focus** (`Modifier.outOfFocus`): blurred with
`Modifier.blur` and dimmed, by a fraction read while drawing so a gesture can drive it. The shell
does it to everything behind the player as it opens.

**What comes and goes as one surface turns into another does so through focus**
(`Modifier.dissolved`): as faint and as blurred as it is absent, sharpening as it shows. It is
`Modifier.blur` too, so unlike a frosted surface it can run during a shared element transition.
The player's sheet does it to what its bar and its full screen do not share.

Below Android 12 nothing blurs: a frosted surface falls back to its container color, nearly opaque,
and what would go out of focus is only dimmed, and more.

## The player

`PlaybackService` is a Media3 `MediaSessionService` with ExoPlayer: background playback, the
system's media notification and lock-screen controls. Its player is built with the audio renderer
and the audio containers only (FLAC, WAV, MP4, AMR, Ogg, Matroska, ADTS, AC-3, AC-4, MP3), so R8
drops the rest of Media3; a new container is one more line there. `Media3PlayerController`
connects to it on first use and exposes a `StateFlow<PlaybackState>`. It is driven from the main
thread, as Media3 requires.

Shuffle is an order of the queue, not a mode of the player: `PlayQueue` (`:player:api`) draws one
random order and keeps it, and the controller puts the player's items in that order around the
song that plays. ExoPlayer's own shuffle mode stays off, since it keeps its order to itself and
the queue shown would not be the one played. Turning shuffle off puts the songs back in the order
they came in.

Both players show the position as a wave that lies flat while paused;
in the full player it is the slider's track.

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
- The song's title, its artist and the play button are tied too, and of each **only the version
  the sheet is going to is ever drawn**, from the first frame. So the rule for each is that its
  two versions look the same in the same bounds: then that swap does not show, and it reads as one
  thing moving and growing. Never both at once with one fading into the other: each version has a
  bounds animation of its own, and seeked as this transition is the two are not in step, so they
  show apart, as a double image.
  - The title and the artist are one line each in both players, and the bar's styles are the full
    player's at a smaller size (`TextStyle.resizedTo`, `TrackNameDefaults`), not other roles of the
    type scale: those differ in weight, line height and tracking, and two texts that differ in any
    of them drift apart glyph by glyph. They are scaled by their height from their start
    (`scaleToBounds(FillHeight, CenterStart)`), so the two coincide for as long as they say the
    same. They are `sharedBounds`, since a `sharedElement` would lay the text out again at every
    width, with no enter and an exit that snaps, which is what draws only one. Both players say
    the same in each line, so it travels whole. It is not clipped to its bounds on the way: on an
    arc they do not keep the shape of the text, and would cut into it. A title too long for its
    line scrolls in the full player instead of wrapping.
  - The album is the full player's alone and is not part of what travels. It is a `LabelChip`
    under the name that rolls into its place the first time the sheet settles (`RolledIn`): up
    from under it, out of focus from the speed and sharp as it stops. Its place is kept meanwhile,
    so nothing below moves. From then on it stays, and goes with the rest of what only the full
    player has as the sheet is dragged down.
  - A new song replaces the old one's pieces through focus, in both players. Its title and artist
    come in a short way from the side (`TrackLine`), the artist a moment after the title; what
    leaves starts slowly and is quickest as it goes, what arrives is quick and slows to a stop.
    Those are tweens on Material's emphasized accelerate and decelerate curves, since a spring
    cannot end at its quickest. Its cover comes into focus over the old one (`TrackArtwork`),
    which blurs beneath it and stays solid until covered. Nothing is clipped on the way but the
    cover, to its own shape: a blur or a slide cut to a rectangle shows the rectangle.
  - The button is a `sharedElement`, laid out again in the changing bounds. `PlayPauseButton`
    fills the size it is given and draws its icon at half its height, and the bar gives it the
    whole 48 dp touch target: left to Material's 40 dp container inside that target, it jumped to
    fill it on the first frame of the transition.
  - The cover steps back while paused, in the full player only. Both covers scale by as much as
    the sheet is open, since a shared element draws only one of the two.
- All three travel on an arc (`ArcAnimationSpec`), not a straight line: sideways first and up late
  on the way to the screen. That way round because the container's top edge rises at a steady pace
  and clips what gets ahead of it.
- What only one of the two has (the bar's next button and progress; everything else on the screen)
  is not shared. It goes through focus (`Modifier.dissolved`), by how far open the sheet is: the
  bar's is gone a fifth of the way up, the screen's sharpens into place between three and eight
  tenths, and in between there is only the surface changing color. Faded in with the surface from
  the start, the whole screen lay half see-through over the bar.
- Shapes are not animated by shared elements. The container's corner and the cover's are values
  of the same transition (`animateDp`), given to the shape and to the container's overlay clip,
  which shared elements inside inherit.
- `PlayerSheetState` holds a `progress` (0 the bar, 1 the screen) and a `SeekableTransitionState`
  that follows it. A drag sets the progress; a release hands it to a spring with the finger's
  velocity. A tap on the bar opens it with a softer spring, since it starts from rest and has the
  whole way to cover. A navigation transition cannot be held halfway like this, which is why the full player
  stopped being a `NavKey`.
- The bounce is the spring running past either end. The transition cannot go past its ends, so
  that excess (`overshoot`) is drawn on the whole sheet: the screen swells slightly, the bar dips.
- The container moves linearly, so its edge stays under the finger. What it carries eases in and
  out. The corners stay round most of the way and straighten late.
- The surface is never see-through: the surface leaving keeps its place while the one arriving
  fades in over it.
- The full player's list shares its gesture through nested scroll: at its top, pulling further
  down pulls the player down. The back gesture previews the collapse through `NavigationEvent`.
- The whole sheet, bar included, is inside one `MetadatorAccentTheme` seeded from the cover. It is
  one surface: a color that differed between its two sizes would change halfway through.
- `PlayerSheetExpansion` tells the shell how far open the sheet is, which is what takes everything
  behind it out of focus (see Blur). Once the sheet covers the screen the shell stops: the sheet is
  opaque, and what is behind it would be blurred on every frame for nobody to see.

### Layouts

The full player has three layouts (`PlayerLayout`). It is not a destination, so nothing tells it
how much room it has: the shell measures the window and passes the layout that fits.

- `Stacked`: one column with the cover, the controls and the queue (`NowPlayingContent`).
- `SideBySide`, when the window is at least 600 dp wide and wider than tall: the song and its
  controls on the left, and on the right the lyrics or the queue, chosen with `ConnectedChoices`
  (`NowPlayingWideContent`).
- `SideBySideCompact`, the same under 600 dp of height (a phone on its side): there is no room for
  the controls under the cover, so they float over the lyrics in a frosted bar.

Each modifier the sheet ties the bar's pieces with (cover, name, play button, drag handle) goes on
exactly one element in every layout. The wide layouts are drawn over the cover, blurred, under a
veil of the surface color. It is blurred small and stretched, which looks the same and costs a
fraction on every frame the lyrics move. Below Android 12 the theme's colors take its place.

### Lyrics

The player shows the lyrics a file has in its `LYRICS` tag and nothing else: `LoadLyricsUseCase`
reads them, and nothing is looked up online from the player. Looking them up is the editor's.

- `SongLyrics.parse` (`:lyrics:api`) reads three things into one model: TTML (`Ttml`), LRC and its
  enhanced form with a mark per word (`Lrc`), and plain text. In `SongLyrics.Synced` every line and
  word has an end: its own, or where the next one starts. A timed line with no text is not shown
  but ends the one before it, which is how files mark an instrumental break.
- TTML is XML from a tag, so it is parsed with document types and external entities refused.
- `NowPlayingViewModel` loads the lyrics and the cover's color on every new song, and again each
  time the player is opened: the tags may have been edited in between.
- `PlaybackClock`: the player reports the position twice a second, too coarse to follow a word.
  The clock counts frames between reports and blends each report in rather than jumping to it. It
  is read only while drawing, and only by the line being sung, so nothing recomposes per frame;
  which line is being sung is a `derivedStateOf` that changes once per line.
- `LyricsPane`: the line being sung comes forward (opacity, scale and the primary color, with an
  effects spec) and the list scrolls it to a third of the way down, also with an effects spec so it
  never bounces. Scrolling by hand stops that until the user asks to go back or leaves the list
  alone for a while. Tapping a line plays from it.
- Where words are timed, the line is drawn by hand from its `TextLayoutResult`, a word at a time:
  in the primary color up to the word being sung and through it in step with it, with a soft edge.
  Each word rises a little as it is sung and stays up, and glows while it is sung and shortly
  after, more on a note that is held: the line shines only where the voice is. Words move apart
  from each other, so each row is split between its words and each part is the whole layout,
  clipped and moved. The glow is the word drawn again with a blurred shadow, which falls behind its
  letters, from a layout of its own: a shadow on the whole line would light every word at once,
  and one cut to a word shows the cut. Left to right is assumed.
