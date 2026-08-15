package dev.bondarenko.fujirecipes.data.text

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Read a recipe out of pasted text — FEAT-011.
 *
 * **Transcribed, not invented.**
 *
 * | | |
 * |---|---|
 * | Source | `fuji-recipes-book/src/utils/recipe-text-parser.ts` (`parseRecipeText`) |
 * | Behaviour source | `fuji-recipes-book/tests/unit/recipe-text-parser.spec.ts` |
 *
 * Two clients reading the same pasted text must land on the same recipe, so this is a
 * line-for-line port rather than a fresh reading of the same problem. No Compose and no
 * Android imports: the whole thing is testable on the JVM (`coding-standards.md` P9).
 *
 * `sensorGeneration` is detected but discarded — D1 migration 0002 dropped the column and
 * this app has no field for it (`RecipeFields` §identity). The detection stays because it
 * decides whether a line is a setting or the recipe's title.
 */
data class ParsedRecipeText(
    val name: String?,
    /** Ready to hand to `RecipeEditorRoute.prefill`; keys are `RecipeFields` ids. */
    val settings: JsonObject,
    val fieldsFound: Int,
) {
    val isEmpty: Boolean get() = name == null && fieldsFound == 0
}

fun parseRecipeText(rawText: String): ParsedRecipeText {
    val settings = LinkedHashMap<String, JsonPrimitive>()
    var name: String? = null

    /** First value wins, exactly as the web client's `setSetting` does. */
    fun set(key: String, value: JsonPrimitive) {
        settings.putIfAbsent(key, value)
    }

    fun set(key: String, value: String) = set(key, JsonPrimitive(value))

    /**
     * Whole numbers go out as integers: the rest of the platform stores a tone of 2 as `2`,
     * and a needless `2.0` is a needless difference.
     */
    fun set(key: String, value: Double) =
        set(key, if (value % 1.0 == 0.0) JsonPrimitive(value.toInt()) else JsonPrimitive(value))

    if (rawText.isBlank()) return ParsedRecipeText(null, JsonObject(emptyMap()), 0)

    val lines = rawText.split(Regex("\r?\n"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { it.replace(LEADING_BULLET, "").trim() }

    var isFirstUnmatchedLine = true

    for (line in lines) {
        val lower = line.lowercase()
        var matchedAny = false

        // 1. Explicit name / title
        val explicitName = EXPLICIT_NAME.find(line)?.groupValues?.get(1)?.trim()
        if (!explicitName.isNullOrEmpty()) {
            name = explicitName
            continue
        }

        // 2. Film simulation. A bare "Classic Chrome" line counts as much as a labelled one.
        parseFilmSimulation(lower)?.let {
            set("filmSimulation", it)
            matchedAny = true
        }

        // 3. Dynamic range
        if (lower.contains("dynamic range") ||
            Regex("""\bdr\d{3}\b""").containsMatchIn(lower) ||
            Regex("""\bdr\s*-?\s*auto\b""").containsMatchIn(lower) ||
            Regex("""^dr[:\s]""").containsMatchIn(lower)
        ) {
            parseDynamicRange(lower)?.let {
                set("dynamicRange", it)
                matchedAny = true
            }
        }

        // 4. Grain effect and grain size
        if (lower.contains("grain")) {
            parseOffWeakStrong(lower)?.let {
                set("grainEffect", it)
                matchedAny = true
            }
            when {
                lower.contains("small") -> "small"
                lower.contains("large") -> "large"
                else -> null
            }?.let {
                set("grainSize", it)
                matchedAny = true
            }
        }

        // 5. Colour chrome effect versus colour chrome FX blue — the second is not a
        //    stronger spelling of the first, it is a different setting.
        if (lower.contains("color chrome fx blue") ||
            lower.contains("colour chrome fx blue") ||
            lower.contains("cc fx blue") ||
            lower.contains("color chrome blue") ||
            lower.contains("chrome fx blue")
        ) {
            parseOffWeakStrong(lower)?.let {
                set("colorChromeFxBlue", it)
                matchedAny = true
            }
        } else if (lower.contains("color chrome") || lower.contains("colour chrome")) {
            parseOffWeakStrong(lower)?.let {
                set("colorChromeEffect", it)
                matchedAny = true
            }
        }

        // 6. White balance, its temperature, and its two shifts
        if (lower.contains("white balance") ||
            lower.contains("wb") ||
            Regex("""\b\d{4}\s*k\b""").containsMatchIn(lower)
        ) {
            parseWhiteBalanceMode(lower)?.let {
                set("whiteBalance", it)
                matchedAny = true
            }
            parseColorTemperature(line)?.let {
                set("colorTemperature", it.toDouble())
                matchedAny = true
            }
            // Clamped like every other numeric setting here: the form's own range is ±9,
            // and a value outside it would arrive in the field as invalid.
            parseShift(line, ShiftAxis.RED)?.let {
                set("wbShiftRed", it.coerceIn(-9, 9).toDouble())
                matchedAny = true
            }
            parseShift(line, ShiftAxis.BLUE)?.let {
                set("wbShiftBlue", it.coerceIn(-9, 9).toDouble())
                matchedAny = true
            }
        }

        // 7–8. Highlights and shadows. Half steps survive: the widest field set has them.
        if (lower.contains("highlight")) {
            parseNumber(line)?.let {
                set("highlightTone", it.coerceIn(-2.0, 4.0))
                matchedAny = true
            }
        }
        if (lower.contains("shadow")) {
            parseNumber(line)?.let {
                set("shadowTone", it.coerceIn(-2.0, 4.0))
                matchedAny = true
            }
        }

        // 9. Colour — but not colour chrome, colour temperature, or monochromatic colour
        if ((lower.contains("color") || lower.contains("colour")) &&
            !lower.contains("chrome") &&
            !lower.contains("temp") &&
            !lower.contains("mono")
        ) {
            parseNumber(line)?.let {
                set("color", Math.round(it.coerceIn(-4.0, 4.0)).toDouble())
                matchedAny = true
            }
        }

        // 10. Sharpness
        if (lower.contains("sharpness")) {
            parseNumber(line)?.let {
                set("sharpness", Math.round(it.coerceIn(-4.0, 4.0)).toDouble())
                matchedAny = true
            }
        }

        // 11. High ISO NR
        if (lower.contains("high iso nr") ||
            lower.contains("noise reduction") ||
            Regex("""\bnr\b""").containsMatchIn(lower)
        ) {
            parseNumber(line)?.let {
                set("highIsoNR", Math.round(it.coerceIn(-4.0, 4.0)).toDouble())
                matchedAny = true
            }
        }

        // 12. Clarity
        if (lower.contains("clarity")) {
            parseNumber(line)?.let {
                set("clarity", Math.round(it.coerceIn(-5.0, 5.0)).toDouble())
                matchedAny = true
            }
        }

        // 13. Monochromatic colour, on both axes
        if (lower.contains("monochromatic") ||
            lower.contains("monochrome color") ||
            Regex("""\bwc\b""").containsMatchIn(lower) ||
            Regex("""\bmg\b""").containsMatchIn(lower)
        ) {
            parseShift(line, ShiftAxis.WC)?.let {
                set("monochromaticColorWc", it.coerceIn(-18, 18).toDouble())
                matchedAny = true
            }
            parseShift(line, ShiftAxis.MG)?.let {
                set("monochromaticColorMg", it.coerceIn(-18, 18).toDouble())
                matchedAny = true
            }
        }

        // 14. D range priority
        if (lower.contains("d range priority") || lower.contains("dr priority")) {
            (if (lower.contains("auto")) "auto" else parseOffWeakStrong(lower))?.let {
                set("dRangePriority", it)
                matchedAny = true
            }
        }

        // 15. Exposure compensation
        if (lower.contains("exposure") || lower.contains("exp comp")) {
            parseExposureComp(line)?.let {
                set("exposureCompensation", it)
                matchedAny = true
            }
        }

        // 16. ISO bounds
        if (lower.contains("iso")) {
            val (min, max) = parseIsoLimits(line)
            min?.let {
                set("isoMin", it.toDouble())
                matchedAny = true
            }
            max?.let {
                set("isoMax", it.toDouble())
                matchedAny = true
            }
        }

        // 17. Sensor generation — read only to know this line is not the title
        if (lower.contains("sensor") || lower.contains("x-trans") || lower.contains("generation")) {
            if (namesASensor(lower)) matchedAny = true
        }

        // A first line that named no setting is the recipe's title, which is how every
        // pasted recipe on Fuji X Weekly opens.
        if (isFirstUnmatchedLine) {
            isFirstUnmatchedLine = false
            if (!matchedAny && name == null && line.length < 80) name = line
        }
    }

    return ParsedRecipeText(
        name = name,
        settings = JsonObject(settings),
        fieldsFound = settings.size,
    )
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

private val LEADING_BULLET = Regex("""^[•\-*\d+.:>–—]+\s*""")
private val EXPLICIT_NAME = Regex("""^(?:name|title|recipe)\s*[:=-]\s*(.+)$""", RegexOption.IGNORE_CASE)
private val FIRST_NUMBER = Regex("""([+-]?\d+(?:\.\d+)?)""")

private fun parseNumber(line: String): Double? =
    FIRST_NUMBER.find(line)?.groupValues?.get(1)?.toDoubleOrNull()

private fun parseOffWeakStrong(lower: String): String? = when {
    lower.contains("off") -> "off"
    lower.contains("weak") -> "weak"
    lower.contains("strong") -> "strong"
    else -> null
}

private fun parseDynamicRange(lower: String): String? = when {
    lower.contains("400") -> "dr400"
    lower.contains("200") -> "dr200"
    lower.contains("100") -> "dr100"
    lower.contains("auto") -> "dr-auto"
    else -> null
}

private fun parseFilmSimulation(lower: String): String? = when {
    lower.contains("classic negative") || lower.contains("classic neg") -> "classic-negative"
    lower.contains("nostalgic negative") || lower.contains("nostalgic neg") -> "nostalgic-negative"
    lower.contains("reala ace") || lower.contains("reala-ace") -> "reala-ace"
    lower.contains("bleach bypass") || lower.contains("eterna bleach") -> "eterna-bleach-bypass"
    lower.contains("eterna") -> "eterna"
    lower.contains("classic chrome") -> "classic-chrome"
    lower.contains("pro neg. hi") || lower.contains("pro neg hi") || lower.contains("pro-neg-hi") -> "pro-neg-hi"
    lower.contains("pro neg. std") || lower.contains("pro neg std") || lower.contains("pro-neg-std") -> "pro-neg-std"
    lower.contains("provia") -> "provia"
    lower.contains("velvia") -> "velvia"
    lower.contains("astia") -> "astia"
    lower.contains("acros") -> "acros" + filterSuffix(lower)
    lower.contains("monochrome") || lower.contains("black & white") || lower.contains("b&w") ->
        "monochrome" + filterSuffix(lower)
    lower.contains("sepia") -> "sepia"
    else -> null
}

/** The contrast filter a black-and-white simulation was shot through, if it names one. */
private fun filterSuffix(lower: String): String = when {
    lower.contains("ye") || lower.contains("yellow") -> "-ye"
    lower.contains("+ r") || lower.contains("+r") || lower.contains("red") -> "-r"
    lower.contains("+ g") || lower.contains("+g") || lower.contains("green") -> "-g"
    else -> ""
}

private fun parseWhiteBalanceMode(lower: String): String? = when {
    lower.contains("white priority") -> "auto-white-priority"
    lower.contains("ambience priority") -> "auto-ambience-priority"
    lower.contains("auto") -> "auto"
    lower.contains("daylight") -> "daylight"
    lower.contains("shade") -> "shade"
    lower.contains("fluorescent 1") || lower.contains("fluorescent1") -> "fluorescent-1"
    lower.contains("fluorescent 2") || lower.contains("fluorescent2") -> "fluorescent-2"
    lower.contains("fluorescent 3") || lower.contains("fluorescent3") -> "fluorescent-3"
    lower.contains("incandescent") -> "incandescent"
    lower.contains("underwater") -> "underwater"
    lower.contains("custom 1") || lower.contains("custom1") -> "custom-1"
    lower.contains("custom 2") || lower.contains("custom2") -> "custom-2"
    lower.contains("custom 3") || lower.contains("custom3") -> "custom-3"
    Regex("""\b\d{4}\s*k\b""").containsMatchIn(lower) ||
        lower.contains("color temp") || lower.contains("colour temp") -> "color-temp"
    else -> null
}

private fun parseColorTemperature(line: String): Int? {
    val kelvin = Regex("""(\d{4})\s*k?""", RegexOption.IGNORE_CASE)
        .find(line)?.groupValues?.get(1)?.toIntOrNull() ?: return null
    if (kelvin !in 2500..10000) return null
    return Math.round(kelvin / 100.0).toInt() * 100
}

private enum class ShiftAxis(val label: String) {
    /** How each axis is written. Fuji X Weekly spells the words out; menus use initials. */
    RED("R(?:ed)?"),
    BLUE("B(?:lue)?"),
    WC("WC"),
    MG("MG"),
}

/**
 * One axis of a shift, written either way round: `R -5` and `-5 Red` are the same shift, and
 * both are in circulation.
 *
 * **The sign is the half that has to survive**, which is why the number-first pattern cannot
 * open with `\b`: between the space and the `-` of ` -5 Red` there is no word boundary, so
 * the engine would match from the digit and read `-5 Red` as `5` — not a value that is merely
 * wrong, but a shift in the opposite direction. What that `\b` was for is keeping `500` out
 * of `1500`, and a character that cannot be part of a number does that job without standing
 * between the pattern and the sign.
 */
private fun parseShift(line: String, axis: ShiftAxis): Int? {
    val labelFirst = Regex("""\b${axis.label}\s*[:=]?\s*([+-]?\d+)\b""", RegexOption.IGNORE_CASE)
    val numberFirst = Regex("""(?:^|[^\w.])([+-]?\d+)\s*${axis.label}\b""", RegexOption.IGNORE_CASE)

    val match = labelFirst.find(line) ?: numberFirst.find(line) ?: return null
    return match.groupValues[1].toIntOrNull()
}

/**
 * The one deliberate deviation from the source: the negative thirds are tested **first**.
 * The web client tests `1/3` before `-1/3`, and `"-1/3"` contains `"1/3"`, so its negative
 * branches can never be reached — `-1/3 EV` arrives there as `+0.333`. Porting that would be
 * porting a typo.
 */
private fun parseExposureComp(line: String): Double? = when {
    line.contains("-1/3") -> -0.333
    line.contains("-2/3") -> -0.667
    line.contains("1/3") -> 0.333
    line.contains("2/3") -> 0.667
    else -> parseNumber(line)
}

private fun parseIsoLimits(line: String): Pair<Int?, Int?> {
    val numbers = Regex("""\b(\d{3,5})\b""").findAll(line)
        .mapNotNull { it.groupValues[1].toIntOrNull() }
        .toList()

    return when {
        numbers.isEmpty() -> null to null
        numbers.size == 1 -> {
            val lower = line.lowercase()
            if (lower.contains("max") || lower.contains("up to")) {
                null to numbers[0]
            } else {
                numbers[0] to null
            }
        }
        else -> numbers.min() to numbers.max()
    }
}

private fun namesASensor(lower: String): Boolean =
    listOf("xtrans-v", "x-trans v", "x-trans 5", "xtrans-iv", "x-trans iv", "x-trans 4",
        "xtrans-iii", "x-trans iii", "x-trans 3", "gfx", "cmos", "bayer")
        .any { lower.contains(it) }
