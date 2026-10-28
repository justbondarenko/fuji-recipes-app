# Fujifilm Bluetooth development handover

Prepared on **27 October 2026**, Europe/Belgrade, against code commit
**`7550ed302003437446c74775af359883e5bf64d2`** on `bluetooth-connection`.
This is a current checkpoint and continuation guide. The chronological research
record is [bluetooth-recipes.md](bluetooth-recipes.md); its early sections describe
older delivery stages and must not override the current state below.

## 1. Start here

**The camera's original C1–C7 recipes are restored. Bluetooth reading, import,
native pairing and a guarded writer are implemented. Further Bluetooth uploads
on the current Pixel are deliberately blocked by a genuine unresolved-write
checkpoint. Preserve that checkpoint.**

The blocker is complete-backup verification: a controlled C1 write reached the
camera and its recipe records matched the intended result, but four nonrecipe
byte positions differed. Those positions also change during read-only operation.
Their meaning is still unknown. This is not permission to ignore them, declare
the write successful, or delete the pending marker. C1 was subsequently restored
through a single USB property write, independently checked across all slots.

The next useful stage is a bounded investigation of the backup format and its
integrity/volatile data, using existing private captures first. Keep production
verification exact until there is independently validated evidence for a
different rule. Do not repeat broad physical experiments or attempt another
Bluetooth write merely to see whether it works this time.

The latest task was read-only. It completed successfully after fixing a
diagnostic notification lifecycle bug. It did not identify the unknown field.
There is no pending request for the user to change a recipe or camera setting.

## 2. Repository and delivery checkpoint

| Item | Checkpoint |
|---|---|
| Workspace | `/Users/andrii/Developer/Personal/fuji-recipes-app` |
| Branch | `bluetooth-connection` |
| Code HEAD | `7550ed302003437446c74775af359883e5bf64d2` |
| Git status before this document | Clean |
| Remote | `git@github.com:justbondarenko/fuji-recipes-app.git` |
| Cached tracking state | HEAD is 8 commits ahead of `origin/bluetooth-connection`, 0 behind; no fetch performed for this document |
| Remote CI / PR state | Not checked for this checkpoint; do not claim green or published |
| Recent delivery | Local, signed SSH commits; no push during these recent stages |
| App ID / test runner | `dev.bondarenko.fujirecipes` / `dev.bondarenko.fujirecipes.test/androidx.test.runner.AndroidJUnitRunner` |
| Android project | min SDK 29, compile/target SDK 37; hardware compatibility is narrower than installation support |

| Commit | Delivery |
|---|---|
| `7550ed3` | Read-only device/lens metadata probe, per-download notification fix in the research probe, findings |
| `1248e9b` | Real pending-checkpoint inspection and persistence verification |
| `eb28f6d` | Retained verification evidence, recovery store and recovery UI/export |
| `862de7d` | USB recovery evidence and read-only backup drift findings |
| `03e9e44` | Native pairing and C1–C7 Bluetooth writer integration |
| `44b260a` | Shared modal bottom-sheet defaults |

Read `AGENTS.md`, `PRD.md` and `specs/steering/coding-standards.md` before editing.
Use existing concrete controller/store/transport classes; no speculative
transport framework or dependencies are needed. Keep pure format logic separate
from Android I/O. All icons must use Google Material Symbols Rounded through
`FujiIcons`, never Compose Material Icons or XML icon vectors. Preserve configured
commit signing; do not bypass a signing failure without user authorization.

## 3. Hardware and actual state

| Item | Last verified / limitation |
|---|---|
| Camera | Fujifilm X-T50, firmware 1.32; GATT model `FF240001`, firmware `01.32` |
| Available phone | Pixel 10 Pro XL, Android 17 / API 37, ADB serial `58061FDCQ0057S` |
| Availability at document preparation | ADB lists that Pixel as `device`; Samsung is absent |
| User's current scope | **Pixel only**; do not resume Samsung tests without new direction |
| Camera recipes | All 7,168 original recipe-record bytes matched in the latest two Bluetooth backups |
| Useful reference values | C1 Color +3; C7 Color +4, Sharpness −1, DR400, Highlight −0.5, Grain Weak/Large |
| Latest independent USB recovery | All 168 original property readings across C1–C7 matched; original selector restored |
| Power management | Last directly confirmed AUTO POWER OFF is **1 MIN**, despite earlier requests/reports of OFF |
| Drive / AUTO lever | Last controlled state is CL Low-speed Burst; AUTO lever outside AUTO |
| Camera USB | Unplugged during the latest successful Bluetooth probe |
| Pixel Wi-Fi / XApp | Wi-Fi was off; XApp stopped for native tests. Recheck before future hardware work |
| Pairing | Current Pixel bond was established natively through Fuji Recipes and Android consent/code confirmation |
| Current physical state | Camera was restarted and awake for the last probe; do not assume it is still awake, unlocked or charged later |

