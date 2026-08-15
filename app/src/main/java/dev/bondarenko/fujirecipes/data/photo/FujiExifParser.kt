package dev.bondarenko.fujirecipes.data.photo

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The recipe a Fujifilm body wrote into a JPEG's MakerNote.
 *
 * **Transcribed from** `fuji-recipes-book/src/utils/fuji-exif-parser.ts` at commit `0c17106`
 * (`coding-standards.md` P3).
 *
 * **Why this is hand-rolled.** `androidx.exifinterface` reads standard EXIF and stops at the
 * MakerNote — which is where every recipe field lives. The walk from the `FUJIFILM` signature
 * through its IFD is the whole feature, and it is a few hundred lines of byte work rather
 * than a dependency.
 *
 * Pure: no `android.*`, no I/O. The caller reads the bytes; this reads the recipe out of them,
 * which is what lets the whole of it be tested against a JPEG assembled in a test.
 *
 * ---
 *
 * **⚠ These encodings are a THIRD dialect.** This app already holds two others, and they are
 * not interchangeable:
 *
 * | | Where | Example |
 * |---|---|---|
 * | Custom-slot properties | `camera/plan/CameraEncoding.kt` | grain is one flat 1–5 enum |
 * | The RAW-conversion profile | (not implemented) | effects are 0/2/3 |
 * | **The MakerNote — here** | this file | grain roughness is 0/32/64 |
 *
 * Reading a value from a photo with the slot tables, or writing one to a slot with these,
 * produces a recipe that looks right and is wrong. Hence the separate file and this notice.
 */

// ─── The result ─────────────────────────────────────────────────────────────

/**
 * Why a photo produced no recipe.
 *
 * Named rather than thrown, and each one is a different message with a different remedy —
 * `coding-standards.md` P5. In particular [NoExif] and [NotFujifilm] are *different facts*:
 * one is a photo that was stripped or re-encoded, the other is someone else's camera.
 */
enum class PhotoReadFailure {
    NOT_JPEG,
    TOO_LARGE,
    NO_EXIF,
    NOT_FUJIFILM,
    UNREADABLE,
}

data class PhotoRecipe(
    /** From standard EXIF IFD0, when the file carries it. */
    val cameraModel: String? = null,
    val filmSimulationLabel: String? = null,
    /**
     * Field id to value, in **this app's own vocabulary** — `dr400`, not `400`; `color-temp`,
     * not `kelvin` — so the result feeds the matcher and the editor with no second
     * translation.
     */
    val settings: Map<String, Any> = emptyMap(),
    /** Label to formatted value, in MakerNote order, for display and copy-as-text. */
    val rawValues: Map<String, String> = emptyMap(),
)

sealed interface PhotoReadResult {
    data class Success(val recipe: PhotoRecipe) : PhotoReadResult
    data class Failure(val reason: PhotoReadFailure) : PhotoReadResult
}

/** Refused before reading. A body's RAF is far larger than any JPEG it writes. */
const val MAX_PHOTO_BYTES = 50L * 1024 * 1024

// ─── The MakerNote dialect ──────────────────────────────────────────────────

private val FILM_MODE = mapOf(
    0 to ("provia" to "PROVIA / Standard"),
    256 to ("pro-neg-std" to "PRO Neg. Std"),
    272 to ("pro-neg-hi" to "PRO Neg. Hi"),
    288 to ("astia" to "ASTIA / Soft"),
    304 to ("astia" to "ASTIA / Soft"),
    512 to ("velvia" to "Velvia / Vivid"),
    768 to ("pro-neg-std" to "PRO Neg. Std"),
    1024 to ("velvia" to "Velvia / Vivid"),
    1280 to ("pro-neg-std" to "PRO Neg. Std"),
    1281 to ("pro-neg-hi" to "PRO Neg. Hi"),
    1536 to ("classic-chrome" to "Classic Chrome"),
    1792 to ("eterna" to "ETERNA / Cinema"),
    2048 to ("classic-negative" to "Classic Negative"),
    2304 to ("eterna-bleach-bypass" to "ETERNA Bleach Bypass"),
    2560 to ("nostalgic-negative" to "Nostalgic Neg."),
    2816 to ("reala-ace" to "REALA ACE"),
)

