# Settings redesign plan

Status: built (unit tested, previewed at 1920x720 in a test harness). Not yet tried in the car.

Goal: make Settings quick to scan and change on the 1920x720 panel, laid out the way
iOS and CarPlay Settings are, so it feels familiar to an iPhone user.

## What's wrong now

Settings is one long scroll of 10 to 12 cards in `DiPlayActivity.settings()`. At 720 px
tall only about two cards fit on screen.

1. **Hard to find things.** No overview. Diagnostics comes second, ahead of display and
   sound, which people change far more often. Language sits last, after About.
2. **Grouping is by how the code is built, not by what the driver wants.** Wi-Fi high
   performance is under Display. Location has its own card. ABRP is mixed in with the
   MG4 car settings.
3. **Choices look like buttons.** A choice shows as a full-width button reading
   "Resolution · Native", then opens a dialog. iOS shows a row with the name on the left,
   the value on the right and a chevron.
4. **Too much text.** Every switch has a description under it, so the list is mostly grey
   text. iOS keeps a row to one line and puts the explanation in a footer under the group.
5. **Reconnect is unclear.** Choice dialogs say **Apply and reconnect**. Some switches
   (sunset mode, battery, video while parked) reconnect without saying so. Others need a
   reconnect and don't say so. The subtitle at the top tries to explain it all at once.
6. **Repeats.** **Choose iPhone** appears on Home, in Automatic connection and in
   Connection setup step 2. App permissions appears twice.
7. **Header.** Text **Back** and **Car home** buttons, no page title, and a
   "fatihdonmezdev" line under the app name on every page.
8. **Specialist settings are mixed with everyday ones.** Overlay renderer, colour range,
   plain display area, microphone source and navigation channel sit beside right-hand
   drive and full screen.
9. **Inconsistent icons.** Sound and Language have none. The MG4 card reuses the
   navigation icon.

## Proposed layout

A two-pane layout, like iPad Settings, which suits a wide, short screen:

- **Left sidebar (about 30 % width):** a status row at the top, then the categories. Each
  has an icon in a coloured rounded square, a name and a chevron. The selected one is
  highlighted. Its scroll position is kept.
- **Right pane:** a large title for the category, then grouped inset lists (rounded
  groups, one-line rows, hairline separators, a small heading above each group and a
  footer below it).
- **Navigation bar:** a back chevron with the previous page's name on the left (e.g.
  "‹ Home"), the title in the middle. Drill-down pages (hotspot details, ABRP, report)
  push inside the right pane, so the sidebar stays put.

On a narrow screen (under 850 dp) it falls back to one list that drills down, as on an
iPhone.

### Categories and where every setting goes

| Category | Group | Rows |
|---|---|---|
| *Status row* | | Connected iPhone and link (e.g. "Andrew's iPhone · Car hotspot"), or "Not connected". Tap opens Connection. |
| **Connection** | Connection | Connection type (Car hotspot, Wi-Fi Direct, USB) › · Car hotspot details › (only for Car hotspot) · iPhone › |
| | Automatic | Connect when MG4CPlay opens ◯ · Open after the car starts ◯ |
| | Wi-Fi | High-performance Wi-Fi ◯ · Wi-Fi Direct band › (Android 9 only) · Open car Wi-Fi settings |
| | | Footer: what each connection type needs (from the current setup steps). |
| **Display** | CarPlay screen | Size › · Right-hand drive ◯ · Full screen ◯ |
| | Picture | Resolution [Native · 80 % · 60 %] · Frame rate [30 · 60] · Efficient video (HEVC) ◯ |
| | | Advanced › (Overlay renderer, Colour range, Plain display area) |
| **Sound** | Audio | CarPlay audio over Wi-Fi ◯ · Calls over CarPlay only ◯ · Music buffer [500 · 750 · 1000 ms] |
| | Microphone | Microphone › (Automatic, Voice, Plain) |
| | | Advanced › (Navigation channel and test tone) |
| **Car** | Driving | Cluster directions ◯ (SWI165) · Send location to iPhone ◯ · Video while parked ◯ |
| | Appearance | Dark mode at sunset ◯ |
| | Battery and charging | Car battery on iPhone ◯ · Charging connectors › · Low charge warning › |
| **Apps** | A Better Route Planner | Live telemetry ◯ · Account › |
| **General** | | Language › · App permissions › · Bluetooth › · Software update › (non-MG4) |
| **Help** | Diagnostics | Save report · Download to iPhone · Choose where to save › |
| | Help | Wireless connection help › |
| **About** | | Version, credits (moves the "fatihdonmezdev" line here), open source |

