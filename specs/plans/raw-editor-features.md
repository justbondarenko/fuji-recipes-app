# RAW Editor feature tracker

Baseline: `0d0b92d` (2026-10-24). Checked items are implemented and verified;
unchecked items are not delivered. Keep this file updated in the same delivery as
the implementation. The original RAF remains unchanged and the recorded film
simulation color profile remains the default starting point.

## Already delivered

- [x] Phone-side sensor RAW decoding, separate from camera-engine Lab.
- [x] Automatic recorded film-simulation color profile (eleven color simulations).
- [x] As-shot white balance applied before the first editable preview.
- [x] Exposure, shadows, and highlights controls.
- [x] Reset to the profiled starting point.
- [x] Hold the image preview to compare with the original embedded camera JPEG;
  release or cancel to return to the edit, with no separate comparison button.
- [x] Double-tap any parameter name to reset only that adjustment, including
  inactive detail controls. Radius resets to 1 px and Preserve detail to 50%.
- [x] Screen-reader custom actions for individual reset and JPEG comparison.
- [x] Interactive sensor RAW preview while dragging, with automatic settled refinement.
- [x] Material 3 XS sliders with precision-matched steps, floating value indicators, and fine dragging.
- [x] Full-resolution JPEG export through the document picker.
- [x] Discard confirmation and stable progress/comparison layouts.
- [x] Requested Light, Color, Details, Curves, Effects, and Optics group/option order and icons.
- [x] Wide/foldable editor: fixed category tabs above a vertically scrolling list of icon-and-title sliders.
- [x] Independent paired sliders; parameter selector above one slider for Grain and Vignette.
- [x] Persistent bottom-right preview Crop button, with no Framing category.
- [x] Compact Material 3 aspect-ratio menu beside rotation controls, replacing the chip row.

## First batch — authorized

These are the recommended everyday controls selected by the user to start with.
Each must work in preview and full-resolution export, persist with the edit session,
and reset to a neutral adjustment without replacing the recorded color profile.

- [x] **2. White balance:** relative temperature and green/magenta tint, with As Shot reset.
- [x] **4. Contrast:** increase or soften overall tonal separation.
- [x] **5. Whites and blacks:** independent endpoint sliders grouped under Highlights and Shadows respectively.
- [x] **6. Saturation:** adjust overall color intensity, including desaturation.
- [x] **7. Vibrance:** preferentially adjust muted colors.
- [x] **11. Sharpening:** amount, radius, and threshold to avoid sharpening small noise.
- [x] **12. Color noise reduction:** reduce colored speckles while preserving edges.
- [x] **13. Luminance noise reduction:** smoothing amount and detail preservation.

## Second batch — items 14–18

- [x] **14. Clarity:** medium-scale local contrast, positive or negative.
- [x] **15. Texture:** fine detail enhancement or softening.
- [x] **16. Dehaze:** haze reduction or added atmospheric haze.
- [x] **17. Framing:** direct-entry crop workspace, locked aspect presets (including portrait),
  left/right 90° buttons, and a ±15° Straighten slider below the photo, with Apply/Cancel.
- [x] **18. Vignette:** darken/brighten amount, midpoint, roundness, and feathering,
  centered on the framed output.

All five work in preview/export and persist in the session. Existing sixteen-value
sessions upgrade without changing their edits. Each scalar control supports label
double-tap reset; crop has Apply/Cancel and a separate reset. Crop movement and
resizing are available as screen-reader actions.

## Lens / geometry — item 20

- [x] Manual barrel/pincushion distortion.
- [x] Independent vertical and horizontal perspective correction, with guide grid.
- [x] Red/cyan and blue/yellow alignment before the color profile.
- [x] Localized purple/green defringe with luminance preservation and neutral-edge gating.
- [x] Automatic edge trimming, editable crop, rotation-aware perspective axes,
  saved sessions, per-parameter reset, and accessible controls.
