# In-camera RAW development through Fujifilm USB

**Status:** Core conversion is feasible; card-origin workflow and X-T50 profile mapping require hardware gates
**Proposed feature:** FEAT-015
**Branch:** `claude/fuji-camera-library-features-6q623b`
**Dependency:** The large-object transport from the camera-photo-download plan. The card-origin version also depends on camera media browsing.

## Decision

The X RAW Studio conversion protocol can be implemented in this Android app without libfuji, an NDK layer, or image-processing code. The app uploads a RAF and a conversion profile, the connected camera renders the JPEG with its own processor, and the app downloads the result.

FilmKit and libfuji contain complete working implementations of the same sequence, with rawji/fp supplying a second profile representation and newer full-resolution behavior. They are strong corroborating implementations, although they are not completely independent discoveries: FilmKit explicitly credits rawji, Fudge/libfuji, libgphoto2, and its own X RAW Studio packet captures.

The protocol is not the largest risk. The two release gates are:

1. The X-T50's native `0xD185` profile layout must be captured and diffed rather than assumed to match the X100VI or X-T30.
2. The documented conversion trigger disagrees across implementations: FilmKit and libfuji use `0`, while newer rawji identifies `0` as preview and `1` as full resolution. Both values must be tested on the X-T50 and judged by output dimensions.

There is also a product-level correction to the requested flow. X RAW Studio does not remotely tell the camera to develop an arbitrary RAF already on its card. The documented USB protocol uploads a RAF from the host using Fuji's vendor `SendObjectInfo`/`SendObject2` operations. On the X-T50, browsing the card officially requires USB CARD READER, while conversion requires USB RAW CONV./BACKUP RESTORE.

Therefore, “pick a RAW on the card and develop it” should be implemented as a resumable two-mode handoff unless the initial hardware spike proves that card objects are also visible in conversion mode:

```text
USB CARD READER                    USB RAW CONV./BACKUP RESTORE
select RAF -> cache on phone  ->  reconnect -> upload cached RAF
                                         -> apply recipe -> render -> pull JPEG
```

The mode switch is inconvenient, but it is honest and uses documented behavior. A second entry point may choose a RAF already on the phone and start directly in conversion mode.

## Evidence

