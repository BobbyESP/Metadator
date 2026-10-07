# Metadator 2.0 — Renovation plan

This plan covers a near-complete rewrite of Metadator. It keeps what the current app already does
well (TagLib-based reading and writing, the MediaStore library, Material You theming) and rebuilds
everything else around three goals:

1. **A user experience that earns good reviews.** Editing a song's tags should be fast and
   predictable, and the app should never lose data.
2. **A clean Material 3 Expressive interface** that follows the current guidelines instead of
   imitating them with custom components.
3. **A modular, testable architecture**, using the conventions already proven in Docucraft so
   that both projects work the same way.

All code, documentation, commits and UI copy are in English. Spanish stays as a translation.

> **Decisions taken (October 2026).** The questions in [section 13](#13-decisions-to-confirm-before-starting)
> were settled as follows, and 2.0 is built on the `v2` branch:
> - **Player:** kept, as a simple one (queue, background playback, media notification, mini player
>   and a full screen), instead of an inline preview only.
> - **Spotify:** removed. MusicBrainz with the Cover Art Archive and Deezer are the sources; neither
>   needs a key.
> - **minSdk:** 26.
> - **No 1.0.1 hotfix:** 1.x has been unmaintained for over a year, so the fixes ship in 2.0.
> - **TagLib:** Kyant0's binding moved from JitPack to Maven Central (`io.github.kyant0:taglib`).
>
> The current state of each area is in [README.md](README.md#status-of-20).

---

## Contents

1. [Where the app stands today](#1-where-the-app-stands-today)
2. [Product and UX vision](#2-product-and-ux-vision)
3. [Design system: Material 3 Expressive](#3-design-system-material-3-expressive)
4. [Architecture](#4-architecture)
5. [Module structure](#5-module-structure)
6. [Technology stack and library updates](#6-technology-stack-and-library-updates)
7. [Data-safety rules for writing tags](#7-data-safety-rules-for-writing-tags)
8. [Screen-by-screen specification](#8-screen-by-screen-specification)
9. [Delivery phases](#9-delivery-phases)
10. [Testing strategy](#10-testing-strategy)
11. [Release and Play Store strategy](#11-release-and-play-store-strategy)
12. [Risks](#12-risks)
13. [Decisions to confirm before starting](#13-decisions-to-confirm-before-starting)

---

## 1. Where the app stands today

### 1.1 Shape of the codebase

About 17,800 lines across five Gradle modules: `:app`, `:app:ui`, `:app:utilities`,
`:app:mediaplayer` and `:crashhandler`. The modules split the code by *kind* ("UI", "utilities"),
not by responsibility. Because of that:

- `:app:utilities` mixes MediaStore queries, Compose permission dialogs, navigation helpers,
  Parcelable models and string extensions. Every module depends on it, and it depends on Compose.
- `:app:ui` holds about 60 generic components. Many of them reimplement things Material 3 now
  ships: a dropdown implementation (`DropdownMenuImplementation.kt`, 490 lines), pull-to-refresh,
  a collapsible top bar, a draggable bottom sheet and marquee text.
- Feature code in `:app` has no domain layer. ViewModels hold a `Context`, call TagLib and
  `ContentResolver` directly, and expose Compose types (`SnapshotStateMap`) in their state.
- There are no real tests. Every test file is the template's `ExampleUnitTest` or
  `ExampleInstrumentedTest`.

### 1.2 Bugs and risks found during the audit

These matter for the plan because several of them **lose user data**, which is the fastest way to
get one-star reviews on a tag editor.

| # | Severity | Finding | Where |
|---|---|---|---|
| A1 | **Data loss** | Saving writes a map with only 15 keys through `TagLib.savePropertyMap`. TagLib's `setProperties` removes every property missing from the map, so saving deletes all other tags: BPM, ISRC, ReplayGain, MusicBrainz IDs, custom fields and so on. | `MetadataEditorViewModel.kt:174`, `AudioFileMetadata.toPropertyMap()` |
| A2 | **Data loss** | *Save* always re-embeds artwork, even if the user never touched it. It uses MediaStore's cached album-art URI (often a downscaled copy) or `Uri.EMPTY`, and replaces every embedded picture, back covers included, with a single "Front cover". | `MetadataEditorPage.kt:234`, `MetadataEditorViewModel.kt:232` |
| A3 | **Data loss** | Multi-value fields are joined with `", "` for display and split on `","` when saved. An artist like "Tyler, The Creator" is silently split into two artists. | `utilities/ext/String.kt:5`, `PropertyMap.toModifiableMap` |
| A4 | Crash | The start destination is resolved in a coroutine on `Dispatchers.IO`, while `setContent` reads it synchronously and throws if it is still `null`. The splash screen hides the race but does not prevent it. | `MainActivity.kt:95` |
| A5 | Crash | Library search puts the search term straight into the SQL (`LIKE '%$searchTerm%'`) **and** passes selection arguments that have no `?` placeholders. Any search crashes, and a quote in the term breaks the query. Search isn't exposed in the UI today, which is the only reason this hasn't shown up. | `MediaStoreReceiver.kt:35,40` |
| A6 | ANR | Saving runs synchronously on the main thread (`onEvent` → `savePropertyMap`). On large FLAC files this can freeze the UI. | `MetadataEditorViewModel.kt:337,367` |
| A7 | Broken flow | After the user grants write access (`RecoverableSecurityException`), only the text tags are retried; a new cover is dropped without telling the user. | `TagEditorRouting.kt:44` |
| A8 | UX | Saving pops the screen immediately. There's no confirmation, no undo, and no rescan, so the library keeps showing the old title until MediaStore notices on its own. | `TagEditorRouting.kt`, no `MediaScannerConnection` use anywhere |
| A9 | UX | Leaving the editor discards changes without asking. The `unsaved_changes` strings exist but are never used. | `strings.xml` |
| A10 | UX | Denying the audio permission closes the app (`context?.finish()`). | `HomePage.kt:256` |
| A11 | UX | *Appearance* and *About* in Settings open blank screens. *About*'s subtitle is the literal text "PLACEHOLDER". | `SettingsRouting.kt:26,30`, `strings.xml` |
| A12 | Security | The Spotify client secret is compiled into the APK through `BuildConfig`, so anyone can extract it. | `app/build.gradle.kts`, `SpotifyModule.kt` |
| A13 | Build | The release signing config uses the key alias as the key password. | `app/build.gradle.kts:51` |
| A14 | Architecture | Songs are identified by absolute file path (`MediaStore.Audio.Media.DATA`, deprecated), and the whole `ParcelableSong` is serialized into the navigation route. | `Route.kt`, `MediaStoreReceiver.kt` |
| A15 | Scope | A full background music player (Media3 session service, notification, queue, shuffle and repeat) is started in `onStart` of every session. A tag editor doesn't need it, and it adds two foreground-service permissions. | `MainActivity.kt`, `:app:mediaplayer` |

### 1.3 What is worth keeping

- **Kyant0's TagLib binding.** It is the right engine; it just needs to sit behind an interface.
- The idea of **observing MediaStore through a `ContentObserver` flow**
  (`ContentResolverObserver.kt`).
- **MaterialKolor-based theming** (seed color, palette style, dynamic color).
- The **Spanish translation**, the crash handler for FOSS builds, and the `Version` model in the
  root build script.
- The **package name, signing key and Play listing**. 2.0 ships as an update, not a new app.

Everything else is rewritten. Small helpers can be copied over when a new module needs them, but
nothing is migrated as-is.

---

## 2. Product and UX vision

### 2.1 Who uses Metadator and what they want

| User | Typical session | What they care about |
|---|---|---|
| **The fixer** (most installs) | Opens the app because one song shows "Unknown artist" or has no cover in their player. | Find that song fast, fix it, see the fix in their player. Under a minute. |
| **The collector** | Curates a local library of FLAC/MP3 files, often whole albums at a time. | Batch editing, consistent album artist, track numbers, high-resolution covers, keeping existing tags intact. |
| **The lyrics fan** | Wants synced lyrics embedded so their player shows them. | One-tap lyrics lookup, a simple sync editor. |

Today the app serves none of these well: there's no search, no batch editing, and saving can
damage files. 2.0 is built around these three journeys.

### 2.2 UX principles

1. **Never lose data.** Write only what the user changed, keep everything else, and always offer
   undo. See [section 7](#7-data-safety-rules-for-writing-tags).
2. **Shortest path to the song.** Search is the main entry point of the library, not an extra.
   The app can also be opened from a file manager or another player ("Open with Metadator") with
   no library permission at all.
3. **Edit in place, confirm in place.** Saving keeps the user on the editor and shows a snackbar
   with *Undo*. Leaving with unsaved changes asks first, and predictive back shows the dialog
   before the screen leaves.
4. **Suggestions never overwrite on their own.** Online lookup proposes values; the user picks
   field by field (a diff view), and nothing is written until they press *Save*.
5. **Show the state of the library.** Missing covers, missing album artists or empty years are
   visible as filters ("Needs attention"), so the collector knows what to fix next.
6. **Ask for permissions in context.** No permission carousel at first launch. The library asks
   when it's opened, and explains why. If the user says no, the app still works for files opened
   from elsewhere.
7. **Adaptive by default.** On tablets, foldables and Chromebooks, the library and the editor sit
   side by side (list-detail).
8. **Accessible.** Every action has a content description, touch targets are at least 48 dp,
   contrast holds in all three contrast levels, and the editor can be used fully with TalkBack and
   a keyboard.

### 2.3 What is removed

- **The full music player** (the `:app:mediaplayer` module, the foreground service, the queue and
  the notification). It is replaced by an **inline preview** in the editor: play, pause and seek
  the song being edited, using ExoPlayer with no service. It stops when the editor closes. If the
  full player is kept, it should become its own optional feature later, not part of the core.
- **Layout and card-size options that don't change what the user can do.** There is one well
  designed list, plus a grid for the album view.
- **The marquee-text and reduce-shadows settings.** Expressive surfaces are tonal, not shadowed,
  and truncated text gets an ellipsis plus the full value on the detail screen.
- **The welcome/permissions onboarding.** Replaced by in-context permission requests (see
  principle 6).

---

## 3. Design system: Material 3 Expressive

The new `:core:designsystem` module is the only place where theme and base components are
defined. Feature modules use Material components through it and never restyle them locally.

### 3.1 Foundations

| Token group | Decision |
|---|---|
| **Theme** | `MaterialExpressiveTheme` with `MotionScheme.expressive()`. The motion scheme instance is created once and reused, since Material keys running shape morphs by spec. Docucraft hit this bug. |
| **Color** | Dynamic color on Android 12+; otherwise a seed color through MaterialKolor (`rememberDynamicColorScheme`, without `animate = true`). Light, dark and system modes, plus standard, medium and high contrast. Schemes are built only when their inputs change, off the main thread, except the first one. Same rule as Docucraft. |
| **Per-song color** | The editor can nest a theme seeded from the cover art (Palette → MaterialKolor), as Docucraft does with `LabelColorTheme`. Only that destination recomposes. This can be turned off in settings. |
| **Typography** | The M3 type scale plus the *emphasized* styles for headlines and key labels (the song title in the editor header, section titles). No more monospace uppercase title on Home. |
| **Shape** | The expressive corner scale (from extra-small up to extra-extra-large). `MaterialShapes` (for example Cookie or Sunny) are used sparingly for artwork placeholders and empty states, not as decoration everywhere. |
| **Spacing** | An 8 dp grid with named spacing tokens (`Spacing.small`, `Spacing.medium`…) instead of numbers scattered across screens. |
| **Elevation** | Tonal surfaces (`surfaceContainer*`). Shadows are reserved for floating elements. |

### 3.2 Components to use, and what they replace

| Need | Expressive component | Replaces |
|---|---|---|
| Library header and search | Search app bar (`AppBarWithSearch` + expanded search) | Custom title with monospace text |
| Editor header | `LargeFlexibleTopAppBar` with exit-until-collapsed scroll behaviour | `ColumnWithCollapsibleTopBar` (300 lines) |
| Primary editor actions | `HorizontalFloatingToolbar` with an integrated FAB (*Save*) and secondary actions (*Find metadata*, *Lyrics*, *More*) | Icon buttons crammed into the top bar |
| Library primary action | FAB menu (`FloatingActionButtonMenu`): *Open file*, *Batch edit*, *Scan library* | Scroll-to-top FAB |
| Selection mode actions | Floating toolbar with *Edit together*, *Find metadata*, *Share* | (didn't exist) |
| Sort and filter | Filter chips and a `ButtonGroup` for sort direction | Dropdown with segmented buttons |
| Loading | `LoadingIndicator` / `ContainedLoadingIndicator` | Custom loading page |
| Grouped settings and editor sections | Grouped lists with segmented rounded containers | `SettingsItem`, `PreferencesItems.kt` (806 lines) |
| Toggles with state | Toggle buttons with shape morph | Custom `SquaredButton` and button chips |
| Messages | Snackbar with an action (*Undo*, *Retry*) | Sonner toasts. Sonner is a web-style pattern that the guidelines don't include. |
| Pull to refresh | `PullToRefreshBox` with the expressive indicator | Custom `pulltorefresh` package |
| Menus | `DropdownMenu` (expressive menus) | `DropdownMenuImplementation.kt` |
| Navigation (wide windows) | `WideNavigationRail` / `ShortNavigationBar` where top-level sections exist | (none) |

Many expressive APIs are still marked experimental (`@ExperimentalMaterial3ExpressiveApi`) and
their names change between alphas. During phase 2, check every name in this table against the
Compose Material 3 release in use. The Docucraft repository's `material-3` skill
(`.claude/skills/material-3`) is the reference to copy into this repo.

### 3.3 Motion

- **One file owns navigation transitions** (as in Docucraft's `NavigationMotion.kt`): shared-axis
  movement between library and editor, a shared element for the cover art from the list item to
  the editor header, and a rising motion for modal destinations.
- Components animate their own state (pressed, selected, scrolled) and `lerp` between theme
  colors; they never animate the color scheme.
- Spring specs come from the motion scheme, not hand-written `tween`s.

---

## 4. Architecture

The architecture mirrors Docucraft so that the two projects share one way of working. The
differences come from Metadator being split into more Gradle modules.

### 4.1 Layers

```
presentation  →  domain  ←  data
(Compose, VMs)   (plain Kotlin: models, ports, use cases)   (TagLib, MediaStore, Ktor, Room, DataStore)
```

- **Domain modules are plain Kotlin.** No Android, Compose or library types. A song is identified
  by a `TrackId` (wrapping the MediaStore ID) or a `ContentRef` (a URI string), never by
  `android.net.Uri` or a file path.
- **Business logic lives in use cases**, for example `LoadTrackTagsUseCase`,
  `SaveTagChangesUseCase`, `UndoLastSaveUseCase`, `ApplyLookupResultUseCase` and
  `BatchEditUseCase`.
- **Framework work goes behind a port**: an interface in the domain, with its implementation in a
  data module. The main ports:
  - `TagReader` / `TagWriter`: TagLib.
  - `AudioLibrary`: MediaStore queries and observation.
  - `WriteAccessRequester`: `createWriteRequest` and `RecoverableSecurityException`.
  - `MediaIndexer`: rescans a file after it is written.
  - `MetadataProvider`: MusicBrainz, Deezer and so on.
  - `LyricsProvider`: LRCLIB.
  - `ArtworkSource`: picker, URL, file.
  - `TagBackupStore`: Room.
- **What the user can cause is a result, not an exception.** For example `SaveOutcome.Saved`,
  `SaveOutcome.NeedsPermission(request)`, `SaveOutcome.FileGone`,
  `SaveOutcome.UnsupportedFormat`, `LookupOutcome.NoMatches`, `LookupOutcome.Offline`.
- **Test ports with fakes** (`:core:testing`), not with mocks of the framework.

### 4.2 ViewModels (MVI)

Port Docucraft's `BaseViewModel<Intent, State, Effect>` as-is:

- `state` is a `StateFlow` of an immutable data class. There are no `SnapshotStateMap`s in state;
  the edit draft is an immutable `TagDraft` with per-field `original` and `current` values.
- `effects` are dropped when nobody is listening; they are for navigation and launching system
  UI.
- `defaultEvents` are buffered until shown; they are for snackbars.
- **A ViewModel never holds a `Context`.** Text comes from a `StringProvider`; system dialogs are
  launched by the screen in response to an effect.
- What must survive process death goes into the `SavedStateHandle`, including the unsaved draft.
  The user can leave the app mid-edit and come back to their changes.

### 4.3 Navigation

Jetpack **Navigation 3**, with the same rules as Docucraft:

- One back stack and one `NavDisplay`. Keys are `@Serializable`, each feature owns its keys, and
  keys carry IDs, never models. The editor key is `Editor(trackId)`, not `TagEditor(ParcelableSong)`.
- Features only receive a `Navigator` (`goTo`, `goBack`, `goBackWhile`, `removeDestination`).
- Sheets and dialogs the user can come back to are destinations on that same stack (the lookup
  sheet, the unsaved-changes dialog), rendered by an `OverlaySceneStrategy`. Never put a nested
  `NavDisplay` inside a sheet.
- A **list-detail scene** shows library and editor side by side on wide windows. Screens are told
  whether they share the window (`LocalPaneContext`); they never read the window size or
  orientation themselves. This replaces `LocalOrientation`, the duplicated
  portrait and landscape layouts in `MetadataEditorPage.kt`, and the global
  `calculateWindowSizeClass` in `MainActivity`.
- **External entry** ("Open with Metadator", `ACTION_VIEW`/`ACTION_EDIT`/`ACTION_SEND` for
  `audio/*`): a separate activity in its own task, with its own stack rooted at
  `ExternalEditor(contentRef)`. This is Docucraft's decision D5, applied here. Closing it returns
  to the calling app.

### 4.4 Dependency injection

Koin 4, with the Koin BOM. Each module exposes its Koin module(s), and `App.kt` lists them all in
one `appModules` list. Swapping an engine (TagLib, a metadata provider) is one line of DI.

---

## 5. Module structure

```
build-logic/                       Convention plugins (included build)
  convention/
    metadator.android.application
    metadator.android.library
    metadator.android.compose
    metadator.android.feature      (library + compose + koin + core deps)
    metadator.jvm.library          (plain Kotlin modules)
    metadator.android.room
    metadator.spotless

app/                               Composition root: App, MainActivity, ExternalEditorActivity,
                                   NavDisplay shell, scenes, flavors (playstore/foss)

core/
  model/          (jvm)            Track, TrackId, ContentRef, TagField, TagValue, TagDraft,
                                   Artwork, AudioProperties, SaveOutcome…
  common/         (jvm)            Result helpers, dispatchers qualifier, StringProvider port
  domain/         (jvm)            Shared ports and use cases (TagReader/TagWriter contracts can
                                   live here or in :tags:api)
  data/                            DataStore preferences, SettingsRepository, StringProvider impl
  database/                        Room: tag backups (undo), recent edits, search history
  designsystem/                    Theme, tokens, motion, Metadator components over M3 Expressive
  ui/                              Shared composables that know models: TrackListItem,
                                   ArtworkImage, TagHealthBadge, empty and error states
  navigation/                      Navigator, NavKey base, scene contracts, LocalPaneContext
  testing/                         Fakes of every port, test fixtures, Compose test rules

tags/
  api/            (jvm)            TagReader, TagWriter, SupportedFormats, TagKeys
  taglib/                          Kyant0 TagLib implementation. TagLib is an `implementation`
                                   dependency, so no TagLib type reaches any other module.

library/
  api/            (jvm)            AudioLibrary, LibraryQuery (search, sort, filters)
  mediastore/                      MediaStore implementation, write access, MediaIndexer

lookup/
  api/            (jvm)            MetadataProvider, LookupQuery, LookupCandidate, confidence
  musicbrainz/                     MusicBrainz + Cover Art Archive (default, no API key)
  deezer/                          Optional second source with good artwork (no API key)
  spotify/                         Optional, Play flavor only, through a proxy (see §13)

lyrics/
  api/            (jvm)            LyricsProvider, SyncedLyrics (LRC model, parser, serializer)
  lrclib/                          LRCLIB implementation (no API key)

feature/
  library/                         Library screen, search, filters, selection
  editor/                          Single-song editor, artwork, preview player, unsaved guard
  lookup/                          Find-metadata sheet and the field-by-field diff
  batch/                           Batch edit, number tracks, tags⇄filename
  lyrics/                          Lyrics fetch and the sync editor (2.1)
  settings/                        Settings, appearance, providers, about, licenses

crashhandler/                      Kept for the FOSS flavor; restyled with the design system
```

**Dependency rules**, enforced by the convention plugins and checked in CI:

- `feature:*` depends on `core:*` and on `*:api` modules only, **never** on implementations
  (`tags:taglib`, `library:mediastore`, `lookup:musicbrainz`…) or on other features.
- Only `:app` depends on implementations, and binds them in DI.
- `*:api` and `core:model`/`core:common`/`core:domain` are JVM modules with no Android
  dependency, so they test on the JVM in milliseconds.
- A feature talks to another feature only through navigation keys, which live in `core:navigation`
  or in a small `feature:*:api` if that becomes necessary.

Why more modules than Docucraft? Metadator has more interchangeable engines (tags, library,
several lookup providers, lyrics), and the API/implementation split is what lets you add or drop a
provider, or keep Spotify out of the FOSS build, without touching features. The structure is meant
to stay this size; don't split further unless build times call for it.

---

## 6. Technology stack and library updates

Baseline: the versions in Docucraft's `gradle/libs.versions.toml`, which are known to work
together as of October 2026. Re-check each version when phase 0 starts.

| Area | Today | 2.0 |
|---|---|---|
| Build | AGP `8.12.0-alpha08` (an alpha), Gradle 8.13, Kotlin 2.1.0, KSP 2.1.0-1.0.29 | AGP 9.4.x, matching Gradle, Kotlin 2.4.x, KSP 2.3.x, Gradle daemon toolchain (foojay) |
| SDK | compile/target 36, min 24, Java 21 | compile 37 (37.1 if Compose requires it), target 37, **min 26** (see §13), Java 17 like Docucraft |
| Build logic | Version class in the root script, flavors in the app script | `build-logic` convention plugins, `ProjectConfig`, version in one place |
| Compose | BOM `2025.01.00` (the alpha BOM), Material 3 `1.3.1` | Compose BOM 2026.09.x, Material 3 1.5.x (expressive APIs), `material3-adaptive` 1.3.x |
| Navigation | Navigation Compose 2.8.5 with type maps for Parcelables | Navigation 3 (1.2.x) with adaptive scenes |
| DI | Koin 4.0.0 | Koin 4.2.x (BOM) with `koin-compose-viewmodel` |
| Persistence | DataStore 1.1.2; Room declared but unused, and wired with `annotationProcessor` instead of KSP | DataStore 1.2.x; Room 2.8.x with KSP for undo backups and history |
| Async | Coroutines 1.9.0 | Coroutines 1.11.x |
| Serialization | kotlinx.serialization 1.7.3, datetime 0.6.1, immutable collections 0.3.7 | 1.11.x, 0.8.x, 0.5.x |
| Networking | Ktor 3.0.2 (seven artifacts, two engines) | Latest Ktor 3.x, one engine (OkHttp), content negotiation, logging in debug only |
| Images | Coil 2.7 + Landscapist-Coil 2.4.4 | Coil 3 directly, or Landscapist as in Docucraft; pick one image stack for both apps |
| Tags | Kyant0 TagLib `1.0.0-alpha25` | Latest Kyant0 TagLib release, verified with round-trip tests (§10) |
| Media | Media3 1.5.1 (exoplayer, session, ui, common) | `media3-exoplayer` only, for the inline preview |
| Theming | MaterialKolor 1.7.0 | MaterialKolor 5.x |
| Permissions | Accompanist Permissions 0.34 (plus an unused WebView) | Activity Result APIs directly; remove Accompanist |
| Messages | Sonner 0.3.8 | M3 Snackbar; remove Sonner |
| Paging | Paging 3.3.5 (Spotify results only) | Remove unless a provider needs it; lookup results are short lists |
| Spotify | `spotify-api-kotlin` 4.1.3 with the secret in the APK | Removed from the client; see §13 |
| Quality | None | ktfmt through Spotless with license headers, Android Lint with `warningsAsErrors` for new modules, the Compose stability compiler config, dependency rules |
| Performance | Profile installer only | Baseline profile module (`androidx.baselineprofile`), R8 full mode |
| Desugaring | No | `coreLibraryDesugaring` enabled (as in Docucraft) |
| Analytics | Firebase Analytics and Crashlytics, Play flavor | Kept in the Play flavor, behind an `AnalyticsHelper` port (no-op in FOSS) |

Remove as well: `legacy-support-v4` (only the player needs it), `appcompat` (unless the in-app
language list for older Android versions uses `AppCompatDelegate.setApplicationLocales`),
`constraintlayout-compose`, and `LazyColumnScrollbar`, which is declared but never imported (a
Material fast-scroller replaces it). Drop the catalog entries nothing uses (`qrcode-kotlin`,
Firebase Auth, Play Services Auth). Add `palette` only if §3.1's per-song color is kept. LeakCanary
stays in debug.

---

## 7. Data-safety rules for writing tags

These rules are the core of the rewrite. Go into `AGENTS.md` verbatim.

1. **Read everything, write only the diff.** The editor loads the file's full property map. When
   saving, the new map is *the original map plus the user's changes*: every key the user didn't
   touch is written back exactly as read, unknown keys included. A test asserts this for every
   supported format (fixes A1).
2. **Pictures are written only when the user changed them.** Replacing the cover replaces only
   the front cover; other picture types (back, artist, booklet) are kept. *Remove cover* is an
   explicit action. Covers come from the picker, a provider, or the file itself, never from
   MediaStore's album-art cache (fixes A2).
3. **Multi-value fields are lists end to end.** Artist, album artist, genre, composer and similar
   fields are edited as chips (one value per chip), so no separator is needed. A setting lets the
   user choose how multi-value fields are written for players that only read the first value:
   multiple values (default) or joined with a separator of their choice (fixes A3).
4. **Every write is backed up first.** Before writing, the original property map and pictures are
   stored in Room (`TagBackupStore`, bounded: last 50 saves or 100 MB, oldest dropped first).
   *Undo* in the snackbar, and *Edit history* in the editor's menu, restore them.
5. **Read back after writing.** After a save, read the tags again and compare them to what was
   meant to be written. If they differ, restore from the backup and report the failure as a
   `SaveOutcome`.
6. **Writes run off the main thread**, in `viewModelScope` with `Dispatchers.IO`, and a batch runs
   in WorkManager as expedited work with a progress notification, so it survives leaving the app
   (fixes A6).
7. **Ask for write access once, up front.** On Android 11+ use `MediaStore.createWriteRequest` for
   all the files a save or batch will touch, before writing anything. On Android 10 handle
   `RecoverableSecurityException`, and on 9 and below use the legacy permission. The pending save
   is kept and resumed in full once access is granted, covers included (fixes A7).
8. **Tell the system after writing.** Rescan each written file (`MediaScannerConnection.scanFile`
   or a MediaStore update), so the library and the user's music player show the new tags right
   away (fixes A8).
9. **Identify files by MediaStore ID or content URI, never by path** (fixes A14).

---

## 8. Screen-by-screen specification

### 8.1 First launch

- No onboarding carousel. The app opens on the **Library**.
- If audio permission isn't granted, the library shows an **in-place explanation card**: what
  Metadator does with the permission, a *Grant access* button, and a secondary *Open a single
  file* action (system file picker, no permission needed).
- If permission is permanently denied, the card links to the app's system settings. The app is
  never closed (fixes A10).
- Notifications permission (Android 13+) is only requested when a batch edit starts, because
  that's when a progress notification is shown.

### 8.2 Library

- **Search app bar** on top. It searches title, artist, album, album artist and file name, uses
  parameterized queries (fixes A5), and remembers recent searches.
- **Views**: Songs (default), Albums (grid), Artists, Folders. Reachable from tabs under the
  search bar on phones and from a navigation rail on wide windows.
- **Sort**: title, artist, album, date added, date modified, duration. Ascending or descending.
- **Filters** (chips): *Needs attention* (missing cover, artist, album, album artist, year or track
  number), format (MP3, FLAC, M4A, OGG/Opus, WAV…), folder.
- **List item**: cover, title, artist · album, and a small tag-health badge when something is
  missing. A long press enters **selection mode**.
- **Selection mode**: select all, select an album at once, then a floating toolbar with *Edit
  together*, *Find metadata*, *Share*.
- Library updates live through a `ContentObserver`, and pull-to-refresh forces a MediaStore rescan
  of the music folders.
- **Empty states**: no songs on the device (with a link to Metadator's help on where files must
  be), no search results (with *Clear filters*).
- Fast scroller with letter indicator for long lists.

### 8.3 Editor (single song)

Layout (phone): a large flexible top app bar with the cover, title and artist; then grouped
sections; a floating toolbar at the bottom.

- **Sections** (each a grouped list container):
  1. *Main*: title, artist(s), album, album artist(s), year/date, genre(s).
  2. *Track*: track number / total, disc number / total. Validated inputs (numbers only, total ≥
     number).
  3. *Credits*: composer, lyricist, conductor, remixer, performer.
  4. *More*: comment, BPM, ISRC, copyright, encoder (read-only), and **All tags**, an advanced
     view listing every key in the file, editable, with *Add field* for custom keys.
  5. *Lyrics*: preview of plain or synced lyrics, with *Edit* and *Find lyrics* (2.1 for find).
  6. *File*: format, codec, bitrate, sample rate, channels, duration, size, path, last modified.
- **Cover**: tap to see it full size with its resolution and size. Actions: *Replace* (photo
  picker or file), *Find cover online* (from providers), *Save image to device*, *Remove*.
  Shows a warning if the image is below 500 × 500 px.
- **Changed fields are marked** (a dot and a different container tone), and each one has a
  *Revert this field* action. *Save* is enabled only when something changed.
- **Saving**: a progress indicator in the FAB, then a snackbar *Saved · Undo*. The user stays on
  the editor (fixes A8).
- **Leaving with unsaved changes**: predictive back shows the dialog *Discard changes? / Keep
  editing / Save*. The draft also survives process death (fixes A9).
- **Inline preview**: a compact play/pause and seek bar in the header to check you're editing the
  right song.
- **Rename file** (menu): from a pattern such as `{artist} - {title}`, with a live preview.
- **Wide windows**: the cover and file info sit in a side column, and the fields in the main
  column. This is the same composable laid out differently, not a duplicated screen.

### 8.4 Find metadata (lookup)

A modal destination (a bottom sheet on phones, a dialog or side sheet on wide windows).

1. The query is pre-filled from the current title, artist and album (editable). Duration is used
   to rank results.
2. Results show cover, title, artist, album, year, track position and a **match confidence**
   label. Results are grouped by release when one recording appears on several albums.
3. Selecting a result opens a **comparison**: for every field, the current value next to the
   proposed one, with a checkbox. Fields that differ are pre-checked; identical ones are hidden
   behind *Show unchanged*. The cover comparison shows both resolutions.
4. *Apply* copies the checked values **into the draft**. Nothing is written until the user saves
   (UX principle 4).
5. Offline and no-results states have clear messages and a *Retry*.

### 8.5 Batch edit

Entered from selection mode.

- **Common fields**: each field shows the shared value or "Multiple values" as a placeholder.
  Editing a field sets it for all selected songs; leaving it untouched keeps each song's value.
- **Tools**:
  - *Number tracks* (by current order, file name or disc).
  - *Tags from file name* (pattern with live preview on three examples).
  - *File names from tags* (pattern with live preview).
  - *Find album metadata*: match the selection as one release (MusicBrainz) and map tracks by
    position and duration, with the same comparison step as §8.4.
  - *Apply cover to all*.
- **Review before writing**: a summary such as "12 files will change: album artist, year, cover".
- **Execution**: WorkManager with a progress notification; one write-access request for all
  files; a final report showing which files succeeded and which failed and why; *Undo all*.

### 8.6 Lyrics (2.1)

- *Find lyrics* via LRCLIB (synced if available, otherwise plain). SongSync stays as an optional
  integration if installed.
- A simple **sync editor**: play the preview and tap to stamp each line; nudge timestamps by
  ±100 ms; preview with highlighted lines.
- Written to the standard `LYRICS` field (USLT for ID3), LRC formatted when synced.

### 8.7 Settings

Grouped lists. No empty pages (fixes A11).

- **Appearance**: theme (system/light/dark), dynamic color, seed color picker, palette style,
  contrast level, color the editor from the cover.
- **Editor**: multi-value write mode and separator, file rename pattern, warn about small covers,
  keep edit history (and *Clear history*).
- **Online lookup**: providers on or off and their order, cover size preference, network use on
  cellular.
- **Language**: per-app language (Android 13+ uses the system picker; older versions get an
  in-app list).
- **About**: version, changelog, source code, licenses (generated with the AboutLibraries Gradle
  plugin), privacy policy, translate (Weblate), rate the app (Play flavor).

### 8.8 External editor ("Open with")

- Accepts `audio/*` from file managers and players.
- `ACTION_EDIT` grants write access, so it saves in place. With `ACTION_VIEW` or `ACTION_SEND`
  (read-only), if the URI is a MediaStore item, write access is requested through
  `createWriteRequest`; otherwise *Save as a copy* is offered through the system file picker.
- Same editor, own task, own back stack. Closing it returns to the calling app.

---

## 9. Delivery phases

Work happens on a long-lived `v2` branch. Each phase is one or more PRs into `v2`, and `v2` merges
into `master` only for the 2.0 release. The current `master` stays releasable in the meantime, in
case 1.x needs an urgent fix. **Before anything else, ship a 1.0.x hotfix for A1–A3** (see §11.1).

Each phase ends with its *exit criteria* met, documentation updated, and a build installable on a
device.

### Phase 0 — Foundations

- Create `v2` with the new structure: `build-logic`, all modules from §5 as empty shells, version
  catalog updated (§6), flavors `playstore`/`foss` in the app convention.
- Spotless with ktfmt and the license header; Lint baseline; dependency-rule checks.
- CI (GitHub Actions): `spotlessCheck`, `lint`, JVM unit tests, `assembleFossDebug` and
  `assemblePlaystoreDebug` on every PR. Release signing fixed (A13).
- `AGENTS.md` and `docs/` skeleton (architecture, navigation, tags, library, lookup, testing),
  following Docucraft's conventions: English, describes the present, explains the why.
- Port `BaseViewModel`, `Navigator`, `UiEvent`, `StringProvider` and the M3 skill from Docucraft.

**Exit:** an empty app with the theme builds in CI for both flavors; module dependency rules fail
the build when broken.

### Phase 1 — Tag engine and data safety

- `tags:api` models (`TagField`, `TagValue` as a list, `TagSnapshot`, `TagChanges`) and the pure
  diff/merge logic in `core:domain` (§7, rules 1–3).
- `tags:taglib`: read properties, audio properties and pictures; write the merged map; write
  pictures selectively; read back and verify.
- `library:mediastore`: `AudioLibrary` (parameterized queries, observation), `WriteAccessRequester`,
  `MediaIndexer`.
- `core:database`: `TagBackupStore` with retention.
- Use cases: `LoadTrackTags`, `SaveTagChanges` (backup → write → verify → rescan → outcome),
  `UndoLastSave`.
- Fixture files for every supported format with a rich set of tags, including unknown keys,
  several pictures and multi-value fields.

**Exit:** round-trip tests pass on a device for every format. They edit one field, save, and
verify every other tag and picture is byte-for-byte the same. Undo restores the original.

### Phase 2 — Design system

- Theme (§3.1), tokens, motion scheme, navigation motion file.
- Metadator components over M3 Expressive: grouped list section, field row (with changed state),
  tag chips input, artwork image with placeholder shape, empty and error states, tag-health badge.
- Compose previews for every component in light, dark and high contrast; screenshot tests.

**Exit:** a catalog screen (debug only) shows every component in every theme and contrast level;
screenshot tests are in CI.

### Phase 3 — Library

- Library screen (§8.2): search, sort, filters, views, selection mode, empty states, permission
  flow (§8.1).
- Navigation shell with list-detail scene.

**Exit:** on a library of 5,000+ songs the list scrolls without jank (checked with Macrobenchmark),
and search returns results while typing.

### Phase 4 — Editor

- Editor (§8.3): sections, chips for multi-values, validation, cover actions, changed markers and
  per-field revert, save with snackbar and undo, unsaved-changes guard with predictive back, draft
  in `SavedStateHandle`, inline preview, all-tags view, rename file.
- External editor entry (§8.8).

**Exit:** the "fixer" journey (open app → search → fix a field and the cover → save → see it in
another player) works in under a minute on a fresh install, and survives rotation and process
death mid-edit.

### Phase 5 — Online lookup

- `lookup:musicbrainz` (with a proper User-Agent and the 1 request/second rate limit) + Cover Art
  Archive; `lookup:deezer` as a second source for covers.
- Lookup sheet and comparison (§8.4), ranking by duration and text similarity.

**Exit:** for a test set of 50 well-known songs, the right recording is in the first three results
for at least 45 of them, and applying a result never writes anything without *Save*.

### Phase 6 — Batch editing

- Selection → batch editor (§8.5), WorkManager execution, single write request, report, undo all.
- Tags from file name and file names from tags; number tracks; album lookup.

**Exit:** editing a 20-track album (album artist, year, cover, track numbers) is one flow with one
permission prompt, and *Undo all* restores every file.

### Phase 7 — Settings, polish and accessibility

- Settings (§8.7), About with licenses, per-app language.
- Accessibility pass: TalkBack walkthrough of every journey, contrast checks, font scale 200%,
  keyboard navigation on a Chromebook or tablet.
- Spanish translation of every new string; Weblate set up for more languages.
- Baseline profiles for startup, library scroll and opening the editor.

**Exit:** Lint and the Accessibility Scanner report no issues on main screens; cold start under
the 1.x time on a mid-range device.

### Phase 8 — Release (2.0)

See §11. Lyrics (§8.6) ship in **2.1**, together with whatever the 2.0 feedback asks for most.

---

## 10. Testing strategy

| Level | What | Where |
|---|---|---|
| JVM unit | Diff and merge of tags, multi-value handling, validation, file-name patterns, LRC parser, lookup ranking, use cases with fakes, ViewModels (state and effects) | `*:api`, `core:domain`, `feature:*` `test/` |
| Instrumented | TagLib round trips per format (§9, phase 1), MediaStore queries and write requests, Room migrations, WorkManager batch | `tags:taglib`, `library:mediastore`, `core:database` `androidTest/` |
| UI | Editor: changed markers, unsaved guard, save/undo; library: search and selection | Compose UI tests in `feature:*` `androidTest/` |
| Screenshot | Design system components and main screens in each theme/contrast level | `core:designsystem`, features (Compose screenshot testing or Roborazzi) |
| Performance | Startup, library scroll, open editor | `:baselineprofile` + Macrobenchmark |
| Manual | Release checklist on a phone, a tablet, Android 8 and the latest Android; TalkBack | `docs/testing.md` |

Fixture audio files are small, generated (silence plus tags, for example with `ffmpeg`) and never
contain anyone's real music. A script regenerates them, as Docucraft does with its PDF fixtures.

---

## 11. Release and Play Store strategy

### 11.1 Right now: a 1.x hotfix

Before the rewrite starts, release **1.0.1** on `master` fixing the data-loss bugs. They're each a
few lines and they protect current users while 2.0 is built:

- A1: merge the edited fields into the file's original property map instead of replacing it.
- A2: don't write pictures unless the user picked a new cover.
- A3: stop splitting on `","`; keep the original array for unchanged multi-value fields.

### 11.2 2.0 launch

- **Same package name and signing key**, so it ships as an update. Version **2.0.0**.
- **Preferences migration**: a DataStore migration maps the 1.x keys that still exist (theme,
  dynamic color, seed color, palette style) and drops the rest. Since onboarding is gone,
  `completed_onboarding` is ignored.
- **Internal → closed testing → production with staged rollout** (10% → 50% → 100%), watching
  Crashlytics' crash-free rate and ANRs (target: crash-free ≥ 99.5%, user-perceived ANR rate
  below Play's bad-behaviour threshold).
- **In-app review** (Play In-App Review API, Play flavor only): asked after the third successful
  save, at most once per 90 days, never after an error.
- **Store listing refresh**: new screenshots generated from the app's own screens (Docucraft's
  `scripts/store` approach), a short description built around the three journeys, and a feature
  graphic in the new visual style. Reply to recent negative reviews once 2.0 is out.
- **Measure the UX goals** (Play flavor, anonymous): time from launch to first save, saves per
  session, lookup applied vs. abandoned, permission grant rate, undo usage (a high undo rate means
  something is wrong). Review after two weeks of production.
- **F-Droid**: the FOSS flavor has no Firebase and no Spotify, so it qualifies. Submit after 2.0.

---

## 12. Risks

| Risk | Impact | Mitigation |
|---|---|---|
| TagLib binding behaves differently per format (picture handling in MP4/OGG, multi-value support) | Data loss | Phase 1 round-trip tests per format before any UI is built; unsupported operations return `UnsupportedFormat` instead of guessing |
| Expressive APIs change between Material 3 alphas | Rework | Keep all expressive usage inside `core:designsystem`; features use Metadator components |
| Scoped storage edge cases (SD cards, files outside MediaStore, OEM quirks) | Saves fail | `SaveOutcome` covers each case with a clear message; external editor with *Save as copy* as a fallback |
| MusicBrainz rate limits and coverage of less-known music | Weak lookup | Rate limiter in the provider, Deezer as a second source, manual entry is always available |
| Rewrite takes longer than planned | Store momentum stalls | 1.0.1 hotfix first; phases deliver usable builds; Lyrics deferred to 2.1; the internal testing track receives builds from phase 4 onward |
| Performance on large libraries | Jank, bad reviews | Paged MediaStore queries if needed, stable keys, baseline profiles, Macrobenchmark exit criteria |

---

## 13. Decisions to confirm before starting

These change the scope or the structure, so they should be settled before phase 0. Each has a
recommendation.

1. **The full music player.** *Recommendation:* remove it and keep only an inline preview in the
   editor (§2.3). It's a large part of the codebase that a tag editor doesn't need.
2. **Spotify.** The secret can't stay in the APK (A12), and Spotify's developer terms restrict
   apps of this kind more and more. *Recommendation:* MusicBrainz + Cover Art Archive as the
   default, Deezer for covers; drop Spotify, or keep it only in the Play flavor through a small
   proxy you control (for example a Cloudflare Worker that holds the secret).
3. **minSdk.** Today 24 (Android 7.0). *Recommendation:* 26 (Android 8.0). It simplifies
   notification channels, adaptive icons and some file APIs, and loses very few users. Check the
   Play Console's device statistics first. Keep 24 if the numbers say otherwise.
4. **Branch strategy.** *Recommendation:* a long-lived `v2` branch with PRs per phase, and a
   1.0.1 hotfix on `master` first (§11.1).
5. **Image stack shared with Docucraft.** *Recommendation:* use the same library in both apps
   (Coil 3 or Landscapist), so components and knowledge carry over.
6. **Build logic.** *Recommendation:* `build-logic` as an included build (better with the
   configuration cache than `buildSrc`). If keeping parity with Docucraft matters more, use
   `buildSrc` as Docucraft does.
