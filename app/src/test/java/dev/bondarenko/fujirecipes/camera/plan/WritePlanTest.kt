package dev.bondarenko.fujirecipes.camera.plan

import dev.bondarenko.fujirecipes.data.fields.SensorGeneration
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * FEAT-006 T-05 — one case per rule, named for it.
 *
 * Mirrors `fuji-recipes-book/tests/unit/write-plan.spec.ts` @ `0c17106`.
 *
 * This is the highest-value suite in the project and it needs no camera. `field-definitions.md`
 * WR-09 makes the plan pure for exactly this reason: it is the last piece of logic before
 * bytes change a slot, and exhaustively is the only acceptable standard for it.
 */
class WritePlanTest {

    private fun settings(json: String): JsonObject =
        Json.parseToJsonElement(json).jsonObject

    private val fullRecipe = settings(
        """
        {
          "filmSimulation": "classic-chrome",
          "dynamicRange": "dr400",
          "highlightTone": 1.5,
          "shadowTone": -2,
          "color": 2,
          "sharpness": -1,
          "highIsoNR": -4,
          "clarity": 3,
          "grainEffect": "strong",
          "grainSize": "large",
          "colorChromeEffect": "weak",
          "colorChromeFxBlue": "strong",
          "whiteBalance": "daylight",
          "wbShiftRed": 2,
          "wbShiftBlue": -3
        }
        """.trimIndent(),
    )

    private fun plan(
        settings: JsonObject = fullRecipe,
        generation: SensorGeneration = SensorGeneration.XTRANS_V,
        slot: Int = 3,
        name: String = "Kodachrome 64",
        recipeGeneration: SensorGeneration? = null,
    ) = buildWritePlan(slot, generation, name, settings, recipeGeneration)

    private fun WritePlan.step(id: String) = steps.firstOrNull { it.id == id }
    private fun WritePlan.droppedIds() = dropped.map { it.id }

    // ─── WR-01: refusals come first ─────────────────────────────────────────

    @Test
    fun `WR-01 - a body with no custom slots is refused, with no steps at all`() {
        val refused = plan(generation = SensorGeneration.XTRANS_III)

        assertNotNull(refused.refusal)
        assertTrue(refused.refusal!!.contains("no custom slots"))
        assertTrue(refused.steps.isEmpty(), "a refused plan must not be partially written")
        assertEquals(0, refused.total)
    }

    @Test
    fun `WR-01 - a Bayer CMOS body is refused too`() {
        assertNotNull(plan(generation = SensorGeneration.CMOS).refusal)
    }

    @Test
    fun `a slot outside C1 to C7 is refused and names the range`() {
        listOf(0, 8, -1).forEach { slot ->
            val refused = plan(slot = slot)
            assertNotNull(refused.refusal, "slot $slot was not refused")
            assertTrue(refused.refusal!!.contains("C1"))
        }
    }

    // ─── Plan shape ─────────────────────────────────────────────────────────

    @Test
    fun `the slot and the name are the first two steps, and both are fatal`() {
        val built = plan(slot = 5)

        assertEquals("slot", built.steps[0].id)
        assertEquals(PRESET_SLOT_PROPERTY, built.steps[0].property)
        assertEquals(5, built.steps[0].value)
        assertTrue(built.steps[0].fatal)

        assertEquals("name", built.steps[1].id)
        assertEquals(PRESET_NAME_PROPERTY, built.steps[1].property)
        assertEquals("Kodachrome 64", built.steps[1].value)
        assertTrue(built.steps[1].fatal)
    }

    /** A refused setting costs one setting; a refused slot select costs the whole write. */
    @Test
    fun `no setting step is fatal`() {
        assertTrue(plan().steps.drop(2).none { it.fatal })
    }

    @Test
    fun `the film simulation precedes every other setting`() {
        val settingIds = plan().steps.drop(2).map { it.id }

        assertEquals("filmSimulation", settingIds.first())
    }

    /** WR-09: same inputs, same ordered output. */
    @Test
    fun `the plan is pure`() {
        assertEquals(plan(), plan())
    }

    @Test
    fun `total agrees with the steps a progress display will count`() {
        val built = plan()

        assertEquals(built.steps.size, built.total)
    }

    @Test
    fun `a long name is shortened and says so rather than failing the fatal name step`() {
        val built = plan(name = "A recipe name well past the camera's slot label limit")

        assertEquals(PRESET_NAME_MAX_CHARS, (built.steps[1].value as String).length)
        assertTrue(built.notes.any { it.contains("shortened") })
    }

    // ─── WR-02 ──────────────────────────────────────────────────────────────

