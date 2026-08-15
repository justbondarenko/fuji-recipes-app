package dev.bondarenko.fujirecipes.camera.plan

import dev.bondarenko.fujirecipes.data.fields.SensorGeneration
import kotlin.math.roundToInt

/**
 * The protocol constants, transcribed.
 *
 * **Transcribed from** `fuji-recipes-book/camera/encoding.ts` at commit `0c17106`
 * (`coding-standards.md` P3), which in turn transcribes `field-definitions.md` §7 and
 * WR-08. That document records its own provenance: `eggricesoy/filmkit` @ `9e3bbcf`, from
 * Wireshark captures of Fujifilm's X RAW Studio cross-referenced against seven presets on an
 * **X100VI in March 2026**.
 *
 * **The trap this file exists to avoid.** Three encodings differ from the ones the
 * RAW-conversion profile at `0xD185` uses for the same values, and mixing them is the
 * likeliest way to write a recipe that looks right and is wrong:
 *
 * - **effects are 1-indexed** — Off is `1`, Weak `2`, Strong `3` (the profile uses 0/2/3);
 * - **dynamic range is a raw percentage** — 100, 200, 400 (the profile uses a 1/2/3 enum);
 * - **grain is one flat enum** covering strength *and* size together.
 *
 * Tone parameters are ×10, as in the profile: `+1.5` is `15`.
 *
 * Pure: no `android.*` (P4), enforced by `CameraPurityTest`.
 */

// ─── Property codes ─────────────────────────────────────────────────────────

/**
 * The two properties that are not fields: the slot selector and the name.
 *
 * [PRESET_SLOT_PROPERTY] takes 1–7 and switches which custom slot the following writes land
 * in; [PRESET_NAME_PROPERTY] is a PTP string. Both are written before any setting, and a
 * failure on either is fatal to the whole write — a name written into the wrong slot is worse
 * than no write at all.
 */
const val PRESET_SLOT_PROPERTY = 0xd18c
const val PRESET_NAME_PROPERTY = 0xd18d

/** The slots the slot selector accepts — C1 to C7. */
const val FIRST_SLOT = 1
const val LAST_SLOT = 7

/**
 * How a value is packed into the property's two bytes.
 *
 * - [U16] / [I16] — the number as it stands, unsigned or signed.
 * - [TONE10] — the value times ten, signed: `+1.5` is `15`. Every tone parameter.
 * - [LOOKUP] — a table, because the encoding is not arithmetic (WR-08's `highIsoNR`, and
 *   every enum).
 */
enum class Packing { U16, I16, TONE10, LOOKUP }

data class PropertyMapping(
    /** The device property code, or `null` when no code is known for this field. */
    val code: Int?,
    val packing: Packing,
    /** Why a field has no code, in words a dropped-field notice can show (P5). */
    val unwritableBecause: String? = null,
)

/**
 * Every field in `RecipeFields`, mapped to the property that carries it.
 *
 * Keyed by **our** field id rather than by property code, so a field added to the source
 * without a code here fails a test rather than being silently skipped. `CameraEncodingTest`
 * iterates the field source and asserts every id appears.
 */