Legend: › opens a list, ◯ is a switch, [a · b] is a segmented control.

## Interaction rules

- **Rows, not buttons.** Name on the left, current value in grey on the right, then a
  chevron. Minimum row height 64 dp (bigger than iOS's 44 pt, for gloves and bumps).
- **Short lists open a picker page, not a dialog.** The options show as rows with a tick on
  the chosen one, like iOS. Tapping saves and goes back. Two or three short options use a
  segmented control inline.
- **One reconnect rule.** Any change that only takes effect on reconnect shows a small
  "Reconnect" tag on the row while CarPlay is connected, plus a bar at the bottom of the
  right pane: "Some changes apply after CarPlay reconnects. [Reconnect now]". Nothing
  reconnects on its own except where it does today and is needed (location permission).
  This replaces the subtitle at the top and the **Apply and reconnect** dialog button.
- **Footers, not descriptions.** Each row is one line. Explanations go in a footer under
  its group. Keep the warnings (hotspot off, ADB access needed) as an orange footer next
  to the row they belong to.
- **Advanced pages** for settings most people never touch. They work the same as now and
  keep their saved values.
- **Destructive actions** (reset Wi-Fi Direct group) in red text, with a confirm sheet.
- **Text size and contrast:** row text 20 sp, value and footer 16 sp; check grey-on-dark
  contrast meets WCAG AA (current `MUTED` on `SURFACE` is about 7:1, fine). Every row
  gets a content description that includes its value.
- **Colours:** keep the dark palette, use the system blue accent for ticks and switches,
  green for "on" switches as iOS does, and one coloured icon tile per category.

## How to build it

Keep code-built views (no XML layouts), as now. Move the new pieces out of
`DiPlayActivity.kt` (1,700 lines) so upstream ports touch less of it.

1. **Components** in a new `common/.../settings/SettingsUi.kt`: `groupedList`,
   `switchRow`, `valueRow`, `segmentedRow`, `actionRow`, `groupHeader`, `groupFooter`,
   `pickerPage`, `navBar`, `sidebarItem`. Plus colour and size tokens in one place.
2. **Model** in `settings/SettingsModel.kt`: a plain Kotlin list of categories, groups and
   rows, each row holding its title, how to read its value, how to save it and whether it
   needs a reconnect. Which rows appear (SWI165 or SWI69, Android version) is decided
   here, so it can be unit-tested in `common` without a device.
3. **Shell**: two-pane layout, sidebar, back stack inside the right pane, saved page on
   rotate and restart (extends the current `page` field).
4. **Move each category over**, one commit each, calling the same `AirPlayPersistence`,
   `VehicleSettings` and `AbrpSettings` methods. No saved setting changes key or default.
5. **Reconnect bar** and row tags, replacing the per-dialog reconnect.
6. **Connection page** replaces the separate Connection setup page. Its numbered steps
   become the footer and the hotspot details page.
7. **Clean-up:** remove the old `section`, `choice` and `toggle` helpers once nothing
   uses them; drop the duplicate **Choose iPhone** and permissions buttons.

Out of scope for now: the Home page, search (eight categories don't need it), and new
settings.

## Testing

- Unit tests for the model: the SWI165 and SWI69 lists each show the right rows, and each row reads and saves the same key as before.
- Build and lint with the usual CI checks.
- Car test steps to add to `docs/MG4_SWI165_TEST_PLAN.md`:
  - Every setting keeps its value after installing over the last build.
  - Every category is reachable in two taps from Home.
  - Change a reconnect setting while connected: the bar appears, **Reconnect now** works,
    and the change takes effect.
  - Readable in daylight and at night, and usable at arm's length with the car moving
    slowly (passenger).
- Log each setting changed (name and new value). Built as the diagnostic report's
  "Settings changes" list (last 40, kept in SharedPreferences), not page opens, so it
  survives restarts.

## Docs to update when it lands

README settings table (new names and places), `docs/MG4_SWI165.md` status table,
`CHANGELOG.md`, and any test plan step that names an old button.

## Decisions

- 2026-11-03: MG4 only. The BYD and other-car settings inherited from upstream are left
  as they are and not redesigned; they only show on non-MG4 head units.
- 2026-11-03: **Send location to iPhone** moves to Car › Driving.
- 2026-11-03: two-pane layout (sidebar and detail pane), with the single drill-down list
  only as the narrow-screen fallback.
