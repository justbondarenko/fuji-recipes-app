# FEAT-003: Recipe form

**Source:** `PRD.md` §8.3; web `spec.md` US-4 and FR-001…FR-004
**Parity target:** `fuji-recipes-book/src/pages/recipe/new.vue`, `recipe/[id]/edit.vue`, `src/components/recipe/RecipeForm.vue`
**Status:** Ready

---

## Problem

The app can read a library it cannot change. Every recipe still has to be created on the
laptop, which defeats the point of having it in the bag with the camera — the moment you
want a new recipe is the moment you are standing somewhere having just taken a photo.

This is also the first feature that **writes**, so it establishes the mutation path every
later feature reuses.

## User stories

- As the photographer, I can create a recipe on the phone, so that a new idea does not have to wait until I am at a desk.
- As the photographer, I can change any parameter of an existing recipe, so that refining a look is a two-minute job.
- As the photographer, I can duplicate a recipe and adjust the copy, so that making a variant does not mean re-entering twenty values.
- As the photographer, I can delete a recipe I no longer want, with a confirmation so it does not happen by accident.
- As the photographer, I can set a rating or add a tag straight from the recipe I am reading, so that filing a recipe is not a trip through a form.
- As the photographer, I am warned before I abandon unsaved edits, so that a mis-swipe does not lose work.
- As the photographer, a save that fails tells me why and keeps what I typed, so that a lost signal never costs me the recipe.

## Scope

### The form

1. One screen for both create and edit — `RecipeEditorRoute(id)`, where a null id is create.
2. Fields grouped exactly as the view groups them (`field-definitions.md` §2), using the
   same `RecipeFields` table. The form and the view disagreeing about what a recipe contains
   would be a bug in one of them.
3. **Identity:** name (required, 1–80), notes (0–2000), rating (0–5), tags (≤20, each 1–30).
4. **Numeric fields** render as a **number input with `−` to its left and `+` to its right.**
   The field itself is editable, not a readout — colour temperature spans 2500–10000 in
   steps of 100, and 75 taps is not an input method. `−`/`+` step by the field's own step,
   clamped to its range.
5. **Enum fields** render as a dropdown of the labels from the field table.
6. **Film simulation** gets a picker showing the swatch and label, since it is the field that
   defines the recipe.
7. Applicability is live: changing the film simulation to a monochrome one removes the colour
   row as you watch, and turning the grain effect off removes grain size. Same predicates as
   the view, so the two cannot drift.
8. **No sensor-generation control, and none is stored.** The owner does not care about it;
   the D1 column is gone. Every field is offered, which is what the widest field set means.
   The generation predicates stay in the field table because FEAT-005 needs them — a
   *connected camera* reports its own generation, and the write plan must drop what that body
   cannot take. The form simply never asks.

### Saving

9. Create is `POST /api/recipes`; edit is `PATCH /api/recipes/:id` with the changed
   properties. Contract: `fuji-recipes-book/specs/contracts.md`.
10. The client validates before sending — name length, tag count and length, numeric ranges —
    and the server validates again. A 422 is rendered against the field path it names.
11. **Writes require the network.** There is no queue and no offline draft
    (`architecture.md` §4). A failed save reports why, **keeps every value the user typed**,
    and offers to retry.
12. On success the library refreshes so the list and the view reflect the change.

### Delete and duplicate

13. **Delete** — `DELETE /api/recipes/:id`, behind a confirmation naming the recipe. Returns
    to the list.
14. **Duplicate** — opens the form pre-filled from an existing recipe with no id, so saving
    creates a new recipe and the original is untouched. The name gets a suffix so two
    identical names do not appear in the list.

### From the view screen

15. Rating and tags are editable **in place** on the recipe view, saved with `PATCH`. Every
    other parameter stays read-only there — those are the two that get adjusted while
    looking at a photo, and sending someone into a form to add one tag is friction with no
    purpose.

### Leaving

16. **Unsaved-changes guard** on back, system back and predictive back. Confirm, discard or
    keep editing.
17. A form with no changes leaves silently — a guard that fires when nothing was typed
    teaches people to dismiss it without reading.

## Out of scope

- Offline writes, a mutation queue, conflict resolution (v2 — `architecture.md` §4)
- Reordering / editing `sortKey` (v2)
- Export, import, copy-as-text (v2)
- Camera writing (FEAT-005)
- Sensor generation, in any form (won't do — §8)
- Slot bookkeeping (won't do — `roadmap.md` §3)

## Constraints

- The form reads the same `RecipeFields` table as the view. No second definition of what a
  recipe contains.
- Validation rules mirror `fuji-recipes-book/specs/shared/recipe.schema.json`. The server is
  the guarantee; the client's copy exists to fail fast, not to be the authority.
- `settings` keys this build does not know, and unknown top-level keys, **survive an edit** —
  a PATCH must not drop a field a newer web client wrote (`coding-standards.md` P2).
- No I/O in composables; the form's state is a `StateFlow` on a ViewModel.

## No data contract

**No `02-schema.json`.** The request bodies are specified in
`fuji-recipes-book/specs/contracts.md` (`POST /api/recipes`, `PATCH /api/recipes/:id`), and
the recipe shape is canonical in `recipe.schema.json`. Duplicating either here would create
a second copy to drift.

## Open questions

None. Four decisions were taken by the owner before work started: stable Material 3 for v1;
numeric input as a `−` / editable field / `+` stepper (*"for now"* — revisit if it proves
slow on the wide-range fields); no sensor generation anywhere; and delete, duplicate,
in-place rating/tags and the unsaved guard all in scope.
