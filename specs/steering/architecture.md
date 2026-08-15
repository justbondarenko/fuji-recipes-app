# Architecture — Android client

**Status:** binding
**Version:** 1.0
**Applies to:** `fuji-recipes-app` (Android)
**Sibling project:** `fuji-recipes-book` (Nuxt) — the other client, and the owner of the shared data shapes

---

## 1. System shape

**This app is offline.** It talks to no server, holds no credentials, and does not declare
the `INTERNET` permission. The library is a file in app-private storage, and recipes move
between devices as export files that a person chooses to write and chooses to read.

```
┌──────────────────────┐                    ┌──────────────────────┐
│  Nuxt PWA (browser)  │                    │  Android app         │
│  fuji-recipes-book   │                    │  fuji-recipes-app    │
│  its own server + D1 │                    │  filesDir/library.json│
└──────────┬───────────┘                    └──────────┬───────────┘
           │                                           │
           └────────── .json / .zip export ────────────┘
                 written, shared, imported by hand

USB-C ── PTP ──► the camera.  Local to the phone, like everything else here.
```

**What the two clients still share is the data, not a database.** The recipe shape, the
field definitions and the export file format are canonical in `fuji-recipes-book` and
transcribed here; a file written by either one has to be readable by the other. That is the
whole of the relationship now.

**Why the Worker went.** The app existed to be usable in the field, and every write needed
a signal to succeed — which is exactly where there isn't one. Removing the server removed
the failure mode rather than handling it, and cost a sync story that one user with one
library did not need.

## 2. The seams that matter

| Seam | Owner | Rule |
|---|---|---|
| Recipe data shape | `fuji-recipes-book/specs/shared/recipe.schema.json` | Canonical. This repo never redefines it — it transcribes or points at it. |
| Field definitions (groups, labels, ranges, defaults, applicability) | `fuji-recipes-book/specs/shared/field-definitions.md` | Canonical **document**. Transcribed into `RecipeFields.kt`, exactly as the Nuxt repo transcribes it into `shared/recipe-fields.ts`. Two transcriptions of one document, never a transcription of a transcription. |
| Import/export file format | `fuji-recipes-book/specs/shared/recipe-format.spec.md` | Canonical, and now the **only** interchange between the two clients. A file this app writes must be one the web client reads, and the reverse. |
| Ordering (`sortKey`) | `fuji-recipes-book/shared/ordering.ts` | Canonical behaviour. Android reimplements the comparator, and — since there is no server — assigns keys too: a new recipe is appended at `max + 1`. |
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
│                LocalRecipeRepository          │
│                  = LibraryStore + bookkeeping │
├───────────────────────────────────────────────┤
│  core/store/   LibraryStore (one JSON file)   │
│  core/result/  LibraryResult / LibraryError   │
│  core/settings/ViewPreferences (DataStore)    │
├───────────────────────────────────────────────┤
│  camera/       CameraController (singleton)   │
│                buildWritePlan — PURE          │
└───────────────────────────────────────────────┘
```

Single activity, Compose Navigation, type-safe routes. The object graph is a hand-written
`AppContainer` on the `Application` — no DI framework (`tech-stack.md` §2). `CameraController`
lives there too, above the nav graph, so a connection survives navigation.

## 4. Storage — decided

**The device is the source of truth.** `filesDir/library.json` holds every recipe, and
`LibraryStore` reads and writes it whole. There is no second copy anywhere.

| | |
|---|---|
| **Reads** | One file read on launch, then the library is in memory: filtered, sorted and searched there. |
| **Writes** (create, edit, rating, tags, import) | Rewrite the whole file through a temporary and rename it into place. A write that fails changes nothing and says so. |
| **Camera writes** | Unchanged, and always were local: USB-C and PTP, nothing else involved. |

Because the store *is* the library, two rules that a cache did not need:

1. **A file that will not parse is not an empty library.** `StoreRead` distinguishes absent
   from unreadable, and the list screen renders the second as a failure with the reason.
2. **Nothing is written on top of a library that could not be read.** Every mutation goes
   through `editable()`, which refuses while that error stands. A file that will not parse
   today may still be recoverable; one that has been overwritten is not.

Rejected: Room. Nothing queries the library — it is loaded whole and filtered in memory —
so a relational store would be a schema, a DAO and a migration story with no reader.

**Backups are the user's, by hand.** Export writes a `.json` or a `.zip`; import reads one
back, reviewing every entry against what is already stored before anything is written. That
is the sync story, and it is deliberate rather than a gap: nothing leaves the phone unless
somebody chooses to send it.

<!-- ponytail: one JSON file, rewritten whole. If a library ever grows past a few thousand
     recipes, the RecipeRepository interface is the seam that makes a real store a swap
     rather than a rewrite. -->

## 5. What leaves the device

**Nothing, unless the user sends it.** There is no account, no telemetry, no sync, and no
`INTERNET` permission — so this is enforced by the manifest rather than promised by the code.

| Path out | How |
|---|---|
| Export | A file built on the phone and handed to the OS share sheet through a `FileProvider`, scoped to one cache directory and granted read one file at a time |
| Import | A file the user picks through the Storage Access Framework. Reviewed entry by entry before anything is written |
| Camera | USB-C, PTP, one slot at a time |

| Consequence | Handling |
|---|---|
| The library is the only copy | Export is not a nice-to-have; it is the backup story, and Settings says so in as many words |
| `android:allowBackup="false"` | Kept. The library is personal data, and shipping it to a cloud backup by default is not a decision to make silently <!-- ponytail: app-private storage only; revisit if the threat model ever includes a rooted device or physical seizure --> |

## 6. Where things live

```
app/src/main/java/dev/bondarenko/fujirecipes/
  FujiRecipesApp.kt              Application; owns the AppContainer
  core/AppContainer.kt           the object graph, by hand
  MainActivity.kt                single activity, NavHost
  core/
    store/LibraryStore.kt        read/write the one JSON library file
    result/LibraryResult.kt      Success | Failure(LibraryError) — no exceptions across the seam
    settings/ViewPreferences.kt      remembered filters and sort (DataStore)
  data/
    model/Recipe.kt              the stored shape, `extra` preserved
    fields/FilmSimulations.kt    transcribed from field-definitions.md §3
    fields/RecipeFields.kt       transcribed from field-definitions.md §4 (FEAT-002)
    library/LibraryView.kt       search, filter, sort — PURE, no Compose, no Android
    repo/RecipeRepository.kt     interface
    repo/LocalRecipeRepository.kt
  ui/
    theme/                       Color.kt, Type.kt, Shape.kt, FujiTheme.kt
    library/                     LibraryScreen, LibraryViewModel, cards
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
| **Store** | `filesDir/library.json` — the library itself, not a cache of one | "snapshot", "cache", "offline DB" |
| **Written** | The act of sending a recipe to a camera slot. The Android client does **not** record when this happened: `lastWrittenSlot` / `lastWrittenAt` are the web client's bookkeeping and pass through this app untouched, inside `extra` | "synced", "applied" |

