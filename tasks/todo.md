# Task: FEAT-005 + FEAT-006 — camera connection and write to slot

**Branch:** `feat/FEAT-005-camera-connection` (both features, see `specs/roadmap.md` §7)
**Specs:** `specs/features/FEAT-005-camera-connection/`, `specs/features/FEAT-006-write-to-slot/`
**Reference:** `fuji-recipes-book` @ `0c17106` — a shipped, unit-tested WebUSB PTP stack.
This is a transcription, not protocol archaeology.

## Plan Items

- [x] 1. Spec folders for FEAT-005 and FEAT-006; roadmap §7 records the re-rated risk and
      the shared branch <!-- id: 1 -->
- [ ] 2. `camera/ptp/PtpFraming.kt` — containers, value codecs, PTP strings. Pure, no
      `android.*`. Tests mirroring `tests/unit/ptp.spec.ts` <!-- id: 2 -->
- [ ] 3. `camera/ptp/DeviceInfo.kt` and `camera/CameraModels.kt` — device-info parsing and
      model → generation, reusing the existing `SensorGeneration` enum <!-- id: 3 -->
- [ ] 4. `camera/usb/` — `PtpTransport` interface, `UsbPtpTransport` over
      `UsbDeviceConnection`, `PtpSession`, and a `FakeCamera` that speaks PTP back <!-- id: 4 -->
- [ ] 5. `camera/CameraController.kt`, `AppContainer` field, manifest `uses-feature` +
      attach intent, `res/xml/device_filter.xml` (vendor-id **1227**, decimal) <!-- id: 5 -->
- [ ] 6. `ui/camera/` — the chip in six states and the camera sheet. **FEAT-005 gate:
      installable and provable on hardware** <!-- id: 6 -->
- [ ] 7. `camera/plan/` — `CameraEncoding.kt`, `WritePlan.kt`, `StepPayload.kt`. Pure. Tests
      covering WR-01…WR-10 individually <!-- id: 7 -->
- [ ] 8. `camera/usb/WriteExecutor.kt` and `ui/camera/WriteSheet.kt` — execution, the six
      stages, haptics, entry point on the recipe view toolbar <!-- id: 8 -->
- [ ] 9. `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`, emulator pass
      over every state, dark scheme, P5 error-copy audit <!-- id: 9 -->

## Notes carried through implementation

- **P4 is structural:** nothing under `camera/ptp/` or `camera/plan/` may import `android.*`.
- **P3 headers:** every transcribed file names its source path and `0c17106`.
- **Naming:** `ui/connection/` is the *server* connection. Camera UI is `ui/camera/`.
- **Expressive components are unreachable** at material3 1.4.0 (`tech-stack.md` §6), so the
  slot picker and progress bar are built from the shell's existing hand-rolled vocabulary.

## Review & Verification

*Filled in as stages land.*
