# RAW Editor

The separate More → RAW Editor section develops sensor RAW on the phone. The recorded
film simulation's color profile and as-shot white balance are applied before the first
editable preview. The original RAF is never modified. Lab retains camera-engine development.

The pipeline is RAF → LibRaw demosaicing in unscaled sensor RGB → optical/geometry
correction and framing/bilinear resampling → floating-point as-shot
WB plus relative temperature/tint → camera matrix → dynamic-range exposure compensation
→ exposure/shadow/highlight adjustments → sRGB encoding → the simulation's published
3D color LUT → contrast/whites/blacks/saturation/vibrance → defringe → dehaze → edge-aware luminance/chroma
noise reduction → texture/clarity → luminance sharpening → vignette → display/JPEG.
Adjustments always re-render from retained linear RAW pixels. The embedded JPEG supplies
MakerNote metadata and an explicitly labeled camera comparison, never editable pixels.

The eleven color simulations have bundled profiles. Unknown simulations and monochrome
variants fail explicitly instead of silently displaying an unprofiled image. These are
camera-matching color profiles, not Fujifilm's complete proprietary renderer. Grain, Color
Chrome, the camera's sharpening/NR settings, clarity, automatic lens corrections, and the other recipe
effects are not replicated. Independent sharpening and NR controls are available in Detail.

Previews use half-size demosaicing and a 1600-pixel long edge. Export reopens the original
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
twenty-nine-value sessions migrated using neutral corrections. Verified lens
calibration profiles, complex radial/fisheye models, and automatic correction
remain separate pending work; Fujifilm recipe metadata alone is not calibration.