val FIELD_PROPERTIES: Map<String, PropertyMapping> = mapOf(
    // ── simulation ──
    "filmSimulation" to PropertyMapping(0xd192, Packing.LOOKUP),
    // Raw percentage — 100, 200, 400 — not the profile format's 1/2/3 enum.
    "dynamicRange" to PropertyMapping(0xd190, Packing.LOOKUP),
    // No code found. The preset property block 0xD18E–0xD1A5 is fully accounted for; the two
    // still-unidentified properties (0xD191, always 0, and 0xD1A5, always 7) behave like
    // neither. D-Range Priority is a *shooting* mode on these bodies, not part of a slot.
    "dRangePriority" to PropertyMapping(
        code = null,
        packing = Packing.LOOKUP,
        unwritableBecause = "no custom-slot property carries D-Range Priority; it is a " +
            "shooting mode on the camera, set outside the slot",
    ),

    // ── tone ──
    "highlightTone" to PropertyMapping(0xd19d, Packing.TONE10),
    "shadowTone" to PropertyMapping(0xd19e, Packing.TONE10),
    // Skipped entirely for a monochrome simulation — WR-03, and the camera rejects it.
    "color" to PropertyMapping(0xd19f, Packing.TONE10),
    "sharpness" to PropertyMapping(0xd1a0, Packing.TONE10),
    // WR-08. The one field whose encoding is neither linear nor monotonic.
    "highIsoNR" to PropertyMapping(0xd1a1, Packing.LOOKUP),
    "clarity" to PropertyMapping(0xd1a2, Packing.TONE10),

    // ── effects ──
    // Strength and size share one property, as a flat five-value enum. The field source keeps
    // them as two fields because that is how the camera menu presents them, so the encoder
    // combines the pair — see [grainCode].
    "grainEffect" to PropertyMapping(0xd195, Packing.LOOKUP),
    "grainSize" to PropertyMapping(0xd195, Packing.LOOKUP),
    "colorChromeEffect" to PropertyMapping(0xd196, Packing.LOOKUP),
    "colorChromeFxBlue" to PropertyMapping(0xd197, Packing.LOOKUP),

    // ── white balance ──
    "whiteBalance" to PropertyMapping(0xd199, Packing.LOOKUP),
    // Written immediately after the mode, and only when the mode asks for it (WR-02).
    "colorTemperature" to PropertyMapping(0xd19c, Packing.U16),
    "wbShiftRed" to PropertyMapping(0xd19a, Packing.I16),
    "wbShiftBlue" to PropertyMapping(0xd19b, Packing.I16),

    // ── monochromatic ──
    // ×10 like the tones, and the camera **rejects a written zero** — a detail WR-05 does not
    // mention: zero means "leave it alone", so the property is omitted rather than set to 0.
    "monochromaticColorWc" to PropertyMapping(0xd193, Packing.TONE10),
    "monochromaticColorMg" to PropertyMapping(0xd194, Packing.TONE10),

    // ── shooting: advisory, never written (WR-06) ──
    "exposureCompensation" to PropertyMapping(
        code = null,
        packing = Packing.TONE10,
        unwritableBecause = "advisory only: a custom slot does not store exposure " +
            "compensation (WR-06)",
    ),
    "isoMin" to PropertyMapping(
        code = null,
        packing = Packing.U16,
        unwritableBecause = "advisory only: a custom slot does not store an ISO range (WR-06)",
    ),
    "isoMax" to PropertyMapping(
        code = null,
        packing = Packing.U16,
        unwritableBecause = "advisory only: a custom slot does not store an ISO range (WR-06)",
    ),
)

/**
 * The order the properties are written in.
 *
 * Not cosmetic, and not the order `PRD.md` §8.3 originally listed — that was written before
 * anyone had seen the protocol, and the captures contradicted it. Two properties of this
 * order are load-bearing: **simulation first**, so a mode-dependent rejection surfaces
 * immediately rather than after twelve successful writes, and **`colorTemperature`
 * immediately after `whiteBalance`**, which WR-02 requires and the captures confirm. A test
 * pins the adjacency.
 */
val WRITE_ORDER: List<String> = listOf(
    "filmSimulation",
    "dynamicRange",
    "monochromaticColorWc",
    "monochromaticColorMg",
    "grainEffect",
    "colorChromeEffect",
    "colorChromeFxBlue",
    "whiteBalance",
    "colorTemperature",
    "wbShiftRed",
    "wbShiftBlue",
    "highlightTone",
    "shadowTone",
    "color",
    "sharpness",
    "highIsoNR",
    "clarity",
)

// ─── WR-08: the noise-reduction lookup ──────────────────────────────────────

/**
 * `highIsoNR` −4..+4 to the value the camera wants.
 *
 * Read it twice. It is not monotonic — +2 encodes as `0x0000`, which is lower than −4's
 * `0x8000` and lower than +1's `0x1000` — and it is not arithmetic in any form. This is
 * exactly why WR-08 says "map through a lookup table, never arithmetic", and why this is the
 * whole documented range rather than a formula with special cases.
 */
