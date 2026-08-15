package dev.bondarenko.fujirecipes.camera

import dev.bondarenko.fujirecipes.data.fields.SensorGeneration

/**
 * Model string to sensor generation.
 *
 * **Transcribed from** `fuji-recipes-book/camera/models.ts` at commit `0c17106`
 * (`coding-standards.md` P3).
 *
 * The camera reports a model in its device info — "X-T5", "X100VI" — and everything
 * downstream needs the *generation*: which fields apply, whether custom slots exist at all
 * (WR-01), and whether the property codes have been verified for it.
 *
 * **Source.** Fujifilm's published specifications, body by body — the processor generation is
 * in every one. Not the protocol reference, which names only four bodies. That makes this
 * table the one piece of camera knowledge in the project with no single citable document
 * behind it, so the shape of the module matters more than its length: an unrecognised body is
 * a first-class answer that refuses writes, and adding a body is one line.
 *
 * **Nothing is inferred from a name.** A prefix rule — "anything starting with GFX is a GFX" —
 * would be one line and would offer a write to a body nobody has tested, on the strength of
 * its name. Either a body is in the table or it is unrecognised.
 *
 * **Two generations are deliberately absent.** X-Trans I and II bodies — X-Pro1, X100S, X-T1
 * and their contemporaries — have no entry in `field-definitions.md` §1, which starts at
 * X-Trans III. They are left out rather than folded into `cmos`, because calling an X100S a
 * Bayer CMOS body would be wrong and the honest answer is already available: unrecognised,
 * writes refused. Do not "fix" this by mapping them.
 *
 * Pure: no `android.*` (P4).
 */
object CameraModels {

    /**
     * Bodies whose generation is settled, keyed by the model name as Fujifilm writes it.
     *
     * Lookups normalise, so the punctuation here is for the reader; `X-T5` and `XT5` both find
     * the same row. A test asserts no two names normalise to the same key.
     */
    val MODEL_GENERATIONS: Map<String, SensorGeneration> = mapOf(
        // ── X-Trans III (X-Processor Pro). No slot registers, so writes are refused. ──
        "X-Pro2" to SensorGeneration.XTRANS_III,
        "X-T2" to SensorGeneration.XTRANS_III,
        "X-T20" to SensorGeneration.XTRANS_III,
        "X-E3" to SensorGeneration.XTRANS_III,
        "X-H1" to SensorGeneration.XTRANS_III,
        "X100F" to SensorGeneration.XTRANS_III,

        // ── X-Trans IV (X-Processor 4) ──
        "X-T3" to SensorGeneration.XTRANS_IV,
        "X-T30" to SensorGeneration.XTRANS_IV,
        "X-T30 II" to SensorGeneration.XTRANS_IV,
        "X-T4" to SensorGeneration.XTRANS_IV,
        "X-Pro3" to SensorGeneration.XTRANS_IV,
        "X-S10" to SensorGeneration.XTRANS_IV,
        "X-E4" to SensorGeneration.XTRANS_IV,
        "X100V" to SensorGeneration.XTRANS_IV,

        // ── X-Trans V (X-Processor 5). The generation the property codes were captured on. ──
        "X-H2" to SensorGeneration.XTRANS_V,
        "X-H2S" to SensorGeneration.XTRANS_V,
        "X-T5" to SensorGeneration.XTRANS_V,
        "X-T50" to SensorGeneration.XTRANS_V,
        "X-S20" to SensorGeneration.XTRANS_V,
        "X-M5" to SensorGeneration.XTRANS_V,
        "X-E5" to SensorGeneration.XTRANS_V,
        "X100VI" to SensorGeneration.XTRANS_V,

        // ── GFX ──
        //
        // The 100-series only. `field-definitions.md` §1 gives `gfx` custom-slot support and
        // the X-Trans V field set, both **unverified**, and the 50-series is the X-Processor
        // Pro generation — the same era as X-Trans III, which this app refuses to write to.
        // Putting a GFX 50S in the `gfx` row would offer it a write on the strength of its
        // name.
        "GFX100 II" to SensorGeneration.GFX,
        "GFX100" to SensorGeneration.GFX,
        "GFX100S" to SensorGeneration.GFX,
        "GFX100S II" to SensorGeneration.GFX,

        // ── Bayer CMOS. Storage only, and correctly labelled as such. ──
        //
        // The GFX 50-series belongs here on the facts: a Bayer sensor, and no verified slot
        // support. It is not an X-Trans body and this row says so, which beats both
        // "unrecognised" and a `gfx` row it has not earned.
        "GFX 50S" to SensorGeneration.CMOS,
        "GFX 50R" to SensorGeneration.CMOS,
        "GFX50S II" to SensorGeneration.CMOS,
        "X-T100" to SensorGeneration.CMOS,
        "X-T200" to SensorGeneration.CMOS,
        "X-A5" to SensorGeneration.CMOS,
        "X-A7" to SensorGeneration.CMOS,
        "XF10" to SensorGeneration.CMOS,
    )

