# Pull photos directly from a Fujifilm camera

**Status:** Feasible, with an X-T50 hardware gate
**Proposed feature:** FEAT-014
**Branch:** `claude/fuji-camera-library-features-6q623b`
**Scope:** Browse JPEGs on the camera card, download selected files, and pass them through the existing EXIF recipe analysis flow. The camera card remains read-only.

## Decision

This feature is feasible on Android and fits the existing architecture. Fujifilm explicitly documents connecting an X-T50 to an Android phone in **USB CARD READER** mode, after which Android reports a USB PTP device and can import pictures. The object operations required to reproduce that flow are standard PTP rather than undocumented Fuji commands.

The current branch already provides most of the lower stack: Android USB host discovery, interface and endpoint selection, PTP sessions, transaction validation, `GetObjectInfo`, `GetObject`, camera-mode reporting, a long-lived serialised `CameraController`, and a fake camera. The existing photo-analysis path already accepts several JPEGs, parses their Fujifilm MakerNotes, runs `RecipeMatcher`, and displays the results.

The missing work is object enumeration, thumbnails, a card-browser UI, and safe streaming of full JPEGs. Streaming is the largest technical prerequisite: `PtpTransport` currently buffers a complete data container, rejects anything over 8 MiB, and repeatedly copies its growing receive array. That is suitable for properties and settings backups but not for X-T50 JPEGs.

There is one important operating constraint: **card browsing and RAW conversion use different camera connection modes**. This feature requires USB CARD READER. Slot operations, backup/restore, and RAW development use USB RAW CONV./BACKUP RESTORE. The app must describe the required mode for the selected action instead of treating one mode as universally correct.

## Evidence

