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
| FEAT-002 | Field source + recipe view | The canonical 27-field table, and a read-only screen to read a recipe on | **done** |
| FEAT-003 | Recipe form | Create, edit, duplicate, delete; rating and tags in place | **done** |
| FEAT-004 | Settings | Connection settings reachable at any time, and clearable | **done** |
| FEAT-005 | Camera connection **GATE** | USB host, attach intent, PTP session, model detection, the connection indicator | **done** — connects on hardware |
| FEAT-006 | Write to slot | Write plan, encoders, slot picker, progress, failure reporting | **done** — writes to a slot on hardware |
| **FEAT-007** | **Import from camera** ← *in progress* | More → Import: read C1–C7 off the body, review against the library with duplicate detection, import | low — the protocol half is done and verified |
| **FEAT-008** | **Export recipes** ← *in progress* | More → Export: select recipes, build the canonical file, hand it to the OS share sheet. Plus a single-recipe export | low — the format is specified and shipped on the web |
| **FEAT-009** | **Read a recipe from a photo** ← *in progress* | Bottom bar → Read: decode a JPEG's Fujifilm MakerNote, match it against the library by name, save it as a new recipe | medium — the MakerNote layout is reverse-engineered, but the reference has run against real files |
| FEAT-010 | Polish | Motion timing, haptics, reduced-motion fallback, dark-scheme audit, predictive back | none |
| **FEAT-011** | **Create a recipe from pasted text** | The create FAB becomes a FAB menu: paste a recipe from Fuji X Weekly, a forum or notes, and the form opens filled in | low — the parser is a port of one the web client already ships |
| **FEAT-012** | **Import a file** | More → Import a file: read back a `.json` or `.zip` this app or the web client exported, review it against the library, resolve id collisions, import | low — the format is specified, and the reference implementation is the one the web client ships |

**FEAT-001 to FEAT-003 carry no protocol risk and ship a usable recipe manager.** That is
deliberate: a hard slog in FEAT-005 must leave a working product behind, not a stalled one.

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

**One was reversed.** The read-only recipe view was deferred to v2, with a card tap going
straight to the editor. Reinstated as FEAT-002 at the owner's request: reading a recipe is
what happens far more often than changing one, and a form is a poor surface for it. The
field-source transcription it needed was going to be built for the form anyway, so the
ordering cost nothing — the form (FEAT-003) now inherits a table that is already tested.

| Deferred | To | Why |
|---|---|---|
| ~~Reading a slot's full settings back as an importable recipe~~ | **FEAT-007** | Reversed. The recipes already on the body could not reach the library at all, which made the camera link one-way. The slot picker's name read (FEAT-006) proved the mechanism; FEAT-007 decodes the whole slot. |
| Reorder / manual-order editing | v2 | The list *respects* manual order as a tiebreak and never offers it as a sort — matching the web client, which also does not offer it. Editing it needs `POST /:id/move` and a drag surface. |
| Delete and duplicate | FEAT-003 | They belong with the editor, not the list |
| ~~Export as a file~~ | **FEAT-008** | Reversed. "The web client is the backup surface" holds right up until the phone is the device you have with you, which is this app's premise — and `PRD.md` C2 calls export the backup *guarantee*, not a convenience. Building the file locally also means it works with no signal, which a backup has to. |
| ~~Import **from a file**~~ | **FEAT-012** | Reversed. Export shipped in FEAT-008 and the files it writes could only be read by the web client, which makes this phone a device that can make a backup and not restore one. The parts that made it "the larger half" — ZIP traversal (SF-015), the migration chain (SF-007), the conflict flow (SF-010 to SF-014) — are all specified, and skipping them is what leaves a backup unreadable. |
| Offline **writes** | v2 | See `architecture.md` §4. Needs a queue, and a queue needs conflict resolution. |
| Sensor-generation filter | won't do | The column was dropped in D1 migration 0002. The web client has no such filter either. |
| Tablet layouts, Wear OS, widgets | won't do | `PRD.md` §3 non-goals stand |
| Slot bookkeeping (`lastWrittenSlot`, `lastWrittenAt`, `POST /:id/written`) | **won't do** | The owner does not want the app recording when a recipe reached a camera. The fields still pass through untouched so the web client's own bookkeeping is not destroyed — declining to track something is not the same as deleting it. |

## 4. Open dependencies on the other repo

None for FEAT-001 through FEAT-003 — every route they need already exists and is specified
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
| `HorizontalFloatingToolbar` on the form | FEAT-003 |
| `LoadingIndicator` on camera connect | FEAT-004 |
| `LinearWavyProgressIndicator` during a write | FEAT-005 |
| `ButtonGroup` for the C1–C7 slot picker | FEAT-005 |

**The decision to take, before FEAT-003:** either move the whole toolchain to AGP 9.1 /
`compileSdk` 37 / Compose beta, or accept standard Material 3 for v1 and revisit when the
Expressive line goes stable. `ui/theme/Theme.kt` carries the swap instructions either way —
it is a one-call change plus a `motionScheme` argument that is already computed.

## 7. The camera stages, re-rated

Written when FEAT-005 started, and it corrects §1's own risk column.

Both camera stages were rated **high** on the premise that the protocol was reverse-engineered
and partly unverified — `PRD.md` C3, and `steering/architecture.md` §8 C3 say the same. That
premise is out of date, and the correction is worth recording because it is the reason these
two stages are being attempted together.

`fuji-recipes-book` shipped a **working, unit-tested WebUSB PTP implementation**:
`camera/usb/{ptp,transport,session,payload,write}.ts`, `camera/{models,encoding,write-plan,read-slot}.ts`,
and roughly 2,600 lines of tests including a fake camera that speaks PTP back. Its stage 32
resolved every property code from `eggricesoy/filmkit` @ `9e3bbcf`, cross-checked against an
X100VI, and its stage 37 replaced the plan-time write order with the observed one.

So the Android work is a **transcription against a known-good reference**, not protocol
archaeology. What remains genuinely unknown is narrower and named:

| Still unverified | Where it bites |
|---|---|
| WebUSB and `android.hardware.usb` differ in claim and endpoint behaviour | FEAT-005 T-08. The reference's ranked-interface and discovered-endpoint logic exists *because* of real claim failures, and is carried over rather than simplified. |
| Property codes are verified on X-Trans V (X100VI) only | `propertyCodesVerifiedFor` is transcribed with the tables; an unverified generation says so rather than pretending. |
| GFX is assumed, not observed | `GFX_FIELD_SET_VERIFIED = false` travels with the transcription. |

**One branch, two feature folders.** `coding-standards.md`'s Git section says one feature
folder, one branch. FEAT-005 and FEAT-006 share `feat/FEAT-005-camera-connection`, at the
owner's decision: they transcribe one reference module, and splitting them would mean porting
`encoding.ts` twice or merging a connection that cannot yet do anything. The gate property is
kept instead of the branch rule — FEAT-005 T-18 is a complete, installable, shippable stage on
its own, so a slog in FEAT-006 still leaves a working product behind. That was always the point
of the ordering.