- [x] Apply valid embedded Fujifilm lens distortion tables automatically at load,
  in previews, 100% detail, crop/mask mapping, and full-resolution export.
- [ ] External lens profiles for files without usable embedded distortion data;
  no profile is guessed from a lens name.

The manual controls are implemented and verified in preview and full-resolution export.
The manual radial coefficient adds fine tuning on top of the embedded correction.
External calibration profiles and fisheye models remain pending.

## Curves, grain, and selective edits — items 8, 19, 21

- [x] **8. Tone curve:** master and separate RGB channels, editable control points,
  direct in-tab gestures, double-tap point deletion, channel reset, grey Master,
  RGB circle pickers, square graph geometry, and screen-reader point actions.
- [x] **19. Grain:** amount, size, and roughness; independent of Fujifilm's grain engine.
- [x] **21. Selective adjustments:** brush paint/erase, linear-gradient, and radial
  masks, with local exposure, shadows, highlights, temperature, tint, contrast,
  and saturation. Includes enable/invert, opacity/feather, overlay, undo/delete,
  and accessible shape controls.

Masks follow the original photo through crop, rotation, straighten, and optics.
All new edits use sensor RAW pixels with the recorded color profile, persist with
the session, and apply to full-resolution export. Eight masks and 4096 stored
brush points per photo bound edit data; brush coverage uses a 1600 px source grid.
Fine grain and very small brush edges require judging the full-resolution export.
See [pipeline notes](raw-editor.md) for the limits.

## Remaining proposed controls

- [ ] **1. Film simulation selector:** switch between the eleven bundled profiles.
- [ ] **3. White balance eyedropper:** sample a neutral surface in the photo.
- [ ] **9. Color mixer / HSL:** hue, saturation, and brightness by color range.
- [ ] **10. Color grading:** shadows, midtones, and highlights, with balance/blending.

## Companion tools

- [ ] Histogram.
- [ ] Highlight and shadow clipping warnings.
- [x] 100% zoom for judging detail and noise.

## Verification record

Baseline: 728 JVM tests and native color checks pass. Real landscape and portrait
X-T50 RAFs pass phone-side profile selection, shadow lift, reset, full-resolution
export, and rejected replacement checks. Compact/wide layout tests pass.
Full lint is blocked by the existing `SlotDetailSheet.kt:332` configuration-aware
resource lookup error. See [the pipeline notes](raw-editor.md) for existing limits.

First batch delivered on 2026-10-24. All 728 JVM tests and native color/filter checks
pass. On the wireless SM-F976B, the `DSCF3946.RAF` test verifies each new control
changes RAW pixels, Reset reproduces the starting point, all sixteen saved values
restore into an identical preview, and the combined controls export a full-resolution
JPEG. The same checks also pass with the portrait `DSCF3979.RAF` from the phone.
Rejected replacement still preserves the image and settings. The landscape/UI run
completes in 38 seconds and the portrait run in 40 seconds; compact/wide UI checks cover the Light, Color, and Detail
panels, scrolling to every new control, As Shot reset, and persistent Save placement.

Temperature/tint are relative to the recorded camera WB, not an invented absolute
Kelvin reading for Auto WB. Noise reduction uses a local edge-aware bilateral filter;
Detail adjusts how strongly edges are preserved. Sharpening
uses luminance unsharp masking with a 0.5–3 px full-resolution radius and threshold.
Detail filtering uses overlapping strips with bounded extra memory and a native
regression test for seams. Preview radii account for the downsampled image, but the
fitted preview cannot replace judging full-resolution pixels; the editor now provides a 100% detail view.
Full lint remains blocked by the same pre-existing camera-screen error.

Gesture update on 2026-10-24: debug and test APKs build, and all 728 JVM tests pass.
Wireless SM-F976B UI checks cover isolated resets for all sixteen parameters,
single-tap and busy-state protection, inactive detail resets, hold/release/cancel
comparison, screen-reader actions, and stable compact/wide layouts.