Do not change AUTO POWER OFF, DRIVE, exposure, lens position or recipes to make a
test convenient. Ask for a shutter half-press immediately before a prepared live
read if needed. The user has already done many wake/restart/charging cycles;
prepare and install first, then request the smallest necessary physical action.

## 4. What is implemented and what is actually proven

| Capability | Evidence and boundary |
|---|---|
| Bluetooth connection and settings download | Native Android BLE; compressed transfer framing/checksum/size/zlib/model validation; repeated real Pixel reads |
| C1–C7 views and library import | Real app flow passed on Pixel and Samsung SM-F976B/API 37 in the earlier read-only stage; temporary imports removed and original libraries restored |
| Native initial pairing | Pixel chooser, code confirmation, encrypted reads and camera restart reads passed without XApp; Samsung native pairing is untested |
| C7 full recipe encoding | Independent USB measurements cover supported fields/name; complete research write and restoration passed on the prior bond |
| C1–C6 writing | Separate Color forward/restoration trials passed through the production controller, with independent complete-backup checks on the prior bond |
| Current native-bond C1 write | Upload accepted and intended recipe bytes read back; strict whole-backup check failed. **Not a passed write proof** |
| Current camera recovery | Original C1 Color restored using USB property `0xd19f`, scalar 30; all original recipe data independently verified |
| Recovery UI and persistence | Retains evidence, blocks new uploads, supports read-only inspection and ZIP export; synthetic and genuine-checkpoint tests plus actual UI/export checks passed |
| Latest metadata probe | Two valid backups and eight device/lens reads passed on Pixel; original recipes and protected files unchanged |
| Other firmware / camera models / older Android | Not hardware-verified; do not widen compatibility gates |
| Samsung Bluetooth writes | Not verified |
| Physical interruption during an upload | Not completed as a controlled physical interruption test; synthetic crash/persistence checks do not replace it |
| Bluetooth Photos / RAW Lab | Unsupported; existing USB paths remain in use |

Earlier normal C7 UI completion passed. A normal C1 UI attempt was interrupted by
battery loss before settings data was uploaded; later C1 UI completion was
recorded in the chronological plan. These older successes do not resolve the
new native-bond strict-verification failure or the genuine current lock.

## 5. Why the write lock exists

During the native-bond C1 proof, the controlled edit was Color +3 → 0. Independent
HCI reconstruction showed that the transmitted backup matched the intended
target exactly: metadata byte 264 and C1 Color byte 63604 were the intended
changes. The camera acknowledged the upload. Read-back matched every intended
recipe-record byte, but offsets **63170–63173** differed outside the recipe
records. The app correctly reported failure and retained the checkpoint.

No reverse Bluetooth upload was attempted after that uncertainty. USB recovery
restored only C1 Color to +3, rather than replaying an old complete settings
backup. The camera now matches the original recipes, but its nonrecipe settings
do not exactly match either historical checkpoint. Therefore the marker stays.

The retained target still describes C1 Color 0. A saved inspection describes the
restored original recipes. That disagreement is expected historical evidence,
not evidence that the camera currently has Color 0.

The app's `accepted` flag was introduced after the genuine failed trial. Its
absence for that old checkpoint means acknowledgement was **not recorded by
that app version**, not that HCI disproved an acknowledgement.

## 6. Protected evidence: preserve before doing anything else

The legacy directory name `no_backup/bluetooth-c7-recovery/` intentionally remains
in use for **every slot** so old checkpoints are discoverable. Do not rename it
without migration. Paths below are relative to the app's private data root.

