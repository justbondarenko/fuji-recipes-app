package dev.bondarenko.fujirecipes.data.fields

/**
 * Every recipe parameter — FEAT-002 T-01.
 *
 * **Transcribed, not invented.**
 *
 * | | |
 * |---|---|
 * | Source document | `fuji-recipes-book/specs/shared/field-definitions.md` §2, §4, version 1 |
 * | Sibling transcription | `fuji-recipes-book/shared/recipe-fields.ts` (`RECIPE_FIELDS`) |
 * | Read at commit | `b802b1dcebde6a4ee204311095b5b893bfed8997` |
 *
 * Two transcriptions of one document, never a transcription of a transcription
 * (`steering/architecture.md` §2). This is the single source the view, the form
 * (FEAT-003) and the camera write plan (FEAT-005) all read: a label written twice is a bug
 * (`coding-standards.md` P3).
 *
 * No Compose and no Android imports, so the whole table is testable on the JVM.
 */

/** §2, in render order. */
enum class FieldGroup(val id: String, val label: String) {
    IDENTITY("identity", "Identity"),
    SIMULATION("simulation", "Film simulation"),
    TONE("tone", "Tone"),
    EFFECTS("effects", "Effects"),
    WHITE_BALANCE("white-balance", "White balance"),
    MONOCHROMATIC("monochromatic", "Monochromatic colour"),

    /** §4: stored, displayed and exported, but never written to the camera. */
    SHOOTING("shooting", "Recommendations — not written to the camera"),
}

data class EnumOption(val id: String, val label: String)

/**
 * What the applicability predicates get to look at.
 *
 * `grainSize` is the reason this carries `grainEffect` as well as the body: §4's rule for it
 * is "generation **and** `grainEffect != off`", and the second half is a property of the
 * recipe rather than of the camera.
 */
data class FieldContext(
    val generation: SensorGeneration,
    val filmSimulationId: String?,
    val grainEffectOff: Boolean = true,
    val whiteBalanceId: String? = null,
) {
    val isMonochrome: Boolean
        get() = FilmSimulations.byId(filmSimulationId)?.monochrome == true
}

sealed interface RecipeField {
    val id: String
    val group: FieldGroup
    val label: String

    /** §4: never written to the camera; a recommendation for the photographer. */
    val advisory: Boolean

    /** §4's Applicability column. An inapplicable field is **omitted**, never disabled. */
    fun appliesTo(context: FieldContext): Boolean

    /** §4's Default column, for the changed-versus-default distinction (§5). */
    val defaultValue: Any?
}

data class NumberField(
    override val id: String,
    override val group: FieldGroup,
    override val label: String,
    val min: Double,
    val max: Double,
    /** Null where the step depends on the body — see [stepFor]. */
    val step: Double?,
    override val defaultValue: Any?,
    override val advisory: Boolean = false,
    /** True where §4's Type column says `int` rather than `number`. */
    val integral: Boolean = true,
    private val applies: (FieldContext) -> Boolean = { true },
) : RecipeField {
    override fun appliesTo(context: FieldContext) = applies(context)

    /** §4: half steps on the widest field set, whole steps elsewhere. */
    fun stepFor(generation: SensorGeneration): Double =
        step ?: if (generation.behavesAsXTransV) 0.5 else 1.0
}

data class EnumFieldDef(
    override val id: String,
    override val group: FieldGroup,
    override val label: String,
    val options: List<EnumOption>,
    override val defaultValue: Any?,
    override val advisory: Boolean = false,
    private val applies: (FieldContext) -> Boolean = { true },
) : RecipeField {
    override fun appliesTo(context: FieldContext) = applies(context)

    /** The label, or the raw id when a newer client wrote a value this build lacks. */
    fun labelFor(id: String?): String =
        options.firstOrNull { it.id == id }?.label ?: id.orEmpty()
}

/** `xtrans-v` and `gfx` share the widest field set — §1's ordering makes them equal. */
private val SensorGeneration.behavesAsXTransV: Boolean
    get() = this == SensorGeneration.XTRANS_V || this == SensorGeneration.GFX

private fun SensorGeneration.atLeast(other: SensorGeneration): Boolean =
    ordinal >= other.ordinal

object RecipeFields {

    val OFF_WEAK_STRONG = listOf(
        EnumOption("off", "Off"),
        EnumOption("weak", "Weak"),
        EnumOption("strong", "Strong"),
    )

