# USB and Bluetooth recipe flows

Reviewed **29 October 2026**, against production code at `9447379` on
`bluetooth-connection`. This document compares the current implementation and
records the selective-Bluetooth research. It changes no production flow.

**15 November hardware follow-up:** Community Fuji 1.10.3 was captured writing
and restoring free slot C2 on Pixel/X-T50 1.32. Both uploads expanded to the
complete 70,524-byte backup. Its observed core method matches ours; see the
[C2 evidence](#community-fuji-c2-upload-and-restoration-15-november) below.

**USB writes individual properties in the selected custom slot. Bluetooth
currently restores a complete, freshly read settings backup after patching one
slot. No validated selective BLE upload was found in the reviewed sources;
selective BLE upload has not been proved impossible.**

Here, a full backup means the camera's settings file, including all seven recipe
records. It is not an image-card backup or a firmware image. The measured X-T50
1.32 file is 70,524 bytes before compression; BLE sends its zlib-compressed
representation, so 70,524 is not the upload's on-air byte count.

## Current implementation comparison

| Aspect | USB | Bluetooth |
|---|---|---|
| Connection | Phone acts as USB host, obtains USB permission, and opens a PTP session with the camera in a supported connection mode. The normal import UI asks for USB RAW CONV./BACKUP RESTORE. | Phone connects to the paired camera over BLE, establishes the encrypted link and Fujifilm handshake, and checks the supported model/firmware. Camera USB is disconnected; no camera Wi-Fi hotspot is needed. |
| Application protocol | Standard PTP operations carrying Fujifilm vendor properties over USB bulk endpoints. | Fujifilm GATT control and file-transfer characteristics. Our BLE implementation has no PTP property bridge. |
| Reading slots | Select each slot with `0xD18C`, then read its name and recipe properties. A slot-selector control write does not upload a new recipe. | Download one complete settings file, validate and decompress it, then decode C1–C7 from its records. GATT control writes initiate the download without uploading camera settings. |
| Selecting a write target | Send `SetDevicePropValue` (`0x1016`) for `0xD18C`, with the chosen slot number. | Choose which record to patch locally. The observed restore request does not contain the chosen slot number. |
| Upload granularity | Write the selected slot's name through `0xD18D`, then its supported recipe properties individually. | Send `backup.dat`, containing the whole compressed target settings file. Only the selected recipe record and settings checksum are changed in the locally constructed target. |
| Data retained from the camera | The normal writer does not need a whole-settings backup. Unwritten properties retain their current values. | Every other byte comes from a fresh camera backup taken inside the write operation. An earlier import or cached snapshot is not the write baseline. |
| Encoding and compatibility | A write plan determines order, field applicability, dropped fields and typed property payloads. | Firmware-specific backup offsets and value maps; unsupported fields or values are refused before upload. This is not a byte-for-byte reuse of USB property payloads. |
| Verification | Read each property back after writing it; report refused, mismatched or unverified steps. | Close the upload connection, reconnect, download a new settings backup, and compare every byte covered by the saved settings-verification contract. Matching just the selected recipe is insufficient. |
| Cancellation | Checked between property transactions; cancellation can leave a partially written slot. | Allowed before settings upload starts. Once file upload starts, finish and verify instead of intentionally interrupting it. |
| Uncertain result | Report attempted steps and whether the slot was touched. The USB writer does not use the BLE whole-file recovery checkpoint. | Persist original/target snapshots and policy before upload; mark pending before sending file metadata. Uncertainty blocks further BLE uploads across restarts until an explicit fresh camera check resolves it. Recovery evidence can be exported. |
| Scope | Support depends on the connected body, mode and property/field compatibility. | Current production format and verification support is restricted to X-T50 firmware 1.32. Public information about other bodies does not extend that support. |

The USB mode's name includes “BACKUP RESTORE”, but that does not mean every
operation in that mode transfers a backup. The recipe writer uses property
operations. Independent [USB protocol documentation][usb-protocol] corroborates
the slot selector and `0xD18D..0xD1A4` stored-slot block; `0xD1A5` belongs to live
shooting settings, not that block. This is USB evidence, not a BLE mapping.

## USB sequence

1. Connect and initialize the USB/PTP session; identify the camera and build the
   supported recipe write plan.
2. Read the slot list for the overwrite choice, using the slot selector and
   property reads.
3. After confirmation, select the target slot, write its name, then write the
   applicable recipe properties in plan order.
4. Read each step back and report its result. A successful property transaction
   does not imply that every later property was written successfully.

Implementation: [property definitions](../../app/src/main/java/dev/bondarenko/fujirecipes/camera/plan/CameraEncoding.kt),
[write plan](../../app/src/main/java/dev/bondarenko/fujirecipes/camera/plan/WritePlan.kt),
[slot reader](../../app/src/main/java/dev/bondarenko/fujirecipes/camera/usb/SlotReader.kt),
[write executor](../../app/src/main/java/dev/bondarenko/fujirecipes/camera/usb/WriteExecutor.kt),
and [PTP session](../../app/src/main/java/dev/bondarenko/fujirecipes/camera/ptp/PtpSession.kt).

## Bluetooth sequence

1. Connect to the supported paired camera and read the slots for the overwrite
   choice.
2. After confirmation, download a fresh backup inside `writeRecipe`. Validate
   its layout, settings checksum and recipe encodings.
3. Copy it, patch the selected 1,024-byte recipe record at
   `63356 + (slot - 1) * 1024`, update the settings checksum at offset 264, and
   durably save original, target and verification policy.
4. Compress the target, prepare restore with value `1` on
   `f1897eb9-a9aa-40f0-8ce6-2fa7915bc459`, and mark the operation pending before
   sending `backup.dat` metadata. Upload the complete compressed file in the
   currently measured 120-byte data chunks with sequence/checksum framing, then
   finish the transfer and save its acceptance state.
5. Reconnect and download a new backup. The `xt50-132-settings-v1` contract
   compares `[0,59920)` and `[63356,70524)`, including all seven recipe records.
   Only the firmware-established non-restored memory interval is excluded;
   literal whole-file equality remains a distinct result. Clear pending only
   after successful verification. Preserve evidence if any stage is uncertain.

Implementation: [BLE transport](../../app/src/main/java/dev/bondarenko/fujirecipes/camera/bluetooth/BluetoothCamera.kt),
[backup codec](../../app/src/main/java/dev/bondarenko/fujirecipes/camera/bluetooth/BluetoothBackup.kt),
[controller verification](../../app/src/main/java/dev/bondarenko/fujirecipes/camera/CameraController.kt),
and [recovery store](../../app/src/main/java/dev/bondarenko/fujirecipes/camera/bluetooth/BluetoothRecoveryStore.kt).
The verification rationale and the separate parked C1 AF-coordinate finding are
in [the integrity investigation](bluetooth-backup-integrity-2026-10-28.md).

## Research: can BLE upload only one slot?

The backup route was chosen because captured XApp traffic and measured backup
records supplied a working read/patch/restore protocol. Successful camera writes
and independent read-backs validate that route. They do not establish that it is
the camera's only possible route. A statement that selective BLE writing is
impossible would require evidence we do not have.

There are two different potential improvements: uploading only slot properties
or a slot record, and sending a full file with a documented apply-only-this-slot
option. Neither has been validated for this camera/firmware. Merely dividing a
full file into small BLE packets is transport chunking, not selective settings
upload or application.

| Primary evidence reviewed | Finding | What it does not establish |
|---|---|---|
| [Fujifilm XApp instructions][xapp] | The official UI saves and restores a named settings file over the Bluetooth connection. It also says to reselect the custom setting after restoration to reflect loaded settings. | The instructions do not state that every possible BLE settings write must use that file. |
| [`fffw` Bluetooth notes][fffw-notes] and [repository README][fffw-readme], revision `ccee949` | The author describes a shared GATT file-transfer protocol and zlib-compressed settings backups. The README describes C1–C7 editing in a backup parser; the BLE client and parser are listed as unpublished. | The available material supplies no reproducible slot-upload command or partial-apply mode. The unpublished implementation was not inspected. |
| [`fffw` GATT catalog][gatt] and [model/firmware map][gatt-map], revision `ccee949` | The map associates X-T50 1.31 with the catalog containing the known restore and file-transfer UUIDs, including 44-byte file metadata. | This is not an X-T50 1.32 command specification. UUIDs, writable flags and payload lengths cannot reveal or exclude undocumented slot semantics. |
| [`libfuji` BLE source][libfuji], revision `3328efc` (content also matches master at review), and [`furble` Fujifilm source][furble], revision `0cac22a` | `libfuji` identifies generic backup file metadata/data characteristics and implements shutter commands and a Wi-Fi handoff. The reviewed `furble` source concerns shutter/GPS control. Neither supplies a selective recipe uploader. | Missing functionality in a library is not proof that the camera lacks it. A BLE-initiated Wi-Fi session would be a different transport path. |
| [FujiStyle's own wireless guide][fujistyle] | The developer describes selecting a slot over pure Bluetooth, an initial settings read, and backup-request/settings-format errors. | A selected-slot UI does not disclose wire granularity. Those errors are consistent with backup-based reading, but do not prove either full-file or selective uploading. No corresponding upload capture or source was available in the reviewed material. |
| [Fujifilm Camera Control SDK interface list][sdk] | The published interfaces are USB and TCP/IP over Wi-Fi. | This supplies no public BLE recipe-property API. Wi-Fi control is not evidence of the same commands being accepted over BLE. |

The observed four-byte value `0` written to
`051dd980-df9d-4472-a2e1-35811dd24ee1` is not a demonstrated camera-memory or slot
offset. Nor is the negotiated chunk length a slot size. Replacing either with
`69500` or `1024`, or uploading a truncated settings file, would be a guess rather
than a supported selective-write experiment. USB property numbers likewise need
a demonstrated BLE encapsulation before they can be reused over GATT.

The conclusion is **no validated selective BLE method found**, with strong
evidence for the current whole-backup route and insufficient evidence for a
camera-wide impossibility claim. Existing captures and implementation establish
our route; public documentation and code do not settle all proprietary commands.
No speculative protocol upload was performed for this research.

## Follow-up: isolated-update checks, 29 October

**The deeper pass found no validated way to make the known BLE settings restore
slot-only. It identified an isolated-settings SDK API, but traced that API to
PTP rather than to an identified BLE binding.** This is stronger evidence about
the inspected paths, not a proof that every proprietary camera command has been
exhausted.

Static inspection used the XApp package already installed on Pixel:
**Android 2.7.6(1), versionCode 65**. This is an installed-version observation,
not a claim about the newest release. The APK was copied and inspected with the
existing Android SDK `dexdump` and NDK LLVM tools; XApp was not launched.
APK/DEX/native-library hashes and the disassembly are retained privately in
`captures/bluetooth/selective-upload-research-2026-10-29/xapp/`.

| Candidate | Concrete evidence | Result / limit |
|---|---|---|
| A slot number or apply mask in the restore request | `RestoreRequestInfo` contains camera ID, saved-file display name and request type. `BTCamera.writeRestoreRequest` sends the enum's u16 value to `f1897eb9-…`. | No slot or mask field was found in this client request path. The enum is `None=0`, `RestoreRequest=1`, `IPTCRequest=2`; value 2 is not a C2 selector. |
| A selector hidden in file metadata | `RestoreFileInformation` carries a filename and file size; the wire metadata is 40 filename bytes plus a u32 size. `startRestore` writes file index 0 immediately before that metadata. | No selector is carried in those metadata fields in the inspected path. Arbitrary filenames, indices or request values remain unvalidated. |
| A direct native recipe API | `libXAPI.so` exports `XSDK_SetCustomSettingParameter` at `0xb7b38`. Its setter at `0xbf888` calls parameter setters, which resolve through `CPTPCommand::SetDeviceProperty` at `0xe38f8` to `FTL_PTP_SetDevicePropValue`. The SDK contains `FTLPTP.so` and `FTLPTPIP.so` backend names. | An isolated property-setting API exists, but this trace is PTP-based. No binding of that API to the inspected BLE camera connection was identified. It is not evidence that the native API can simply be called over our GATT link. |
| Another open-source BLE recipe writer | [Dial In's restore code][dialin-restore] and [protocol reference][dialin-protocol], revision `848a651`, describe a locally patched single-slot experiment uploaded as a complete settings file. [Fuji Bridge's reader][bridge-backup], revision `38fc427`, also reads the full backup. | These provide additional implementations of the backup route, not a selective-upload command. Their other camera models/firmware do not establish X-T50 compatibility. |
| Separate slot files exposed by the backup service | On Pixel/X-T50 1.32, the opt-in read-only diagnostic read `e3fbbfcf-…` after settings-backup preparation. It returned **0**, yet file index 0 downloaded a valid **70,524-byte `backup.dat`** containing all seven recipe records. XApp reads this characteristic in its log-transfer path. | Zero cannot be interpreted as “there are no settings files”, since the normal download succeeds. This observation does not establish an inventory of separate recipe files or validate alternate indices. |
| A separate Bluetooth data channel carrying the native protocol | All 49 retained H4 captures parsed; 48 camera sessions were identified by backup-file metadata. They contained 16,310 ATT messages on CID `0x0004`, 92 LE signalling messages on `0x0005`, and 15 security messages on `0x0006`. No dynamically allocated channel was observed in those sessions. Channel roles follow the [Bluetooth specification][l2cap]. | No second Bluetooth data path was found in these recorded backup sessions. This does not rule out an unused channel or command in other operations or applications. |

Twelve captures containing exactly one upload were also independently decoded
with the existing capture extractor. Every one expanded to the full 70,524-byte
settings file. A further capture contains multiple uploads and was not counted
as twelve additional individually validated payloads. These recordings establish
the route actually used; repeated full-file uploads cannot prove the absence of
another route.

The previously collected [camera restore-handler evidence](bluetooth-backup-integrity-2026-10-28.md#next-delivery-firmware-bounded-settings-verification)
shows the known path forwarding a payload pointer/length, applying the settings
prefix, then restoring the seven-record section. Its checksum validator accepts
payload sizes `0xf6d4` or `0x112d4`; it does not establish a safe standalone
1,024-byte slot format. The temporary decoded firmware sections are no longer
present, so this pass reviewed the retained trace rather than reconstructing or
claiming a new exhaustive firmware analysis.

The new hardware observation is reproducible through
`CameraBluetoothHardwareTest#downloadSettingsBackup` with both
`bluetoothProbe=true` and `bluetoothFileInventory=true`. The latter adds only the
four-byte read after backup preparation and validates the resulting backup with
the production decoder. Combining it with any of the three restore diagnostic
flags is refused before connecting. Defaults and production transfer behavior
are unchanged.

Validation: `:app:assembleDebugAndroidTest` passed, only the diagnostic APK was
updated on Pixel, and the opt-in test passed in **4.59 seconds**. The saved backup
has a valid settings checksum and all 7,168 original recipe bytes. The 30-recipe
library and all 13 retained recovery/reference files are byte-for-byte unchanged;
no settings were uploaded and no pending checkpoint exists. Reports and before/
after snapshots are private under
`captures/bluetooth/selective-upload-research-2026-10-29/`.

The remaining question is whether a different BLE command or a file-level
selective-apply mechanism exists. Neither has been identified. No parameter
found in this pass can safely be repurposed as a C7-only write. A source or trace
showing a genuinely different upload/apply operation would change that result.

## Public-source recheck and Community Fuji capture, 15 November

**No publicly demonstrated slot-only BLE upload or selective-apply command was
found.** USB/PTP remains the demonstrated isolated-property route. This is an
evidence limit, not a proof that an undocumented BLE command cannot exist.

The public-source recheck adds a concrete commercial-client description:
[Fuji-X Recipes' terms, updated 9 November](https://fujixrecipes.app/terms),
section 4, explicitly describe reading the camera settings file, changing the
selected custom slot within it, and writing the file back over Bluetooth. This
is the developer's description, not our packet capture, and must not be
attributed to Community Fuji or FujiStyle.

[Dial In's protocol reference][dialin-protocol], still at revision `848a651`,
section 3.7 describes a fresh backup, one-record patch, checksum update, zlib
compression and complete-file restore. Its [restore implementation][dialin-restore]
uses file metadata, index 0 and sequenced file chunks. Its real-camera evidence
is X100VI 1.10, not X-T50 1.32. The reference labels the absence of a direct BLE
setting characteristic as a hypothesis. Searches for the restore UUID and
slot-property codes did not reveal a different published slot-only BLE writer;
the `fffw` tree remains at `ccee949`, with GATT maps rather than a newly published
selective writer.

The [USB protocol][usb-protocol] explicitly exposes a slot selector (`0xD18C`)
and property writes (`0x1016`) for individual slot values. Fujifilm's
[SDK interface list][sdk] names USB and TCP/IP via Wi-Fi, with Android listed as
USB only. Neither establishes these properties over Bluetooth or an X-T50
Wi-Fi recipe-writing route. A Bluetooth-triggered Wi-Fi session must not be
counted as a selective BLE implementation.

The attempted third-party capture used **Community for Fuji / Community Fuji
1.10.3 (versionCode 40), `co.fujicommunity.app`, on Pixel**, not FujiStyle.
Its initial slot read yielded 2,059 compressed bytes in 18 data chunks, expanding
to the complete 70,524-byte X-T50 backup. Transfer and settings checksums passed;
the camera reported firmware `01.32`, and C7 was `Kodak Negative`, Color +4.
The original is preserved privately as `camera-before.dat`.

A local `TEST Community C7`, Color 0 draft was prepared. The first write action
required sign-in; after the user signed in, the second reached a Pro paywall
for C4–C7. Both captures contain **zero settings-file metadata/data upload writes**.
No TEST preset reached the camera, so no restoration was required. This proves
the read format only; Community Fuji's upload format remains unobserved.
The user stopped the paid-app experiment and requested internet research.

The paywall and [Community Fuji's public site](https://fuji-community.com/)
identify C4–C7 as Pro, leaving C1–C3 as a possible no-purchase capture route.
That free-slot write was not attempted. Do not suggest payment is required for
every slot, or resume hardware writes after the user's switch to research.

Both recordings ended on complete records (594 and 640 respectively). The app
was closed, the temporary ADB forward removed, the HCI logging socket restored
off, and stay-awake restored to 0. Bluetooth is on and the user's originally
enabled HCI file logging remains enabled. Private evidence is under
`captures/bluetooth/community-fuji-2026-11-15/`; raw captures remain ignored.
Our previous app package is no longer installed on Pixel, so October's app
library/recovery state must not be described as currently present on that phone.
No production code or transfer policy changed in this pass.

## Community Fuji C2 upload and restoration, 15 November

The user subsequently authorized testing a free slot and specified **C2 for all
camera preset tests**, with a TEST prefix and restoration afterward. This
supersedes the earlier no-upload result for Community Fuji; the C7 attempt
remains a separate paywall-limited experiment.

Community Fuji 1.10.3 on Pixel wrote `TEST Community C2`, changing Color from +3
to 0, then restored `Kodak Portra 800 v3`, Color +3. Both operations succeeded
without a Pro purchase. The original complete backup was saved before editing.

| Captured operation | Compressed file bytes | Expanded file bytes | Verification |
|---|---:|---:|---|
| TEST C2 upload | 2,053 | 70,524 | Full `backup.dat`; compared with the fresh pre-upload read, only C2's name/Color and the settings checksum changed. Camera read-back exactly equals the upload. |
| Original C2 restoration | 2,048 | 70,524 | Full `backup.dat`; all seven 1,024-byte recipe records and all compared backed-up settings match the original, including a further read after disconnect/reconnect. |

These are compressed file payload sizes, excluding GATT/HCI overhead. The
existing capture decoder validated file metadata, chunk order, transport
checksum and zlib completion. Expanded settings checksums also passed. The
forward trace contains the initial read, a fresh pre-upload read, the complete
upload and a verification read. The restoration trace contains another fresh
read, complete upload, verification read and the later reconnect read.

The restoration read-back differs from its upload at offsets 63170–63173;
the final read differs from the initial original only at 63170. These are in
the previously established non-restored memory interval, outside the production
`xt50-132-settings-v1` comparison. Literal whole-file equality is therefore
false, while every original recipe byte and compared backed-up setting matches.
No comparison rule was changed and no firmware investigation was resumed.

**Conclusion:** for this tested operation, Community Fuji uses the same core
method as our `BluetoothCamera`/`BluetoothBackup` implementation: fresh complete
backup, local selected-slot patch plus checksum, zlib compression, complete-file
upload, then read-back. This establishes transfer granularity, not identical
source code, error handling, recovery policy or behavior on other models.

Both recordings ended with complete records and no partial bytes (1,370 forward,
1,182 restoration). Evidence is private under
`captures/bluetooth/community-c2-2026-11-15/`, including `forward-verified.json`,
`restoration-verified.json` and the original/upload/read-back files. The app is
closed, capture and forwarding stopped, logging socket restored off and
stay-awake restored to 0; Bluetooth and the user's HCI file logging remain on.
No library create/delete action was performed. No restoration remains pending.

## Evidence needed before changing the flow

1. Obtain a trace or published implementation of an actual selected-slot BLE
   write, including its before/after camera state. A comparable third-party app
   is a useful lead, but its UI description alone is insufficient.
2. Reassemble the outgoing transfer and inflate any compressed file. Determine
   whether it is a full settings backup, a slot-only payload, property commands,
   or a documented selective-apply request. Small compressed size alone proves
   none of these. Separate any Wi-Fi handoff from pure BLE traffic.
3. Only after identifying a concrete command and payload, validate one reversible
   `TEST` C7 edit on Pixel, independently verify other recipes and backed-up
   settings, then restore and verify the original. Preserve recovery evidence
   and existing user library recipes throughout.

The current flow remains unchanged pending that evidence. The follow-up used
existing firmware notes, static client inspection, recorded traffic and one
read-only download; broader firmware/AF investigation remains parked. Device
acceptance and the final C7 restoration are recorded in
[the current handover](bluetooth-handover-2026-10-27.md).

[usb-protocol]: https://github.com/ILFforever/fujifilm-ptp-recipes/blob/main/docs/protocol.md
[xapp]: https://digitalcamera-support-en.fujifilm.com/digitalcameraengpcdetail?aid=000008450
[fffw-notes]: https://github.com/tiredboffin/fffw/wiki/Notes-on-Bluetooth
[fffw-readme]: https://github.com/tiredboffin/fffw/blob/ccee949884a6dad10095231441e4d608c10e59f0/README.md
[gatt]: https://github.com/tiredboffin/fffw/blob/ccee949884a6dad10095231441e4d608c10e59f0/ffbt/cfg/gatt-ad14d4.yaml
[gatt-map]: https://github.com/tiredboffin/fffw/blob/ccee949884a6dad10095231441e4d608c10e59f0/ffbt/cfg/gatt-profiles-map.csv
[libfuji]: https://github.com/petabyt/libfuji/blob/3328efc51f74d754f8fa76af70c6becdde62271f/lib/bluetooth.c
[furble]: https://github.com/gkoh/furble/blob/0cac22aacc3d9ef250be40849118af78a5f39f27/lib/furble/Fujifilm.cpp
[fujistyle]: https://www.fujistyleapp.com/guides/wireless-import/
[sdk]: https://www.fujifilm-x.com/global/camera-control-sdk/
[dialin-restore]: https://github.com/maam00/dialin-tools/blob/848a651c926a44c6f7b5bc1d1f44b27e8a50b959/ios/DialInCamera/Sources/DialInCamera/BLE/FujiBLERestore.swift
[dialin-protocol]: https://github.com/maam00/dialin-tools/blob/848a651c926a44c6f7b5bc1d1f44b27e8a50b959/docs/FUJIFILM-PROTOCOL.md
[bridge-backup]: https://github.com/Th0rgal/fuji-bridge/blob/38fc4275574c2016a1f13ece13496b0ad1e9b452/FujiBridge/FujiBackup.swift
[l2cap]: https://www.bluetooth.com/wp-content/uploads/Files/Specification/HTML/Core-61/out/en/host/logical-link-control-and-adaptation-protocol-specification.html
