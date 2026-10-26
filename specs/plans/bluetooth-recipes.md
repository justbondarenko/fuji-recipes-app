# Bluetooth recipe connection

Hardware investigation completed on 26 October 2026 on branch `bluetooth-connection`. The debug probe can connect to the paired X-T50 1.32 after a normal camera restart, download its settings over Bluetooth, change a complete measured C7 recipe, verify persistence, and restore the original recipe. The app now includes read-only Bluetooth connection, C1–C7 views and library import; product recipe writes still require USB.

## Verified scope

The investigation used Fujifilm X-T50 firmware 1.32 and Pixel 10 Pro XL on Android 17 / API 37, with an existing XApp-created Bluetooth bond. Phone Wi-Fi was disabled and XApp was stopped before and after native operations. Camera USB was disconnected for Bluetooth transfers. The read-only app was subsequently tested on that Pixel and Samsung SM-F976B, also API 37. This does not establish native initial pairing, older Android versions, other camera models/firmware or writes to C1–C6.

| Check | Result |
|---|---|
| Independent Bluetooth settings read | Validated transfer checksum, zlib EOF, declared length, header and model; 70,524 bytes |
| USB whole-backup read | RAW CONV./BACKUP RESTORE object zero returns the same layout; all seven recipe records matched the corresponding BLE read |
| Independent USB reference | 24 properties per slot across C1–C7, preserving and reading back the original selector |
| Research decoder | Seven names and 112 field values agree with independent USB reads |
| Research C7 encoder | Reproduces 569 measured USB changes byte-for-byte, including the metadata update; unchanged backup preserved and invalid edits rejected |
| Native unchanged restore | Fresh download → upload → independent reconnect/read-back matched all 70,524 bytes |
| Controlled Color proof | C7 +4 → 0 → +4, power-cycle persistence and restoration verified |
| Complete C7 recipe proof | Changed 16 settings and the test name; repeat upload and independent read-back matched all 70,524 bytes |
| Full-recipe persistence | After user power cycle, all seven recipe records matched the expected test-state records exactly |
| Original restoration | All original seven-slot records restored; independent HCI reconstruction and native full read-back matched the uploaded fresh snapshot |
| Standalone cold startup | Corrected client identity; normal off/on followed by native read passed in 4.132 seconds with XApp closed |

The camera is back at its original recipe state. C7 includes Color +4, Sharpness -1, DR400, Highlight -0.5 and Grain Weak/Large. Temporary write-reference files were removed from the phone. No capture or instrumentation is running. The user changed AUTO POWER OFF during testing; its previous and current values were not supplied, so do not restore a guessed value.

## Protocol and format established by measurement

The BLE operation transfers a whole settings backup, rather than the existing USB PTP property commands. Reconstructing HCI traffic independently verifies the native transfer. Downloads use sequence/length-framed chunks, a sum16 checksum over compressed bytes and zlib data. Restore metadata declares compressed size; the observed final upload packet repeats its checksum. Relevant UUIDs and exact framing are implemented in the opt-in `CameraBluetoothHardwareTest`.

Secure reconnect uses Android's existing encrypted bond. The client identity captured from XApp is the phone's Bluetooth name with spaces replaced by hyphens, followed by `-2065` and NUL. The original probe invented another name; the corrected platform-derived identity passed the subsequent off/on test. The suffix remains a measured constant whose meaning is unknown; do not assume it is universal. Native initial pairing and restart after battery removal have not been independently repeated with this correction.

The observed backup has header `FUJIFILMX-BACKUP`, model text `X-T50`, exactly 70,524 bytes, and seven 1,024-byte records beginning at 63356. These are model/layout gates, not permission to accept other firmware. The relative metadata update at 264 equals the changed recipe-byte sum across the measured C7 operations. This is sufficient for the bounded proof, but does not claim a general checksum algorithm or validated C1–C6 encoding.