Items 14–18 delivered on 2026-10-24: all 730 JVM tests and native checks pass.
Checks cover old-session migration, 2,000 repeated crop moves/resizes, normalized
bounds and aspect locking, full-resolution square presets at every quarter-turn
and multiple straighten angles, effect direction/neutrality, finite output, and
seam-free strip processing with all effects enabled. Wireless SM-F976B UI checks
cover the new controls, all 25 scalar resets, crop dragging, preset locking,
accessible movement/resize, Apply/Cancel/Reset, and stable compact/wide layouts.
The landscape `DSCF3946.RAF` run passes individual positive/negative effects,
uncropped crop-editor preparation, full-resolution square framing, identical
29-value session restoration, full framed export, and rejection safety in 79 seconds.
The same four device tests pass on portrait `DSCF3979.RAF` with the final crop
boundary fix in 88 seconds, including export after rotating the portrait to landscape.
Full lint still has the existing `SlotDetailSheet.kt:332` error; the editor has no
lint errors. See [the pipeline notes](raw-editor.md) for processing and crop limits.

Item 20 manual corrections delivered on 2026-10-24: all 731 JVM tests and native
checks pass. Native checks cover unclamped source bounds across 324 extreme
correction/rotation/straighten combinations, synthetic lateral fringe alignment,
purple/green fringe removal with luminance preservation, flat-color protection,
and strip seams with all effects enabled. UI checks pass in 13 seconds for Optics
controls, all 32 scalar resets, grid/comparison behavior, and compact/wide layouts.
The real landscape `DSCF3946.RAF` check passes positive/negative distortion,
perspective and channel alignment, reset, identical 36-value session restoration,
square crop on the corrected canvas, full-resolution export, and rejected replacement
in 76 seconds. Automatic calibration profiles remain unchecked above.
The same real-RAF checks pass on portrait `DSCF3979.RAF` in 72 seconds. Debug APKs
build successfully; full lint still stops at the existing `SlotDetailSheet.kt:332`
error, with no RAW Editor lint errors.

Items 8, 19, and 21 delivered on 2026-10-24: all 732 JVM tests and native checks
pass. Checks cover curve interpolation/channel isolation/identity, tightly spaced
control points, deterministic grain, mask coverage/erase/invert/opacity/enable,
source anchoring, malformed payload rejection, and old-session migration. Four
wireless SM-F976B UI tests pass in 14 seconds, covering curve drawing and point
actions, grain resets, all mask types, brush gestures/undo/delete, accessible
shape edits, and compact/wide/short-landscape layouts. The supplied landscape
DSCF3946.RAF passes control effects, overlays, reset, identical restoration, and
full-resolution framed export (3278 × 5248) in 70 seconds. Portrait DSCF3979.RAF
passes the same checks plus actual drawing through crop/rotation and repeating an
unchanged gradient without stalling, exporting at 4920 × 3496 in 102 seconds.
Debug APKs build; the only lint error remains the existing SlotDetailSheet.kt:332
resource-lookup error. Grain and mask adjustments reuse the current carousel and
tick dial; sharing and customizable navigation files are unchanged.

Curves UI refinement: curves appear directly in the tab, with grey Master and
colored RGB circle pickers. Touch the line to add, drag to move, and double-tap
an interior point to delete. Visible input/output, previous/next, and add/remove
controls are removed; screen-reader graph actions retain equivalent operations.
All four phone UI checks pass in 14 seconds, including fast drags from the initial
finger-down position and deletion without affecting another color channel.

Preview responsiveness improved on 2026-10-24. The prior 120 ms restarting
preview debounce delivered zero frames during a continuous exposure drag on the
wireless SM-F976B; the first frame appeared after 2484 ms. The renderer now takes
the latest pending settings, displays completed drafts while input continues,
and refines after 160 ms idle. Normal edits use an 800 px sensor preview; active
detail filters use 400 px. Settled previews remain 1600 px and JPEG exports still
decode the original RAW at full active resolution. Tick dials also retain small
movement fractions instead of rounding them away on every event.

