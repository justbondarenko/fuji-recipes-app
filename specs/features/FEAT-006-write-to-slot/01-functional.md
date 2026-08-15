# FEAT-006: Write to slot

**Tracker issue:** —
**Source PRD:** `PRD.md` §7.4 (the write flow), §8.3 (ordering and encoding), §13
**Canonical rules:** `fuji-recipes-book/specs/shared/field-definitions.md` §6 (WR-01…WR-10)
and §7 (property codes, resolved in stage 32)
**Parity target:** `fuji-recipes-book/camera/{encoding,write-plan}.ts`, `camera/usb/{payload,write}.ts`
**Status:** Ready

---

## Problem

FEAT-005 ends with the app connected to a camera it has correctly identified, and able to do
nothing with it. This feature is the payload: turn a stored recipe into an ordered list of
PTP property writes, show what the body cannot take before anything is sent, send it, and
report exactly what happened.

The parameter encodings are not guesses. `field-definitions.md` §7 records them as resolved
from `eggricesoy/filmkit` @ `9e3bbcf` — Wireshark captures of Fujifilm's own X RAW Studio,
cross-referenced against seven presets on an X100VI. Three of them differ from the encodings
the RAW-conversion profile uses for the same values, and mixing the two is the likeliest way
to write a recipe that looks right and is wrong.

## User stories

- As the photographer, I can send a recipe to a custom slot and watch it land, so that the app finishes the job it exists for.
- As the photographer, I am shown which parameters this body cannot take **before** the write starts, so that a compromise is a decision I make rather than one I discover.
- As the photographer, I am stopped from writing to a body that has no custom slots, so that nothing is half-written.
- As the photographer, I feel the write succeed without looking at the phone, so that I can keep my eyes on the camera.
- As the photographer, I am told which parameter failed and what the camera said, so that a failure is a fact rather than a mystery.

## Scope

### The plan — pure, and the highest-value test suite in the project

1. `buildWritePlan(recipe, camera, slot)` is a **pure function**: no I/O, no clock, no
   randomness, no `android.*` (`coding-standards.md` P4, WR-09). Same inputs, same ordered
   output. Testable with no hardware, which is the whole point of building it this way.
2. It returns ordered steps, dropped fields with a reason each, notes, and a refusal when
   the write must not happen at all.
3. **WR-01** — a body whose generation has no custom slots (`xtrans-iii`, `cmos`) is refused
   outright. Not partially written. The refusal comes first and unconditionally.
4. Slot must be an integer C1–C7.
5. The first two steps are always the slot select (`0xD18C`) and the preset name
   (`0xD18D`), both fatal: if either fails, nothing after it means anything.
6. **Write order** is `WRITE_ORDER` in the encoding table, and it is the observed order from
   the captures, not the plan-time guess `PRD.md` §8.3 originally carried. Two properties of
   it are load-bearing: **simulation first**, so a mode-dependent rejection surfaces
   immediately rather than after twelve successful writes, and **`colorTemperature`
   immediately after `whiteBalance`** (WR-02). A test pins the adjacency.
7. Skip rules, each with its own test: **WR-03** monochrome simulations reject `color`;
   **WR-04** `grainSize` when grain is off; **WR-05** `monochromaticColorWc` / `Mg` unless
   X-Trans V *and* monochrome, and a written **zero is rejected by the camera**, so zero
   means omit rather than write 0; **WR-06** advisory fields (`dRangePriority`,
   `exposureCompensation`, `isoMin`, `isoMax`) are never written **and never reported as
   dropped** — they are recommendations, and a warning that appears on every write for
   something that was never going to be written is a warning that stops being read;
   **WR-07** any field whose applicability fails for the camera's generation, reported as
   dropped; **WR-10** `filmSimulation` when the recipe's simulation postdates the body,
   reported as dropped and prominently — the slot ends up with this recipe's tones over
   whatever simulation it already had, and that is the user's call.
