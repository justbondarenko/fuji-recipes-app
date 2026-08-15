package dev.bondarenko.fujirecipes.data.text

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Ported from `fuji-recipes-book/tests/unit/recipe-text-parser.spec.ts`.
 *
 * The same paste has to become the same recipe on both clients, so the cases are the
 * sibling's cases rather than new ones.
 */
class RecipeTextParserTest {

    private fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.content

    private fun shifts(line: String): Pair<String?, String?> {
        val s = parseRecipeText(line).settings
        return s.str("wbShiftRed") to s.str("wbShiftBlue")
    }

    @Test
    fun `reads a Fuji X Weekly paste, bullets and all`() {
        val parsed = parseRecipeText(
            """
            Film Simulation: Classic Chrome
            •  Dynamic Range: DR400
            •  Color Chrome Effect: Strong
            •  Color Chrome FX Blue: Weak
            •  Grain: Off
            •  White Balance: 6300k to 7500k 0R, 0B
            •  Highlights: 0
            •  Shadows: -0.5
            •  Color: +2
            •  Sharpness: +1
            •  High ISO NR: -4
            •  Clarity: 0
            """.trimIndent(),
        )

        val s = parsed.settings
        assertEquals("classic-chrome", s.str("filmSimulation"))
        assertEquals("dr400", s.str("dynamicRange"))
        assertEquals("strong", s.str("colorChromeEffect"))
        assertEquals("weak", s.str("colorChromeFxBlue"))
        assertEquals("off", s.str("grainEffect"))
        assertEquals("color-temp", s.str("whiteBalance"))
        assertEquals("6300", s.str("colorTemperature"))
        assertEquals("0", s.str("wbShiftRed"))
        assertEquals("0", s.str("wbShiftBlue"))
        assertEquals("0", s.str("highlightTone"))
        assertEquals("-0.5", s.str("shadowTone"))
        assertEquals("2", s.str("color"))
        assertEquals("1", s.str("sharpness"))
        assertEquals("-4", s.str("highIsoNR"))
        assertEquals("0", s.str("clarity"))
        assertEquals(15, parsed.fieldsFound)
    }

    @Test
    fun `reads the unpunctuated, British-spelled variant`() {
        val parsed = parseRecipeText(
            """
            Film Simulation Classic Negative
            Dynamic Range DR400
            Grain Effect Strong / Large
            White Balance Auto R:+4 B:-5
            Highlight -2
            Shadow +1
            Colour +4
            Sharpness 0
            Noise Reduction -4
            Clarity -4
            Colour Chrome Strong
            CC FX Blue Strong
            """.trimIndent(),
        )

        val s = parsed.settings
        assertEquals("classic-negative", s.str("filmSimulation"))
        assertEquals("strong", s.str("grainEffect"))
        assertEquals("large", s.str("grainSize"))
        assertEquals("auto", s.str("whiteBalance"))
        assertEquals("4", s.str("wbShiftRed"))
        assertEquals("-5", s.str("wbShiftBlue"))
        assertEquals("-2", s.str("highlightTone"))
        assertEquals("1", s.str("shadowTone"))
        assertEquals("4", s.str("color"))
        assertEquals("-4", s.str("highIsoNR"))
        assertEquals("strong", s.str("colorChromeEffect"))
        assertEquals("strong", s.str("colorChromeFxBlue"))
        assertEquals(15, parsed.fieldsFound)
    }