| Field | Relative record offset | Measured coverage |
|---|---:|---|
| Kelvin | 16, little-endian u16 | 2500–10000 at 100K increments |
| WB mode | 114 | Auto, Ambience, Daylight, Shade, three Fluorescent modes, Incandescent, Underwater and Kelvin |
| WB red/blue shift | Mode-specific | Every -9..+9 value; encoded as `9 - shift` |
| High ISO NR | 172 | -4..+4 |
| Clarity | 175 | -5..+5 |
| Film simulation | 229 | All 20 existing app simulations |
| Remembered ACROS/Monochrome variant | 232 / 235 | Film changes also update these family choices; preserve or update deliberately |
| Dynamic range | 245 | DR100, DR200 and DR400 |
| Color | 248 | -4..+4; nonlinear lookup |
| Sharpness | 250 | -4..+4 |
| Highlight / Shadow | 252 / 254 | -2..+4 in half steps |
| Color Chrome / Blue | 256 / 257 | Off, Weak, Strong |
| Grain strength / size | 258 / 259 | Strong=0, Weak=1, Off=2; Small=0, Large=1 |
| Name | 622 | Printable ASCII through 25 characters; 26/31/32 refused |

WB red/blue banks are Auto 116/118, Ambience 124/126, Daylight 144/146, Shade 148/150, Fluorescent 1 152/154, Fluorescent 2 156/158, Fluorescent 3 160/162, Incandescent 164/166, Underwater 168/170 and Kelvin 140/142. Other WB modes, monochromatic color and D-Range Priority are not mapped for BLE. The tested USB name setter did not preserve non-ASCII text; the research encoder therefore rejects it.

Do not reuse USB grain read-back codes as write codes. Off is written as 1 but reads as 6 or 7 depending on remembered size; writing 6/7 was refused. These findings do not change the production USB encoder in this stage.

Immediate operation switches were refused; the probe currently uses measured three-second pauses. A real readiness signal remains preferable to widening arbitrary retry loops. A transfer while camera USB remained attached also stalled in this setup. Camera sleep/menu state can prevent preparation. A first complete restore after USB work changed two bytes outside the recipe region; the strict comparison caught this. Subsequent original restoration and repeat recipe write matched every byte. No universal exclusion mask was added.

## Integration delivery plan

### Read-only app stage delivered

`BluetoothBackup` is a pure Kotlin decoder with a synthetic 70,524-byte fixture and an independently decoded golden result. It checks the complete layout and all seven slots before returning recipes. The shipping `BluetoothCamera` retains checksum, zlib EOF, size/model/firmware gates and bounded, serial GATT operations. It has no settings-upload entry point and saves no raw backups. Transfer subscriptions are enabled for each download, so Refresh and bulk import work on an existing connection.

The existing Camera sheet now offers USB/Bluetooth selection and the Android Nearby devices permission request. Bluetooth uses one already bonded X-T50 and displays a read-only label. Slot details reuse the validated snapshot from the most recent complete read; Refresh or reopening the Camera sheet obtains a fresh snapshot. Bulk import always reads afresh. USB writes, Photos and RAW Lab retain their USB paths, with Bluetooth refusals in both UI and controller. USB host support is optional for installation and does not suppress the Bluetooth choice.

Both phones passed the actual app flow: connect with XApp stopped, read seven slot names, view C7, save C7, refresh, release/reconnect, review and import all seven slots. Persisted imports matched across phones for all seven names and 112 field values. Both imported sets also matched the independent research decoder of the previously restored camera backup. Existing recipes were unchanged; all temporary imports were then removed and both original library files restored byte-for-byte. Pixel Wi-Fi was off; Samsung Wi-Fi remained on. The app has no Internet permission and this transport uses only Android BLE APIs. Camera recipes were not written during this stage.

The compatibility ceiling remains X-T50 1.32 with an existing bond. Older Android callback/write APIs are implemented but untested on hardware; both available phones run API 37. Unsupported backup layouts, field codes and non-ASCII names fail the whole read. Auto White Priority/custom WB, monochromatic color and D-Range Priority remain unmapped. Native pairing and general Bluetooth writes are separate stages.

The remaining steps below describe the wider write-capable delivery; they do not enable product Bluetooth writes in this stage.

1. Finish the format boundary before enabling general writes. Produce sanitized, checked-in examples from the measured layout, retaining unknown bytes without personal names/identities. Verify C1–C6 changes and restoration before advertising writes to every slot. Refuse unsupported model/firmware/layout and unsupported requested values. Unknown/global data must be preserved from a fresh snapshot, never copied from an old complete backup.

2. Move the proven transport into the app with native Android BLE APIs. Keep serialized GATT requests, bounded queues/timeouts, disconnect handling and all integrity checks. The API-37 encryption-status diagnostic needs an explicitly implemented/tested compatibility path before claiming older Android support. Initial delivery may use the existing bond, with clear pairing help; native initial pairing needs a separate hardware proof. Keep pairing data private and never log credentials.