| File | Purpose / checkpoint state |
|---|---|
| `no_backup/bluetooth-c7-recovery/original.dat` | Genuine pre-upload full backup; 70,524 bytes |
| `no_backup/bluetooth-c7-recovery/target.dat` | Genuine intended full backup; 70,524 bytes; C1 Color 0 |
| `no_backup/bluetooth-c7-recovery/pending` | Genuine unresolved-write marker, one byte `01`; must remain |
| `no_backup/bluetooth-c7-recovery/inspection.dat` | Latest production recovery inspection, 70,524 bytes; original recipes match, nonrecipe bytes differ |
| `no_backup/bluetooth-slot-1-hardware-original.dat` | Independent hardware-proof original; 70,524 bytes |
| `files/library.json` | Original phone library retained byte-for-byte; last recorded 30 recipes / 28,201 bytes |
| `readback.dat`, `accepted` | Supported by the new store, but absent for the historical failed trial |

Five protected fingerprints cover original, target, pending, independent
hardware original and library. They were rechecked unchanged while preparing
this document. Later `inspection.dat` is intentionally separate mutable evidence
and is not one of those five fixed baseline files.

The existing baseline was also copied, without regenerating it, into ignored
`captures/bluetooth/c7-app/recovery-inspection/protected-files-baseline.json`.
Use that durable local copy if `/private/tmp` is cleared. Neither private
captures nor this baseline exist in a clean Git clone. Do not mistake missing
private evidence on another machine for permission to create a new checkpoint.

Do not uninstall the app, run `pm clear`, replace its data directory, upload a
synthetic fixture, remove a marker/atomic sidecar, re-pair, or copy stale full
settings into the camera to unblock development. `adb install -r` with the
existing debug signing identity preserves app data; stop on a signature mismatch
instead of uninstalling.

## 7. Implementation map and write flow

Paths in this section are relative to the workspace root.

| File | Responsibility |
|---|---|
| `app/src/main/java/dev/bondarenko/fujirecipes/camera/bluetooth/BluetoothBackup.kt` | Pure decoder/selected-slot patcher, layout/value checks, exact comparison and recipe-only diagnostic classification |
| `app/src/main/java/dev/bondarenko/fujirecipes/camera/bluetooth/BluetoothCamera.kt` | Serialized GATT handshake/download/upload, transfer validation and bounded waits |
| `app/src/main/java/dev/bondarenko/fujirecipes/camera/bluetooth/BluetoothRecoveryStore.kt` | Atomic private evidence, marker/sidecar handling, strict resolution, export allowlist |
| `app/src/main/java/dev/bondarenko/fujirecipes/camera/CameraController.kt` | Shared connection lock/state, transport selection, pairing, slot reads, guarded write, inspection/export |
| `app/src/main/java/dev/bondarenko/fujirecipes/ui/camera/CameraSheetHost.kt` | Permission/companion chooser, USB/Bluetooth connected icon group, recovery actions and native document picker |
| `app/src/main/java/dev/bondarenko/fujirecipes/ui/camera/BluetoothRecoveryCard.kt` | Saved inspection/acknowledgement state and recovery actions |
| `app/src/main/java/dev/bondarenko/fujirecipes/ui/camera/WriteViewModel.kt`, `WriteSheetState.kt`, `WriteSheet.kt` | Selected-slot write flow, preflight and recovery-required result without blind Retry |
| `app/src/androidTest/java/dev/bondarenko/fujirecipes/camera/CameraBluetoothHardwareTest.kt` | Opt-in live read/write checks and independent private `BleBackupProbe` diagnostic transport |
| `app/src/androidTest/java/dev/bondarenko/fujirecipes/camera/BluetoothRecoveryTest.kt` | Synthetic persistence/export checks, genuine-checkpoint guard/export test and opt-in production inspection |
| `app/src/test/java/dev/bondarenko/fujirecipes/camera/bluetooth/BluetoothBackupTest.kt` | Pure parser/encoder/comparison regression tests |
| `app/src/test/resources/bluetooth/` | Synthetic backup/golden output, 566 measured C7 cases and six measured C1–C6 Color cases; never upload fixtures |

The production write flow is: refuse any unresolved checkpoint → require a
supported connected camera and slot → download/validate a fresh backup → patch
only requested supported fields plus measured relative metadata → persist and
verify original/target → check pre-upload cancellation → persist pending before
settings upload → finish under wake lock/non-cancellable work → record complete
upload acknowledgement → close/reconnect → save the received read-back before
decoding → require an exact complete target match → resolve the marker and
report success. Failures retain evidence and suppress a blind upload retry.

