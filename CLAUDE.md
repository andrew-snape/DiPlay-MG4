# MG4CPlay (DiPlay-MG4, SWI165 fork)

Wireless CarPlay receiver for an Australian MG4 head unit: SWI165 firmware
(`SWI165-29176-1100R11`), Android 9, MediaTek MT2712, 1920x720 panel. Fork of
`fatihdonmezdev/DiPlay-MG4`, which follows upstream `shihabal3amri/DiPlay`. Start with
[README.md](README.md) and [docs/MG4_SWI165.md](docs/MG4_SWI165.md).

## Layout

- `shared/`: CarPlay protocol (AirPlay, iAP2, transport, media, MFi) and vehicle code
  (`vehicle/` for MG4). The app is MG4 only: upstream code for other cars has been removed,
  so don't port it back.
- `common/`: the app: `CarPlayHostActivity` (session), `DiPlayActivity` (settings and
  diagnostics), `AirPlayPersistence` (settings), `Mg4FirmwareProfile` (per-firmware
  defaults).
- `mobile/`: the APK module. The version is in `version.properties` at the root.
- `shared/.../vehicle/`: MG4 car reads, SWI165 only (`Mg4Firmware` gate). Read-only: `SaicBinder`
  (SAIC hub and services), `CarProperties` (CarPropertyManager), `Mg4CarReader` (sanity bounds).
  No vendor library: battery, speed, gear and charging are read here.

## Building and testing

- Java: any JDK 17 or later starts Gradle (`brew install openjdk@21`). Gradle then downloads and
  runs on Java 25, pinned in `gradle/gradle-daemon-jvm.properties`, the same as CI. Same checks as CI:
  `./gradlew :shared:testDebugUnitTest :common:testDebugUnitTest :mobile:lintDebug :mobile:assembleDebug`
- Car builds: `./scripts/build_mg4.sh`, with `DIPLAY_AUTH_ASSETS_DIR=$PWD/.private/runtime-assets`.
  The APK goes to `dist/`. Never commit `.private/`, keys or APKs.
- The repo sits in iCloud-synced `~/Documents`. iCloud makes `name 2.kt` / `name 2.xml`
  copies, which break the build with duplicate classes or resources. Move untracked
  `* 2.*` files out of the tree (don't commit them), and clean before building:
  `./gradlew --stop && rm -rf build */build .gradle`

## Conventions

- Always pass `-R andrew-snape/DiPlay-MG4` to `gh`. Its default repo is upstream.
- Branch from `main`; open PRs against `main` on `andrew-snape/DiPlay-MG4`.
- Commit subjects: `feat:`, `fix:`, `docs:`, `build:`, `ci:`, `test:`, `chore:`, and
  `port: <what> (upstream DiPlay #N)` for upstream fixes.
- Versions are `MAJOR.MINOR.PATCH` (minor: new feature, patch: fix only), kept in
  `version.properties` only. Don't bump per change: batch work under `# Unreleased` in
  `CHANGELOG.md` and cut a release when the car needs one. `scripts/bump_version.sh
  major|minor|patch` bumps `versionName` and `versionCode` (+1, never reset) and dates the
  changelog heading; commit that on its own. A build with the same `versionCode` won't
  install over the last one. See [docs/VERSIONING.md](docs/VERSIONING.md).
- Release: run the `Release` workflow on `main` with tag `v<versionName>`
  (`gh workflow run release.yml -R andrew-snape/DiPlay-MG4 --ref main -f tag=v<MAJOR.MINOR.PATCH>`).
  See [docs/RELEASING.md](docs/RELEASING.md).
- MG4-only behaviour is gated on `Mg4FirmwareProfile.detect().isMg4` (or
  `VehicleIntegration.isMg4`). On unknown firmware the car features stay off.
- MG4 defaults that a user can change use the "saved choice wins" pattern in
  `AirPlayPersistence` (see `loadCallsOverCarPlayOnly`). Battery, connector, low-charge
  and video-while-parked choices live in `VehicleSettings`.
- Car reads are read-only and fail closed: a value that can't be read is null, never a guess.
- Log what a car test needs to confirm a change (`appendLog` in the session, and the
  diagnostic report), and add a step to [docs/MG4_SWI165_TEST_PLAN.md](docs/MG4_SWI165_TEST_PLAN.md).
- Docs are plain and short, in Australian English. Keep the README "What works" and
  settings tables, `docs/MG4_SWI165.md` status table and `CHANGELOG.md` up to date with
  each feature, and mark anything not yet tried in the car as such.

## Car facts worth knowing

- USB CarPlay: removed from the MG4 UI in mg4.96, because the car's own wired CarPlay holds
  the iPhone. The USB code and `VpnSelfAuthorizer` stay in the tree, unused.
- No ADB on this car: builds are installed from a USB stick. Diagnostics come back as
  the report file (USB or **Download to iPhone**).
- Gear: `vehiclecondition` code 5: 1 P, 2 R, 3 N, 4 D (car test 2026-11-03).
- Wheel raw keys (`com.saic.keyevent.hardkey.report`): 287 voice (opens Siri), 291 phone,
  299 previous, 300 next, 297/298 volume, 301 mute, 286 and 17 star, 288 HVAC.
- Charging status: 1 AC, 10 DC, 5 connecting, 7 connected not charging, 8 stopped.
- Day/night: `Settings.System.screen_day_night_mode` (0 day, 1 night) switches with the
  headlights, and Android's night mode follows it, so CarPlay dark mode follows the car.
  Sunset mode (`SunPosition`) is optional; `DayNightProbe` still logs changes.
- Vehicle code in `shared/` logs to logcat only unless it also calls the session log
  (`VehicleIntegration.setReportLog`). The diagnostic report has no logcat.