    /**
     * The sign is the half that has to survive: `-5 Red` read as `5` is not a wrong number,
     * it is a shift towards cyan instead of red.
     */
    @Test
    fun `keeps the sign whichever side of the label the number sits`() {
        assertEquals("-1" to "-5", shifts("White Balance: Auto, -1 Red & -5 Blue"))
        assertEquals("-5" to "5", shifts("White Balance: 5500K, -5 Red & +5 Blue"))
        assertEquals("2" to "-3", shifts("WB: Auto, +2 Red, -3 Blue"))
        assertEquals("2" to "-1", shifts("White Balance Auto, +2 R & -1 B"))
        assertEquals("4" to "-5", shifts("White Balance Auto R:+4 B:-5"))
        assertEquals("-3" to "-7", shifts("White Balance: Auto, R -3, B -7"))
        assertEquals("4" to "-5", shifts("WB: Auto R+4 B-5"))
        assertEquals("-5" to "3", shifts("WB Shift: Red -5 / Blue +3"))
        assertEquals("-1" to "-5", shifts("white balance: auto, -1 red & -5 blue"))
        assertEquals("0" to "0", shifts("White Balance: 6300k to 7500k 0R, 0B"))
    }

    @Test
    fun `does not take a shift out of the colour temperature`() {
        assertEquals(null to null, shifts("White Balance: 5500K"))

        val kelvin = parseRecipeText("White Balance: Kelvin 5500, Red +1, Blue -1").settings
        assertEquals("5500", kelvin.str("colorTemperature"))
        assertEquals("1", kelvin.str("wbShiftRed"))
        assertEquals("-1", kelvin.str("wbShiftBlue"))
    }

    @Test
    fun `clamps a shift to the range the form accepts`() {
        assertEquals("-9" to "9", shifts("White Balance: Auto, -12 Red & +15 Blue"))
    }

    @Test
    fun `reads monochromatic colour on both axes, either way round`() {
        val numberFirst = parseRecipeText("Monochromatic Color: -6 WC, +2 MG").settings
        assertEquals("-6", numberFirst.str("monochromaticColorWc"))
        assertEquals("2", numberFirst.str("monochromaticColorMg"))

        val labelFirst = parseRecipeText("Monochromatic Color: WC -6, MG +2").settings
        assertEquals("-6", labelFirst.str("monochromaticColorWc"))
        assertEquals("2", labelFirst.str("monochromaticColorMg"))
    }

    @Test
    fun `takes the name from the first line, or from an explicit one`() {
        val fromFirstLine = parseRecipeText(
            """
            CineStill 800T
            Film Simulation: Eterna
            Dynamic Range: DR400
            """.trimIndent(),
        )
        assertEquals("CineStill 800T", fromFirstLine.name)
        assertEquals("eterna", fromFirstLine.settings.str("filmSimulation"))

        val explicit = parseRecipeText(
            """
            Name: Kodachrome 64
            Film Simulation: Classic Chrome
            """.trimIndent(),
        )
        assertEquals("Kodachrome 64", explicit.name)
    }

    @Test
    fun `carries the signs through a whole pasted recipe`() {
        val parsed = parseRecipeText(
            """
            Kodachrome 64
            Film Simulation: Classic Chrome
            Dynamic Range: DR400
            White Balance: Auto, -2 Red & -3 Blue
            Highlight: +1
            Shadow: -2
            Color: +4
            Sharpness: -1
            """.trimIndent(),
        )

        assertEquals("Kodachrome 64", parsed.name)
        assertEquals("-2", parsed.settings.str("wbShiftRed"))
        assertEquals("-3", parsed.settings.str("wbShiftBlue"))
        assertEquals("1", parsed.settings.str("highlightTone"))
        assertEquals("-2", parsed.settings.str("shadowTone"))
    }

    /** The web client's own ordering makes its negative branches unreachable. Ours are not. */
    @Test
    fun `reads a negative third of a stop as negative`() {
        assertEquals("-0.333", parseRecipeText("Exposure Compensation: -1/3").settings.str("exposureCompensation"))
        assertEquals("0.667", parseRecipeText("Exposure Compensation: +2/3").settings.str("exposureCompensation"))
    }

    @Test
    fun `finds nothing in text that is not a recipe`() {
        val parsed = parseRecipeText("   \n  \n")
        assertNull(parsed.name)
        assertEquals(0, parsed.fieldsFound)
    }
}
