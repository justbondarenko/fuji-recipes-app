# FEAT-005: Camera connection

**Tracker issue:** —
**Source PRD:** `PRD.md` §7.2 (indicator), §8 (camera integration), §13 (error handling)
**Parity target:** `fuji-recipes-book/camera/usb/{ptp,transport,session}.ts`,
`camera/models.ts`, `src/utils/camera-chip.ts`, `src/stores/camera.ts`
**Status:** Ready

---

## Problem

The app manages recipes it cannot deliver. Everything up to FEAT-004 ships a library on the
phone; the reason the phone is in the bag with the camera at all is that it should be able
to put a recipe into a custom slot, and today the cable does nothing.

This feature is the half that has to work before any of that is worth attempting: find the
camera on the USB bus, claim it, open a PTP session, and find out what body it is. It ends
with the app knowing — and showing — that it is talking to an X100VI, and knowing whether
that body can take a write at all.

It is also the stage that closes the roadmap's **GATE**. `roadmap.md` rated it high risk on
the assumption that the protocol was unverified. That assumption is stale: the sibling repo
shipped a working WebUSB implementation with 2,600 lines of tests, and the property codes
were resolved in its stage 32 from `eggricesoy/filmkit` @ `9e3bbcf`, cross-checked against
an X100VI in March 2026. This feature transcribes that work rather than repeating it.

## User stories

- As the photographer, I can plug the camera into the phone and have the app open already connected, so that connecting is not a task I perform.
- As the photographer, I can see which body the app thinks it is talking to, so that a misidentification is visible before it matters.
- As the photographer, I am told plainly when a body cannot take a write, so that I do not discover it halfway through one.
- As the photographer, I can see why a connection failed in words that name a remedy, so that a busy interface and a wrong USB mode are not the same message.
- As the photographer, I can unplug the camera and have the app notice, so that the indicator never claims a connection that is gone.

## Scope

### Transport

1. `android.hardware.usb` host API. Fujifilm's vendor id is `0x04CB` (decimal `1227`).
2. The PTP interface is **discovered, not assumed**. Candidate interfaces are those with a
   bulk in/out pair, ranked still-image class (`0x06` / `0x01` / `0x01`) first, and each is
   claimed in turn until one succeeds. Endpoint addresses are read off the claimed
   interface. *Both rules are the sibling repo's fixes for real failures — claiming
   interface 0 blindly fails on a body whose OS photo service already holds it, and
   hard-coded endpoint numbers break on the second camera anyone tries.*
3. `claimInterface(force = true)`. If it still fails, the message names the likely cause.
4. Command / data / response phases with transaction ids, per ISO 15740. Reads are chunked
   until the container's declared length is satisfied. 5 s timeout per transfer.
5. Container framing, value codecs and PTP strings are **pure Kotlin with no `android.*`
   import** (`coding-standards.md` P4), so the whole of it is testable with byte arrays.

### Session and identity

6. `OpenSession` (session id 1), `GetDeviceInfo`, `CloseSession`.
7. The device-info dataset is parsed for the model string.
8. A model table maps that string to a `SensorGeneration`. Nothing is inferred from a name:
   a body is either in the table or it is **unrecognised**, and an unrecognised body refuses
   writes and says so in its own words. It must never be labelled "Bayer CMOS", which would
   be an assertion about a sensor nobody has looked at.
9. `SensorGeneration` is the enum that already exists in `data/fields/FilmSimulations.kt`.
   No second generation type.

### State and lifecycle

10. `CameraController` is a singleton on `AppContainer`, above the nav graph, exposing
    `StateFlow<CameraState>`: `NoUsbHost`, `Disconnected`, `Connecting`,
    `Connected(model, generation, writable)`, `Writing(…)`, `Error(message, ptpCode)`
    (`PRD.md` §8.4). `Writing` is held for FEAT-006 and is not entered in this feature.
11. `packageManager.hasSystemFeature(FEATURE_USB_HOST)` false → permanent `NoUsbHost`. It is
    not an error and offers no retry.
12. USB permission via `PendingIntent`. Permission granted by the attach intent is not
    re-requested.
13. A receiver for `ACTION_USB_DEVICE_DETACHED` returns the state to `Disconnected` and
    releases the interface.

### Attach intent

14. `<uses-feature android:name="android.hardware.usb.host" android:required="true" />`.
15. `USB_DEVICE_ATTACHED` intent-filter and `res/xml/device_filter.xml` with
    `vendor-id="1227"` — decimal, which is the single most common way this silently fails.
16. Launching by attach grants permission implicitly for that connection, so the plugged-in
    path shows no permission dialog. **This is the feature that justifies going native**
    (`PRD.md` §8.2).

### Indicator

17. A camera chip in the app shell, tappable, in the six states of `PRD.md` §7.2.
18. **Colour is never the only signal.** Every state carries its own icon and its own words,
    because a green pill and a grey pill are the same pill outdoors and to a colour-blind
    reader.
19. Connected shows the model — "X100VI" says more than "Connected", and it is also the
    fastest way to notice a misidentification.
20. Tapping opens a camera sheet: model, generation, whether writes are available and why
    not when they are not, and a disconnect action.

## Out of scope

| Deferred | To | Why |
|---|---|---|
| Writing a recipe to a slot | FEAT-006 | This feature ends at "connected and identified". A protocol slog here must not also block the write UI's design. |
| Reading slots back | v1.1 | `PRD.md` §14.3. The sibling's `camera/read-slot.ts` is written and waiting; the slot picker labels contents "Unknown" until then. |
| Camera help screen | FEAT-007 | Prose, and it is worth writing once the real failure messages are known rather than guessed. |
| Anything reported to the server | won't do | `roadmap.md` §3 — this client does not keep slot bookkeeping. Camera work never crosses the network. |

## Constraints this feature is shaped by

| # | Constraint | Consequence |
|---|---|---|
| C1 | The phone has one USB-C port and the camera occupies it | No cabled debugging during any test that matters. Wireless ADB is paired before the cable goes in, and a protocol trace is written to a file readable after unplugging. |
| C3 | The protocol is reverse-engineered | Every pure layer is testable with no hardware, and a fake camera that speaks PTP back covers the session logic. |
| P4 | The protocol layer is pure and isolated | Nothing under `camera/ptp/` or `camera/plan/` may import `android.*`. |
| P5 | Errors name the thing that failed | A busy interface, a wrong USB mode and a timeout have three different remedies and must not render as one message. |

## Error surfaces

| Failure | Surface | Message names |
|---|---|---|
| No USB host support | Permanent chip state | That this device cannot do it at all; no retry offered |
| Permission denied | Chip → `Error`, sheet offers retry | That permission was refused |
| Every interface refused the claim | Sheet, retry offered | That something else holds the camera, and what usually does |
| No bulk endpoints found | Sheet | That the camera is probably in the wrong USB mode — set it to the RAW-conversion / backup mode |
| PTP response other than OK | Sheet | The operation and the response name, e.g. `DeviceBusy`, never the bare hex |
| Timeout | Sheet, retry offered | That the camera stopped answering |
| Unrecognised body | Connected, writes unavailable | That the app does not know this body, and that storing recipes still works normally |