`BluetoothBackup.Match.COMPLETE` means complete equality. `RECIPES_ONLY` means
all seven complete 1,024-byte records match while other bytes differ, including
undecoded recipe bytes. It is diagnostic only. `DIFFERENT_RECIPES` is not success.
Malformed backups fail validation. `BluetoothRecoveryStore.resolve()` accepts
only a fresh complete match to saved original or target; a pending `.bak` or
`.new` also keeps the guard active. Atomic writes are read back because a rename
failure can be logged by Android without throwing.

Recovery inspection downloads only; it may resolve the marker if a fresh backup
really matches a complete retained snapshot. Export works offline and allowlists
checkpoint files/atomic sidecars plus README. It does not export HCI or Android
bond storage. A recovery ZIP is evidence, not an automatic restoration file.

## 8. Format and protocol constraints worth remembering

The supported raw backup is exactly **70,524 bytes**, header
`FUJIFILMX-BACKUP`, model text `X-T50`. Seven 1,024-byte recipe records begin at
**63356**. GATT additionally gates model `FF240001` and firmware `01.32`.
Do not infer other firmware compatibility from the same total file size.

Downloads validate sequential chunks, compressed-byte sum16, checksum trailer,
declared file size, zlib EOF and complete input consumption. Transfer checksum
validation is separate from the meaning of fields inside the raw settings file.

Metadata at **264** has a measured relative update equal to the recipe-byte sum
delta across the validated edits. This is not a decoded general checksum for all
camera settings. Do not use it to rewrite unrelated unknown/global values.

Recipe encoding includes mode-specific WB banks, nonlinear Color codes and
remembered monochrome family choices. Reuse the encoder and field definitions;
do not transplant USB write codes into the backup. Names are 1–25 printable
ASCII characters. Unspecified values and unknown bytes come from the **fresh**
camera backup. Unsupported requested fields/values fail preflight. Remembered
Kelvin is ignored unless the requested WB is Kelvin. Broader WB modes,
D-Range Priority and monochromatic Color are not mapped.

The reconnect identity uses the platform phone Bluetooth name with spaces
replaced by hyphens, followed by measured suffix `-2065` and NUL. The suffix's
meaning/universality remains unknown. Do not rename the phone or invent a
different identity during an experiment. API-37 encryption status is explicitly
waited for; older Android paths are implemented but not hardware-proven.

Observed operation transitions need a three-second pause. Prefer a validated
readiness signal if one is identified; do not enlarge sleeps or add retry loops
without a controlled reproduction. GATT requests must stay sequential.

## 9. Unresolved findings and negative results

| Investigation | Established result | What it does not establish |
|---|---|---|
| Read-only backup drift | 14 independently reconstructed backups preserve all recipe bytes while 63170–63173 drift; drift needs no upload | The four positions are not proven to be one logical 32-bit field or harmless data |
| CL vs Single Frame | Read-only backups and shipping reads passed in both; original CL restored | CL alone is not a deterministic explanation for prior refusals |
| USB property comparison | 17 known properties queried first; then 61 advertised properties, 58 answering; no raw-value match for unknown bytes | Property types/other encodings or properties unavailable in RAW mode remain unestablished |
| USB orientation control | 90° rotation changed 264 and 1811; reversing rotation restored the whole file; unknown four bytes unchanged | It does not identify the four bytes as orientation or exclude all imaging-state dependencies |
| Device/lens metadata | Eight reads bracketed by valid backups; no raw/trimmed/printable/UTF-16LE match near the unknown field | Not proof of another field type or encoding |
| Phone-name comparison | No direct match | Registration data was an unsuccessful hypothesis, not a finding |
| Integrity candidates | Selected CRC32/sum32/region candidates did not produce a universal explanation | No complete checksum algorithm has been recovered |
| Nearby time-like value | A seconds-since-2000 interpretation at 63164 looked plausible offline | Not independently confirmed camera RTC semantics; not an explanation for 63170–63173 |
| Installed Fujifilm SDK | Static inspection exposed opaque backup transfer functions | No backup parser or field semantics were identified |

The latest metadata probe read manufacturer, model, serial, hardware, firmware,
software, lens name and lens firmware. The only complete printable match was the
manufacturer at offset 0. Before/after backups differed only at **63268**; the
unknown four positions did not change in that run. No raw identities are logged
or reproduced here.

