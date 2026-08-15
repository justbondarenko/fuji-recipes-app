# FEAT-008: Export recipes

**Tracker issue:** —
**Source PRD:** `PRD.md` §7.6 (export), C2 (export is the backup guarantee)
**Canonical format:** `fuji-recipes-book/specs/shared/recipe-format.spec.md` — SF-001…SF-018
**Parity target:** `fuji-recipes-book/shared/format/export.ts`, `src/pages/more/export.vue`
**Status:** Ready

---

## Problem

The library lives on one server behind one Cloudflare Access token, and there is no way to
get a copy of it out of this app. No file, no backup, nothing to send someone who asks what
you shot.

`PRD.md` C2 calls export "the only backup, which promotes it from a feature to a guarantee".
`roadmap.md` §3 deferred it to v2 on the grounds that the web client is the backup surface —
true, and irrelevant the moment the phone is the device you have with you, which is the
premise of this app.

**More → Export** selects recipes, builds a file, and hands it to the OS share sheet, which
reaches the file system, Google Drive, mail, messaging and anything else installed with no
per-service integration to write. Plus an **Export** action on a single recipe, for sending
one to someone.

## User stories

- As the photographer, I can save my whole library to a file, so that it exists somewhere other than one server behind one token.
- As the photographer, I can put that file wherever I already keep things — the phone, Drive, a message — without the app knowing anything about those services.
- As the photographer, I can export with no signal, so that a backup is something I can take in the field rather than only at home.
- As the photographer, I can send one recipe to someone as a small readable file, so that sharing a look does not mean sharing my library.
- As the photographer, I can choose one file or one file per recipe, so that the export suits what I am going to do with it.

## Scope

### The file

1. The file is built **on the phone**, from the library already in memory
   (`architecture.md` §4). It is not fetched from `GET /api/export`.
2. Consequence of §1, and the reason for it: **export works with no network**. A backup that
   needs a working connection to the thing being backed up is not a backup.
3. The format is `recipe-format.spec.md`'s, which is binding and shared with the web client:
   an envelope of `format` / `version` / `exportedAt` / `recipes` (SF-002), `format` is the
   literal `fuji-recipe` (SF-003), version `1` (SF-004).
4. UTF-8, no BOM (SF-001). Two-space indentation and a trailing newline — the format pins a
   key order so that a diff of two backups is readable, and a single-line file defeats that.
5. **SF-008 exclusions:** `sortKey`, `lastWrittenSlot`, `lastWrittenAt` and
   `sensorGeneration` never leave. The first is this library's own order and means nothing in
   another; the middle two are a phone's camera bookkeeping; the last was dropped as a column
   in D1 migration 0002.
6. Key order within a recipe and within `settings` is `recipe-format.spec.md` §3's, stated
   **explicitly** rather than derived from `RecipeFields`. The reference derived it, and a UI
   regrouping silently reordered every exported file — a display grouping and a serialisation
   order are different concerns that happened to coincide, and only one of them is a
   compatibility promise.
7. **Values are re-ordered, never re-serialised.** Every value came from the server already
   validated, and this client holds `settings` as a raw JSON object, so export copies values
   verbatim. `coding-standards.md` P2 — "it never repairs a value it received" — at its
   strongest.
8. Consequence of §7: **SF-017 is free.** Unknown keys, inside a recipe and inside `settings`,
   survive the round trip. They are emitted after the documented ones so §3's order still
   reads as §3's order.
9. A key the stored recipe does not have is **left out**, not filled with its default.
   Defaults are filled on *import*; inventing them here would put values in a file the
   photographer never chose.
10. Filenames (SF-018): `fuji-recipes-YYYY-MM-DD.json` or `.zip` for a multi-recipe export,
    `<slugified-name>.json` for one. The date is **UTC**, matching every other timestamp in
    the format.
11. A name that slugifies to nothing — all punctuation — falls back to `recipe-<first 8 of
    id>.json`, which is unique and traceable, rather than a generic name a second export
    would overwrite.

### The archive

12. The ZIP holds one JSON file per recipe, each a **bare recipe object** with no envelope.
13. Names inside the archive are the single-recipe names, **de-duplicated**: recipe names are
    not unique, so two recipes called "Portra 400" would otherwise be one filename twice and
    the archive would silently ship one recipe instead of two. The first keeps the plain name;
    each later clash gets `-2`, `-3` before the extension.

### The screen

14. **More → Export**, a dedicated screen beside Import.
15. A selection list of every recipe, **everything selected by default**. A backup is the
    common reason to be here, and a screen that opens with nothing selected asks for work to
    reach the obvious result.
16. Select all / none, and a live count of how many of how many.
17. A choice of **one JSON file** or a **ZIP of JSON files**, each with a line saying when to
    want it.
18. The export action names the file it will produce, and is disabled with a reason when
    nothing is selected.
19. An empty library says so rather than showing an empty list with a dead button.

### Sharing

20. The file goes to the **OS share sheet**, which covers saving to the device, Google Drive,
    mail and anything else installed — with no per-service API in this app.
21. **This deviates from `PRD.md` §7.6**, which specifies the Storage Access Framework
    (`CreateDocument`). Recorded rather than silently ignored: one sheet reaches every
    destination, where SAF reaches only the file system. A direct "save to device" via SAF
    remains available against the same file builder if it is ever wanted.
22. The file is written to app-private cache and shared as a `content://` URI through a
    `FileProvider`. No storage permission is requested, and none is needed.
23. Previous exports in that cache directory are cleared each time. They are transient by
    definition — whatever the share sheet hands the file to owns the real copy.

### One recipe

24. An **Export** action on the recipe view shares a single `<slug>.json`.
25. It is a **bare recipe object, not an envelope** — SF-005 has import accept exactly that
    shape, and it makes the file readable at a glance, which is the point of exporting one.

## Out of scope

| Deferred | To | Why |
|---|---|---|
| Importing from a file | v2 | This feature is the backup half. Reading files back needs the ZIP-traversal rules (SF-015), the migration chain (SF-007) and the conflict flow — and the web client already has all of it. |
| `GET /api/export` | won't do | The route exists so a bare URL produces a backup. This client builds the file locally, which is faster, works offline, and is what the web client's own user-facing path does. |
| Exporting a filtered view | won't do | The selection list already answers it, and more precisely. |
| Encrypting or password-protecting the archive | won't do | The file goes wherever the photographer sends it, and that destination's own protection is the one that matters. |

## Error surfaces

| Failure | Surface | Message names |
|---|---|---|
| The library has not loaded yet | The screen, while it waits | That it is loading, not that it is empty |
| The library is empty | The screen, plainly | That there is nothing to export yet |
| Nothing selected | The action, disabled | That at least one recipe is needed |
| The file could not be written to cache | The screen, with Retry | That the export could not be prepared, and that nothing left the app |
| No app can receive the file | The system's own sheet handles it | — |
