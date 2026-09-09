package dev.bondarenko.fujirecipes.camera.raw

import dev.bondarenko.fujirecipes.camera.CameraModels
import dev.bondarenko.fujirecipes.data.model.Recipe
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
    recipe: Recipe,
): RawProfilePatch {
    return when (CameraModels.normaliseModel(cameraModel)) {
        "X100VI" -> patchNative625(base, recipe)
        "XT50" -> {
            val identifier = nativeProfileIdentifier(base)
            if (base.size != 625 || parameterCount(base) != 29 || identifier != "FF189504") {
                throw UnsupportedRawProfile(
                    "X-T50 returned an unrecognised RAW profile " +
                        "(${base.size} bytes, identifier ${identifier ?: "missing"}).",
                )
            }
            patchNative625(base, recipe)
        }
        else -> throw UnsupportedRawProfile(
            "RAW recipe mapping for $cameraModel has not been calibrated. Uploading and native " +
                "profile capture are supported, but conversion stays disabled until a verified " +
                "0xD185 fixture is added for this model.",
        )
    }
}

private fun patchNative625(base: ByteArray, recipe: Recipe): RawProfilePatch {
    if (base.size != 625) {
        throw UnsupportedRawProfile(
            "X100VI returned a ${base.size}-byte RAW profile; the verified layout is 625 bytes.",
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

    fun set(index: Int, field: String, value: Int?) {
        if (value == null) {
            preserved += field
        } else {
            view.putInt(start + index * 4, value)
            applied += field
        }
    }

    val settings = recipe.settings
    val filmId = settings.string("filmSimulation") ?: "provia"
    val monochrome = filmId in MONOCHROME_FILM_SIMULATIONS
    set(8, "filmSimulation", RAW_FILM_SIMULATION_CODES[filmId])
    set(4, "exposureCompensation", ((settings.number("exposureCompensation") ?: 0.0) * 1000).roundToInt())
    set(6, "dynamicRange", RAW_DYNAMIC_RANGE_CODES[settings.string("dynamicRange") ?: "dr-auto"])

    val grain = settings.string("grainEffect") ?: "off"
    val grainSize = settings.string("grainSize") ?: "small"
    set(9, "grainEffect", RAW_GRAIN_CODES[grain to grainSize])
    set(10, "colorChromeEffect", RAW_EFFECT_CODES[settings.string("colorChromeEffect") ?: "off"])
    set(25, "colorChromeFxBlue", RAW_EFFECT_CODES[settings.string("colorChromeFxBlue") ?: "off"])

    val whiteBalance = settings.string("whiteBalance") ?: "auto"
    set(12, "whiteBalance", RAW_WHITE_BALANCE_CODES[whiteBalance])
    set(13, "wbShiftRed", (settings.number("wbShiftRed") ?: 0.0).roundToInt())
    set(14, "wbShiftBlue", (settings.number("wbShiftBlue") ?: 0.0).roundToInt())
    set(
        15,
        "colorTemperature",
        if (whiteBalance == "color-temp") {
            (settings.number("colorTemperature") ?: 5500.0).roundToInt()
        } else {
            null
        },
    )

    set(16, "highlightTone", ((settings.number("highlightTone") ?: 0.0) * 10).roundToInt())
    set(17, "shadowTone", ((settings.number("shadowTone") ?: 0.0) * 10).roundToInt())
    set(18, "color", if (monochrome) null else ((settings.number("color") ?: 0.0) * 10).roundToInt())
    set(19, "sharpness", ((settings.number("sharpness") ?: 0.0) * 10).roundToInt())
    set(20, "highIsoNR", RAW_NOISE_REDUCTION_CODES[(settings.number("highIsoNR") ?: 0.0).roundToInt()])
    set(27, "clarity", ((settings.number("clarity") ?: 0.0) * 10).roundToInt())

    preserved += listOf("dRangePriority", "monochromaticColorWc", "monochromaticColorMg", "isoMin", "isoMax")
    return RawProfilePatch(
        patched,
        applied.distinct(),
        preserved.distinct(),
    )
}

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

private fun kotlinx.serialization.json.JsonObject.string(key: String): String? =
    (get(key) as? JsonPrimitive)?.content

private fun kotlinx.serialization.json.JsonObject.number(key: String): Double? =
    get(key)?.jsonPrimitive?.doubleOrNull

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
