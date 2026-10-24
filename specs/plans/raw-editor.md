# RAW Editor

The separate More → RAW Editor section develops sensor RAW on the phone. The recorded
film simulation's color profile and as-shot white balance are applied before the first
editable preview. The original RAF is never modified. Lab retains camera-engine development.

The pipeline is RAF → LibRaw demosaicing in unscaled sensor RGB → floating-point as-shot
WB plus relative temperature/tint → camera matrix → dynamic-range exposure compensation
→ exposure/shadow/highlight adjustments → sRGB encoding → the simulation's published
3D color LUT → contrast/whites/blacks/saturation/vibrance → edge-aware luminance/chroma
noise reduction → luminance sharpening → display/JPEG.
Adjustments always re-render from retained linear RAW pixels. The embedded JPEG supplies
MakerNote metadata and an explicitly labeled camera comparison, never editable pixels.

The eleven color simulations have bundled profiles. Unknown simulations and monochrome
variants fail explicitly instead of silently displaying an unprofiled image. These are
camera-matching color profiles, not Fujifilm's complete proprietary renderer. Grain, Color
Chrome, the camera's sharpening/NR settings, clarity, lens corrections, and the other recipe
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