On the same landscape DSCF3946.RAF, the final exposure check starts at 58 ms and
shows 34 frames during the roughly two-second drag; effects plus a radial mask
start at 53 ms and show 37 frames. Native rendering is 56 ms at 800 × 533 for
exposure, compared with 226 ms at 1600 × 1066; the effects draft takes 52 ms at
400 × 266, compared with 954 ms at 1600 × 1066. Portrait DSCF3979.RAF starts at
61/54 ms and shows 33/36 exposure/effects frames, then refines to 1066 × 1600.
Final refinement takes roughly 0.4 s for exposure and 1.1–1.2 s with the combined
effects; the adjusted draft is displayed during that work. This remains a CPU
renderer rather than a guaranteed 60 fps engine.

All 732 JVM tests and native checks pass, including odd-sized sensor averaging.
Real-RAF checks verify both preview scales, matching mask overlays, latest saved
settings after rapid input, exact settled-preview restoration, reset, rejected
replacement safety, and full-resolution framed JPEG export. The final portrait
hardware run passes both tests in 88 seconds, exporting at 4920 × 3496. Landscape
full export was also verified with the preview worker. Test RAFs available here
are compressed 40 MP files of 41.5 and 42.0 MB; an 80 MB file was not available.
After opening, preview rendering uses fixed-size caches without reading the RAF.
Five phone UI tests pass in 16 seconds, including slow sub-step dial movement,
compact/wide layouts, curve gestures, individual resets, and camera comparison.
Full lint still has only the existing SlotDetailSheet.kt:332 error; no RAW Editor
lint errors were introduced.
The final portrait JPEG is byte-for-byte identical to the saved pre-optimization
export from the curves/grain/masks delivery: both have SHA-256
`abda171f57d299bc99e55f864285b160a2843544ec577b7effef5d801928e7dc`.


100% zoom delivered on 2026-10-24. Pinch to zoom, drag to pan, and double-tap
or tap the Fit badge to switch between Fit and 100%. At 100%, one rendered RAW
pixel occupies one physical screen pixel. Hold still on the image to compare
with the fitted camera JPEG; release returns to the same RAW zoom and position.
Framing changes reset zoom to Fit. Zoom is a viewing aid and does not change
saved edits or export dimensions.

The detail view decodes the original full-resolution sensor once while zoomed
and renders only the visible region with the export pipeline. Tiles include
filter context on all sides. The app labels pending detail explicitly while
showing the fast preview underneath. Returning to Fit normally releases the
full decode; a crop small enough to fit at 100% retains its detail view.
The first full decode of the phone's 41,998,640-byte portrait X-T50 RAF took
about 11 seconds. Cached 256 × 192 regions took 8 ms without detail filters and
70–99 ms with combined effects. Real-RAF tests found zero channel differences
against full-resolution export, including image edges, optics, masks, grain,
and detail filters; fractional zoom regions also match a whole scaled render.
The edit/pan queue test passes and preserves continuous slider preview updates.
All 733 JVM tests and native color checks pass. Full lint still has the existing
camera-screen error described above.

The six RAW Editor gesture/layout tests pass on SM-F976B, including compact/wide
controls and hold/release/cancel comparison. The combined-controls portrait
export remains 4920 × 3496 and byte-identical to the pre-zoom reference:
`abda171f57d299bc99e55f864285b160a2843544ec577b7effef5d801928e7dc`.


Automatic embedded lens distortion correction delivered on 2026-10-24. The
starting point reads the RAF's signed-rational `FujiIFD:GeometricDistortionParams`
(`0xf00b`) through LibRaw's existing EXIF callback. It uses the recorded source
radius/displacement table and a frame-filling scale, without a lens-name lookup
or changing saved adjustment values. Reset and individual Distortion reset keep
the automatic baseline; manual Distortion remains an additional adjustment.
Optics reports whether usable embedded data was applied. Missing, malformed,
folding, or unverified normalization tables leave manual correction available.

