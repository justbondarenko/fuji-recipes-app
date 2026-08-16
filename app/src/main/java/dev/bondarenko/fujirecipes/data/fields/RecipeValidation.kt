package dev.bondarenko.fujirecipes.data.fields

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

/**
 * What the client refuses to send — FEAT-003 T-04.
 *
 * Mirrors `fuji-recipes-book/specs/shared/recipe.schema.json`. Applied twice: here, next to
 * the field the user is looking at, and again in the repository before anything is written —
 * because an imported file is not obliged to have been through a form.
 *
 * Pure, so every rule is testable without a form (`coding-standards.md` P7).
 */
object RecipeValidation {

    /** A problem, addressed to the control that caused it. */
    data class Problem(val fieldId: String, val message: String)

    const val NAME_MAX = 80
    const val NOTES_MAX = 2000
    const val TAGS_MAX = 20
    const val TAG_MAX = 30
    const val RATING_MAX = 5

    const val NAME_FIELD = "name"
    const val NOTES_FIELD = "notes"
    const val RATING_FIELD = "rating"
    const val TAGS_FIELD = "tags"

    fun validateName(name: String): Problem? = when {
        name.isBlank() -> Problem(NAME_FIELD, "A recipe needs a name.")
        name.length > NAME_MAX -> Problem(NAME_FIELD, "Keep the name to $NAME_MAX characters.")
        else -> null
    }

    fun validateNotes(notes: String): Problem? =
        if (notes.length > NOTES_MAX) {
            Problem(NOTES_FIELD, "Notes are limited to $NOTES_MAX characters.")
        } else {
            null
        }

    fun validateRating(rating: Int): Problem? =
        if (rating !in 0..RATING_MAX) {
            Problem(RATING_FIELD, "A rating runs from 0 to $RATING_MAX.")
        } else {
            null
        }

    fun validateTags(tags: List<String>): Problem? = when {
        tags.size > TAGS_MAX -> Problem(TAGS_FIELD, "Up to $TAGS_MAX tags.")
        tags.any { it.isBlank() } -> Problem(TAGS_FIELD, "A tag cannot be empty.")
        tags.any { it.length > TAG_MAX } ->
            Problem(TAGS_FIELD, "Keep tags to $TAG_MAX characters.")
        tags.size != tags.distinct().size -> Problem(TAGS_FIELD, "That tag is already on this recipe.")
        else -> null
    }

    /**
     * A numeric setting against its own row in the field table.
     *
     * Reached mainly by typing: the `−`/`+` buttons clamp, so they cannot produce an
     * out-of-range value, but the editable field can (`01-functional.md` §4).
     */
    fun validateNumber(field: NumberField, value: Double?): Problem? {
        if (value == null) {
            // Only the advisory bounds are legitimately absent; everything else has a default.
            return if (field.defaultValue == null) null else Problem(field.id, "Enter a value.")
        }
        return if (value < field.min || value > field.max) {
            Problem(field.id, "${field.label} runs from ${plain(field.min)} to ${plain(field.max)}.")
        } else {
            null
        }
    }

    /** `-2` rather than `-2.0`, so a message about an integer range reads like one. */
    private fun plain(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

    /**
     * Everything wrong with a recipe as currently edited, in field order.
     *
     * Only the **applicable** settings are checked: a colour value left behind on a recipe
     * that has since become monochrome is not shown, is not written, and must not block a
     * save (`field-definitions.md` §4).
     */
    fun validate(
        name: String,
        notes: String,
        rating: Int,
        tags: List<String>,
        settings: JsonObject,
        context: FieldContext,
    ): List<Problem> = buildList {
        validateName(name)?.let(::add)
        validateNotes(notes)?.let(::add)
        validateRating(rating)?.let(::add)
        validateTags(tags)?.let(::add)

        for (field in RecipeFields.applicable(context)) {
            if (field !is NumberField) continue
            val raw = (settings[field.id] as? JsonPrimitive)?.doubleOrNull
            validateNumber(field, raw ?: (field.defaultValue as? Number)?.toDouble())?.let(::add)
        }
    }
}
