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
- [x] Full-resolution JPEG export through the document picker.
- [x] Discard confirmation and stable progress/comparison layouts.

## First batch — authorized

These are the recommended everyday controls selected by the user to start with.
Each must work in preview and full-resolution export, persist with the edit session,
and reset to a neutral adjustment without replacing the recorded color profile.

- [x] **2. White balance:** relative temperature and green/magenta tint, with As Shot reset.
- [x] **4. Contrast:** increase or soften overall tonal separation.
- [x] **5. Whites and blacks:** adjust bright and dark endpoints independently.
- [x] **6. Saturation:** adjust overall color intensity, including desaturation.
- [x] **7. Vibrance:** preferentially adjust muted colors.
- [x] **11. Sharpening:** amount, radius, and threshold to avoid sharpening small noise.
- [x] **12. Color noise reduction:** reduce colored speckles while preserving edges.
- [x] **13. Luminance noise reduction:** smoothing amount and detail preservation.

## Remaining proposed controls

- [ ] **1. Film simulation selector:** switch between the eleven bundled profiles.
- [ ] **3. White balance eyedropper:** sample a neutral surface in the photo.
- [ ] **8. Tone curve:** master curve, with optional separate RGB curves.
- [ ] **9. Color mixer / HSL:** hue, saturation, and brightness by color range.
- [ ] **10. Color grading:** shadows, midtones, and highlights, with balance/blending.
- [ ] **14. Clarity:** medium-scale local contrast.
- [ ] **15. Texture:** fine surface detail enhancement or softening.
- [ ] **16. Dehaze:** haze reduction or a softer atmospheric appearance.
- [ ] **17. Framing:** crop, aspect ratio, rotate, and straighten.
- [ ] **18. Vignette:** amount, midpoint, roundness, and feathering.
- [ ] **19. Grain:** amount, size, and roughness; independent of Fujifilm's grain engine.
- [ ] **20. Lens / geometry corrections:** distortion, perspective, and color fringing;
  automatic correction requires verified lens data.
- [ ] **21. Selective adjustments:** brush, linear-gradient, and radial masks.

## Companion tools

- [ ] Histogram.
- [ ] Highlight and shadow clipping warnings.
- [ ] 100% zoom for judging detail and noise.

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
fitted preview cannot replace judging the full-resolution export at 100% zoom.
Full lint remains blocked by the same pre-existing camera-screen error.

Gesture update on 2026-10-24: debug and test APKs build, and all 728 JVM tests pass.
Wireless SM-F976B UI checks cover isolated resets for all sixteen parameters,
single-tap and busy-state protection, inactive detail resets, hold/release/cancel
comparison, screen-reader actions, and stable compact/wide layouts.
