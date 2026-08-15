# Task: FEAT-005 + FEAT-006 — camera connection and write to slot

**Branch:** `feat/FEAT-005-camera-connection` (both features, see `specs/roadmap.md` §7)
**Specs:** `specs/features/FEAT-005-camera-connection/`, `specs/features/FEAT-006-write-to-slot/`
**Reference:** `fuji-recipes-book` @ `0c17106` — a shipped, unit-tested WebUSB PTP stack.
This is a transcription, not protocol archaeology.

## Plan Items

- [x] 1. Spec folders for FEAT-005 and FEAT-006; roadmap §7 records the re-rated risk and
      the shared branch <!-- id: 1 -->
- [x] 2. `camera/ptp/PtpFraming.kt` — containers, value codecs, PTP strings. Pure, no
      `android.*`. Tests mirroring `tests/unit/ptp.spec.ts` <!-- id: 2 -->
- [x] 3. `camera/ptp/DeviceInfo.kt` and `camera/CameraModels.kt` — device-info parsing and
      model → generation, reusing the existing `SensorGeneration` enum <!-- id: 3 -->
- [x] 4. `camera/usb/` — `PtpTransport` interface, `UsbPtpTransport` over
      `UsbDeviceConnection`, `PtpSession`, and a `FakeCamera` that speaks PTP back <!-- id: 4 -->
- [x] 5. `camera/CameraController.kt`, `AppContainer` field, manifest `uses-feature` +
      attach intent, `res/xml/device_filter.xml` (vendor-id **1227**, decimal) <!-- id: 5 -->
- [x] 6. `ui/camera/` — the chip in six states and the camera sheet. **FEAT-005 gate** <!-- id: 6 -->
- [x] 7. `camera/plan/` — `CameraEncoding.kt`, `WritePlan.kt`, `StepPayload.kt`. Pure. Tests
      covering WR-01…WR-10 individually <!-- id: 7 -->
- [x] 8. `camera/usb/WriteExecutor.kt` and `ui/camera/WriteSheet.kt` — execution, the six
      stages, haptics, entry point on the recipe view <!-- id: 8 -->
- [x] 9. `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`, emulator pass,
      dark scheme, reduced motion, P5 error-copy audit <!-- id: 9 -->
- [ ] 10. **Hardware pass on the X100VI** — the one thing no test replaces. FEAT-005 T-18 and
      FEAT-006 T-15 <!-- id: 10 -->

## Notes carried through implementation

- **P4 is structural:** `CameraPurityTest` fails the build if anything under `camera/ptp/` or
  `camera/plan/` imports `android.*`.
- **P3 headers:** every transcribed file names its source path and `0c17106`.
- **Naming:** `ui/connection/` is the *server* connection. Camera UI is `ui/camera/`.
- **Expressive components are unreachable** at material3 1.4.0 (`tech-stack.md` §6), so the
  chip's loader, the slot picker and the progress bar are built from the shell's existing
  vocabulary, each with a comment naming the component it stands in for.

## Review & Verification

### What shipped

The whole chain, connection through write: PTP framing → transport → session → model identity
→ connection state and indicator → encoding tables → write plan → executor → write sheet.

### Where the risk actually went

`roadmap.md` rated both stages **high** on the premise that the protocol was unverified. That
premise was stale — the sibling repo shipped a working, tested WebUSB implementation and
resolved every property code against an X100VI in its stage 32. Re-rated to **medium** in
`roadmap.md` §7, which also names what is still genuinely unknown.

### Gate

`./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` — green.
**299 unit tests**, 0 failures. Camera-specific:

| Suite | Tests | Covers |
|---|---|---|
| `PtpFramingTest` | 28 | Containers, codecs, PTP strings |
| `DeviceInfoTest` | 10 | The two dataset parsers |
| `CameraModelsTest` | 10 | Model → generation, and the refusals |
| `PtpSessionTest` | 17 | Session lifetime, reassembly, stale replies |
| `CameraEncodingTest` | 28 | The tables, against `field-definitions.md` §4 |
| `WritePlanTest` | 33 | WR-01…WR-10, one case per rule |
| `WriteExecutorTest` | 15 | Refusals, unplug, cancel, read-back |
| `CameraChipLookTest` | 9 | Six states, each with its own icon and words |
| `WriteSheetStateTest` | 7 | Which stage the sheet opens on |
| `CameraPurityTest` | 1 | P4, structurally |

### Verified on the Pixel 10 Pro XL emulator

- The chip renders top-right on every chrome screen, in light and dark, and reads **No USB** —
  the correct state for a device with no USB host support.
- Its sheet explains why and offers **no retry**, which is right: a phone that cannot host
  will not start being able to, and a retry button there is a lie someone acts on.
- The recipe screen's **Write to camera** action is present and disabled with no camera
  attached, rather than hidden.
- Content clears the chip's row rather than scrolling under it.

The write stages cannot be reached on an emulator — there is no camera on the bus. They are
covered by `@Preview` for all six, by `WriteSheetStateTest`, and by `WriteExecutorTest`
against a fake camera that speaks PTP back.

### Audits

- **P5, error copy.** Every camera failure is a distinct sentence naming a distinct remedy:
  permission refused, interface busy (with the likely culprit), wrong USB mode, timeout,
  unplugged mid-write, a PTP refusal quoted by name *and* hex code. None of them renders as
  "connection failed".
- **Reduced motion.** The chip's spinner honours `LocalReducedMotion` — the first continuous
  animation in the app, and the first place that signal earns its keep. Nothing is lost when
  it is off, because the state still has its own icon and words.
- **Dark scheme.** Chip, sheet and every write stage take their colours from the scheme; the
  previews cover both.

### Still open — the hardware pass

The one thing no test replaces. Run in this order:

1. Pair **wireless ADB before the cable goes in** — the camera occupies the only USB-C port
   (`architecture.md` C1), so this cannot be done afterwards.
2. Set the camera's USB mode to **RAW conversion / backup**.
3. Plug in cold with the app closed. It should launch itself and the chip should name the
   body, with no permission dialog.
4. Write a known recipe to **C7** — least likely to be in use.
5. Read C7 on the camera and compare every field against the recipe screen.

Two things to watch, because they are the parts a fake camera cannot prove: whether the
ranked-interface claim succeeds on the first candidate, and whether the read-back verification
returns values or is refused for this property block. The second is expected on some bodies
and is reported as *unverified* rather than as a failure.