Validated with the user's `DSCF3946.RAF` (X-T50, Tamron 18–300 mm, 64.3 mm).
Against its embedded camera JPEG, 2450 shared SIFT features at 1200 × 800 gave
median similarity-aligned residuals of 1.27 px before and 0.36 px after correction;
the 90th percentile fell from 2.62 to 0.89 px. This measures geometric agreement,
not an identical rendering of Fujifilm color or sharpening. The on-open flag and
corrected 100% tile/export pixel checks pass on SM-F976B. Native checks cover
source-table inversion, both distortion signs, sensor-edge coverage, rotation,
manual corrections, preview scaling, and malformed data. Six device UI checks
and all 733 JVM tests pass.

With automatic distortion enabled on this landscape RAF, 800 px exposure drafts
render in 83 ms and 400 px effects drafts in 69 ms. During continuous input the
phone delivers 21 exposure and 25 effects frames before the gesture ends; the
first frames arrive in 100 and 77 ms. Full lint still reports only the existing
`SlotDetailSheet.kt:332` error.

The final real-RAF workflow also passes with automatic correction enabled:
all control effects, Reset to the corrected baseline, crop/mask drawing and
overlays, exact saved-session restoration, full-resolution JPEG export, and
rejected replacement preserving the active image. Both device workflow tests
pass together; the on-open and corrected tile/export checks pass separately.


Tone control grouping: Highlights opens two independent sliders, Highlights and
Whites; Shadows opens Shadows and Blacks. All four retain their existing values,
processing, session storage, and individual double-tap resets. Selecting a group
or double-tapping its option does not change parameters. The group indicator
reflects edits to either slider. Exposure and Contrast remain separate options.

Verified on 2026-10-25: all 733 JVM tests and seven SM-F976B UI tests pass.
The paired-slider check covers compact and wide layouts, independent changes,
name double-tap resets, unchanged saved values on group selection, and persistent
Save access. The updated debug APK is installed on the phone.


The preview corner Crop button opens the framing workspace directly. The
header has separate Rotate left 90° and Rotate right 90° actions. Straighten
remains an independent dial below the photo in compact and wide layouts; its
label supports double-tap reset. Crop handles, aspect presets, framing Reset,
Apply, and Cancel remain available. A denser grid helps judge straightening.
Rotation carries the current crop and aspect lock into the new orientation,
and replacing an asynchronously rendered preview preserves crop edits.

Framing edits are staged separately from saved/global adjustments. Previews
use the existing conflated sensor-preview worker; Apply commits the selected
crop, rotation, and angle together. Cancel restores the previous RAW preview
and saved settings, including if the editor opened during a pending preview.

Verified on 2026-10-25: all 733 JVM tests and eight SM-F976B UI tests pass.
Framing checks cover direct entry, 90° turns in both directions, crop retention
across preview replacement, locked 3:2 → 2:3 rotation, Straighten below the
photo in compact/wide layouts, staged Cancel/Apply, and disabled crop/aspect input
while the new preview is pending. The real X-T50 RAF workflow confirms staged
rotation/straightening previews, unchanged saved edits until Apply, and a
byte-identical return to the pre-framing preview on Cancel.


RAW Editor control organization (2026-10-25): Light (Exposure, Contrast,
Shadows, Highlights), Color (As shot, Temperature, Tint, Saturation, Vibrance),
Details (Sharpening, Noise Reduction, Preserve Detail), Curves, Effects (Grain,
Clarity, Texture, Dehaze, Vignette), and Optics (Distortion, Perspective,
Defringe, Color alignment). Masks remains available after Optics.