## 8. Known constraints

| # | Constraint | Consequence |
|---|---|---|
| C1 | The phone has one USB-C port and the camera occupies it | No cabled debugging during any test that matters. Wireless ADB is set up in FEAT-000, not discovered in FEAT-003. |
| C2 | The library file is the only copy of the user's recipes | A file that will not parse is reported as such and never written over, and export is presented as the backup it is. A blank list is never an acceptable rendering of "could not read". |
| C3 | The camera protocol is reverse-engineered and partly unverified | `buildWritePlan` is pure and testable with no hardware. Protocol features are scheduled last. |
| C4 | The field context is one-handed, outdoors, often with no signal | Primary actions in thumb reach. Nothing on any path needs a network, which is what removing the server bought. |
| C5 | The two clients meet only in files | A change to the export format is specified in `fuji-recipes-book` first, because a file this app writes has to stay readable there. |
| C6 | `sensorGeneration` is no longer a stored field | It is not filterable and not listed. It survives inside `extra` on older recipes and nowhere else. |

## 9. Existing contracts

Before defining any shape, check these. Do not write a parallel copy.

| Contract | Location |
|---|---|
| Recipe JSON Schema | `fuji-recipes-book/specs/shared/recipe.schema.json` |
| Field definitions | `fuji-recipes-book/specs/shared/field-definitions.md` |
| Export/import file format | `fuji-recipes-book/specs/shared/recipe-format.spec.md` |
| Ordering rules | `fuji-recipes-book/specs/data-model.md` §3 |
| List selection pipeline (search → filter → sort) | `fuji-recipes-book/src/utils/library-view.ts` |

**The HTTP contract is no longer one of these.** `fuji-recipes-book/specs/contracts.md`
describes that project's own server; nothing in this repo calls it, and a feature here may
not cite a route.

**Failures, in full.** `LibraryError` is the whole set, and it is short because a store on
the same filesystem cannot refuse your credentials or be unreachable:

| Case | Means | Remedy offered |
|---|---|---|
| `Unreadable` | The library file is there and did not parse | Restore an export. Nothing is written meanwhile |
| `Storage` | The library could not be written; nothing changed | Try again — usually the device is out of space |
| `NotFound` | The recipe an action names is not in the library | Nothing to do; it is already gone |
| `Invalid` | A recipe with a value outside what the field table allows, with the failing paths | Fix the field named |
