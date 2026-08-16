# FEAT-013: Offline library

**Tracker issue:** —
**Source PRD:** `PRD.md` §4.1 (data storage), C2 (export is the backup guarantee)
**Supersedes in part:** FEAT-001 (connection setup, API client, snapshot cache), FEAT-004 (connection card)
**Status:** Ready

---

## Problem

The library lived on a Cloudflare Worker behind an Access service token, and the phone kept
a read-only snapshot of it. That arrangement bought one thing — a second client sharing one
database — and charged for it on every screen:

- **Every write needed a signal.** Create, edit, rating, tag, import: all of them failed
  where the app is actually used, which is outdoors with a camera in one hand.
- **Setup came before use.** A first launch showed a form asking for a base URL, a client id
  and a client secret before it would show a recipe.
- **A whole error vocabulary existed for failures the user could not fix** — a refused token,
  a server with no gate configured, a database that was not answering, a captive portal
  eating the response.

The owner is one photographer with one library. The sync that cost all of that is not a
sync problem they have.

**This feature removes the server from this app.** The library becomes a file on the device,
and recipes move between devices as export files that a person chooses to write and chooses
to read — which is the mechanism FEAT-008 and FEAT-012 already shipped.

## User stories

- As the photographer, I can open the app and see my recipes immediately, so that there is no setup standing between me and the thing I installed it for.
- As the photographer, I can create, edit, rate, tag and delete recipes with no signal, so that the app works where I use it.
- As the photographer, I can move my library to a new phone by exporting a file and importing it, so that changing devices is something I control.
- As the photographer, I know my recipes stay on my phone, so that there is no account, no token and no service to trust.

## Scope

### What is removed

1. `core/net/` entirely — the HTTP client, the Access interceptor, and the `ApiResult` /
   `ApiError` pair that mirrored the server's error envelope.
2. `core/settings/ConnectionSettings.kt` and the connection screen. There is nothing to
   configure, so there is no screen for it, and none in Settings either.
3. `core/cache/SnapshotCache.kt` and `NetworkRecipeRepository`.
4. `okhttp` and `mockwebserver` from the build, and the debug network-security config.
5. **`android.permission.INTERNET` and `ACCESS_NETWORK_STATE` from the manifest.** This is
   the load-bearing one: it turns "your recipes stay on this phone" from a claim the code
   makes into one the platform enforces.

### What replaces it

6. `LibraryStore` — `filesDir/library.json`, holding an envelope of `libraryVersion`,
   `updatedAt` and `recipes`. Read whole, written whole through a temporary file that is
   renamed into place.
7. `LocalRecipeRepository` — the same `RecipeRepository` interface every ViewModel already
   depended on, doing on the device what the Worker used to do: assigning ids, `sortKey`,
   `createdAt` and `updatedAt`, validating against the one field table, and deciding import
   collisions.
8. `LibraryResult` / `LibraryError` — four failure cases where there were eleven, because a
   store on the same filesystem cannot refuse your credentials or be unreachable.

### The two rules a store has that a cache did not

9. **A file that will not parse is not an empty library.** The store answers "absent" and
   "unreadable" differently, and the list screen renders the second as a named failure with
   the reason and a way to retry. Rendering it as "No recipes yet" would tell someone their
   library was gone when it is sitting on the disk.
10. **Nothing is written on top of a library that could not be read.** Every mutation goes
    through a guard that refuses while that error stands. A file that will not parse today
    may still be recoverable; one that has been overwritten is not.

### Identity and order, now assigned here

11. A new recipe gets a random UUID. Not a counter: ids from two devices meet the moment
    someone imports an export, and a counter would collide there.
12. `sortKey` is `max + 1` — appended at the end, which is the order an import is reviewed
    in (SF-009).
13. An update is a partial body applied over what is stored, with `id`, `sortKey` and
    `createdAt` forced back afterwards. Keys this build does not model survive the round
    trip, as they always had to (`coding-standards.md` P2).
14. An import keeps an id the file carried when the library does not already hold it — that
    is what makes an export a **backup** rather than a copy. `replace` overwrites in place
    and keeps the original's `createdAt` and `sortKey`; `keep-both` lands beside it under a
    new id; `skip`, and an id collision nobody resolved, write nothing.

### The empty library

15. A library with nothing in it is drawn as a `FujiIconPanel` — the icon-shape-title-body-action
    page the photo reader, both imports and the camera already use — rather than the bordered
    card it was. Its action is **Import from camera**, because a first launch is rarely
    someone who wants to type twenty parameters in and usually someone whose recipes are
    already in C1–C7. Creating one by hand sits under it, quieter, and the create FAB is on
    screen throughout either way.

### What does not change

16. Every screen above the repository. The seam `architecture.md` §4 kept for exactly this
    reason meant the library list, the recipe view, the editor, both import flows, export,
    the photo reader and the camera writer needed their result types renamed and nothing else.
17. The export file format. It is shared with the web client and is now the only thing the
    two projects have in common.

## Out of scope

- Any automatic backup, cloud or otherwise. Export is the backup, deliberately manual.
- Migrating an existing snapshot into the new store. The snapshot was a cache of a server
  that this app no longer reaches; a user with recipes on that server exports them from the
  web client and imports the file. Said once, in the release note, rather than built.
- Room, or any store with a query language. Nothing queries the library.

## Acceptance

- A fresh install opens on the library, with no setup screen anywhere in the app.
- Create, edit, rate, tag, delete and both imports work with the device in aeroplane mode —
  and would work with no radio at all, since the app cannot use one.
- Recipes survive force-stopping the app and reopening it.
- A corrupted `library.json` produces a named failure, an intact file afterwards, and a
  library that comes back when a good file is restored.
- `grep -r okhttp` and `grep -r INTERNET` over the repo return nothing outside history.
