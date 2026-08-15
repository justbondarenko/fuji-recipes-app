# FEAT-002: Field source and recipe view

**Source:** `PRD.md` §8.4; web `spec.md` US-3 and FR-020…FR-026
**Parity target:** `fuji-recipes-book/src/pages/recipe/[id]/index.vue`, `src/components/recipe/RecipeView.vue`
**Status:** Ready

**Reverses a deferral.** `roadmap.md` §3 put the read-only view in v2 and sent a card tap
straight to the editor. Reinstated at the owner's request: reading a recipe is the thing
that happens far more often than changing one, and a form is a poor way to read.

---

## Problem

The list identifies a recipe; it does not tell you what is in one. Today the only way to see
a recipe's twenty-odd parameters on the phone is to open it in a form, which invites edits
you did not mean to make and buries the values you came to read among inputs.

This feature adds the screen you actually spend time on, and — because nothing can display
a parameter set without knowing what the parameters *are* — the canonical field source that
the form (FEAT-003) and the camera write plan (FEAT-005) both build on.

## User stories

- As the photographer, I can open a recipe and read its settings laid out for reading, so that I am not editing a form to answer a question.
- As the photographer, I can see at a glance which parameters I deliberately changed and which sit at their default, so that the recipe's character is legible in seconds.
- As the photographer, I can hide the untouched parameters, so that a recipe with three changes reads as three changes.
- As the photographer, I never see a parameter my camera cannot use, so that the screen reflects my body rather than the format.
- As the photographer, I can get from reading to editing in one action, so that noticing a problem and fixing it are not separate journeys.
- As the photographer, opening a recipe that no longer exists tells me so and gets me back, so that a stale link is not a dead end.

## Scope

### The field source

1. `RecipeFields.kt` transcribes `fuji-recipes-book/specs/shared/field-definitions.md` §4 —
   all 27 rows, with group, label, type, range, step, default, applicability and the
   advisory flag. Carries the vendoring header (`architecture.md` §2).
2. Groups render in §2's order: `identity`, `simulation`, `tone`, `effects`,
   `white-balance`, `monochromatic`, `shooting`.
3. Applicability is a predicate over `(generation, filmSimulation)`. **An inapplicable field
   is omitted** — never disabled, never "N/A".
4. **`sensorGeneration` is not a stored column.** D1 migration 0002 dropped it. The view
   reads it from `extra` when an older recipe carries one, and otherwise assumes
   `xtrans-v` — the widest field set and the body this tool is verified against
   (`field-definitions.md` §1). The assumption is stated in code, not hidden.
5. `grainSize` has a two-part rule: generation **and** `grainEffect != off`. The second half
   depends on another field's value, so the predicate takes the recipe, not just the body.

### Display rules — `field-definitions.md` §5

6. Enums render their **label**, never their id.
7. Signed numerics carry an explicit sign when non-zero (`+2`, `-1`); zero is bare `0`.
8. Half steps show one decimal (`+1.5`).
9. Off states render the word **Off**, never blank or a dash.
10. A null advisory field (`isoMin`, `isoMax`) **omits its row entirely**.
11. Colour temperature renders `5500K`, no space.
12. WB shift is **one combined row**: `R +3 / B −2`.
13. Values at their default render at reduced emphasis, so changed fields are scannable.
14. Numeric values use tabular figures.

### The screen

15. Route `RecipeViewRoute(id)`, reached by tapping a card. Its top bar reads **"Back to
    list"** rather than repeating the recipe name, which is already the largest thing on
    the screen.
16. Header: name, film-simulation badge and label, rating and tags. **No last-written
    line** — see `roadmap.md` §3.
17. Settings grouped per §2, each row a label and a formatted value.
18. A **Changed only** toggle hides every field sitting at its default. Off by default —
    the full set is the honest first view; the toggle is for when you already know what you
    are looking for.
19. When the toggle is on and nothing has changed, say so rather than showing an empty screen.
20. An **Edit** action routes to the editor (FEAT-003; a placeholder until then).
21. **Not found** — a recipe id that is not in the library shows a clear state with a route
    back to the list, not a blank screen or a crash.
22. The screen reads from the already-loaded library. It does **not** issue
    `GET /api/recipes/:id` — the whole library is in memory (`architecture.md` §4), and a
    per-recipe fetch would make an offline read fail for data the app already holds.

## Out of scope

- Editing anything, including rating and tags in place (FEAT-003 — the web allows it here, but on Android that needs the PATCH path the form will build)
- Duplicate, delete, export, copy-as-text (FEAT-003)
- Write to camera (FEAT-005)
- Previous/next navigation through the filtered order (v2 — needs the filtered list handed across the nav boundary)
- Notes rendering beyond a plain paragraph
- `GET /api/recipes/:id` (§22)

## Constraints

- The field table is transcribed from the canonical document, never from the web client's
  TypeScript. Two transcriptions of one document, never a transcription of a transcription.
- Formatting is pure and lives outside Compose, so §5's rules are testable directly.
- A recipe carrying an unknown enum value must still render — the raw id is shown rather
  than the row being dropped, matching how the list handles an unknown film simulation.

## No data contract

**No `02-schema.json`.** This feature introduces no persisted or transmitted shape: it reads
`Recipe`, already defined in FEAT-001's schema and canonically in
`fuji-recipes-book/specs/shared/recipe.schema.json`. The field table is a display
definition, not a wire format.

## Open questions

None.
