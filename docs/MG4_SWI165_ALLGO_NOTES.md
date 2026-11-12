# Stock AllGo CarPlay on SWI165: what it does

Read from `com.allgo.carplay.service_AllgoCarplay_EH32.apk`, exported from the car and
decompiled with jadx 1.5.6. The APK is a Java wrapper around system native libraries
(`libjni_carplay`, `libjni_carplayscreen`). Those were not exported, so the CarPlay
protocol, audio playback and microphone capture are not visible here. The audio
focus, call and Bluetooth logic is in Java.

The app runs as `android.uid.system` (`sharedUserId`).

## Audio: each type tags its car source

The stock app builds `AudioAttributes` with a hidden extras bundle,
`key_car_source_type`, which tells the SAIC audio policy which car source is playing
(`CarPlayAudioManager` constructor):

| CarPlay audio | Usage | `key_car_source_type` | Focus | Audio mode |
|---|---|---|---|---|
| Media | 1 `USAGE_MEDIA` | 50 | `AUDIOFOCUS_GAIN` | unchanged |
| Navigation and alternate audio | 12 `USAGE_ASSISTANCE_NAVIGATION_GUIDANCE` | 52 | `GAIN_TRANSIENT` | unchanged |
| Phone call | 2 `USAGE_VOICE_COMMUNICATION` | 53 | `GAIN_TRANSIENT` | `MODE_IN_CALL` (2) |
| Ring and alert | 6 `USAGE_NOTIFICATION_RINGTONE` | none | `GAIN_TRANSIENT` | `MODE_RINGTONE` (1) |
| Siri | 16 `USAGE_ASSISTANT` | none | `GAIN_TRANSIENT` | unchanged |

Built with `new AudioAttributes.Builder().setUsage(u).addBundle(bundle)`.
`addBundle` is `@SystemApi`, so DiPlay would call it by reflection; the platform-signed
build should be allowed to.

On phone focus it calls `setMode(MODE_IN_CALL)`, and on release it abandons focus and
calls `setMode(MODE_NORMAL)`. If phone focus is refused, it waits 200 ms and asks again.

The Bluetooth source in MediaService uses `key_car_source_type=5` (see
`Mg4BluetoothSource.kt`), so these values look like SAIC's source IDs: 5 Bluetooth,
50 CarPlay media, 52 CarPlay navigation, 53 CarPlay phone.

**For DiPlay:** `AndroidMediaSink.channelOverride` plays CarPlay calls on
`STREAM_MUSIC` because plain `USAGE_VOICE_COMMUNICATION` came out silent (mg4.65). The
stock app plays calls on `USAGE_VOICE_COMMUNICATION` too, but adds source 53 and
`MODE_IN_CALL`. Those two missing pieces are the likely reason the call was silent.
Trying the stock combination for calls, and source 50 for media, should get calls onto
the car's real call path: the call volume group, ducking and possibly the hands-free
microphone tuning.

## Calls: a self-managed Telecom account

`CarPlayTelephonyManager` registers a `PhoneAccount` named `CarPlayPhone` with
capability 2 (`CAPABILITY_CALL_PROVIDER`), through
`CarPlayTelephonyManager$CarPlayConnectionService`. It enables the account and makes it
the user-selected outgoing account.

`MirroringControllerService` mirrors each CarPlay call into Telecom when the iPhone
reports it:

