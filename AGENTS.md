# AGENTS.md

Metadator is an Android tag editor for local music: it lists the songs on the device, edits their
tags and covers, finds metadata and lyrics online, edits many songs at once, and plays them. Nothing
leaves the device but a song's title, artist and album, when the user searches online.

This file is the map and the rules. How each part works, and why, is in [`docs/`](docs/README.md).

---

## 1. Modules

| Module | What it is | Rule |
|---|---|---|
| `:app` | Composition root: `App`, `MainActivity`, `ExternalEditorActivity`, the navigation shell, DI wiring, flavors. | The only module that names implementations. |
| `:core:model` | `Track`, `ContentRef`, `UserSettings`… | Plain Kotlin. |
| `:core:common` | Dispatchers, `StringProvider`, text helpers. | Plain Kotlin. |
| `:core:domain` | Use cases: load, save, undo, lookup, lyrics, batch, library queries. | Plain Kotlin. Tested on the JVM with fakes. |
| `:core:data` | DataStore settings, Android strings. | |
| `:core:database` | Room: tag backups for undo. | |
| `:core:network` | Ktor client, cover downloads. | Plain Kotlin. |
| `:core:designsystem` | Theme, tokens, blur, shared components over Material 3 Expressive. | No feature knowledge. |
| `:core:ui` | `BaseViewModel`, messages, artwork and its accent color, track rows, field labels. | |
| `:core:navigation` | Keys, `Navigator`, scenes, motion. | Every `NavKey` lives here. |
| `:tags:api` / `:tags:taglib` | Reading and writing tags. | TagLib is an `implementation` dependency: no TagLib type leaves `:tags:taglib`. |
| `:library:api` / `:library:mediastore` | The device's songs, file access, write permission, rescans. | |
| `:lookup:api` / `:lookup:musicbrainz` / `:lookup:deezer` | Online metadata. | Plain Kotlin. No API keys. |
| `:lyrics:api` / `:lyrics:lrclib` | Lyrics: finding them, and reading LRC and TTML. | Plain Kotlin. |
| `:player:api` / `:player:media3` | The music player. | |
| `:feature:*` | `library`, `editor`, `batch`, `player`, `settings`. | Depend on `:core:*` and `*:api` only, never on an implementation or another feature. The `metadator.android.feature` convention fails the build otherwise. |

Build setup: `buildSrc` holds the convention plugins (`metadator.jvm.library`,
`metadator.android.library`, `metadator.android.compose`, `metadator.android.feature`,
`metadator.android.application`) and `ProjectConfig` (minSdk 26, compile 37.1, target 37, Java 17).
Versions are in `gradle/libs.versions.toml`; the app's version in the root `build.gradle.kts`.

## 2. Rules

### Data safety (read [docs/saving-tags.md](docs/saving-tags.md) before touching a write path)

- **Write only what changed.** A save applies `TagChanges` to the file as it is at that moment;
  every key the user did not touch is written back exactly as read. Never build a property map
  from a list of known fields: TagLib removes every key missing from the map.
- **Pictures are written only when the cover changed**, and replacing or removing the cover keeps
  every other picture.
- **Multi-value fields are lists end to end.** Never join and split them on a separator in between.
- **Every write is backed up first** (`TagBackupStore`) and can be undone.
- **Files are `ContentRef`s (`content://` URIs), never paths.** A path is only read to ask the
  media scanner to rescan.
- **Write access is asked for before writing** (`WriteAccess`), for all files of a batch at once.

### Layers

- `*:api`, `:core:model`, `:core:common` and `:core:domain` are plain Kotlin: no Android, no
  Compose.
- Business logic is a use case in `:core:domain`. Framework work goes behind a port in an `*:api`
  module, implemented next to it.
- What the user can cause is a result, not an exception: `SaveOutcome`, `TagReadResult`,
  `LookupResult`, `AccessResult`.
- Test a port with a fake (see `core/domain/src/test/.../Fakes.kt`), not with a mock of the
  framework.

### ViewModels