Two-parameter options show two independent dials: Shadows/Blacks,
Highlights/Whites, Sharpening/Radius, Color/Luminance noise reduction,
Vertical/Horizontal perspective, Green/Purple defringe, and Red/Cyan plus
Blue/Yellow alignment. Grain selects Amount, Size, or Roughness above one dial;
Vignette selects Amount, Midpoints, Roundness, or Feather. The selector uses
Unfold more, and each parameter name retains its own double-tap reset, including
inactive size/detail settings. Selecting a parameter never changes its value.
All option icons use the requested Google Material Symbols Rounded definitions
and support the existing global 300/400/500 icon weights.

Framing is no longer a category. Screenshot frame 2 is an always-visible Crop
button at the preview's bottom-right, including during camera-JPEG comparison.
Apply and Cancel preserve the selected editor category. Older saved sharpening
Threshold values remain intact in processing/session data; the requested
Sharpening option exposes only Sharpening and Radius.


Verified on 2026-10-25: all 733 JVM checks and nine SM-F976B UI checks pass.
The UI suite asserts exact group/option order, independent values and resets,
all Grain/Vignette selector choices, paired sliders without selectors, compact
and wide layouts, camera-JPEG comparison, 100% zoom, curves/masks, and staged
framing from the persistent preview button. The updated debug APK is installed; the selector and preview Crop button were
also visually checked with the real DSCF3979.RAF open on the phone.
Lint remains blocked only by the pre-existing LocalContextGetResourceValueCall
error at SlotDetailSheet.kt:332.


Crop ratio menu (2026-10-25): a single header control shows the current preset
and opens the existing app-wide Material 3 menu. Free, Original, square,
landscape, and portrait presets remain available, with a selected checkmark.
The menu scrolls within short windows; selecting an item closes it. Original
remains distinct from an equal fixed ratio, fixed presets follow quarter-turn
rotation, and Reset returns to Free. The chip row below the photo is removed.


Verified on 2026-10-25: the APK builds and all nine SM-F976B UI checks pass
in 26 seconds. Crop checks cover scrolling to portrait/wide presets, selected
preset labels, Original remaining distinct from a matching fixed ratio,
rotation updating fixed ratio orientation, disabled selection during pending
rendering, Reset to Free, and Apply/Cancel in compact and wide layouts.


Material parameter sliders (2026-10-25): the custom Canvas tick dial is removed.
Global parameters, Straighten, and mask/shape controls share the library's XS
Slider (16 dp track, 44 dp handle with a 48 dp touch target). The library draws
step markers, handles input, and shows the formatted value in a floating Label
while pressing or dragging. Signed ranges use Material's centered track.
Steps match displayed precision: 0.1 EV, pixels, or degrees; one point for
percentage controls. Brush size shows one decimal percentage point so its
0.5% lower bound remains selectable. Parameter names keep individual double-tap
reset, and each slider exposes its own name, range, steps, and accessible reset.

Option selection occurs on a completed tap without waiting for the double-tap
window. Double-tap resets remain isolated, and swiping the option carousel does
not select the option where the swipe began.


Verified on 2026-10-25: debug APKs build and all nine SM-F976B UI checks pass
in 30.5 seconds. Checks cover step counts and rounding, floating value visibility
during drag, accumulated fine movement, disabled input, isolated name resets,
instant option selection, paired controls, masks, and compact/wide framing.
The updated debug APK is installed on the phone.


Wide editor panel (2026-10-25): at the existing 700 dp breakpoint, the right
pane uses its full height. Category tabs stay at the top; every slider in the
selected category appears in a vertically scrolling list, with the option icon
beside its title and its current value at the trailing edge. Grouped parameters
appear as individual sliders, including all Grain and Vignette controls, with
context in their titles. As shot stays available in Color; Curves and Masks
remain accessible. Crop and zoom remain on the photo preview.

Compact layouts retain the option carousel, paired sliders, and selectors.
Resizing preserves the selected category and all adjustment values. Each
category owns its list scroll state. Individual name double-tap resets,
Material value indicators, and accessible slider actions continue to work.


