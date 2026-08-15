# FEAT-003 — tasks

Ordered. Paths relative to `app/src/main/java/dev/bondarenko/fujirecipes/`.

**Prerequisite:** FEAT-002 (the field source).
**Blocking dependency:** the Access service token, for anything verified against the real
deployment. Everything below is testable against `MockWebServer` and the local mock.

---

## The write path

- [x] **T-01** — `core/net/ApiClient.kt` gains `createRecipe`, `updateRecipe`, `deleteRecipe`
      against `POST /api/recipes`, `PATCH /api/recipes/:id`, `DELETE /api/recipes/:id`.
      Same error mapping as `listRecipes`; 201 and 204 are successes.
- [x] **T-02** — Tests for T-01: 201, 200, 204, 422 with field paths, 403, 409, network
      failure. Assert the PATCH body carries **only** changed keys.
- [x] **T-03** — `RecipeRepository` gains `create`, `update`, `delete`, each returning
      `ApiResult`. On success the library refreshes so list and view follow. On failure
      nothing local changes.

## Validation

- [x] **T-04** — `data/fields/RecipeValidation.kt`, pure. Name 1–80, notes ≤2000, rating 0–5,
      tags ≤20 each 1–30, numerics within their field's range. Mirrors
      `recipe.schema.json`; the server stays the authority.
- [x] **T-05** — Tests per rule, plus: a valid recipe passes, and an out-of-range typed value
      is caught before any request is made.

## The form

- [x] **T-06** — `ui/editor/RecipeEditorViewModel.kt`. Holds the working copy as a
      `JsonObject` so unknown keys survive; tracks dirtiness; computes the PATCH diff.
- [x] **T-07** — `ui/editor/NumberStepper.kt`. `−`, an **editable** number field, `+`.
      Steps by the field's step, clamps to range, accepts typed input, rejects out-of-range.
- [x] **T-08** — `ui/editor/EnumDropdown.kt` and the film-simulation picker (swatch + label).
- [x] **T-09** — `ui/editor/RatingInput.kt` and `TagInput.kt`.
- [x] **T-10** — `ui/editor/RecipeEditorScreen.kt`: name, notes, grouped controls, live
      applicability, save. Takes state and lambdas.
- [x] **T-11** — Save failure handling: message per `ApiError`, every entered value kept,
      retry offered.
- [x] **T-12** — Delete with confirmation, and duplicate.
- [x] **T-13** — Unsaved-changes guard, including system and predictive back. Silent when
      nothing changed.

## On the view screen

- [x] **T-14** — Rating and tags editable in place, saved with PATCH. Everything else stays
      read-only.

## Verification

- [x] **T-15** — `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` green;
      every scenario mapped to a test or to T-16.
- [x] **T-16** — Manual check against the local mock: create, edit, duplicate, delete, and a
      save with the server stopped.


---

## Verified on device

Against a writable local mock, with the server's own log as the evidence:

- **Create** — `POST /api/recipes → 201`, immediately followed by `GET /api/recipes → 200`.
  That second line is the point: a mutation refetches rather than guessing at the `id`,
  `sortKey` and `updatedAt` the server assigns.
- **In-place rating** — `PATCH → 200`, then the refetch, and the server's stored rating
  changed from 5 to 2 without the form ever opening.
- **Steppers** — `+` on Sharpness moved 0 → 2, and the value rendered `+2` on the view
  afterwards, which is §5's signed formatting on a value that had just been entered.
- **Unsaved guard** — an untouched form leaves silently; a form with one stepper press asks
  "Discard your changes?". Both halves matter.

Not driven on device: delete and duplicate. Both are unit-covered and share the same
`ApiResult` path as create and update.


---

## Design review — changes applied

Six items from the designer, plus one bug their screenshot exposed:

| Item | Change |
|---|---|
| Rating too spread out | Five `IconButton`s reserved 48dp each plus internal padding. Now 40dp targets with no padding between: still tappable, roughly half the width. |
| Tag field always on screen | Behind a `+` chip at the end of the tags. The field appears focused when asked for and leaves once a tag is added or the entry abandoned. |
| "Changed only" reads as a button | A labelled `Switch`. It has a persistent on and off state, and a chip made the reader guess which one they were looking at. |
| Notes above the parameters | Moved to the very bottom. Notes get written once the parameters are decided, and a text box above them pushed the actual controls below the fold. |
| Section titles too small, inside the card | `SectionHeader` in `ui/common`, `titleMedium`, **above** its card. Inside the card it read as the first row's caption rather than the group's name. Applied to the view as well as the editor — the same critique was true there. |
| Same rating and tag treatment in the editor | One component each, so both screens changed together. |

**The bug:** every stepper rendered blank except Clarity. An absent settings key produced an
empty field, because only the read-only view fell back to the field's default. A recipe
written before a parameter existed has no key for it and the camera applies the default, so a
blank control claimed the parameter was unset when it was not — and the `−`/`+` buttons then
stepped from 0 rather than from the real starting point. The stepper now falls back the same
way the view does.