Fujifilm's [X-T50 RAW-processing instructions](https://app.fujifilm-dsc.com/en/manual/x-t50/connections/raw/) require USB RAW CONV./BACKUP RESTORE and state that X RAW Studio uses the camera's image-processing engine. The [connection-mode documentation](https://app.fujifilm-dsc.com/en/manual/x-t50/connections/network_usb_menu/) lists USB CARD READER and USB RAW CONV./BACKUP RESTORE as separate modes.

[FilmKit's protocol reference](https://github.com/eggricesoy/filmkit/blob/master/QUICK_REFERENCE.md) describes the entire conversion sequence and its 625-byte native X100VI profile. [libfuji's USB implementation](https://github.com/petabyt/libfuji/blob/master/lib/fuji_usb.c) independently implements RAF upload, `0xD185`, conversion triggering, handle polling, JPEG download, and cleanup. [rawji's USB implementation](https://github.com/pinpox/rawji/blob/main/src/rawji/fuji_usb.py) documents the full/preview trigger distinction, while [rawji's profile module](https://github.com/pinpox/rawji/blob/main/src/rawji/fuji_profile.py) and [fp](https://github.com/petabyt/fp) document the alternate standard profile layout.

The implementation should be a Kotlin port of verified protocol behavior, not a dependency on any of these projects. FilmKit and libfuji are suitable implementation references under their repository licences. rawji should be treated as behavioral evidence unless its licensing terms are clarified.

## User flow

The feature starts from a recipe because that is the app's central object. A recipe screen gains **Develop RAW**, which opens a source choice:

- **Choose from camera** connects in USB CARD READER mode, browses RAF objects, and caches the selected file. The app then gives exact instructions to switch the camera to USB RAW CONV./BACKUP RESTORE and reconnects to resume the pending development job.
- **Choose from phone** uses the Storage Access Framework and skips directly to the conversion-mode step.

After the conversion connection is ready, the app shows the selected RAF, camera model, selected recipe, settings that will be applied, and settings that this build cannot yet apply. Rendering is explicit rather than automatic. The output preview appears only after the camera has returned a JPEG; **Save JPEG** writes the full result through the Storage Access Framework, while **Try another recipe** reuses the already uploaded RAF when the camera session remains open.

The current manual recipe workflow remains unchanged. RAW development never edits the stored recipe or a camera custom slot.

If the app or camera disconnects between the two modes, the pending job keeps the cached RAF and selected recipe ID long enough to resume. It must not retain a stale camera object handle across sessions. If Android reclaims the cache file, the state says the RAW must be selected again.

## Protocol sequence

### 1. Establish capability and mode

Require a connected session and check that the body reports, or successfully accepts, the following:

| Capability | Code |
|---|---:|
| Fuji `SendObjectInfo` | `0x900C` |
| Fuji `SendObject2` | `0x900D` |
| `GetDevicePropValue` | `0x1015` |
| `SetDevicePropValue` | `0x1016` |
| `GetObjectHandles` | `0x1007` |
| `GetThumb` | `0x100A`, optional preview path |
| `GetObject` | `0x1009` |
| `DeleteObject` | `0x100B` |
| RAW profile property | `0xD185` |
| Conversion trigger property | `0xD183` |

The branch already reads Fuji USB mode property `0xD16E`; value `6` is RAW conversion. This is a positive capability signal, but an unreported mode remains unknown rather than automatically unsupported. The X-T50 hardware report must capture its advertised operations and properties in this mode.

### 2. Upload the RAF

Use Fuji vendor operation `0x900C` with parameters `[0, 0, 0]` and a PTP `ObjectInfo` dataset containing:

| Field | Value |
|---|---|
| Storage ID | `0` |
| Object format | `0xF802` |
| Protection | `0` |
| Compressed size | RAF byte length |
| Thumbnail/image metadata | zero unless a capture proves otherwise |
| Filename | PTP string `FUP_FILE.dat` |

Then send the RAF bytes with vendor operation `0x900D`. These are not the standard backup operations `0x100C`/`0x100D` already present on the branch, so they need distinct names and methods.

RAF files are tens of megabytes and must be uploaded from a stream with a known length. `packContainer` must not allocate a RAF-sized `ByteArray`. The streaming USB writer must combine the 12-byte data header with the first payload bytes, keep intermediate writes aligned so they do not create premature short packets, and send a zero-length packet only when the **entire container**, not an intermediate chunk, ends exactly on the endpoint's maximum packet size. The current `UsbBulkChannel.write` finalises every supplied array, so it cannot simply be called once per chunk.

The upload stage checks available phone cache/storage, reports bytes transferred, holds the existing bounded partial wake lock, and treats any interruption after `0x900C` as a session that must be closed and reopened.

### 3. Read and patch `0xD185`

Read `0xD185` only after the RAF has loaded. The camera-returned blob is the base profile for that RAF and body. Copy it byte-for-byte, validate its shape, and patch only fields whose positions and encodings have been verified for the identified profile adapter. All headers, sentinels, processor identifiers, image-size controls, quality controls, and unknown words remain untouched.

There must not be one universal `D185Profile` layout. Known sources currently describe at least these shapes:

| Layout | Evidence | Important difference |
|---|---|---|
| Native 625-byte profile | FilmKit, confirmed on X100VI | Parameter array derived from the blob's count; film simulation at native index 8 |
| Native 601/605-byte variants | fp/rawji observations | Different body generations and offsets |
| Standard 629/632-byte profile | fp/rawji, exercised on X-T30 | Parameter array at `0x201`; film simulation at standard index 7 |

The X-T50 adapter is enabled only after a native profile dump and one-setting-at-a-time diff establish its profile length, count, parameter start, processor identifier, supported indices, and enum values. A merely recognised X-Trans V body is not enough to select the X100VI adapter.

If the X-T50 follows FilmKit's native layout, the candidate fields are:

| Recipe field | Candidate native index | Encoding |
|---|---:|---|
| `exposureCompensation` | 4 | millistops; `1000` = +1 EV |
| `dynamicRange` | 6 | literal 100/200/400; preserve base for `dr-auto` |
| `filmSimulation` | 8 | RAW-profile film-simulation enum |
| grain effect and size | 9 | combined RAW-profile enum |
| `colorChromeEffect` | 10 | RAW-profile effect enum |
| smooth skin | 11 | not currently represented by the recipe model |
| `whiteBalance` | 12 | RAW-profile WB enum/sentinel |
| `wbShiftRed`, `wbShiftBlue` | 13, 14 | signed values |
| `colorTemperature` | 15 | Kelvin |
| highlight, shadow, color, sharpness | 16–19 | signed setting × 10 |
| `highIsoNR` | 20 | non-linear Fuji lookup |
| `colorChromeFxBlue` | 25 | RAW-profile effect enum |
| `clarity` | 27 | signed setting × 10 |

These are hypotheses for the X-T50 until the diff confirms them. RAW-profile enums must live in their own module. They must never reuse custom-slot codes from `CameraEncoding.kt` or MakerNote codes from `FujiExifParser.kt`; the repository already identifies these as three distinct dialects.

Applying a sparse stored recipe needs explicit semantics. For each **verified, applicable** RAW field, resolve missing recipe values through `RecipeFields` defaults so a sparse recipe renders consistently rather than inheriting whichever camera setting happened to be active. Preserve the camera-returned base for unsupported fields and for concepts such as `dr-auto` that have no confirmed explicit profile encoding.

For the first version, support only settings verified on the X-T50. Expected candidates are film simulation, DR100/200/400, grain, Color Chrome/Blue, white balance, Kelvin and shifts, highlight, shadow, color, sharpness, high-ISO NR, clarity, and exposure compensation.

Keep these out of the first version unless the profile diff resolves them:

- `dRangePriority`, because available mappings are uncertain.
- `monochromaticColorWc` and `monochromaticColorMg`, because the known standard layout exposes a different monochromatic representation.
- Smooth Skin, because the D185 profile may support it but the current recipe model does not.
- `isoMin` and `isoMax`, because they are shooting advice rather than RAW-development controls.

Before triggering conversion, write the patched profile to `0xD185`. Where the body permits it, read it back and compare every patched field. A camera-normalised value is reported as such; a mismatched unknown byte is not silently accepted.

### 4. Trigger and retrieve the conversion

Snapshot the current object-handle set before triggering. Write a `uint16` to `0xD183`, then poll `GetObjectHandles` with bounded backoff until a new handle appears or the render timeout expires. Treat `DeviceBusy` during processing as retryable; transaction, framing, disconnect, and other PTP errors are terminal for the session.

The trigger value is an explicit hardware gate:

- FilmKit and current libfuji use `0`.
- Newer rawji uses `0` for preview and `1` for full resolution.
- Earlier Fudge testing observed a roughly 6.4 MP ceiling on the `0` path on at least one body.

On the X-T50, render the same RAF/profile with both values. Record pixel dimensions, byte length, EXIF, quality, and the handle behavior. The full-resolution value becomes an X-T50-tested constant rather than a global assumption.

`GetThumb` may provide a fast preview, but **Save JPEG** must call the streaming `GetObject` path and verify that the result is a JPEG with the expected dimensions. The temporary output handle is deleted with `DeleteObject` after successful download and also during best-effort cleanup after downstream failure.

Deletion must be narrowly safe: delete only a handle absent from the pre-trigger snapshot and discovered by this conversion attempt. Never delete the selected card RAF or any pre-existing handle. If more than one new handle appears, fetch object info and select the JPEG attributable to the current transaction; otherwise fail closed and leave objects untouched.

The uploaded RAF can remain loaded while the session is healthy, allowing another recipe to be rendered without uploading it again. Any framing error, cancelled upload, or cable detach invalidates that reuse state.

## Card-origin handoff

The camera-photo browser must be extended to recognise RAF objects by tested `ObjectInfo` format and filename, but it should not guess based on one alone. The full RAF is streamed to an app-private `.part` file and atomically promoted after the declared length and `FUJIFILMCCD-RAW` signature are validated.

The pending job stores:

| Value | Reason |
|---|---|
| Cached RAF path and length | Resume after disconnect/mode switch |
| Source filename and camera model | User confirmation and same-model check |
| Recipe ID, not a copied recipe | Reload current library value when rendering begins |
| Source object metadata | Diagnostics only; never reuse its handle in the new session |
| Created/expiry time | Clean abandoned large files deterministically |

A minimal RAF metadata reader should extract the originating camera model when that can be done from a verified RAF fixture. The app warns before upload if it differs from the connected model. The camera remains authoritative and may still reject an incompatible RAF; the error must say that X RAW Studio requires a RAW from the same camera model.

Because the phone has one USB-C port and the camera occupies it, cached RAF state must survive navigation and the expected USB detach/reconnect. It does not need permanent library storage. Stale files are removed after a short explicit retention period or when the user cancels.

## Proposed code placement

| Responsibility | Location |
|---|---|
| Fuji vendor opcodes, object handles, delete, thumb | `camera/ptp/PtpFraming.kt` |
| Streamed input/output data phases | `camera/ptp/PtpTransport.kt`, `camera/ptp/PtpSession.kt`, `camera/usb/UsbBulkChannel.kt` |
| Fuji RAW upload `ObjectInfo` builder | `camera/raw/FujiRawObjectInfo.kt` |
| Profile validation and pure patching | `camera/raw/RawDevelopmentProfile.kt` |
| X-T50 mapping and RAW-specific enum tables | `camera/raw/XT50RawProfile.kt` |
| Conversion state machine and cleanup | `camera/usb/RawDevelopmentSession.kt` |
| Serialized lifecycle, wake lock, mode handoff | `camera/CameraController.kt` |
| Pending RAF cache | `core/store/RawDevelopmentCache.kt` |
| Development screen and ViewModel | `ui/raw/` |
| Protocol simulator extensions | `camera/FakeCamera.kt` and focused JVM tests |

All profile construction and recipe translation remain pure and free of `android.*`. Transport knows PTP bytes and streams but knows nothing about recipes. The coordinator consumes a patched profile and owns sequencing.

## Ordered development plan

### Phase 0 — X-T50 gates

1. Capture camera reports in card-reader and RAW-conversion modes, including operation and property lists.
2. In RAW-conversion mode, upload one known-good X-T50 RAF and save the native `0xD185` blob.
3. Capture profiles while changing one X RAW Studio control at a time; diff 32-bit words and identify constant versus setting-dependent regions.
4. Test `0xD183` values `0` and `1`, full and thumbnail retrieval, object-handle timing, repeated conversions, and cleanup.
5. Test whether card storage is visible in RAW-conversion mode. If it is not, confirm the two-mode cached handoff on Android before building the final UI.

Do not implement a recipe encoder until the X-T50 profile diff is recorded as a test fixture and mapping note.

### Phase 1 — Shared transfer foundation

1. Complete the streamed download work specified by the camera-photo plan.
2. Add streamed outgoing data containers with known lengths and correct final-packet/ZLP behavior.
3. Add operation-specific progress and stall timeouts, cancellation checks between chunks, and session invalidation after an interrupted outgoing data phase.
4. Extend `FakeCamera` to consume and produce large data phases without holding the entire transfer merely to test chunking.

### Phase 2 — RAW profile layer

1. Implement defensive profile-shape detection and the X-T50 adapter from captured evidence.
2. Implement RAW-specific enum encoders and non-linear NR mapping separately from slot and EXIF dialects.
3. Implement pure `patchRawProfile(base, recipe, identity)` output containing the patched blob, applied fields, preserved fields, and unsupported requested fields.
4. Test every supported field independently, combinations, defaults on sparse recipes, signed/fractional values, conditional white balance and grain fields, malformed blobs, unknown layouts, preservation of every unmodified byte, and model/profile mismatch.

### Phase 3 — Conversion coordinator

1. Implement vendor RAF object-info creation and streamed `0x900C`/`0x900D` upload.
2. Implement base-profile read, pure patch application, write, and optional read-back verification.
3. Implement pre-trigger handle snapshot, tested full-resolution trigger, bounded polling, thumbnail/full retrieval, and narrowly scoped deletion.
4. Model progress as named stages: preparing, uploading, applying recipe, processing in camera, downloading preview, downloading full JPEG, and cleaning up.
5. Model failures by remedy: wrong mode, unsupported body/profile, wrong-model RAF, invalid RAF, insufficient phone space, upload interrupted, setting refused, camera busy, render timeout, no unique output, incomplete JPEG, save failure, and disconnect.

### Phase 4 — Card-to-conversion handoff

1. Allow RAW selection in the card browser and cache one RAF with progress.
2. Persist a bounded pending-job record across the intentional disconnect.
3. Show exact X-T50 menu instructions for switching to USB RAW CONV./BACKUP RESTORE.
4. Resume only after the reconnected body matches the expected model and capabilities.
5. Add the phone-RAF picker as a simpler alternate source without weakening RAF validation.

### Phase 5 — Product UI

1. Add **Develop RAW** to the recipe view.
2. Build source selection, mode instructions, transfer progress, recipe/application review, unsupported-setting disclosure, rendered preview, save, retry, and try-another-recipe states.
3. Use an explicit Render action for the first release; do not debounce recipe changes into expensive camera work.
4. Save through `ActivityResultContracts.CreateDocument("image/jpeg")`; do not request storage permission.
5. Keep the selected RAF cached while trying recipes, and remove it when the job is cancelled, completed without reuse, expired, or explicitly cleared.

### Phase 6 — Verification

Run `:app:assembleDebug`, `:app:testDebugUnitTest`, and `:app:lintDebug`. Hardware verification must cover compressed, lossless-compressed, and uncompressed X-T50 RAFs; RAFs from the same model and a different model; every supported recipe field individually; monochrome recipes with unsupported controls disclosed; DR constraints; repeated renders without re-upload; full-resolution dimensions; EXIF in the result; cancellation and unplugging during every stage; low battery; process recreation during the mode handoff; and cleanup after success and failure.

The end-to-end acceptance test is: select an X-T50 RAF that exists only on the camera card, cache it through USB CARD READER, reconnect the same body in USB RAW CONV./BACKUP RESTORE, apply a named recipe from the library, save the full-resolution JPEG to Android storage, and verify its dimensions and MakerNote settings against the requested supported fields.

## Main risks and decisions

| Risk | Handling |
|---|---|
| Card browsing and conversion require separate modes | Make the reconnect a designed, resumable handoff; test for a one-mode shortcut but do not assume it |
| X-T50 `0xD185` layout differs from published examples | Capture and diff the native profile before writing an adapter |
| Preview/full trigger differs by implementation or body | Test `0` and `1`; assert pixel dimensions in hardware acceptance |
| Slot, EXIF, and RAW encodings look similar but differ | Separate modules and golden tests; no shared numeric lookup without byte-level proof |
| Large RAF/JPEG exhausts heap or hits the current 8 MiB cap | Stream both directions; bound by declared size and available storage |
| Cleanup deletes a real card object | Delete only the newly observed conversion handle, never a pre-existing handle |
| Camera rejects a setting or silently normalises it | Optional D185 read-back plus applied/unsupported disclosure |
| Power or cable loss leaves protocol state unknown | Hold a bounded wake lock, discard partial phone files, and reopen the PTP session |

## Estimate and release boundary

Once shared media streaming exists, a phone-RAF/X-T50 conversion MVP is approximately four to seven focused development days plus hardware profiling. Adding the camera-card source, durable mode-switch handoff, and its failure states brings the complete requested flow to approximately seven to twelve focused days after the profile gates are resolved.

The first release should claim only the X-T50 and only the settings proven by the captured native profile. Additional bodies are adapters with their own fixtures and hardware acceptance; they are not enabled merely because they share X-Processor 5.