Two earlier attempts in that stage failed before encryption with status 147.
After a user restart, the first backup and metadata reads completed, but the
second download timed out because the research probe subscribed only at
connection and unsubscribed after its first download. Commit `7550ed3` moves
subscription enablement into each download. The production reader already did
this. The corrected two-backup hardware test passed in **9.831 seconds**. Do not
attribute that diagnostic bug to the camera or repeat the broken older probe.

## 10. Connection reliability is a separate unresolved issue

Distinguish pre-encryption status 147, connection-readiness timeout, explicit
backup-preparation state 0, file-transfer timeout and post-upload verification
mismatch. They occur at different stages and need different evidence. A link
connection or encrypted handshake does not prove backup availability.

A confirmed automatic sleep produced status 147; user shutter wake then allowed
three connections/six reads without radio reset. Cold restart and idle-gap
regressions have also passed, but intermittent failures remain. A single manual
phone Bluetooth OFF/ON cycle recovered an earlier stale state; the app does not
toggle Bluetooth automatically. Status 147 alone is not proof of a bad bond,
sleep, battery loss or firmware defect. A new GATT client is not proof of a new
physical radio connection; HCI is required for that claim.

Do not run concurrent phone clients or loop indefinitely. Prepare a bounded
read-only attempt, record its stage and actual camera state, then decide the
next controlled comparison. Keep XApp closed. XApp's current registration is
stale after native re-pairing: an attempted backup-only control demanded deleting
registration/re-pairing and was stopped. It is not a readily available control
without a new user-coordinated pairing change.

## 11. Evidence inventory and portability

All paths below are local and **Git-ignored** unless explicitly described as code.
Keep raw backups, serials, paired identities and HCI/bonding material out of Git,
logs, public issue reports and sanitized test fixtures. Prefer owner-readable
permissions. A different agent on this Mac can use these files; an agent in a
fresh clone/cloud environment cannot assume they exist.

| Location | Contents / use |
|---|---|
| `captures/bluetooth/c7-app/pixel-native-bond-c1-*.dat` | Genuine original, intended target and rejected read-back |
| `captures/bluetooth/c7-app/usb-native-bond-c1-recovery/` | Single-property recovery snapshots, 168-property output, read-only drive comparisons, `verify.py` |
| `captures/bluetooth/c7-app/recovery-inspection/pixel-live-inspection.dat` | Successful production inspection; original recipes exact, 26 historical nonrecipe differences |
| `captures/bluetooth/c7-app/recovery-inspection/protected-files-baseline.json` | Existing five-file fingerprint baseline retained durably during this handover |
| `captures/bluetooth/c7-app/device-info-probe/` | Latest before/after backups and eight metadata payloads; owner-readable |
| `captures/bluetooth/c7-app/usb-unknown-field-facts/`, `usb-unknown-field-advertised/` | Known/advertised USB property comparison evidence |
| `captures/bluetooth/xt50-132-pixel/` | Original decoder/extractor/USB helper, checks and calibration evidence |
| `/private/tmp/fuji-recovery-protection.py`, `fuji-recovery-before.json` | Convenience fingerprint checker/baseline; temporary, not portable |
| `/private/tmp/fuji-device-info-corrected.log` | Latest passing metadata instrumentation output |
| `/private/tmp/fuji-fffw-research/` | Read-only research clone, HEAD `ccee949884a6dad10095231441e4d608c10e59f0` |
| `/private/tmp/fuji-libfuji-bluetooth.c` | Pinned protocol source used for comparison |
| `/private/tmp/fuji-xgfxapi-arm64-disassembly.txt` | Static inspection output from installed Fujifilm software |

The latest metadata files also live on Pixel under
`cache/camera-ble-info-probe/`; the probe clears **only that diagnostic directory**
before a new run. Completed individual reads are saved immediately. An
incomplete directory after a failed run is partial evidence, not a passing pair
of backups. The older single-backup diagnostic saves
`cache/camera-ble-backup.dat`; retrieve it only after a passing run, never treat
old cache as a current read.