/** Mapped straight to this app's white-balance ids; the label is the camera's wording. */
private val WHITE_BALANCE = mapOf(
    0 to ("auto" to "Auto"),
    1 to ("auto" to "Auto (White Priority)"),
    2 to ("auto-ambience-priority" to "Auto (Ambience Priority)"),
    256 to ("daylight" to "Daylight"),
    512 to ("shade" to "Cloudy / Shade"),
    768 to ("fluorescent-1" to "Daylight Fluorescent"),
    769 to ("fluorescent-2" to "Day White Fluorescent"),
    770 to ("fluorescent-3" to "White Fluorescent"),
    771 to ("fluorescent-3" to "Warm White Fluorescent"),
    772 to ("fluorescent-3" to "Living Room Warm White"),
    1024 to ("incandescent" to "Incandescent"),
    1280 to ("underwater" to "Flash"),
    1536 to ("underwater" to "Underwater"),
    3840 to ("custom-1" to "Custom 1"),
    3841 to ("custom-2" to "Custom 2"),
    3842 to ("custom-3" to "Custom 3"),
    3843 to ("custom-3" to "Custom 4"),
    3844 to ("custom-3" to "Custom 5"),
    4080 to ("color-temp" to "Kelvin"),
)

/** The tag that means "read the temperature from 4101". */
private const val WB_KELVIN_CODE = 4080

private val DYNAMIC_RANGE = mapOf(0 to "Auto", 1 to "Manual", 256 to "100", 512 to "200", 513 to "400")

/**
 * **Negated and ×16.** `-64` is `+4`, `16` is `-1`.
 *
 * The single easiest thing in this file to get backwards, and getting it backwards produces a
 * recipe that is plausible and inverted. A test pins it.
 */
private val TONE_CURVE = mapOf(-64 to 4.0, -48 to 3.0, -32 to 2.0, -16 to 1.0, 0 to 0.0, 16 to -1.0, 32 to -2.0)

private val SATURATION = mapOf(
    0 to 0, 128 to 1, 256 to 2, 192 to 3, 224 to 4,
    384 to -1, 1024 to -2, 1216 to -3, 1248 to -4,
)

private val SHARPNESS = mapOf(0 to -4, 1 to -3, 2 to -2, 130 to -1, 3 to 0, 132 to 1, 4 to 2, 5 to 3, 6 to 4)

/** Not monotonic, not arithmetic — a table, like the custom-slot NR and for the same reason. */
private val NOISE_REDUCTION = mapOf(
    736 to -4, 704 to -3, 512 to -2, 640 to -1, 0 to 0,
    384 to 1, 256 to 2, 448 to 3, 480 to 4,
)

/** ×1000. */
private val CLARITY = mapOf(
    -5000 to -5, -4000 to -4, -3000 to -3, -2000 to -2, -1000 to -1, 0 to 0,
    1000 to 1, 2000 to 2, 3000 to 3, 4000 to 4, 5000 to 5,
)

private val GRAIN_ROUGHNESS = mapOf(0 to "off", 32 to "weak", 64 to "strong")
private val GRAIN_SIZE = mapOf(0 to "off", 16 to "small", 32 to "large")
private val EFFECT = mapOf(0 to "off", 32 to "weak", 64 to "strong")

// The MakerNote tag ids this build reads. Anything else in the IFD is skipped.
private const val TAG_SHARPNESS = 4097
private const val TAG_WHITE_BALANCE = 4098
private const val TAG_SATURATION = 4099
private const val TAG_COLOR_TEMPERATURE = 4101
private const val TAG_WB_SHIFT = 4106
private const val TAG_NOISE_REDUCTION = 4110
private const val TAG_CLARITY = 4111
private const val TAG_SHADOW_TONE = 4160
private const val TAG_HIGHLIGHT_TONE = 4161
private const val TAG_GRAIN_ROUGHNESS = 4167
private const val TAG_COLOR_CHROME = 4168
private const val TAG_GRAIN_SIZE = 4172
private const val TAG_COLOR_CHROME_FX_BLUE = 4174
private const val TAG_FILM_MODE = 5121
private const val TAG_DR_SETTING = 5122
private const val TAG_DR_VALUE = 5123

// ─── The parse ──────────────────────────────────────────────────────────────

private class Entry(val value: Int, val entryOffset: Int)

/**
 * Reads the recipe out of a JPEG's bytes.
 *
 * Every exit is a named [PhotoReadResult] rather than an exception: the caller has five
 * different things to tell the user, and a thrown error collapses them into one.
 */
