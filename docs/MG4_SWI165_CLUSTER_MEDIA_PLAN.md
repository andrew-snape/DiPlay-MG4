# Plan: CarPlay song details on the SWI165 cluster

Background: [MG4_SWI165_CLUSTER_MEDIA_NOTES.md](MG4_SWI165_CLUSTER_MEDIA_NOTES.md). The
cluster shows a CarPlay song only when the car's audio source is **50** and the stock
`com.allgo.remoteui.mediabrowserservice` media session carries the song. DiPlay does
neither yet. Nothing here has been tried in the car.

## 0. Read the stock media browser's code (blocker)

The feed into the stock browser is probably its exported
`...RemoteUIMediaBrowserServiceInterface` binder, but its descriptor and transaction codes
are only in the car's `oat/arm64/*.vdex`. mg4.85 already puts those files in the
diagnostic report (`SystemAppCodeExport`, "Stock app code" section). No report with that
section has been read yet.

- Save a diagnostic report from the car (mg4.85 or later) and decode the
  `BEGIN-FILE`/`END-FILE` blocks; check each SHA-256.
- Pull the dex out of the vdex (Android 9 vdex holds the dex as is) and read:
  - the `...Interface` descriptor, its methods and transaction codes, and parcel layouts
    (metadata, play state, position, artwork);
  - how `com.allgo.rui` calls it, to copy the call order;
  - whether the browser's `onGetRoot` / session needs anything first (a connected flag,
    a source check).
- Write the findings into the notes doc, then remove `SystemAppCodeExport` and its
  CHANGELOG "temporary" entry.

**Go / no-go:** if the interface takes title, artist, album, duration and play state, carry
on. If not, stop: the only other route is replacing the stock package, which we don't do.

## 1. Parse the full now-playing update

`CarPlayPlaybackStatus` only reads play status from NowPlayingUpdate (0x5001).

- New `shared/.../media/CarPlayNowPlaying.kt`: keeps title (1), duration (4), album (6),
  artist (12) from the media item group, plus play status and elapsed time from the
  playback group. Updates carry only what changed, so merge into the last state; clear
  on session end.
- Check `StartNowPlayingUpdates` (`Iap2Messages.kt:279`) asks for every attribute
  needed.
- Unit tests with captured frames, like `CarPlayPlaybackStatusTest`.
- Artwork (26) arrives over an iAP2 file transfer, which DiPlay doesn't handle. Leave it
  for a later step; the cluster copes with no bitmap.

## 2. Tag the music track as source 50

- Add `CARPLAY_MEDIA = 50` to `Mg4CarAudioSource` and tag the music track in
  `AndroidMediaSink` the way the call track is tagged (`AndroidMediaSink.kt:1083`). MG4
  only, behind the setting in step 5. Log `Audio: music track car source=50|untagged`.
- Risk: the music track uses the legacy `STREAM_MUSIC` path on some setups
  (`AndroidMediaSink.kt:1031`). Check the bundle survives that, and that the car still
  plays the audio (a source-50 track might be routed or ducked differently from an
  untagged one).
- Car test on its own first: does the cluster switch to its CarPlay music page? Does
  music still play, and do radio and Bluetooth still take over when picked?

## 3. Feed the stock media browser

New `shared/.../vehicle/Mg4ClusterMedia.kt`, built like `Mg4ClusterNavigation`: binds the
stock service with the `...Interface` action, sends raw `Parcel` transactions with the
codes from step 0, runs on its own handler thread, fails closed (no bind or a failed call
is logged and skipped, never retried in a loop).

- On song change: send title, artist, album, duration.
- On play status change: send playing/paused, and position for the progress bar.
- On session end or setting off: send stopped and empty metadata, then unbind.
- Only send changes; throttle position updates (about once a second while playing).
- Hooked into the session where `CarPlayPlaybackStatus` is fed today.

Risks to check in the car:
- The stock session gets media buttons. Wheel previous/next (299/300) may go to the
  AllGo stack instead of DiPlay's handling. Check they still skip tracks.
- A media session on Android 9 once made the car's Bluetooth stack pause the iPhone
  (`CarPlayMediaKeys`). This one is the stock app's session, but test with music
  playing.
- Does the stock CarPlay stack also write to the browser when it starts? Note any
  fighting over the session.

## 4. Artwork (later)

Handle the iAP2 file transfer for attribute 26, scale the image down, and send it if the
interface takes a bitmap. Separate release.

## 5. Setting, logs, docs, release

- Setting **MG4 vehicle > Song on the dashboard (test)**, off by default, in
  `AirPlayPersistence`, gated on `Mg4FirmwareProfile.detect().isMg4`. Turns on both the
  source-50 tag and the feed.
- Session log lines (`Cluster media: bound|title=…|state=…|failed …`) and a diagnostic
  report line with the bind state and last call result.
- New steps in [MG4_SWI165_TEST_PLAN.md](MG4_SWI165_TEST_PLAN.md): cluster music page
  shows, song changes follow, pause/play follows, progress bar moves, wheel skip still
  works, radio/Bluetooth take over, ending CarPlay clears the cluster.
- README "What works" and settings tables, `MG4_SWI165.md` status table, CHANGELOG, all
  marked "not yet tried in the car".
- Release as `0.2.9-mg4.N` per [RELEASING.md](RELEASING.md).

## Order

Step 0 needs a car trip for the report. Steps 1 and 2 can be built meanwhile and shipped
as one test build (step 2's car test doesn't need step 0). Step 3 follows once step 0 is
read. Step 4 is separate.
