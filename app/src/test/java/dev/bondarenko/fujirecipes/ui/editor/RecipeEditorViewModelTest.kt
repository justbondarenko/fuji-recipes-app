package dev.bondarenko.fujirecipes.ui.editor

import dev.bondarenko.fujirecipes.data.model.Recipe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The bodies the editor sends — FEAT-003 T-06.
 *
 * Two guarantees live here, and neither is visible on screen:
 *
 * 1. A PATCH carries **only** what changed, so a key that was never touched cannot be
 *    overwritten — including settings this build does not model.
 * 2. A create carries what the user chose and nothing else; `id`, `sortKey`, `createdAt` and
 *    `updatedAt` are the server's to assign.
 */
class RecipeEditorViewModelTest {

    private fun parse(text: String): JsonObject = Json.parseToJsonElement(text).jsonObject

    private val stored = Recipe.fromJson(
        parse(
            """
            {"id":"abc","name":"Kodachrome 64","notes":"Warm.","rating":3,
             "tags":["street"],"sortKey":1000,
             "settings":{"filmSimulation":"classic-chrome","sharpness":1,
                         "aFieldFromTheFuture":"keep me"},
             "createdAt":"2026-07-02T18:04:11.000Z","updatedAt":"2026-08-12T07:22:39.512Z"}
            """.trimIndent(),
        ),
    )

    private fun stateOf(recipe: Recipe) = EditorUiState(
        isLoading = false,
        name = recipe.name,
        notes = recipe.notes,
        rating = recipe.rating,
        tags = recipe.tags,
        settings = recipe.settings,
    )

    @Test
    fun `an unchanged recipe produces an empty diff`() {
        // An empty PATCH would still refresh `updatedAt` and reshuffle the "recently
        // updated" sort for a save that changed nothing.
        val diff = RecipeEditorViewModel.diffAgainst(stored, stateOf(stored))
        assertTrue(diff.isEmpty())
    }

    @Test
    fun `changing the rating sends only the rating`() {
        val diff = RecipeEditorViewModel.diffAgainst(stored, stateOf(stored).copy(rating = 5))
        assertEquals(setOf("rating"), diff.keys)
        assertEquals("5", diff["rating"]?.jsonPrimitive?.content)
    }

    @Test
    fun `changing the name sends only the name, trimmed`() {
        val diff = RecipeEditorViewModel.diffAgainst(
            stored,
            stateOf(stored).copy(name = "  Kodachrome 200  "),
        )
        assertEquals(setOf("name"), diff.keys)
        assertEquals("Kodachrome 200", diff["name"]?.jsonPrimitive?.content)
    }

    @Test
    fun `a name differing only by surrounding whitespace is not a change`() {
        val diff = RecipeEditorViewModel.diffAgainst(
            stored,
            stateOf(stored).copy(name = "  Kodachrome 64  "),
        )
        assertTrue(diff.isEmpty())
    }

    @Test
    fun `editing one setting keeps the ones this build does not know`() {
        // The whole reason the working copy is a raw JsonObject. A typed settings class
        // would drop `aFieldFromTheFuture` on the first edit, silently.
        val edited = stored.settings.toMutableMap()
        edited["sharpness"] = kotlinx.serialization.json.JsonPrimitive(3)

        val diff = RecipeEditorViewModel.diffAgainst(
            stored,
            stateOf(stored).copy(settings = JsonObject(edited)),
        )

        val settings = diff["settings"]?.jsonObject
        assertEquals("3", settings?.get("sharpness")?.jsonPrimitive?.content)
        assertEquals("keep me", settings?.get("aFieldFromTheFuture")?.jsonPrimitive?.content)
    }

    @Test
    fun `tags are sent as an array when they change`() {
        val diff = RecipeEditorViewModel.diffAgainst(
            stored,
            stateOf(stored).copy(tags = listOf("street", "warm")),
        )
        assertEquals(
            listOf("street", "warm"),
            diff["tags"]?.jsonArray?.map { it.jsonPrimitive.content },
        )
    }

    @Test
    fun `a create body carries the user's choices and nothing the server assigns`() {
        val body = RecipeEditorViewModel.createBody(stateOf(stored).copy(name = "New"))

        assertEquals(setOf("name", "notes", "rating", "tags", "settings"), body.keys)
        // The server owns these; sending them would be the client guessing at values it
        // cannot know.
        assertTrue("id" !in body.keys)
        assertTrue("sortKey" !in body.keys)
        assertTrue("createdAt" !in body.keys)
    }

    @Test
    fun `a create body keeps unknown settings too`() {
        val body = RecipeEditorViewModel.createBody(stateOf(stored))
        assertEquals(
            "keep me",
            body["settings"]?.jsonObject?.get("aFieldFromTheFuture")?.jsonPrimitive?.content,
        )
    }
}
