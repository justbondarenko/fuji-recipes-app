# FEAT-008 — tasks

Ordered. Dependencies come first. Each item is one logical commit.
Paths are relative to `app/src/main/java/dev/bondarenko/fujirecipes/` unless stated.

**Prerequisite:** FEAT-001 (the library and its snapshot). Nothing here needs the camera.
**Reference:** `fuji-recipes-book` @ `0c17106`, and `specs/shared/recipe-format.spec.md`,
which is **binding**. Every transcribed file carries the P3 header.
**Branch:** `feat/FEAT-008-export-recipes`.

---

## The format — pure, no `android.*`, no I/O

- [x] **T-01** — `data/exporting/RecipeExport.kt` ← `shared/format/export.ts`. `EXPORT_EXCLUDED`
      (SF-008 plus `sensorGeneration`), `RECIPE_KEY_ORDER`, and the 22-key
      `SETTINGS_KEY_ORDER` **stated explicitly**. Not derived from `RecipeFields`: the
      reference derived it, a UI regrouping reordered every exported file, and its conformance
      suite caught it. A display grouping and a serialisation order are different concerns.
- [x] **T-02** — `toExportRecipe`: documented keys in order, unknown keys after them
      verbatim (SF-017), excluded keys dropped, absent keys left absent, `null` preserved.
      **Values are never re-serialised** — they came from the server validated, and this
      client holds `settings` as a raw `JsonObject` (P2).
- [x] **T-03** — `buildEnvelope` + `serialise`: SF-002's envelope in SF-002's key order,
      two-space indent, trailing newline, UTF-8 with no BOM (SF-001).
- [x] **T-04** — SF-018 filenames: `libraryExportFilename(kind, at)` with a **UTC** date, and
      `recipeExportFilename` — slugified, keeping Unicode letters, falling back to
      `recipe-<id8>.json` when a name slugifies to nothing.
- [x] **T-05** — `archiveEntries`: one bare recipe per entry, names **de-duplicated** with
      `-2`, `-3`. Recipe names are not unique, and a map keyed by name would silently ship one
      recipe where there were two.
- [x] **T-06** — `RecipeExportTest`: `recipe-format.spec.md` §3's own example round-trips key
      for key; an unknown top-level key and an unknown setting both survive and land after the
      documented ones; each excluded key is absent; a 1.5 stays 1.5; an absent key stays
      absent; a null stays null; the filename rules including the fallback; two identical
      names produce two entries.

## The archive

- [x] **T-07** — `data/exporting/ZipWriter.kt`: entries to a `ByteArray` via
      `java.util.zip.ZipOutputStream` (`tech-stack.md` §2 — stdlib, no dependency).
- [x] **T-08** — `ZipWriterTest`: the archive reads back through `ZipInputStream` with entry
      names and contents intact, and an entry's content parses as a recipe.

## Sharing

- [x] **T-09** — `core/share/ShareFile.kt`: write to `cacheDir/export/`, wrap in a
      `FileProvider` URI, launch `Intent.createChooser` with
      `FLAG_GRANT_READ_URI_PERMISSION`. Clears the directory first — the cache is not a
      document store, and whatever receives the file owns the real copy.
- [x] **T-10** — `AndroidManifest.xml` gains the `FileProvider`, and
      `res/xml/file_paths.xml` scopes it to `export/` inside the cache and nothing else. No
      storage permission is requested, because none is needed.

## The screen

- [x] **T-11** — `ui/exporting/ExportViewModel.kt`: the library, the selection (**everything
      selected on open**), the JSON/ZIP choice, and the filename the action will produce.
- [x] **T-12** — `ui/exporting/ExportScreen.kt`: the selection list, select all / none, the
      live `n of m`, the format choice with a line each on when to want it, and an action that
      names its file and is disabled with a reason at zero. An empty library says so rather
      than showing a dead button.
- [x] **T-13** — `ExportRoute` in `ui/nav/FujiNavHost.kt`, a row on
      `ui/settings/SettingsScreen.kt` beside Import, strings in `strings.xml`.
      `@Preview` for all-selected, partial, none, and an empty library.

## One recipe

- [x] **T-14** — An **Export** action on `ui/recipe/RecipeViewScreen.kt` sharing a bare
      `<slug>.json` — SF-005's third shape, which import accepts directly, and readable at a
      glance, which is the point of exporting one.

## Verification

- [ ] **T-15** — `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` green.
      Emulator: export a selection, confirm the share sheet opens, save through Files, and
      re-open the file to check its shape. **The end-to-end check that means the most:**
      export from Android and import that file into the web client — it proves the two
      implementations still agree about a format that is a compatibility promise.
