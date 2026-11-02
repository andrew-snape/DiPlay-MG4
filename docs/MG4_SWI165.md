# MG4 SWI165 firmware

Development car: MG4, Australian build, firmware `SWI165-29176-1100R11`, Android 9
(`mt2712_saic_eh32`, build 10749), 1920x720 panel with a 1778x720 app area at
160 dpi. It runs MG4Dash as the home screen and MG4Control. ADB over USB has never
worked on this car, so builds are installed from a USB stick.

DiPlay reads `ro.build.mt2712.version` at start-up and picks a firmware profile
(`common/.../Mg4FirmwareProfile.kt`). Screen size, signing and install steps are the
same as SWI69: platform key, `pm install -r -f -d -t` from `/data/local/tmp`.

| | SWI69 | SWI165 |
|---|---|---|
| Stock launcher package | `com.saicmotor.launcher` | `com.saicmotor.hmi.launcher` |
| Launcher CarPlay card shortcut | On (measured) | Off until the card is measured |
| Default video renderer | TextureView | SurfaceView (overlay) |
| Vehicle data (EVHardware) | Supported | Supported |

## Video renderer

The stock `com.allgo.carplay.service` (AllgoCarplay_EH32) decodes CarPlay video
straight into a `SurfaceView`, so frames go to the display's hardware overlay plane.
DiPlay's original `TextureView` path composites the video on the GPU instead, which
looks washed out next to the stock app on SWI165.

SWI165 now defaults to the `SurfaceView` path. **Settings > Display and performance >
Overlay video renderer** switches between the two on any firmware. The choice applies the next time
CarPlay opens. The diagnostic log records which renderer and firmware profile were used:

```text
Video renderer SurfaceView firmware=SWI165-29176-1100R11 profile=SWI165
```

## Launcher shortcut

The SWI165 launcher's CarPlay card has not been measured, so the transparent touch
target is not created on this firmware. Measuring it needs a screenshot of the stock
home screen (`adb exec-out screencap -p > launcher.png`); add the rectangle to the
SWI165 branch of `Mg4FirmwareProfile.forFirmware`. Cars running a replacement home
screen such as MG4Dash never show the stock card, so they don't need it.

## Status

| Item | State |
|---|---|
| Firmware detection and profile | Done, unit tested |
| Overlay video renderer | Confirmed on the car, 2026-11-01 (H.264 and HEVC, iPhone 15 Pro, iOS 27.2) |
| Launcher CarPlay-card shortcut | Not needed with MG4Dash; card not measured |
| Wi-Fi audio instead of Bluetooth | Music plays on the car (mg4.65). Default on SWI165 from mg4.66, with the car’s Bluetooth music disconnected while CarPlay runs. mg4.67 and mg4.68 lost all CarPlay audio (Opus microphone input no longer offered); mg4.69 restores it and encodes the call microphone in software. Calls not yet confirmed. See [MG4_SWI165_WIFI_AUDIO_PLAN.md](./MG4_SWI165_WIFI_AUDIO_PLAN.md) |
| Battery and range to the iPhone | Confirmed on the car, 2026-11-01 |
| Switch to Bluetooth Music on connect | mg4.65 stayed on radio: command 15 does not exist on SWI165. Fixed in mg4.66 to send `play` (2), which requests the car’s Bluetooth source. Awaiting a car test |
| Car GPS and wheel speed to the iPhone | mg4.90: location on by default on MG4. Speed is the AOSP vehicle-speed property (EVHardware scales it per firmware); gear is `vehiclecondition` code 5, where only 1 = P is known, so other gears are sent as D. Awaiting a car test (test plan D5, D6) |
| Charging in Apple Maps | mg4.90: `isCharging` from the car's charging status (1 AC, 10 DC; 5 connecting, 7 connected, 8 stopped are not charging), falling back to the plug or pack power. Awaiting a car test (D7) |
| Dark mode | Found 2026-11-03 (mg4.91 probe): the car's day/night is `Settings.System.screen_day_night_mode` (0 day, 1 night), with `SAIC_DAY_NIGHT_MODE_FROM_CAR=1`. Switching the headlights on sets it to 1, and Android's night mode (`Settings.Secure.ui_night_mode` 2, `uiNight=true`) follows within a second, so CarPlay follows too. mg4.92 makes this the default; sunset mode is optional and falls back to the car until a position is known |

## Signing note

`com.allgo.carplay.service` on SWI165 is signed with the same AOSP platform
certificate as DiPlay (SHA-1 `27:19:6E:38:6B:87:5E:76:AD:F7:00:E7:EA:84:E4:C6:EE:E3:3D:FA`).
DiPlay still installs under its own package and leaves the AllGo stack in place,
because the launcher, SystemUI and mirroring control bind to AllGo's services.