    @Test
    fun `WR-02 - the colour temperature is written immediately after the white balance`() {
        val built = plan(
            settings = settings(
                """{"filmSimulation":"provia","whiteBalance":"color-temp","colorTemperature":5500}""",
            ),
        )

        val ids = built.steps.map { it.id }
        assertEquals(ids.indexOf("whiteBalance") + 1, ids.indexOf("colorTemperature"))
    }

    /**
     * Skipped, not dropped. The camera would accept it; it is the recipe's own white-balance
     * mode that makes it meaningless, and a warning about it would appear on almost every
     * recipe for ever.
     */
    @Test
    fun `a colour temperature without the colour-temp mode is skipped silently`() {
        val built = plan(
            settings = settings(
                """{"filmSimulation":"provia","whiteBalance":"daylight","colorTemperature":5500}""",
            ),
        )

        assertNull(built.step("colorTemperature"))
        assertFalse(built.droppedIds().contains("colorTemperature"))
    }

    // ─── WR-03 ──────────────────────────────────────────────────────────────

    @Test
    fun `WR-03 - a monochrome simulation drops colour, and says why`() {
        val built = plan(
            settings = settings("""{"filmSimulation":"acros","color":2,"sharpness":1}"""),
        )

        assertNull(built.step("color"))
        val drop = built.dropped.first { it.id == "color" }
        assertTrue(drop.reason.contains("monochrome"))
        // The rest of the recipe still goes.
        assertNotNull(built.step("sharpness"))
    }

    // ─── WR-04 ──────────────────────────────────────────────────────────────

    @Test
    fun `WR-04 - grain size never gets a step of its own, and grain off encodes alone`() {
        val built = plan(settings = settings("""{"filmSimulation":"provia","grainEffect":"off"}"""))

        assertNull(built.step("grainSize"))
        assertEquals(1, built.step("grainEffect")?.value)
    }

    @Test
    fun `grain strength and size arrive as one combined value`() {
        assertEquals(5, plan().step("grainEffect")?.value)
    }

    // ─── WR-05 ──────────────────────────────────────────────────────────────

    @Test
    fun `WR-05 - monochromatic colour is written on X-Trans V with a monochrome simulation`() {
        val built = plan(
            settings = settings(
                """{"filmSimulation":"acros","monochromaticColorWc":3,"monochromaticColorMg":-2}""",
            ),
        )

        assertEquals(30, built.step("monochromaticColorWc")?.value)
        assertEquals(-20, built.step("monochromaticColorMg")?.value)
    }

    @Test
    fun `WR-05 - a colour simulation drops it, naming the simulation as the cause`() {
        val built = plan(
            settings = settings("""{"filmSimulation":"provia","monochromaticColorWc":3}"""),
        )

        val drop = built.dropped.first { it.id == "monochromaticColorWc" }
        assertTrue(drop.reason.contains("monochrome film simulation"))
    }

    @Test
    fun `WR-05 - an older body drops it, naming the body as the cause`() {
        val built = plan(
            settings = settings("""{"filmSimulation":"acros","monochromaticColorWc":3}"""),
            generation = SensorGeneration.XTRANS_IV,
        )

        val drop = built.dropped.first { it.id == "monochromaticColorWc" }
        assertTrue(drop.reason.contains("X-Trans IV"))
        assertFalse(drop.reason.contains("film simulation"))
    }

    /** The detail WR-05 does not record: the camera rejects a written zero. */
    @Test
    fun `WR-05 - a monochromatic colour of zero is omitted rather than written as zero`() {
        val built = plan(
            settings = settings(
                """{"filmSimulation":"acros","monochromaticColorWc":0,"monochromaticColorMg":0}""",
            ),
        )

        assertNull(built.step("monochromaticColorWc"))
        assertNull(built.step("monochromaticColorMg"))
        assertFalse(built.droppedIds().contains("monochromaticColorWc"))
    }

    // ─── WR-06 ──────────────────────────────────────────────────────────────

    /**
     * Never written **and** never reported. A warning that appears on every write about
     * something that was never going to be written is a warning that stops being read.
     */
    @Test
    fun `WR-06 - advisory fields are neither written nor reported as dropped`() {
        val built = plan(
            settings = settings(
                """
                {
                  "filmSimulation": "provia",
                  "dRangePriority": "weak",
                  "exposureCompensation": 0.7,
                  "isoMin": 160,
                  "isoMax": 6400
                }
                """.trimIndent(),
            ),
        )

        listOf("dRangePriority", "exposureCompensation", "isoMin", "isoMax").forEach { id ->
            assertNull(built.step(id), "$id was written")
            assertFalse(built.droppedIds().contains(id), "$id was reported as dropped")
        }
    }

