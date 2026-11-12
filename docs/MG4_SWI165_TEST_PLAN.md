# SWI165 release test plan: calls, music, Bluetooth and Wi-Fi

For checking a build on the car before it's called a release. Written for mg4.84.

## Before you start

| Item | Setting |
|---|---|
| Build | The version shown on DiPlay's About page matches the one you built |
| CarPlay audio over Wi-Fi | On |
| Calls over CarPlay only | On (turn off only for section E) |
| Microphone (test) | Automatic, unless the step says otherwise |
| iPhone | Bluetooth on, paired to the car, Low Power Mode off, on the mount |
| Car | Fully off for a few minutes first, so each run starts from a cold start |

Note the time at each step, so it can be matched to the log. Save one diagnostic report at the
end, wait a few seconds, then copy it off.

## A. Start-up

| # | Step | Pass | Log lines to check |
|---|---|---|---|
| A1 | Start the car, open DiPlay, wait for CarPlay | Picture within about 10 s | `AirPlay session active`, `first frame rendered` |
| A2 | Check what's connected over Bluetooth on the iPhone | Car shows as CarPlay only, not as a Bluetooth audio or phone device | `A2DP sink release ... result=true`, `Hands-free release ... result=true` |
| A3 | Wait 60 s | Still nothing on Bluetooth audio or phone | Any `hands-free reconnected ... releasing again` lines (fine if present: the watcher worked) |

## B. Music

| # | Step | Pass | Log lines |
|---|---|---|---|
| B1 | Play music for 2 minutes | No stutter, no loud clipping | `audio stats ... underruns=0`, `Audio loss ... resendRequests` (resends being sent is good) |
| B2 | Wheel next, previous | Track changes once per press | `Wheel key: code=5/4 ... -> CarPlay` |
| B3 | Wheel volume up and down, then mute | Volume changes; mute silences | Raw codes 297, 298, 301 |
| B4 | Pause on the iPhone, wait 30 s, play | Resumes within 2 s | New `Audio: playback started` |
| B5 | Car's own radio button, then back to CarPlay | Radio stops; CarPlay music comes back | `car source:` lines |

## C. Calls (CarPlay)

| # | Step | Pass | Log lines |
|---|---|---|---|
| C1 | Outgoing call from CarPlay, 30 s, count out loud | Both sides hear each other; no echo; delay under about a second | `audioType=telephony`, `Microphone telephony: started ... opus=software`, `Microphone telephony stats ... encodeAvgUs` under 10000 |
| C2 | Same call: does the iPhone show the car as CarPlay, not Bluetooth? | CarPlay | No `mode=2` in `Audio environment` |
| C3 | Incoming call while music plays | Rings through the car speakers; music pauses | Look for an `alert` or `telephony` stream and whether it played (see risk R1) |
| C4 | Answer with the steering-wheel phone key | Answers | `Wheel raw key: code=291 ... -> CarPlay call` |
| C5 | End with the phone key | Hangs up | Same |
| C6 | Music after the call | Resumes on its own | `audioType=media` restarts |
| C7 | Call with music paused, then start music mid-call | Call keeps priority; music stays quiet | Focus lines |
| C8 | mg4.93, Wi-Fi, fan on medium, windows up: call someone and talk normally for 30 s, then ask whether you sound clear, echoey or noisy. Repeat with Microphone (test) on each option, noting the time of each call | Calls work on every option; note which one the other person hears best | `Microphone telephony: started source=...` (`refused, MIC` means the car's call processing wasn't used), `Microphone telephony stats ... peakDb= rmsDb= clipped=`: speech around -20 to -6 dB with clipped=0 |

## D. Siri and Maps

