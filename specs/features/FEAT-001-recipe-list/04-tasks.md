# FEAT-001 — tasks

Ordered. Dependencies come first. Each item is one logical commit.
Paths are relative to `app/src/main/java/dev/bondarenko/fujirecipes/` unless stated.

**Prerequisite:** FEAT-000 (project, theme, icon, nav shell) is complete.
**Blocking dependency:** T-12 onward needs a Cloudflare Access service token whose Service
Auth policy admits it — `specs/roadmap.md` §4. T-01 to T-11 are testable without it.

---

## Data and field source

- [x] **T-01** — `data/fields/FilmSimulations.kt`. Transcribe the 20-row table from
      `steering/design-system.md` §3 (`id`, `label`, `swatch`, `monochrome`,
      `minGeneration`). Header names the source document and the commit it was read at
      (`coding-standards.md` P3). Lookup by id returns null for an unknown id — it does not
      throw.
- [x] **T-02** — Copy the 20 `*.webp` swatch images from
      `fuji-recipes-book/src/public/film-simulations/` into `app/src/main/res/drawable/`,
      renamed to valid resource names (`classic_negative.webp`). One mapping function from
      simulation id to drawable resource, returning null when absent.
- [x] **T-03** — `data/model/Recipe.kt`. `@Serializable`, `ignoreUnknownKeys = true`, with
      `settings` and unrecognised top-level keys held as `JsonObject` so nothing is dropped
      (`02-schema.json`; `coding-standards.md` P2). Test: a recipe carrying an unknown
      property round-trips through decode and encode with that property intact.

## Network

- [x] **T-04** — `core/net/ApiError.kt`. Sealed type mirroring the contract's envelope:
      `Forbidden`, `AccessUnconfigured(missing)`, `NotFound(id)`, `ValidationFailed(fields)`,
      `IdExists`, `StorageUnavailable`, `Internal(requestId)`, `Network(cause)`,
      `Malformed(cause)`. Source: `steering/architecture.md` §9.
- [x] **T-05** — `core/net/ApiResult.kt`. `Success<T>` / `Failure(ApiError)`. No exceptions
      cross the repository seam.
- [x] **T-06** — `core/net/AccessInterceptor.kt`. Adds `CF-Access-Client-Id` and
      `CF-Access-Client-Secret` when both are configured; adds neither when either is
      missing. Test with `MockWebServer`: headers present, and absent when unconfigured.
- [x] **T-07** — `core/net/ApiClient.kt`. `listRecipes()` against `GET /api/recipes` on
      OkHttp + `kotlinx.serialization`. Maps every documented status and `error` code to the
      T-04 type. Tests with `MockWebServer` for 200, 403, 404, 422, 500, 503
      (`storage_unavailable`), 503 (`access_unconfigured`), a socket failure, and an
      unparseable body — nine cases, one per branch (`coding-standards.md` P9).

## Settings and cache

- [x] **T-08** — `core/settings/ConnectionSettings.kt`. DataStore-backed base URL, client id,
      client secret, exposed as a `Flow`. Base URL is normalised (trailing slash trimmed,
      scheme required). `android:allowBackup="false"` set in the manifest in this commit.
- [x] **T-09** — `core/settings/ViewPreferences.kt`. DataStore-backed sort, min rating, tags,
      simulations. Reading applies the repair rules of `01-functional.md` §23 — unknown
      simulation dropped, rating clamped, unknown sort → default, unknown tag kept. Pure
      repair function, tested directly with the four cases.
- [x] **T-10** — `core/cache/SnapshotCache.kt`. Read and write the envelope in
      `02-schema.json` to one app-private file. Writes the response body **verbatim**.
      Discards a snapshot whose `snapshotVersion` is unrecognised or whose `baseUrl` differs
      from the configured one. Tests: round trip, version mismatch discarded, base-URL
      mismatch discarded, unreadable file behaves as no snapshot.
- [x] **T-11** — `data/repo/RecipeRepository.kt` (interface, `Flow<List<Recipe>>`) and
      `NetworkRecipeRepository.kt`: emit the snapshot first if present, then fetch; on
      success write the snapshot and emit; on failure emit the cached list alongside the
      error rather than replacing it. Test: a failed refresh does not clear a cached list
      and does not overwrite the snapshot.

## Connection screen

- [x] **T-12** — `ui/connection/ConnectionScreen.kt` + `ConnectionViewModel.kt`. Three
      inputs, masked secret with a reveal toggle, save, and **Test connection** issuing
      `GET /api/recipes` — never `/api/health`. Outcome copy is per `ApiError` case
      (`coding-standards.md` P5). Include the regression test that a deployment with an
      ungated health route and a bad token still reports the credentials as refused.
- [x] **T-13** — Route the app to the connection screen on launch when settings are absent,
      and expose it from the list's overflow afterwards.
      *Only the first half shipped, and this was ticked anyway. Once credentials were saved
      the connection screen was reachable only by provoking a 403. Closed by **FEAT-004**,
      which puts it under the More tab rather than in a list overflow.*

