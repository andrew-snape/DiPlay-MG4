# MG4CPlay on SWI165: first install

MG4CPlay is a wireless CarPlay receiver for an Australian MG4 on SWI165 firmware
(`SWI165-29176-1100R11`). It is a personal fork of DiPlay-MG4 and is **not an
Apple-certified product**. It uses an experimental accessory identity, and whether iOS
keeps accepting it is unresolved. The full caveats are in the [README](../README.md).

## Before you start

- **Car:** MG4 on SWI165 firmware. Unknown firmware keeps upstream behaviour, so the car
  features stay off. Where to read the firmware string on the car: not documented yet.
- **iPhone:** the car was tested with an iPhone 15 Pro on iOS 27.2. Bluetooth on, Low
  Power Mode off, and on the mount ([test plan](MG4_SWI165_TEST_PLAN.md)).
- **USB stick:** to carry the APK into the car.
- **The APK:** `MG4CPlay-v<version>.apk` from the
  [Releases page](https://github.com/andrew-snape/DiPlay-MG4/releases) of this fork. The
  current build is 0.3.0.
- **Name and clashes:** car-test builds install as `com.shihab.diplay.hudtest`, alongside
  any upstream `com.shihab.diplay` install. Run one at a time.

## Install from a USB stick

1. Copy the APK to a USB stick.
2. Put the stick in the car and open it in the car's file manager. The file manager's
   name and where it sits in the menus: not documented yet.
3. Tap the APK to install it.

Each build has a higher version number, so it installs over the last one. ADB has never
worked on this car. For the ADB route, see [MG4_PLATFORM_INSTALL.md](MG4_PLATFORM_INSTALL.md).

After installing, open MG4CPlay's About page and check the version matches the build you
installed.

## First connection

- **Connection type:** the car hotspot is the default. **Settings > Connection** holds the
  connection type (car hotspot or Wi-Fi Direct), the hotspot details, the iPhone, and the
  Wi-Fi Direct band.
- **Car hotspot:** turn the hotspot on in the car's own settings (5 GHz if available), then
  copy its name and password exactly into **Hotspot details**. Leave the hotspot on.
  MG4CPlay sends the details to the iPhone over Bluetooth and the iPhone joins by itself
  (from the app's own setup text).
- **Wi-Fi Direct:** needs the car hotspot off and the car's Wi-Fi on. It is in the test
  plan (F5) and not yet confirmed.
- **Pairing the iPhone with the car over Bluetooth:** the test plan assumes the iPhone is
  already paired to the car. How to do this from the car's menus: not documented yet.
- **In-app pairing (test build, mg4.96, not yet tried in the car):** go to
  **Settings > Connection > iPhone > Pair another** with the iPhone's Bluetooth settings
  open. Tap the iPhone, check the code matches on both, then tap **Pair**. If it falls
  back to Android settings, note the step and report it.
- **Connect when MG4CPlay opens** and **Open after the car starts** are on
  **Settings > Connection**, unchanged from earlier builds.

Start the car, open MG4CPlay and wait. The README reports about 4 s from launch to the
first frame. Test plan A1 expects a picture within about 10 s.

## What to expect

| Feature | Status | Notes |
|---|---|---|
| Picture | Working | Overlay video renderer. Fixes the washed-out look |
| Music over Wi-Fi | Working | Music buffer is 750 ms; try 1000 ms if music skips on a weak link |
| Phone calls | Working | Some static when Wi-Fi drops call packets |
| Callers hear you | Not confirmed | Not confirmed on recent builds |
| Wheel voice button (Siri) | Working | Opens Siri on release. From mg4.93, holding it opens Siri straight away |
| Siri hears you | Not working | Dictation writes the wrong words. Fix in test (mg4.97, mg4.98), not yet tried in the car |
| Steering-wheel next and previous | Working | Volume and mute stay with the car |
| Dark mode | Working | Follows the car's headlights. Confirmed in the car on 2026-11-03 |
| Settings screen | In test | Two-pane screen. Built, not yet tried in the car |
| In-app pairing | In test | mg4.96. Not yet tried in the car |
| Car GPS, wheel speed and charging in Apple Maps | In test | mg4.90. Not yet tested in the car |
| Instrument-cluster directions | In test | mg4.80. Not yet confirmed |
| Video while parked | In test | Not yet tested in the car |
| DRM video such as Apple TV+ | Not supported | MG4CPlay is not a licensed FairPlay receiver |
| USB CarPlay | Removed | Removed in mg4.96. Use the car's built-in CarPlay for a cable |

With **Calls over CarPlay only** on (the default on MG4), the car's hands-free is released
during CarPlay, so calls only come through CarPlay. Once CarPlay is up, the car's
Bluetooth music link to the iPhone is disconnected.

## Updates

The app checks this fork's releases when it opens, and from the ↻ button on the home
screen. It needs the head unit online and the repository public. On this head unit the
install itself is refused, so the Downloads app opens and you tap the `MG4CPlay` file to
install it ([OTA.md](OTA.md)).

## If something goes wrong

- **Save a report:** in MG4CPlay, go to **Diagnostics > Save diagnostic report**. During
  wireless CarPlay you can tap **Download to iPhone** instead, then scan the QR code with
  the iPhone camera. The link expires after 5 minutes. The button is greyed out when
  CarPlay is not connected.
- Wait a few seconds after saving before copying the file off. Some uploads have arrived
  empty when copied straight away.
- Reports stay on the car unless you share them. How to copy one to a USB stick: not
  documented yet.
- For step-by-step checks and what to send back, see the
  [test plan](MG4_SWI165_TEST_PLAN.md). Send the report, with the time of each step that
  failed.