Verified on 2026-10-25: debug APKs build. The existing nine UI checks passed;
the dedicated wide-panel check passes at 840 × 800, 1200 × 800, and 840 × 393 dp.
It covers every category's slider order, icon/title placement, fixed tabs,
scrolling to the final Vignette control, isolated reset, Curves/Masks access,
persistent Save/Crop, and preserved category/values when returning to compact.
A later combined run passed the new wide check but lost the reset test's Compose
window while the device changed display/foreground state; it is not recorded
as a clean full-suite pass. The updated debug APK is installed on the SM-F976B.


Curve geometry and category swipes (2026-10-25): the curve plot keeps a 1:1
aspect ratio. Wide panels use their available width; compact screens cap its
size relative to the editor height so the photo and tabs remain usable.
Channel controls and the gesture hint remain outside the graph.

Horizontal swipes on unused control-panel space navigate adjacent categories
in every tab, in compact and wide layouts. A completed swipe changes one tab;
the first and last categories clamp instead of wrapping. Vertical scrolling,
slider dragging, curve editing, and preview gestures keep their own input.
Navigation is disabled while the editor is busy, matching the category tabs.

Verification: debug and instrumentation APK builds passed; all 11
RawEditorLayoutTest checks passed on the connected SM-F976B. The new check
swipes through all seven categories in both directions at compact and wide
sizes, checks boundary/busy behavior, preserves slider/curve editing and
vertical scrolling, and measures square curve geometry including short wide
windows. These are UI checks with an in-memory image, not a new RAF processing
or export test. The updated debug APK is installed on the device.

Navigation and RAW Editor identity (2026-10-25):

- [x] Use the Material Symbols Rounded `raw_on` icon for RAW Editor in the
  bottom bar, sidebar, More, pin chooser, and editor landing panel. All three
  global icon weights use the official Google Fonts vector definitions.
- [x] Reorder sidebar destinations vertically in navigation edit mode;
  compact navigation retains horizontal reordering. Long-press More to edit.
- [x] Save separate pin lists for compact navigation (up to four destinations)
  and the sidebar (up to eight), with layout-specific counts and instructions.
  The sidebar inherits existing pins until first customized. More and selected
  destination handling use the pin list for the currently displayed layout.
- [x] Keep sidebar destinations scrollable when height is limited and preserve
  access to More and the camera footer. Short wide windows retain the compact
  bar and its saved pins when the sidebar cannot fit.

Verification: debug and instrumentation APK builds passed; all eight
UiPreferencesTest checks passed, including persistence, migration fallback,
independent pin orders, normalization, and four/eight limits. All 15 device UI
checks passed on the connected SM-F976B: one navigation check, three camera
placement checks, and eleven RAW Editor layout checks. Navigation coverage
includes vertical and horizontal reordering, pin limits, More access, folding
between sizes, and the scrolling sidebar at 840 × 600 dp. The updated APK is
installed; user pin selections and photo data were preserved.

Finger-following category transitions (2026-10-25):

- [x] Replace release-only category switching with Compose HorizontalPager in
  compact and wide layouts. The current page follows the finger while the
  neighboring category slides into view; native paging handles velocity,
  settling, short-drag snap-back, and the first/last page bounds.
- [x] Animate category-tab taps through the same pager. The category row stays
  fixed, and the photo preview retains comparison, zoom, and pan.
- [x] Preserve slider and curve drags, vertical list scrolling, and option-row
  scrolling. The option carousel consumes remaining horizontal scroll/fling
  movement so it cannot hand a drag to the category pager at its boundary.
- [x] Exclude cached off-screen page controls from the accessibility tree;
  each category owns its parameter selection and list scroll state.

