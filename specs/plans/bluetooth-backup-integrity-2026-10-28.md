# Bluetooth backup integrity investigation

Started **28 October 2026**, Europe/Belgrade, from `43621b1` on
`bluetooth-connection`. Continues
[the handover](bluetooth-handover-2026-10-27.md), not the earlier write proofs.

## Scope and preservation

Explain unexplained nonrecipe backup differences before changing verification.
At the start of this research, the comparator, genuine pending marker and saved
original/target were unchanged. Later deliveries below recover the firmware
checksum and introduce a documented policy for new checkpoints. The genuine
legacy checkpoint remains unchanged. No settings upload, re-pairing or camera
configuration change was performed; the final validation included one explicitly
coordinated read-only camera inspection.

The connected Pixel's five protected files matched the existing private baseline
at the start of this stage. The private recovery check passed: 168 properties,
7,168 original recipe bytes, unchanged library and 14 read-only HCI reconstructions.
These are historical capture checks, not a new read of camera settings.

## Hypotheses and discriminating checks

**H1: offsets 63170–63173 store an integrity value determined entirely by the
other stored backup bytes.** Compare independently captured read-only backups
that differ only at those positions. Two different values with identical
remaining bytes reject that hypothesis regardless of the candidate checksum
algorithm. This does not exclude an integrity value with an external input,
multiple independent fields or an undocumented volatile state.

**H2: the measured additive metadata word at 264 has identifiable section
coverage that explains why these bytes can change without updating it.** Inspect
stored length/boundary words first, then compare byte sums at those boundaries
against every locally retained valid X-T50 backup, including USB calibration,
rotation controls, historical BLE captures and the rejected upload. A candidate
must pass held-out captures; a fitted interval alone does not establish field
semantics or authorize excluding bytes from verification.

Keep original captures private and read-only. Retain a small runnable offline
check for any reproducible finding, with malformed-input and counterexample
checks if new parsing logic is introduced. Use existing helpers and standard
library code; no new dependencies or transport abstractions.

## Decision gate

Record supporting samples, counterexamples and the exact limits of each result.
Only propose a live read when it distinguishes a named remaining hypothesis and
cannot be answered from saved evidence. Explain its purpose before requesting
the smallest necessary user action; prepare the test before asking for a wake.

Checksum coverage alone does not prove unrelated settings survived upload.
Changing verification or resolving the genuine checkpoint requires the separate
evidence and guarantees specified in the handover. If that evidence is absent,
leave uploads blocked and record the next discriminating research step.

## Results

**H1 rejected.** The offline checker found four groups with identical bytes
outside 63170–63173 but different values inside that range. A concrete independent
pair is `usb-native-bond-c1-recovery/ble-single-frame-wire-1.dat` and
`ble-single-frame-wire-8.dat`: all other 70,520 bytes match exactly. Therefore the
four-byte range cannot, as a whole, be a deterministic checksum of the remaining
stored contents. Its semantics are still unknown; this result does not establish
that it is harmless or camera-managed on upload.