    // ─── WR-07 ──────────────────────────────────────────────────────────────

    @Test
    fun `WR-07 - a field the body's generation cannot take is dropped and reported`() {
        val built = plan(
            settings = settings("""{"filmSimulation":"provia","clarity":3}"""),
            generation = SensorGeneration.XTRANS_IV,
        )

        // Clarity applies from X-Trans IV, so it is present here…
        assertNotNull(built.step("clarity"))
    }

    @Test
    fun `WR-07 - the predicate is evaluated against the camera, not the recipe`() {
        val built = plan(
            settings = settings("""{"filmSimulation":"acros","monochromaticColorWc":5}"""),
            generation = SensorGeneration.XTRANS_IV,
        )

        assertNull(built.step("monochromaticColorWc"))
        assertTrue(built.droppedIds().contains("monochromaticColorWc"))
    }

    // ─── WR-08 ──────────────────────────────────────────────────────────────

    @Test
    fun `WR-08 - the noise reduction goes as its looked-up bit pattern, unsigned`() {
        val step = plan().step("highIsoNR")

        assertEquals(0x8000, step?.value)
        assertEquals(StepKind.U16, step?.kind)
    }

    // ─── WR-10 ──────────────────────────────────────────────────────────────

    @Test
    fun `WR-10 - a simulation the body does not have is dropped, not sent as another`() {
        val built = plan(
            settings = settings("""{"filmSimulation":"nostalgic-negative","sharpness":1}"""),
            generation = SensorGeneration.XTRANS_IV,
        )

        assertNull(built.step("filmSimulation"))
        val drop = built.dropped.first { it.id == "filmSimulation" }
        assertTrue(drop.reason.contains("keep the simulation it already has"))
        // The rest of the recipe is still written — this is a warning, not a refusal.
        assertNotNull(built.step("sharpness"))
    }

    @Test
    fun `WR-10 - a simulation the body does have is written`() {
        assertEquals(0x0b, plan().step("filmSimulation")?.value)
    }

    @Test
    fun `an unknown simulation is dropped rather than sent as a guess`() {
        val built = plan(settings = settings("""{"filmSimulation":"velvia-ii"}"""))

        assertNull(built.step("filmSimulation"))
        assertTrue(built.droppedIds().contains("filmSimulation"))
    }

    // ─── Missing values ─────────────────────────────────────────────────────

    @Test
    fun `a null or absent value is skipped rather than written as a default`() {
        val built = plan(settings = settings("""{"filmSimulation":"provia","sharpness":null}"""))

        assertNull(built.step("sharpness"))
        assertFalse(built.droppedIds().contains("sharpness"))
    }

    @Test
    fun `an empty recipe still writes the slot and the name`() {
        val built = plan(settings = settings("{}"))

        assertEquals(2, built.steps.size)
        assertNull(built.refusal)
    }

    @Test
    fun `a value this build cannot encode is dropped with the value named`() {
        val built = plan(settings = settings("""{"filmSimulation":"provia","dynamicRange":"dr-auto"}"""))

        val drop = built.dropped.first { it.id == "dynamicRange" }
        assertTrue(drop.reason.contains("Auto"), "the reason should name the value: ${drop.reason}")
    }

    // ─── Compatibility ──────────────────────────────────────────────────────

    @Test
    fun `a recipe made for a wider body is flagged incompatible`() {
        val built = plan(
            generation = SensorGeneration.XTRANS_IV,
            recipeGeneration = SensorGeneration.XTRANS_V,
        )

        assertTrue(built.incompatible)
    }

    @Test
    fun `a recipe with nothing dropped is compatible`() {
        assertFalse(plan().incompatible)
    }

    /** The GFX field set is an assumption, and the plan says so instead of implying it is not. */
    @Test
    fun `an unverified generation carries the assumption as a note`() {
        val built = plan(generation = SensorGeneration.GFX)

        assertTrue(built.notes.any { it.contains("has not been verified") })
        assertNull(built.refusal)
    }

    @Test
    fun `a verified generation carries no such note`() {
        assertTrue(plan().notes.none { it.contains("has not been verified") })
    }

    // ─── Every step is packable ─────────────────────────────────────────────

    @Test
    fun `every step a full recipe produces fits the width it declares`() {
        plan().steps.forEach { step ->
            when (step.kind) {
                StepKind.STRING -> assertTrue(step.value is String)
                StepKind.I16 -> assertTrue((step.value as Int) in -32768..32767, step.id)
                StepKind.U16 -> assertTrue((step.value as Int) in 0..0xffff, step.id)
            }
        }
    }
}
