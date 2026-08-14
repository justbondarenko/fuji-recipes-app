package dev.bondarenko.fujirecipes.data.fields

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The display rules — FEAT-002 T-06.
 *
 * One test per rule in `fuji-recipes-book/specs/shared/field-definitions.md` §5, plus the
 * applicability rules from §4. These are what make "reads like the web client" a checked
 * claim: every one of them is a small consistency that costs real time in the field when it
 * slips, and none of them is visible in a screenshot review.
 */
class FieldFormattingTest {

    private fun settings(json: String): JsonObject = Json.parseToJsonElement(json).jsonObject

    private fun context(
        simulation: String = "classic-chrome",
        generation: SensorGeneration = SensorGeneration.XTRANS_V,
        grainOff: Boolean = true,
        whiteBalance: String? = "auto",
    ) = FieldContext(generation, simulation, grainOff, whiteBalance)

    private fun field(id: String) = requireNotNull(RecipeFields.byId(id))

    private fun rowFor(id: String, json: String, context: FieldContext = context()) =
        FieldFormatting.rowsFor(settings(json), context).firstOrNull { it.fieldId == id }

    // --- §5 display rules ---

    @Test
    fun `enums render their label, never their id`() {
        assertEquals("DR400", rowFor("dynamicRange", """{"dynamicRange":"dr400"}""")?.value)
    }

    @Test
    fun `non-zero numerics carry an explicit sign`() {
        assertEquals("+2", rowFor("sharpness", """{"sharpness":2}""")?.value)
        assertEquals("-1", rowFor("sharpness", """{"sharpness":-1}""")?.value)
    }

    @Test
    fun `zero is bare, not plus zero`() {
        // Zero is the default for every signed parameter, so it is the value most often on
        // screen; "+0" would claim a nudge that never happened.
        assertEquals("0", rowFor("sharpness", """{"sharpness":0}""")?.value)
    }

    @Test
    fun `half steps show one decimal`() {
        assertEquals("+1.5", rowFor("highlightTone", """{"highlightTone":1.5}""")?.value)
        assertEquals("-0.5", rowFor("highlightTone", """{"highlightTone":-0.5}""")?.value)
    }

    @Test
    fun `a whole value on a half-step field stays whole`() {
        assertEquals("+2", rowFor("highlightTone", """{"highlightTone":2}""")?.value)
    }

    @Test
    fun `an off state says Off`() {
        assertEquals("Off", rowFor("grainEffect", """{"grainEffect":"off"}""")?.value)
    }

    @Test
    fun `colour temperature has no space before K`() {
        val row = rowFor(
            "colorTemperature",
            """{"whiteBalance":"color-temp","colorTemperature":5500}""",
            context(whiteBalance = "color-temp"),
        )
        assertEquals("5500K", row?.value)
    }

    @Test
    fun `white balance shift is one combined row`() {
        val rows = FieldFormatting.rowsFor(
            settings("""{"wbShiftRed":3,"wbShiftBlue":-2}"""),
            context(),
        )
        val shift = rows.single { it.fieldId == FieldFormatting.WB_SHIFT_ROW_ID }
        assertEquals("R +3 / B -2", shift.value)
        // And the two source fields are not also listed.
        assertTrue(rows.none { it.fieldId == "wbShiftRed" || it.fieldId == "wbShiftBlue" })
    }

    @Test
    fun `an unset advisory field omits its row entirely`() {
        val rows = FieldFormatting.rowsFor(settings("""{"sharpness":1}"""), context())
        assertTrue(rows.none { it.fieldId == "isoMin" })
        assertTrue(rows.none { it.fieldId == "isoMax" })
    }

    @Test
    fun `a set ISO bound renders as a plain magnitude`() {
        // Not "+3200": an ISO floor is a magnitude, not an offset from anything.
        assertEquals("3200", rowFor("isoMin", """{"isoMin":3200}""")?.value)
    }

    @Test
    fun `an unknown enum value renders as its raw id rather than vanishing`() {
        // A newer client may write a value this build has never heard of. Dropping the row
        // would silently hide part of the recipe.
        assertEquals(
            "dr800",
            rowFor("dynamicRange", """{"dynamicRange":"dr800"}""")?.value,
        )
    }

    // --- default versus changed ---

    @Test
    fun `a value at its default is marked as such`() {
        assertTrue(rowFor("sharpness", """{"sharpness":0}""")!!.isDefault)
        assertFalse(rowFor("sharpness", """{"sharpness":2}""")!!.isDefault)
    }

    @Test
    fun `an absent value counts as the default`() {
        // A recipe written before a field existed did not choose anything, and showing it
        // as a deliberate change would misrepresent the recipe.
        assertTrue(FieldFormatting.isDefault(field("sharpness"), null))
    }

