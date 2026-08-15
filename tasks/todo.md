# Task: FEAT-005 → FEAT-007 — the camera link, both directions

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
- [x] 11. **Hardware pass on an X-T50** — connects, is identified, launches on attach, and
      writes to a slot. FEAT-005 T-18 and FEAT-006 T-15 <!-- id: 11 -->
- [x] 12. **FEAT-007 — import from camera.** More → Import reads C1–C7, decodes each slot,
      reviews against the library with duplicate detection, imports the chosen ones <!-- id: 12 -->
- [ ] 13. Read the slots on the X-T50 and compare against the camera's own menu — the only
      check that proves the properties mean what the tables say <!-- id: 13 -->

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
**385 unit tests**, 0 failures. Camera-specific:

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
| `SlotRecipeReaderTest` | 14 | Reading a whole slot back as a recipe (FEAT-007) |
| `RecipeConfigTest` | 17 | Whether two recipes are the same camera configuration |
| `ImportReviewTest` | 17 | New / already-held / name-clash, and what gets sent |
| `ApiClientImportTest` | 9 | `POST /api/import`, against MockWebServer |
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

### The write — ran on hardware

**A recipe was written to C6 of an X-T50 and the write completed.** The chain ran end to end
against a real body: build the plan, pack each step, select the slot, write the name, write
seventeen properties, read each one back. Nothing in it was refused hard enough to abandon the
write, and no partial-slot warning was raised.

Every layer of this project now has hardware behind it except one claim, below.

### FEAT-007 — the link now runs both ways

**More → Import** reads C1–C7 off the body, decodes each slot, reviews it against the library
and imports what you choose. Ported from `read-slot.ts`, `recipe-config-equal.ts` and
`import-review.ts`.

Three decisions worth keeping:

- **The phone sends no recipe ids.** P2 forbids inventing one the server would assign, and
  `shared/schemas/recipe.ts` makes `id` optional while `import.post.ts:143` is
  `input.id ?? randomUUID()`. The web client generates a UUID per slot only to satisfy its own
  client-side schema check. Consequence: an imported slot cannot collide by id, so the
  contract's whole `resolutions` mechanism is unreachable and five statuses collapse to three.
- **Duplicates are matched on configuration, not on name.** Settings are normalised — defaults
  filled, inapplicable fields dropped — and compared over the fields that apply to that
  simulation and body. Without normalisation every import looks new, because a camera reports
  every property it holds and a hand-typed recipe omits whatever was left at its default.
- **There is no "replace".** Identical settings are what made a row a duplicate, so replacing
  would change nothing. Skip or import-anyway are the two real choices, and a duplicate is
  listed and selectable rather than hidden.

One bug the tests caught rather than the field: `getOrPut` re-invokes its lambda on a `null`
value, so a property the body *refused* was being asked for again by every field sharing its
code. The refusal has to be cached too.

### The one claim still unproven — that the properties *mean* what the tables say

Property **existence** was confirmed on this body by the sibling repo's dump (all nineteen,
2026-08-09). Property **meaning** is a separate claim: nothing yet proves `0xD19D` is highlight
tone rather than something else that also accepts −20, only that it exists, accepts the value
and gives it back.

A successful write does not settle it. A write that put highlight tone into the sharpness
property would look identical from the phone: sent, accepted, read back unchanged.

**What settles it:** open C6 on the camera and compare each setting against the recipe screen.
That is the only check that crosses from "the camera stored what I sent" to "the camera stored
what I meant", and it needs doing once — after that, every encoder is pinned by a test and a
change to one breaks it.

**FEAT-007 makes this much easier.** More → Import now reads all seven slots and decodes them
on screen, so the comparison is the app's own list against the camera's menu rather than
against a recipe you have to remember writing. Two readings settle it at once:

- **C6**, which this app wrote. Every value should come back as it was sent — that closes the
  round trip through the encoders.
- **Any slot the app did not write.** That one is the real test of *meaning*: nothing about it
  came from this build's assumptions, so if its decoded recipe matches what the camera's menu
  shows, the property→field mapping is right rather than merely self-consistent.

Two smaller things worth reading off the same write:

- **`0xD1A1` (High ISO NR).** The earlier dump found it reading `0x8000`, which is either NR −4
  per WR-08's table or the reference implementation's sentinel value; a dump cannot tell them
  apart. If the recipe written to C6 had any NR other than −4 and the step verified, the table
  is right and it was not a sentinel.
- **Whether every step verified.** The result stage reports `n of m properties written and read
  back`. If `n == m`, this body returns values for the whole property block — which also means
  the read-back check is doing real work rather than passing vacuously.