3. Add the smallest transport boundary required by the existing USB and new BLE implementations. `CameraController` currently owns a concrete `PtpSession`; `readSlots`, `readSlotRecipes`, `readSlotRecipe` and `write` are the recipe integration points. Reuse `SlotRecipe`, recipe validation and library import behavior. The USB `WritePlan` contains PTP property operations, so it must not be passed to BLE as though it were a transport-neutral recipe. Leave USB Photos and RAW Lab on their existing sessions and wake-lock/transfer paths.

4. Add an explicit USB/Bluetooth choice to the existing Camera screen, preserving its UI language and using only FujiIcons. Handle Android Bluetooth permissions in the existing app permission flow. Do not imply Bluetooth Photos or RAW Lab support. Revisit the USB-host-required installation filter and `NoUsbHost` state so they do not suppress a valid Bluetooth option. Keep offline behavior; BLE alone does not require Internet. No recipe-library schema migration is expected; connection/bond metadata is separate app-private state.

5. Verify the user-facing flow on the connected Pixel and X-T50: cold connect without XApp, all-slot import, recipe selection, supported-field preflight, controlled write, independent verification, disconnect/reconnect and restoration. Include unsupported values, camera sleep/menu state, cancellation, interrupted transfer and stale-snapshot refusal. Run the relevant camera/recipe JVM tests and debug build once integration changes land. Current private capture checks are hardware research evidence, not checked-in CI fixtures.

6. Deliver in coherent stages: diagnostic proof, read-only app import, controlled write support, then broader compatibility. Review the full diff at each stage. Do not enable writes beyond hardware-verified scope or declare unobserved devices compatible. The first two stages are delivered; general recipe writing remains disabled over Bluetooth.

## Local verification and recovery

Private captures, backups, helpers and complete checkpoints remain under the ignored `captures/bluetooth/xt50-132-pixel/`. The standard-library research checks are `decode_slots.py --check`, `check_extraction.py` and `read_usb_slots.py --check`; they require those private research files. The USB helper's normal modes only change/restore the selector; explicit bounded calibration modes temporarily measured C7 fields and verified restoration.

Build the diagnostics with `./gradlew :app:assembleDebug :app:assembleDebugAndroidTest`. With the debug app/test APK installed, Bluetooth permission granted and XApp closed, run the hardware test using `bluetoothProbe=true`; without this opt-in it skips. `bluetoothRoundTrip`, `bluetoothColorProof` and `bluetoothRecipeProof` are separate, mutually exclusive restore opt-ins. Edited proofs require app-private, independently checked reference files; those files have been removed after restoration. A failed run deletes its old result, so only retrieve a backup after `OK (1 test)`.

For the shipping reader regression, run `CameraBluetoothHardwareTest#readRecipesWithAppTransport` with `-e bluetoothAppRead true`. It reads all seven slots twice on one connection and requires the results to match, without writing settings or a backup file. Run phones sequentially so the camera has only one client. `CameraTransportTest` checks the Bluetooth choice on a state without USB host support, disabled transport changes while connecting and the read-only label; it needs no camera. Parser and write-capability checks run in `:app:testDebugUnitTest`. The final build passed 737 JVM tests and both device checks on each phone (four instrumentation checks). Samsung also displayed the native Nearby devices prompt, handled denial with actionable guidance and reconnected after its original permission was restored.

Every product write must start with a fresh validated backup, patch only supported fields in the selected slot and measured metadata, preserve all other fresh bytes, then reconnect and verify. On interruption, read actual camera state before deciding recovery; do not replay a whole-settings restore blindly. Retain original recipe data and report whether the transfer was accepted, the recipe read back correctly, and the complete snapshot matched. Disable Bluetooth writes when format/integrity validation fails; USB remains the established recovery route. No server, account, migration or external radio sniffer is required for this path.

Protocol source leads include the inspected [libfuji Bluetooth implementation](https://github.com/petabyt/libfuji/blob/3328efc51f74d754f8fa76af70c6becdde62271f/lib/bluetooth.c), its [USB backup reader](https://github.com/petabyt/libfuji/blob/master/lib/fuji_usb.c), and [Fujifilm XApp](https://www.fujifilm-x.com/en-us/products/software/xapp/). Their existence is background; the compatibility claims above come from this camera's measured responses.
