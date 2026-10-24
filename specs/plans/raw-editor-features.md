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
- [x] Original embedded camera JPEG comparison.
- [x] Full-resolution JPEG export through the document picker.
- [x] Discard confirmation and stable progress/comparison layouts.

## First batch — authorized

These are the recommended everyday controls selected by the user to start with.
Each must work in preview and full-resolution export, persist with the edit session,
and reset to a neutral adjustment without replacing the recorded color profile.

- [ ] **2. White balance:** temperature and green/magenta tint, with As Shot reset.
- [ ] **4. Contrast:** increase or soften overall tonal separation.
- [ ] **5. Whites and blacks:** adjust bright and dark endpoints independently.
- [ ] **6. Saturation:** adjust overall color intensity, including desaturation.
- [ ] **7. Vibrance:** preferentially adjust muted colors.
- [ ] **11. Sharpening:** amount, radius, and threshold/masking to protect noise.
- [ ] **12. Color noise reduction:** reduce colored speckles while preserving edges.
- [ ] **13. Luminance noise reduction:** smoothing amount and detail preservation.

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

First batch: not started. Record checks and concrete limits here before marking
its items complete.
