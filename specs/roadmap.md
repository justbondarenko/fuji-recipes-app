# Roadmap — staged delivery

**Status:** proposed
**Scope:** v1 = list, create, edit, connect to camera, write to a slot
**Governed by:** `specs/steering/coding-standards.md`

---

## 0. What a stage is

A feature folder is a stage. A stage ships when all four hold:

| Rule | Meaning |
|---|---|
| **It runs** | `./gradlew :app:assembleDebug` succeeds and the app is navigable. No stage ends mid-refactor. |
| **It is green** | `testDebugUnitTest` and `lintDebug` pass. Every scenario in `03-behavior.feature` has a test or a named manual check. |
| **It is cited** | Every task names the requirement behind it. |
| **It is worth shipping** | Someone could install this build and it would do something more than the last one. |

Stages are sequential. A **GATE** must be fully closed before the next stage starts.

## 1. Sequence

| # | Feature | Ships | Risk |
|---|---|---|---|
| FEAT-000 | Foundation — project, theme, icon, shell | An app that opens, looks right, and navigates between empty screens | **done** |
| **FEAT-001** | **Recipe list** ← *start here* | Connection setup, the API client, the snapshot cache, and a real library on screen with search, filters and sort | low |
| FEAT-002 | Field source + recipe form | Create and edit a recipe against the full parameter set | medium — the field transcription is large and exacting |
| FEAT-003 | Camera connection **GATE** | USB host, attach intent, PTP session, model detection, the connection indicator | **high** — reverse-engineered protocol |
| FEAT-004 | Write to slot | Write plan, encoders, slot picker, progress, failure reporting, `POST /:id/written` | **high** |
| FEAT-005 | Polish | Motion timing, haptics, reduced-motion fallback, dark-scheme audit, predictive back | none |

**FEAT-001 and FEAT-002 carry no protocol risk and ship a usable recipe manager.** That is
deliberate: a hard slog in FEAT-003 must leave a working product behind, not a stalled one.

## 2. Why the list is first

It is the stage that forces every foundational decision to become real code:

- The **API client** and its error envelope — every later feature reuses it
- The **Access service token** path — nothing reaches the server until this works
- The **snapshot cache** — the offline story, proved on the screen that needs it most
- The **theme** applied to real content rather than a swatch page
- The **film-simulation table**, the first canonical transcription from the Nuxt repo

And it is independently useful: a read-only library on the phone, offline, is already worth
installing.

## 3. Deliberate deferrals

Recorded here so they are decisions rather than omissions.

| Deferred | To | Why |
|---|---|---|
| Read-only recipe detail screen | v2 | v1 scope is list / create / edit / camera. Tapping a card opens the **editor**. A separate read-only route is the right long-term design (the web client has one) and is not in the stated v1. |
| Reorder / manual-order editing | v2 | The list *respects* manual order as a tiebreak and never offers it as a sort — matching the web client, which also does not offer it. Editing it needs `POST /:id/move` and a drag surface. |
| Delete and duplicate | FEAT-002 | They belong with the editor, not the list |
| Export / import | v2 | The web client is the backup surface and `P2` of the web constitution already guarantees it. Duplicating it on Android before the camera works is misordered. |
| Offline **writes** | v2 | See `architecture.md` §4. Needs a queue, and a queue needs conflict resolution. |
| Sensor-generation filter | won't do | The column was dropped in D1 migration 0002. The web client has no such filter either. |
| Tablet layouts, Wear OS, widgets | won't do | `PRD.md` §3 non-goals stand |

## 4. Open dependencies on the other repo

None for FEAT-001 through FEAT-002 — every route they need already exists and is specified
in `fuji-recipes-book/specs/contracts.md`.

One configuration dependency, needed before FEAT-001 can be tested against the real
deployment:

- [ ] A Cloudflare Access **service token** is created and the Access application's policy
      admits it (Service Auth). Dashboard configuration, no code change.
      *Owner: Andrii. Blocks FEAT-001 T-12 onward; the rest of FEAT-001 is testable against
      `MockWebServer`.*

## 5. Documents this roadmap corrects

`PRD.md` predates the decision to share a database with the Nuxt client and predates the
web client's shipped theme. Two of its sections are superseded and are marked as such in the
file itself:

| Section | Said | Now |
|---|---|---|
| §4.1 Data storage | "Room only. No backend, no auth, no network permission." | Server-backed via the existing `/api`, snapshot cache, `INTERNET` required — `architecture.md` §4 |
| §5.2 Colour tokens | "Cappuccino", source colour `#CC785C` | Stone monochrome with an amber accent, matching what the web client shipped — `steering/design-system.md` §2 |

Everything else in `PRD.md` — the field semantics, the camera integration, the USB gotchas,
the error-handling table, the build order's *reasoning* — stands and is still worth reading.


## 6. Open toolchain decision — Material 3 Expressive

Surfaced while building FEAT-000, by compiling against the API rather than reading release
notes. Full detail and the evidence: `steering/tech-stack.md` §6.

**The finding:** at `material3` 1.4.0 — the newest release that resolves under the AGP
8.13 / `compileSdk` 36 toolchain this machine has — every Expressive entry point is
`internal` or absent. Getting them means `material3` 1.5.0-alpha → Compose 1.12.0-beta →
AGP 9.1 → `compileSdk` 37, and the `android-37` platform is not installed.

**What shipped instead:** the standard `MaterialTheme` carrying the full Fuji palette, type
scale and shape scale. That is the part of `design-system.md` that holds parity with the web
client, and it is unaffected.

**What is deferred:** the Expressive motion scheme and four components. Every screen that
wants one is a later feature, so nothing is blocked today:

| Wants Expressive | Feature |
|---|---|
| `HorizontalFloatingToolbar` on the form | FEAT-002 |
| `LoadingIndicator` on camera connect | FEAT-003 |
| `LinearWavyProgressIndicator` during a write | FEAT-004 |
| `ButtonGroup` for the C1–C7 slot picker | FEAT-004 |

**The decision to take, before FEAT-002:** either move the whole toolchain to AGP 9.1 /
`compileSdk` 37 / Compose beta, or accept standard Material 3 for v1 and revisit when the
Expressive line goes stable. `ui/theme/Theme.kt` carries the swap instructions either way —
it is a one-call change plus a `motionScheme` argument that is already computed.