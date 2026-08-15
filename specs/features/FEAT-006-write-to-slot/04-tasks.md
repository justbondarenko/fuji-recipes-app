# FEAT-006 — tasks

Ordered. Dependencies come first. Each item is one logical commit.
Paths are relative to `app/src/main/java/dev/bondarenko/fujirecipes/` unless stated.

**Prerequisite:** FEAT-005 T-01 to T-18. The transport and session are what this runs on.
**Reference:** `fuji-recipes-book` @ `0c17106`; rules from
`specs/shared/field-definitions.md` §6 and §7. Every transcribed file carries the P3 header.
**Branch:** `feat/FEAT-005-camera-connection`, shared with FEAT-005 — see `roadmap.md` §1.

---

## Pure plan layer — no `android.*` (P4, WR-09)

- [x] **T-01** — `camera/plan/CameraEncoding.kt` ← `camera/encoding.ts`. Property codes
      (`0xD18C` slot, `0xD18D` name, `0xD18E`–`0xD1A5` settings), `FIELD_PROPERTIES`,
      `WRITE_ORDER`, `HIGH_ISO_NR_CODES`, the film-simulation / dynamic-range / effect /
      grain / white-balance code tables, `encodeValue`, `grainCode`,
      `propertyCodesVerifiedFor`.
- [x] **T-02** — `CameraEncodingTest.kt` ← `tests/unit/camera-encoding.spec.ts`. Checked
      against `field-definitions.md` §4 rather than against itself. The three that must not
      cross with the RAW-profile encodings get their own cases: 1-indexed effects, raw
      percentage dynamic range, the flat five-value grain enum. WR-08 by table, not formula.
- [x] **T-03** — `camera/plan/WritePlan.kt` ← `camera/write-plan.ts`.
      `buildWritePlan(recipe, camera, slot)` returning slot, ordered steps, total, dropped,
      notes, refusal, incompatible. Preset name truncated to its maximum with a note.
- [x] **T-04** — `camera/ptp/StepPayload.kt` ← `camera/usb/payload.ts`. Lives beside the
      byte code rather than the plan so P4's import ban holds through the whole chain.
      Refuses a kind/value mismatch instead of packing a zero.
- [x] **T-05** — `WritePlanTest.kt`. One case per rule, named for it: WR-01 refusal with no
      steps, WR-02 adjacency, WR-03 monochrome drops colour, WR-04 grain size, WR-05
      monochromatic colour incl. the zero-means-omit case, WR-06 advisory fields absent from
      *both* steps and dropped, WR-07 generation gating, WR-10 simulation dropped. Plus:
      slot and name are the first two steps and are fatal, and the same input twice gives an
      identical plan.

## Execution

- [x] **T-06** — `camera/usb/WriteExecutor.kt` ← `camera/usb/write.ts`. Executes steps in
      order over `PtpSession`, emits progress per step, settles after the slot select,
      aborts on a fatal failure, records and continues on a non-fatal one, produces an
      outcome carrying every step's status and a partial-write warning when one applies.
- [x] **T-07** — `WriteExecutorTest.kt` against `FakeCamera` (FEAT-005 T-07): a clean run
      writes every step in order; a refused slot select attempts nothing further; a refused
      non-fatal step leaves the rest written and is reported; a disconnect mid-run produces
      the partial-write warning.
- [x] **T-08** — `CameraController` gains the `Writing(slot, done, total, current)` state, a
      partial wake lock held only for the write, and a detach mid-write that aborts cleanly
      rather than leaving the state stuck.

## The write sheet

- [x] **T-09** — `ui/camera/WriteSheetState.kt`. The stage machine as a pure type —
      connection, compatibility, picker, confirm, progress, result — so the stages are
      testable without the sheet.
- [x] **T-10** — `ui/camera/WriteSheet.kt` + `WriteViewModel.kt`. `PRD.md` §7.4's six stages
      in one expanding `ModalBottomSheet`. Connection stage skipped when already connected.
      Non-dismissible during the write, back intercepted with a cancel confirmation.
- [x] **T-11** — Slot picker: a row per slot, C1–C7, each carrying what the camera says it
      holds. `ButtonGroup` is unreachable at material3 1.4.0 (`tech-stack.md` §6), and would
      not have carried a second line anyway.
- [x] **T-12** — Progress: `n / total` in tabular figures with the current property named.
      Haptic `Confirm` on success and `Reject` on failure.
- [x] **T-13** — Entry point: **Write to camera** on `ui/recipe/RecipeViewScreen.kt`'s
      floating toolbar. Disabled with a reason when no camera is connected or the body
      cannot take a write.
- [x] **T-14** — Strings in `strings.xml`. `@Preview` for every stage, including the
      refusal, the dropped-fields warning and the failure.
## Reading the slots

- [x] **T-16** — `camera/plan/SlotStates.kt` ← `fuji-recipes-book/src/utils/slots.ts`.
      `SlotNameReading`, the five statuses, `slotStates`, `slotCaution`. Pure (P4); the labels
      live in `strings.xml` rather than in the module, per the Compose conventions.
- [x] **T-17** — `camera/usb/SlotReader.kt` ← `camera/read-slot.ts`'s `readSlotNames`. Select
      the slot, settle, read `0xD18D`. A `PtpError` marks that slot unread and the rest
      continue; anything else propagates. `SlotReaderTest` against `FakeCamera`, which gains
      per-slot name registers so the settle and the selector are exercised properly.
- [x] **T-18** — `WriteViewModel` reads the slots every time the picker is reached, never
      cached: the answer changes without this app, and a stale name is a wrong fact rather
      than a missing one. `SlotStatesTest` covers the four post-read states and which of them
      need a second tap.

- [x] **T-15** — `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` green.
      Emulator: every stage renders. **Hardware: a recipe was written to C6 of an X-T50 and
      the write completed.** The chain ran end to end on a real body — plan, pack, slot
      select, name, seventeen properties, read-back. Open: whether every property *landed as
      the recipe describes it*, which only the camera's own menu can confirm — see
      `tasks/todo.md`.
