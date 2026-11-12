# Stock CarPlay driving side patch (AllGo)

Patches the car's own CarPlay app (`com.allgo.carplay.service`, the R11 build, versionCode 104) so
its CarPlay bar always sits on the right. This car is right-hand drive. Not tried in the car yet.

## What it fixes

On a cold start the stock app creates its CarPlay session before it reads the driver position
from the car, so the session gets the default left-hand mode and the bar can appear on the
left. See "Driving side race" in `docs/MG4_SWI165_ALLGO_NOTES.md`.

The patch changes three smali lines in `CarPlayService` (see `rhd.patch`):

- `createSession()` always passes right-hand mode to the session config.
- The car connection callback no longer sets the driver position back to left (two lines).

Nothing else in the app changes. The package name, shared user id and platform signature are
the same. The version is 105 (stock is 104) with `-rhd` added to the version name, so it
installs over the stock app.

## Build

You need `apktool` (`brew install apktool`), the Android SDK build-tools and the platform
key (`platform.pk8` and `platform.x509.pem`, never committed).

```sh
MG4_PLATFORM_KEYS_DIR=/path/to/keys \
  tools/allgo-rhd-patch/patch_allgo_rhd.sh /path/to/com.allgo.carplay.service_AllgoCarplay_EH32.apk
```

`MG4_PLATFORM_KEYS_DIR` defaults to this repo's `tools/` folder, as in `scripts/build_mg4.sh`.
The output is `dist/AllgoCarplay-rhd-v105.apk`, signed with the platform key (v1, v2 and v3).
The script prints the certificate; its SHA-1 should end in `...3D:FA`, the same as the stock
app. The stock APK is only read.

## Install

1. Copy `AllgoCarplay-rhd-v105.apk` to a USB stick.
2. Plug it into the car and open the Files app.
3. Tap the APK and install it over the stock app.

## Revert

In Android settings, open Apps, pick the CarPlay app (`com.allgo.carplay.service`) and choose
**Uninstall updates**. That brings back the stock version 104.

## Status

Untested in the car. Test plan section J in `docs/MG4_SWI165_TEST_PLAN.md`.