    /** §4, "White balance values". */
    val WHITE_BALANCE_OPTIONS = listOf(
        EnumOption("auto", "Auto"),
        EnumOption("auto-white-priority", "Auto — white priority"),
        EnumOption("auto-ambience-priority", "Auto — ambience priority"),
        EnumOption("daylight", "Daylight"),
        EnumOption("shade", "Shade"),
        EnumOption("fluorescent-1", "Fluorescent 1"),
        EnumOption("fluorescent-2", "Fluorescent 2"),
        EnumOption("fluorescent-3", "Fluorescent 3"),
        EnumOption("incandescent", "Incandescent"),
        EnumOption("underwater", "Underwater"),
        EnumOption("color-temp", "Colour temperature"),
        EnumOption("custom-1", "Custom 1"),
        EnumOption("custom-2", "Custom 2"),
        EnumOption("custom-3", "Custom 3"),
    )

    /**
     * The settings fields, §4 row by row, in render order.
     *
     * `identity` rows (`name`, `notes`, `rating`, `tags`, `sensorGeneration`) are deliberately
     * absent: they live on the recipe itself rather than inside `settings`, and the view
     * renders them in its header. Everything here is a key of `settings`.
     */
    val all: List<RecipeField> = listOf(
        // simulation
        EnumFieldDef(
            id = "filmSimulation",
            group = FieldGroup.SIMULATION,
            label = "Film simulation",
            options = FilmSimulations.all.map { EnumOption(it.id, it.label) },
            defaultValue = "provia",
        ),
        EnumFieldDef(
            id = "dynamicRange",
            group = FieldGroup.SIMULATION,
            label = "Dynamic range",
            options = listOf(
                EnumOption("dr100", "DR100"),
                EnumOption("dr200", "DR200"),
                EnumOption("dr400", "DR400"),
                EnumOption("dr-auto", "Auto"),
            ),
            defaultValue = "dr-auto",
        ),

        // tone
        NumberField(
            id = "highlightTone",
            group = FieldGroup.TONE,
            label = "Highlight tone",
            min = -2.0, max = 4.0,
            // Step depends on the body: 0.5 on X-Trans V, 1 elsewhere.
            step = null,
            defaultValue = 0,
            integral = false,
        ),
        NumberField(
            id = "shadowTone",
            group = FieldGroup.TONE,
            label = "Shadow tone",
            min = -2.0, max = 4.0,
            step = null,
            defaultValue = 0,
            integral = false,
        ),
        NumberField(
            id = "color",
            group = FieldGroup.TONE,
            label = "Color",
            min = -4.0, max = 4.0, step = 1.0,
            defaultValue = 0,
            // **not** monochrome sims.
            applies = { !it.isMonochrome },
        ),
        NumberField(
            id = "sharpness",
            group = FieldGroup.TONE,
            label = "Sharpness",
            min = -4.0, max = 4.0, step = 1.0,
            defaultValue = 0,
        ),
        NumberField(
            id = "highIsoNR",
            group = FieldGroup.TONE,
            label = "High ISO NR",
            min = -4.0, max = 4.0, step = 1.0,
            defaultValue = 0,
        ),
        NumberField(
            id = "clarity",
            group = FieldGroup.TONE,
            label = "Clarity",
            min = -5.0, max = 5.0, step = 1.0,
            defaultValue = 0,
            // gen >= xtrans-iv.
            applies = { it.generation.atLeast(SensorGeneration.XTRANS_IV) },
        ),

        // effects
        EnumFieldDef(
            id = "grainEffect",
            group = FieldGroup.EFFECTS,
            label = "Grain effect",
            options = OFF_WEAK_STRONG,
            defaultValue = "off",
        ),
        EnumFieldDef(
            id = "grainSize",
            group = FieldGroup.EFFECTS,
            label = "Grain size",
            options = listOf(EnumOption("small", "Small"), EnumOption("large", "Large")),
            defaultValue = "small",
            // gen >= xtrans-iv **and** grainEffect != off. The second half is why
            // `FieldContext` carries the recipe's grain value and not only the body.
            applies = {
                it.generation.atLeast(SensorGeneration.XTRANS_IV) && !it.grainEffectOff
            },
        ),
        EnumFieldDef(
            id = "colorChromeEffect",
            group = FieldGroup.EFFECTS,
            label = "Color chrome effect",
            options = OFF_WEAK_STRONG,
            defaultValue = "off",
        ),
        EnumFieldDef(
            id = "colorChromeFxBlue",
            group = FieldGroup.EFFECTS,
            label = "Color chrome FX blue",
            options = OFF_WEAK_STRONG,
            defaultValue = "off",
            applies = { it.generation.atLeast(SensorGeneration.XTRANS_IV) },
        ),

        // white-balance
        EnumFieldDef(
            id = "whiteBalance",
            group = FieldGroup.WHITE_BALANCE,
            label = "White balance",
            options = WHITE_BALANCE_OPTIONS,
            defaultValue = "auto",
        ),
        NumberField(
            id = "colorTemperature",
            group = FieldGroup.WHITE_BALANCE,
            label = "Colour temperature",
            min = 2500.0, max = 10000.0, step = 100.0,
            defaultValue = 5500,
            applies = { it.whiteBalanceId == "color-temp" },
        ),
        NumberField(
            id = "wbShiftRed",
            group = FieldGroup.WHITE_BALANCE,
            label = "WB shift red",
            min = -9.0, max = 9.0, step = 1.0,
            defaultValue = 0,
        ),
        NumberField(
            id = "wbShiftBlue",
            group = FieldGroup.WHITE_BALANCE,
            label = "WB shift blue",
            min = -9.0, max = 9.0, step = 1.0,
            defaultValue = 0,
        ),

        // monochromatic
        NumberField(
            id = "monochromaticColorWc",
            group = FieldGroup.MONOCHROMATIC,
            label = "Warm / cool",
            min = -18.0, max = 18.0, step = 1.0,
            defaultValue = 0,
            // gen == xtrans-v **and** monochrome sim.
            applies = { it.generation.behavesAsXTransV && it.isMonochrome },
        ),
        NumberField(
            id = "monochromaticColorMg",
            group = FieldGroup.MONOCHROMATIC,
            label = "Magenta / green",
            min = -18.0, max = 18.0, step = 1.0,
            defaultValue = 0,
            applies = { it.generation.behavesAsXTransV && it.isMonochrome },
        ),

        // shooting — advisory, never written to the camera
        EnumFieldDef(
            id = "dRangePriority",
            group = FieldGroup.SHOOTING,
            label = "D range priority",
            options = OFF_WEAK_STRONG + EnumOption("auto", "Auto"),
            defaultValue = "off",
            advisory = true,
            applies = { it.generation.atLeast(SensorGeneration.XTRANS_IV) },
        ),
        NumberField(
            id = "exposureCompensation",
            group = FieldGroup.SHOOTING,
            label = "Exposure compensation",
            min = -3.0, max = 3.0, step = 0.333,
            defaultValue = 0,
            advisory = true,
            integral = false,
        ),
        NumberField(
            id = "isoMin",
            group = FieldGroup.SHOOTING,
            label = "Minimum ISO",
            min = 64.0, max = 12800.0, step = 1.0,
            // Null, not zero: §5 says an unset advisory field omits its row entirely.
            defaultValue = null,
            advisory = true,
        ),
        NumberField(
            id = "isoMax",
            group = FieldGroup.SHOOTING,
            label = "Maximum ISO",
            min = 64.0, max = 51200.0, step = 1.0,
            defaultValue = null,
            advisory = true,
        ),
    )

    private val byId = all.associateBy { it.id }

    fun byId(id: String): RecipeField? = byId[id]

    /**
     * The fields that apply, in group order.
     *
     * §4: a field that is not applicable is **omitted from the UI** — never shown disabled,
     * never shown as "N/A". A monochrome recipe has no colour row at all.
     */
    fun applicable(context: FieldContext): List<RecipeField> =
        all.filter { it.appliesTo(context) }

    /**
     * The generation to reason about, given a recipe that may not name one.
     *
     * **D1 migration 0002 dropped `sensorGeneration` as a column**, so anything written since
     * carries none, and older rows keep theirs only inside `extra`. `xtrans-v` is the
     * fallback because it is the widest field set and the body this tool is actually
     * verified against (`field-definitions.md` §1) — assuming narrower would hide fields a
     * recipe legitimately uses.
     */
    fun generationOf(storedId: String?): SensorGeneration =
        SensorGeneration.fromId(storedId) ?: SensorGeneration.XTRANS_V
}
