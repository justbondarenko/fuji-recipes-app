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

- [ ] **T-06** — `ui/editor/RecipeEditorViewModel.kt`. Holds the working copy as a
      `JsonObject` so unknown keys survive; tracks dirtiness; computes the PATCH diff.
- [ ] **T-07** — `ui/editor/NumberStepper.kt`. `−`, an **editable** number field, `+`.
      Steps by the field's step, clamps to range, accepts typed input, rejects out-of-range.
- [ ] **T-08** — `ui/editor/EnumDropdown.kt` and the film-simulation picker (swatch + label).
- [ ] **T-09** — `ui/editor/RatingInput.kt` and `TagInput.kt`.
- [ ] **T-10** — `ui/editor/RecipeEditorScreen.kt`: name, notes, grouped controls, live
      applicability, save. Takes state and lambdas.
- [ ] **T-11** — Save failure handling: message per `ApiError`, every entered value kept,
      retry offered.
- [ ] **T-12** — Delete with confirmation, and duplicate.
- [ ] **T-13** — Unsaved-changes guard, including system and predictive back. Silent when
      nothing changed.

## On the view screen

- [ ] **T-14** — Rating and tags editable in place, saved with PATCH. Everything else stays
      read-only.

## Verification

- [ ] **T-15** — `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` green;
      every scenario mapped to a test or to T-16.
- [ ] **T-16** — Manual check against the local mock: create, edit, duplicate, delete, and a
      save with the server stopped.