fun parseRecipeFromJpeg(bytes: ByteArray): PhotoReadResult {
    if (bytes.size > MAX_PHOTO_BYTES) return PhotoReadResult.Failure(PhotoReadFailure.TOO_LARGE)

    // The SOI marker. Not a name check: a file called .jpg that is not one fails here, which
    // is the honest place for it to fail.
    if (bytes.size < 4 || bytes[0] != 0xFF.toByte() || bytes[1] != 0xD8.toByte()) {
        return PhotoReadResult.Failure(PhotoReadFailure.NOT_JPEG)
    }

    val view = ByteBuffer.wrap(bytes)
    val cameraModel = runCatching { readCameraModel(bytes, view) }.getOrNull()

    val fujiOffset = findMakerNote(bytes, view)
    if (fujiOffset < 0) {
        // A JPEG with no EXIF at all and one from another make are different answers. The
        // APP1 marker is the thing that separates them.
        return PhotoReadResult.Failure(
            if (hasExif(bytes, view)) PhotoReadFailure.NOT_FUJIFILM else PhotoReadFailure.NO_EXIF,
        )
    }

    val tags = runCatching { readMakerNoteTags(bytes, view, fujiOffset) }.getOrNull()
        ?: return PhotoReadResult.Failure(PhotoReadFailure.UNREADABLE)

    if (tags.isEmpty()) return PhotoReadResult.Failure(PhotoReadFailure.UNREADABLE)

    return PhotoReadResult.Success(buildRecipe(tags, cameraModel, bytes, view, fujiOffset))
}

/**
 * Whether the file carries an EXIF APP1 segment at all.
 *
 * Sets the byte order it needs rather than inheriting it. [ByteBuffer.order] is *state on the
 * buffer*, and every reader here shares one — so a function that assumes the order left by
 * whoever ran last reads a segment length backwards the moment the call sequence changes.
 * Every reader in this file sets its own.
 */
private fun hasExif(bytes: ByteArray, view: ByteBuffer): Boolean {
    view.order(ByteOrder.BIG_ENDIAN)

    var offset = 2
    while (offset + 4 < bytes.size) {
        if (bytes[offset] != 0xFF.toByte()) return false
        val marker = bytes[offset + 1].toInt() and 0xFF
        // JPEG segment lengths are big-endian regardless of the TIFF block's own order.
        val length = view.getShort(offset + 2).toInt() and 0xFFFF

        if (marker == 0xE1 && length >= 8) {
            val header = String(bytes, offset + 4, 4, Charsets.US_ASCII)
            if (header == "Exif") return true
        }
        if (length <= 0) return false
        offset += 2 + length
    }
    return false
}

/**
 * Scans for the `FUJIFILM` signature and confirms it really is a MakerNote IFD.
 *
 * The confirmation matters: those eight bytes could occur in image data. Reading the IFD
 * offset and checking the entry count is plausible is what tells a signature from a
 * coincidence.
 */
private fun findMakerNote(bytes: ByteArray, view: ByteBuffer): Int {
    view.order(ByteOrder.LITTLE_ENDIAN)

    val signature = "FUJIFILM".toByteArray(Charsets.US_ASCII)
    val maxScan = min(bytes.size - 14, 512 * 1024)

    outer@ for (at in 0 until maxScan) {
        if (bytes[at] != signature[0]) continue
        for (j in 1 until signature.size) {
            if (bytes[at + j] != signature[j]) continue@outer
        }

        val ifdOffset = at + view.getInt(at + 8)
        if (ifdOffset + 2 > bytes.size || ifdOffset < 0) continue

        val count = view.getShort(ifdOffset).toInt() and 0xFFFF
        if (count in 1..249) return at
    }

    return -1
}

/**
 * The MakerNote IFD's entries, by tag id.
 *
 * Formats 3, 4 and 9 are u16, u32 and i32 — the three this build reads. Anything else keeps
 * its entry offset (for the pointer tags) with a zero value, which is what the WB-shift read
 * needs.
 */
