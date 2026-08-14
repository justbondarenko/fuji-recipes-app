package dev.bondarenko.fujirecipes.data.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The forward-compatibility guarantee — FEAT-001 T-03.
 *
 * `data-model.md` §1 gives the server an `extra` column so a field written by a newer client
 * survives a round trip through an older one. That promise is only kept if *this* client
 * holds up its end, and a model with a fixed field list would break it silently — the app
 * would work, and data would quietly disappear on the next edit.
 */
class RecipeTest {

    private fun parse(text: String): JsonObject =
        Json.parseToJsonElement(text).jsonObject

    private val full = """
        {"id":"3f2b1c8e-9d44-4a17-b0e6-7c1d2e5a9f30","name":"Kodachrome 64",
         "notes":"Warm midday light.","rating":5,"tags":["street","warm"],"sortKey":1000,
         "settings":{"filmSimulation":"classic-chrome","clarity":2},
         "createdAt":"2026-07-02T18:04:11.000Z","updatedAt":"2026-08-12T07:22:39.512Z",
         "lastWrittenSlot":3,"lastWrittenAt":"2026-08-12T07:25:02.104Z"}
    """.trimIndent()

    @Test
    fun `every known field is read`() {
        val recipe = Recipe.fromJson(parse(full))

        assertEquals("Kodachrome 64", recipe.name)
        assertEquals(5, recipe.rating)
        assertEquals(listOf("street", "warm"), recipe.tags)
        assertEquals(1000.0, recipe.sortKey)
        assertEquals(3, recipe.lastWrittenSlot)
        assertEquals("classic-chrome", recipe.filmSimulationId)
    }

    @Test
    fun `an unknown top-level property survives a round trip`() {
        val withFuture = parse(
            full.dropLast(1) + ""","stackId":"abc-123","favourite":true}""",
        )

        val recipe = Recipe.fromJson(withFuture)
        val out = recipe.toJson()

        assertEquals("abc-123", out["stackId"]?.jsonPrimitive?.content)
        assertEquals("true", out["favourite"]?.jsonPrimitive?.content)
    }

    @Test
    fun `an unknown settings key survives a round trip`() {
        // `settings` is held raw precisely so FEAT-002 can grow into it without FEAT-001
        // having to know every parameter today.
        val recipe = Recipe.fromJson(parse(full))
        val out = recipe.toJson()

        assertEquals("2", out["settings"]?.jsonObject?.get("clarity")?.jsonPrimitive?.content)
    }

    @Test
    fun `a known field cannot be shadowed by a stray extra of the same name`() {
        // Not reachable from a well-behaved server, but the precedence has to be *some*
        // way round, and this is the way the server itself does it in rowToEntity.
        val recipe = Recipe.fromJson(parse(full)).copy(
            extra = parse("""{"name":"Impostor"}"""),
        )

        assertEquals("Kodachrome 64", recipe.toJson()["name"]?.jsonPrimitive?.content)
    }

    @Test
    fun `a sparse recipe parses on defaults rather than failing`() {
        val recipe = Recipe.fromJson(parse("""{"id":"a","name":"Bare"}"""))

        assertEquals(0, recipe.rating)
        assertTrue(recipe.tags.isEmpty())
        assertNull(recipe.lastWrittenSlot)
        assertNull(recipe.filmSimulationId)
    }

    @Test
    fun `a recipe with no film simulation does not blow up the accessor`() {
        val recipe = Recipe.fromJson(parse("""{"id":"a","name":"X","settings":{"clarity":1}}"""))
        assertNull(recipe.filmSimulationId)
    }
}
