# RAW development lab (FEAT-016)

Plan: `specs/plans/raw-development-lab.md`.

## Phase 1 — protocol split
- [x] `uploadRaf` / `applyRawSettings` / `convertAndFetch` split out of `developRaw`, which is gone
- [x] `RawLabSession`: load once, render many; invalidated only by a framing or timeout error
- [x] `CameraController.renderRawInLab`, with the lab session tied to the PTP session
- [x] `FakeCamera.uploadCount`, and tests for one-upload-many-renders

## Phase 2 — settings-driven profile
- [x] `patchRawDevelopmentProfile` takes a `JsonObject`; the `Recipe` form delegates
- [x] One declared mapping table; `rawSupportedFieldIds` reads it
- [x] `defaultRecipeSettings()` shared by the editor and the lab
- [x] Test holding the supported set and the applied set together

## Phase 3 — lab state and screen
- [x] `RawLabState` — pure, with its transitions tested
- [x] `RawLabWorkspace` above the nav graph, in `AppContainer`
- [x] Preview pane with stale dimming, stage progress and quality badge
- [x] Parameter panel, reusing `EditorControls`
- [x] Recipe picker, save sheet, name dialog, update confirmation

## Phase 4 — navigation
- [x] `RawLabRoute(recipeId)`, `ui/raw/` deleted, `Develop RAW` repointed
- [x] Bar item after Analyze; `toolbarIndex` renumbered
- [x] `FujiIcons.Science` in all three weights

## Phase 0 — hardware gates (open)
- [ ] Gate A: five conversions from one upload on the X-T50
- [ ] Gate B: trigger `0` vs `1`, and `GetThumb` on a conversion result
- [ ] Record both as fixtures and fold the findings into the plan

## Phase 5 — verification
- [x] 30 JVM tests pass (compiled and run offline; CI runs the full suite)
- [ ] On device: ten previews on one upload, monochrome recipe, detach mid-render, save paths

## Review follow-ups
- [x] Draw only the fields the camera renders; drop the "saved, not rendered" band
- [x] Empty lab asks for a RAF and nothing else
- [x] Recipe picker button: labelled and filled, not a bare glyph
- [x] "Re-render automatically"; no "starting from the defaults" beside the filename
- [x] Head exposure compensation under **Exposure**, not §4's "not written to the camera"
- [x] Drop the screen's own `TopAppBar`: it drew under the shell's camera-button row, which is
      why the header looked crowded. A header row inside the content, like Photos and Analyze,
      and the full `contentPadding` applied
- [x] Recipe button tonal, labelled **Recipes**
- [x] Camera state moved into the preview area — no camera, no render, so that space was the
      right place for it; the warning no longer sits between the filename and the controls
- [x] Connect is offered, not instructed: the attach intent already opens the session
- [x] Preview 95% of the width, and zoomable in place (pinch, pan, double-tap)
- [x] Filename becomes the header title; the recipe name its subtitle. The chip row is gone
- [x] Save moves to the header; it renders full-resolution first when it has to, so the
      Render full button and the preview-sized save are both gone
- [x] The automatic switch and Update preview share a row; the button hides while the switch
      is on and comes back on a failure

## Review

- **The lab replaces the one-shot screen rather than sitting beside it.** `ui/raw/` is deleted and
  both `Develop RAW` entry points open the lab seeded with that recipe, so there is one RAW surface.
- **"Live" is deliberately not continuous.** Every preview is a full PTP round trip, so the default
  is an explicit button; the automatic mode debounces 900 ms, runs one render at a time, takes the
  newest settings when a render finishes, and switches itself off after two failures.
- **The upload is the thing worth protecting.** `RawLabSession` keeps the load through a refused
  setting or a render timeout, and the workspace sits in `AppContainer` so a bottom-bar tap cannot
  cost a 40 MB transfer.
- **A control that does nothing is worse than one that is absent** — so it is absent. The panel is
  drawn from the same table the patch writes from, a test fails if the two disagree, and fields the
  lab never shows still survive a save untouched.
- Not verified on a device or a camera. The two hardware gates above decide whether the automatic
  mode and the preview/full split survive as designed.
