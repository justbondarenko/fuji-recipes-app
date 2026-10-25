# RAW Editor

The separate More → RAW Editor section develops sensor RAW on the phone. The recorded
film simulation's color profile and as-shot white balance are applied before the first
editable preview. The original RAF is never modified. Lab retains camera-engine development.

The pipeline is RAF → LibRaw demosaicing in unscaled sensor RGB → optical/geometry
correction and framing/bilinear resampling → floating-point as-shot
WB plus global/masked relative temperature/tint → camera matrix → dynamic-range exposure compensation
→ global and masked exposure/shadow/highlight adjustments → sRGB encoding → the simulation's published
3D color LUT → global/masked contrast and saturation, whites/blacks/vibrance → master/RGB curves → defringe → dehaze → edge-aware luminance/chroma
noise reduction → texture/clarity → luminance sharpening → vignette → grain → display/JPEG.
Adjustments always re-render from retained linear RAW pixels. The embedded JPEG supplies
MakerNote metadata and an explicitly labeled camera comparison, never editable pixels.

The eleven color simulations have bundled profiles. Unknown simulations and monochrome
variants fail explicitly instead of silently displaying an unprofiled image. These are
camera-matching color profiles, not Fujifilm's complete proprietary renderer. Grain, Color
Chrome, the camera's sharpening/NR settings, clarity, and the other recipe
effects are not replicated. Independent sharpening and NR controls are available in Detail.

Previews use half-size demosaicing and retain linear sensor samples at a 1600-pixel
long edge. Slider edits first render an 800-pixel draft (400 pixels with detail filters),
then refine to 1600 after
160 ms without a newer request. Export reopens the original
RAF and demosaics at the full active crop, with the same profile and adjustments. It writes
a completed JPEG in private cache before copying it to the user-selected SAF document.
Exposure is ±3 EV; shadows and highlights provide a weighted ±2 EV adjustment. Clipped
sensor values cannot be recovered. There is no network or camera dependency at runtime.

LibRaw 0.22.2 is bundled as the unmodified, SHA-256-checked source archive under CDDL 1.0.
Profiles are the unmodified sRGB `.cube` files, gzip-compressed for storage, from
https://github.com/abpy/FujifilmCameraProfiles at
`b5a1f16abfb2759b337c83d69d9dc49470e7e518`, under CC BY-NC-SA 4.0. The profile assets
retain that license and attribution; they are not covered by the app code's MIT license.
They cannot be sold or used commercially under this license. Photos remain the user's work.
Licenses and the upstream profile README are bundled under `assets/licenses/`.

Build requires NDK 28.2.13676358 and CMake 3.22.1 (Gradle installs these with accepted SDK
licenses). Run `sh scripts/check-raw-color.sh`, `:app:testDebugUnitTest`, `:app:assembleDebug`,
and `:app:lintDebug`. Hardware checks use real X-T50 RAFs: open, recorded-profile selection,
shadow lift, reset, camera comparison, full-resolution JPEG export, replacement failure,
rotation/process restoration, and the discard prompt. No sample photos enter the repository.

Verified on 2026-10-24: all 728 JVM tests and the native color checks pass. On the wireless
SM-F976B, `DSCF3946.RAF` (landscape) and `DSCF3979.RAF` (portrait, from Pictures/FUJIFILM
X-T50) automatically selected Classic Chrome, lifted shadows, reset identically, exported
full-resolution JPEGs, and retained the open image/adjustments after a rejected replacement.
LibRaw's aligned X-Trans active crop exports at 7728 × 5149 (or 5149 × 7728), slightly
shorter than the camera's 7728 × 5152 JPEG dimensions. The device layout regression test
passes at 393 dp and 840 dp: progress and comparison do not move the preview or controls.
`lintDebug` remains blocked by the pre-existing `LocalContextGetResourceValueCall` error
in `ui/camera/SlotDetailSheet.kt:332`; the editor has no lint errors. Process-death restoration
is implemented through SavedStateHandle and the persisted SAF grant, but has not been
independently forced and verified on hardware. No camera-engine operation was used.

The first expanded control batch is tracked in [raw-editor-features.md](raw-editor-features.md).
Light, Color, and Detail panels share the same saved adjustments and native processing in
preview and export. The white-balance cache retains camera-space samples so WB edits happen
before the camera matrix. Temperature and tint are relative to As Shot. Zero adjustments
keep the original color profile as the starting point. Detail operations use overlapping
96-row strips; additional filter memory does not scale with the whole image height.
The expanded phone test verifies each control, identical restoration from SavedStateHandle,
reset, rejection safety, and full-resolution export with the combined controls enabled.

