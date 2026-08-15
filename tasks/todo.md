# Task: FEAT-012 — import a recipe file

Branch: `feat/FEAT-012-import-from-file`

## Goal

Import the files this app's Export feature produces (`.json` envelope, bare array, bare
recipe, and `.zip` archives), which today can only be imported by the web client. Camera
import (FEAT-007) stays exactly as it is; this is the other half of the round trip that
`specs/roadmap.md` deferred.

Binding spec: `fuji-recipes-book/specs/shared/recipe-format.spec.md` — SF-003 to SF-017.
Reference implementations: `shared/format/import.ts`, `shared/format/migrate.ts`,
`src/utils/import-review.ts`, and `POST /api/import` in `specs/contracts.md`.

## Plan Items

- [x] 1. `data/importing/RecipeFile.kt` — read one JSON document in any of SF-005's three shapes; reject wrong `format` (SF-003), bad or future `version` (SF-004, SF-006); run the migration chain (SF-007) <!-- id: 1 -->
- [x] 2. Same file — ZIP: every `.json` entry at any depth, sorted, non-JSON ignored, traversal paths refuse the whole archive (SF-015) <!-- id: 2 -->
- [x] 3. `data/importing/FileReview.kt` — five statuses (new / conflict / config-duplicate / name-warning / invalid), the resolutions (SF-010 to SF-014), and the `POST /api/import` body <!-- id: 3 -->
- [x] 4. Unit tests from the spec's own conformance fixtures (§6) <!-- id: 4 -->
- [x] 5. `ui/importing/FileImportScreen.kt` + view model: pick a file, review, resolve conflicts, import <!-- id: 5 -->
- [x] 6. Route, a More card, strings <!-- id: 6 -->
- [x] 7. Tests, lint, and a run on the emulator against real files <!-- id: 7 -->

## Decisions

- **A separate screen, not a second source on the camera Import screen.** The camera rows are
  keyed by slot and have three statuses; file rows are keyed by index and have five, plus a
  resolution and per-row validation errors. Merging them would complicate both screens to save
  one entry point in More.
- **Validation reuses `RecipeValidation`** rather than a second transcription of the field
  table (`coding-standards.md` P3). Missing keys are not errors — §4 fills them with defaults
  on import, and the server's `normaliseSettings` is what does it.
- **Two deliberate deviations from the reference**, both recorded in the code:
  1. A **configuration duplicate is offered Skip / Keep both, not SF-012's three options.**
     Replacing needs an id to replace; a duplicate collides on nothing the server can key on.
     The web client offers Replace there and it does nothing.
  2. **Skip is honoured locally for a row with no id collision.** The reference sends
     `resolutions` keyed by id and lets the server decide, so a skipped configuration
     duplicate — which has no colliding id — is imported anyway. Dropping it from the request
     is the only place that choice can be kept.
- **Not in scope:** receiving a file from another app's share sheet (an intent filter), and
  importing from a URL. Both are extra entry points to this same screen.

## Review & Verification

### What changed

1. **[`RecipeFile.kt`](app/src/main/java/dev/bondarenko/fujirecipes/data/importing/RecipeFile.kt)** — the file reader. Envelope, bare array and bare object (SF-005); `format` and `version` refusals with the reasons named (SF-003, SF-004, SF-006, including SF-006's required wording verbatim); the migration chain with §5's `1 -> 2` identity slot (SF-007); ZIP entries at any depth, sorted, non-JSON ignored, an escaping path refusing the whole archive (SF-015). Unzipping is `java.util.zip` with a 64 MB cap on *expanded* bytes, counted as it copies — a decompression bomb is inflated on read, so measuring afterwards would be measuring from inside the failure. The shape is decided by the file's own magic bytes, not its name.
2. **[`FileReview.kt`](app/src/main/java/dev/bondarenko/fujirecipes/data/importing/FileReview.kt)** — what each entry is, and the request body. This is the full five statuses, unlike the camera path: a file carries ids, so the id-collision branch `ImportReview.kt` could never reach is the one that matters here.
3. **[`FileImportViewModel.kt`](app/src/main/java/dev/bondarenko/fujirecipes/ui/importing/FileImportViewModel.kt)** / **[`FileImportScreen.kt`](app/src/main/java/dev/bondarenko/fujirecipes/ui/importing/FileImportScreen.kt)** — choose, review, resolve, import. Reading and reviewing are local; only the import needs the network, and a failed import keeps the review and the resolutions so retrying costs nothing.
4. **More → Import a file**, its route, and the strings.

### Verification

- `./gradlew :app:testDebugUnitTest` — green, with 33 new tests: 16 for the file reader (the spec's §6 fixtures, built in the test rather than checked in) and 17 for the review and the request body.
- `./gradlew :app:lintDebug` — green.
- On the emulator, against files pushed to Downloads:
  - `lib.json` — 4 recipes: one new, one identical to a library recipe (offered Skip / Keep both), one with `clarity: 99` and one with no name. The screen read "4 found · 2 new · 2 that cannot be imported", named `clarity` and `name` on the failing rows, and kept Import disabled until the duplicate was decided.
  - The request body was captured off the wire: the two valid recipes only, timestamps preserved, and the unknown `futureField` carried through untouched (SF-017). `resolutions` was empty, correctly — nothing in that file collided on an id.
  - `bundle.zip` — one recipe found at `nested/velvia.json`, `README.txt` ignored silently, the source entry shown on the row.
  - `future.json` (`version: 99`) — refused whole, with SF-006's sentence first and the version numbers after it.
  - With the server unreachable, the review still built from the cached library and the import failed with the offline message, the rows and the chosen resolution intact.
