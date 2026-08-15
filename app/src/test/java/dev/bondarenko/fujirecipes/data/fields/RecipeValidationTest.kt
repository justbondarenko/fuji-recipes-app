package dev.bondarenko.fujirecipes.data.fields

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The client's own refusals — FEAT-003 T-05.
 *
 * Mirrors `recipe.schema.json`. The server validates again and stays the authority; these
 * rules exist so a mistake is caught next to the control that caused it.
 */
class RecipeValidationTest {

    private fun settings(json: String): JsonObject = Json.parseToJsonElement(json).jsonObject

    private val context = FieldContext(
        generation = SensorGeneration.XTRANS_V,
        filmSimulationId = "classic-chrome",
        grainEffectOff = true,
        whiteBalanceId = "auto",
    )

    private fun field(id: String) = RecipeFields.byId(id) as NumberField

    @Test
    fun `a name is required`() {
        assertNotNull(RecipeValidation.validateName(""))
        assertNotNull(RecipeValidation.validateName("   "))
        assertNull(RecipeValidation.validateName("Kodachrome 64"))
    }

    @Test
    fun `a name has an upper bound`() {
        assertNull(RecipeValidation.validateName("a".repeat(80)))
        assertNotNull(RecipeValidation.validateName("a".repeat(81)))
    }

    @Test
    fun `notes have an upper bound`() {
        assertNull(RecipeValidation.validateNotes("a".repeat(2000)))
        assertNotNull(RecipeValidation.validateNotes("a".repeat(2001)))
    }

    @Test
    fun `a rating runs from zero to five`() {
        assertNull(RecipeValidation.validateRating(0))
        assertNull(RecipeValidation.validateRating(5))
        assertNotNull(RecipeValidation.validateRating(6))
        assertNotNull(RecipeValidation.validateRating(-1))
    }

    @Test
    fun `tags are bounded in count and length`() {
        assertNull(RecipeValidation.validateTags(List(20) { "tag$it" }))
        assertNotNull(RecipeValidation.validateTags(List(21) { "tag$it" }))
        assertNotNull(RecipeValidation.validateTags(listOf("a".repeat(31))))
        assertNotNull(RecipeValidation.validateTags(listOf("")))
    }

    @Test
    fun `a duplicate tag is refused`() {
        // Two identical tags narrow nothing and read as a mistake in the filter drawer.
        assertNotNull(RecipeValidation.validateTags(listOf("street", "street")))
    }

    @Test
    fun `a numeric value must sit inside its own range`() {
        val sharpness = field("sharpness")
        assertNull(RecipeValidation.validateNumber(sharpness, 4.0))
        assertNull(RecipeValidation.validateNumber(sharpness, -4.0))
        assertNotNull(RecipeValidation.validateNumber(sharpness, 5.0))
    }

    @Test
    fun `a typed colour temperature outside the range is caught before any request`() {
        // The steppers clamp, so this is only reachable by typing — which is exactly why
        // the field is editable and therefore why this check has to exist.
        val temperature = field("colorTemperature")
        assertNull(RecipeValidation.validateNumber(temperature, 7200.0))
        assertNotNull(RecipeValidation.validateNumber(temperature, 99000.0))
        assertNotNull(RecipeValidation.validateNumber(temperature, 1000.0))
    }

    @Test
    fun `an unset advisory bound is allowed`() {
        // `isoMin` has a null default: "no minimum" is a legitimate state.
        assertNull(RecipeValidation.validateNumber(field("isoMin"), null))
    }

    @Test
    fun `a range message reads as integers, not as doubles`() {
        val problem = assertNotNull(RecipeValidation.validateNumber(field("sharpness"), 9.0))
        assertTrue(problem.message.contains("-4"))
        assertTrue(problem.message.contains("4"))
        assertTrue(!problem.message.contains("4.0"))
    }

    @Test
    fun `a well-formed recipe has nothing wrong with it`() {
        val problems = RecipeValidation.validate(
            name = "Kodachrome 64",
            notes = "",
            rating = 5,
            tags = listOf("street"),
            settings = settings("""{"sharpness":2,"clarity":1}"""),
            context = context,
        )
        assertEquals(emptyList(), problems)
    }

    @Test
    fun `problems are addressed to the field that caused them`() {
        val problems = RecipeValidation.validate(
            name = "",
            notes = "",
            rating = 9,
            tags = emptyList(),
            settings = settings("""{"sharpness":99}"""),
            context = context,
        )
        assertEquals(
            setOf(RecipeValidation.NAME_FIELD, RecipeValidation.RATING_FIELD, "sharpness"),
            problems.map { it.fieldId }.toSet(),
        )
    }

    @Test
    fun `a value left behind on an inapplicable field does not block a save`() {
        // The recipe was colour once; it is monochrome now. `color` is not shown, is not
        // written, and a stale out-of-range value on it must not be a reason to refuse.
        val monochrome = context.copy(filmSimulationId = "acros")
        val problems = RecipeValidation.validate(
            name = "Acros Night",
            notes = "",
            rating = 0,
            tags = emptyList(),
            settings = settings("""{"color":99}"""),
            context = monochrome,
        )
        assertEquals(emptyList(), problems)
    }
}
