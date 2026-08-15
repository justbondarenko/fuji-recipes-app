# FEAT-007: Import from camera

**Tracker issue:** —
**Source PRD:** `PRD.md` §7.7 (import review), §14.3 (reading slots back)
**Canonical contract:** `fuji-recipes-book/specs/contracts.md` — `POST /api/import`
**Parity target:** `fuji-recipes-book/camera/read-slot.ts`, `src/utils/import-review.ts`,
`src/utils/recipe-config-equal.ts`, `src/pages/more/import.vue`
**Status:** Ready

---

## Problem

The camera link runs one way. FEAT-006 puts a recipe from the library into a custom slot, but
the recipes already **on the body** — tuned by hand on the camera, written from the web
client, or set before this app existed — cannot get into the library at all. Getting one in
means reading the camera's menu and typing twenty-odd values into the form without a mistake.

This closes the loop. **More → Import** reads C1–C7 off the body, decodes each slot into a
recipe, shows what it found against what is already in the library, and imports the ones the
photographer chooses.

It also settles something FEAT-006 left open. The property codes are confirmed to *exist* on
this generation, but nothing yet proves `0xD19D` is highlight tone rather than something else
that also accepts −20. Reading a slot back and comparing it against the camera's own menu is
the check that crosses from "the camera stored what I sent" to "the camera stored what I
meant".

## User stories

- As the photographer, I can pull the recipes already on my camera into my library, so that a look I tuned on the body is not trapped there.
- As the photographer, I am told which of them I already have, so that importing does not fill my library with duplicates of itself.
- As the photographer, I can choose which slots to take, so that an experiment I do not want to keep stays on the camera.
- As the photographer, I can see what each slot actually contains before importing it, so that I am choosing between recipes rather than between slot numbers.
- As the photographer, I am told plainly that reading works without a signal but saving does not, so that a failed import is never a surprise.

## Scope

### Reading

1. A **Import** entry on the More screen, opening a dedicated screen.
2. **Read slots** reads C1 through C7 in order, reporting progress per slot.
3. Per slot: select it (`0xD18C`), let the body settle, read the name (`0xD18D`), then read
   each mapped settings property once and decode it.
4. Several field ids share one property — `grainEffect` and `grainSize` are both `0xD195` —
   so properties are read **once each and cached by code**, not once per field.
5. A slot that decodes to **no settings at all is dropped**, not imported as an empty recipe.
   An unconfigured slot is not a recipe.
6. A slot with no name becomes `Slot C3`. A camera slot need not be named, and a nameless
   recipe is worse than a plainly-labelled one.
7. A property the tables cannot name is **omitted from the recipe**, not guessed. One unknown
   value costs one field rather than the slot.
8. Reading works with **no network**. The camera is local (`architecture.md` §1).

### The review

9. Every slot read gets a row: slot number, the name, the film simulation, and a status.
10. Three statuses, and no more:
    - **New** — nothing in the library matches.
    - **Already in your library** — an existing recipe has the **exact same camera
      configuration**. This is the duplicate detection: settings are compared after
      normalisation, field by field, over the fields that apply to that simulation and body.
    - **Name already used** — a *different* recipe already has this name. A warning, not a
      problem: names are not unique, and the import proceeds as a second recipe.
11. Duplicates are also detected **within the batch**, so two identical slots do not both
    import.
12. New and name-warning rows start **selected**. Duplicates start **deselected**, naming the
    recipe they match. Either can be toggled — a deliberate second copy is the photographer's
    call.
13. **There is no "replace".** Identical settings are what made a row a duplicate, so
    replacing an existing recipe with it would change nothing.

### Importing

14. `POST /api/import` with the selected recipes. Atomic — a failure rolls the whole import
    back (`SF-016`), which is why this is one request rather than one per recipe.
15. **No recipe carries an id.** `coding-standards.md` P2: the phone never invents an id the
    server would assign. `shared/schemas/recipe.ts` makes `id` optional and
    `src/server/api/import.post.ts` assigns one when it is absent. The web client generates a
    UUID per slot only to satisfy its own client-side schema check.
16. Consequence of §15: an imported slot **cannot collide by id**, so the contract's
    `resolutions` mechanism is unreachable here and the request carries `resolutions: {}`.
17. Importing **requires the network** (`architecture.md` §4), and the failure says so in
    those terms: reading worked without a signal, saving will not. There is deliberately **no
    pre-flight connectivity check** — `ConnectivityManager` reports a captive portal as
    connected, and a button disabled on a wrong answer is worse than a clear failure with the
    review still on screen and Retry available.
18. On success: how many were imported, and the library refreshes.
19. On failure: the contract's error, named per `coding-standards.md` P5. Nothing was written.

## Out of scope

| Deferred | To | Why |
|---|---|---|
| Importing from a **file** (JSON / ZIP) | v2 | `roadmap.md` §3 — the web client is the backup surface. The screen is shaped so a file source slots in beside the camera one without rework. |
| Exporting to a file | v2 | Same. |
| Writing back the recipes just imported | — | Not a thing anyone wants: they are already on the camera. |
| Reading the seven per-slot settings no recipe models | won't do | Image size, quality, smooth skin, long-exposure NR, colour space and two unidentified constants. None is a field, so there is nothing to import them into. |

## Error surfaces

| Failure | Surface | Message names |
|---|---|---|
| No camera connected | The screen offers Connect rather than Read | That a camera is needed |
| The body refuses one slot's properties | That slot is dropped from the results | Nothing — it is reported as a count of slots read |
| The read fails as a whole | The screen, with Retry | That the camera stopped answering |
| Every slot is empty | The screen, plainly | That the camera has no custom recipes saved |
| Every slot is already in the library | The review, with all rows deselected | That nothing is new, and the rows are still there to override |
| No network at import | The screen, with the review kept and Retry offered | That saving needs a connection and reading did not |
| The server rejects the import | The screen, with Retry | The contract's `error` code, and that nothing was written |
