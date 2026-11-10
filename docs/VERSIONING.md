# Versioning strategy

Status: adopted. Starts at `0.3.0`, `mg4` marker dropped, full migration done (`version.properties`, `scripts/bump_version.sh`, docs, release workflow check).

## What is wrong now

- `versionName` is `0.2.9-mg4.NN`. The `0.2.9` is the upstream base from months ago and never moved. Every change, from a one-line fix to the whole Settings rewrite, is just `NN + 1` (now `mg4.98`).
- The number says nothing about size or risk, and `mg4.98` reads as "nearly 100 releases".
- The `mg4` tag is a fork marker that is now redundant (the app is MG4 only).
- `versionCode` (125) and `versionName` (98) have drifted apart, so there are two counters to keep in step by hand.

## Hard constraints (do not break these)

1. **`versionCode` must keep rising.** Android refuses to install a lower code over a higher one, and the car has no ADB to force it. Continue from 126. Never reset it.
2. **OTA compares the numbers in order** (`OtaUpdater.compare`: `0.2.9-mg4.98` is read as `0, 2, 9, 4, 98`). So the new scheme must compare **higher** than `0.2.9-mg4.98`. Any `0.3.x` or `1.x.y` does. A restart at `0.1.0` or `1.0` would never be offered as an update to the cars already running mg4.98, and the updater would say "up to date".
3. **Tag `v<versionName>`**, one APK named `MG4CPlay-v<versionName>.apk`, as `docs/RELEASING.md` already requires.

So "restart" means: restart the **name**, keep the **code** counting.

## Proposed scheme: semantic versioning, `MAJOR.MINOR.PATCH`

| Part | Bump when | Example |
| --- | --- | --- |
| MAJOR | A change that needs a manual step or breaks saved settings | 2.0.0 |
| MINOR | A new feature or visible change (new setting, new screen, new car read) | 0.4.0 |
| PATCH | A fix only, no new behaviour | 0.4.1 |

- Drop the `0.2.9-` prefix and the `mg4.` marker. The package name already says it is the MG4 app.
- `versionCode` stays a plain +1 per shipped build (126, 127, ...). It is the install counter and has no meaning beyond order.
- Car-test builds keep the existing `-hud-test` suffix from the debug build type. The OTA compare ignores it.

### Where to start

**Recommended: `0.3.0`.** It is higher than `0.2.9-mg4.98`, so OTA offers it, and the leading `0` honestly says "still being proven in the car". Cut `1.0.0` when the test plan is fully passed on the car (sections D, E, F and H are still marked not yet tried).

Alternative: jump straight to `1.0.0`. Cleaner number, but it claims a stability the untested items do not yet back up.

## Release rhythm

Stop shipping every small change as its own version.

1. **Batch.** Work lands on `main` with no version bump. `CHANGELOG.md` keeps its `Unreleased` section as the running list.
2. **Cut a release** only when the car needs something: a car test is due, or a fix matters on the road. One commit does the bump (`versionCode`, `versionName`, `VERSION` in `scripts/build_mg4.sh`, `CHANGELOG.md` heading), then the `Release` workflow runs with that tag.
3. **Test builds between releases** (when a car test needs an install but is not a real release): use a build number, not a version. Either install a local debug build with the next `versionCode` and a `-test.N` suffix, or skip the tag and GitHub release entirely. Only tagged releases are offered by OTA.
4. **Changelog per release, grouped.** Replace the `mg4.NN (test build)` headings with `## 0.3.0 (2026-11-10)`, then Added, Changed, Fixed.

Rule of thumb: if you would not mention it to a friend, it waits for the next release.

## One source of truth

Today the version lives in three places (`mobile/build.gradle.kts` twice, `scripts/build_mg4.sh`). Proposed follow-up:

- Put `versionName` and `versionCode` in one `version.properties` file at the repo root.
- `mobile/build.gradle.kts` reads it. `scripts/build_mg4.sh` reads it (`grep`/`cut`, no duplicate string).
- A one-line script, `scripts/bump_version.sh minor|patch`, edits the file, adds the changelog heading and makes the commit.
- CI check: the tag passed to the `Release` workflow must equal `v<versionName>`.

## Migration (done)

- `version.properties` holds `versionName=0.3.0` and `versionCode=126`. Gradle, `scripts/build_mg4.sh` and the `Release` workflow read it.
- `scripts/bump_version.sh major|minor|patch` bumps both numbers and dates the changelog heading. It does not commit or tag.
- The Release workflow already fails when the tag differs from `v<versionName>`.
- `OtaUpdaterTest` checks `0.3.0` is newer than `0.2.9-mg4.98` and that the `-hud-test` suffix is ignored.
- `CHANGELOG.md` keeps the old `mg4.62` to `mg4.98` notes under a "Test builds before 0.3.0" heading, with one `0.3.0` entry on top.
- `CLAUDE.md`, `README.md` and `docs/RELEASING.md` describe the new scheme. The old `v0.2.9-mg4.*` tags and releases stay as history.

## Still to do before releasing

1. Run the CI checks (`./gradlew :shared:testDebugUnitTest :common:testDebugUnitTest :mobile:lintDebug :mobile:assembleDebug`).
2. Build, then install over mg4.98 from the USB stick and confirm it upgrades in place.
3. Run the `Release` workflow with tag `v0.3.0`.