The [X-T50 manual's Android instructions](https://app.fujifilm-dsc.com/en/manual/x-t50/connections/usage_usb/) explicitly require USB CARD READER and tell the user to open the Android notification labelled “Connected to USB PTP”. The same manual warns that cards with many images may take time to enumerate and that the camera must not be disconnected during a transfer.

The object operations and datasets are defined by PTP/ISO 15740 and are implemented generically by [libgphoto2's PTP camera driver](https://github.com/gphoto/libgphoto2/tree/master/camlibs/ptp2). No Fuji-specific media-download protocol is required after Android has claimed the still-image interface.

The final compatibility claim must nevertheless be based on an X-T50 trace. The official documentation proves that Android transfer is supported, but it does not prove which optional filters, folder associations, thumbnail formats, or object ordering this firmware implements.

## User flow

The existing **Analyze** screen should gain a second source beside the system picker: **Choose from camera**. Selecting it opens a camera-photo browser, not a generic file manager.

The browser shows JPEG thumbnails and capture dates from the connected card. The user may select one or several photos, then choose **Analyze**. Only the selected full JPEGs are downloaded. Once each download completes, the existing `FujiExifParser` and `RecipeMatcher` produce the same result cards used for phone photos. The user can open a matching recipe, save a new recipe, or attach the downloaded image to a recipe through the existing paths.

The browser must never offer delete, rename, move, or write operations. `DeleteObject` is not part of this feature. A failed or cancelled transfer removes only the app's incomplete cache file and changes nothing on the camera card.

If the connected camera is in RAW CONV./BACKUP RESTORE mode, the screen explains that photo browsing requires USB CARD READER and that changing the camera setting requires disconnecting and reconnecting. If the camera does not report its mode, capability checks and the attempted PTP response decide the message; `UNREPORTED` must not be treated as a confirmed wrong mode.

## Protocol design

### Capability gate

After `GetDeviceInfo`, the feature requires `GetObjectHandles`, `GetObjectInfo`, and `GetObject`. `GetThumb` is optional: if it is absent or an individual object has no thumbnail, the browser shows a placeholder and still permits downloading the full JPEG.

The implementation should also add `GetStorageIDs` and, if useful for diagnostics, `GetStorageInfo`. Calling `GetObjectHandles` with the all-storage sentinel can work, but explicit storage enumeration distinguishes an empty card from a camera that exposed no storage.

The first hardware spike must record the operations advertised by the X-T50 in both USB CARD READER and RAW CONV./BACKUP RESTORE modes using the camera-report functionality already present on this branch.

### Enumeration

Add the following standard operation codes to `camera/ptp/PtpFraming.kt`:

| Operation | Code | Use |
|---|---:|---|
| `GetStorageIDs` | `0x1004` | Discover mounted card slots |
| `GetStorageInfo` | `0x1005` | Optional label/capacity diagnostics |
| `GetObjectHandles` | `0x1007` | Enumerate objects by storage, format, or association |
| `GetThumb` | `0x100A` | Retrieve the camera's small preview |

`GetObjectHandles` returns a `uint32` count followed by that many `uint32` handles. The parser must reject counts that cannot fit in the remaining dataset, preserve the raw 32-bit handle bits in Kotlin `Int`, and accept an empty array as an empty card rather than an error.

`GetObjectInfo` is required even though it was not named in the feature shorthand. It supplies the object format, compressed size, thumbnail metadata, parent association, filename, capture date, and modification date needed to filter and label the browser. The existing method returns raw bytes because backup mode exposes a Fuji-specific object-info shape; card media needs a separate, bounds-checked ISO `ObjectInfo` parser. The two layouts must not be conflated.

The preferred enumeration is storage- and association-aware: discover the card, walk root associations to DCIM, then load one DCF folder at a time. That avoids issuing `GetObjectInfo` for an entire card before showing anything. The hardware spike must verify that the X-T50 honours the association parameter. If it does not, fall back to a JPEG-format query using object format `0x3801`, parse object info sequentially, and surface progress because a large card can take minutes.

PTP does not guarantee useful handle ordering. The UI must sort by parsed capture date and then filename/handle; it must not call the numerically largest handle “newest” unless an X-T50 capture establishes and tests that rule.

### Thumbnail retrieval

Thumbnails are requested only for visible or near-visible items, through the same serial PTP session. A small bounded cache keyed by camera identity, storage ID, handle, and object modification metadata prevents re-fetching while scrolling. A thumbnail failure belongs to that tile and does not abort enumeration.

The bytes returned by `GetThumb` must be validated by their file signature before being handed to the image decoder. PTP object info may advertise a thumbnail that the body later refuses, so the placeholder is a normal state rather than an exceptional screen.

### Full-object download

`GetObject` must gain a streaming form that writes the data payload directly to a caller-provided sink. The current `ByteArray` form can remain for small settings backups and tests, implemented on top of the streaming primitive with a strict small-object limit.

The transport must:

1. Read and validate the 12-byte PTP data-container header before trusting its declared size.
2. Stream exactly the declared payload length into an app-private temporary file.
3. Report byte progress without emitting UI state on every USB packet.
4. Read and validate the response container and transaction ID after the data phase.
5. Compare the received length with `ObjectInfo.compressedSize` when the camera supplied one.
6. Delete the partial phone-side file on cancellation, timeout, framing failure, or detach.

The receive path must stop using `bytes += more`, which repeatedly copies all bytes already received. The fixed 8 MiB limit should become an operation-specific policy: small control datasets retain a conservative cap, while a media download is bounded by object info, available phone storage, and an explicit product limit.

### Cache and analysis hand-off

Downloaded JPEGs should land in `cacheDir/camera-media`, first as a `.part` file and then through an atomic rename after the transfer and JPEG signature checks succeed. The cache is transient and may be reclaimed; choosing **Add photo to recipe** continues to use the existing persistent `ImageStore`.

The photo screen now has two justified input sources, so its injected reader should become a small explicit source model rather than making camera handles resemble Android content URIs. Both sources converge after bytes are available:

```text
Phone Uri ───────┐
                 ├─ JPEG bytes/file ─ FujiExifParser ─ RecipeMatcher ─ existing result UI
Camera object ───┘
```

The current parser accepts a `ByteArray` and refuses files over 50 MiB. For the first version, reject an object whose advertised size exceeds that limit before downloading. A later optimisation can parse the APP1/MakerNote from a stream or use `GetPartialObject`; neither is required to prove the feature.

## Proposed code placement

| Responsibility | Location |
|---|---|
| New opcodes and handle-array parsing | `camera/ptp/PtpFraming.kt` |
| ISO card `ObjectInfo` dataset parser | `camera/ptp/ObjectInfo.kt` |
| Streaming PTP data phase | `camera/ptp/PtpTransport.kt`, `camera/ptp/PtpSession.kt` |
| Android bulk streaming and final-packet handling | `camera/usb/UsbBulkChannel.kt` |
| Enumeration, thumbnail, and download orchestration | `camera/usb/CameraMediaReader.kt` |
| Serialised public camera operations | `camera/CameraController.kt` |
| Transient files and cleanup | `core/store/CameraMediaCache.kt` |
| Source selection and camera browser | `ui/photo/` or a nested `ui/photo/camera/` package |
| Fake object catalogue and transfer failures | `camera/FakeCamera.kt` and focused JVM tests |

No native library, NDK layer, network permission, database, or broad storage permission is needed.

## Ordered development plan

### Phase 0 — X-T50 protocol spike

1. Capture the camera report in USB CARD READER and RAW CONV./BACKUP RESTORE modes.
2. Confirm `GetStorageIDs`, `GetObjectHandles`, `GetObjectInfo`, `GetThumb`, and `GetObject` support in card-reader mode.
3. Record card storage IDs, JPEG object format, association behaviour, handle order, thumbnail format/size, and the response for an empty card.
4. Test a card containing JPEG-only, RAW+JPEG pairs, several DCF folders, and enough files to expose enumeration latency.

The feature is a go if the camera exposes JPEG handles and full objects through the claimed Android PTP interface. If Android's system importer owns the only interface and it cannot be claimed, stop before UI work and investigate interface selection; do not replace this with an undocumented filesystem hack.

### Phase 1 — Shared large-object transport

1. Add bounded streaming receive to `BulkChannel`/`PtpTransport` without changing the behavior of existing property and backup commands.
2. Make timeouts operation-aware. A five-second period with no USB progress is a useful stall timeout; it is not a useful total-file timeout.
3. Preserve transaction validation and ensure the response following a streamed data phase cannot be mistaken for payload.
4. Add tests for payloads below, above, and exactly on chunk and endpoint packet boundaries, including a data container larger than the old 8 MiB cap.

This phase is shared with in-camera RAW development and should be implemented once.

### Phase 2 — Pure object datasets

1. Implement bounded parsers for storage IDs, object handles, and ISO `ObjectInfo`.
2. Add fixed golden datasets plus malformed count, truncated string, unsigned size, association, and unknown-format tests.
3. Introduce immutable camera-media values carrying only wire facts; UI labels and selection state remain outside the protocol layer.

### Phase 3 — Camera media reader

1. Implement capability checks and storage discovery.
2. Implement association-aware JPEG enumeration with the tested fallback.
3. Implement lazy thumbnail requests and streamed full downloads.
4. Add named errors for wrong mode, no card/storage, unsupported operation, empty card, no JPEGs, insufficient phone space, camera busy, timeout, detach, invalid object info, incomplete transfer, and invalid JPEG.
5. Expose the operations through `CameraController` under its existing mutex. Do not permit a slot write, report, backup, thumbnail request, or download to interleave with another PTP transaction.

### Phase 4 — Product flow

1. Add **Choose from camera** to Analyze.
2. Build loading, empty, wrong-mode, browsing, selection, downloading, per-thumbnail failure, and full-transfer failure states, with Compose previews.
3. Hand completed cached JPEGs to the existing multi-photo analysis state machine.
4. Retain the existing system photo picker unchanged.
5. Clean stale `.part` files at startup and completed transient camera files when the analysis session is reset or expires.

### Phase 5 — Verification

Run `:app:assembleDebug`, `:app:testDebugUnitTest`, and `:app:lintDebug`. On the X-T50, verify a one-photo and multi-photo analysis, exact recipe matching, a stripped/non-Fuji JPEG, a failed thumbnail with a successful full download, a card with many files, cancellation, screen rotation, process recreation after selection, cable removal during enumeration and transfer, and insufficient phone storage.

The end-to-end acceptance test is: shoot a JPEG with a recipe already in the library, leave it only on the camera card, connect the X-T50 in USB CARD READER mode, select it in the app, and receive the same exact-match result by name as the existing phone-photo path.

## Main risks and decisions

| Risk | Handling |
|---|---|
| Android's camera importer competes for the PTP interface | Follow Fujifilm's instruction to cancel the other-app prompt; verify the existing ranked interface claim on X-T50 |
| A large card makes full enumeration slow | Prefer folder associations, load incrementally, and show measured progress |
| JPEG exceeds current 8 MiB transport cap | Complete streaming transport before product UI |
| Thumbnail is absent or malformed | Placeholder; never block full download |
| Card and RAW-development modes differ | Make the required mode part of each action and expect a reconnect |
| Accidental card mutation | Do not expose any mutating object operation in this feature |

## Estimate

After the hardware spike, the X-T50-only implementation is approximately five to eight focused development days: two to three for shared transport and datasets, one to two for enumeration/download orchestration, and two to three for UI, failure states, and hardware verification. Multi-model support should be claimed only as each body is tested; standard PTP lowers implementation risk but does not remove firmware differences.