val HIGH_ISO_NR_CODES: Map<Int, Int> = mapOf(
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

// ─── Enum encodings ─────────────────────────────────────────────────────────

/**
 * Film simulations, by our id. All twenty documented simulations have a code.
 *
 * The values come from `rawji` via filmkit, noted there as "may need adjustment for
 * X-Processor 5".
 */
val FILM_SIMULATION_CODES: Map<String, Int> = mapOf(
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

/**
 * Dynamic range as a raw percentage.
 *
 * `dr-auto` has no observed code — the captures only ever show 100, 200 and 400 — so a recipe
 * set to Auto cannot be written and says so rather than being sent as DR100.
 */
val DYNAMIC_RANGE_CODES: Map<String, Int?> = mapOf(
    "dr100" to 100,
    "dr200" to 200,
    "dr400" to 400,
    "dr-auto" to null,
)

/**
 * Effects are **1-indexed** in the preset properties: Off is 1, not 0.
 *
 * The profile format uses 0/2/3 for the same three states, which is the trap this file's
 * header warns about.
 */
val EFFECT_CODES: Map<String, Int> = mapOf(
    "off" to 1,
    "weak" to 2,
    "strong" to 3,
)

/**
 * Grain, as the single flat enum the property takes.
 *
 * Keyed `"<effect>/<size>"`. `off` ignores the size, which is why WR-04 skips `grainSize`
 * when the effect is off — there is nothing to put it in.
 */
val GRAIN_CODES: Map<String, Int> = mapOf(
    "off/small" to 1,
    "off/large" to 1,
    "weak/small" to 2,
    "strong/small" to 3,
    "weak/large" to 4,
    "strong/large" to 5,
)

/**
 * White balance, by our id. Four of the fourteen modes have no observed code.
 *
 * The three custom slots and Auto's white-priority variant never appeared in the captures.
 * Guessing `0x8022` for white priority because ambience is `0x8021` is precisely the kind of
 * inference this table forbids, so they are `null` and their recipes report the mode as
 * undeliverable.
 */
val WHITE_BALANCE_CODES: Map<String, Int?> = mapOf(
    "auto" to 0x0002,
    "auto-white-priority" to null,
    "auto-ambience-priority" to 0x8021,
    "daylight" to 0x0004,
    "shade" to 0x8006,
    "fluorescent-1" to 0x8001,
    "fluorescent-2" to 0x8002,
    "fluorescent-3" to 0x8003,
    "incandescent" to 0x0006,
    "underwater" to 0x0008,
    "color-temp" to 0x8007,
    "custom-1" to null,
    "custom-2" to null,
    "custom-3" to null,
)

/** The white-balance mode that makes `colorTemperature` writable (WR-02). */
const val COLOR_TEMP_MODE = "color-temp"

// ─── Lookups ────────────────────────────────────────────────────────────────

/**
 * The encoded value for a field's value, or `null` when this build cannot encode it.
 *
 * `null` is a first-class answer, not a failure: the write plan turns it into a dropped field
 * with a reason, which is the behaviour that lets a recipe with one unencodable value still
 * reach the camera with the rest intact.
 */
fun encodeValue(fieldId: String, value: Any?, grainSizeValue: Any? = null): Int? {
    val mapping = FIELD_PROPERTIES[fieldId] ?: return null
    if (mapping.code == null) return null

    return when (mapping.packing) {
        Packing.TONE10 -> (value as? Number)?.let { (it.toDouble() * 10).roundToInt() }
        Packing.U16, Packing.I16 -> (value as? Number)?.toDouble()?.roundToInt()
        Packing.LOOKUP -> lookup(fieldId, value, grainSizeValue)
    }
}

private fun lookup(fieldId: String, value: Any?, grainSizeValue: Any?): Int? = when (fieldId) {
    "highIsoNR" -> (value as? Number)?.let { HIGH_ISO_NR_CODES[it.toDouble().roundToInt()] }
    "filmSimulation" -> FILM_SIMULATION_CODES[value as? String]
    "dynamicRange" -> DYNAMIC_RANGE_CODES[value?.toString()]

    // Either field resolves the same property, so both need the pair.
    "grainEffect" -> grainCode(value, grainSizeValue)
    "grainSize" -> grainCode(grainSizeValue, value)

    "colorChromeEffect", "colorChromeFxBlue" -> EFFECT_CODES[value?.toString()]
    "whiteBalance" -> WHITE_BALANCE_CODES[value?.toString()]
    else -> null
}

// ─── Decoding ───────────────────────────────────────────────────────────────

/**
 * The inverse of [encodeValue]: what a value read off the camera means.
 *
 * Transcribed from `encoding.ts`'s decoding half, which this project left out of FEAT-006
 * because nothing read a value then. FEAT-007 imports whole slots, so it does now.
 *
 * The reference added it because a real body made it necessary: an X-T50 refuses
 * `GetDevicePropDesc` and answers `GetDevicePropValue`, so the only evidence it offers about
 * the encodings is the values it currently holds — and a table of raw numbers cannot be
 * checked by a person. Decoded, `0xD192 = 19` reads "Nostalgic Negative" in a slot named
 * "1970s Summer", which is a fact anyone can confirm against the camera's own menu.
 *
 * `null` means the code is not one this build can name — a finding, not a bug. The importer
 * omits the field rather than guessing, so one unknown value costs one field and not the slot.
 */
fun decodeValue(fieldId: String, code: Int): Any? {
    val mapping = FIELD_PROPERTIES[fieldId] ?: return null
    if (mapping.code == null) return null

    return when (mapping.packing) {
        // Division, not multiplication by 0.1: `-5 / 10.0` is -0.5 exactly, while `-5 * 0.1`
        // differs in the last bit — and half steps have to compare equal for the duplicate
        // detection to work at all.
        Packing.TONE10 -> code / 10.0
        Packing.U16, Packing.I16 -> code
        Packing.LOOKUP -> decodeLookup(fieldId, code)
    }
}

private fun decodeLookup(fieldId: String, code: Int): Any? = when (fieldId) {
    "highIsoNR" -> HIGH_ISO_NR_CODES.entries.firstOrNull { it.value == code }?.key

    "grainEffect", "grainSize" -> {
        val pair = GRAIN_CODES.entries.firstOrNull { it.value == code }?.key
        val effect = pair?.substringBefore('/')
        when {
            pair == null -> null
            // Off ignores the size, so there is no size to report for it.
            effect == "off" -> if (fieldId == "grainEffect") "off" else null
            fieldId == "grainEffect" -> effect
            else -> pair.substringAfter('/')
        }
    }

    "filmSimulation" -> FILM_SIMULATION_CODES.entries.firstOrNull { it.value == code }?.key
    "dynamicRange" -> DYNAMIC_RANGE_CODES.entries.firstOrNull { it.value == code }?.key
    "whiteBalance" -> WHITE_BALANCE_CODES.entries.firstOrNull { it.value == code }?.key
    "colorChromeEffect", "colorChromeFxBlue" ->
        EFFECT_CODES.entries.firstOrNull { it.value == code }?.key

    else -> null
}

/** The single value the grain property takes, from the two fields the menu presents. */
fun grainCode(effect: Any?, size: Any?): Int? {
    val effectId = effect?.toString() ?: return null
    // Off ignores the size, so a recipe that carries no size is still writable.
    val sizeId = size?.toString() ?: if (effectId == "off") "small" else return null
    return GRAIN_CODES["$effectId/$sizeId"]
}

// ─── The GFX question ───────────────────────────────────────────────────────

/**
 * Whether the property codes above have been observed on this generation.
 *
 * X-Trans V is the generation the captures were taken on. X-Trans IV shares the property
 * block per the reference, and GFX is `field-definitions.md`'s **unverified** assumption —
 * the write is still offered, because refusing it would be a guess in the other direction,
 * but the sheet says so.
 */
fun propertyCodesVerifiedFor(generation: SensorGeneration): Boolean =
    generation == SensorGeneration.XTRANS_V || generation == SensorGeneration.XTRANS_IV

const val GFX_ASSUMPTION =
    "The property codes were captured on an X-Trans V body. GFX support is assumed from the " +
        "field definitions and has not been verified on a GFX camera — check the slot after " +
        "writing."