private fun readMakerNoteTags(bytes: ByteArray, view: ByteBuffer, fujiOffset: Int): Map<Int, Entry> {
    view.order(ByteOrder.LITTLE_ENDIAN)

    val ifdStart = fujiOffset + view.getInt(fujiOffset + 8)
    if (ifdStart + 2 > bytes.size) return emptyMap()

    val count = view.getShort(ifdStart).toInt() and 0xFFFF
    val tags = mutableMapOf<Int, Entry>()

    for (index in 0 until count) {
        val entry = ifdStart + 2 + index * 12
        if (entry + 12 > bytes.size) break

        val tagId = view.getShort(entry).toInt() and 0xFFFF
        val value = when (view.getShort(entry + 2).toInt() and 0xFFFF) {
            3 -> view.getShort(entry + 8).toInt() and 0xFFFF
            4 -> view.getInt(entry + 8)
            9 -> view.getInt(entry + 8)
            else -> 0
        }

        tags[tagId] = Entry(value, entry)
    }

    return tags
}

/** The model string from standard EXIF IFD0, when the file carries one. */
private fun readCameraModel(bytes: ByteArray, view: ByteBuffer): String? {
    var offset = 2
    while (offset + 4 < bytes.size) {
        // Re-set each pass: readModelFromTiff below switches the order to the TIFF block's,
        // and the next segment length is big-endian again.
        view.order(ByteOrder.BIG_ENDIAN)

        if (bytes[offset] != 0xFF.toByte()) return null
        val marker = bytes[offset + 1].toInt() and 0xFF
        val length = view.getShort(offset + 2).toInt() and 0xFFFF

        if (marker == 0xE1 && length >= 14 &&
            String(bytes, offset + 4, 4, Charsets.US_ASCII) == "Exif"
        ) {
            return readModelFromTiff(bytes, view, offset + 10)
        }

        if (length <= 0) return null
        offset += 2 + length
    }
    return null
}

private fun readModelFromTiff(bytes: ByteArray, view: ByteBuffer, tiff: Int): String? {
    if (tiff + 8 > bytes.size) return null

    val little = bytes[tiff] == 'I'.code.toByte()
    view.order(if (little) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN)

    val ifd0 = tiff + view.getInt(tiff + 4)
    if (ifd0 + 2 > bytes.size) return null

    val count = view.getShort(ifd0).toInt() and 0xFFFF

    for (index in 0 until count) {
        val entry = ifd0 + 2 + index * 12
        if (entry + 12 > bytes.size) break

        // 0x0110 is Model.
        if ((view.getShort(entry).toInt() and 0xFFFF) != 0x0110) continue

        val length = view.getInt(entry + 4)
        val valueOffset = if (length > 4) tiff + view.getInt(entry + 8) else entry + 8
        if (valueOffset < 0 || valueOffset + length > bytes.size) return null

        return String(bytes, valueOffset, length, Charsets.US_ASCII).trimEnd(' ', ' ')
            .ifBlank { null }
    }

    return null
}

// ─── Tags to a recipe ───────────────────────────────────────────────────────

