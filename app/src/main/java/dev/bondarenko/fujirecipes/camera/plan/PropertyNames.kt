package dev.bondarenko.fujirecipes.camera.plan

/**
 * Names for the codes a camera report prints, and **only the codes this project can source.**
 *
 * A report exists to be pasted into an issue by someone whose body this app has never met. Its
 * value is the shape it records — data type, writable, allowed values — which needs no names
 * at all. A name is a convenience on top, and a *wrong* one is worse than none: it turns a
 * report into a claim about the body that the next person builds on.
 *
 * **What is in here.**
 *
 * - **ISO 15740 device properties** (`0x5001`–`0x501F`) and the standard operations. These are
 *   the specification; nothing about them is a guess.
 * - **The preset block** (`0xD18C`–`0xD1A5`). This project's own captures, via
 *   `field-definitions.md` §7 and `eggricesoy/filmkit` — the same source `CameraEncoding.kt`
 *   uses, and the two files must not disagree.
 * - **The singletons** this app reads or names outside that block: `0xD16E`, `0xD183`,
 *   `0xD184`, `0xD185`, `0xD186`, `0xD187`, `0xD310`, `0xD36A`, `0xD36D`. The last three of the
 *   `0xD18x` group were identified from an X-T50 run: `0xD184` returned an IOP code string and
 *   `0xD186`/`0xD187` both returned `X-T50_01…`, which is the `prop_group_version` that
 *   `petabyt/libfuji`'s profile parser names — matching the codes that header gives them.
 *
 * **What is deliberately not in here, and why.** The shooting-property range `0xD001`–`0xD10B`
 * is named in both `eggricesoy/filmkit` and `petabyt/libfuji`, and **the two contradict each
 * other** — `0xD007` is `ColorTemperature` in one and `DRangeMode` in the other, `0xD00A` is
 * `NoiseReduction` against `ColorSpace`, `0xD017` is `GrainEffect` against `ColorTemperature`,
 * `0xD171` is `RawConversionEdit` against `FocusPosition`. Neither is authoritative and nobody
 * here has a capture to settle it. So a report prints those codes bare, which is the honest
 * answer and is also precisely the evidence needed to settle them: the body's own declared
 * type and enumeration for a code says more than either table.
 *
 * The two codes inside the preset block that nobody has identified are named *as*
 * unidentified. That is knowledge — it says this build looked and did not find out — and it is
 * different from a code that is simply not in the table.
 *
 * Pure: no `android.*` (P4), enforced by `CameraPurityTest`.
 */

private const val UNIDENTIFIED = "(unidentified)"

/**
 * The preset block, plus the singletons around it.
 *
 * Every code `FIELD_PROPERTIES` writes to must appear here, and `PropertyNamesTest` fails the
 * build if one does not — a code the app writes but a report labels only by its number is the
 * sort of divergence nobody notices until a report is used to debug the wrong thing.
 */
private val FUJI_PROPERTY_NAMES: Map<Int, String> = mapOf(
    0xd16e to "USBMode",
    0xd184 to "IOPCode",
    0xd186 to "TetherRawConditionCode",
    0xd187 to "TetherRawCompatibilityCode",
    0xd183 to "StartRawConversion",
    0xd185 to "RawConvProfile",
    0xd310 to "TotalShotCount",
    0xd36a to "BatteryInfo1",
    0xd36d to "LensNameAndSerial",

    // ── The custom-slot block ──
    0xd18c to "PresetSlot",
    0xd18d to "PresetName",
    0xd18e to "Preset.ImageSize",
    0xd18f to "Preset.ImageQuality",
    0xd190 to "Preset.DynamicRange%",
    0xd191 to "Preset.$UNIDENTIFIED",
    0xd192 to "Preset.FilmSimulation",
    0xd193 to "Preset.MonochromaticColorWC",
    0xd194 to "Preset.MonochromaticColorMG",
    0xd195 to "Preset.GrainEffect",
    0xd196 to "Preset.ColorChromeEffect",
    0xd197 to "Preset.ColorChromeFXBlue",
    0xd198 to "Preset.SmoothSkin",
    0xd199 to "Preset.WhiteBalance",
    0xd19a to "Preset.WBShiftRed",
    0xd19b to "Preset.WBShiftBlue",
    0xd19c to "Preset.ColorTemperature",
    0xd19d to "Preset.HighlightTone",
    0xd19e to "Preset.ShadowTone",
    0xd19f to "Preset.Color",
    0xd1a0 to "Preset.Sharpness",
    0xd1a1 to "Preset.HighISONR",
    0xd1a2 to "Preset.Clarity",
    0xd1a3 to "Preset.LongExposureNR",
    0xd1a4 to "Preset.ColorSpace",
    0xd1a5 to "Preset.$UNIDENTIFIED",
)

