# Stock CarPlay patch (AllGo): driving side and cluster directions

Patches the car's own CarPlay app (`com.allgo.carplay.service`, the R11 build, versionCode 104)
to do two things. Not tried in the car yet.

1. The CarPlay bar always sits on the right (this car is right-hand drive).
2. CarPlay turn-by-turn shows on the instrument cluster.

## What it fixes

### Driving side

On a cold start the stock app creates its CarPlay session before it reads the driver position
from the car, so the session gets the default left-hand mode and the bar can appear on the
left. See "Driving side race" in `docs/MG4_SWI165_ALLGO_NOTES.md`.

`rhd.patch` changes three smali lines in `CarPlayService`:

- `createSession()` always passes right-hand mode to the session config.
- The car connection callback no longer sets the driver position back to left (two lines).

### Cluster directions

The stock app receives the iPhone's route guidance but only passes it to its own listeners.
The cluster app takes guidance from the car's `com.saicmotor.adapterservice` `MapService`, so
nothing showed there. `ClusterNavBridge` (our own code, Java in `src/`, smali in `smali/`)
binds that service and sends what MG4CPlay sends: guidance on or off, the next turn's icon,
distance and road, and the time and distance remaining. The icon table and the distance
(the value CarPlay shows, else rounded) match `Mg4ClusterNavigation` in the app.

`cluster.patch` adds four calls, each one line:

- `onNavRouteGuidanceUpdate` and `onNavRouteManeuverInfoUpdate` pass their info to the bridge.
- `StopRouteGuidanceUpdate()` and the nav controller's `unregister()` end guidance.

The bridge catches everything, so a failure never reaches the stock app. It logs to logcat
with the tag `RHD_CLUSTER` (no ADB in the car, so check the cluster by eye).

Guidance only starts when the stock app holds the car's navigation focus, because the stock
app only asks the iPhone for route guidance then. If the cluster stays blank, that is the
first thing to check.

Guesses not yet checked in the car:

- The stock info has no "current maneuver" index, so the bridge shows the lowest-numbered
  maneuver it has been sent and moves on when the distance jumps up after a turn.
- Time remaining is read as [hours, minutes].
- Remaining distance is read as metres.

## Build

You need `apktool` (`brew install apktool`), the Android SDK build-tools and the platform
key (`platform.pk8` and `platform.x509.pem`, never committed).

```sh
MG4_PLATFORM_KEYS_DIR=/path/to/keys \
  tools/allgo-rhd-patch/patch_allgo_rhd.sh /path/to/com.allgo.carplay.service_AllgoCarplay_EH32.apk
```

`MG4_PLATFORM_KEYS_DIR` defaults to this repo's `tools/` folder, as in `scripts/build_mg4.sh`.
The output is `dist/AllgoCarplay-rhd-v106.apk` (versionCode 106, version name `9-rhd-cluster`),
signed with the platform key (v1, v2 and v3). Its certificate SHA-1 should end in `...3D:FA`,
the same as the stock app. The stock APK is only read.

For the bar only, with no cluster code, pass `--rhd-only` (or set `ALLGO_PATCH_CLUSTER=0`):
that builds `dist/AllgoCarplay-rhd-v105.apk` (versionCode 105, `9-rhd`). Test the bar-only
build first. The full build (106) installs over 105.

To change the helper, edit `src/com/allgo/carplay/service/ClusterNavBridge.java`, compile it
with `javac --release 8` against `android.jar` (API 28) and `stubs/`, run `d8` on the two
`ClusterNavBridge*.class` files, and turn the dex back into smali (for example by zipping it
into an APK and running `apktool d --no-res`). Copy the two `.smali` files into `smali/`.

## Install

1. Copy `AllgoCarplay-rhd-v106.apk` to a USB stick.
2. Plug it into the car and open the Files app.
3. Tap the APK and install it over the stock app.

## Revert

In Android settings, open Apps, pick the CarPlay app (`com.allgo.carplay.service`) and choose
**Uninstall updates**. That brings back the stock version 104.

## Status

Untested in the car. Test plan section J in `docs/MG4_SWI165_TEST_PLAN.md`.
