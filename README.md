# MG4CPlay (SWI165 fork)

Wireless CarPlay for the MG4 head unit. This is a personal fork of
[fatihdonmezdev/DiPlay-MG4](https://github.com/fatihdonmezdev/DiPlay-MG4), adapted
for an Australian **MG4 on SWI165 firmware** (`SWI165-29176-1100R11`, Android 9,
AUTUS SAIC MT2712, 1920x720 panel). Upstream is tested on SWI69.

Current build: **0.4.0**. Car-test builds install as
`com.shihab.diplay.hudtest`, alongside any upstream `com.shihab.diplay` install.
Run one at a time. The version has no test suffix from 0.3.2 on.

Versions are `MAJOR.MINOR.PATCH` from 0.3.0 on. Earlier test builds were `0.2.9-mg4.N`,
so "mg4.N" in the tables below means one of those builds. See [Versions](#versions).

## What works on SWI165

| Feature | Status |
|---|---|
| Wireless CarPlay over the car hotspot | Working, about 4 s from launch to first frame |
| Picture (overlay video renderer) | Working, fixes the washed-out look |
| Battery and range sent to the iPhone | Working (Apple Maps EV features) |
| Music over Wi-Fi (CarPlay audio) | Working. Broken in mg4.67 and mg4.68 (no CarPlay audio at all); fixed in mg4.69 and confirmed in the car in mg4.71 |
| Phone calls over Wi-Fi | Working in mg4.83: the call played through the car's phone audio path. Some static occurs when Wi-Fi drops call packets (30–60% loss in one car test); the cause is the Wi-Fi link, not the routing |
| Call microphone | Working. Software Opus encoder on Android 9 (mg4.69), about 5 ms per frame (mg4.83). From 0.3.3 calls use the plain microphone by default, as the car's own CarPlay does (not yet tried in the car). Echo cancellation is on from 0.3.1 (not yet tried in the car) |
| Wheel voice button opens Siri | Works (mg4.92, 2026-11-03), but only on release. mg4.93: holding the button opens Siri straight away; a short press still opens it on release |
| Steering-wheel next and previous | Working (mg4.66, re-confirmed mg4.80) |
| Hands-free released during CarPlay | Working (mg4.80); released again on each reconnect (mg4.81) |
| Square corners | Confirmed in the car (mg4.70), default since mg4.75 |
| Siri and Maps voice prompts | Siri audio clean in mg4.71 test; Maps voice prompts not yet tested |
| Siri hears you | Not working: Siri dictation writes the wrong words. mg4.97 fixed the microphone's RTP clock (24 kHz for Siri) and mg4.98 adds a microphone recording and a PCM option to find the cause. Neither is tried in the car yet |
| Callers hear you | Not confirmed on recent builds. 0.3.3 changes the call microphone to the plain microphone (test plan D2i and C8). mg4.98 records what callers are sent (test plan D2d) |
| Instrument-cluster directions | Test build (mg4.80), not yet confirmed in the car |
| Bluetooth Music mode (fallback) | Fix in mg4.66, awaiting a car test |
| Car GPS and wheel speed to the iPhone | Test build (mg4.90): location on by default on MG4, with the car's own speed for tunnels (no adb). Not yet tested in the car |
| Charging shown in Apple Maps | Test build (mg4.90): uses the car's charging status (AC or DC session), not just the plug. Not yet tested in the car |
| In-app pairing | Test build (mg4.96): Pair another searches and pairs the iPhone without leaving the app. Not yet tried in the car |
| USB CarPlay | Removed in mg4.96. The car's own wired CarPlay holds the iPhone, so MG4CPlay cannot claim it. Use the car's built-in CarPlay for a cable |
| Wi-Fi high performance | Test build (mg4.91): keeps the car's Wi-Fi out of power saving during CarPlay (from xcertplay 1.4.1). Being tested against the 160 s freezes |
| Video while parked | Built in (upstream feature, gated on the MG4's gear P). Not yet tested in the car |
| Call echo | Test build (0.4.0): software echo suppression on calls, since the head unit has no echo canceller. Not yet tried in the car |
| Car MFi chip | Test build (0.4.0): read-only probe of the car's MFi chip, plus an optional switch to sign with it. Not yet tried in the car |
| Stock app ports | Test build (0.4.0): Telecom call mirroring, car phone mode, music source tag, screen size, narrow-band calls and more. Not yet tried in the car |
| Dark mode | Follows the car's own day/night, which switches with the headlights. Confirmed in the car (2026-11-03): headlights on turned the car, Android and CarPlay dark. Dark mode at sunset is an option (mg4.92: off by default) |

The full test record is in [docs/MG4_SWI165.md](docs/MG4_SWI165.md).

## What this fork changes

| Area | Change |
|---|---|
| Firmware profile | Reads `ro.build.mt2712.version` at start-up and applies SWI69 or SWI165 settings. Unknown firmware keeps upstream behaviour. |
| Video | SWI165 draws CarPlay on the hardware video overlay (`SurfaceView`), like the stock AllGo CarPlay app. |
| Audio | SWI165 plays CarPlay music and calls over Wi-Fi through DiPlay. Once CarPlay is up, the car's Bluetooth music link to the iPhone is disconnected; hands-free stays connected and pairing is kept. Call audio plays like the stock CarPlay app: voice audio tagged as car source 53, with the car in call mode (mg4.83). |
| Calls over CarPlay only | Optional, on by default on MG4 (mg4.82). Keeps hands-free released for the whole CarPlay session, so calls can only come through CarPlay. A short press of the wheel phone key answers or ends a CarPlay call (mg4.78). |
| Steering wheel | SWI165 sends wheel keys as a SystemUI broadcast, not Android media keys. DiPlay listens for it in Wi-Fi mode and sends next and previous to CarPlay. Volume and mute stay with the car. |
| Instrument cluster | Test only (mg4.80): sends CarPlay's next turn, distance and street to the cluster's navigation area through the car's `MapService`. |
| Bluetooth Music mode | Kept as a fallback. Switches the car to Bluetooth Music with the media service's `play` command, as read from the car's own MediaService. |
| Location | On by default on MG4 (mg4.90). Sends the car's GPS with its own wheel speed and gear (`$PASCD`), read directly from the car, so Apple Maps keeps moving in tunnels. Gear is 1 P, 2 R, 3 N, 4 D (car test 2026-11-03); an unknown code is sent as D. |
| Wi-Fi | Holds Android's high-performance Wi-Fi lock during wireless CarPlay (mg4.91, from xcertplay 1.4.1). Location sends each NMEA sentence as its own parameter, only the types the iPhone asked for, at its intervals (mg4.91, from xcertplay 1.4.1). |
| Charging | Apple Maps' "charging" comes from the car's charging status (1 AC, 10 DC) (mg4.90). Plugged in but stopped or finished is not charging. |
| Dark mode | Follows the car's day/night (`screen_day_night_mode`), which Android's night mode tracks and which switches with the headlights (found by the mg4.90 probe, 2026-11-03). Optional sunset mode (mg4.90). |
| Build fixes | Replaces upstream calls to Bluetooth functions that were never published, so the tree compiles. The build script finds the Android SDK and writes the APK to `dist/`. |
| Stock app behaviour | Not yet tried in the car: Telecom mirroring of calls, car phone mode, music source tag, vendor call listener, 200 ms phone focus retry, display report log, and the mute key silencing Siri. See the Sound settings and test plan section I. |
| Diagnostics | Reports record the firmware profile, video renderer, audio mode, car audio source, microphone source and every wheel key. |

## Settings

On MG4, **Settings** is a two-pane screen like iOS Settings: a list of categories on the left and the chosen category on the right. It has Connection, Display, Sound, Car, General, Help and About; every setting is two taps from Home. The screen is built but not yet tried in the car. Other head units keep the upstream settings page.

Some settings only reach a running CarPlay session after it reconnects. Those rows carry a **Reconnect** tag, and changing one while connected shows **Some changes apply after CarPlay reconnects. Reconnect now**. Dark mode at sunset, Car battery on iPhone and Video while parked take effect straight away.

| Setting | Where | Default on SWI165 |
|---|---|---|
| Connection type (car hotspot or Wi-Fi Direct), hotspot details, iPhone (choose or pair in the app), Wi-Fi Direct band | Connection | Car hotspot. Replaces the old Connection setup page. |
| Connect when MG4CPlay opens, Open after the car starts | Connection | As before. |
| Wi-Fi high performance | Connection | On (mg4.91). Turn off to compare freezes and skips. |
| CarPlay size, Right-hand drive, Full screen | Display | As before. |
| Resolution, Frame rate, Efficient video (HEVC) | Display | Native, 60 fps, on. HEVC is half the Wi-Fi data of H.264; turn off to compare colours. |
| Overlay video renderer, Square corners, Colour range (test) | Display > Advanced | On, on, from the video. |
| CarPlay audio over Wi-Fi, Calls over CarPlay only | Sound | On, on. Off uses the car's Bluetooth Music; calls-only disconnects the car's hands-free while CarPlay runs. A saved choice still wins. |
| Music buffer | Sound | 750 ms. Try 1000 ms if music skips on a weak Wi-Fi link. 2000 ms (mg4.96) rides out a short dropout and adds about a second of music delay. |
| Echo suppression on calls | Sound | On (not yet tried in the car). The head unit has no echo canceller, so DiPlay turns the call microphone down while it only hears the other person's own voice coming back from the speakers. |
| Microphone (test) | Sound | Automatic uses the plain microphone for calls and voice recognition for Siri, the same as Stock. Plain microphone for both or call processing can be chosen to compare. |
| Show calls to the car (test), Car phone mode for calls (test), Tag music as CarPlay (test) | Sound | Off, off, off. Copy the stock CarPlay app: mirror calls into the car's Telecom, tell the car's audio policy a CarPlay call is on, mark music as the car's CarPlay source. Not yet tried in the car. |
| Screen size reported to iPhone | Display | Automatic. Stock (244 x 80 mm) tells the iPhone the car's own app's physical size, which changes how big CarPlay draws icons and text; the picture size is unchanged. Applies after you reconnect. Not yet tried in the car. |
| Narrow-band calls (test) | Sound | Off. Offers the iPhone only 8 kHz audio for calls, as the car's own app can. Siri and music are unchanged. Applies after you reconnect. Not yet tried in the car. |
| Use car's MFi chip (test) | Car > Authentication | Off. Uses the car's own Apple authentication chip instead of the identity built into the app. Applies after you reconnect; if the chip can't be used the app falls back to its own identity for that connection. Not yet tried in the car. The app also always probes the chip read-only and writes the result to the report. |
| Siri microphone as PCM (test), Record microphone (test) | Sound | Off, off (mg4.98). PCM sends Siri plain audio instead of Opus. Recording keeps the first 10 s of each Siri request and call on the car; **Download to iPhone** sends them with the report as a zip. |
| Send location to iPhone | Car > Driving | On (mg4.90). Asks for location permission once. Sends the car's wheel speed with it. |
| Video while parked | Car > Driving | Off. Lets iOS 27 video apps play on the car screen, only while the car reads gear P. DRM video such as Apple TV+ won't play. |
| Dark mode at sunset | Car > Appearance | Off (mg4.92). Off follows the car's day/night, which changes with the headlights. On uses sunset and sunrise at the car's position. |
| Car battery on iPhone, Directions on the dashboard (test) | Car | As before. |
| Diagnostics, updates, permissions | General, Help, About | Settings changes are listed in the diagnostic report. |

Hidden on MG4 because the firmware profile sets them: audio focus, advanced audio channel mapping, and the music audio channel.

## Versions

`MAJOR.MINOR.PATCH`, kept in `version.properties` (`versionName` and `versionCode`).
Minor is a new feature or setting, patch is a fix only, major needs a manual step or
breaks saved settings. `versionCode` only ever goes up, because Android refuses to
install a lower one and the car has no ADB to force it. Debug builds add `-hud-test`
to the name. Builds from 0.3.2 have no suffix; 0.3.1 and earlier added `-hud-test`, which the update check still ignores.

Work lands on `main` under `# Unreleased` in `CHANGELOG.md`. A release is cut when the
car needs one:

```bash
scripts/bump_version.sh minor    # or major, patch: bumps both numbers, dates the changelog heading
```

Commit that on its own (`chore: release X.Y.Z`), then run the `Release` workflow on
`main` with tag `vX.Y.Z`. Details: [docs/VERSIONING.md](docs/VERSIONING.md) and
[docs/RELEASING.md](docs/RELEASING.md).

## Building on a Mac

**Keep the repo out of iCloud.** If it sits in `~/Documents` with iCloud Desktop &
Documents syncing on, iCloud makes `name 2.xml` copies of build files and the build
fails with "Duplicate resources". Either turn that syncing off, or clean before
each build:

```bash
./gradlew --stop && rm -rf build */build .gradle
```

```bash
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties
```

The car build needs the CarPlay identity files, which are never committed. Copy
them out of any upstream release APK (`/tmp/diplay-release.apk` here) into
`.private/`, which git ignores:

```bash
mkdir -p .private/runtime-assets && unzip -o /tmp/diplay-release.apk 'assets/offline-mfi/*' -d /tmp/diplay-mfi && cp -R /tmp/diplay-mfi/assets/offline-mfi .private/runtime-assets/
```

Each build, in the same terminal window (Java from `brew install openjdk@21`, or use
Android Studio's at `/Applications/Android Studio.app/Contents/jbr/Contents/Home`). This Java only
starts Gradle: the first build downloads Java 25 and builds with it, the same as CI:

```bash
export JAVA_HOME="$(brew --prefix openjdk@21)/libexec/openjdk.jdk/Contents/Home" && export DIPLAY_AUTH_ASSETS_DIR="$PWD/.private/runtime-assets"
```

```bash
./scripts/build_mg4.sh
```

The signed APK lands in `dist/MG4CPlay-v<version>.apk` (git-ignored), named from `version.properties`. The first
build downloads Gradle, the NDK and Android SDK 37, so it takes a few minutes.

## Installing

**USB stick (no ADB):** copy the APK to a USB stick, open it in the car's file
manager and tap it to install. Each release has a higher `versionCode`, so it
installs over the last one. A rebuild of the same version will not install over itself.

**ADB:** the APK is platform-signed and installs as a system app, so the usual
`adb install` is refused by this head unit. Use:

```bash
adb push MG4CPlay-v<version>.apk /data/local/tmp/update.apk
```

```bash
adb shell "pm install -r -f -d -t /data/local/tmp/update.apk"
```

See [docs/MG4_PLATFORM_INSTALL.md](docs/MG4_PLATFORM_INSTALL.md) for why `-f`
matters.

## Diagnostics

In DiPlay: **Diagnostics > Save diagnostic report**, or **Download to iPhone**
during wireless CarPlay. Reports stay on the car unless you share them. On MG4 the
report has a `Day/night:` line, and the log has `Dark mode on (sunset)`,
`gear raw=` and `chargeStatus=` lines for the mg4.90 features. Wait a few seconds after saving before copying the file
off; some uploads have arrived empty when copied straight away.

## Updates

The in-app update check looks at **this fork's** releases (`andrew-snape/DiPlay-MG4`),
when the app opens and from the ↻ button on the home screen. It offers the highest
published release newer than the installed version, comparing only the numbers (so
`0.3.2` is newer than `0.3.1-hud-test`), and only an `MG4CPlay-…apk` with a
GitHub SHA-256 digest. After download it checks the digest, package name and signing key,
then installs, or opens Downloads if the head unit refuses. It needs the head unit online
and the repo public. The diagnostic report shows the last check (`Update check:`). See
[docs/OTA.md](docs/OTA.md).

## Caveats

This is **not an Apple-certified product**. The APK bundles an experimental
accessory identity recovered from public Carlinkit firmware, not an MFi identity
provisioned for this app. The bundled private key is extractable. Whether iOS
keeps accepting it is unresolved.

DRM-protected video such as Apple TV+ is not supported; this is not a licensed
FairPlay receiver.

## Documentation

- [SWI165 firmware profile and test record](docs/MG4_SWI165.md): start here
- [First install on SWI165](docs/MG4_SWI165_FIRST_INSTALL.md): install, pair and what to expect
- [Wi-Fi audio plan and car test checklist](docs/MG4_SWI165_WIFI_AUDIO_PLAN.md)
- [Stock AllGo CarPlay app notes](docs/MG4_SWI165_ALLGO_NOTES.md): how the car's own CarPlay app handles calls and audio
- [Release test plan: calls, music, Bluetooth and Wi-Fi](docs/MG4_SWI165_TEST_PLAN.md)
- [Release notes](CHANGELOG.md)
- [Instructions for Claude](CLAUDE.md), and the [claude.ai Project instructions](docs/claude-project-instructions.md)
- [Platform-signed builds](docs/MG4_PLATFORM_BUILD.md)
- [Installing over ADB](docs/MG4_PLATFORM_INSTALL.md)
- [Versioning strategy](docs/VERSIONING.md)
- [Upstream build, release and OTA notes](docs/RELEASING.md), [OTA](docs/OTA.md)

## Source and credits

Forked from [fatihdonmezdev/DiPlay-MG4](https://github.com/fatihdonmezdev/DiPlay-MG4)
by [@fatihdonmezdev](https://github.com/fatihdonmezdev). Based on
[xcertplay](https://github.com/shilapi/xcertplay), GPL-3.0. The home/settings UI
adapts [DiAuto](https://github.com/shihabal3amri/DiAuto), AGPL-3.0; that license is
included in `docs/licenses`. Preserve those notices when distributing modifications.

The Android 9 call microphone uses [Concentus](https://github.com/lostromb/concentus)
(BSD-3-Clause), a pure-Java Opus encoder.

SWI165 media and steering-wheel details were read from the car's own MediaService
APK and cross-checked against [MG4Control](https://github.com/SliDeeN/MG4Control),
[MG4Dash](https://github.com/Skittle6938/MG4Dash) and an SWI68 launcher project.

CarPlay and its icon belong to Apple Inc.; no affiliation or endorsement is implied.
Accessory and signing keys are excluded from source archives; a car build selects
the identity explicitly through `DIPLAY_AUTH_ASSETS_DIR`. Publishing an APK makes its
bundled identity extractable.
