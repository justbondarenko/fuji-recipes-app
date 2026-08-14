# FEAT-001: Recipe list

**Tracker issue:** —
**Source PRD:** `PRD.md` §7.1, with §4.1 superseded by `specs/steering/architecture.md` §4
**Parity target:** `fuji-recipes-book/src/pages/index.vue`, `src/utils/library-view.ts`, `src/components/recipe/RecipeCard.vue`
**Status:** Ready

---

## Problem

The recipe library exists on the server and is only reachable from a browser. The phone is
the field device — it goes in the bag with the camera — and today it can only see the
library through a PWA that needs a browser session and a signal. This feature puts the
library on the phone: readable at a glance, searchable, and present with no connection.

It is also the stage where the Android client first talks to the platform at all, so it
carries the connection setup, the API client, and the offline snapshot that every later
feature reuses.

## User stories

- As the photographer, I can point the app at my library once and have it stay pointed there, so that opening it is never a setup task.
- As the photographer, I can see my whole library as a scrollable list showing each recipe's name, film simulation, rating and tags, so that I can identify a recipe without opening it.
- As the photographer, I can type part of a name or tag and watch the list narrow, so that I can find a recipe in seconds with a camera in my other hand.
- As the photographer, I can filter by tag, by minimum rating, and by film simulation, so that I can narrow a large library along the axes I actually think in.
- As the photographer, I can sort by name, rating, or how recently a recipe changed, so that the list is ordered the way the current task needs.
- As the photographer, I can reopen the app and find my filters and sort as I left them, so that a chosen view is not re-chosen every launch.
- As the photographer, I can open the app with no signal and still read my library, so that the app is useful in the places I actually shoot.
- As the photographer, I can see plainly when the app cannot reach the library and what to do about it, so that a failure is never a blank screen.

## Scope

### Connection setup

1. A **Connection** screen with three inputs: **Base URL**, **Access client id**, **Access
   client secret**. Secret is masked with a reveal toggle.
2. A **Test connection** action that issues `GET /api/recipes` and reports the outcome
   using the contract's `error` code. It must **not** use `GET /api/health` — that route is
   ungated (`fuji-recipes-book/src/server/middleware/auth.ts`, `UNGATED_PATHS`), so it
   answers 200 with no credentials at all and would report a broken token as working.
3. Settings persist to app-private DataStore. `android:allowBackup="false"`.
4. On first launch with nothing configured, the app opens here rather than on an empty list.
5. Reachable afterwards from the list's overflow menu.

### The list

6. `GET /api/recipes` on entering the list, and on pull-to-refresh.
7. On success, the response body is written **verbatim** to the snapshot file (§02-schema).
8. On launch, the snapshot renders first if present, then the network refresh replaces it.
9. `LazyColumn` of recipe cards. Card anatomy, spacing and omission rules:
   `steering/design-system.md` §7.
10. A header row showing `n recipes` — always the library's total. The narrowed count lives
    in the toolbar's summary bar (§12b) so the two do not say the same thing twice, and so
    the header stays a stable fact rather than a number that changes as you type.
11. Tapping a card opens the recipe editor (FEAT-002). Until FEAT-002 exists, it opens a
    placeholder route. No read-only detail screen in v1.
12. Overflow `⋮` on each card is present but its only enabled item in this feature is
    **Copy name**; the rest arrive with FEAT-002.

### Toolbar layout — compact, expands on demand

12a. The controls occupy **two rows at rest**, matching the web client's `LibraryToolbar.vue`:
    a search field, then one line holding a **Filters** button (with a badge counting active
    axes) and a **sort** menu. The filter controls themselves live in a bottom sheet opened
    from that button.
12b. A **"Showing n of m · Clear all"** bar appears only while the list is narrowed. A
    restored filter that is invisible is how a library appears to have lost recipes
    overnight.
12c. Rating, film simulation and tag controls are **never permanently on screen**. They are
    set rarely and read never, and the list screen's job is showing recipes.

### Search, filter, sort — parity with `library-view.ts`

13. **Search** over name and tags. Whitespace-separated terms; **every** term must match
    somewhere; a term may land on the name or on any one tag; substring, not prefix;
    case- and accent-folded. Empty search matches everything.
14. **Filter by tags** — conjunctive. All selected tags must be present on a recipe.
15. **Filter by minimum rating** — `0` means any, and includes unrated recipes.
16. **Filter by film simulation** — disjunctive across selected simulations.
17. **Sort**, offering exactly three options in a menu: `Name A–Z` (default),
    `Rating high–low`, `Recently updated`.
18. Name sort is locale-aware with `numeric` collation and base sensitivity, so
    "Portra 2" precedes "Portra 10" and case does not split adjacent names.
