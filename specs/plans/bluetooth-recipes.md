# Bluetooth recipe connection

Hardware investigation completed on 26 October 2026 on branch `bluetooth-connection`. The debug probe can connect to the paired X-T50 1.32 after a normal camera restart, download its settings over Bluetooth, change a complete measured C7 recipe, verify persistence, and restore the original recipe. The app now includes Bluetooth connection, C1–C7 views, library import and controlled C7 writing. C1–C6 writes still require USB.

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

### Controlled C7 writing

The production Kotlin encoder is restricted to the measured C7 record and relative metadata update. Its checked-in synthetic fixture and 566 sanitized field-change cases reproduce independent USB measurements; names, unknown fields, ranges and metadata overflow are validated before an upload. Names must be 1–25 printable ASCII characters. Unsupported non-advisory fields are refused. Unspecified settings, inactive WB banks and every other fresh backup byte are preserved; remembered Kelvin is ignored unless the recipe explicitly selects Kelvin WB. These rules do not imply that an unmapped field was reset to a default.

The Bluetooth write picker exposes C7 only. The controller uses the recipe settings directly, never USB PTP property steps. Every write downloads a fresh, fully validated backup and durably stores original and intended snapshots under app-private `noBackupFilesDir/bluetooth-c7-recovery/`. A pending marker is written before settings upload. If a previous upload was unresolved, a new fresh read must match its complete original or intended snapshot before another write is permitted. No byte-exclusion mask or automatic whole-backup rollback is used.

Cancellation is accepted before settings upload; once upload begins, the operation finishes under a wake lock and non-cancellable coroutine, then reconnects and verifies all 70,524 bytes. Only a complete match reports success. Failure closes the connection, retains recovery data and suppresses the write sheet's direct Retry action. The original backup remains local after success until the next write replaces the checkpoint. Recovery export/UI and physical interruption tests are not delivered in this stage; uncertain states require inspection, with USB remaining the recovery route.

On the available Pixel, three separate connections and six shipping-reader downloads passed with XApp stopped. The production controller then wrote C7 Color to 0 and restored its original +4; both writes passed independent reconnect verification, and the separate research probe matched the complete original backup after restoration. This verifies the shipping C7 transport on Pixel, not C1–C6 writes or Samsung writes. The broader complete-recipe hardware proof above remains separate research evidence.

Testing found a foreground-service startup race: its start command could observe Writing or Disconnected instead of Connected and return before fulfilling Android's foreground-service requirement. The shared service now promotes before deciding to stop and keeps a notification during Writing. A device regression waits beyond Android's failure deadline to detect the original asynchronous crash. The hardware test uses the application's real controller so the service observes the same connection state.

Connection timeouts were reproduced before recipe transfer; waking/restarting the camera allowed the reader checks to pass. Status 147 is a link timeout, not proof of a bad bond. BLE requests now stop promptly when a disconnect arrives, including while waiting for notifications or MTU. No automatic write replay or broad connection retry loop was added. A separate readiness-notification timeout also occurred; its exact cause is not established. After the user's power cycle and +4 menu confirmation, a private HCI trace captured successful LE connection, Start Encryption, then disconnect reason 0x08 without a successful encryption event. One complete Pixel adapter OFF/ON cycle, preserving the bond and restoring `ble_scan_always_enabled` from 1 back to 1, recovered three connections and six reads. This establishes a recovery procedure, not an unassisted cold-start guarantee or proof of a firmware cause. The app explains the manual phone Bluetooth toggle if timeouts persist; it never resets the adapter itself. A readiness polling experiment was removed rather than shipped without evidence. Camera sleep, cold restart and multi-phone behavior must remain part of hardware regression testing.

The build passes 738 JVM tests and three Pixel UI/service checks. The cancellation-after-backup test passed after the adapter recovery: no upload began and an independent read matched the complete original backup. The user confirmed C7 Color +4 after power cycling. The normal UI write flow remains to be completed; the existing library's Kodak Negative matches C7 and can be used without changing the recipe. Actual UI testing found that an idle disconnect still presented a USB-only Upload tooltip. With Bluetooth selected, Upload now opens the existing connection stage even when disconnected, waits for that connection attempt to finish, and displays an actionable failure in the same sheet. The app connected, showed all seven read slots and the C7-writing label, and the disconnected Upload/reconnection-error path was exercised; the final C7 picker → write → completion UI check awaits another camera wake after a subsequent link timeout. The library was verified unchanged byte-for-byte after the hardware tests. Samsung is no longer connected; this stage continues on Pixel only at the user's request. The previous AUTO POWER OFF value remains unknown, so it must not be restored by guessing.