- Ringing (state 2): `TelecomManager.addNewIncomingCall(handle, extras)` with
  `INCOMING_CALL_ADDRESS`, `CALL_EXTERNAL_RINGER=true` (the iPhone's ring plays over
  CarPlay audio, not the car's ringer), and `CARPLAY_CALL_UUID`, `_REMOTE_ID`, `_NAME`
  and `_STATE`, plus `carplay_call=true`.
- Sending (state 1): `addNewUnknownCall` (hidden, called by reflection) with the same
  extras.
- When the session stops, every connection is disconnected and destroyed
  (`finalizeTelephonyConnection`).

This makes the car's Telecom, the SAIC BTCall UI and the audio policy see a call in
progress, which is probably what gives the call its audio path and stops the car
treating it as music. DiPlay has no Telecom integration today.

The stock app also listens to a vendor `PhoneStateListener` callback,
`onCallStatusChanged(state, type, number)` (listen mask `0x60000`), where type 0 is a
native Bluetooth call and type 2 is a CarPlay call. It uses this to hand audio back
when a native hands-free call starts.

## Bluetooth hands-free

The stock app does **not** disconnect hands-free or A2DP. It only opens
`BluetoothHeadsetClient` and `BluetoothA2dpSink` proxies to check whether a native call
in progress is on the same iPhone; if so, it starts CarPlay with audio
"borrowed/never" until that call ends. That matches DiPlay keeping hands-free connected
("Calls over CarPlay only" is the exception).

## Screen

The stock app hard-codes a 1920x640 logical screen, 244 x 80 mm physical. It reads
`sys.screen.widthpixel` / `heightpixel` and then overrides them. DiPlay uses 1920x720
(the full panel). The physical size gives the right pixel density for iOS layout.

## Other useful pieces in the APK

- SAIC SDK binder interfaces bundled in the app: `com.saicmotor.sdk.btcall.IBtCall` /
  `BtCallManager`, `media.IMusicPlayerBinderInterface`, `systemsettings.IBtService`,
  `vehiclesettings.*` (charging, condition), `radio`, `engmode`.
- `com.allgo.mirroring.control.NavRouteGuidanceUpdatesController`: CarPlay turn-by-turn
  for the cluster.
- `CarPlayTelephoneKey`, `CarPlayPlayKey`, `CarPlayKnob`: how wheel and phone keys go
  to CarPlay.

## Second read, Nov 2026 (R11 APK, jadx)

Same APK, from `Car Exports/apps` (the car's own R11 build). Read only; nothing patched
or tried in the car.

### Driving side race (the CarPlay bar on the left)

- `onCreate()` calls `Car.createCar(this, mCarConnection, mHandler)` and `connect()`,
  then `startCarPlayService()` posts `createSession()` to the same `mHandler` thread.
- `createSession()` copies the static `mRightHandDrive` (default `false`) into
  `ConfigInfo.setRightHandMode`. It runs once per service start, retrying only on error.
- The driver position (`CAL_DRIVE_HDL_TYPE`, `0x01` right) is read later, in
  `mCarConnection.onServiceConnected`, which is queued behind the session start on the
  same thread.
- So the first start after a cold boot most likely gets left-hand mode and the bar on the
  left. `mRightHandDrive` is static, so a service restart in the same process keeps the
  last read, which would explain why it only happens sometimes.
- Fix for a patched APK: create the session from `onServiceConnected`, after the read.
  A simpler patch for this right-hand-drive car is to set `mRightHandDrive = true`.
- The `Driver position` log line is logcat only, so with no ADB the fix can only be
  checked by watching which side the bar is on.

### Keys

| Key | Stock behaviour |
|---|---|
| 287 voice | Short press: Siri prewarm then button down; release: button up. Long press: button down. Ignored while the car's own voice assistant has voice focus. |
| 291 phone | Short press: CarPlay telephone hook switch. Long press ignored. |
| 164 mute | Only acts while Siri is active: borrows audio, and mutes the alert group during an alert. |
| Volume up | After a mute, gives audio back to CarPlay (unborrow). |

### Display off

The broadcast `com.saic.conn.display.report` (boolean `state`) tells the app the screen
turned off or on. Off: it gives the screen back to the car (borrow). On: it asks for the
CarPlay screen again.

### Wireless code

The Java layer has wireless paths: `CLIENTTYPE_WIFI` (2) and `CLIENTTYPE_USB_WIFI` (3),
`mActiveTransport`, `WirelessCarPlayUpdate`, Wi-Fi credential and Bluetooth out-of-band
pairing interfaces. Whether the native library and the car's Wi-Fi and Bluetooth
pairing support it on R11 is unknown. The firewall still drops TCP 7000, where stock
wireless would listen.

### Not visible

Call audio, Siri audio and the microphone are handled in `libjni_carplay` and
`libjni_carplayscreen`, which weren't exported. Copying those from `/system/lib*` to
USB would show which microphone source and processing the stock app uses.
