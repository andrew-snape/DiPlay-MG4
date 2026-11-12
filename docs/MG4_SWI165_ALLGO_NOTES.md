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

## Native libraries (R11)

Disassembled from the car's own libraries (`libCarPlayPlatformAudio.so`, `libAirPlay.so`, `carplay.rc`, `arp_update.sh`), Nov 2026. Read only. Nothing here has been tried in the car.

- **Call audio.** The stock library plays calls on native stream type `VOICE_CALL` (0). Before it starts it calls `AudioSystem::setDeviceConnectionState(device=0x400, AVAILABLE, address="", name="carplay_source=phone")`, and with `UNAVAILABLE` when the call track stops. This looks like a vendor audio-policy hook that switches the car into CarPlay phone mode (routing, and probably echo cancellation). MG4CPlay now has it behind **Car phone mode for calls (test)**, called by reflection (`(int, int, String, String)` on Android 9), paired on and off, and cleared on session stop.
- **Microphone.** Calls use `AudioRecord` source 1 (`MIC`) with a vendor input flag `0x200`. Siri uses source 6 (`VOICE_RECOGNITION`) with vendor flag `0x100`. Java cannot set those flags. The **Microphone (stock)** option uses `MIC` for calls and leaves Siri as it is.
- **Narrow band.** The property `persist.sys.carplay.narrow_band` is read at start (narrow-band call audio). Not used here.
- **No stock wireless.** `libAirPlay.so` reports `wireless=no` in its `$Features` string, so stock wireless CarPlay is compiled out. `carplay.rc` and `arp_update.sh` show stock CarPlay is IP over USB NCM, with `ip6tables` letting port 7000 through on `usb0` only. That agrees with the firewall dropping TCP 7000 on other interfaces.
- **Exporting more.** Diagnostics now copy `libjni_carplay*.so` and `libcarplay*.so` from the system and vendor library folders (test plan I1).

## Ported into MG4CPlay (Nov 2026, not yet tried in the car)

Telecom mirroring of calls, vendor `onCallStatusChanged` listener (mask `0x60000`), 200 ms phone focus retry, music source tag 50, display report log, mute key during Siri, car phone mode. Test plan section I. For the display report, the stock code treats `state=true` as the display going off; this is unconfirmed, so the app logs both and only asks for a key frame when the display comes back.

## Car MFi chip

Read from the car's own libraries (`libmfi_cp.so`, `libCarPlayPlatformMFI.so`, `libiap2.so`), Nov 2026. The `mfid` daemon itself was not available, so its side is inferred from what the client accepts. Nothing here has been tried in the car.

The stock CarPlay does not read the Apple authentication chip itself. It asks `/system/bin/mfid` (started as `mfid i2c /dev/i2c-3 0x10 0`) over a Unix socket.

**Socket.** `AF_UNIX`, `SOCK_STREAM`, filesystem path `/dev/socket/MFISocket` (init: `0660 root system`, so uid system is fine by file permissions; SELinux is unknown). The stock client keeps one connection open and has no timeout; MG4CPlay opens one connection per call with a 3 s read timeout.

**Frame, both directions.** A little-endian u32 body length, then a protobuf body. The stock reader uses a 1024-byte buffer, so replies stay under about 1020 bytes.

```
RPCProtoMessage { int32 tx_id = 1; int32 opcode = 2; repeated ArgMessage argument = 3; }   // proto2, all required
ArgMessage      { int32 datatype = 1; bytes argument = 2; }
bytes on the wire: 08 <tx_id> 10 <opcode> { 1A <len> 08 <datatype> 12 <len> <data> }*
datatype: 4 = u32 LE, 5 = s32 LE, 8 = blob.  At most 12 args.
```

The reply echoes `tx_id` and `opcode`. Reply arguments come in a fixed order and the last is always an s32 status (0 is OK; an error reply probably has only the status).

| Opcode | Call | Request args | Reply args (then s32 status) |
|---|---|---|---|
| 0 | GetDeviceId | none | u32 id: 0x200 is MFi 2.0C, 0x300 is 3.0 (from Apple's register map, unconfirmed here) |
| 1 | GetCertificate | u32 buffer capacity (the stock app sends 1024) | blob: the DER certificate |
| 2 | CreateSignature | blob: the digest, raw | blob: the signature |
| 3 | ReadCertificateSerialNum | none | blob: ASCII hex string, at most 30 characters |

Example frames (tx_id 0): GetDeviceId `04 00 00 00 | 08 00 10 00`; GetCertificate with capacity 1024 `0E 00 00 00 | 08 00 10 01 1A 08 08 04 12 04 00 04 00 00`. CreateSignature of a 20-byte digest with tx_id 1 is `1E 00 00 00 | 08 01 10 02 1A 18 08 08 12 14 <20 bytes>`.

**What is signed.** Neither `libmfi_cp` nor the wrapper hashes anything: the caller hashes and the chip signs the bytes it is given. For CarPlay's AirPlay auth-setup that is SHA-1 over the two X25519 public keys on a 2.0C chip (20 bytes, 128-byte RSA-1024 signature) and SHA-256 on a 3.0 chip (32 bytes, 64-byte ECDSA signature). For iAP2 the phone's challenge goes through unchanged. The chip is probably 2.0C: `libiap2` has a 2.0C register driver, a 128-byte signature buffer and a 20-byte challenge path.

**In MG4CPlay.** `CarMfiClient` speaks this protocol (pure Kotlin protobuf, one call at a time, tx_id and status checked). `CarMfiProbe` runs the four calls once per run on MG4 and logs the result (id, serial, certificate size and a short SHA-256 prefix, signature size, timings, SELinux context, and the exact exception if the socket is refused). `CarMfiAuthenticator` is the `MfiAuthenticator` behind the **Use car's MFi chip (test)** switch. The existing AirPlay and iAP2 code already pick the digest and pass the challenge by `protocolMajor()` (2 or 3), so both paths use the car chip with no other change. An id other than 0x200 or 0x300 is logged as not supported by this build, and the bundled identity (P-256, MFi 3.0 style, 32-byte digest) is used instead. The certificate and signatures always come from one source for a connection.

**Unknowns to settle in the car.** Whether the app's SELinux domain may connect to the socket; whether `mfid` accepts a connection per call, and whether it works while the stock CarPlay runs; the real status codes; and whether the iPhone accepts a 2.0C certificate for wireless CarPlay (not only wired). Test plan I13 and I14.

## Microphone, from the car's native libraries (2026-11-12)

Pulled from the car: `/system/lib64/libCarPlayPlatformAudio.so` (disassembled; not tried in
the car).

- The stock audio platform opens one `AudioRecord` per input stream: telephony uses
  `AudioSource.MIC` (1) and Siri uses `VOICE_RECOGNITION` (6). It never asks for
  `VOICE_COMMUNICATION` (7). Both are mono, 16-bit PCM, read in 512-byte chunks (256 bytes for
  8 kHz narrowband telephony, set by `persist.sys.carplay.narrow_band`).
- It attaches no echo canceller. Echo handling on stock comes from the head unit's own mic path.
- It calls `AudioSystem::setDeviceConnectionState`; what for is not yet known.
- A format table in the same library lists mic formats of 8, 16 and 24 kHz mono for the
  telephony and Siri streams (reading of the table is tentative).
- DiPlay's Automatic setting now follows this: plain microphone for calls, voice recognition
  for Siri.
