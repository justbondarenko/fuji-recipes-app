# FEAT-005 — tasks

Ordered. Dependencies come first. Each item is one logical commit.
Paths are relative to `app/src/main/java/dev/bondarenko/fujirecipes/` unless stated.

**Prerequisite:** FEAT-001 to FEAT-004 complete.
**Reference:** `fuji-recipes-book` @ `0c17106`. Every transcribed file carries a header
naming the source path and that commit (`coding-standards.md` P3).
**Branch:** `feat/FEAT-005-camera-connection`, shared with FEAT-006 — see `roadmap.md` §1.

---

## Pure protocol layer — no `android.*` (P4)

- [x] **T-01** — `camera/ptp/PtpFraming.kt` ← `camera/usb/ptp.ts`. `ContainerType`,
      `Operation` (six codes only), `ResponseCode`, `DataType` + `dataTypeSize`,
      `packContainer` / `unpackContainer` / `containerLength`, `packU16` / `packI16` /
      `packU32` and unpackers, `packPtpString` / `unpackPtpString`, `responseName`,
      `PtpError`, `PtpFramingError`, `expectOk`.
- [x] **T-02** — `PtpFramingTest.kt` ← `tests/unit/ptp.spec.ts`. The two that catch real
      bugs: a data container reports no parameters, and an empty string packs to a single
      zero byte. Plus the character-count length rule and a signed-tone round trip.
- [x] **T-03** — `camera/ptp/DeviceInfo.kt` ← `camera/usb/session.ts`. `parseDeviceInfo`
      (model string is what this feature needs; the rest of the dataset is parsed because
      skipping fields correctly is what makes the offset arithmetic right) and
      `parseDevicePropertyDescription`. Test: a recorded dataset yields the model.
- [x] **T-04** — `camera/CameraModels.kt` ← `camera/models.ts`. `MODEL_GENERATIONS`,
      `normaliseModel`, `identifyModel` returning model, generation, recognised, label,
      writable and the reason when not. Reuses `SensorGeneration` from
      `data/fields/FilmSimulations.kt`. **Nothing is inferred from a name.**
- [x] **T-05** — `CameraModelsTest.kt`: no two names normalise to the same key; `XT5` finds
      the X-T5; an unrecognised body is not writable and does not claim a generation; an
      X-Trans III body is recognised, not writable, and says why.

## Transport — `android.hardware.usb`

- [x] **T-06** — `camera/usb/PtpTransport.kt`. The interface the session talks to:
      `command(operation, params, data): CommandResult`, `close()`. Small on purpose — it
      is what lets a fake camera stand in.
- [x] **T-07** — `src/test/…/camera/FakeCamera.kt` ← `tests/support/fake-camera.ts`.
      Speaks PTP back: transaction ids, multi-chunk reads, stale responses, refusals.
- [x] **T-08** — `camera/usb/UsbPtpTransport.kt` ← `camera/usb/transport.ts`. Open the
      device, rank candidate interfaces (bulk pair mandatory, still-image class first),
      claim each in turn with `force = true`, discover endpoint addresses, run the
      command / data / response phases over `bulkTransfer`, chunked reads to the declared
      length, 5 s timeout. `UsbConnectError` carries a cause the UI branches on.
- [x] **T-09** — `camera/usb/PtpSession.kt` ← `camera/usb/session.ts`. `openSession`,
      `getDeviceInfo`, `getPropValue`, `setPropValue`, `closeSession`.
- [x] **T-10** — `PtpSessionTest.kt` against `FakeCamera`: a session opens and reads a
      model; a refusal surfaces as `PtpError` with the operation and code; a stale response
      is skipped rather than accepted.

## Controller and lifecycle

- [x] **T-11** — `camera/CameraController.kt`. `StateFlow<CameraState>` with the six states
      of `PRD.md` §8.4; find the Fujifilm device by vendor id; permission via
      `PendingIntent`; connect on `Dispatchers.IO`; `ACTION_USB_DEVICE_DETACHED` receiver
      releases and returns to `Disconnected`; `FEATURE_USB_HOST` absent → `NoUsbHost`.
- [x] **T-12** — `core/AppContainer.kt` gains one lazy `cameraController` field.
- [x] **T-13** — `AndroidManifest.xml`: `<uses-feature android.hardware.usb.host
      required="true" />`, the `USB_DEVICE_ATTACHED` intent-filter and its meta-data on
      `MainActivity`. `res/xml/device_filter.xml` with `vendor-id="1227"` — **decimal**.
- [x] **T-14** — `MainActivity` hands an attach intent to the controller so the launched-by-
      plugging path connects with no permission dialog.

## Indicator

- [x] **T-15** — `ui/camera/CameraChip.kt` + `CameraChipLook.kt`. The six states of
      `PRD.md` §7.2, each with its own icon and words — colour is never the only signal.
      The look is a pure function so it is unit-testable; the composable renders what it
      returns and adds nothing.
- [x] **T-16** — `ui/camera/CameraSheet.kt` + `CameraViewModel.kt`. Model, generation, write
      availability with the reason when unavailable, connect / disconnect / retry.
      `@Preview` for every state including the error and unrecognised ones.
- [x] **T-17** — Mount the chip in `ui/shell/AppShell.kt`. Strings in `strings.xml`.
- [x] **T-18** — `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` green.
      Emulator: every chip state renders. **Hardware: confirmed on the owner's phone — the
      camera connects.** The gate is closed: the ranked-interface claim, the endpoint
      discovery, the PTP session and `GetDeviceInfo` all work against a real body, which is
      the half no fake camera could prove.