## List — pure logic first

- [x] **T-14** — `data/library/LibraryView.kt`. Pure, no Compose, no Android imports
      (`coding-standards.md` P7). Port of `fuji-recipes-book/src/utils/library-view.ts`:
      `matchesSearch`, `matchesFilters`, `compareBy`, `selectRecipes`, plus the manual-order
      comparator (`sortKey` asc, `createdAt` asc) from `fuji-recipes-book/shared/ordering.ts`.
      The file already exists — T-09 put `SortId`, `LibraryFilters` and the repair rules in
      it — so this adds the pipeline to it rather than creating it.
- [x] **T-15** — Parity test suite for T-14, driven by plain lists. One test per scenario in
      the Search, Filters and Sorting sections of `03-behavior.feature`. **This is the
      highest-value suite in the feature** — it is what makes "parity with the web client" a
      checked claim rather than an asserted one.

## List — UI

- [x] **T-16** — `ui/library/FilmSimBadge.kt`. Circle, 1dp ring (`black 10%` light /
      `white 20%` dark), swatch fill, image over it with `ContentScale.Crop`. Unknown id →
      `#9CA3AF` fill and the raw id as the label. Preview for a known id, an image-less id,
      and an unknown id.
- [x] **T-17** — `ui/library/RecipeCard.kt` to the anatomy in `steering/design-system.md` §7.
      Rating pill omitted at 0; tag row omitted when empty, capped at 5 then `+n`. Overflow
      button is a 48dp sibling of the click target, not nested.
      *(Amended after FEAT-002: the last-written line this task originally specified was
      removed — the app does not track slot bookkeeping, `roadmap.md` §3.)*
- [x] **T-18** — `ui/library/LibraryUiState.kt` and `LibraryViewModel.kt`. One `UiState`
      exposed as `StateFlow`; search held in the ViewModel only, filters and sort round-trip
      through `ViewPreferences`. Turbine test: search state is not persisted, filter and
      sort state is.
- [x] **T-19** — `ui/library/LibraryScreen.kt`. `LazyColumn`, the `n recipes` /
      `n of m recipes` header, pull-to-refresh, `WindowInsets.safeDrawing`. Screen composable
      takes state and lambdas; a `LibraryRoute` does the `viewModel(factory = …)` wiring
      against `AppContainer`.
- [x] **T-20** — Search field and the filter/sort surface: tag chips, minimum-rating
      selector, film-simulation selector, three sort options, filter badge counting axes.
- [x] **T-21** — Every state from `01-functional.md` §24–§33 as a distinct rendering, each
      with a `@Preview`: loading-cold (skeletons), loading-warm, empty library, no matches,
      offline-with-snapshot banner, offline-no-snapshot, `forbidden`, `access_unconfigured`,
      `storage_unavailable`, `internal` with request id. Strings in `strings.xml`.
- [x] **T-22** — Card tap navigates to the recipe editor route. Until FEAT-002 lands this is
      a placeholder destination; the navigation contract is what this task fixes.

## Verification

**Verified on an emulator against a local mock of `/api/recipes`** (10 recipes, one carrying
an unknown `filmSimulation` and an unknown top-level key): the library lists, search narrows
to `3 of 10` on "portra", "Portra 2" sorts before "Portra 10", the unknown simulation draws
the fallback swatch with its raw id, tag overflow shows `+1`, and the written line renders
the written line as it then existed. Two fixes came out of it — see the notes below.

- [ ] **T-23** — Compose UI tests covering the state scenarios in `03-behavior.feature`:
      each of the ten states renders its own distinguishable content, and 403 in particular
      does **not** render as an empty library.
- [ ] **T-24** — Forward-compatibility test: a recipe with an unknown top-level property and
      an unknown `filmSimulation` id survives fetch → snapshot → read → render, with the
      property intact and the card drawn with the fallback swatch.
- [ ] **T-25** — Manual check, recorded in the PR: install on the real device, configure the
      real service token, load the real library, enable airplane mode, cold-start, confirm
      the library still lists and the cached banner names the fetch time.
      *Manual because it needs the production Access deployment and a genuine cold start;
      `MockWebServer` cannot prove either.*
- [ ] **T-26** — `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` green,
      and every scenario in `03-behavior.feature` maps to a test or to T-25.

---

## Notes from implementation

Two things the on-device run turned up that the unit tests could not:

- **Cleartext HTTP is blocked**, so a local mock is unreachable without a network security
  config. Added as `app/src/debug/` only — release keeps the default block, and the real
  deployment is HTTPS behind Access, so this is a development affordance and not a
  loosening of the shipped app.
- **The centre-docked FAB covered the last card.** The list now carries an 88dp bottom
  content inset. Unit tests cannot see this and neither can a preview that is taller than
  the content.

Still open in this feature: T-23 (Compose UI state tests), T-24 (the forward-compatibility
round trip as an automated test rather than the manual observation above), T-25 (the manual
check against the real deployment, which needs the Access service token).
