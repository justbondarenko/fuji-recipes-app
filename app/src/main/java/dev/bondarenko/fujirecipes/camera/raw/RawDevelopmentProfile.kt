package dev.bondarenko.fujirecipes.camera.raw

import dev.bondarenko.fujirecipes.camera.CameraModels
import dev.bondarenko.fujirecipes.data.model.Recipe
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

const val RAW_PROFILE_PROPERTY = 0xd185
const val RAW_CONVERSION_TRIGGER_PROPERTY = 0xd183

data class RawProfilePatch(
    val bytes: ByteArray,
    val appliedFields: List<String>,
    val preservedFields: List<String>,
)

class UnsupportedRawProfile(message: String) : Exception(message)

/**
 * Patches only native 625-byte layouts selected by an exact camera/profile adapter.
 * Sharing a processor generation is never sufficient: X-T50 is additionally pinned to the
 * processor identifier captured from the physical body used for its fixture.
 */
fun patchRawDevelopmentProfile(
    cameraModel: String,
    base: ByteArray,
    settings: JsonObject,
): RawProfilePatch {
    return when (CameraModels.normaliseModel(cameraModel)) {
        "X100VI" -> patchNative625(base, settings)
        "XT50" -> {
            val identifier = nativeProfileIdentifier(base)
            if (base.size != 625 || parameterCount(base) != 29 || identifier != "FF189504") {
                throw UnsupportedRawProfile(
                    "X-T50 returned an unrecognised RAW profile " +
                        "(${base.size} bytes, identifier ${identifier ?: "missing"}).",
                )
            }
            patchNative625(base, settings)
        }
        else -> throw UnsupportedRawProfile(
            "RAW recipe mapping for $cameraModel has not been calibrated. Uploading and native " +
                "profile capture are supported, but conversion stays disabled until a verified " +
                "0xD185 fixture is added for this model.",
        )
    }
}

/** The stored-recipe form. The lab renders unsaved settings through the overload above. */
fun patchRawDevelopmentProfile(
    cameraModel: String,
    base: ByteArray,
    recipe: Recipe,
): RawProfilePatch = patchRawDevelopmentProfile(cameraModel, base, recipe.settings)

private fun patchNative625(base: ByteArray, settings: JsonObject): RawProfilePatch {
    if (base.size != 625) {
        throw UnsupportedRawProfile(
            "The camera returned a ${base.size}-byte RAW profile; the verified layout is 625 bytes.",
        )
    }
    val count = parameterCount(base)
    if (count < 28) throw UnsupportedRawProfile("The RAW profile declares only $count parameters.")
    val start = base.size - count * 4
    if (start < 2) throw UnsupportedRawProfile("The RAW profile parameter table is malformed.")

    val patched = base.copyOf()
    val view = ByteBuffer.wrap(patched).order(ByteOrder.LITTLE_ENDIAN)
    val applied = mutableListOf<String>()
    val preserved = mutableListOf<String>()

    val values = RawSettings(settings)
    NATIVE_625_FIELDS.forEach { field ->
        val value = field.encode(values)
        if (value == null) {
            preserved += field.ids
        } else {
            view.putInt(start + field.index * 4, value)
            applied += field.ids
        }
    }

    preserved += RAW_UNRENDERED_FIELD_IDS
    return RawProfilePatch(patched, applied.distinct(), preserved.distinct())
}

/**
 * The settings a RAW render reads, resolved through the documented defaults.
 *
 * A sparse recipe must render the same way every time, so a missing key falls back to the
 * field's default rather than to whichever camera setting happened to be active — the rule
 * `specs/plans/in-camera-raw-development.md` §3 sets for applying a stored recipe.
 */
private class RawSettings(private val settings: JsonObject) {
    fun string(key: String): String? = (settings[key] as? JsonPrimitive)?.content

    fun number(key: String): Double? = settings[key]?.jsonPrimitive?.doubleOrNull

    /** A setting scaled by ten, which is how the profile carries the ±N tone controls. */
    fun tenths(key: String): Int = ((number(key) ?: 0.0) * 10).roundToInt()

    val filmSimulation: String get() = string("filmSimulation") ?: "provia"

    val isMonochrome: Boolean get() = filmSimulation in MONOCHROME_FILM_SIMULATIONS

    val whiteBalance: String get() = string("whiteBalance") ?: "auto"
}

/**
 * One profile word, and the recipe fields that decide it.
 *
 * Declared rather than written out as statements so that "what this adapter applies" has a
 * single answer: [rawSupportedFieldIds] reads the same table the patch writes from, and
 * `RawFieldSupportTest` holds the two to it.
 *
 * `ids` is plural because a word can be decided by more than one field — grain's strength and
 * size share one enum — and both of those are settings the user can change and expect to see.
 */
private class NativeProfileField(
    val ids: List<String>,
    val index: Int,
    /** Null preserves the camera's own value for this word. */
    val encode: (RawSettings) -> Int?,
)