The most useful external starting points are pinned
[libfuji Bluetooth source](https://github.com/petabyt/libfuji/blob/3328efc51f74d754f8fa76af70c6becdde62271f/lib/bluetooth.c),
[fffw](https://github.com/tiredboffin/fffw), and the existing references in the
chronological plan. The inspected fffw README describes its backup parser as
unpublished; do not assume the clone contains a usable parser. Do not execute
service/jig commands or read/export pairing-key files from research projects.
The installed software inspected was
`/Applications/FUJIFILM X RAW STUDIO.app/Contents/Frameworks/XRFC.dylib` and its
`Contents/Resources/XGFXAPI.bundle/Contents/MacOS/XGFXAPI` sibling resources.
Do not run a vendor restore to investigate an opaque file format.

## 12. Safe restart procedure for another agent

1. Inspect branch, HEAD, working tree and instructions. Read this checkpoint
   before the historical plan. Preserve unrelated work. If code has moved beyond
   `7550ed3`, inspect the intervening diff before relying on this snapshot.
2. Confirm Pixel availability with explicit ADB serial. Do not connect Samsung.
   Do not assume camera battery, display, USB mode or Wi-Fi state from old notes.
3. Verify the five protected fingerprints against the existing local baseline.
   If the baseline/private evidence is missing or different, stop mutation work
   and inspect/export available evidence; do not overwrite the baseline to make
   the check pass.
4. Re-run the offline recovery evidence check and review the latest metadata
   pair. Start the next format investigation offline. Define one hypothesis,
   its expected discriminating result and a read-only method before asking the
   user to manipulate hardware.
5. If a fresh read is needed, build/install first, close XApp and ensure Bluetooth
   permission. Ask for one shutter wake only when ready to launch. Camera USB
   must be unplugged; Pixel stays connected by USB. Run one named opt-in test,
   not the entire camera hardware class.
6. Afterward, recheck protected fingerprints, close capture/forwarding/processes,
   and record exactly what was downloaded, what failed and whether settings data
   was uploaded. No matching cache file is evidence of a fresh read by itself.

## 13. Commands and test selection

Run commands from `/Users/andrii/Developer/Personal/fuji-recipes-app`. Local tools
are `/opt/homebrew/bin/adb`, system `python3`, and the Android Studio JBR below.
ADB/Gradle/Git writes may need normal sandbox escalation in Codex; that is not a
reason to change signing or device state.

```sh
git status --short
git log -6 --oneline
/opt/homebrew/bin/adb devices -l
export JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home'
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
```

The previous full application stage passed **740 JVM tests** and **six Pixel
UI/storage/service checks**. The latest diagnostic stage rebuilt its test APK
and passed its one real metadata probe. Those are distinct observations; no new
full-suite run or remote CI run was performed just to write this document.

A portable five-file preservation check, using the retained existing baseline:

```sh
python3 - <<'PY'
import hashlib, json, subprocess
from pathlib import Path
baseline = Path('captures/bluetooth/c7-app/recovery-inspection/protected-files-baseline.json')
expected = json.loads(baseline.read_text())
assert len(expected) == 5
for path, fingerprint in expected.items():
    raw = subprocess.check_output([
        '/opt/homebrew/bin/adb', '-s', '58061FDCQ0057S', 'exec-out',
        'run-as', 'dev.bondarenko.fujirecipes', 'cat', path,
    ])
    actual = {'size': len(raw), 'sha256': hashlib.sha256(raw).hexdigest()}
    assert actual == fingerprint, f'Protected file changed: {path}'
print('All five protected files match the existing baseline')
PY

python3 captures/bluetooth/c7-app/usb-native-bond-c1-recovery/verify.py
```

The offline `verify.py` was rerun while preparing this document and passed:
168 properties, 7,168 recipe bytes, unchanged library and 14 reconstructed
read-only backups. Other local offline checks include `decode_slots.py --check`,
`check_extraction.py`, `read_usb_slots.py --check`, `check_fact_reader.py` and
`check_orientation.py` under `captures/bluetooth/xt50-132-pixel/`. Read each
helper's entry point before running it; similarly named hardware/calibration
modes can write properties. USB library/runtime prerequisites may differ on a
different Mac.

Install updates without clearing app data, only if necessary:

```sh
/opt/homebrew/bin/adb -s 58061FDCQ0057S install -r app/build/outputs/apk/debug/app-debug.apk
/opt/homebrew/bin/adb -s 58061FDCQ0057S install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
```

The ordinary app on Pixel includes the recovery implementation. The most recent
installation updated only the diagnostic test APK; it introduced no production
feature change. Rebuild from current sources rather than inferring installed
commit identity from versionName, which does not identify a Git revision.

The following is a **read-only production recovery inspection**. It can resolve
the marker only under the already-implemented exact-match policy and will save
a new `inspection.dat`:

```sh
/opt/homebrew/bin/adb -s 58061FDCQ0057S shell am instrument -w -r \
  -e recoveryInspection true \
  -e class dev.bondarenko.fujirecipes.camera.BluetoothRecoveryTest#inspectExistingCheckpointWithoutUploading \
  dev.bondarenko.fujirecipes.test/androidx.test.runner.AndroidJUnitRunner
```

The latest two-backup metadata probe is also **read-only**, requires API 37 and
the genuine retained original for its preservation comparison:

```sh
/opt/homebrew/bin/adb -s 58061FDCQ0057S shell am instrument -w -r \
  -e bluetoothDeviceInfo true \
  -e class dev.bondarenko.fujirecipes.camera.CameraBluetoothHardwareTest#compareBackupWithReadOnlyDeviceInformation \
  dev.bondarenko.fujirecipes.test/androidx.test.runner.AndroidJUnitRunner
```

| Check / opt-in | Suitable now? |
|---|---|
| `readRecipesWithAppTransport`, `bluetoothAppRead=true` | Read-only; three clients/six decoded reads, default 3-second gaps. Does not prove full-backup equality |
| `downloadSettingsBackup`, `bluetoothProbe=true` only | Read-only backup; **do not add any restore opt-in** |
| `inspectExistingCheckpointWithoutUploading`, `recoveryInspection=true` | Yes, read-only camera inspection with strict resolution |
| `existingCheckpointBlocksUploadsAndExportsWithoutChangingProtectedFiles`, `recoveryCheckpoint=true` | Existing-checkpoint guard/export regression; calls the guarded entry point to prove refusal. Read its body first; no upload is expected |
| `uncertainWriteSurvivesRestartInspectionAndExportUntilAnExactMatch` | Synthetic cache-only storage/export regression; no camera |
| `CameraTransportTest` | UI/service state checks; four named tests, no live write required |
| `writeSlotWithAppTransportAndRestore`, `bluetoothAppWrite=true` | **Do not run while genuine pending exists**; settings upload/restoration test |
| `bluetoothRoundTrip`, `bluetoothColorProof`, `bluetoothRecipeProof` | **Do not run now**; restore opt-ins, not harmless diagnostics |
| `unresolvedWriteRefusesChangedCameraBeforeUpload`, `bluetoothPendingGuard=true` | **Do not run on this checkpoint**; requires no existing pending and creates its own diagnostic marker |
| USB `--calibrate-*` / `--restore-c1-color` | Write modes; not general diagnostic or automatic recovery commands |

For captures, use the existing local HCI socket workflow if needed; inspect the
helper first and prove the socket is receiving current packets. Do not invent a
second capture system. Pairing captures can contain secrets. Stop recording and
remove only the forwarding established for that run when finished.

## 14. What must happen next, in order

1. **Preserve and explain the blocker.** Recheck the existing checkpoint and
   original recipes as above. Keep the product guard. The next agent's first
   deliverable is a narrow research plan with a falsifiable hypothesis, not a
   looser verification comparator or another broad UI rewrite.
2. **Investigate the raw format offline.** Use the existing upload, read-only
   pairs, independent USB backups and sanitized encoder vectors to distinguish
   known recipe metadata from other changes. Inspect field boundaries and
   integrity coverage rather than assuming 63170–63173 is one integer. There
   may be other changing fields, as shown by the 26 historical differences and
   latest offset 63268. Confirm any proposed semantics against independent
   camera/protocol information, then test against every saved counterexample.
3. **Use a bounded read-only experiment only when it discriminates a concrete
   hypothesis.** Direct raw metadata matching has already failed; USB rotation
   and broad property probing have already been measured. Do not repeat them
   without a specific new reason. If the required evidence is unavailable,
   report that limit and keep writes blocked rather than silently reducing
   safety. Re-pairing or XApp controls need explicit user coordination.
4. **Define any verification-policy change before implementing it.** Prefer
   validated semantics/integrity or an independently established way to verify
   every affected setting. Recipe equality alone does not cover a whole-settings
   upload. A byte mask, a checksum guess or several successful reads is
   insufficient. Document preserved guarantees, known exclusions and adversarial
   counterexamples; add the smallest regression that rejects unexplained drift
   and unrelated changes. If the evidence does not support a safe change, leave
   the current policy intact and clearly state why.
5. **Resolve the genuine checkpoint through an auditable path.** Current code
   permits only a fresh complete match to original or target. Never make one
   artificially by uploading a stale backup. If a different resolution mechanism
   becomes necessary, design and justify it as a separate change with explicit
   evidence; do not implement it as manual marker deletion or baseline overwrite.
6. **Only after resolution, resume controlled native-bond write proofs.** Save
   a fresh independent reference; change one reversible field in one slot;
   verify intended recipe and all protected nonrecipe settings independently;
   reconnect; restore only after a verified forward state; confirm original
   restoration. Stop immediately on uncertainty. Finish the ordinary app upload
   UI and cold-start checks under the current native bond, then assess other
   slots without conflating older-bond results.
7. **Complete robustness and compatibility afterward.** Controlled interruption,
   cancellation boundaries, menu/sleep/battery loss, process restart, recovery
   export, idle/cold reconnect and USB-to-BLE handoff remain relevant. Samsung,
   older Android and other firmware are separate future scopes. Keep release
   claims restricted to actual tested devices. Inspect/push/check CI only when
   requested/authorized, and report actual remote outcomes.

The gate for claiming this blocker solved is evidence that the camera's intended
settings and unrelated settings are preserved under a justified verification
rule, plus safe resolution of the real checkpoint and a passing controlled
native-bond write/restoration. A green parser test, matching C1 Color or an
acknowledged upload alone does not satisfy that gate.

## 15. UI work already settled

The Camera connection UI uses the shared modal side sheet in
`ui/common/SideSheet.kt`, with scrim/dismissal behavior and bounded width for
foldables. The USB/Bluetooth mode control is a connected, icon-only button group
in the sheet's content area; the user rejected crowding it beside the header
close action. Camera artwork uses a circle, and disconnect uses the requested
eject Material Symbol. Preserve these choices during Bluetooth changes.

Bottom sheets were audited and committed using Material modal bottom-sheet
behavior. A modal sheet is identified by its modal interaction/scrim/dismissal
behavior; a custom horizontal inset is not the rule that makes it modal. Do not
reopen the resolved Filters inset debate or replace the camera side sheet while
investigating the protocol. Existing modal sheet tests cover that work.

## 16. Working style and mistakes to avoid

Give short, evidence-backed updates. Do necessary work autonomously, but ask for
specific physical hardware actions only when ready to use them. Keep experiments
single-variable and record unsuccessful outcomes as such. A failed transfer
must not be called a successful write because the displayed recipe looks right.
State whether a result is code-only, a synthetic test, a decoded app read, an
independent full backup, HCI reconstruction or a user-confirmed camera display.

Avoid repeating speculative lens/scene/rotation exercises; the user explicitly
questioned their relationship to recipes. The reason for format investigation
is protecting unrelated camera settings during whole-backup upload, not adding
camera telemetry features. Do not introduce infrastructure, dependencies or
unrelated UI changes to create the appearance of progress.

Maintain a clean separation between historical observations, last-known device
state and newly verified state. Do not claim fresh remote CI, camera availability,
full native-bond write success, all-firmware support or Samsung write support
from this document. It deliberately records the remaining uncertainty.

## 17. Suggested opening instruction for the next agent

> Continue Fujifilm Bluetooth work in
> `/Users/andrii/Developer/Personal/fuji-recipes-app` on `bluetooth-connection`.
> Read `AGENTS.md`, `specs/steering/coding-standards.md`, and
> `specs/plans/bluetooth-handover-2026-10-27.md` before editing. Code checkpoint is
> `7550ed3`. Pixel only. Original C1–C7 recipes are restored; a genuine pending
> whole-backup verification checkpoint remains and must not be deleted,
> overwritten or bypassed. Begin by verifying the protected fingerprints and
> existing offline recovery evidence. Investigate unexplained nonrecipe drift
> using saved captures before proposing one bounded read-only experiment.
> Do not upload settings, re-pair, change camera configuration or relax complete
> verification to make tests pass. Record evidence and uncertainty, and preserve
> private captures outside Git.
