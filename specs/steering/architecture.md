# Architecture — Android client

**Status:** binding
**Version:** 1.0
**Applies to:** `fuji-recipes-app` (Android)
**Sibling client:** `fuji-recipes-book` (Nuxt) — the platform's other client and, for now, the owner of the server

---

## 1. System shape

Two clients, one platform. The Nuxt project is not a thing this app copies — it is the
thing this app shares a database with.

```
┌──────────────────────┐          ┌──────────────────────┐
│  Nuxt PWA (browser)  │          │  Android app         │
│  fuji-recipes-book   │          │  fuji-recipes-app    │
└──────────┬───────────┘          └──────────┬───────────┘
           │  HTTPS                          │  HTTPS
           │  Access cookie                  │  Access service token
           ▼                                 ▼
      ┌────────────────────────────────────────────┐
      │  Cloudflare Access  (edge gate)            │
      └──────────────────────┬─────────────────────┘
                             ▼
      ┌────────────────────────────────────────────┐
      │  Cloudflare Worker — the Nuxt Nitro server │
      │  /api/recipes · /api/import · /api/export  │
      │  src/server/middleware/auth.ts verifies    │
      │  the Access JWT and fails closed           │
      └──────────────────────┬─────────────────────┘
                             ▼
                    ┌─────────────────┐
                    │  Cloudflare D1  │   one `recipes` table
                    └─────────────────┘

USB-C ── PTP ──► the camera.  Local to the phone. Never crosses the network.
```

**The server is not in this repository.** Every change to the HTTP API belongs to
`fuji-recipes-book`. If an Android feature needs a route that does not exist, the route is
specified there first and this repo waits for it. An Android-only endpoint is a smell — the
two clients are peers, and a shape only one of them understands will drift.

## 2. The seams that matter

| Seam | Owner | Rule |
|---|---|---|
| Recipe data shape | `fuji-recipes-book/specs/shared/recipe.schema.json` | Canonical. This repo never redefines it — it transcribes or points at it. |
| Field definitions (groups, labels, ranges, defaults, applicability) | `fuji-recipes-book/specs/shared/field-definitions.md` | Canonical **document**. Transcribed into `RecipeFields.kt`, exactly as the Nuxt repo transcribes it into `shared/recipe-fields.ts`. Two transcriptions of one document, never a transcription of a transcription. |
| Import/export file format | `fuji-recipes-book/specs/shared/recipe-format.spec.md` | Canonical. Not implemented in Android v1. |
| HTTP contract | `fuji-recipes-book/specs/contracts.md` | Canonical. This repo's features cite route + status codes from it. |
| Ordering arithmetic (`sortKey`) | `fuji-recipes-book/shared/ordering.ts` | Canonical behaviour; Android reimplements the comparator only. Key *arithmetic* stays server-side — Android never computes a `sortKey`. |
| Camera protocol decisions | this repo, `camera/` | Shared in spirit with the Nuxt `camera/` module; both derive from `field-definitions.md` §6. |

**Vendoring rule.** When a canonical file is copied into this repo (the film-simulation
table, the field definitions), the copy carries a header naming the source path and the
commit it was taken from, and a task in the owning feature's `04-tasks.md` re-checks it.
A silent copy is how two clients stop agreeing about what "Classic Negative" means.

## 3. Layers

```
┌───────────────────────────────────────────────┐
│  ui/           Compose screens + ViewModels   │
│                StateFlow<UiState> downward,   │
│                events upward. No I/O here.    │
├───────────────────────────────────────────────┤
│  data/repo/    RecipeRepository (interface)   │
│                NetworkRecipeRepository        │
│                  = ApiClient + SnapshotCache  │
├───────────────────────────────────────────────┤
│  core/net/     ApiClient, AccessInterceptor   │
│  core/cache/   SnapshotCache (one JSON file)  │
│  core/settings/ConnectionSettings (DataStore) │
├───────────────────────────────────────────────┤
│  camera/       CameraController (singleton)   │
│                buildWritePlan — PURE          │
└───────────────────────────────────────────────┘
```

Single activity, Compose Navigation, type-safe routes. The object graph is a hand-written
`AppContainer` on the `Application` — no DI framework (`tech-stack.md` §2). `CameraController`
lives there too, above the nav graph, so a connection survives navigation.