| # | Step | Pass | Log lines |
|---|---|---|---|
| D1 | Voice button on the wheel: hold it, then a short press (mg4.93) | Holding opens Siri while still held; letting go does nothing more. A short press opens Siri on release. Note whether the car's own voice assistant also starts | `Wheel raw key: code=287 down=true long=true -> Siri sent=true`; for a short press, `down=false long=false -> Siri sent=true` |
| D2 | "What's the weather in Geelong", three times per Microphone (test) option | Siri hears it correctly | `Microphone speechrecognition: started source=...` |
| D2a | n/a since mg4.96 (USB removed). Was USB CarPlay: record a Voice Memo and dictate a message (upstream #1148) | Both record your voice, not silence | `Microphone speechrecognition: started ... codec=LPCM rate=48000 ... frameMs=10` (or another rate with its frameMs) |
| D2b | mg4.93: hold the voice button, speak a normal sentence at normal volume, once per Microphone (test) option | Siri understands; note which option works best | `Microphone speechrecognition stats ... peakDb= rmsDb= clipped=`: speech should peak around -20 to -6 dB with clipped=0 |
| D2c | mg4.97: Microphone (test) on Automatic. Hold the voice button and say "What's the time?", then "Play Triple J", then call a contact | Siri understands and acts each time | `airplay audio format type=100 audioType=speechrecognition ... rtpClock=24000`, `Microphone speechrecognition: started ... tsStep=480` |
| D2d | mg4.98: **Record microphone** on. Call your own voicemail (or a second phone), leave a 10 second message, then listen to it | Your voice is clear in the message | `Microphone telephony: started ... rtpClock= tsStep=` (expect rtpClock=16000 tsStep=320), `Microphone telephony recording saved file=` |
| D2e | mg4.98: **Record microphone** on, **Siri microphone as PCM** off. Parked, music off. Hold the voice button and dictate "Text Andrew: I will be home at half past five and I am bringing dinner". Note what Siri wrote, then cancel | Siri writes the sentence correctly | `Microphone test: siriFormat=Opus recording=on`, `Microphone speechrecognition stats ... floorDb=`, `recording saved` |
| D2f | mg4.98: turn **Siri microphone as PCM** on, tap **Reconnect now**, and repeat D2e. Check music and Siri's voice still play; if CarPlay has no sound, turn it off again | Siri writes the sentence correctly | `Microphone test: siriFormat=PCM`, `Microphone speechrecognition: started ... codec=LPCM` |
| D2g | mg4.98: with the better format, repeat D2e once per **Microphone (test)** option, then once with music playing | Note which option is best | `Microphone speechrecognition: started source=...` |
| D2h | mg4.98: **Download to iPhone**. Open the zip in Files and play a `mic-...-speechrecognition.wav` and a `mic-...-telephony.wav` | Your voice is clear in both, with no gaps, echo or music | Report line `Microphone test: ... recordings=` lists the files |
| D2i | Echo cancellation on calls: ring a second phone and have the other person say a few sentences while you talk | The other person does not hear themselves | `Microphone telephony: started source=VOICE_COMMUNICATION ... aec=on` (`unavailable` or `failed` means no canceller) |
| D3 | Start an Apple Maps route; wait for a spoken prompt | Prompt is heard; music lowers, then comes back | `audioType=default` or `alert` stream, and whether it played (see risk R1) |
| D4 | Dashboard (Directions on the dashboard on). Drive a route with a few turns | Next turn, distance, street on the cluster. The distance steps (600, 500, 400 ...) with CarPlay's screen and does not count down by odd numbers (537, 512) | `Cluster nav: guidance started`, `Cluster nav: apple=...`, `Cluster nav: distance raw=... phoneText=... units=... shown=...m via phone` |
| D5 | Report location to iPhone (now on by default on MG4). Accept the location prompt once, then drive with Apple Maps open | Maps follows the car; through a tunnel or car park it keeps moving | `wheel speed started`, `first wheel speed`, `gear raw=` lines |
| D5a | mg4.91: same drive as D5 | Maps still follows the car and tunnels | `start-location-information components=[...] intervals={...}`, then `tx=0xfffb` |
| D6 | Select P, R, N and D in turn while parked | Done 2026-11-03: 1 P, 2 R, 3 N, 4 D. mg4.93 sends R and N as such | `MG4 gear raw=2 sent=REVERSE` etc. |
| D7 | Plug in to charge with CarPlay running, then unplug | Apple Maps shows charging while charging, not once it stops | `chargeStatus=` and `charging=` on the MG4 battery line |
| D8 | Switch the headlights on and off (or drive into dusk with auto headlights) | CarPlay turns dark with the headlights and light again without them | `Day/night probe: SYSTEM.screen_day_night_mode=1`, `Dark mode on (car)`, `AirPlay dark mode=dark eventChannelReady=true` |
| D10 | Settings > Car > MG4 vehicle > **Car data** with CarPlay running (no vendor library) | Firmware SWI165; hub connected; properties ready; charge, range, gear and speed match the car and cluster; each value shows where it came from | `MG4 car: hub=connected properties=ready`, `MG4 car: first reading soc=…`, `MG4 gear raw=` |
| D9 | Turn on **MG4 vehicle > Video while parked**, park in P, play a video in a supported iPhone app via CarPlay; then select D | Video plays on the car screen in P; the player closes on leaving P | `video in car allowed=true sent=true` in P, then `allowed=false` on leaving P. Player details only go to logcat, so note what you see |

## E. Bluetooth fall-backs and recovery

| # | Step | Pass | Log lines |
|---|---|---|---|
| E1 | Close DiPlay (home, or swipe away) during music | Car's Bluetooth phone reconnects within about 30 s | `hands-free reconnect requested` |
| E2 | Close DiPlay, then make a call | Call goes over Bluetooth hands-free | n/a |
| E3 | Turn the car off and on again, open DiPlay | CarPlay returns; A1 to A3 pass again | |
| E4 | Calls over CarPlay only off, make a call | Note where it goes (CarPlay or Bluetooth) | `mode=` and stream lines |
| E5 | CarPlay audio over Wi-Fi off (Bluetooth Music mode), play music | Plays over the car's Bluetooth Music | `car source: ... switched`, `source=5 (bluetooth)` |
| E6 | mg4.96: Settings > Connection > iPhone > **Pair another**, with the iPhone's Settings > Bluetooth open | The iPhone appears within 20 s; tapping it shows a code in MG4CPlay and on the iPhone; **Pair** saves it and CarPlay connects without leaving the app. If it falls back to Android settings, note which step | `Pairing: discovery started=`, `found phones=`, `bond requested=`, `pairing request variant=2 handled=true`, `bond result=bonded` (Settings changes section) |

## F. Wi-Fi stress

| # | Step | Pass | Log lines |
|---|---|---|---|
| F1 | Put the iPhone in a pocket or bag for 2 minutes of music | Short skips at most | `Audio loss`: `ap0RxDrop` and `udpRcvbufErrors` stay `+0` (loss in the air, not in the car) |
| F2 | Turn the iPhone's Wi-Fi off for 10 s, then on | CarPlay reconnects by itself within about 30 s | `transport error`, `Reconnecting`, new `AirPlay session active` |
| F3 | During a call, walk 10 m from the car and back | Call carries on over the iPhone, or recovers | Note what happened |
| F4 | Car's own hotspot or Wi-Fi setting changed | n/a: note only | `wireless hotspot backend` line |
| F5 | mg4.87: switch to **Wi-Fi Direct** (car hotspot off, car Wi-Fi on), band **Automatic**, then connect | CarPlay starts; note the band. If it stops with "Wi-Fi Direct is switched off", note it and go back to Car hotspot | `Wi-Fi P2P legacy state=`, `fiveGhzCapable=`, `Wi-Fi P2P legacy plan`, `setChannels`, `Wi-Fi P2P ready mode=LEGACY band=` |
| F6 | If F5 fails or is on 5 GHz and stutters, set band **2.4 GHz only** and repeat | CarPlay starts on 2.4 GHz | `band=2.4 GHz` |
| F7 | Repeat B (music) and C (calls) on Wi-Fi Direct and compare with the car hotspot | Fewer skips and less call static than the hotspot | `Audio loss` lines and `call loss packets=` at hang-up, for both modes |
| F8 | mg4.86 log: with the car hotspot, video and music froze for 1 to 5 s about every 160 s (Android 9's longest gap between Wi-Fi scans while not joined to a network). Car hotspot on, **car Wi-Fi off**, play music with the map showing for at least 10 minutes | No freezes every 160 s | `Receive: video ... interArrivalMaxMs=` stays under 1000 and `audio stats ... underruns=0`; no `Audio loss` bursts with `udpRcvbufErrors=+` about 160 s apart |
| F9 | Repeat F8 with the car Wi-Fi **on** (not joined to a network), for comparison | Note whether the 160 s freezes return | Same lines as F8; note the times of each freeze |
| F10 | mg4.92: 10 minutes or more of music with the map showing, car hotspot, **Wi-Fi high performance** on | Note whether the hiccups line up with scans | `Wi-Fi scan done ... sinceLast=` times against `Audio loss` lines with `udpRcvbufErrors=+` and `interArrivalMaxMs=` over 1000. mg4.91 (2026-11-03): the lock did not stop the ~160 s hiccups |
| F11 | HEVC on, **Colour range (test)** set to **Limited**; compare with H.264 | HEVC no longer washed out | `Video: decoder=...HEVC ... colourRangeHint=limited` and the `output format ... color=` line (H.264 logs `86/1/2`, HEVC `1/1/2` with the default). mg4.92 (2026-11-03): still washed out with Limited |
| F12 | mg4.96: drive 15 minutes with an Apple Maps route and music; note the time of each freeze. Optionally set **Music buffer** to 2000 ms | Each freeze has a `stall=[...]` on a `Receive:` line | Whether `ap0RxPackets` stops while `ap0TxPackets` keeps rising, the `wireless=` signal, `load=` and `gcBlocking=` |

## G. Getting the report

| # | Step | Pass | Log lines |
|---|---|---|---|
| G1 | mg4.88: with wireless CarPlay connected, open MG4CPlay > Settings > Diagnostics, tap **Download to iPhone**, scan the QR code with the iPhone camera and open the link | Safari downloads `MG4CPlay-<time>.txt`; the dialog says it was sent; the file opens in Files and can be AirDropped to the Mac | `Report download served bytes= ms=` |
| G2 | Open the same link again, or wait 5 minutes without scanning | The link no longer loads; after 5 minutes the dialog says the link expired | `Report download expired` |
| G3 | Disconnect CarPlay and open Diagnostics | **Download to iPhone** is greyed out with a short explanation | n/a |

## H. Settings screen

Not yet tried in the car. It applies from the first build with the two-pane Settings screen.

| # | Step | Pass | Log lines |
|---|---|---|---|
| H1 | Install over the last build, open MG4CPlay > Settings | Connection type, iPhone, hotspot details, display, sound and location choices all keep their values | n/a |
| H2 | From Home, open each category (Connection, Display, Sound, Car, General, Help, About) | Each is reachable in two taps; nothing is clipped at 1920x720 and the list scrolls | n/a |
| H3 | Connected, change **Frame rate** (a Reconnect row) | A **Reconnect** tag shows on the row and the bar appears. **Reconnect now** reconnects, the bar goes, and the new rate is in use | report lines like `FRAME_RATE = 30` and `Reconnect now after FRAME_RATE` |
| H4 | Connected, change **Dark mode at sunset**, **Car battery on iPhone** or **Video while parked** | Takes effect with no reconnect and no bar | Settings changes list |
| H5 | Read Settings in daylight and at night | Text and rows are readable in both | n/a |
| H6 | Save the report | The **Settings changes (newest last)** section lists the changes from H3 and H4 | `--- Settings changes (newest last) ---` |

## I. Stock app ports

Not yet tried in the car. Each step is MG4 only and, unless noted, behind a setting that is off by default (Settings > Sound). Turn one on at a time, reconnect, and note the time.

| # | Step | Pass | Log lines |
|---|---|---|---|
| I1 | Save a report and open Download to iPhone, then open the zip | The report has a `Stock native libraries` section listing each `libjni_carplay*.so` or `libcarplay*.so` with its size (or says none found). The zip has them under `native-libs/`. Save report writes them as `BEGIN-FILE` blocks | `found /system/lib64/libjni_carplay.so size=` |
| I2 | **Show calls to the car** on. Call out from CarPlay, then take an incoming call | Report shows the account registered and each call added; the car's call screen may show the call. Answer and hang up from the car's call screen if it appears. Note any change in call audio | `Telecom: CarPlayPhone account registered`, `Telecom: call INCOMING`, `addNewIncomingCall sent`, `addNewUnknownCall sent`, `Telecom: service create connection ... found=true`, `Telecom: session stopped` |
| I3 | Same on, end CarPlay (close DiPlay) during a call | The Telecom call ends | `Telecom: session stopped; disconnected 1 call(s)` |
| I4 | **Car phone mode for calls** on. Make a call, talk, hang up | Call is audible both ways; note any change in volume, echo or routing against C1 | `Audio: car phone mode on (call track started) result=`, then `off (call track stopped) result=`. `failed` means the hidden call was refused |
| I5 | **Microphone (test)** set to **Stock**. Call someone, then use Siri | Compare how the other person hears you against C8; Siri still works | `Microphone telephony: started source=MIC`, `Microphone speechrecognition: started source=VOICE_RECOGNITION` |
| I6 | **Tag music as CarPlay** on, reconnect, play music | Music is audible; the cluster may switch to its CarPlay music page. If music is silent, note it and turn the setting off | `Audio: music track car source=50`, `car source:` lines, `Audio: ready audioType=media ... route=usage` |
| I7 | With the car connected to the iPhone over Bluetooth hands-free, make a native hands-free call (Calls over CarPlay only off) while CarPlay music plays | CarPlay audio steps aside during the call and returns after | `Vendor call status: state=4 type=0`, `Audio: native call started; CarPlay focus released`, `... restored` |
| I8 | Make a CarPlay call where the car's call focus is busy (for example with the car's own phone ringing) | Note whether the focus retry fires | `Audio: focus requested channel=PHONE ... granted=0`, then `Audio: phone focus retry after 200ms granted=` |
| I9 | Turn the screen off and on from the car (display off or standby), with CarPlay running | CarPlay picture returns at once; session stays up | `Display report: state=` with `display off` or `on`; `key frame requested`. Note which `state=` value matched the screen |
| I10 | Hold the voice button, ask Siri a long question, press the wheel mute key while Siri speaks, then press it again | First press silences Siri's voice (music unaffected); the next press, or volume up, restores it. With Siri idle, mute does nothing extra | `Wheel raw key: code=301 ... Siri audio muted`, `Siri: audio muted streams=`, `... restored` |
| I11 | Settings > Display > **Screen size reported to iPhone** set to **Stock (244 x 80 mm)**, reconnect. Compare icon and text size against Automatic | CarPlay still fills the screen at the same picture size; icons and text may draw at a different size. Note which looks better, and whether anything is cut off | `Screen size reported to iPhone: stock physical=244x80mm pixels=`, and `automatic physical=` with the setting back on Automatic |
| I12 | **Narrow-band calls (test)** on, reconnect. Call out from CarPlay, talk, hang up. Then ask Siri a question and play music | Call is audible both ways, probably thinner than C1 (8 kHz). Siri and music sound as before. If the call is silent or CarPlay drops, turn it off and note it | `Call audio formats offered: narrow-band=on out=0x4 in=0x4`, and the call stream's rate (8000) in the audio ready line. Off shows `narrow-band=off` |
| I13 | Start MG4CPlay with the car's MFi setting off, connect as usual, then save a report | The report header has a `Car MFi chip:` line, and the log has `Car MFi chip:` probe lines. Nothing else changes | `Car MFi chip: probe result working id=0x200 mfi=2 certBytes= signatureBytes=128` (or the exact failure, for example `EACCES`, `ENOENT`, `ECONNREFUSED`, `SocketTimeoutException`), `selinux context=`, `enforce=`, device id, cert size, signature size, and timings. Send the report back whatever it says |
| I14 | Only if I13 says `working`: turn **Use car's MFi chip (test)** on (Settings > Car > Authentication), reconnect over Wi-Fi as usual. Check CarPlay connects, then try Siri and a call. Turn it off if it fails | CarPlay connects as before. If the chip can't be used the connection still works on the bundled identity | `Car MFi chip: in use for this session`, then `Car MFi chip: signature count=1 digestBytes=20 signatureBytes=128` for the AirPlay auth, and `iap2 mfi tx=0xaa03 signature bytes=` for iAP2. A fallback shows `Car MFi chip: not used, ...; using the bundled identity` or `...; the next connection uses the bundled identity` |

## Known risks to check in code before release

| # | Risk | Why | What to check |
|---|---|---|---|
| R1 | Ringtone and Maps prompts may be silent | They use the navigation channel, which plays as `USAGE_*` audio, not the music stream. Calls were silent on this firmware until they were moved to the music stream | C3 and D3. If silent, route `alert` and `default` to the music stream in Wi-Fi mode, as calls already are |
| R2 | Hands-free given back on every DiPlay close | Leaving CarPlay closes the session (`Activity destroyed`) and reconnects hands-free, then the next session releases it again | E1. If it flaps, keep the session alive in the background instead |
| R3 | No call audio if Wi-Fi drops mid-call | Hands-free is released, so the iPhone has nowhere to send the call except its own earpiece | F3. The iPhone should fall back to its earpiece; confirm |
| R4 | Call and Siri streams can't ask for resends | The iPhone doesn't send a control port for these streams | mg4.84 hides call gaps up to 200 ms (FEC, then concealment); check `call loss` at hang-up |
| R5 | Bluetooth music doesn't come back after CarPlay | A2DP is released and not reconnected on close | E1: check whether Bluetooth music needs a manual reconnect |
| R6 | Siri mishears | Microphone source or processing | D2 results per option |
| R8 | Music tag may silence music | Tag music as CarPlay plays music by usage instead of the music stream, which this firmware once needed (mg4.65) | I6. If silent, leave it off |
| R7 | Calls sound noisy or echoey | DiPlay adds no noise or echo processing of its own; that comes from the car when the call source (`VOICE_COMMUNICATION`) is used. If the car refuses it, or does no processing for it, the call uses the raw microphone | C8 results per option |

## What to send back

- The diagnostic report.
- The times of each step, with anything that failed.
- For D2: what Siri heard each time, for each Microphone (test) option.
- For C8: how the other person heard you on each Microphone (test) option.