### Read-only app stage delivered

`BluetoothBackup` is a pure Kotlin decoder with a synthetic 70,524-byte fixture and an independently decoded golden result. It checks the complete layout and all seven slots before returning recipes. The shipping `BluetoothCamera` retains checksum, zlib EOF, size/model/firmware gates and bounded, serial GATT operations. It has no settings-upload entry point and saves no raw backups. Transfer subscriptions are enabled for each download, so Refresh and bulk import work on an existing connection.

The existing Camera sheet now offers USB/Bluetooth selection and the Android Nearby devices permission request. Bluetooth uses one already bonded X-T50 and displays a read-only label. Slot details reuse the validated snapshot from the most recent complete read; Refresh or reopening the Camera sheet obtains a fresh snapshot. Bulk import always reads afresh. USB writes, Photos and RAW Lab retain their USB paths, with Bluetooth refusals in both UI and controller. USB host support is optional for installation and does not suppress the Bluetooth choice.

Both phones passed the actual app flow: connect with XApp stopped, read seven slot names, view C7, save C7, refresh, release/reconnect, review and import all seven slots. Persisted imports matched across phones for all seven names and 112 field values. Both imported sets also matched the independent research decoder of the previously restored camera backup. Existing recipes were unchanged; all temporary imports were then removed and both original library files restored byte-for-byte. Pixel Wi-Fi was off; Samsung Wi-Fi remained on. The app has no Internet permission and this transport uses only Android BLE APIs. Camera recipes were not written during this stage.

The compatibility ceiling remains X-T50 1.32 with an existing bond. Older Android callback/write APIs are implemented but untested on hardware; both available phones run API 37. Unsupported backup layouts, field codes and non-ASCII names fail the whole read. Auto White Priority/custom WB, monochromatic color and D-Range Priority remain unmapped. Native pairing and general Bluetooth writes are separate stages.

The original integration sequence below covers delivered and remaining work. The historical read-only stage above was followed by the controlled C7 stage; general C1–C7 writing remains disabled.

1. Finish the format boundary before enabling general writes. Produce sanitized, checked-in examples from the measured layout, retaining unknown bytes without personal names/identities. Verify C1–C6 changes and restoration before advertising writes to every slot. Refuse unsupported model/firmware/layout and unsupported requested values. Unknown/global data must be preserved from a fresh snapshot, never copied from an old complete backup.

2. Move the proven transport into the app with native Android BLE APIs. Keep serialized GATT requests, bounded queues/timeouts, disconnect handling and all integrity checks. The API-37 encryption-status diagnostic needs an explicitly implemented/tested compatibility path before claiming older Android support. Initial delivery may use the existing bond, with clear pairing help; native initial pairing needs a separate hardware proof. Keep pairing data private and never log credentials.

3. Add the smallest transport boundary required by the existing USB and new BLE implementations. `CameraController` currently owns a concrete `PtpSession`; `readSlots`, `readSlotRecipes`, `readSlotRecipe` and `write` are the recipe integration points. Reuse `SlotRecipe`, recipe validation and library import behavior. The USB `WritePlan` contains PTP property operations, so it must not be passed to BLE as though it were a transport-neutral recipe. Leave USB Photos and RAW Lab on their existing sessions and wake-lock/transfer paths.

4. Add an explicit USB/Bluetooth choice to the existing Camera screen, preserving its UI language and using only FujiIcons. Handle Android Bluetooth permissions in the existing app permission flow. Do not imply Bluetooth Photos or RAW Lab support. Revisit the USB-host-required installation filter and `NoUsbHost` state so they do not suppress a valid Bluetooth option. Keep offline behavior; BLE alone does not require Internet. No recipe-library schema migration is expected; connection/bond metadata is separate app-private state.

