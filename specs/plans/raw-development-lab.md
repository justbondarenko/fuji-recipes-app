# RAW development lab — a full editing surface on the camera engine

**Status:** Plan. Two hardware gates must be cleared before Phase 3 UI work is worth finishing.
**Proposed feature:** FEAT-016
**Branch:** `claude/raw-develop-lab-view-5mdbs3`
**Builds on:** `specs/plans/in-camera-raw-development.md` (FEAT-015), which is already implemented for the
one-shot recipe → JPEG path in `camera/raw/`, `camera/usb/RawDevelopmentSession.kt` and `ui/raw/`.

---

## 1. What this adds

Today RAW development is a one-way errand started from a recipe: pick a RAF, render once with that
recipe's settings exactly as stored, save the JPEG. `ui/raw/RawDevelopmentViewModel.kt` has no
concept of editing, and `camera/usb/developRaw` uploads the RAF, renders once and returns.

The lab turns that into a workspace:

1. **A top-level destination**, third in the bottom bar, directly after **Analyze**.
2. **Start from ground zero** — field defaults from `RecipeFields`, no recipe involved — or **apply a
   recipe from the library** as a starting point.
3. **Edit every parameter in place**, with the camera-applicable subset clearly separated from the
   settings the camera will not act on.
4. **Preview against the real engine**: re-render the loaded RAF as settings change, without
   re-uploading it.
5. **Save what came out of it**: the JPEG, a new recipe, an update to the applied recipe, or all
   of them.

The existing recipe-scoped `Develop RAW` entry points (library row menu, recipe view) stop opening
their own screen and open the lab pre-seeded with that recipe instead. One RAW surface, two ways in.

### What "live preview" can and cannot mean

Every preview is a camera round trip: write `0xD185`, write the trigger `0xD183`, poll
`GetObjectHandles`, download, `DeleteObject`. On hardware that is seconds, not frames. So:

- **No continuous live preview.** Dragging a slider must never fire renders.
- The default is an explicit **Update preview** action.
- An **Auto preview** toggle (off by default) re-renders on a settled edit — 900 ms after the last
  change, never mid-gesture — and coalesces: one render in flight, the newest settings win, any
  intermediate state is dropped rather than queued.
- The preview stays on screen, dimmed and badged **stale**, while the next render runs, so the panel
  never flashes empty.

This is the honest version of the request. It is also the only version that does not cook the
camera's battery.

---

## 2. Hardware gates

Neither is a code problem; both change the design if they fail.

### Gate A — repeated conversion from a single upload

FEAT-015 assumed a loaded RAF can be re-rendered while the session is healthy, but the shipped code
never exercises it: `developRaw` uploads on every call. The whole lab rests on this.

**Probe:** upload one X-T50 RAF, then run five conversions with different `0xD185` payloads without
re-sending `0x900C`/`0x900D`. Record for each: response codes, whether a new handle appears, the
output's pixel dimensions, and whether the applied settings actually differ between renders.

**If it fails** the lab still ships, with re-upload folded into every render. That is roughly a
40 MB transfer per preview, which kills Auto preview (it would be removed, not merely defaulted off)
and makes the preview budget a manual action with a visible byte counter.

### Gate B — a cheaper preview than a full render

Three candidate paths, in order of preference:

| Path | Mechanism | Unknown |
|---|---|---|
| Preview trigger | `0xD183 = 0` per rawji's preview/full distinction | FEAT-015 pinned X-T50 to `0` as *full*; the two claims disagree and only hardware settles it |
| Thumbnail fetch | `GetThumb` (`0x100A`) on the produced handle | Whether the camera attaches a usable thumb to a conversion result |
| Downscale on phone | Full `GetObject`, sample down for display | Always works; saves no camera time, only decode time |

**Probe:** render the same RAF/profile with trigger `0` and `1`; record dimensions, byte length, wall
time, EXIF. Then `GetThumb` both results. The outcome becomes a per-model constant beside the
existing `fullResolutionTrigger`, never a global assumption.

**If both fail** preview and final render are the same operation, and the UI drops the
preview/full distinction rather than lying about it — a single **Render** action whose result is
also what gets saved.

Both probes reuse the existing capture route: the lab keeps FEAT-015's calibration state, which
uploads a RAF, captures the native `0xD185` blob and shares it without triggering a conversion.

---

## 3. Shape

```
ui/lab/                          ← new
  RawLabScreen.kt                preview pane + parameter sheet + action bar
  RawLabViewModel.kt             one VM, reads/writes the holder below
  RawLabState.kt                 PURE state + reducer, JVM-testable
  RawLabParameterPanel.kt        the editing surface, reusing EditorControls
  RecipePickerSheet.kt           "apply a recipe from the library"

core/store/RawLabSessionHolder.kt   ← new. Survives navigation and, partially, process death
camera/usb/RawLabSession.kt         ← new. Load-once / render-many over an open PtpSession
camera/raw/RawFieldSupport.kt       ← new. Which field ids this model's adapter can apply
```

