package dev.bondarenko.fujirecipes.camera.plan

import dev.bondarenko.fujirecipes.data.fields.EnumFieldDef
import dev.bondarenko.fujirecipes.data.fields.FieldContext
import dev.bondarenko.fujirecipes.data.fields.FilmSimulations
import dev.bondarenko.fujirecipes.data.fields.RecipeFields
import dev.bondarenko.fujirecipes.data.fields.SensorGeneration
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull

/**
 * The write plan — `field-definitions.md` §6, WR-01 to WR-10.
 *
 * **Transcribed from** `fuji-recipes-book/camera/write-plan.ts` at commit `0c17106`
 * (`coding-standards.md` P3).
 *
 * Everything that decides *what* goes to the camera, and nothing that puts it there. The plan
 * is a list of steps and a list of fields that will not make it, computed from a recipe and
 * the connected body — and that is all it is. `camera/usb/WriteExecutor.kt` executes it.
 *
 * **Pure, and `CameraPurityTest` enforces it** (WR-09, `coding-standards.md` P4). That is not
 * tidiness. A plan that could read the camera would be impossible to test exhaustively, and
 * exhaustively is the only acceptable standard for the last piece of logic before bytes
 * change a slot.
 *
 * **The rules are not re-implemented here.** WR-03 (monochrome rejects colour) and WR-05 (the
 * monochromatic pair needs X-Trans V *and* a monochrome simulation) are already the
 * applicability predicates of those fields in `RecipeFields`, so this file asks the field
 * source rather than branching on the same conditions a second time — P3, and the reason the
 * two cannot drift apart. What is here is what the field source cannot know: which property
 * carries a field, in what order, and what the camera refuses to be told.
 */

/** How a step's value is packed. [STRING] is the preset name and nothing else. */
enum class StepKind { U16, I16, STRING }

data class WriteStep(
    /** A field id, or `slot` / `name` for the two steps that are not fields. */
    val id: String,
    /** What to call this step while it is happening. */
    val label: String,
    val property: Int,
    val kind: StepKind,
    /** An [Int] for a numeric step, a [String] for the name. */
    val value: Any,
    /**
     * Whether failing this step abandons the whole write.
     *
     * The slot selector and the name are fatal; a refused setting is a warning. A name written
     * into the wrong slot is worse than no write at all, while one refused property costs one
     * setting.
     */
    val fatal: Boolean,
)

data class DroppedField(
    val id: String,
    val label: String,
    /** Why it will not be written, in words the sheet can show (P5). */
    val reason: String,
)

data class WritePlan(
    val slot: Int,
    val steps: List<WriteStep> = emptyList(),
    val dropped: List<DroppedField> = emptyList(),
    /** Advisories that change nothing about the write but the user should see. */
    val notes: List<String> = emptyList(),
    /** Non-null means write nothing at all (WR-01). */
    val refusal: String? = null,
    /** The recipe was made for a wider body than this camera (`PRD.md` §8.3's warning). */
    val incompatible: Boolean = false,
) {
    /** So a progress display divides by a number the plan agrees with. */
    val total: Int get() = steps.size
}

/**
 * How many characters of a recipe name reach the camera.
 *
 * **Unverified**, and deliberately conservative. No source states the limit for a custom-slot
 * name, and the name write is fatal — so a name the camera refuses would abandon the whole
 * write for a cosmetic reason. Truncating cannot produce a *wrong* setting the way a guessed
 * property code could; the only cost of being too careful is a shortened label, and it is
 * reported rather than done silently.
 */
const val PRESET_NAME_MAX_CHARS = 32

