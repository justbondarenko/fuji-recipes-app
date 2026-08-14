# Coding standards — Android client

**Status:** binding
**Version:** 1.0

These are the rules a spec must not require breaking. They outrank convenience and they
outrank the plan.

---

## P1 — Single user, no accounts

There is exactly one user. No user ids on entities, no sharing, no permissions, no profile.
The Access service token is a door key, not an identity: nothing in the app may vary by
who is holding it.

*Inherited from `fuji-recipes-book/specs/constitution.md` P1, and it holds on both clients
or it holds on neither.*

## P2 — The server owns the data

The phone never invents a `sortKey`, a `createdAt`, an `updatedAt`, or an `id` that the
server would otherwise assign. It never repairs a value it received. It never writes to the
snapshot anything the server did not send.

Corollary: **the snapshot is written verbatim.** Not normalised, not re-serialised from
parsed models, not stripped of unknown keys. A field a newer server sends must survive a
round trip through an older build of this app.

## P3 — One source of truth for field definitions

Groups, labels, ranges, defaults, and applicability rules are transcribed from
`fuji-recipes-book/specs/shared/field-definitions.md` into exactly one Kotlin object. The
form, the list, the write plan, and any text renderer read it. A label written twice is a
bug.

Every transcribed file carries a header naming the source document, its version, and the
commit it was read at.

## P4 — The camera protocol layer is pure and isolated

`buildWritePlan(recipe, cameraInfo): WritePlan` is a pure function — no I/O, no clock, no
randomness, no Android imports. Transport (`UsbManager`, PTP framing) is a separate module
that consumes a plan and knows nothing about recipes.

Enforced structurally: nothing under `camera/plan/` may import `android.*`.

## P5 — Errors name the thing that failed

No "Something went wrong." A refused request names which of the contract's `error` codes
came back and what the user can do. A failed camera write names the property and the PTP
response code. A validation failure names the field path the server gave.

Specifically forbidden: rendering a 403, a 503, and a socket timeout as the same message.
They have three different remedies.

## P6 — No I/O in ViewModels or composables

ViewModels call repositories. Repositories call `ApiClient` and `SnapshotCache`. A
composable that touches the network, the filesystem, or `System.currentTimeMillis()`
directly is a bug, because it cannot be tested and it cannot be previewed.

Time is injected. `Clock` is a constructor parameter wherever a timestamp is compared or
formatted.

## P7 — Selection logic is pure and lives outside Compose

Search, filtering, and sorting are pure functions over a list, in a file with no Compose and
no Android imports — `data/library/LibraryView.kt`, mirroring
`fuji-recipes-book/src/utils/library-view.ts`. It sits under `data/` rather than `ui/` so
that `core/settings` can read the stored-view types without a layer inversion. The ViewModel
adds reactivity; the file decides. This is what makes the parity with the web client
testable rather than asserted.

## P8 — No speculative abstraction

An interface with one implementation is allowed exactly where `architecture.md` names it
(`RecipeRepository`) and nowhere else. `AppContainer` gains a field when a feature needs it,
not before. No factories for one product. No config for a value
that never changes. No `RemoteSyncSource` stub, no dirty flag, no conflict-resolution
strategy — they are guesses and they will be wrong when a real requirement arrives.

## P9 — Non-trivial logic ships with a check

A branch, a loop, a parser, a comparator, or an encoding path leaves one runnable test
behind — the smallest thing that fails if the logic breaks. Trivial one-liners need none.
No fixtures scaffolding, no per-function suites unless asked.

The pure files (P4, P7) are where the tests earn their keep. Compose UI tests cover screen
*states*, not individual widgets.

## P10 — Deliberate shortcuts are marked

A simplification that cuts a real corner with a known ceiling gets a `// ponytail:` comment
naming the ceiling and the upgrade path:

```kotlin
// ponytail: whole library held in memory; paginate if the design limit of ~2000 recipes
// (fuji-recipes-book/specs/data-model.md §7) is ever approached.
```

`specs/roadmap.md` carries the ones that are scheduled. The rest are found by grep, on
purpose.

---

## Compose conventions

- One `UiState` data class per screen, exposed as `StateFlow`. No `mutableStateOf` in a
  ViewModel for screen state.
- Screen composables take state and lambdas, never a ViewModel. A `…Route` composable does
  the `viewModel(factory = …)` wiring against `AppContainer`. This is what makes previews and
  UI tests possible.
- `@Preview` for every screen state a spec names, including the error and empty ones. A
  preview of only the happy path is how the empty state ships broken.
- Consume `WindowInsets.safeDrawing`. Edge-to-edge is mandatory on Android 15+ regardless.
- Strings in `strings.xml`. Not because of translation — because an error message hardcoded
  at a call site is one nobody finds when P5 is violated.

## Naming

Follow `architecture.md` §7's glossary. In code as in UI: `Recipe`, `settings`,
`filmSimulation`, `slot`, `manualOrder`, `snapshot`, `lastWrittenSlot`.

Wire field names are `camelCase` exactly as the API sends them. Do not rename on the way in
— `@SerialName` used to bridge a rename is a place where a future field gets dropped
silently.

## Git

One feature folder, one branch: `feat/FEAT-XXX-slug`. A commit closes tasks from
`04-tasks.md` and names them. `01-functional.md` is frozen once implementation starts;
changes after that point ship in the same commit as the code that required them.
