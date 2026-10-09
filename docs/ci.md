# Continuous integration

Two workflows in `.github/workflows`, and one shared setup in `.github/actions/setup-build`.

## CI (`ci.yml`)

Runs on every pull request, on every push to `master` and `v2`, in a merge queue, and by hand.

| Job | Command | Keeps |
|---|---|---|
| Format | `./gradlew spotlessCheck` | |
| Unit tests | `./gradlew test --continue` | The test reports, when a test fails |
| Lint | `./gradlew lint --continue` | The lint reports |
| Debug build | `./gradlew :app:assembleFossDebug :app:assemblePlaystoreDebug` | The FOSS debug APK |
| CI | Fails unless the four above passed | |

The jobs run in parallel, so a formatting error is reported in a couple of minutes and does not
hide a failing test.

**Require the `CI` job in branch protection, not the four others.** It keeps its name when jobs are
added or renamed, and a job that was skipped or cancelled does not count as passed.

### Why it is built this way

- **No Android SDK step.** The runner image ships the SDK with its licenses accepted, and the
  Android Gradle Plugin downloads the platform and build tools the build asks for. A setup action
  that drives `sdkmanager` itself is one more thing that breaks when the SDK's packages change.
- **The runner image is pinned** (`ubuntu-24.04`, not `ubuntu-latest`), because the build relies on
  the SDK that image ships. Moving to a newer image is a pull request that shows whether it works,
  not something that happens to every branch on a day GitHub picks. Nothing updates this pin:
  change it by hand in both workflows.
- **Actions are pinned to a commit**, with the version in a comment. A tag can be moved to other
  code; a commit cannot. Dependabot (`.github/dependabot.yml`) opens the pull requests that move
  the pins, and one a week for the Gradle dependencies.
- **Only `master` and `v2` write the Gradle cache.** A pull request reads the cache of its base
  branch. Its own cache would be readable by nobody else and would push the useful ones out of the
  repository's quota.
- **A new push to a pull request cancels its previous run.** Runs on `master` and `v2` are left to
  finish, so every commit there has a result.
- **Nothing needs a secret**, so a pull request from a fork gets the same checks.

## Release (`release.yml`)

Started by hand from the Actions tab, on the branch or tag to release. It runs the CI workflow
first, then builds `:app:assembleFossRelease` and `:app:bundlePlaystoreRelease` and keeps the FOSS
APK and the playstore app bundle (`.aab`, the only format Google Play takes) as the
`metadator-release` artifact.

| Secret | What |
|---|---|
| `SIGNING_KEY_STORE_BASE64` | The keystore, base64-encoded (`base64 -w0 metadator_keystore.jks`) |
| `SIGNING_STORE_PASSWORD` | The keystore's password |
| `SIGNING_KEY_ALIAS` | The key's alias |
| `SIGNING_KEY_PASSWORD` | The key's password |
| `GOOGLE_SERVICES_JSON_BASE64` | Optional. `app/google-services.json`, base64-encoded |

`scripts/set-release-secrets.ps1` sets them all from the keystore and its password, with the GitHub
CLI. It checks the password with `keytool` first and reads the alias from the keystore:

```powershell
./scripts/set-release-secrets.ps1 C:\keys\metadator_keystore.jks 'the password'
```

`-GoogleServicesJson app\google-services.json` adds the optional secret, `-KeyPassword` is for a
key whose password is not the keystore's, and `-PrintOnly` prints the values instead of setting
them.

The workflow stops before building when a signing secret is missing, and fails when the APK or the
bundle comes out unsigned: without a keystore Gradle builds them unsigned and reports success.
Without `GOOGLE_SERVICES_JSON_BASE64` it warns and builds the playstore bundle without Crashlytics,
as a fork would.

## Changing a workflow

- A workflow runs as it is in the branch that triggered it, so a change is tested by its own pull
  request. Dependabot and scheduled runs only read the default branch.
- Add a job to `needs` of the `CI` job, or it will not block a merge.