private fun buildRecipe(
    tags: Map<Int, Entry>,
    cameraModel: String?,
    bytes: ByteArray,
    view: ByteBuffer,
    fujiOffset: Int,
): PhotoRecipe {
    val settings = linkedMapOf<String, Any>()
    val raw = linkedMapOf<String, String>()
    var simulationLabel: String? = null

    // A value this build cannot name is left out. A recipe missing a field is useful; a
    // recipe carrying a wrong field is worse than useless, because it will match something.
    tags[TAG_FILM_MODE]?.let { entry ->
        FILM_MODE[entry.value]?.let { (id, label) ->
            settings["filmSimulation"] = id
            simulationLabel = label
            raw["Film simulation"] = label
        }
    }

    tags[TAG_WHITE_BALANCE]?.let { entry ->
        WHITE_BALANCE[entry.value]?.let { (id, label) ->
            settings["whiteBalance"] = id
            raw["White balance"] = label

            if (entry.value == WB_KELVIN_CODE) {
                tags[TAG_COLOR_TEMPERATURE]?.let { kelvin ->
                    settings["colorTemperature"] = kelvin.value
                    raw["Colour temperature"] = "${kelvin.value}K"
                }
            }
        }
    }

    readWbShift(tags, bytes, view, fujiOffset)?.let { (red, blue) ->
        settings["wbShiftRed"] = red
        settings["wbShiftBlue"] = blue
        raw["WB shift"] = "R ${signed(red)} / B ${signed(blue)}"
    }

    tags[TAG_DR_SETTING]?.let { entry ->
        // Setting 1 is "Manual", and then the actual percentage lives in the next tag.
        val value = if (entry.value == 1) {
            tags[TAG_DR_VALUE]?.value?.toString()
        } else {
            DYNAMIC_RANGE[entry.value]
        }

        when (value) {
            "Auto" -> {
                settings["dynamicRange"] = "dr-auto"
                raw["Dynamic range"] = "Auto"
            }

            "100", "200", "400" -> {
                settings["dynamicRange"] = "dr$value"
                raw["Dynamic range"] = "DR$value"
            }
        }
    }

    tone(tags[TAG_HIGHLIGHT_TONE])?.let {
        settings["highlightTone"] = it
        raw["Highlight tone"] = signed(it)
    }
    tone(tags[TAG_SHADOW_TONE])?.let {
        settings["shadowTone"] = it
        raw["Shadow tone"] = signed(it)
    }

    tags[TAG_SATURATION]?.let { entry ->
        SATURATION[entry.value]?.let {
            settings["color"] = it
            raw["Colour"] = signed(it.toDouble())
        }
    }
    tags[TAG_SHARPNESS]?.let { entry ->
        SHARPNESS[entry.value]?.let {
            settings["sharpness"] = it
            raw["Sharpness"] = signed(it.toDouble())
        }
    }
    tags[TAG_NOISE_REDUCTION]?.let { entry ->
        NOISE_REDUCTION[entry.value]?.let {
            settings["highIsoNR"] = it
            raw["High ISO NR"] = signed(it.toDouble())
        }
    }
    tags[TAG_CLARITY]?.let { entry ->
        CLARITY[entry.value]?.let {
            settings["clarity"] = it
            raw["Clarity"] = signed(it.toDouble())
        }
    }

    // Grain is two tags, and "off" in the roughness tag means the size is meaningless — the
    // same rule WR-04 states from the writing side.
    val roughness = tags[TAG_GRAIN_ROUGHNESS]?.let { GRAIN_ROUGHNESS[it.value] } ?: "off"
    val size = tags[TAG_GRAIN_SIZE]?.let { GRAIN_SIZE[it.value] }

    settings["grainEffect"] = roughness
    if (roughness == "off") {
        raw["Grain effect"] = "Off"
    } else {
        if (size != null && size != "off") settings["grainSize"] = size
        raw["Grain effect"] = listOfNotNull(
            roughness.replaceFirstChar { it.uppercase() },
            size?.takeIf { it != "off" }?.replaceFirstChar { it.uppercase() },
        ).joinToString(", ")
    }

    tags[TAG_COLOR_CHROME]?.let { entry ->
        EFFECT[entry.value]?.let {
            settings["colorChromeEffect"] = it
            raw["Colour chrome effect"] = it.replaceFirstChar { c -> c.uppercase() }
        }
    }
    tags[TAG_COLOR_CHROME_FX_BLUE]?.let { entry ->
        EFFECT[entry.value]?.let {
            settings["colorChromeFxBlue"] = it
            raw["Colour chrome FX blue"] = it.replaceFirstChar { c -> c.uppercase() }
        }
    }

    return PhotoRecipe(
        cameraModel = cameraModel,
        filmSimulationLabel = simulationLabel,
        settings = settings,
        rawValues = raw,
    )
}

private fun tone(entry: Entry?): Double? = entry?.let { TONE_CURVE[it.value] }

/**
 * The white-balance shift, which is a **pointer tag**: its value field holds an offset to two
 * `int32`s rather than the values themselves.
 *
 * Divided by 20 and bounded to ±9, which is the range the field takes.
 */
private fun readWbShift(
    tags: Map<Int, Entry>,
    bytes: ByteArray,
    view: ByteBuffer,
    fujiOffset: Int,
): Pair<Int, Int>? {
    val entry = tags[TAG_WB_SHIFT] ?: return null

    view.order(ByteOrder.LITTLE_ENDIAN)
    val at = fujiOffset + view.getInt(entry.entryOffset + 8)
    if (at < 0 || at + 8 > bytes.size) return null

    fun bounded(raw: Int) = min(9, max(-9, (raw / 20.0).roundToInt()))

    return bounded(view.getInt(at)) to bounded(view.getInt(at + 4))
}

private fun signed(value: Double): String {
    val text = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
    return if (value > 0) "+$text" else text
}

private fun signed(value: Int): String = if (value > 0) "+$value" else "$value"