private val NATIVE_625_FIELDS: List<NativeProfileField> = listOf(
    NativeProfileField(listOf("exposureCompensation"), 4) {
        ((it.number("exposureCompensation") ?: 0.0) * 1000).roundToInt()
    },
    NativeProfileField(listOf("dynamicRange"), 6) {
        // `dr-auto` has no confirmed explicit encoding, so the camera's base survives it.
        RAW_DYNAMIC_RANGE_CODES[it.string("dynamicRange") ?: "dr-auto"]
    },
    NativeProfileField(listOf("filmSimulation"), 8) { RAW_FILM_SIMULATION_CODES[it.filmSimulation] },
    NativeProfileField(listOf("grainEffect", "grainSize"), 9) {
        RAW_GRAIN_CODES[(it.string("grainEffect") ?: "off") to (it.string("grainSize") ?: "small")]
    },
    NativeProfileField(listOf("colorChromeEffect"), 10) {
        RAW_EFFECT_CODES[it.string("colorChromeEffect") ?: "off"]
    },
    NativeProfileField(listOf("whiteBalance"), 12) { RAW_WHITE_BALANCE_CODES[it.whiteBalance] },
    NativeProfileField(listOf("wbShiftRed"), 13) { (it.number("wbShiftRed") ?: 0.0).roundToInt() },
    NativeProfileField(listOf("wbShiftBlue"), 14) { (it.number("wbShiftBlue") ?: 0.0).roundToInt() },
    NativeProfileField(listOf("colorTemperature"), 15) {
        // Only meaningful under the Kelvin white balance; any other mode keeps the base word.
        if (it.whiteBalance == "color-temp") {
            (it.number("colorTemperature") ?: 5500.0).roundToInt()
        } else {
            null
        }
    },
    NativeProfileField(listOf("highlightTone"), 16) { it.tenths("highlightTone") },
    NativeProfileField(listOf("shadowTone"), 17) { it.tenths("shadowTone") },
    NativeProfileField(listOf("color"), 18) { if (it.isMonochrome) null else it.tenths("color") },
    NativeProfileField(listOf("sharpness"), 19) { it.tenths("sharpness") },
    NativeProfileField(listOf("highIsoNR"), 20) {
        RAW_NOISE_REDUCTION_CODES[(it.number("highIsoNR") ?: 0.0).roundToInt()]
    },
    NativeProfileField(listOf("colorChromeFxBlue"), 25) {
        RAW_EFFECT_CODES[it.string("colorChromeFxBlue") ?: "off"]
    },
    NativeProfileField(listOf("clarity"), 27) { it.tenths("clarity") },
)

/** Every field the native 625-byte adapter can act on, whatever the recipe says. */
internal val NATIVE_625_FIELD_IDS: Set<String> =
    NATIVE_625_FIELDS.flatMap { it.ids }.toSet()

/**
 * Stored, exported and displayed — never rendered.
 *
 * `dRangePriority` because the available mappings are uncertain, the monochromatic pair
 * because the known layouts expose a different representation, and the ISO bounds because
 * they are shooting advice rather than development controls.
 */
val RAW_UNRENDERED_FIELD_IDS: Set<String> = setOf(
    "dRangePriority",
    "monochromaticColorWc",
    "monochromaticColorMg",
    "isoMin",
    "isoMax",
)

private fun parameterCount(base: ByteArray): Int =
    if (base.size < 2) 0
    else ByteBuffer.wrap(base, 0, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xffff

private fun nativeProfileIdentifier(base: ByteArray): String? {
    if (base.size < 3) return null
    val length = base[2].toInt() and 0xff
    if (length == 0 || 3 + length * 2 > base.size) return null
    return buildString(length) {
        repeat(length) { index ->
            val offset = 3 + index * 2
            val codePoint = (base[offset].toInt() and 0xff) or
                ((base[offset + 1].toInt() and 0xff) shl 8)
            if (codePoint != 0) append(codePoint.toChar())
        }
    }
}

private val RAW_FILM_SIMULATION_CODES = mapOf(
    "provia" to 0x01,
    "velvia" to 0x02,
    "astia" to 0x03,
    "pro-neg-hi" to 0x04,
    "pro-neg-std" to 0x05,
    "monochrome" to 0x06,
    "monochrome-ye" to 0x07,
    "monochrome-r" to 0x08,
    "monochrome-g" to 0x09,
    "sepia" to 0x0a,
    "classic-chrome" to 0x0b,
    "acros" to 0x0c,
    "acros-ye" to 0x0d,
    "acros-r" to 0x0e,
    "acros-g" to 0x0f,
    "eterna" to 0x10,
    "classic-negative" to 0x11,
    "eterna-bleach-bypass" to 0x12,
    "nostalgic-negative" to 0x13,
    "reala-ace" to 0x14,
)

private val MONOCHROME_FILM_SIMULATIONS = setOf(
    "monochrome", "monochrome-ye", "monochrome-r", "monochrome-g", "sepia",
    "acros", "acros-ye", "acros-r", "acros-g",
)

private val RAW_DYNAMIC_RANGE_CODES = mapOf("dr100" to 100, "dr200" to 200, "dr400" to 400)

private val RAW_GRAIN_CODES = mapOf(
    ("off" to "small") to 1,
    ("off" to "large") to 1,
    ("weak" to "small") to 2,
    ("strong" to "small") to 3,
    ("weak" to "large") to 4,
    ("strong" to "large") to 5,
)

private val RAW_EFFECT_CODES = mapOf("off" to 1, "weak" to 2, "strong" to 3)

private val RAW_WHITE_BALANCE_CODES = mapOf(
    "auto" to 0x0002,
    "auto-ambience-priority" to 0x8021,
    "daylight" to 0x0004,
    "incandescent" to 0x0006,
    "underwater" to 0x0008,
    "fluorescent-1" to 0x8001,
    "fluorescent-2" to 0x8002,
    "fluorescent-3" to 0x8003,
    "shade" to 0x8006,
    "color-temp" to 0x8007,
)

private val RAW_NOISE_REDUCTION_CODES = mapOf(
    -4 to 0x8000,
    -3 to 0x7000,
    -2 to 0x4000,
    -1 to 0x3000,
    0 to 0x2000,
    1 to 0x1000,
    2 to 0x0000,
    3 to 0x6000,
    4 to 0x5000,
)