**H2 remains unresolved; both tested coverage candidates fail.** A newly located
independent [X-T5 format investigation](https://github.com/KyleOndy/dotfiles/blob/285e5704f5c80481bc578855de2b49736f00bf46/nix/pkgs/helios/FUJI_BLOB_FORMAT.md#the-whole-file-checksum-solved-confirmed-on-hardware)
and [parser](https://github.com/KyleOndy/dotfiles/blob/285e5704f5c80481bc578855de2b49736f00bf46/nix/pkgs/helios/fuji_backup.py)
describe an additive sum with a fixed bias, excluding the checksum word and an
11-byte state region. The author explicitly limits the fitted rule to the samples
on hand. Those offsets and claimed lens/state semantics are not an X-T50 mapping.

All 711 local X-T50 backup files (593 distinct byte strings, including edited
targets and duplicate/reconstructed captures, **not 711 independent camera
experiments**) declare payload start 168 at offset 152 and length 70,356 at
offset 160. Testing an adaptation that skips 264–267 and 63164–63174 requires
**ten different additive biases**, so no single bias explains this corpus.
Even excluding the entire 192-byte interval 63164–63355 requires two biases.
These fitted exclusions are rejected candidates, not proposed verification masks.

The latest `device-info-probe/before.dat` and `after.dat` give a short direct
counterexample to the 11-byte exclusion: only offset **63268** changes, while the
stored word at 264, proposed excluded region and every recipe byte are identical.
Thus ignoring only the previously identified state region still cannot explain
the stored sum. This is a recheck of the handover's saved metadata pair, not a new
camera observation. Static inspection of the installed X RAW Studio SDK exposed
backup transfer entry points, but did not independently establish a checksum or
field parser.

## Reproduction and next gate

The committed checker uses the Python standard library and reads files only:

```sh
python3 scripts/check-bluetooth-backup-integrity.py --check
python3 scripts/check-bluetooth-backup-integrity.py captures/bluetooth
```

The self-check covers duplicate captures, a true four-byte-only counterexample,
an unrelated change that must not be grouped with it, failure of both checksum
candidates, missing captures and malformed size/header/payload metadata. The
capture report prints counts only, never raw contents or identities. Its header
checks are not a replacement for the app's recipe/transport validation. The
existing private USB recovery and reverse-orientation checks also pass.

The next useful evidence is an independently supported **X-T50 section/field
layout**, especially around 63164–63355, or a model-specific checksum algorithm
that survives the saved counterexamples. Do not import the X-T5 rule, its fitted
bias or its lens/state label into production. Another unchanged-camera download
would not distinguish the rejected hypotheses, so no fresh camera experiment is
requested at this stage. Production exact comparison, recovery resolution and
the genuine pending checkpoint remain unchanged; no camera settings were
uploaded or changed during this investigation.

Final validation: the Python self-check and full private-corpus analysis passed,
as did the existing USB recovery and reverse-orientation evidence checks. All
**four `BluetoothBackupTest` JVM tests** passed with zero failures/errors/skips
using `:app:testDebugUnitTest --tests
dev.bondarenko.fujirecipes.camera.bluetooth.BluetoothBackupTest`. This was a
targeted run, not a new full application suite. All five protected Pixel files
matched the retained baseline again after the investigation. No app/test APK was
installed and no live camera read was run. Validation was performed before the
checkpoint commit; no remote delivery or CI run was part of this stage.

## Next stage: vendor validator

Continues from signed checkpoint `c18dbe9`. **H3: the vendor backup reader
validates structured sections, and its actual checksum coverage differs from the
fitted X-T5 whole-file rule.** Inspect the official X Acquire archive statically
to find the backup reader and its validation boundaries, then test any recovered
rule against all saved X-T50 counterexamples. An absence of a parser or an
unsupported model-specific layout is a negative result, not permission to invent
a verification mask.

The [official download page](https://www.fujifilm-x.com/global/support/download/software/x-acquire/)
provides Mac 1.27.0. That version predates X-T50; finding generic validation code
would not establish model compatibility or identify the unknown fields. Download
and extraction are for static research only: do not install/launch the vendor
app, connect it to the camera or perform a vendor restore.

### Vendor trace result

**H3 was not established.** Static inspection of Mac 1.27.0 and Windows 1.29.0
did not recover a section checksum validator from their backup paths. The Mac
application's pre-upload check at ARM64 address `0x100026d80` checks ASCII version
`0100` and applies a legacy-model compatibility filter. It does not calculate the
raw settings checksum. Its SDK's `ExecSetBackupSettingsInfo` constructs the
1,076-byte PTP ObjectInfo, and `ExecSetBackupSettings` passes the supplied buffer
to `SendObject`. Neither inspected method parses the settings fields.

The Windows SDK confirms the same transport boundary. With image base
`0x10000000`, `XSDK_SetBackupSettings` is at `0x10026160`; the backup-command
vtable is at `0x10043218`. `CheckParameter` at `0x10001070` checks required
pointers. The command dispatcher at `0x10001600` calls the ObjectInfo builder
at `0x10001120` and settings sender at `0x10001290`, which pass the supplied
length and payload to PTP operations `0x100c` and `0x100d`. No checksum or
unknown-field semantics were recovered from these methods. This is not a claim
about every path in either application, or about validation inside the camera.
The external X-T5 rule therefore still lacks independent SDK confirmation here.

Official archives were downloaded and extracted, never installed or launched.
The Mac disk image was mounted read-only and detached after copying the relevant
binaries. Windows extraction used a temporary build of
[Unshield 1.6.2](https://github.com/twogood/unshield/releases/tag/1.6.2), the
project's existing Android CMake and system zlib; no package or app dependency
was installed. Local manifests and disassemblies are under
`/private/tmp/fuji-xacquire-static-20261028/` and may disappear when temporary
files are cleared. The SDK fingerprints needed to identify the inspected code
are public software fingerprints, not camera identities:

| SDK | Bytes | SHA-256 |
|---|---:|---|
| Mac XGFXAPI, universal binary | 1,549,024 | `351299d2d7e7374b0c23d139416b705bf50418483d8423892155b19bbfe73b77` |
| Windows XGFXAPI.dll | 385,392 | `82ee72855de1a26f6b678e8492ff1b360180faac43a7b689c4c0a32110574af8` |

### Shared decoder guard

The narrow implementation result is a stricter **supported-layout gate**, not a
new checksum algorithm. All 711 retained files use version `0100`, payload start
168 and payload length 70,356. `BluetoothBackup.decode()` previously ignored
these header fields, so even an unsupported header could be decoded, patched or
compared as a supported backup if its total size, model and recipe fields matched.
The shared decoder now requires all three measured header values. Its callers
already route reads, patching, verification and recovery through this decoder;
no caller-specific guards or new transport code are needed.

The new JVM regression first failed against the old decoder at a changed version
byte. It exercises each of the twelve version/window bytes and requires refusal
by decoding, patching and comparison, even when two malformed backups are equal.
The synthetic JVM resource and cache-only Android recovery fixture now include
the measured header declarations. The offline checker uses the same layout gate
and still finds the original four counterexample groups and ten/two candidate
biases across all 711 files. This guard does not explain offsets 63170–63173 or
63268, exclude any mismatch, or permit resolving the genuine checkpoint from
recipe equality. Received evidence is still saved before decoding.

Stage validation passed **741 JVM tests**, with zero failures/errors/skips,
plus both APK builds and the updated Python self-check/corpus analysis. The app
and test APKs were installed on Pixel with `adb install -r`, preserving app data.
The cache-only synthetic recovery persistence/export test and the genuine
checkpoint guard/export test both passed. The genuine test also explicitly
requires `damaged == false`, proving that the stricter parser still reads the
retained evidence; it was rerun successfully after adding that assertion.

All five protected Pixel fingerprints matched the retained baseline before the
parser change and after the device checks. All three protected raw snapshots
also satisfy the new header gate. No live camera download, settings upload,
configuration change, re-pairing or radio reset occurred. The unknown fields
remain unidentified and the genuine pending marker remains active. Further
checksum/field research needs an independent serializer/format definition;
the inspected host transfer methods do not supply one. Remote CI was not run.

## Firmware serializer investigation

The full Bluetooth workflow remains incomplete: the genuine verification
checkpoint must be resolved safely before native-bond write/restoration and
remaining robustness proofs can finish. The header gate is a completed safeguard,
not completion of that workflow.

**H4: the matching X-T50 1.32 firmware contains an identifiable backup serializer
or restore validator that establishes field boundaries and integrity coverage.**
Download the matching official image for static analysis, inspect compressed
sections with the existing published decompressor, and locate backup-related
references before attempting to infer a layout. Cross-check any recovered rule
against every saved counterexample before proposing a policy change. No firmware
installation, camera service mode, hardware dump or settings upload is authorized
by this offline investigation; the genuine checkpoint remains protected.

### Matching firmware and reproducible extraction

The [official X-T50 firmware page](https://www.fujifilm-x.com/global/support/download/firmware/cameras/x-t50/)
provides 1.32, dated 15 January 2026. The matching
[public image](https://dl.fujifilm-x.com/support/firmware/x-t50-132-kne1es15/FWUP0033.DAT)
is 110,561,484 bytes, SHA-256
`2c360848736985531db407c8597911f1b701b4a854dc3b9b949ccc9f7b274212`.
It was downloaded for static inspection only; no firmware installer, service
mode, hardware dump or camera connection was used.

The [published container decoder](https://github.com/fujihack/patcher/blob/0e81431c32f2fc908d9626d255e1d758ebfe8a79/patcher.js)
and [byte inversion helper](https://github.com/fujihack/patcher/blob/0e81431c32f2fc908d9626d255e1d758ebfe8a79/util.js)
describe the type-8 header and inverted payload. On a copy, invert every byte
after the 532-byte header. Scan that payload for five little-endian u32 values:
unpacked length, packed length including the 20-byte header, 16-KiB page count,
page size 16,384 and alignment 4. Reject inconsistent sizes and file bounds.
Six compressed sections satisfy those declarations. Decode them with
[ffcompress at ccee949](https://github.com/tiredboffin/fffw/tree/ccee949884a6dad10095231441e4d608c10e59f0/ffcompress),
built with existing Go and no new dependencies. Its Go tests passed; the main
section also recompressed byte-for-byte with `ffcompress t`.

Two relevant decoded sections, with offsets relative to the inverted payload:

| Section | Packed offset | Packed bytes | Decoded bytes | Decoded SHA-256 |
|---|---:|---:|---:|---|
| Settings | `0x9c00a8` | 7,571,664 | 27,751,872 | `5a32fc0cbf753ca0a246d4d195823541bef72872ec57bd46b295a19839feeb4b` |
| Main/Bluetooth | `0x11000a8` | 9,667,497 | 22,417,408 | `f4484b281581cddd1255184662ce5efdf62fc112f2a8c3da2aa11c56c68056f0` |

Temporary manifests, decoded sections and ELF disassembly views are under
`/private/tmp/fuji-xt50-132-static-20261028/`. The original image hash remained
unchanged. Addresses below are **decoded section offsets**, not claimed runtime
addresses. Local AArch64 ADRP/ADD references locate `X-BACKUP`, `backup.dat` and
`PCSH_BACKUP_TXBUF_OVER` in the main section. Cross-section direct calls have
the consistent main-to-settings displacement `0x1a6c000`, checked against both
the memcpy implementation and manufacturer-string getter.

### Established checksum, not a fitted coverage rule

**H4 is established for the checksum.** The settings validator at `0x20e0c8`
accepts payload lengths `0xf6d4` or `0x112d4`, reads and clears the u32 at payload
offset `0x60`, sums unsigned bytes over `[0, 0xe968)` and
`[0xf6d4, 0x112d4)`, then compares the saved u32 with the full 32-bit sum. This
trace establishes the supported full-size backup; it does not establish a safe
short-backup parser. There
is no bias or sum16 truncation. For the supported 70,524-byte backup, payload
starts at 168 and the exact rule is:

```python
stored = int.from_bytes(raw[264:268], "little")
expected = sum(raw[168:264]) + sum(raw[268:59920]) + sum(raw[63356:70524])
assert stored == expected
```

This independently recovered rule matches **711/711 retained files**, including
593 distinct files, with zero mismatches. Those files include synthetic targets
and repeated reconstructions, not 711 independent hardware trials. Both earlier
candidate exclusions still fail their holdouts. The raw checksum excludes the
3,436-byte interval `[59920, 63356)`, which contains 63164, 63170–63173 and 63268.
That explains why changes there need not change the checksum; it does not by
itself establish safe verification exclusions or names for those fields.

`scripts/check-bluetooth-backup-integrity.py --check` exercises both ends of
each covered range, every checksum byte and both ends of the excluded interval.
The normal private-corpus command reports matches/mismatches and exits with an
error if any checksum fails. It prints counts only.

The shared production decoder now rejects a bad settings checksum before
reading, patching or comparing a backup. Patching recomputes this recovered
checksum instead of relying only on a relative recipe delta. Independent USB
recipe-delta expectations remain the patcher's regression oracle. The new JVM
corruption regression failed against the previous decoder before the fix. The
synthetic JVM and Android fixtures now carry valid checksums; the Android
recovery check also requires corrupt received evidence to remain saved while
the pending marker stays active. Literal `Match.COMPLETE` and exact-only
recovery resolution remain unchanged in this delivery.

### Restore range trace still separate from policy

The main Bluetooth builder at `0x551a90` creates the 168-byte header and joins
two supplied payload chunks. The export path at `0x2bc58c` obtains the settings
prefix through settings wrapper `0x32b7c`, obtains seven records through
`0x32b90`, and supplies lengths `0xf6d4` and `0x1c00` respectively. The prefix
exporter `0x20df04` first copies a larger in-memory structure through `0xf9004`
and then runs export normalization. Thus bytes outside the checksum coverage
are present in the exported memory snapshot; they are not inferred to be zero
padding.

Inspected restore paths call settings `0x20df70` for the prefix and `0x204688`
for records starting at payload `+0xf6d4`; the combined settings restore at
`0x2042a4` uses the same split. The prefix restore copies only `0xe968` bytes
to its scratch buffer, while the mode-dependent live copy is smaller still.
The record restore copies seven 1,024-byte source records through its own
mode-dependent copy sizes. These findings support investigating a verifier
over all checksum-covered settings bytes, while retaining full raw evidence.
The Bluetooth operation-dispatch trace and changed contract are recorded in the
next delivery below. The genuine checkpoint must not be cleared merely
because its recipes or checksum match.

The checksum delivery passed **742 JVM tests** with zero failures/errors/skips,
both APK builds and the Python self-check/corpus check. Both APKs were updated
on Pixel with `adb install -r`; the synthetic recovery persistence/corrupt-evidence
check and genuine checkpoint guard/export check passed in 0.429 seconds. All
five protected fingerprints matched before and after, and all three retained
raw backups satisfy the checksum. No live camera download or settings upload
was performed. Remote CI was not run.

## Next delivery: firmware-bounded settings verification

The Bluetooth restore receiver at main `0x552038` checks the file header and
passes its declared payload pointer/length to operation `0x1c` through
`0x547afc`. The service dispatcher uses its signed-byte jump table at `0xadb1dc`;
entry `0x1c` lands at `0x547df8` and calls `0x547610`. That handler forwards the
pointer, length and completion callback in restore message `0x80e9`. Shared
restore validation (`0x2bc5dc`, calling settings `0x32bcc`) and apply
(`0x2c5c0c`) use the received pointer. Apply calls prefix wrapper `0x32ba4`,
refreshes live properties through `0x2c2f64`, then calls record wrapper `0x32bb8`
with source pointer advanced by `0xf6d4`. The other inspected apply path at
`0x2687b0` has the same split. These source ranges, together with checksum
coverage and independently reconstructed read-only drift, establish a
model/firmware-specific distinction between restored settings and the exported
memory tail. The individual drifting fields are still unnamed.

The proposed implementation contract is deliberately narrower than decoding all
settings. Validate both backups completely, including the recovered checksum.
Keep `COMPLETE` as literal equality. Add a distinct `SETTINGS` match requiring
byte-for-byte equality over `[0, 59920)` and `[63356, 70524)`: every header byte,
the complete checksum-covered settings prefix, and all seven complete recipe
records, including undecoded bytes. Only `[59920, 63356)` may differ. A matching
checksum alone or equality of decoded recipe values is never sufficient.
`RECIPES_ONLY` remains a failure when any compared nonrecipe byte differs.

Before a **new** upload can start, persist and read back an atomic
`verification-policy` file containing exactly ASCII `xt50-132-settings-v1`,
alongside original and target. That checkpoint may resolve from a fresh
`COMPLETE` or `SETTINGS` match to original or target. A missing policy denotes
the existing legacy exact-only contract; never create or migrate a policy for a
pending legacy checkpoint. Unknown, incomplete or unreadable policies fail
closed. Keep original raw snapshots and read-backs unchanged and include the
policy in exported recovery evidence. Cached status never resolves a marker.

Rename the write outcome flag to `bluetoothSettingsVerified` and describe the
confirmed settings in UI copy. A settings match must never claim literal whole
file equality. Hardware proofs must still independently verify every recipe
record and the same header/settings ranges after forward write and restoration.
No older Android/model compatibility gate is widened.

Before using hardware, test the range boundaries, covered-byte changes with
valid checksums, checksum collisions, undecoded bytes in every recipe, new and
legacy checkpoint behavior, invalid policy markers and evidence export. Re-run
the genuine checkpoint guard/export test and its protected fingerprints. The
historical rejected read-back should match target settings under the proposed
rule, while the later externally repaired inspection may still fail because
covered nonrecipe bytes changed. Neither historical result clears the genuine
lock. Resolving that old checkpoint remains a separate auditable task requiring
current camera evidence; no upload is authorized merely by this policy change.

### Implementation and validation

The documented contract is implemented. `SETTINGS` is a separate match class;
`COMPLETE` retains literal equality. New checkpoint preparation persists the
policy atomically before the pending marker/upload. Legacy checkpoints are not
migrated. A partial `.new` policy blocks even if the canonical policy is valid;
unknown canonical or `.bak` policies also fail closed. Raw evidence remains
unchanged, and the exported README describes both contracts. The renamed
`bluetoothSettingsVerified` outcome and UI wording make the verification scope
explicit. Independent hardware proof assertions now use that scope while still
checking every recipe byte; those write proofs have not yet been rerun.

The new comparison regression first failed against the prior comparator.
Validation passed **743 JVM tests** with zero failures/errors/skips and both APK
builds. Three Pixel recovery tests passed: genuine guard/export, synthetic legacy
exact-only recovery, and new-policy target/original settings recovery with
covered-byte and unknown/partial-policy refusals. Two UI tests initially lacked
Compose hierarchies while Pixel was locked and dozing; after the user unlocked
it, both passed in 2.309 seconds, including the new saved-settings wording.

The historical rejected native-bond read-back now classifies as a settings match
to its target, while still failing literal equality. The historical USB-repaired
inspection still fails settings comparison: 18 of its 26 changed nonrecipe bytes
are in the compared prefix. This is a preserved counterexample, not a reason to
expand the exclusion.

After the user confirmed Pixel unlocked and X-T50 awake/unplugged, one named
production read-only recovery inspection passed in **4.276 seconds**. Its fresh
backup passes the recovered checksum and matches **all 7,168 original recipe
bytes**. It differs from original at 18 compared-prefix bytes and nine excluded
tail bytes, so neither full-file nor settings equality holds against original
or target. The genuine legacy marker remained active, acquired no policy file,
and all five protected fingerprints matched before and after the inspection and
UI tests. XApp was stopped to avoid a competing Bluetooth client. No settings
upload, radio reset, pairing change or camera setting adjustment occurred.

The fresh private backup/report are retained under ignored
`captures/bluetooth/c7-app/recovery-inspection/pixel-2026-10-28-firmware-inspection.{dat,json}`.
Temporary build/device logs are `/private/tmp/fuji-settings-verification-build.log`,
`fuji-settings-verification-device.log`, `fuji-firmware-live-inspection.log` and
`fuji-settings-ui-unlocked.log`. No remote CI was run. Native-bond forward-write,
restoration and interruption/reconnect completion still require an auditable
resolution of the genuine old checkpoint first.

## Separate recovery proposal: accept externally repaired recipes

The fresh camera read proves recipe restoration but not restoration of every
other setting to the old snapshot. Further format exclusions cannot turn that
fact into full verification. A separate operator acceptance can retire this
historical checkpoint **only if the user explicitly chooses to keep the current
other camera settings**. It must not label the historical upload successful or
claim those 18 prefix differences are harmless/restored.

Prepare an opt-in instrumentation procedure, not an automatic application
migration. Require both the live read-only inspection opt-in and the explicit
`legacyExternalRecovery=accept-current-other-settings` operator flag. The live
procedure must first perform the production inspection, disconnect, and then
validate the fresh received file. Cached evidence alone cannot authorize it.
The operator must verify the five frozen fingerprints before invoking it.

Limit the transition to the known legacy C1 Color +3 → 0 trial: saved target
must equal the supported patch of saved original, no policy/atomic sidecar may
be present, pending must be canonical byte `01`, and fresh camera recipes plus
the independent hardware original must match all 7,168 original recipe bytes.
Validate each backup and checksum. Refuse malformed, different-recipe or
new-policy checkpoints before moving or writing anything.

Write and fsync an immutable fresh retirement read-back and JSON acceptance
record inside the existing checkpoint directory, including all retained file
fingerprints, the independent original/library fingerprints, changed-byte
counts, destination name and the explicit limit of acceptance. Move the
independent hardware original into that still-locked directory, then atomically
rename the entire directory beneath `no_backup/bluetooth-recovery-archive/`.
Fsync the source/destination directories and verify all retained bytes afterward.
The pending marker is retained inside the archive; no marker is deleted and no
frozen baseline is replaced. The phone library stays at its current path,
unchanged. New writes must obtain their own fresh backup under the new policy.

If interrupted before the directory rename, the active pending marker still
blocks writes. If interrupted afterward, the full checkpoint, original marker,
fresh read-back, independent original and acceptance record are together in the
archive. Refuse overwriting an existing retirement record or destination;
inspect an incomplete transition rather than retrying it blindly. Test refusals
and successful archival using only a UUID-named cache directory before asking
for approval. **Do not run the live retirement without explicit user approval.**

The opt-in procedure is implemented only in the Android test APK as
`BluetoothRecoveryTest#archiveExternallyRepairedLegacyCheckpoint`. It requires
both flags above and performs the existing production inspection before any
retirement action. The release application has no automatic retirement path.
`legacyRetirementPreservesEvidenceAndRefusesUnrepairedRecipes` exercises it
using a UUID-named cache tree, refusing a new-policy checkpoint, changed recipes,
corrupt checksum and an existing destination, then verifying all retained files,
the original pending marker, independent original, fresh read-back and unchanged
synthetic library after archival. Source evidence is also rechecked immediately
before the committing directory rename.

The updated test APK built and was installed with `adb install -r`. The final
cache-only archival test and genuine guard/export test both passed in **0.297
seconds**, and all five frozen genuine fingerprints matched before and after.
Directory fsync uses the public Android `Os.open(..., O_RDONLY, ...)` API with
an explicit directory check. An initial compile failed because `O_DIRECTORY`
is not exposed; a prematurely started previous-APK run executed only the old
guard test and was rejected by the expected-test-count check. Only the rebuilt
two-test runs count as archival validation.

Build/test logs are `/private/tmp/fuji-legacy-retirement-build.log` and
`fuji-legacy-retirement-cache-check.log`. The genuine live retirement has
**not been invoked**. Its next prerequisite is explicit acceptance of the
current other camera settings, followed by a fresh camera wake/read. Preserve
the frozen baseline file; after an approved transition, verify its original
fingerprints at the archived paths (including `hardware-original.dat`) and the
unchanged live `files/library.json`, rather than replacing that baseline.

### Approved live retirement

On 28 October, the user explicitly answered **“Approve; camera is awake”** to
keeping the current other camera settings and archiving the historical
checkpoint. All five frozen fingerprints were verified immediately before the
single named live archival run. The production fresh-read inspection and
retirement passed in **5.680 seconds**. No settings data was uploaded.

The complete checkpoint is retained on Pixel at
`no_backup/bluetooth-recovery-archive/2026-10-28T15-24-23.203311Z-external-repair/`.
Its seven files include the unchanged original, target and pending marker, fresh
inspection, immutable retirement read-back, acceptance record and independent
hardware original. All four moved frozen files match their original hashes at
the archived paths; the fifth frozen file, `files/library.json`, matches at its
unchanged live path. Every hash in the acceptance record was independently
checked after archival. The original baseline JSON remains unchanged.

All original recipe bytes still match. The 18 compared-prefix differences are
accepted as the current baseline for other settings, not identified as harmless
or restored. The audit explicitly records `historicalWriteVerified=false`.
The marker remains byte `01` inside the archive. The old active checkpoint
directory and old independent-original path are absent, and controller recovery
state is clear. New proofs may now save their own fresh references and use the
new settings-verification policy.

The seven files and an archive-path mapping report were copied privately under
ignored `captures/bluetooth/c7-app/recovery-inspection/approved-legacy-archive/`
with the same archive basename. The live instrumentation log is
`/private/tmp/fuji-approved-legacy-retirement.log`. The latest test APK was then
installed with `adb install -r`; the next gate is the controlled native-bond C1
write/restoration proof. No previous write proof is retroactively marked passed.

### Recipe proof and scope correction

The native-bond C1 Color +3 → 0 → +3 trial completed both shipping writes and
the independent forward recipe checks, but the overall proof failed after
53.691 seconds on the final comparison with its starting reference. The saved
restoration target/read-back match all 7,168 original recipe bytes. The compared
nonrecipe differences are offsets 264–265 (checksum) and 2136 (AF-area X).
The independent starting reference remains at
`no_backup/bluetooth-slot-1-hardware-original.dat`; do not overwrite or discard
it. The shipping restoration has no active pending marker because its own
fresh target/read-back verification passed. The original historical archive and
phone library remain intact.

The user confirmed that no camera controls were touched during the trial.
A subsequent four-backup, read-only comparison of the independent probe and
shipping reader passed in 22.598 seconds. Every recipe byte and compared prefix
byte stayed identical across those four reads; all seven current proof/library
fingerprints remained unchanged. This does not explain the earlier AF change.
Private evidence is under ignored
`captures/bluetooth/c7-app/native-bond-settings-proof-2026-10-28/`; logs are
`/private/tmp/fuji-native-bond-c1-settings-proof.log` and
`/private/tmp/fuji-af-read-only-trace.log`.

At the user's direction, firmware investigation is parked. The AF discrepancy
is an unresolved compatibility issue, not a passed full-settings proof or a
new verification exclusion. Continue recipe connection, import, slot selection,
write UI, cancellation and reconnect work. Preserve existing safeguards. The
hardware proof now saves independent evidence before assertions and requires
independent forward verification before automatically restoring a recipe.