Verification: debug and instrumentation APK builds passed, and all eleven
RawEditorLayoutTest checks passed on the connected SM-F976B. The motion check
measures outgoing and adjacent page positions with the finger held down,
checks that category tabs stay fixed, verifies short-drag snap-back and
animated tab taps, and navigates all categories in both directions at compact
and wide sizes. Slider/curve editing, carousel scrolling, vertical scrolling,
busy/end bounds, framing, zoom, comparison, and resets also pass. The updated
APK is installed; image processing and export code are unchanged.

Horizontal wheels and stable parameter titles (2026-10-25):

- [x] Give parameter titles and selectors the same fixed 48 dp title area.
  Switching between Grain/Vignette selectors and single-parameter Effects
  options keeps the preview bounds unchanged. Each parameter remains
  independently adjustable, including the paired controls.
- [x] Replace Material XS sliders with the shared RawParameterWheel in the
  compact editor, wide lists, framing, and mask controls. A stationary center
  marker selects from a horizontally scrolling ruler; dragging left increases
  the value. Ticks and major-value labels move underneath the marker.
- [x] Use native LazyRow scrolling with deceleration and center snapping.
  Repeated small movements retain their fraction until they cross a parameter
  step. Tapping does not jump to an absolute position. Values keep the existing
  precision and range limits.
- [x] Keep name double-tap resets and accessible range/reset actions. External
  resets stop an active fling, disabled controls do not edit, and keyboard
  arrows/Home/End adjust the focused wheel. Shared horizontal scroll boundaries
  keep wheel and option-row movement from switching categories at range ends.

Verification: debug and instrumentation APK builds passed. All twelve
RawEditorLayoutTest checks passed on the connected SM-F976B, covering compact
and wide layouts, unchanged Effects preview bounds, independent paired
controls, wheel precision, momentum, reset during a fling, disabled input,
keyboard/accessibility actions, and category gesture ownership. The app-only
capture confirms the fixed center marker and labeled ruler. The updated APK
is installed; image processing and export code are unchanged.

Paired wheel layout (2026-10-25):

- [x] Place compact two-parameter options in one row, with equal half-width
  columns and independent titles, wheels, values, and resets. Applies to
  Highlights/Whites, Shadows/Blacks, Sharpening/Radius, Color/Luminance noise,
  Perspective, Defringe, and Color alignment. Single parameters and selectors
  retain the same title/wheel height; the wide scrolling list is unchanged.

Verification: debug and instrumentation APK builds passed, and all thirteen
RawEditorLayoutTest checks passed on SM-F976B. The paired-layout check covers
all seven pairs, equal half-width columns, matching vertical bounds, and
unchanged preview bounds when switching from single to paired controls.
Independent values and resets continue to pass in compact and wide layouts.
The updated APK is installed.

Comparison sources (2026-10-25):

- [x] Add a circular comparison control at the preview's bottom left, matching
  Crop at the bottom right. Choose Embedded JPEG or Before edits from its menu;
  selecting a source displays it, and tapping the control again returns to the
  edited RAW. Holding the preview compares against the last selected source.
- [x] Show comparison status as a rounded indicator at the preview's top
  center, labeled only Embedded JPG or Original for the selected source.
- [x] Retain the initial RAW rendering before user adjustments, including the
  film simulation color profile and automatic lens correction. Reuse the first
  preview for a fresh session; recreate the neutral starting render when
  reopening with saved adjustments. Editing and framing do not replace it.

Verification: debug and instrumentation APK builds passed. The thirteen
existing layout checks passed on SM-F976B; the new comparison check passed
separately after allowing pixel rounding in opposite button insets. It checks
compact/wide button placement, both menu sources, actual displayed pixels,
hold/release behavior, unchanged edits, and stable preview bounds. A real
DSCF3946.RAF check passed for initial-render sharing, retained pixels after
exposure/crop edits, and recreating the neutral image when reopening with
saved adjustments. The updated APK is installed.

The shortened comparison indicator also passed the focused compact/wide UI
check for its exact text, top inset, centered position, and disappearance on
release. The existing rendering/comparison stability check passed as well.