fun buildWritePlan(
    slot: Int,
    generation: SensorGeneration,
    recipeName: String,
    settings: JsonObject,
    /** The generation the recipe was written for, when the row still records one. */
    recipeGeneration: SensorGeneration? = null,
): WritePlan {
    // WR-01, first and unconditionally: a body without slot registers is not partially
    // written.
    if (!generation.hasCustomSlots) {
        return WritePlan(
            slot = slot,
            refusal = "${generation.label} has no custom slots, so a recipe cannot be " +
                "written to it.",
        )
    }

    if (slot !in FIRST_SLOT..LAST_SLOT) {
        return WritePlan(
            slot = slot,
            refusal = "A recipe can only be written to C$FIRST_SLOT through C$LAST_SLOT.",
        )
    }

    val simulationId = settings.value("filmSimulation") as? String ?: "provia"
    val context = FieldContext(
        generation = generation,
        filmSimulationId = simulationId,
        grainEffectOff = settings.value("grainEffect")?.toString().let { it == null || it == "off" },
        whiteBalanceId = settings.value("whiteBalance") as? String,
    )

    val notes = mutableListOf<String>()
    val name = presetName(recipeName, notes)

    val steps = mutableListOf(
        WriteStep(
            id = "slot",
            label = "Slot C$slot",
            property = PRESET_SLOT_PROPERTY,
            kind = StepKind.U16,
            value = slot,
            fatal = true,
        ),
        WriteStep(
            id = "name",
            label = "Preset name",
            property = PRESET_NAME_PROPERTY,
            kind = StepKind.STRING,
            value = name,
            fatal = true,
        ),
    )

    val dropped = mutableListOf<DroppedField>()

    WRITE_ORDER.forEach { id ->
        val outcome = planField(id, settings, context)
        outcome.step?.let { steps += it }
        outcome.dropped?.let { dropped += it }
    }

    /**
     * A field with no property code, reported **only if it is not advisory**.
     *
     * WR-06's four are recommendations: no camera stores them, and the recipe already shows
     * them under a group whose label says they are not written. Reporting them here put a
     * warning in front of every write about something that was never going to be written —
     * on every recipe, for ever, which is how a warning stops being read.
     *
     * The loop stays for the case it was written for: a field that has no code and is *not*
     * advisory would be a genuine gap, and silence about it would be worse than noise.
     */
    FIELD_PROPERTIES.forEach { (id, mapping) ->
        if (mapping.code != null) return@forEach
        val field = RecipeFields.byId(id) ?: return@forEach
        if (field.advisory) return@forEach
        if (!settings.carries(id)) return@forEach

        dropped += DroppedField(
            id = id,
            label = field.label,
            reason = mapping.unwritableBecause ?: "this app has no property code for it",
        )
    }

    if (!propertyCodesVerifiedFor(generation)) notes += GFX_ASSUMPTION

    return WritePlan(
        slot = slot,
        steps = steps,
        dropped = dropped,
        notes = notes,
        refusal = null,
        incompatible = if (recipeGeneration != null) {
            recipeGeneration.ordinal > generation.ordinal
        } else {
            dropped.isNotEmpty()
        },
    )
}

private class FieldOutcome(val step: WriteStep? = null, val dropped: DroppedField? = null)

/**
 * One field's fate.
 *
 * The order of the checks is the order of their causes, and the distinction that matters runs
 * through all of them: **dropped** means the camera cannot be told this, and belongs in front
 * of the user. **Skipped** means there is nothing to tell it — no value, or a value the
 * recipe's own other settings make meaningless — and belongs nowhere, because a list of
 * things that were never going to happen is not information.
 */
private fun planField(
    id: String,
    settings: JsonObject,
    context: FieldContext,
): FieldOutcome {
    val mapping = FIELD_PROPERTIES[id]
    // No code at all: handled once, after the loop, so that an unwritable field is reported
    // whether or not it happens to appear in the write order.
    if (mapping?.code == null) return FieldOutcome()

    val field = RecipeFields.byId(id) ?: return FieldOutcome()
    if (!settings.carries(id)) return FieldOutcome()

    val value = settings.value(id)

    /**
     * The live-value rules come **first**, and the ordering is the whole point.
     *
     * Neither of these is a drop: the camera would accept both, and it is the recipe's own
     * other settings that make them meaningless. Run after the predicate they would be
     * reported as dropped — "X-Trans V does not support colour temperature" on every recipe
     * not using a colour-temperature white balance, which is both false and noise.
     *
     * This is where the Kotlin field source differs from the sibling's: `FieldContext` carries
     * `whiteBalanceId` and `grainEffectOff`, so its predicates already fold in rules the
     * TypeScript ones cannot see. That makes the split between *dropped* and *skipped* this
     * function's job rather than the predicate's.
     */
    if (id == "colorTemperature" && settings.value("whiteBalance") != COLOR_TEMP_MODE) {
        return FieldOutcome()
    }
    // WR-05's unwritten half: the camera **rejects a written zero** for the monochromatic
    // pair, so zero means "leave it alone" rather than "set it to 0".
    if (isMonochromaticColour(id) && (value as? Number)?.toDouble() == 0.0) return FieldOutcome()

    // WR-07, and with it WR-03 and WR-05: the field source's own predicate, evaluated against
    // the *camera's* generation rather than the recipe's.
    if (!field.appliesTo(context)) {
        return FieldOutcome(
            dropped = DroppedField(id, field.label, applicabilityReason(id, context)),
        )
    }

    /**
     * WR-10 — the simulation itself may not exist on this body.
     *
     * Every simulation has a minimum generation and the *form* filters its options by it, but
     * nothing in the write path did until a check sent a Nostalgic Negative recipe to an
     * X-T4. WR-07 cannot catch it: `filmSimulation`'s predicate is "always", and it is the
     * **value** the body lacks rather than the field.
     *
     * Reported rather than refused, per `PRD.md` §8.3's "write anyway" — but it is the most
     * consequential drop there is, because the slot then keeps whatever simulation it had
     * under this recipe's tones. That is the user's call to make, not the app's to take
     * silently.
     */
    if (id == "filmSimulation") {
        val simulation = FilmSimulations.byId(value as? String)
        if (simulation == null || simulation.minGeneration.ordinal > context.generation.ordinal) {
            return FieldOutcome(
                dropped = DroppedField(
                    id = id,
                    label = field.label,
                    reason = "${context.generation.label} has no " +
                        "${simulation?.label ?: value} simulation, so the slot will keep the " +
                        "simulation it already has",
                ),
            )
        }
    }

    val code = encodeValue(id, value, settings.value("grainSize"))
        ?: return FieldOutcome(
            dropped = DroppedField(
                id = id,
                label = field.label,
                reason = "this build has no code for ${describeValue(field, value)}, so it " +
                    "cannot be sent",
            ),
        )

    return FieldOutcome(
        step = WriteStep(
            id = id,
            label = field.label,
            property = mapping.code,
            kind = wireKind(mapping.packing),
            value = code,
            fatal = false,
        ),
    )
}

