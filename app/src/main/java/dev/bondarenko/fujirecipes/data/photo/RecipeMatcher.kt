package dev.bondarenko.fujirecipes.data.photo

import dev.bondarenko.fujirecipes.data.fields.FilmSimulations
import dev.bondarenko.fujirecipes.data.fields.RecipeFields
import dev.bondarenko.fujirecipes.data.model.Recipe
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlin.math.roundToInt

/**
 * Which of your recipes a photo was shot with.
 *
 * **Transcribed from** `fuji-recipes-book/src/utils/recipe-matcher.ts` at commit `0c17106`
 * (`coding-standards.md` P3).
 *
 * **Scored, not boolean, and that is the design.** An exact match answers "which of mine is
 * this?", which is the question the feature exists for. A 90% match answers the more
 * interesting one — "this is Kodachrome 64 but you shot it with a different white balance" —
 * and hiding that behind a yes/no would throw away the most useful thing the app knows.
 *
 * **Only the fields the photo carried are compared.** A photo reporting eight settings is
 * scored out of eight. Scoring it out of all twenty-two would punish a recipe for values the
 * photo never mentioned, and every match would look poor.
 *
 * Pure: no `android.*`. Distinct from `camera/plan/RecipeConfig.kt`, which asks a different
 * question — *are these two recipes the same configuration*, over the fields that **apply**.
 * Here the photo decides what is compared, because it is the one making a claim.
 */

/** Below this, a recipe is not offered as a match at all. */
const val SIMILAR_THRESHOLD = 70

data class FieldMismatch(
    val fieldId: String,
    val label: String,
    val photoValue: String,
    val savedValue: String,
)

data class RecipeMatch(
    val recipe: Recipe,
    val matched: Int,
    val compared: Int,
    val mismatches: List<FieldMismatch>,
) {
    val percentage: Int get() = if (compared == 0) 0 else (matched * 100.0 / compared).roundToInt()
    val isExact: Boolean get() = compared > 0 && matched == compared
}

data class MatchResult(
    val exact: List<RecipeMatch> = emptyList(),
    val similar: List<RecipeMatch> = emptyList(),
    /** The one to lead with, or null when nothing reached the threshold. */
    val best: RecipeMatch? = null,
    val recipesChecked: Int = 0,
)

/**
 * Compares a photo's decoded settings against the library.
 *
 * The values on both sides are already in this app's vocabulary — the parser maps them on the
 * way out — so there is no normalisation here beyond making numbers compare by value. The
 * reference needs a `normalizeWb`/`normalizeDr` pass at this point precisely because its
 * parser leaves the camera's own wording in place; ours does not, and the translation has one
 * home instead of two.
 */
fun findMatches(photo: PhotoRecipe, library: List<Recipe>): MatchResult {
    val compared = photo.settings.filterKeys { RecipeFields.byId(it) != null }
    if (compared.isEmpty() || library.isEmpty()) {
        return MatchResult(recipesChecked = library.size)
    }

    val all = library.map { recipe -> score(photo, compared, recipe) }
        .sortedWith(
            compareByDescending<RecipeMatch> { it.isExact }
                .thenByDescending { it.percentage }
                .thenByDescending { it.matched },
        )

    return MatchResult(
        exact = all.filter { it.isExact },
        similar = all.filter { !it.isExact && it.percentage >= SIMILAR_THRESHOLD },
        best = all.firstOrNull()?.takeIf { it.percentage >= SIMILAR_THRESHOLD },
        recipesChecked = library.size,
    )
}

private fun score(
    photo: PhotoRecipe,
    compared: Map<String, Any>,
    recipe: Recipe,
): RecipeMatch {
    var matched = 0
    val mismatches = mutableListOf<FieldMismatch>()

    compared.forEach { (fieldId, photoValue) ->
        val saved = recipe.settings.plainValue(fieldId)

        if (sameValue(photoValue, saved)) {
            matched += 1
        } else {
            mismatches += FieldMismatch(
                fieldId = fieldId,
                label = RecipeFields.byId(fieldId)?.label ?: fieldId,
                photoValue = format(fieldId, photoValue),
                savedValue = format(fieldId, saved),
            )
        }
    }

    return RecipeMatch(recipe, matched, compared.size, mismatches)
}

/**
 * Numbers compare by value, not by type.
 *
 * A tone decoded from a photo is a `Double`; the same tone stored in a recipe may have come
 * back from JSON as an `Int`. `2` and `2.0` are the same setting, and comparing the boxed
 * types says otherwise — which would report a mismatch on a field that matches exactly.
 */
private fun sameValue(a: Any?, b: Any?): Boolean = when {
    a is Number && b is Number -> a.toDouble() == b.toDouble()
    else -> a == b
}

private fun JsonObject.plainValue(key: String): Any? {
    val primitive = this[key] as? JsonPrimitive ?: return null
    if (primitive.isString) return primitive.content
    return primitive.booleanOrNull ?: primitive.doubleOrNull
}

/** A value as a person reads it, for the "what differs" list. */
private fun format(fieldId: String, value: Any?): String {
    if (value == null) return "Not set"

    return when (fieldId) {
        "filmSimulation" -> FilmSimulations.byId(value as? String)?.label ?: value.toString()

        "dynamicRange" -> when (val id = value.toString()) {
            "dr-auto" -> "Auto"
            else -> id.removePrefix("dr").let { "DR$it" }
        }

        "colorTemperature" -> "${(value as? Number)?.toInt() ?: value}K"

        else -> when (value) {
            is Number -> {
                val number = value.toDouble()
                val text = if (number % 1.0 == 0.0) number.toInt().toString() else number.toString()
                if (number > 0) "+$text" else text
            }

            // An enum id as the form would show it: "weak" reads as "Weak".
            else -> value.toString().replaceFirstChar { it.uppercase() }
        }
    }
}
