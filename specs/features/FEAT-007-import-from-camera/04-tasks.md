# FEAT-007 — tasks

Ordered. Dependencies come first. Each item is one logical commit.
Paths are relative to `app/src/main/java/dev/bondarenko/fujirecipes/` unless stated.

**Prerequisite:** FEAT-005 and FEAT-006, both verified on an X-T50.
**Reference:** `fuji-recipes-book` @ `0c17106`. Every transcribed file carries the P3 header.
**Branch:** `feat/FEAT-007-import-from-camera`.

---

## Decoding — pure, no `android.*` (P4)

- [ ] **T-01** — `camera/plan/CameraEncoding.kt` gains `decodeValue` ← `encoding.ts`'s decoding
      half, deliberately left out of FEAT-006 because nothing read a value then. Tone values
      **divide** by ten (`code / 10`, not `code * 0.1` — half steps have to compare equal);
      every lookup table reverses; grain splits one code back into an effect *and* a size;
      `null` means this build cannot name the code, which is a finding rather than a bug.
- [ ] **T-02** — `CameraEncodingTest` gains the round trip: `decodeValue(encodeValue(v)) == v`
      for every field and every documented value, plus the cases where it must **not** round
      trip — an unknown code, and grain "off" having no size to report.

## Reading a whole slot

- [ ] **T-03** — `camera/usb/SlotReader.kt` gains `readSlotRecipes(session, onProgress)` ←
      `read-slot.ts`. Per slot: select, settle, read the name, read each mapped property
      **once, cached by property code** (`grainEffect` and `grainSize` are both `0xD195`),
      decode into a settings object. A slot that decodes to nothing is dropped; a nameless one
      becomes `Slot C3`.
- [ ] **T-04** — `FakeCamera` gains per-slot settings registers, so a slot can hold a real
      recipe and the reader can be driven end to end.
- [ ] **T-05** — `SlotReaderTest`: a slot that decodes; an empty slot dropped; a refused slot
      dropped without costing the others; progress reported once per slot; the shared grain
      property read once.
- [ ] **T-06** — `CameraController.readSlotRecipes()`, behind the same lock as everything else
      so it cannot interleave with a write — they share the slot selector.

## Comparing — pure

- [ ] **T-07** — `camera/plan/RecipeConfig.kt` ← `recipe-config-equal.ts`. `normaliseSettings`
      first: fill each applicable field's default and null the inapplicable ones. Without it a
      recipe that omits `sharpness` and one that sets it to `0` compare unequal and every
      import looks new. Then `areConfigsEqual` — simulation first, then every applicable field
      — and `findDuplicateConfig`. Reuses `RecipeFields.applicable(FieldContext)` rather than
      re-deriving applicability (P3).
- [ ] **T-08** — `RecipeConfigTest`: identical configs match; one differing field does not;
      an omitted value equals its default; a field that does not apply to the simulation is
      not compared; a different simulation never matches.

## The review — pure

- [ ] **T-09** — `data/import/ImportReview.kt` ← `import-review.ts`, minus the id-conflict
      path — see `01-functional.md` §15/§16 for why it cannot arise. Three statuses: `NEW`,
      `CONFIG_DUPLICATE` carrying the matched recipe, `NAME_WARNING`. Duplicates detected
      against the library **and** against earlier rows in the same batch. Under `data/` rather
      than `ui/`, like `data/library/LibraryView.kt` (P7).
- [ ] **T-10** — Default selection: new and name-warning selected, duplicates not. The
      request body is the selected rows, carrying **no ids** (P2).
- [ ] **T-11** — `ImportReviewTest`: one case per status, the in-batch duplicate, the default
      selection, and that a name match with different settings is a warning rather than a
      duplicate.

## The route

- [ ] **T-12** — `core/net/ApiClient.kt` gains `importRecipes` → `POST /api/import`
      (`contracts.md`), returning `imported` / `skipped` / `replaced` / `failed`. One atomic
      request rather than one per recipe (`SF-016`). `resolutions` is always `{}`.
- [ ] **T-13** — `RecipeRepository.import(...)` + `NetworkRecipeRepository`, refreshing the
      snapshot on success so the library shows the new recipes without a manual pull.
- [ ] **T-14** — `ApiClientImportTest` with `MockWebServer`: the request shape, no ids in the
      body, the success envelope, and a 422 mapped to `ValidationFailed`.

## The screen

- [ ] **T-15** — `ui/import/ImportViewModel.kt`: not-connected → reading → review → importing
      → result, with the row selection and the network check.
- [ ] **T-16** — `ui/import/ImportScreen.kt`: one row per slot with slot number, name, film
      simulation, status and a checkbox. Duplicates deselected and naming their match.
      `@Preview` for every state, including the empty-camera and everything-already-held ones.
- [ ] **T-17** — `ImportRoute` in `ui/nav/FujiNavHost.kt`, a row on
      `ui/settings/SettingsScreen.kt`, strings in `strings.xml`.
- [ ] **T-18** — `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` green.
      Emulator: every state except reading, which needs a camera.
      **Hardware, on the X-T50:** read all seven slots and check the decoded recipes against
      the camera's own menu. This is also the check FEAT-006 is waiting on — it proves the
      properties *mean* what the tables say, which a successful write cannot.