8. **WR-08** — `highIsoNR` is non-linear. Lookup table, never arithmetic.
9. Encodings that differ from the RAW-conversion profile, and must not be crossed with it:
   effects are **1-indexed** (Off `1`, Weak `2`, Strong `3`); dynamic range is a **raw
   percentage** (100, 200, 400); grain is **one flat enum** covering strength and size
   together, so §4's two fields encode as one value (`1` off, `2` weak/small, `3`
   strong/small, `4` weak/large, `5` strong/large). Tone parameters are ×10.
10. `stepPayload` turns a step into bytes — `u16`, `i16`, or a PTP string. It **refuses**
    rather than guessing when a step's kind and value disagree; a zero written into a
    property because a guard was missing is the exact failure this project keeps refusing.

### Execution

11. `executeWritePlan` runs the steps in order over the PTP session, reporting progress per
    step. A settle delay follows the slot select.
12. Fatal steps abort the write. A non-fatal refusal is recorded and the write continues, so
    one unsupported parameter does not cost the other sixteen.
13. Unplug mid-write aborts cleanly and the result warns that the slot may be partly written.
14. A partial wake lock is held for the duration of the write and released after. Writes take
    seconds; a doze mid-transfer is a bad way to leave a slot.
15. No foreground service — the operation is short and user-initiated.
16. **A slot is not reset before it is written.** Seven per-slot settings no recipe models —
    image size, image quality, smooth skin, long-exposure NR, colour space and two
    unidentified constants — keep whatever the slot already had. Stated in the UI where the
    user can see it, because it is a behaviour rather than a gap.

### The write sheet

17. One `ModalBottomSheet` expanding through the stages of `PRD.md` §7.4, not a stack of
    dialogs: connection → compatibility → slot picker → confirm → progress → result.
18. **Stage 1 connection** is skipped entirely when a camera is already connected.
19. **Stage 2 compatibility** appears only when the plan drops something or refuses. Refusal
    is blocking with no write action. Dropped fields are listed by name, with *Write anyway*
    and *Cancel*.
20. **Stage 3 slot picker** — seven large targets, C1…C7. Each shows "Unknown" for its
    contents; this client does not read slots back (v1.1) and does not keep bookkeeping
    (`roadmap.md` §3).
21. **Stage 4 confirm** — a second tap on the chosen slot.
22. **Stage 5 progress** — `n / total` in tabular figures with the current property named.
    The sheet is non-dismissible and back is intercepted with a cancel confirmation.
23. **Stage 6 result** — success names the slot; failure names the failing property and the
    PTP response code, with Retry.
24. Haptics: `Confirm` on success, `Reject` on failure. This matters more than it sounds —
    you will be looking at the camera, not the phone, when the write lands.
25. Entry point: the **Write to camera** action on the recipe view screen's floating toolbar.

## Out of scope

| Deferred | To | Why |
|---|---|---|
| Reading slot contents back | v1.1 | `PRD.md` §14.3. Would turn "Unknown" into real contents. `fuji-recipes-book/camera/read-slot.ts` is already written. |
| Recording that a recipe was written | won't do | `roadmap.md` §3. `lastWrittenSlot` / `lastWrittenAt` pass through untouched inside `extra` so the web client's own bookkeeping is not destroyed. |
| Writing the seven unmodelled per-slot settings | won't do | No recipe models them. Giving one of them a field is what would change this. |
| Batch writing several recipes | won't do | Seven slots and one hand. |

## Error surfaces

| Failure | Surface | Message names |
|---|---|---|
| Body has no custom slots | Blocking card, write action disabled | The generation and that it has no custom slots |
| Recipe postdates the body | Warning card listing dropped fields, *Write anyway* offered | Each dropped parameter by its label, and why |
| Slot select or name refused | Failure stage, no partial write claimed | The step and the PTP response name |
| A parameter refused mid-write | Failure stage, Retry | The parameter's label and the response name, e.g. `InvalidDevicePropValue` |
| Unplugged mid-write | Failure stage | That the cable went, and that the slot may be partly written |
| Plan and step disagree about a value's kind | Refused before anything is sent | That it is a bug, with the step id — never a silent zero |