19. **Every comparator falls back to manual order** — `sortKey` ascending, ties broken by
    `createdAt` ascending. Equal ratings and same-day edits keep the arrangement the user
    chose.
20. Pipeline order is search → filter → sort, and it never mutates the source list.
21. A filter badge shows how many *axes* are set (0–3), not how many values.
22. Filters and sort persist across launches. **The search text does not** — it is a way of
    getting to one recipe, not a setting.
23. A stored view is repaired field by field, not discarded wholesale: an unknown film
    simulation id is dropped, a rating out of range is clamped, a sort id the app no longer
    offers falls back to the default, and a tag that currently matches nothing is **kept**
    (it may be mid-import on the other client).

### States

24. **Loading, nothing cached** — five skeleton cards, so the layout does not jump.
25. **Loading, snapshot present** — the snapshot renders immediately; refresh is a
    non-blocking indicator.
26. **Empty library** — headline, one line of copy, and a **Create a recipe** action.
27. **Narrowed to nothing** — states how many recipes are in the library and offers
    **Clear search and filters**.
28. **Offline with a snapshot** — the list renders normally. Freshness is reported by a
    quiet **"Last updated <date>, <time>"** line at the *end* of the list, in muted body
    text with no container.

    Deliberately not a banner at the top: a cached library is the normal state in the field,
    not a warning, and colouring it like one trains the reader to ignore colour that does
    mean something. The same line shows after a successful refresh — one timestamp, always
    present, saying how fresh the library is rather than announcing a failure mode.
29. **Offline with no snapshot** — an error state with a retry, naming the network as the
    problem.
30. **Refused (403 `forbidden`)** — names the credentials as the problem and offers a route
    to the Connection screen. Never rendered as an empty library.
31. **Server gate unconfigured (503 `access_unconfigured`)** — says the *server's* Access
    configuration is incomplete and names the missing variable the body reports. Must not
    blame the token or the database.
32. **Storage unreachable (503 `storage_unavailable`)** — says the library is stored, not
    lost, and offers retry.
33. **Unexpected (500 `internal`)** — shows the `requestId` so a log can be found.

### Local preferences

**No schema file for these** — they never leave the process, so a JSON Schema would be a
type declaration in a more verbose format (`sdd-spec-authoring` Step 4). DataStore keys:

| Key | Type | Default |
|---|---|---|
| `connection.base_url` | String | *(unset)* |
| `connection.client_id` | String | *(unset)* |
| `connection.client_secret` | String | *(unset)* |
| `view.sort` | String, one of `name` / `rating` / `updated` | `name` |
| `view.min_rating` | Int, 0–5 | `0` |
| `view.tags` | String set | empty |
| `view.simulations` | String set | empty |

## Out of scope

- Creating, editing, duplicating or deleting a recipe (FEAT-002)
- A read-only recipe detail screen (v2 — v1 taps straight into the editor)
- Reordering / editing manual order, and a "manual" sort option (v2; the web client offers neither)
- Sorting by last written (v2; the comparator exists in the web client but is not offered)
- Filtering by sensor generation (won't do — the D1 column was dropped in migration 0002)
- Export and import (v2)
- Anything camera-related, including the connection indicator (FEAT-003)
- Offline **writes**, a mutation queue, or conflict resolution (v2 — `architecture.md` §4)
- Pagination (won't do — the contract returns the whole library and the design limit is ~2,000)
- Multi-select and bulk actions (v2)
- Tablet or foldable layouts (won't do in v1 — build responsively, do not design for it)

## Constraints

- **The server is not modified.** Every route used here already exists and is specified in
  `fuji-recipes-book/specs/contracts.md`.
- **`GET /api/recipes` returns the whole library, unpaginated**, ordered by `sortKey`
  ascending with ties already broken by `createdAt`. The client re-applies the comparator
  anyway, because its own sorts fall back to it.
- **Unknown fields must survive.** The snapshot is stored verbatim and the parsed model
  keeps unrecognised top-level keys, because the web client's `extra` column exists to let a
  newer version write fields this build does not know (`data-model.md` §1).
- **`sensorGeneration` is not a column.** It may appear on older rows via `extra`. Nothing
  in this feature may require it.
- Selection logic is pure and lives outside Compose (`coding-standards.md` P7), so parity
  with `library-view.ts` is testable rather than asserted.
- Search must stay responsive while typing on a library at the design limit. Filter in
  memory; do not re-request.
- Minimum touch target 48dp; the card's overflow button is a sibling of the card's click
  target, never nested inside it.

## Open questions

None. The three that would have blocked this were settled before drafting:

- API auth → Cloudflare Access **service token**, no server change (`architecture.md` §5)
- Offline → **read-only snapshot**, network required for writes (`architecture.md` §4)
- Palette → **the shipped Nuxt theme**, stone + amber (`steering/design-system.md`)