    /** What an unrecognised body falls back to: the generation that permits nothing. */
    val FALLBACK_GENERATION: SensorGeneration = SensorGeneration.CMOS

    const val UNRECOGNISED_LABEL = "Unrecognised body"

    const val UNRECOGNISED_NOTE =
        "This app does not know this body's sensor generation, so it will not write to it. " +
            "Storing recipes works normally."

    /**
     * Uppercase, letters and digits only.
     *
     * Bodies are written with hyphens and spaces in some places and neither in others, and a
     * camera's own string is not guaranteed to match a marketing name's punctuation. Dropping
     * everything else compares what is actually distinctive.
     */
    fun normaliseModel(model: String): String =
        model.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }

    /** Normalised name to the row it came from. Built once; see the collision test. */
    val MODEL_LOOKUP: Map<String, ModelRow> =
        MODEL_GENERATIONS.entries.associate { (name, generation) ->
            normaliseModel(name) to ModelRow(name, generation)
        }

    data class ModelRow(val name: String, val generation: SensorGeneration)

    /**
     * What this build knows about a reported model.
     *
     * An unrecognised body gets `cmos` — writes refused, the narrowest field set — but it does
     * **not** get `cmos`'s label. "Bayer CMOS" would be an assertion about a sensor nobody has
     * looked at; [UNRECOGNISED_LABEL] and its note say what is actually true, which is that
     * the app does not know this body and will not write to it. A caller showing
     * `identity.generation.label` instead of `identity.label` would reintroduce exactly the
     * claim this avoids.
     */
    fun identify(model: String?): ModelIdentity {
        val reported = model.orEmpty().trim()
        val row = MODEL_LOOKUP[normaliseModel(reported)]
            ?: return ModelIdentity(
                model = reported,
                generation = FALLBACK_GENERATION,
                recognised = false,
                label = UNRECOGNISED_LABEL,
                writable = false,
                note = UNRECOGNISED_NOTE,
            )

        val writable = row.generation.hasCustomSlots

        return ModelIdentity(
            model = reported,
            generation = row.generation,
            recognised = true,
            label = row.generation.label,
            writable = writable,
            // The two reasons a write is unavailable must stay distinguishable: a body without
            // slot registers will never accept one, which is not the same as one this build
            // has not met.
            note = if (writable) {
                null
            } else {
                "${row.generation.label} has no custom slots, so recipes cannot be written " +
                    "to it."
            },
        )
    }
}

data class ModelIdentity(
    /** The model string as the camera reported it, trimmed. */
    val model: String,
    val generation: SensorGeneration,
    /** Whether the table names this body. */
    val recognised: Boolean,
    /** What to show for the generation — never a fallback presented as a fact. */
    val label: String,
    /** Whether a write can be offered at all (WR-01). */
    val writable: Boolean,
    /** Why not, when the answer is no (P5). */
    val note: String? = null,
)
