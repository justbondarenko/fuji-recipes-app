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
- [x] 10. Slot read-back in the picker — reversed from a deferral at the owner's
      request <!-- id: 10 -->
- [ ] 11. **Hardware pass on the X100VI** — the one thing no test replaces. FEAT-005 T-18 and
      FEAT-006 T-15 <!-- id: 11 -->

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
**318 unit tests**, 0 failures. Camera-specific:

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
| `SlotStatesTest` | 12 | The four post-read slot states, and which need a second tap |
| `SlotReaderTest` | 7 | Reading C1–C7 off the body without turning a failure into a fact |
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

### One deferral reversed: the slot picker reads the camera

Shipped first as seven bare buttons, with slot read-back deferred to v1.1 and a `ponytail:`
comment naming the upgrade. **Wrong call, corrected at the owner's request.** Picking a slot
blind is exactly the case the deferral was gambling on: the point of the stage is to decide
what to overwrite, and a picker that cannot say what is in a slot cannot support that
decision. `fuji-recipes-book/camera/read-slot.ts` already had it.

What that brought with it, ported from the sibling's `src/utils/slots.ts`:

- **The camera is the only source.** The names are read from the body when the picker is
  reached, and re-read every time. The sibling tried deriving them from its own past writes
  and found the model wrong the moment anything else touched the camera — a recipe set by
  hand, or written from the other client. This app keeps no slot bookkeeping to derive from
  anyway, so there was never a second option.
- **Four states, kept apart.** Named, no-name-set, the-camera-would-not-say, and unknown.
  Merging the middle two shows an empty-looking slot for one nobody knows anything about,
  which is how a recipe gets overwritten.
- **The second tap is now conditional**, which is what `PRD.md` §7.4 said all along ("a slot
  with **known contents**"). A named slot asks and names what it is replacing; a slot the
  camera would not describe asks with a different warning; a slot the camera answered for with
  no name goes straight through. Confirming all seven slots of an untouched camera would train
  someone to tap past the dialog that exists to protect them.

### The hardware pass — connection confirmed

**The camera connects on the owner's phone.** That closes the FEAT-005 gate, and it is the
half no fake camera could prove: the ranked-interface claim finds and takes the PTP interface
on a real body, the endpoint addresses discovered off it are right, the PTP session opens, and
`GetDeviceInfo` comes back readable. Everything above that layer was already tested on the
JVM; this was the layer that could only be settled with a cable.

### Still open — the write

Not yet run. In this order:

1. Open a recipe → **Write to camera**. The picker should list C1–C7 with the names the body
   reports. This is the first hardware exercise of `SlotReader`.
2. Write a known recipe to **C7** — least likely to be in use.
3. Read C7 on the camera and compare every field against the recipe screen.

What to watch, because these are the parts a fake camera cannot settle:

- **Do slot names come back?** If the body refuses `GetDevicePropValue` on `0xD18D`, every
  slot reads "The camera did not answer" — correct behaviour, but it means the picker cannot
  help and the confirmation falls back to its unknown-contents wording.
- **Does the read-back verification return values?** Some bodies refuse reads for this whole
  property block. Expected, and reported as *unverified* rather than as a failure — but if
  every step comes back unverified, the write cannot be confirmed from the phone and step 3
  becomes the only proof.
- **The settle after the slot switch.** If names or values look like they belong to the
  *previous* slot, `SLOT_SETTLE_MS` is too short for this body.