/** ISO 15740 §13, the standard device properties. */
private val STANDARD_PROPERTY_NAMES: Map<Int, String> = mapOf(
    0x5001 to "BatteryLevel",
    0x5002 to "FunctionalMode",
    0x5003 to "ImageSize",
    0x5004 to "CompressionSetting",
    0x5005 to "WhiteBalance",
    0x5006 to "RGBGain",
    0x5007 to "FNumber",
    0x5008 to "FocalLength",
    0x5009 to "FocusDistance",
    0x500a to "FocusMode",
    0x500b to "ExposureMeteringMode",
    0x500c to "FlashMode",
    0x500d to "ExposureTime",
    0x500e to "ExposureProgramMode",
    0x500f to "ExposureIndex",
    0x5010 to "ExposureBiasCompensation",
    0x5011 to "DateTime",
    0x5012 to "CaptureDelay",
    0x5013 to "StillCaptureMode",
    0x5014 to "Contrast",
    0x5015 to "Sharpness",
    0x5016 to "DigitalZoom",
    0x5017 to "EffectMode",
    0x5018 to "BurstNumber",
    0x5019 to "BurstInterval",
    0x501a to "TimelapseNumber",
    0x501b to "TimelapseInterval",
    0x501c to "FocusMeteringMode",
    0x501d to "UploadURL",
    0x501e to "Artist",
    0x501f to "CopyrightInfo",
)

/** ISO 15740 §10, the standard operations. */
private val OPERATION_NAMES: Map<Int, String> = mapOf(
    0x1001 to "GetDeviceInfo",
    0x1002 to "OpenSession",
    0x1003 to "CloseSession",
    0x1004 to "GetStorageIDs",
    0x1005 to "GetStorageInfo",
    0x1006 to "GetNumObjects",
    0x1007 to "GetObjectHandles",
    0x1008 to "GetObjectInfo",
    0x1009 to "GetObject",
    0x100a to "GetThumb",
    0x100b to "DeleteObject",
    0x100c to "SendObjectInfo",
    0x100d to "SendObject",
    0x100e to "InitiateCapture",
    0x100f to "FormatStore",
    0x1014 to "GetDevicePropDesc",
    0x1015 to "GetDevicePropValue",
    0x1016 to "SetDevicePropValue",
    0x1017 to "ResetDevicePropValue",
    0x101b to "GetPartialObject",
)

/** The property's name, or null when this build cannot source one. */
fun propertyName(code: Int): String? =
    FUJI_PROPERTY_NAMES[code] ?: STANDARD_PROPERTY_NAMES[code]

/** The operation's name, or null. Vendor operations are all null, by design. */
fun operationName(code: Int): String? = OPERATION_NAMES[code]

/**
 * Every property code a report probes regardless of what the body listed.
 *
 * Fuji bodies do not reliably enumerate their vendor properties in `GetDeviceInfo` — this app
 * has always read the preset block without consulting that list, and it works — so a report
 * that only walked `devicePropertiesSupported` would miss exactly the codes it exists to
 * document. These are asked for on top of whatever the body did list.
 *
 * `0x5001` is in here despite being standard: an X-T50 lists it in card-reader mode and not in
 * RAW conversion mode, and it is the one property that declares the battery scale.
 */
val ALWAYS_PROBED_PROPERTIES: List<Int> =
    (FUJI_PROPERTY_NAMES.keys + STANDARD_BATTERY_PROPERTY).sorted()

/**
 * The custom-slot block.
 *
 * These registers **follow the slot selector**, so a single reading of them describes one
 * recipe rather than the body. A report walks them once per slot and lays the seven readings
 * side by side; everything else is walked once. `0xD18C` stays in the range on purpose — read
 * back per slot it is the check that the selector actually moved.
 */
val PRESET_BLOCK: IntRange = 0xd18c..0xd1a5

/**
 * The preset codes a report tabulates per slot.
 *
 * The name is excluded because it is a string rather than a 16-bit value and gets its own line
 * above the table.
 */
val PRESET_MATRIX_CODES: List<Int> = PRESET_BLOCK.filter { it != PRESET_NAME_PROPERTY }