Items 14–18 add Effects and Framing panels. Clarity uses midtone-weighted local
contrast at a 40 px full-resolution radius; texture uses a 6 px radius. Both can
soften or enhance. Dehaze uses a dark-channel transmission estimate with sampled
airlight and a smoothed local minimum; negative amounts add atmospheric haze.
The model is based on [He, Sun and Tang's dark-channel paper](https://people.csail.mit.edu/kaiming/publications/cvpr09.pdf).
This is a conservative manual adjustment with bounded transmission, not a depth
reconstruction. Smoothed transmission can leave halos at strong depth boundaries;
guided refinement is a possible later improvement. Filter halos accumulate across
all enabled operations and native tests compare strips with whole-image results.

Framing stores normalized crop bounds, clockwise quarter-turns, and a ±15° angle.
Straightening takes the largest centered inset with the original aspect ratio that
avoids empty corners, then applies the crop. The crop dialog renders the full
uncropped, rotated/straightened preview, supports dragging corners and moving the
selection, locks aspect presets, and exposes accessible movement/resize actions.
Presets use full-resolution dimensions rather than rounded preview dimensions.
Crop selections retain at least 5% of each image dimension. Rotating also rotates
the selection so it follows the same subject. Geometry samples the source directly
into the final output, avoiding a second full-resolution bitmap allocation.
Vignette is centered on the framed output and supports darkening/brightening,
midpoint, rectangular/elliptical/circular roundness, and feathering.

The saved parameter array expands from 16 to 29 values; earlier sessions upgrade
with neutral effects and framing. All 25 scalar controls retain label double-tap
reset, including inactive vignette shape controls. Crop is applied or cancelled as
a group and has its own reset. The original camera JPEG comparison stays unframed.

Item 20 adds an Optics panel for manual corrections; no calibration profile is inferred
from a lens name. Distortion uses the first radial coefficient, normalized by the
upright image's half diagonal, with k1 in ±0.25. Vertical/horizontal perspective
use an inverse projective mapping with bounded coefficients (±0.35). These follow
the standard [radial camera model](https://docs.opencv.org/4.x/d9/d0c/group__calib3d.html)
and [destination-to-source mapping](https://docs.opencv.org/4.x/da/d54/group__imgproc__transform.html);
OpenCV is not a dependency. A conservative global bound accounts for perspective,
straightening, radial expansion, and every color channel when trimming empty edges.
Crop bounds remain normalized on the corrected canvas. Rotating the image also
rotates its perspective axes so the correction stays attached to the photo.

Red/cyan and blue/yellow alignment resample the red and blue camera channels
independently around the optical center, with ±0.2% radial magnification, before
white balance, the camera matrix, and the simulation profile. Purple/green defringe
then reduces the selected chroma while preserving luminance only where nearby
neutral dark and bright pixels establish a high-contrast edge. Flat colored regions
are unchanged. This conservative gate does not handle fringes against every colored
background; a hue-range tool is a possible extension. Full-resolution export is
needed for judging fringes smaller than a fitted preview pixel.

All seven Optics controls are neutral by default and share label double-tap reset
and accessible actions. Sessions expand from 29 to 36 values, with sixteen- and
twenty-nine-value sessions migrated using neutral corrections. External lens
calibration profiles and fisheye models remain pending. Embedded
Fujifilm distortion calibration now applies independently of the manual controls
as described below; recipe metadata alone is not calibration.


Items 8, 19, and 21 extend the current compact carousel and tick dial. Curves
appear directly in their tab; the Masks editor has a fitted photo and scrollable
controls. Both adapt to short landscape screens. Existing sharing, navigation,
Save, double-tap scalar resets, and hold-to-compare remain in place.

Tone curves appear directly in the Curves tab with master, red, green, and blue
channels, each holding up to sixteen control points. Master is grey; the RGB
channel pickers are colored circle buttons with screen-reader labels. Touching
the curve line adds/selects a point and dragging moves it; double-tapping an
interior point removes it. Input endpoints stay at 0 and 1, and double-tapping
an endpoint resets its output to the identity value. There are no visible point
navigation, add/remove, or input/output slider controls. Screen-reader custom
actions on the graph retain point navigation, adding/removing, moving, and reset.
The reset icon clears the current channel; global Reset clears all edits.
Conservative cubic Hermite interpolation stays inside each segment's control
values; native processing uses a 1,025-entry table. Identity curves bypass
processing exactly. Curves act after the recorded simulation profile and color
adjustments.

Grain in Effects has amount, full-resolution size (0.5–10 px), and roughness.
It adds deterministic, luminance-weighted monochrome noise after detail filtering
and vignette. Preview and export use the same full-resolution output coordinates;
the fitted preview averages four samples when downscaled. Fine grain still needs
the editor’s full-resolution 100% view to judge. This is independent grain, not a
reproduction of Fujifilm's proprietary grain settings.

Masks support brush paint/erase, linear gradients, and radial ellipses. Up to eight
masks combine weighted exposure, shadows, highlights, temperature, tint, contrast,
and saturation. Every mask has enable, invert, opacity, feather, and delete;
brushes also have radius and stroke undo. The selected mask has a toggleable
colored coverage overlay. Drag places/replaces gradients; brush taps paint dabs.
Shape coordinates and a center-dab action provide accessible editing alternatives.
Shape coordinates refer to the original upright photo. Local exposure and WB
operate before the matrix/profile, so they retain sensor RAW recovery.

Drawing coordinates map through the current crop, rotation, straighten, and
optics into normalized original-image positions. Masks therefore stay attached
to the subject when framing changes. Linear and radial coverage are analytic;
brush coverage uses the retained preview-size source grid (maximum 1600 px long
edge), shared by preview and export. This deliberately bounds memory and gives
soft, stable brush edges; tiny full-resolution brush retouching would require a
tiled full-resolution mask grid. A mask holds at most 64 strokes and a stroke
holds 256 sampled points, with 4096 points total per image. The UI bounds temporary
stroke feedback too and reports reaching the stored-point limit without losing
edits. Grain, curves, and masks persist in a versioned, bounded float payload;
old sixteen-, twenty-nine-, and thirty-six-value sessions upgrade unchanged.


Preview responsiveness uses a single StateFlow consumer with conflated requests.
A completed draft is displayed even while a newer adjustment is pending; only the
latest settings get the settled refinement. This replaces the previous 120 ms
restart-on-every-change debounce, which could suppress every frame during a drag
and discard native renders that had already done their work. Opening a different
RAF or starting export invalidates preview requests; source generations prevent
an old photo's result from replacing the current one. Errors from superseded
requests do not override the newest state.

The 800-pixel draft contains one quarter of the retained preview's pixels (about
0.43 MP for an X-T50 3:2 shot). When detail filters are enabled, a 400-pixel draft
keeps their neighborhood processing responsive (about 0.11 MP). All levels
contain averaged linear sensor RGB, then use the same camera WB, matrix, simulation profile, and edit pipeline.
None of the preview levels edits the embedded JPEG. The smaller caches are built once when the
RAF opens; sliders perform no RAW file reads or demosaicing. Detail radii and mask
overlays use the current preview scale; settled masks and export retain the
1600-pixel coverage grid. Export explicitly rejects draft mode and decodes the
original RAF at full active resolution, applying the saved crop and all edits.
The CPU renderer still finishes an in-flight frame before taking newer settings;
GPU processing or native interruption is deferred unless measurements require it.

Tick dials accumulate an unsnapped value during a gesture and round only the
emitted parameter. Previously, repeated sub-step movements rounded back to the
same value and were lost, making slow exposure or color adjustments feel stuck.
This fix applies to every global and mask dial through the shared component.


The main image supports pinch zoom and pan, bounded between Fit and 100%.
Double-tap or the Fit/percentage badge toggles Fit and 100%; accessibility actions
also expose zoom and four pan directions. Image pixels map to physical display
pixels at 100%, independently of Android density. Geometry changes and opening
another RAF reset the view; ordinary edits preserve it. Camera comparison is a
stationary long press so pinch and pan do not flash the JPEG. Comparison retains
the fitted original JPEG and restores the previous RAW zoom on release.

Detail requests share the serialized, conflated preview worker. After input
settles, a full sensor decode is cached and only the visible output rectangle
is rendered. The renderer is shared with export, preserving source mapping,
profile, adjustments, grain coordinates, vignette, and masks. Horizontal and
vertical filter halos keep viewport boundaries consistent with export. A
pending tile is labelled, and stale edits/scales never masquerade as current
full detail. CPU tiles are limited to 4096 pixels per side; the full sensor
cache uses hundreds of MB and is released on Fit, framing reset, session close,
and before export. A small crop already fitting at 100% still requests detail.
Tiled sensor decoding is deferred for devices that cannot retain a full decode.

Zoom is transient UI state, separate from saved adjustments. Export always uses
the full original resolution and current crop, regardless of zoom or pan.


Embedded lens distortion is part of the unedited starting point. A LibRaw EXIF
callback reads the RAF FujiIFD `0xf00b` signed-rational table with bounded counts
(8, 9, or 11 knots). Validation rejects nonfinite values, invalid radii, singular
or folding maps, and a reference half diagonal inconsistent with the active
image by more than 1%. The last check deliberately leaves unverified camera crop
modes/normalizations uncorrected rather than guessing their units. Only the
X-T50 samples here are device-validated; other bodies need representative RAFs.

Source radii are normalized to the image half diagonal; values are percentage
radial displacements. Their piecewise-linear source-radius function is inverted
analytically for output-to-source sampling. The largest source mapping factor
along the full image border defines the frame-filling zoom. This mapping runs
in the shared Frame class after manual geometry, so every preview scale,
100% region, export, crop, mask stroke, and mask overlay uses the same profile.
Saved crop/mask coordinates stay attached to the original sensor. The automatic
profile is rederived from the source on reopen and Reset, not written into the
manual Distortion parameter. Output resolution is preserved.

Optics identifies whether embedded distortion data was applied or manual
correction is needed. This change does not automatically apply vignetting or
chromatic-aberration tables; those are separate from geometric distortion.
Table tag definitions: [ExifTool FujiIFD tags](https://exiftool.org/TagNames/FujiFilm.html#IFD).
Source-radius units and camera-border geometry were cross-checked against the
[LightCraft embedded-lens research](https://github.com/storytold/lightcraft/blob/main/docs/fuji-lens-corrections.md)
and the camera JPEG from the supplied RAF. No upstream lens implementation or
external lens database was added.


The bottom-right preview Crop button starts the full-screen framing workspace. Its two
rotation buttons apply counterclockwise/clockwise 90° turns, while Straighten
is a separate ±15° slider below the image. Wide layouts put aspect/action controls
beside the image and retain the Straighten slider underneath it. The crop and
aspect lock rotate with the photo; crop state is independent of preview bitmap
identity so draft/settled image replacement cannot discard a handle movement.

The existing preview worker handles staged framing requests using the same
sensor caches and native Frame mapping as ordinary adjustments. The global
adjustments and SavedStateHandle remain untouched until Apply; Cancel drops
staged requests and restores the original settled preview. Crop geometry input
and Apply wait for the latest staged rendering to finish. Errors are displayed
inside the workspace, leave global edits intact, and allow Cancel or retry.


Control groups are ordered Light, Color, Details, Curves, Effects, Optics, then
existing Masks. Each option retains independent RawAdjustments fields and
native processing. Pairs show both sliders; Grain and Vignette show one slider with
an accessible parameter menu above it. Selection changes no edit values, and
individual name resets affect only the selected field. Crop is independent of
category selection and returns to the same category after Apply or Cancel.


Framing's aspect-ratio picker lives in the header beside rotation controls.
It reuses FujiAnchoredMenu with scrollable, selectable Material 3 items instead
of a horizontal chip row. Original tracks the source orientation separately
from fixed presets with the same numerical ratio; all crop fitting and locking
continue to use the existing sensor/output mapping.


The earlier RAW parameter inputs used RawParameterSlider, a shared Material 3 Slider
with default XS dimensions and the library Label/PlainTooltip on its thumb.
Positive/negative ranges use CenteredTrack; unipolar ranges use Track. Steps
are derived from the existing displayed precision and rounding keeps exact
parameter units. The previous custom pointer handling and Canvas dial drawing
are removed. Option circles select controls; the native slider owns adjustable
semantics, avoiding duplicate sliders in the accessibility tree.


At 700 dp and wider, RawEditorScreen keeps the preview on the left and fills
the right pane vertically. One category row is shared between top placement
in the wide pane and bottom placement in compact mode. The wide body scrolls
independently and renders the same RawParam definitions and callbacks as the
compact editor, flattening grouped parameters into individually labeled rows.
No processing, persisted adjustment, or export format changes are involved.


RawToneCurveEditor sizes the graph with aspectRatio(1f); the parent editor
bounds its maximum size on compact screens while the wide list can scroll.
The controls body uses Compose HorizontalPager with rememberPagerState.
Dragging reveals the adjacent category while the content follows the finger;
the native pager handles release velocity, settling, snap-back, and end bounds.
Category taps use animateScrollToPage, with the category row outside the pager.
Each page owns its parameter selection and vertical scroll state. Cached
off-screen pages keep rendering but their controls are excluded from the
accessibility tree. The photo preview is outside the pager, retaining
comparison, zoom, and pan; descendant editing controls keep their gestures.
The option carousel uses a nested-scroll boundary to consume leftover
horizontal scroll and fling movement before it reaches the category pager.

Current parameter inputs use RawParameterWheel, replacing the Material XS
sliders above. Its custom ruler drawing sits in a native LazyRow with fixed
8 dp tick spacing, a stationary center marker, and native center-snapping
flings. Value steps use RawParam's existing display precision. The nearest
center tick updates the adjustment during drag and deceleration; external
values and resets use requestScrollToItem so synchronization schedules a
remeasure rather than forcing nested layout work. Parameter titles and
selectors share a fixed 48 dp header, eliminating the Effects preview jump.
The same wheel serves framing and mask parameters; accessible progress/reset
actions and keyboard input operate on the existing parameter callbacks.
Compact options with two parameters place their independently adjustable
wheels in equal-width columns in one row, keeping the same height as a single
parameter. The wide layout retains its vertical list of individual controls.

The preview's bottom-left comparison control offers Embedded JPEG and Before
edits. Selecting a source shows it; tapping the control again returns to the
edited RAW. Holding the photo temporarily shows the selected source, and
release/cancellation returns to the edit. Both comparison images are fitted
in the preview. A rounded top-center indicator reads Embedded JPG or Original
while comparing. Before edits retains the neutral RAW render with the applied
film profile and automatic lens correction, independently of current edits
or framing. It shares the initial preview bitmap on a fresh open and renders
the neutral settings separately when restoring saved adjustments. Comparison
does not change adjustments or the export source.
Comparison and grouped parameter choices use the same `FujiAnchoredMenu` and
`FujiSelectionMenuItem` as crop, including vibrant colors, checked leading
icons, selected item shapes, and shared bounded scrolling.

Color includes a film simulation menu limited to the eleven bundled profiles.
The recorded profile is selected initially and marked As shot. Choosing it again
clears the override without changing other edits. The native session swaps its
validated LUT while retaining decoded sensor data; preview, framing, 100% detail,
and full-resolution export select the requested LUT under the render mutex.
The override belongs to RawAdjustments for reset, unsaved-change tracking, and
frozen exports, and persists in SavedStateHandle beside the numeric adjustment
vector. Older sessions default to the recorded profile. Original comparison
always retains the neutral rendering with the recorded profile, including when
restoring a session that uses a different simulation.

Explanations do not occupy inline rows around the photo. Lens-correction
status is available from a question-mark tooltip over the Optics preview;
Curves and mask gesture guidance use the same native tooltip in their
existing headers. The obvious crop caption and duplicate loading captions
are omitted. Errors remain visible through overlay snackbars, and progress
indicators reserve their height so state changes do not resize the preview.
Only blocking file operations show the main progress bar; live edits and detail
refinement remain unobstructed.

Compact Curves uses a square graph over the photo, leaving Crop and Compare
clear and tappable. Channels, help, and reset sit underneath it.
The compact pager is 212 dp tall for every category, including two-line option
labels, so the preview does not resize when entering or leaving Curves. The
overlay follows the native pager offset and hides its inactive semantics.
Wide layouts keep the graph in the sidebar and place channels beneath it.
Channel selection uses connected Material 3 ToggleButtons labeled Master,
Red, Green, and Blue. Question-mark help and Reset channel occupy a separate
row below the selector, within the existing compact control-area height.
Point editing and rendering still use the same curve adjustments.
The graph's idle background is 8% black. Pressing its line or dragging a point
makes the background, border, and grid fully transparent, leaving the curve
and handles visible over the live edit. Release or cancellation restores the
idle appearance; channel/disabled changes clear transient interaction state.

Native behavior follows the official
[snapping API](https://developer.android.com/reference/kotlin/androidx/compose/foundation/gestures/snapping/package-summary)
and [LazyListState requests](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/LazyListState).