## 4. Storage — decided

**The server is the source of truth. The phone keeps a read-only snapshot.**

`GET /api/recipes` returns the whole library in one response (no pagination — the Nuxt
contract is explicit about this, and the design limit is ~2,000 recipes). The Android app
writes that response verbatim to one file and reads it back on next launch.

| | |
|---|---|
| **Reads** | Served from the snapshot immediately on launch, then refreshed from the network. Works with no signal. |
| **Writes** (create, edit, rating, tags, reorder, slot bookkeeping) | Require the network. There is no queue, no dirty flag, no last-write-wins merge. A write with no signal fails, says so, and keeps the user's input. |
| **Camera writes** | Entirely local. Work with no signal. `POST /:id/written` is best-effort bookkeeping afterwards and its failure never fails the camera write. |

Rejected: a Room mirror with a sync engine. It is a real distributed-systems problem —
queued mutations, conflict resolution, partial-failure recovery — built before the list
screen exists, for one user who is almost always the only writer. The snapshot gives the
field-use property that actually matters (the library is readable with no signal) for
roughly fifty lines.

<!-- ponytail: read-only snapshot, no write queue. Upgrade to Room + queued mutations
     if offline *editing* becomes a real need — the RecipeRepository interface is the
     seam that makes it a swap rather than a rewrite. -->

**What this buys later, and the only thing built for it now:** `RecipeRepository` is an
interface returning `Flow<List<Recipe>>`. ViewModels depend on the interface.
`NetworkRecipeRepository` is the only implementation. Nothing else is built in advance.

## 5. Authentication

The API is behind Cloudflare Access, and `src/server/middleware/auth.ts` in the Nuxt repo
verifies the token itself and **fails closed** when Access is unconfigured. A browser gets
through with the `CF_Authorization` cookie. A native client cannot.

**Android uses an Access service token.** The app sends two headers on every request:

```
CF-Access-Client-Id:     <token-id>.access
CF-Access-Client-Secret: <token-secret>
```

Access validates them at the edge, mints the JWT, and the Worker's existing verification
passes unchanged. **No server-side change is required** — the Access application needs a
Service Auth policy admitting the token, which is dashboard configuration, not code.

| Consequence | Handling |
|---|---|
| `/api/health` is ungated | So it does **not** prove the token works. Connection setup validates against `GET /api/recipes`, never health. |
| 403 `{ "error": "forbidden" }` | The token is missing, wrong, or expired. Route the user to connection setup with that stated. |
| 503 `{ "error": "access_unconfigured" }` | The *server's* Access config is broken, not the app's. Say so — do not blame the token. |
| The secret lives on the device | App-private DataStore, `android:allowBackup="false"`. <!-- ponytail: app-private storage only; Keystore-wrapped encryption if the threat model ever includes a rooted device or physical seizure. --> |

## 6. Where things live

```
app/src/main/java/dev/bondarenko/fujirecipes/
  FujiRecipesApp.kt              Application; owns the AppContainer
  core/AppContainer.kt           the object graph, by hand
  MainActivity.kt                single activity, NavHost
  core/
    net/ApiClient.kt             the six routes, OkHttp + kotlinx.serialization
    net/AccessInterceptor.kt     the two Access headers
    net/ApiResult.kt             Success | Failure(ApiError) — no exceptions across the seam
    net/ApiError.kt              the contract's error envelope, as a sealed type
    cache/SnapshotCache.kt       read/write the one JSON snapshot file
    settings/ConnectionSettings.kt   base URL + service token (DataStore)
    settings/ViewPreferences.kt      remembered filters and sort (DataStore)
  data/
    model/Recipe.kt              the wire shape, `extra` preserved
    fields/FilmSimulations.kt    transcribed from field-definitions.md §3
    fields/RecipeFields.kt       transcribed from field-definitions.md §4 (FEAT-002)
    repo/RecipeRepository.kt     interface
    repo/NetworkRecipeRepository.kt
  ui/
    theme/                       Color.kt, Type.kt, Shape.kt, FujiTheme.kt
    library/                     LibraryScreen, LibraryViewModel, LibraryView.kt (pure), cards
    common/                      shared composables
  camera/                        FEAT-003, FEAT-004

app/src/main/res/
  drawable/                      film-simulation swatch images (from the Nuxt repo)
  mipmap-anydpi-v26/             adaptive launcher icon
  font/                          Lora + Inter, bundled

app/src/test/java/…              pure-logic unit tests (JVM)
app/src/androidTest/java/…       Compose UI tests

specs/
  steering/                      this file and its siblings — read before any spec
  roadmap.md                     feature order and gates
  features/FEAT-XXX-slug/        01-functional.md, 02-schema.json, 03-behavior.feature, 04-tasks.md
```

