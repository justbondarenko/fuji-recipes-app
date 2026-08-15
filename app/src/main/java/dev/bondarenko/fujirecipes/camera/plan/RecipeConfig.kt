package dev.bondarenko.fujirecipes.camera.plan

import dev.bondarenko.fujirecipes.data.fields.FieldContext
import dev.bondarenko.fujirecipes.data.fields.RecipeFields
import dev.bondarenko.fujirecipes.data.fields.SensorGeneration

/**
 * Whether two recipes describe the **same camera configuration**.
 *
 * **Transcribed from** `fuji-recipes-book/src/utils/recipe-config-equal.ts` at commit
 * `0c17106` (`coding-standards.md` P3).
 *
 * This is the duplicate detection the import review runs on. It compares what the *camera*
 * would end up set to, not what the two records happen to look like — so a recipe that omits
 * `sharpness` and one that stores `0` for it are the same configuration, and two recipes with
 * different names and the same twenty values are duplicates.
 *
 * **Normalisation is the load-bearing half.** Without it every import looks new: a slot read
 * off a camera reports every property it holds, while a recipe typed into the form may omit
 * any field left at its default, and the two would never compare equal.
 *
 * Pure: no `android.*` (P4), enforced by `CameraPurityTest`.
 */

/** The generation to compare under when a recipe does not name one — see [RecipeFields]. */
private val DEFAULT_GENERATION = SensorGeneration.XTRANS_V

/**
 * The settings as the camera would hold them: every applicable field present, at its stored
 * value or its default, and nothing else.
 *
 * Inapplicable fields are **dropped rather than nulled**. The web client nulls them because
 * its wire format stores them as explicit nulls; here the only consumer is the comparison
 * below, and a field neither side should carry is better absent from both.
 */
fun normaliseSettings(
    settings: Map<String, Any?>,
    generation: SensorGeneration = DEFAULT_GENERATION,
): Map<String, Any?> {
    val context = contextFor(settings, generation)

    return RecipeFields.applicable(context)
        .filterNot { it.advisory }
        .mapNotNull { field ->
            val value = settings[field.id] ?: field.defaultValue ?: return@mapNotNull null
            field.id to normaliseValue(value)
        }
        .toMap()
}

/**
 * Numbers compare by value, not by type.
 *
 * A camera read yields `Double` for a tone and `Int` for a temperature; a recipe decoded from
 * JSON yields whatever the parser produced. `2` and `2.0` are the same setting, and comparing
 * them as boxed types says otherwise.
 */
private fun normaliseValue(value: Any): Any =
    if (value is Number) value.toDouble() else value

private fun contextFor(settings: Map<String, Any?>, generation: SensorGeneration) = FieldContext(
    generation = generation,
    filmSimulationId = settings["filmSimulation"] as? String,
    grainEffectOff = (settings["grainEffect"] as? String ?: "off") == "off",
    whiteBalanceId = settings["whiteBalance"] as? String,
)

/**
 * Whether two settings objects would leave the camera in the same state.
 *
 * The film simulation is checked first and on its own, because it decides which other fields
 * apply at all — comparing a monochrome recipe's fields against a colour one's would ask
 * questions neither answers.
 */
fun areConfigsEqual(
    a: Map<String, Any?>,
    b: Map<String, Any?>,
    generation: SensorGeneration = DEFAULT_GENERATION,
): Boolean {
    val left = normaliseSettings(a, generation)
    val right = normaliseSettings(b, generation)

    if (left["filmSimulation"] != right["filmSimulation"]) return false

    // Normalisation already reduced both to the applicable set, so a key present in one and
    // absent from the other is a real difference rather than an artefact of how it was stored.
    return left == right
}

/**
 * The first recipe in [library] with the same configuration, or null.
 *
 * [excludeId] skips a recipe by id, for the case where the thing being compared is already in
 * the library and would otherwise match itself.
 */
fun <T> findDuplicateConfig(
    settings: Map<String, Any?>,
    library: List<T>,
    settingsOf: (T) -> Map<String, Any?>,
    idOf: (T) -> String? = { null },
    excludeId: String? = null,
): T? = library.firstOrNull { candidate ->
    if (excludeId != null && idOf(candidate) == excludeId) {
        false
    } else {
        areConfigsEqual(settings, settingsOf(candidate))
    }
}
