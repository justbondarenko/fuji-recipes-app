package dev.bondarenko.fujirecipes.data.compare

import dev.bondarenko.fujirecipes.data.fields.FieldGroup
import dev.bondarenko.fujirecipes.data.model.Recipe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecipeComparisonTest {

    private fun parse(text: String): JsonObject = Json.parseToJsonElement(text).jsonObject

    private val recipeA = Recipe.fromJson(
        parse(
            """
            {
              "id": "recipe-a",
              "name": "Kodachrome 64",
              "notes": "Midday warm light",
              "rating": 5,
              "tags": ["street", "warm"],
              "settings": {
                "filmSimulation": "classic-chrome",
                "dynamicRange": "dr400",
                "highlightTone": 1.5,
                "shadowTone": 0,
                "color": 2,
                "sharpness": 0,
                "grainEffect": "weak",
                "grainSize": "small",
                "whiteBalance": "color-temp",
                "colorTemperature": 5500,
                "wbShiftRed": 3,
                "wbShiftBlue": -2
              }
            }
            """.trimIndent(),
        ),
    )

    private val recipeB = Recipe.fromJson(
        parse(
            """
            {
              "id": "recipe-b",
              "name": "Portra 400",
              "notes": "Portraits and golden hour",
              "rating": 4,
              "tags": ["portrait"],
              "settings": {
                "filmSimulation": "classic-chrome",
                "dynamicRange": "dr200",
                "highlightTone": -1.0,
                "shadowTone": 0,
                "color": 2,
                "sharpness": 0,
                "grainEffect": "strong",
                "grainSize": "large",
                "whiteBalance": "auto",
                "wbShiftRed": 3,
                "wbShiftBlue": -4
              }
            }
            """.trimIndent(),
        ),
    )

    @Test
    fun `comparing identical recipes reports zero differences and allMatch`() {
        val result = RecipeComparison.compare(recipeA, recipeA)
        assertEquals(0, result.differencesCount)
        assertTrue(result.allMatch)
        assertTrue(result.groups.isNotEmpty())
        assertTrue(result.groups.all { group -> group.rows.all { it.isSame } })
    }

    @Test
    fun `comparing differing recipes identifies exact matching and differing rows`() {
        val result = RecipeComparison.compare(recipeA, recipeB)

        assertFalse(result.allMatch)
        assertTrue(result.differencesCount > 0)

        val allRows = result.groups.flatMap { it.rows }.associateBy { it.fieldId }

        // filmSimulation: both classic-chrome -> same
        val simRow = allRows["filmSimulation"]
        assertEquals("Classic Chrome", simRow?.valueA)
        assertEquals("Classic Chrome", simRow?.valueB)
        assertEquals(true, simRow?.isSame)

        // dynamicRange: DR400 vs DR200 -> different
        val drRow = allRows["dynamicRange"]
        assertEquals("DR400", drRow?.valueA)
        assertEquals("DR200", drRow?.valueB)
        assertEquals(false, drRow?.isSame)

        // highlightTone: +1.5 vs -1.0 -> different
        val hlRow = allRows["highlightTone"]
        assertEquals("+1.5", hlRow?.valueA)
        assertEquals("-1", hlRow?.valueB)
        assertEquals(false, hlRow?.isSame)

        // shadowTone: 0 vs 0 -> same
        val stRow = allRows["shadowTone"]
        assertEquals("0", stRow?.valueA)
        assertEquals("0", stRow?.valueB)
        assertEquals(true, stRow?.isSame)

        // wbShift: R +3 / B -2 vs R +3 / B -4 -> different
        val wbShiftRow = allRows["wbShift"]
        assertEquals("R +3 / B -2", wbShiftRow?.valueA)
        assertEquals("R +3 / B -4", wbShiftRow?.valueB)
        assertEquals(false, wbShiftRow?.isSame)
    }

    @Test
    fun `groups preserve FieldGroup order`() {
        val result = RecipeComparison.compare(recipeA, recipeB)
        val groups = result.groups.map { it.group }

        assertEquals(
            groups.sortedBy { it.ordinal },
            groups,
            "Groups must strictly preserve FieldGroup enum definition order",
        )
    }

    @Test
    fun `monochrome fields apply correctly when one recipe is monochrome`() {
        val monochromeRecipe = Recipe.fromJson(
            parse(
                """
                {
                  "id": "recipe-c",
                  "name": "Acros Contrast",
                  "settings": {
                    "filmSimulation": "acros-r",
                    "monochromaticColorWc": 2,
                    "monochromaticColorMg": -1
                  }
                }
                """.trimIndent(),
            ),
        )

        val result = RecipeComparison.compare(recipeA, monochromeRecipe)
        val allRows = result.groups.flatMap { it.rows }.associateBy { it.fieldId }

        // Color applies to recipeA but not to monochrome
        val colorRow = allRows["color"]
        assertEquals("+2", colorRow?.valueA)
        assertEquals("—", colorRow?.valueB)
        assertEquals(false, colorRow?.isSame)

        // Monochromatic warm/cool applies to monochrome recipe
        val wcRow = allRows["monochromaticColorWc"]
        assertEquals("—", wcRow?.valueA)
        assertEquals("+2", wcRow?.valueB)
        assertEquals(false, wcRow?.isSame)
    }
}
