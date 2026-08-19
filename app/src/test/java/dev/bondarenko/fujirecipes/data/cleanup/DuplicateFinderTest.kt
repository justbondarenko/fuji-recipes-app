package dev.bondarenko.fujirecipes.data.cleanup

import dev.bondarenko.fujirecipes.data.model.Recipe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DuplicateFinderTest {

    private fun parse(text: String): JsonObject = Json.parseToJsonElement(text).jsonObject

    private fun makeRecipe(
        id: String,
        name: String,
        filmSim: String = "classic-chrome",
        highlight: Double = 0.0,
        shadow: Double = 0.0,
        color: Double = 0.0,
        grain: String = "off",
        rating: Int = 0,
        updatedAt: String = "2026-01-01T00:00:00Z",
    ): Recipe {
        val json = """
            {
              "id": "$id",
              "name": "$name",
              "rating": $rating,
              "updatedAt": "$updatedAt",
              "settings": {
                "filmSimulation": "$filmSim",
                "highlightTone": $highlight,
                "shadowTone": $shadow,
                "color": $color,
                "grainEffect": "$grain"
              }
            }
        """.trimIndent()
        return Recipe.fromJson(parse(json))
    }

    @Test
    fun `empty library or single recipe returns empty results`() {
        val emptyResult = DuplicateFinder.findDuplicates(emptyList())
        assertTrue(emptyResult.isEmpty)
        assertEquals(0, emptyResult.totalRecipesScanned)

        val single = makeRecipe("1", "Recipe 1")
        val singleResult = DuplicateFinder.findDuplicates(listOf(single))
        assertTrue(singleResult.isEmpty)
        assertEquals(1, singleResult.totalRecipesScanned)
    }

    @Test
    fun `identifies 100 percent match exact duplicates`() {
        val r1 = makeRecipe("r1", "Kodachrome A", highlight = 1.0, shadow = -1.0, color = 2.0, rating = 3)
        val r2 = makeRecipe("r2", "Kodachrome B", highlight = 1.0, shadow = -1.0, color = 2.0, rating = 5)
        val r3 = makeRecipe("r3", "Unique Velvia", filmSim = "velvia", highlight = 0.0)

        val result = DuplicateFinder.findDuplicates(listOf(r1, r2, r3))

        assertEquals(1, result.exactGroups.size)
        assertEquals(1, result.totalDuplicateRecipesCount)
        val group = result.exactGroups.first()
        assertEquals(2, group.recipes.size)
        // High rating recipe is chosen as default keep
        assertEquals("r2", group.defaultKeepId)
        assertTrue(result.similarPairs.isEmpty())
    }

    @Test
    fun `clusters multiple exact duplicates into same group`() {
        val r1 = makeRecipe("r1", "Copy 1", highlight = 1.0)
        val r2 = makeRecipe("r2", "Copy 2", highlight = 1.0)
        val r3 = makeRecipe("r3", "Copy 3", highlight = 1.0)

        val result = DuplicateFinder.findDuplicates(listOf(r1, r2, r3))

        assertEquals(1, result.exactGroups.size)
        assertEquals(2, result.totalDuplicateRecipesCount)
        assertEquals(3, result.exactGroups.first().recipes.size)
    }

    @Test
    fun `identifies highly similar recipes differing in 1 to 3 fields with same film simulation`() {
        // Differing in 1 field: highlightTone (1.0 vs 2.0)
        val r1 = makeRecipe("r1", "Recipe 1", highlight = 1.0, shadow = 0.0, color = 1.0)
        val r2 = makeRecipe("r2", "Recipe 2", highlight = 2.0, shadow = 0.0, color = 1.0)

        val result = DuplicateFinder.findDuplicates(listOf(r1, r2))

        assertEquals(0, result.exactGroups.size)
        assertEquals(1, result.similarPairs.size)

        val pair = result.similarPairs.first()
        assertEquals("r1", pair.recipeA.id)
        assertEquals("r2", pair.recipeB.id)
        assertEquals(1, pair.differences.size)
        assertEquals("highlightTone", pair.differences.first().fieldId)
        assertEquals("+1", pair.differences.first().valueA)
        assertEquals("+2", pair.differences.first().valueB)
    }

    @Test
    fun `identifies highly similar recipes differing in 3 fields`() {
        // Differing in 3 fields: highlightTone, shadowTone, color
        val r1 = makeRecipe("r1", "Recipe 1", highlight = 1.0, shadow = -1.0, color = 1.0)
        val r2 = makeRecipe("r2", "Recipe 2", highlight = 2.0, shadow = 0.0, color = 2.0)

        val result = DuplicateFinder.findDuplicates(listOf(r1, r2))

        assertEquals(0, result.exactGroups.size)
        assertEquals(1, result.similarPairs.size)
        assertEquals(3, result.similarPairs.first().differences.size)
    }

    @Test
    fun `does not include recipes with 4 or more differences in highly similar`() {
        // Differing in 4 fields: highlightTone, shadowTone, color, grainEffect
        val r1 = makeRecipe("r1", "Recipe 1", highlight = 1.0, shadow = -1.0, color = 1.0, grain = "off")
        val r2 = makeRecipe("r2", "Recipe 2", highlight = 2.0, shadow = 0.0, color = 2.0, grain = "weak")

        val result = DuplicateFinder.findDuplicates(listOf(r1, r2))

        assertEquals(0, result.exactGroups.size)
        assertEquals(0, result.similarPairs.size)
    }

    @Test
    fun `does not include recipes with different film simulations in highly similar`() {
        // Identical parameters but different film simulations (classic-chrome vs velvia)
        val r1 = makeRecipe("r1", "Recipe Chrome", filmSim = "classic-chrome", highlight = 1.0)
        val r2 = makeRecipe("r2", "Recipe Velvia", filmSim = "velvia", highlight = 1.0)

        val result = DuplicateFinder.findDuplicates(listOf(r1, r2))

        assertEquals(0, result.exactGroups.size)
        assertEquals(0, result.similarPairs.size)
    }
}