5. Verify the user-facing flow on the connected Pixel and X-T50: cold connect without XApp, all-slot import, recipe selection, supported-field preflight, controlled write, independent verification, disconnect/reconnect and restoration. Include unsupported values, camera sleep/menu state, cancellation, interrupted transfer and stale-snapshot refusal. Run the relevant camera/recipe JVM tests and debug build once integration changes land. Current private capture checks are hardware research evidence, not checked-in CI fixtures.

6. Deliver in coherent stages: diagnostic proof, read-only app import, controlled C7 write support, then broader compatibility. Review the full diff at each stage. Do not enable writes beyond hardware-verified scope or declare unobserved devices compatible. General C1–C7 writing remains disabled over Bluetooth.

## Local verification and recovery

Private captures, backups, helpers and complete checkpoints remain under the ignored `captures/bluetooth/xt50-132-pixel/`. The standard-library research checks are `decode_slots.py --check`, `check_extraction.py` and `read_usb_slots.py --check`; they require those private research files. The USB helper's normal modes only change/restore the selector; explicit bounded calibration modes temporarily measured C7 fields and verified restoration.

Build the diagnostics with `./gradlew :app:assembleDebug :app:assembleDebugAndroidTest`. With the debug app/test APK installed, Bluetooth permission granted and XApp closed, run the hardware test using `bluetoothProbe=true`; without this opt-in it skips. `bluetoothRoundTrip`, `bluetoothColorProof` and `bluetoothRecipeProof` are separate, mutually exclusive restore opt-ins. Edited proofs require app-private, independently checked reference files; those files have been removed after restoration. A failed run deletes its old result, so only retrieve a backup after `OK (1 test)`.

For the shipping reader regression, run `CameraBluetoothHardwareTest#readRecipesWithAppTransport` with `-e bluetoothAppRead true`. It reads all seven slots twice on one connection and requires the results to match, without writing settings or a backup file. Run phones sequentially so the camera has only one client. `CameraTransportTest` checks the Bluetooth choice on a state without USB host support, disabled transport changes while connecting and the read-only label; it needs no camera. Parser and write-capability checks run in `:app:testDebugUnitTest`. The final build passed 737 JVM tests and both device checks on each phone (four instrumentation checks). Samsung also displayed the native Nearby devices prompt, handled denial with actionable guidance and reconnected after its original permission was restored.

The current reader regression repeats that pair of reads over three independent connections. For the C7 controller round trip, use `CameraBluetoothHardwareTest#writeC7WithAppTransportAndRestore` with `-e bluetoothAppWrite true`. It saves a private original, changes Color, verifies through the production controller and separate research probe, restores the original value, and requires a complete byte-for-byte match. Only a verified forward change is automatically reversed; an uncertain upload requires inspection. `#cancelC7BeforeUploadLeavesCameraUnchanged` with `-e bluetoothAppCancel true` checks cancellation after backup but before upload against an independent full read. These tests require the camera awake and XApp stopped. Never upload the synthetic fixture.

Every product write must start with a fresh validated backup, patch only supported fields in the selected slot and measured metadata, preserve all other fresh bytes, then reconnect and verify. On interruption, read actual camera state before deciding recovery; do not replay a whole-settings restore blindly. Retain original recipe data and report whether the transfer was accepted, the recipe read back correctly, and the complete snapshot matched. Disable Bluetooth writes when format/integrity validation fails; USB remains the established recovery route. No server, account, migration or external radio sniffer is required for this path.

Protocol source leads include the inspected [libfuji Bluetooth implementation](https://github.com/petabyt/libfuji/blob/3328efc51f74d754f8fa76af70c6becdde62271f/lib/bluetooth.c), its [USB backup reader](https://github.com/petabyt/libfuji/blob/master/lib/fuji_usb.c), and [Fujifilm XApp](https://www.fujifilm-x.com/en-us/products/software/xapp/). Their existence is background; the compatibility claims above come from this camera's measured responses. Android documents status 147 as [GATT_CONNECTION_TIMEOUT](https://developer.android.com/reference/android/bluetooth/BluetoothGatt#GATT_CONNECTION_TIMEOUT); that code alone does not diagnose the camera's physical state or pairing.
