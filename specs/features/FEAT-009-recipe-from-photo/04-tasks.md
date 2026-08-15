# FEAT-009 — tasks

Ordered. Dependencies come first. Each item is one logical commit.
Paths are relative to `app/src/main/java/dev/bondarenko/fujirecipes/` unless stated.

**Prerequisite:** FEAT-001 (the library) and FEAT-003 (the editor, which the save path reuses).
Nothing here needs the camera.
**Reference:** `fuji-recipes-book` @ `0c17106`. Every transcribed file carries the P3 header.
**Branch:** `feat/FEAT-009-recipe-from-photo`.

---

## The parser — pure, no `android.*`, no I/O

- [ ] **T-01** — `data/photo/FujiExifParser.kt` ← `src/utils/fuji-exif-parser.ts`. The byte
      walk: SOI check, APP1 scan for the camera model in IFD0, the `FUJIFILM` signature scan,
      the MakerNote IFD offset, and its 12-byte entries by format (3 = u16, 4 = u32, 9 = i32).
- [ ] **T-02** — The lookup tables, in **their own file section with a header naming the
      dialect**. These are neither the custom-slot codes (`camera/plan/CameraEncoding.kt`) nor
      the RAW-profile ones, and crossing them would produce a recipe that looks right and is
      wrong. Tone curves are negated and ×16; NR is a nine-entry table; clarity is ×1000;
      grain and chrome are 0/32/64.
- [ ] **T-03** — Values map into **the app's own ids** — `dr400`, `color-temp`,
      `auto-ambience-priority` — so the result feeds the matcher and the editor without a
      second translation. WB shift is a pointer tag: two `int32`s, ÷20, bounded to ±9.
- [ ] **T-04** — Failures are a named sealed result, never an exception: `NotJpeg`, `TooLarge`,
      `NoExif`, `NotFujifilm`, `Unreadable`. P5 — "no metadata at all" and "not a Fujifilm
      photo" send the user to different places. An unknown *code* omits its field: a recipe
      missing a value is useful, one carrying a wrong value will match something.

## Parser tests

- [ ] **T-05** — `SyntheticJpeg.kt` in `src/test`: assembles SOI + APP1 + TIFF + a Fujifilm
      MakerNote IFD from a tag map. The reference's own suite guards on a sample file that is
      not in its repo, so its real case never runs; this one always does.
- [ ] **T-06** — `FujiExifParserTest`: a full recipe decodes; the tone-curve negation
      (`-64` → `+4`), which is the easiest thing here to get backwards; the NR table; clarity's
      scale; a Kelvin white balance carrying its temperature; the WB-shift pointer; an unknown
      film-mode code omitting the field; and each named failure, including a non-Fuji JPEG
      being a different answer from one with no EXIF.

## The matcher — pure

- [ ] **T-07** — `data/photo/RecipeMatcher.kt` ← `src/utils/recipe-matcher.ts`. Compares only
      the fields the photo carried; percentage, exact flag, and per-field mismatches carrying
      both values formatted. Sorted exact-first then by percentage; 70% is the floor for
      "similar". Labels come from `FilmSimulations` (P3).
- [ ] **T-08** — `RecipeMatcherTest`: an exact match; a one-field difference named with both
      values; scoring out of what the photo carried rather than out of everything; ordering;
      the 70% floor; an empty library.

## The screen

- [ ] **T-09** — `ui/photo/PhotoReaderViewModel.kt`: pick → read → parse → match, with each
      named failure as its own state.
- [ ] **T-10** — `ui/photo/PhotoReaderScreen.kt`: the picker
      (`ActivityResultContracts.PickVisualMedia` — **no permission**), the decoded settings,
      and the three result shapes (exact, near with its differences, none). Copy as text via
      `RecipeTextFormatter`. `@Preview` for every state including each failure.

## Nav and the hand-off

- [ ] **T-11** — `ui/shell/AppShell.kt` gains a third `FloatingToolbarItem`:
      **Recipes · Read · Settings**. `PhotoRoute` in `ui/nav/FujiNavHost.kt`, and
      `MainActivity` learns the third destination for its selected state.
- [ ] **T-12** — `RecipeEditorRoute` gains `prefill: String?` — the decoded settings as JSON —
      and `RecipeEditorViewModel` starts from it when there is no id to load. A nav argument
      rather than a holder on `AppContainer`, so it survives process death like every other
      route argument.
- [ ] **T-13** — Strings in `strings.xml`.

## Verification

- [ ] **T-14** — `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` green.
      Emulator: push a real Fujifilm JPEG to the device, read it, and check the decoded
      settings against what the camera reports for that shot.
      **The check that proves the whole chain, and it needs the X-T50:** shoot a frame with a
      recipe that is already in the library, read that photo, and confirm it comes back as an
      **exact match by name**. That exercises the parser, the value mapping and the matcher at
      once — and it approaches the same claim the camera-write path makes, from the other
      direction.