Changed:

| File | Change |
|---|---|
| `camera/usb/RawDevelopmentSession.kt` | Split: keep `developRaw` as the one-shot composition, extract `uploadRaf`, `applyProfile`, `triggerAndFetch` so the lab drives them separately |
| `camera/CameraController.kt` | Add `loadRawForLab`, `renderLoadedRaw(settings, quality)`, `releaseRawLab`; track the loaded RAF and a session epoch, cleared by `closeSession` |
| `camera/raw/RawDevelopmentProfile.kt` | Take a `JsonObject` of settings rather than a `Recipe`; derive both the patch and the supported-field set from one declared mapping table |
| `data/fields/RecipeFields.kt` | Move `defaultSettings()` out of `RecipeEditorViewModel` (currently private) so the editor and the lab build ground zero from the same place |
| `ui/nav/FujiNavHost.kt` | `RawLabRoute(recipeId: String? = null)`; delete `RawDevelopmentRoute`; renumber `toolbarIndex` |
| `ui/shell/AppShell.kt` | Fifth bar item; `isLabSelected` / `onLabClick` |
| `MainActivity.kt` | Bar wiring and selection predicate |
| `ui/theme/icons/FujiIcons.kt` | Add `science` (Material Symbols Rounded, W300/W400/W500) per `AGENTS.md` §1 |
| `ui/raw/` | Deleted once the lab covers its states; its strings are reused, not duplicated |

### Why the session holder exists

Bottom-bar navigation destroys the destination's ViewModel. Losing a 40 MB upload because the user
checked the camera's photo list would be indefensible, so the live part of the lab lives in
`AppContainer` alongside `CameraController`:

| Held | Survives navigation | Survives process death |
|---|---|---|
| Cached RAF file + name | yes | yes — the file is already in `RawDevelopmentCache` |
| Working settings, applied recipe id, dirty flag | yes | yes — snapshotted as JSON beside the RAF |
| Last preview / last full result file | yes | yes |
| Camera-side loaded-RAF state (epoch) | yes | **no** — the PTP session is gone; the RAF re-uploads on the next render |

The holder never caches a camera object handle across a session, which is the rule FEAT-015 already
set.

---

## 4. Parameter surface

The lab must not present a slider that does nothing. `RawFieldSupport.kt` exposes:

```kotlin
fun rawSupportedFieldIds(cameraModel: String): Set<String>
```

derived from the **same** table `patchRawDevelopmentProfile` patches from, so the two cannot drift —
a JVM test asserts that a fully-populated recipe's `appliedFields` equals the supported set exactly.

The panel renders three bands, all using the existing `NumberStepper`, `EnumDropdown`,
`EnumButtonGroup` and `FilmSimulationPicker` from `ui/editor/EditorControls.kt`:

1. **Applied by the camera** — the supported set, in `FieldGroup` order. Edits here change the next
   render.
2. **Saved, not rendered** — `dRangePriority`, `monochromaticColorWc/Mg`, `isoMin`, `isoMax` and any
   advisory field. Editable, because they belong in a saved recipe, and badged so nobody waits for
   the preview to change.
3. **Unknown keys** — anything a newer web client wrote. Never shown as controls, never dropped;
   carried through save exactly as `RecipeEditorViewModel` already does.

Applicability still comes from `FieldContext`: a monochrome film simulation removes `color` from the
panel rather than disabling it, same rule as the editor.

---

## 5. Flow

```
Lab (empty)
  ├── Choose RAF ──────────► RAF cached, settings = RecipeFields defaults
  └── Apply a recipe ──────► recipe picker → settings = that recipe's settings

Loaded
  ├── edit parameters ─────► dirty; preview badged stale
  ├── Update preview ──────► [upload if not loaded] → patch → trigger → fetch → show
  ├── Auto preview (off) ──► same, 900 ms after the last settled edit, latest-wins
  ├── Render full ─────────► full-resolution path, result replaces the preview
  └── Save ────────────────► JPEG (SAF) · new recipe · update applied recipe
```

**Save semantics**

| Action | Behaviour |
|---|---|
| Save JPEG | `ActivityResultContracts.CreateDocument("image/jpeg")`, as today. Offered for a full render; a preview-quality result says so in the dialog before saving |
| Save as new recipe | Name prompt (defaults to `<applied recipe> variant` or `Lab <date>`), `sortKey = max + 1`, unknown keys preserved |
| Update recipe | Only when a recipe was applied and settings are dirty; a diff-style confirm listing changed fields, since this overwrites library data |

Nothing auto-saves. Leaving the lab keeps the working state in the holder; an explicit **Discard**
clears the holder and the cache.

**Camera states** reuse the existing copy: not connected, wrong USB mode, connecting, and FEAT-015's
calibration-required state, which for an uncalibrated body still lets the user upload, capture and
share the native `0xD185` blob. Editing is allowed with no camera attached — only rendering needs one.