    @Test
    fun `the combined shift row is a default only when both halves are zero`() {
        assertTrue(FieldFormatting.wbShiftRow(settings("""{}"""))!!.isDefault)
        assertFalse(FieldFormatting.wbShiftRow(settings("""{"wbShiftRed":1}"""))!!.isDefault)
    }

    // --- §4 applicability ---

    @Test
    fun `colour is omitted for a monochrome simulation`() {
        val rows = FieldFormatting.rowsFor(
            settings("""{"color":2}"""),
            context(simulation = "acros"),
        )
        assertTrue(rows.none { it.fieldId == "color" })
    }

    @Test
    fun `monochromatic colour appears only for a monochrome simulation`() {
        val mono = FieldFormatting.rowsFor(settings("{}"), context(simulation = "acros"))
        val colour = FieldFormatting.rowsFor(settings("{}"), context(simulation = "velvia"))

        assertTrue(mono.any { it.fieldId == "monochromaticColorWc" })
        assertTrue(colour.none { it.fieldId == "monochromaticColorWc" })
    }

    @Test
    fun `grain size is hidden while the grain effect is off`() {
        val off = FieldFormatting.rowsFor(settings("{}"), context(grainOff = true))
        val on = FieldFormatting.rowsFor(settings("{}"), context(grainOff = false))

        assertTrue(off.none { it.fieldId == "grainSize" })
        assertTrue(on.any { it.fieldId == "grainSize" })
    }

    @Test
    fun `colour temperature appears only when white balance is set to it`() {
        val daylight = FieldFormatting.rowsFor(
            settings("""{"colorTemperature":5500}"""),
            context(whiteBalance = "daylight"),
        )
        assertTrue(daylight.none { it.fieldId == "colorTemperature" })
    }

    @Test
    fun `an older generation loses the fields it never had`() {
        val third = FieldFormatting.rowsFor(
            settings("""{"clarity":2}"""),
            context(generation = SensorGeneration.XTRANS_III),
        )
        assertTrue(third.none { it.fieldId == "clarity" })
        assertTrue(third.none { it.fieldId == "colorChromeFxBlue" })
    }

    @Test
    fun `a recipe with no stored generation is treated as the widest field set`() {
        // migration 0002 dropped the column, so this is the common case now. Assuming
        // narrower would hide fields a recipe legitimately uses.
        assertEquals(SensorGeneration.XTRANS_V, RecipeFields.generationOf(null))
        assertEquals(SensorGeneration.XTRANS_IV, RecipeFields.generationOf("xtrans-iv"))
    }

    @Test
    fun `half steps exist on X-Trans V and not on older bodies`() {
        val tone = field("highlightTone") as NumberField
        assertEquals(0.5, tone.stepFor(SensorGeneration.XTRANS_V))
        assertEquals(1.0, tone.stepFor(SensorGeneration.XTRANS_IV))
    }

    // --- the table itself ---

    @Test
    fun `every settings field from the document is present`() {
        val expected = setOf(
            "filmSimulation", "dynamicRange",
            "highlightTone", "shadowTone", "color", "sharpness", "highIsoNR", "clarity",
            "grainEffect", "grainSize", "colorChromeEffect", "colorChromeFxBlue",
            "whiteBalance", "colorTemperature", "wbShiftRed", "wbShiftBlue",
            "monochromaticColorWc", "monochromaticColorMg",
            "dRangePriority", "exposureCompensation", "isoMin", "isoMax",
        )
        assertEquals(expected, RecipeFields.all.map { it.id }.toSet())
    }

    @Test
    fun `the shooting group is advisory and nothing else is`() {
        // "Advisory" means no camera ever takes it. Getting this wrong would either warn
        // about a parameter that was never going to be written, or silently drop one that
        // should have been.
        RecipeFields.all.forEach { field ->
            assertEquals(
                field.group == FieldGroup.SHOOTING,
                field.advisory,
                "${field.id} advisory flag disagrees with its group",
            )
        }
    }

    @Test
    fun `rows come back in the document's group order`() {
        val rows = FieldFormatting.rowsFor(
            settings("""{"whiteBalance":"color-temp","colorTemperature":5500}"""),
            context(simulation = "acros", grainOff = false, whiteBalance = "color-temp"),
        )
        val groups = rows.mapNotNull { row ->
            RecipeFields.byId(row.fieldId)?.group
                ?: FieldGroup.WHITE_BALANCE.takeIf { row.fieldId == FieldFormatting.WB_SHIFT_ROW_ID }
        }.distinct()

        assertEquals(
            listOf(
                FieldGroup.SIMULATION, FieldGroup.TONE, FieldGroup.EFFECTS,
                FieldGroup.WHITE_BALANCE, FieldGroup.MONOCHROMATIC, FieldGroup.SHOOTING,
            ),
            groups,
        )
    }
}