- MVI on `BaseViewModel<Intent, State, Effect>`: one immutable `state`; `effects` for whoever is
  on screen now (dropped if nobody listens); `messages` for snackbars (buffered until shown).
- A ViewModel never holds a `Context`. Text comes from `StringProvider`; Android work goes through
  a port (`ImageSource`, `ArtworkAccentSource`, `WriteAccess`).
- What must survive process death goes in the `SavedStateHandle` (the editor's unsaved text
  changes, the library's tab and search).

### Navigation

- One back stack and one Navigation 3 `NavDisplay` (`MetadatorNavDisplay`). Keys carry ids or URIs,
  never models.
- Features get a `Navigator`; they never touch the stack.
- On wide windows, list-detail puts the editor beside the library. A destination is told whether it
  shares the window (`LocalPaneContext`); it never measures the window itself.
- The player is not a destination: the shell measures the window and passes it a `PlayerLayout`.
- Transitions live in `core/navigation/.../motion/NavigationMotion.kt`.

### Theme

- `MetadatorTheme` is Material 3 Expressive with one `MotionScheme.expressive()` instance for the
  app's lifetime. Color schemes are built off the main thread and cached.
- A destination with its own color nests `MetadatorAccentTheme` (the editor, from the cover).
- Use Material components through the design system; don't restyle them per screen. Spacing comes
  from `Spacing`, grouped-list shapes from `GroupShapes`. Menus are `ActionMenu`/`PopupMenu`,
  exclusive options are `ConnectedChoices`, text fields are `TonalTextField` (no outlined fields),
  chips are `ToggleChip`/`ActionChip`/`LabelChip`, a floating toolbar is `FloatingActionToolbar`.
- Blur comes from `MetadatorBlurDefaults`, and stands in for shadows: `Modifier.frosted` (Haze)
  for a surface floating over content, `Modifier.blurHalo` to lift what floats (it keeps its shadow
  only where `isHaloSupported` is false), `Modifier.outOfFocus` for what something opens over,
  `Modifier.dissolved` for what comes and goes as one surface turns into another. A `hazeSource`
  is a sibling of what it blurs, never an ancestor, and nothing is frosted while a shared element
  transition runs. A halo stays out of its element's enter and exit animation.
- No deprecated API: the build has no deprecation warnings, keep it so. Animate with the theme's
  `motionScheme`, not hand-written tweens. The player sheet and predictive back are the
  exceptions, and say why.
- Regular expressions run on Android's engine (ICU), stricter than the JVM's the tests use: escape
  every literal `}` and `]`.

## 3. Build, test, verify

| What | Command |
|---|---|
| Debug APK (no Google services) | `./gradlew :app:assembleFossDebug` |
| Unit tests, every module | `./gradlew test` |
| Format, then verify | `./gradlew spotlessApply`, then `./gradlew spotlessCheck` |

- Spotless formats Kotlin with ktfmt 0.64 (kotlinlang style) and adds the license header from
  `spotless/copyright.txt`. Run it before every commit.
- CI (`.github/workflows/ci.yml`) runs `spotlessCheck`, `test`, `lint` and the debug build of both
  flavors on every pull request and on every push to `master` and `v2`. `release.yml` is started by
  hand and builds the signed release APKs. See [docs/ci.md](docs/ci.md).
- The `playstore` flavor adds Firebase Crashlytics and Play in-app review. Firebase is only applied
  when `app/google-services.json` exists, so forks and the FOSS flavor build without it.

## 4. Conventions

- Everything is in English: code, comments, docs, commits. The app ships English, Arabic, Chinese
  (Simplified), Dutch, French, German, Hindi, Italian, Japanese, Korean, Portuguese, Russian and
  Spanish strings; a new string needs all of them.
- A string lives in the module that shows it, under the section comment it belongs to, in the same
  place in every language. One that several features show lives in `:core:ui`: a name is defined
  in one module only, since the app merges them all.
- Commits follow `type(scope): summary`; the body explains why.
- Comments and KDoc say why, not what.
- Docs describe the app as it is now. When code changes, update the doc that describes it.