## 7. Domain glossary

Use these words. They are the Nuxt repo's words, and a second vocabulary for one data model
is how two clients end up disagreeing in the UI.

| Term | Meaning | Not |
|---|---|---|
| **Recipe** | One stored parameter set with a name, rating, tags, notes | "preset", "profile", "look" |
| **Library** | All recipes | "collection", "gallery" |
| **Settings** | The `settings` object inside a recipe — the camera parameters | "parameters", "config" |
| **Film simulation** | The `filmSimulation` field; ids like `classic-negative` | "film sim" in code; the abbreviation is fine in UI copy |
| **Sensor generation** | `xtrans-iii` … `gfx`. **No longer a stored column** — dropped in D1 migration 0002; it survives inside `extra` on older rows | |
| **Slot** | A camera custom setting, C1–C7 | "bank", "preset slot" |
| **Manual order** | `sortKey` ascending, ties broken by `createdAt` ascending | "custom order", "user order" |
| **Snapshot** | The cached `GET /api/recipes` response on disk | "cache", "offline DB" |
| **Written** | `lastWrittenSlot` / `lastWrittenAt` — bookkeeping after a successful camera write | "synced", "applied" |

## 8. Known constraints

| # | Constraint | Consequence |
|---|---|---|
| C1 | The phone has one USB-C port and the camera occupies it | No cabled debugging during any test that matters. Wireless ADB is set up in FEAT-000, not discovered in FEAT-003. |
| C2 | The API is behind Cloudflare Access and fails closed | Every screen has a credible "the gate refused me" state. A blank list is never an acceptable rendering of 403. |
| C3 | The camera protocol is reverse-engineered and partly unverified | `buildWritePlan` is pure and testable with no hardware. Protocol features are scheduled last. |
| C4 | The field context is one-handed, outdoors, often with no signal | Primary actions in thumb reach. Reads work offline. Writes fail loudly rather than silently queueing. |
| C5 | The server is another repository | No Android feature may require a server change without that change being specified in `fuji-recipes-book` first. |
| C6 | `sensorGeneration` is no longer a column | It is not filterable and not listed. Anything that assumes the D1 row has it is wrong. |

## 9. Existing contracts

Before defining any shape, check these. Do not write a parallel copy.

| Contract | Location |
|---|---|
| HTTP routes, status codes, error envelope | `fuji-recipes-book/specs/contracts.md` |
| Recipe JSON Schema | `fuji-recipes-book/specs/shared/recipe.schema.json` |
| Field definitions | `fuji-recipes-book/specs/shared/field-definitions.md` |
| Export/import file format | `fuji-recipes-book/specs/shared/recipe-format.spec.md` |
| D1 table definition | `fuji-recipes-book/migrations/0001_initial_schema.sql`, `0002_drop_sensor_generation.sql` |
| Ordering rules | `fuji-recipes-book/specs/data-model.md` §3 |
| List selection pipeline (search → filter → sort) | `fuji-recipes-book/src/utils/library-view.ts` |

**Error envelope, in full** — every non-2xx from `/api` looks like this and the Android
error type mirrors it exactly:

```json
{ "error": "validation_failed", "message": "…",
  "fields": [ { "path": "settings.clarity", "message": "must be between -5 and 5" } ] }
```

| `error` | Status | Means |
|---|---|---|
| `forbidden` | 403 | Access refused the request — the app's credentials |
| `access_unconfigured` | 503 | The *server* has no gate configured — not the app's fault |
| `not_found` | 404 | With an `id` field |
| `validation_failed` | 422 | With a `fields` array |
| `id_exists` | 409 | Client-supplied id collided |
| `storage_unavailable` | 503 | D1 unreachable, retryable |
| `internal` | 500 | With a `requestId` |
