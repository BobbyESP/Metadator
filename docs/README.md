# Metadator documentation

How each part of the app works, and why it was built that way. The rules for working on the code
are in [`AGENTS.md`](../AGENTS.md).

| Document | Covers |
|---|---|
| [architecture.md](architecture.md) | Modules, layers, ViewModels, navigation, theme, DI |
| [saving-tags.md](saving-tags.md) | How a save keeps every tag it didn't change, backups and undo, write access, rescans |
| [testing.md](testing.md) | What is tested where, and what still needs a device |
| [ci.md](ci.md) | What runs on a pull request, the release build and its secrets |
| [renovation-plan.md](renovation-plan.md) | The plan behind 2.0: the audit of 1.x, the UX goals and the phases |

## Status of 2.0

| Area | State |
|---|---|
| Domain (save, undo, lookup, batch, library queries) | Done. Unit tested on the JVM. |
| Online sources (MusicBrainz, Deezer, LRCLIB) | Done. Unit tested with a mock HTTP engine. |
| Android layers and UI | Written; must be built and checked on a device (see [testing.md](testing.md#not-yet-verified)). |
| Lyrics sync editor | Not started (planned for 2.1). |