---

## 6. Failure handling

Every failure keeps the working settings. That is the point of the holder.

| Failure | Response |
|---|---|
| Preview render fails, session healthy | Inline error above the panel, keep the stale preview, keep the loaded RAF |
| Framing/timeout error | Session closed by `CameraController`; loaded-RAF epoch invalidated so the next render re-uploads |
| Cable detached mid-render | Same, plus the connect prompt; nothing is re-attempted automatically |
| Camera busy (`DEVICE_BUSY`) | Existing bounded backoff; surfaced as "the camera is still working" rather than a failure |
| Several new handles appear | Fail closed, delete nothing — FEAT-015's rule, unchanged |
| Setting refused / normalised | Reported in the applied/preserved summary under the preview |
| Auto preview repeatedly failing | Auto preview switches itself off after two consecutive failures and says so |

Wake lock is held per render, not for the lab session.

---

## 7. Phases

### Phase 0 — gates (hardware, ~1 day with the body to hand)
1. Probe A: five conversions from one upload; record everything in §2.
2. Probe B: trigger `0` vs `1`, `GetThumb` on both, dimensions and wall time.
3. Write the findings into this file and add the captures as test fixtures under
   `app/src/test/resources/camera/raw/`.

Phases 1–2 do not depend on the outcome. Phase 3's preview controls do.

### Phase 1 — protocol split (JVM-testable)
1. Extract `uploadRaf` / `applyProfile` / `triggerAndFetch` from `developRaw`; `developRaw` becomes
   their composition and its existing test must pass untouched.
2. `RawLabSession`: `ensureLoaded(raf)`, `render(settings, quality)`, `invalidate()`.
3. `CameraController`: loaded-RAF tracking, epoch reset in `closeSession`, `renderLoadedRaw`.
4. Extend `FakeCamera` to serve repeated conversions and to fail a render without losing the load.
5. Tests: one upload for N renders; re-upload after invalidation; no stale handle reuse; cleanup runs
   on both success and failure paths.

### Phase 2 — settings-driven profile + field support
1. `patchRawDevelopmentProfile(cameraModel, base, settings: JsonObject)`; `Recipe` overload delegates.
2. `RawFieldSupport.rawSupportedFieldIds`, derived from the same table.
3. Move `defaultSettings()` into `data/fields/`; point the editor at it.
4. Tests: supported-set ↔ applied-set equality; ground-zero defaults render a valid patch; sparse
   settings fall back to defaults rather than to whatever the camera had.

### Phase 3 — lab state and screen
1. `RawLabState` + reducer, pure: apply recipe, edit field, dirtiness, staleness, coalescing decision,
   save-target availability. Tested without Compose.
2. `RawLabSessionHolder` in `AppContainer`, with a JSON snapshot beside the cached RAF.
3. `RawLabScreen`: preview pane (Coil, `ContentScale.Fit`, stale dim + badge), parameter panel in a
   bottom sheet with a drag handle so the picture keeps the top half, action bar.
4. Recipe picker sheet over `repository.library`.
5. Save paths, including the update-confirm diff.

### Phase 4 — navigation
1. `RawLabRoute(recipeId: String? = null)`; delete `RawDevelopmentRoute` and `ui/raw/`.
2. Bar item after Analyze; `toolbarIndex` becomes Library 0, Analyze 1, Lab 2, Photos 3, More 4.
3. `FujiIcons.Science` added per `AGENTS.md` §1 — all three weights fetched, no Compose Material icon.
4. `nav_lab` = **Lab**; reuse the `raw_*` strings, add only what the new states need.
5. Repoint `onDevelopRaw` in the library and recipe view at the lab.

### Phase 5 — verification
`:app:assembleDebug`, `:app:testDebugUnitTest`, `:app:lintDebug`, then on hardware: ground-zero
render; recipe applied then edited; ten consecutive previews on one upload; monochrome recipe with
`color` correctly absent; save-as-new round-tripped through export and back; detach during upload,
during render and during download; process death with a loaded RAF; Auto preview under fast edits.

---

## 8. Estimate

| Phase | Days |
|---|---|
| 0 — gates | 1 (hardware-bound) |
| 1 — protocol split | 1.5 |
| 2 — settings/profile | 1 |
| 3 — state + screen | 3 |
| 4 — navigation | 0.5 |
| 5 — verification | 1 |

**Roughly 8 focused days** if both gates pass. If Gate A fails, subtract Auto preview and add a day of
transfer-budget UI. If Gate B fails, subtract the preview/full split entirely — that is a simpler
build, not a longer one.

## 9. Deliberately out of scope

- Picking the RAF from the camera card. That needs FEAT-015's two-mode handoff, which is unbuilt.
- Batch development of several RAFs.
- Any phone-side rendering. The camera is the engine; an app-side approximation would be a second,
  disagreeing answer to what a recipe looks like.
- Writing lab settings to a camera custom slot. Separate feature, separate encoding dialect.
