# FEAT-002 — tasks

Ordered. Paths relative to `app/src/main/java/dev/bondarenko/fujirecipes/`.

**Prerequisite:** FEAT-001 (the list, the repository, the snapshot).
**No blocking dependency** — this feature reads data the app already holds, so it needs
neither the Access service token nor a live deployment.

---

## The field source

- [x] **T-01** — `data/fields/RecipeFields.kt`. Transcribe `field-definitions.md` §4 row by
      row: 27 fields with group, label, type, range, step, default, `advisory`, and an
      `applies` predicate. Vendoring header naming the document, its version and the commit.
      Groups in §2's order.
- [x] **T-02** — Enum option tables with labels: dynamic range, D-range priority, grain
      effect and size, colour chrome ×2, the 14 white-balance values. Labels come from the
      document, never from an id prettified at runtime.
- [x] **T-03** — Applicability. `generation` resolves from `extra.sensorGeneration` and
      falls back to `xtrans-v`, stated in a comment with the reason. `grainSize` takes the
      recipe so it can read `grainEffect`. Tests: monochrome hides `color`, non-monochrome
      hides the monochromatic pair, grain off hides `grainSize`, colour temperature appears
      only for `whiteBalance == color-temp`.

## Display

- [x] **T-04** — `data/fields/FieldFormatting.kt`, pure and outside Compose. Implements
      `field-definitions.md` §5: enum label, explicit sign, one decimal on half steps, "Off",
      `5500K`, omitted null advisory, unknown enum falls back to its raw id.
- [x] **T-05** — The combined WB shift row (`R +3 / B -2`), which is one displayed row from
      two stored fields and therefore does not fall out of the generic renderer.
- [x] **T-06** — Tests for T-04 and T-05, one per rule in §5. This is the suite that makes
      "reads like the web client" checkable.

## The screen

- [x] **T-07** — `ui/recipe/RecipeViewState.kt` + `RecipeViewModel.kt`. Resolves the recipe
      from the repository's already-loaded library by id; a missing id is a state, not a
      crash. Holds the "changed only" toggle.
- [x] **T-08** — `ui/recipe/RecipeViewScreen.kt`. Header (name, badge, simulation label,
      rating, tags, last-written), then grouped rows. Takes state and lambdas; a
      `RecipeViewRoute` does the wiring.
- [x] **T-09** — Default versus changed emphasis, and the **Changed only** toggle, including
      the "nothing has been changed" state.
- [x] **T-10** — Not-found state with a route back to the list.
- [x] **T-11** — Navigation: a card tap opens `RecipeViewRoute`; the view's Edit action opens
      `RecipeEditorRoute`. The FAB still opens the editor directly for a new recipe.
- [x] **T-12** — `@Preview` for: a full recipe, a monochrome recipe, changed-only on,
      changed-only with nothing changed, and not-found.

## Verification

- [x] **T-13** — `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` green,
      and every scenario in `03-behavior.feature` maps to a test or to T-14.
- [x] **T-14** — Manual check on a device against the mock library: open a recipe, confirm
      grouping and omission, toggle "Changed only", and confirm an unknown enum still renders.


---

## Note from implementation

One bug the tests caught that the screen would have hidden: `format` originally dropped any
row whose stored value was absent. A recipe written before a field existed would then show
*fewer* settings than the camera will actually apply. An absent value now falls back to the
field's default and renders at reduced emphasis; null is left to mean only "an advisory
field that was never set", which is the one case §5 says to omit.