/**
 * How a packing goes on the wire — and the one place the reference got this wrong.
 *
 * A lookup field's code is a **bit pattern, not a number**: `highIsoNR` −4 is `0x8000` and the
 * colour-temperature white-balance mode is `0x8007`, both above `i16`'s maximum of 32767. The
 * first version of this mapped everything that was not `u16` to `i16`, and `packI16` *clamps*
 * rather than wrapping — so those two would have gone out as `0x7FFF`: the wrong noise
 * reduction, and the wrong white-balance mode.
 *
 * Signed means "this field's own values can be negative": the tones and the white-balance
 * shifts. Everything else is unsigned, including every lookup.
 */
fun wireKind(packing: Packing): StepKind =
    if (packing == Packing.TONE10 || packing == Packing.I16) StepKind.I16 else StepKind.U16

/**
 * Whether the recipe has anything to say about a field.
 *
 * `null` is how the data model records "not applicable to the body this was written for", and
 * an absent key is how an older file records "this version did not have the field". Both mean
 * there is nothing to write.
 */
private fun JsonObject.carries(id: String): Boolean = value(id) != null

/** A JSON value as the plain Kotlin type the field table talks in. */
private fun JsonObject.value(key: String): Any? {
    val primitive = this[key] as? JsonPrimitive ?: return null
    if (primitive.isString) return primitive.content
    primitive.booleanOrNull?.let { return it }
    primitive.doubleOrNull?.let { return it }
    return null
}

private fun isMonochromaticColour(id: String): Boolean =
    id == "monochromaticColorWc" || id == "monochromaticColorMg"

/**
 * Why a field's predicate failed, in the terms the user is looking at.
 *
 * Two causes, and they must not be confused: the body cannot do it, or the film simulation
 * rules it out.
 */
private fun applicabilityReason(id: String, context: FieldContext): String {
    val label = context.generation.label

    if (id == "color") return "$label rejects a colour value on a monochrome simulation"

    if (isMonochromaticColour(id)) {
        // Both halves of WR-05 can be the cause, so the sentence names whichever applies: the
        // same field is asked again against a monochrome simulation on this same body.
        val field = RecipeFields.byId(id)
        val onMonochromeBody = field?.appliesTo(context.copy(filmSimulationId = "acros")) == true
        return if (onMonochromeBody) {
            "monochromatic colour needs a monochrome film simulation"
        } else {
            "$label has no monochromatic colour"
        }
    }

    return "$label does not support it"
}

/** A value in a sentence, so a dropped-field reason reads as words rather than as a dump. */
private fun describeValue(
    field: dev.bondarenko.fujirecipes.data.fields.RecipeField,
    value: Any?,
): String = if (field is EnumFieldDef) {
    "“${field.labelFor(value as? String)}”"
} else {
    value.toString()
}

/** The name as it will reach the camera, with a note when it had to be shortened. */
private fun presetName(name: String, notes: MutableList<String>): String {
    val trimmed = name.trim()
    if (trimmed.length <= PRESET_NAME_MAX_CHARS) return trimmed

    notes += "The name was shortened to $PRESET_NAME_MAX_CHARS characters for the camera's " +
        "slot label."
    return trimmed.take(PRESET_NAME_MAX_CHARS)
}
