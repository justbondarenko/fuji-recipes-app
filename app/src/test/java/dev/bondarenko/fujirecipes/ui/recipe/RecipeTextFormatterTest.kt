package dev.bondarenko.fujirecipes.ui.recipe

import dev.bondarenko.fujirecipes.data.fields.FieldFormatting
import dev.bondarenko.fujirecipes.data.fields.FieldGroup
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecipeTextFormatterTest {

    @Test
    fun `formats complete recipe with groups, rating, tags, and notes`() {
        val header = RecipeHeader(
            id = "test-1",
            name = "Kodachrome 64",
            filmSimulationId = "classic-chrome",
            filmSimulationLabel = "Classic Chrome",
            rating = 5,
            tags = listOf("street", "warm"),
            notes = "Best shot in golden hour.\nhttps://fujixweekly.com/recipes",
        )

        val groups = listOf(
            SettingsGroup(
                group = FieldGroup.SIMULATION,
                rows = listOf(
                    FieldFormatting.Row("filmSimulation", "Film simulation", "Classic Chrome", isDefault = false, advisory = false),
                    FieldFormatting.Row("dynamicRange", "Dynamic range", "DR400", isDefault = false, advisory = false),
                ),
            ),
            SettingsGroup(
                group = FieldGroup.TONE,
                rows = listOf(
                    FieldFormatting.Row("highlightTone", "Highlight tone", "+1.5", isDefault = false, advisory = false),
                    FieldFormatting.Row("shadowTone", "Shadow tone", "0", isDefault = true, advisory = false),
                ),
            ),
        )

        val result = RecipeTextFormatter.format(header, groups)

        assertTrue(result.contains("Kodachrome 64"))
        assertTrue(result.contains("Film simulation: Classic Chrome"))
        assertTrue(result.contains("Rating: ★★★★★ (5/5)"))
        assertTrue(result.contains("Tags: street, warm"))
        assertTrue(result.contains("Film simulation\n• Film simulation: Classic Chrome\n• Dynamic range: DR400"))
        assertTrue(result.contains("Tone\n• Highlight tone: +1.5\n• Shadow tone: 0"))
        assertTrue(result.contains("Notes:\nBest shot in golden hour.\nhttps://fujixweekly.com/recipes"))
    }

    @Test
    fun `formats minimal recipe without rating, tags, or notes`() {
        val header = RecipeHeader(
            id = "test-2",
            name = "Simple Acros",
            filmSimulationId = "acros",
            filmSimulationLabel = "ACROS",
            rating = 0,
            tags = emptyList(),
            notes = "",
        )

        val groups = listOf(
            SettingsGroup(
                group = FieldGroup.SIMULATION,
                rows = listOf(
                    FieldFormatting.Row("filmSimulation", "Film simulation", "ACROS", isDefault = false, advisory = false),
                ),
            ),
        )

        val result = RecipeTextFormatter.format(header, groups)

        val expected = """
            Simple Acros
            Film simulation: ACROS

            Film simulation
            • Film simulation: ACROS
        """.trimIndent()

        assertEquals(expected, result)
    }
}
