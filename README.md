<div align="center">
    <img src="./assets/app_logo.png" alt="Metadator icon" width="100" height="100"/>
</div>
<h1 align="center"><b>Metadator</b></h1>
<div align="center">

**Fix your music's tags, covers and lyrics. On your device, without losing a thing.**

![Kotlin](https://img.shields.io/badge/Kotlin-2.4-7F52FF?style=flat-square&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3_Expressive-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)
![Android](https://img.shields.io/badge/Android-8.0+-3DDC84?style=flat-square&logo=android&logoColor=white)
![GitHub all releases](https://img.shields.io/github/downloads/BobbyESP/Metadator/total?label=Downloads&style=flat-square)

</div>

## Features

- **Edit every tag**: title, artists, album, album artists, year, genres, track and disc numbers,
  credits, comment, BPM, ISRC, lyrics, and any other tag in the file under *All tags*.
- **Never lose data**: Metadator writes only what you changed, keeps every other tag and picture,
  and backs the file up first. Every save can be undone.
- **Find metadata online**: search MusicBrainz and Deezer at once, compare each field with what the
  song has, and pick what to use. Covers up to 1200 px.
- **Lyrics**: find plain or synced lyrics on LRCLIB.
- **Edit many songs at once**: set an album artist or a cover for a whole album, number tracks,
  read tags from file names.
- **A library that shows what to fix**: search everything, sort, filter by format, and see which
  songs are missing tags.
- **A simple music player**, with background playback and the system's media controls.
- **Open with Metadator** from any file manager or player, no library permission needed.
- **Material 3 Expressive**, with colors from your wallpaper or the song's cover, and a two-pane
  layout on tablets and foldables.

Supported formats: everything TagLib reads, including MP3, FLAC, M4A/AAC/ALAC, Ogg Vorbis, Opus,
WAV, AIFF, WMA and APE.

## Download

[Google Play](https://play.google.com/store/apps/details?id=com.bobbyesp.metadator) ·
[GitHub releases](https://github.com/BobbyESP/Metadator/releases/latest)

## Privacy

Metadator works on your device. The only thing that leaves it is a song's title, artist and album
when you search for metadata or lyrics, sent to the source you search. The Google Play build sends
anonymous crash reports; the FOSS build sends nothing.

## Build it

Requirements: JDK 21 and the Android SDK (compile SDK 37).

```bash
./gradlew :app:assembleFossDebug   # an APK without Google services
./gradlew test                     # unit tests
./gradlew spotlessApply            # format before committing
```

How the code is organized, and the rules for changing it, are in [AGENTS.md](AGENTS.md) and
[docs/](docs/README.md).

## Translations

English and Spanish are included. Contributions of other languages are welcome: copy
`src/main/res/values/strings.xml` of each module into `values-<language>/`.

## License

GPL-3.0. See [LICENSE](LICENSE).
