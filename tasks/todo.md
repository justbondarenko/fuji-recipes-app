# Task: FEAT-011 — create a recipe from pasted text

Branch: `feat/FEAT-011-recipe-from-text`

## Goal

The web client can turn a pasted Fuji X Weekly / forum / notes recipe into a filled-in
creation form. Bring the same to Android, reached from a Material 3 FAB menu
(https://m3.material.io/components/fab-menu/overview) that replaces the single create FAB
with two options: **Parse text** and **Manual**.

## Plan Items

- [x] 1. Port `fuji-recipes-book/src/utils/recipe-text-parser.ts` to a pure Kotlin file under `data/text/` <!-- id: 1 -->
- [x] 2. Port the sibling's parser spec as a JVM unit test <!-- id: 2 -->
- [x] 3. Turn the right-hand FAB into a FAB menu (scrim, two labelled items, rotating icon, back to dismiss) <!-- id: 3 -->
- [x] 4. A paste sheet that shows what was recognised before committing, and opens the editor pre-filled <!-- id: 4 -->
- [x] 5. Tests, lint, and an end-to-end run on the emulator <!-- id: 5 -->

## Review & Verification

### What changed

1. **[`data/text/RecipeTextParser.kt`](app/src/main/java/dev/bondarenko/fujirecipes/data/text/RecipeTextParser.kt)** — a line-for-line port of the web parser, returning name + a `JsonObject` of `RecipeFields` ids. No Android imports, so it is testable on the JVM (P9). `sensorGeneration` is detected but discarded: D1 migration 0002 dropped the column, and the detection only survives because it decides whether a line is a setting or the title.
2. **[`ui/shell/AppShell.kt`](app/src/main/java/dev/bondarenko/fujirecipes/ui/shell/AppShell.kt)** — the create FAB became a FAB menu: scrim, two pill items, the plus rotating 45° into a close, `BackHandler` to dismiss. Hand-built because `FloatingActionButtonMenu` first ships in material3 1.5.0-alpha, which pulls compose-foundation 1.12 and the AGP 9 / compileSdk 37 move `libs.versions.toml` is deliberately pinned away from.
3. **[`ui/editor/PasteRecipeSheet.kt`](app/src/main/java/dev/bondarenko/fujirecipes/ui/editor/PasteRecipeSheet.kt)** — the paste surface. The recognised-count is live on every keystroke, because the failure it guards against is a paste that parses to almost nothing and silently opens an empty form.
4. **[`MainActivity.kt`](app/src/main/java/dev/bondarenko/fujirecipes/MainActivity.kt)** — the sheet is state, not a route: a destination of its own would leave an empty text box in the back stack behind every recipe made from a paste. Import reuses `RecipeEditorRoute(prefill, prefillName)`, already built for FEAT-009.

### One deliberate deviation from the source

The web parser tests `1/3` before `-1/3`, and `"-1/3"` contains `"1/3"`, so its negative
exposure-compensation branches are unreachable — `-1/3 EV` arrives as `+0.333`. The Kotlin
port tests the negatives first and has a test for it. Worth fixing on the web side too.

### Verification

- `./gradlew :app:testDebugUnitTest` — green, including 10 new parser cases ported from the sibling's spec.
- `./gradlew :app:lintDebug` — green.
- On the emulator, end to end: FAB → menu opens with a scrim → **Parse text** → sheet → *Insert an example* reports "Found 15 settings" → **Fill the form** opens the editor with Classic Chrome, DR400, highlight −1, shadow 1.5, colour +2, sharpness 0, high ISO NR −2, clarity −2, grain Strong/Small, CC Strong, FX blue Weak, WB Auto with R +2 / B −3. **Manual** opens an empty form at the documented defaults.
