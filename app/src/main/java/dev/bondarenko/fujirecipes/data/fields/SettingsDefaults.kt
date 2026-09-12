package dev.bondarenko.fujirecipes.data.fields

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Ground zero: every documented default, as a settings object.
 *
 * A new recipe starts here rather than at zero, and so does a lab session with no recipe
 * applied — the same object, because "a recipe I have not changed yet" has to mean one thing
 * in both places. Fields whose §4 default is null (the ISO bounds) are omitted rather than
 * written as null, which is what §5 means by an unset advisory field.
 */
fun defaultRecipeSettings(): JsonObject = buildJsonObject {
    RecipeFields.all.forEach { field ->
        when (val default = field.defaultValue) {
            is String -> put(field.id, default)
            is Number -> put(field.id, default)
            else -> Unit
        }
    }
}
