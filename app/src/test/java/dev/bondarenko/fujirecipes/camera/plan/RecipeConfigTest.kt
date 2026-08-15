package dev.bondarenko.fujirecipes.camera.plan

import dev.bondarenko.fujirecipes.data.fields.SensorGeneration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * FEAT-007 T-08.
 *
 * Mirrors `fuji-recipes-book/tests/unit/recipe-config-equal.spec.ts` @ `0c17106`.
 *
 * What is under test is the claim the import review makes to the user: "you already have this
 * one". Getting it wrong in either direction is bad in a different way — a false negative
 * fills the library with copies of itself, and a false positive hides a recipe the
 * photographer wanted.
 */
class RecipeConfigTest {

    private val base = mapOf<String, Any?>(
        "filmSimulation" to "classic-chrome",
        "dynamicRange" to "dr400",
        "highlightTone" to 1.5,
        "shadowTone" to -2.0,
        "color" to 2,
        "sharpness" to 0,
        "highIsoNR" to 0,
        "clarity" to 3,
        "grainEffect" to "strong",
        "grainSize" to "large",
        "colorChromeEffect" to "weak",
        "colorChromeFxBlue" to "strong",
        "whiteBalance" to "daylight",
        "wbShiftRed" to 2,
        "wbShiftBlue" to -3,
    )

    @Test
    fun `identical settings are the same configuration`() {
        assertTrue(areConfigsEqual(base, base.toMap()))
    }

    @Test
    fun `one differing field is a different configuration`() {
        assertFalse(areConfigsEqual(base, base + ("sharpness" to 1)))
    }

    @Test
    fun `a different film simulation is never the same configuration`() {
        assertFalse(areConfigsEqual(base, base + ("filmSimulation" to "velvia")))
    }

    /**
     * The load-bearing case. A slot read off a camera reports every property it holds; a
     * recipe typed into the form may omit anything left at its default. Without normalisation
     * every import would look new.
     */
    @Test
    fun `an omitted value equals its default`() {
        val omitted = base - "sharpness"

        assertTrue(areConfigsEqual(omitted, base), "omitting a default-valued field changed it")
    }

    @Test
    fun `an omitted value does not equal a non-default one`() {
        assertFalse(areConfigsEqual(base - "sharpness", base + ("sharpness" to 3)))
    }

    /** A camera read gives a Double for a tone; JSON may give an Int. Same setting. */
    @Test
    fun `numbers compare by value rather than by type`() {
        assertTrue(areConfigsEqual(base + ("color" to 2.0), base + ("color" to 2)))
        assertTrue(areConfigsEqual(base + ("highlightTone" to 1.5f), base))
    }

    /**
     * A monochrome simulation has no colour field at all, so two monochrome recipes that
     * differ only there are the same configuration — the camera would end up identical.
     */
    @Test
    fun `a field that does not apply to the simulation is not compared`() {
        val mono = base + ("filmSimulation" to "acros")

        assertTrue(areConfigsEqual(mono + ("color" to 4), mono + ("color" to -4)))
    }

    @Test
    fun `a field that does not apply to the generation is not compared`() {
        val withClarity = base + ("clarity" to 5)
        val withoutClarity = base - "clarity"

        // Clarity applies from X-Trans IV, so on an older body the two are the same.
        assertTrue(
            areConfigsEqual(withClarity, withoutClarity, SensorGeneration.XTRANS_III),
        )
        assertFalse(areConfigsEqual(withClarity, withoutClarity, SensorGeneration.XTRANS_V))
    }

    /** WR-06's four are recommendations no camera stores, so they cannot make a difference. */
    @Test
    fun `advisory fields are not part of the configuration`() {
        assertTrue(
            areConfigsEqual(
                base + ("isoMin" to 160) + ("exposureCompensation" to 0.7),
                base + ("isoMin" to 800),
            ),
        )
    }

    /** Grain size is meaningless when grain is off, so it must not split two identical looks. */
    @Test
    fun `grain size is not compared when grain is off`() {
        val off = base + ("grainEffect" to "off")

        assertTrue(areConfigsEqual(off + ("grainSize" to "large"), off + ("grainSize" to "small")))
    }

    @Test
    fun `the colour temperature counts only when the white balance asks for it`() {
        val daylight = base + ("colorTemperature" to 5500)
        val other = base + ("colorTemperature" to 8000)
        assertTrue(areConfigsEqual(daylight, other), "temperature counted under a daylight WB")

        val temp = base + ("whiteBalance" to "color-temp")
        assertFalse(
            areConfigsEqual(temp + ("colorTemperature" to 5500), temp + ("colorTemperature" to 8000)),
        )
    }

    // ─── Normalisation ──────────────────────────────────────────────────────

    @Test
    fun `normalisation fills applicable defaults and drops what does not apply`() {
        val normalised = normaliseSettings(mapOf("filmSimulation" to "acros"))

        assertEquals("acros", normalised["filmSimulation"])
        assertEquals(0.0, normalised["sharpness"], "an applicable field was not defaulted")
        assertNull(normalised["color"], "a monochrome recipe kept a colour field")
    }

    @Test
    fun `normalisation is stable`() {
        assertEquals(normaliseSettings(base), normaliseSettings(normaliseSettings(base)))
    }

    // ─── Finding a duplicate ────────────────────────────────────────────────

    private data class Entry(val id: String, val settings: Map<String, Any?>)

    @Test
    fun `finds the first library entry with the same configuration`() {
        val library = listOf(
            Entry("a", base + ("sharpness" to 4)),
            Entry("b", base.toMap()),
            Entry("c", base.toMap()),
        )

        val found = findDuplicateConfig(base, library, Entry::settings, Entry::id)

        assertEquals("b", found?.id)
    }

    @Test
    fun `finds nothing when the library has nothing like it`() {
        val library = listOf(Entry("a", base + ("filmSimulation" to "velvia")))

        assertNull(findDuplicateConfig(base, library, Entry::settings, Entry::id))
    }

    @Test
    fun `a recipe does not match itself when excluded`() {
        val library = listOf(Entry("a", base.toMap()))

        assertNull(
            findDuplicateConfig(base, library, Entry::settings, Entry::id, excludeId = "a"),
        )
    }

    @Test
    fun `an empty library finds nothing`() {
        assertNull(findDuplicateConfig(base, emptyList<Entry>(), Entry::settings))
    }
}
