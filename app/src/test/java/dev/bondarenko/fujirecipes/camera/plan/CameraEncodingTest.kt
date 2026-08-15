package dev.bondarenko.fujirecipes.camera.plan

import dev.bondarenko.fujirecipes.data.fields.FilmSimulations
import dev.bondarenko.fujirecipes.data.fields.RecipeFields
import dev.bondarenko.fujirecipes.data.fields.SensorGeneration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * FEAT-006 T-02.
 *
 * Mirrors `fuji-recipes-book/tests/unit/camera-encoding.spec.ts` @ `0c17106`, and checks the
 * table against `field-definitions.md` §4 and §7 rather than against itself — a test that
 * reads the same constant it is testing proves only that Kotlin can compare a value to
 * itself.
 *
 * The cases that earn their keep are the three encodings that differ from the RAW-conversion
 * profile's for the same values. Crossing them writes a recipe that looks right and is wrong,
 * which is the one failure mode no amount of care at the transport layer catches.
 */
class CameraEncodingTest {

    // ─── Coverage ───────────────────────────────────────────────────────────

    /**
     * Exhaustive by construction: a field added to `RecipeFields` without a mapping fails
     * here rather than being silently skipped by the write plan.
     */
    @Test
    fun `every field in the source has a mapping`() {
        RecipeFields.all.forEach { field ->
            assertNotNull(FIELD_PROPERTIES[field.id], "${field.id} has no property mapping")
        }
    }

    @Test
    fun `every mapping names a field the source has`() {
        FIELD_PROPERTIES.keys.forEach { id ->
            assertNotNull(RecipeFields.byId(id), "$id is mapped but is not a field")
        }
    }

    /** A field with no code must say why, or a dropped-field notice has nothing to show. */
    @Test
    fun `every unwritable field carries a reason`() {
        FIELD_PROPERTIES.filterValues { it.code == null }.forEach { (id, mapping) ->
            assertNotNull(mapping.unwritableBecause, "$id has no code and no reason")
        }
    }

    /** WR-06: the four advisory fields are exactly the ones with no code. */
    @Test
    fun `the fields with no code are the advisory ones`() {
        val codeless = FIELD_PROPERTIES.filterValues { it.code == null }.keys

        assertEquals(
            setOf("dRangePriority", "exposureCompensation", "isoMin", "isoMax"),
            codeless,
        )
    }

    @Test
    fun `every property code sits inside the custom-slot block`() {
        FIELD_PROPERTIES.values.mapNotNull { it.code }.forEach { code ->
            assertTrue(code in 0xd18e..0xd1a5, "0x${code.toString(16)} is outside the block")
        }
    }

    // ─── Write order ────────────────────────────────────────────────────────

    /** So a mode-dependent rejection surfaces immediately, not after twelve good writes. */
    @Test
    fun `the film simulation is written first`() {
        assertEquals("filmSimulation", WRITE_ORDER.first())
    }

    /** WR-02, and the captures are stricter than the rule: *immediately* after. */
    @Test
    fun `the colour temperature immediately follows the white balance mode`() {
        val mode = WRITE_ORDER.indexOf("whiteBalance")
        val temperature = WRITE_ORDER.indexOf("colorTemperature")

        assertEquals(mode + 1, temperature)
    }

    @Test
    fun `the white-balance shifts come after the temperature`() {
        val temperature = WRITE_ORDER.indexOf("colorTemperature")

        assertTrue(WRITE_ORDER.indexOf("wbShiftRed") > temperature)
        assertTrue(WRITE_ORDER.indexOf("wbShiftBlue") > temperature)
    }

    @Test
    fun `the write order carries no field twice and no unwritable field`() {
        assertEquals(WRITE_ORDER.size, WRITE_ORDER.toSet().size)
        WRITE_ORDER.forEach { id ->
            assertNotNull(FIELD_PROPERTIES[id]?.code, "$id is ordered but has no code")
        }
    }

    /** Grain size never gets its own step: its value rides in the grain effect's code. */
    @Test
    fun `grain size is not ordered separately`() {
        assertFalse(WRITE_ORDER.contains("grainSize"))
        assertEquals(FIELD_PROPERTIES["grainEffect"]?.code, FIELD_PROPERTIES["grainSize"]?.code)
    }

    // ─── WR-08 ──────────────────────────────────────────────────────────────

    /**
     * The table is not monotonic and not arithmetic: +2 encodes as `0x0000`, below −4's
     * `0x8000` and below +1's `0x1000`. Any formula that appears to fit is a coincidence over
     * part of the range.
     */
    @Test
    fun `high ISO NR maps through the table rather than arithmetic`() {
        assertEquals(0x8000, encodeValue("highIsoNR", -4))
        assertEquals(0x2000, encodeValue("highIsoNR", 0))
        assertEquals(0x0000, encodeValue("highIsoNR", 2))
        assertEquals(0x5000, encodeValue("highIsoNR", 4))

        assertTrue(
            HIGH_ISO_NR_CODES.getValue(2) < HIGH_ISO_NR_CODES.getValue(1),
            "the table has been 'fixed' into something monotonic",
        )
    }

    /** §4 gives highIsoNR the range −4..+4 in steps of 1 — nine entries exactly. */
    @Test
    fun `the noise-reduction table covers the documented range and nothing else`() {
        assertEquals((-4..4).toSet(), HIGH_ISO_NR_CODES.keys)
    }

    @Test
    fun `a noise-reduction value outside the table is refused rather than guessed`() {
        assertNull(encodeValue("highIsoNR", 5))
    }

    // ─── The three that differ from the profile format ──────────────────────

    @Test
    fun `effects are one-indexed, not the profile's zero`() {
        assertEquals(1, encodeValue("colorChromeEffect", "off"))
        assertEquals(2, encodeValue("colorChromeEffect", "weak"))
        assertEquals(3, encodeValue("colorChromeFxBlue", "strong"))
    }

    @Test
    fun `dynamic range is a raw percentage, not a one-two-three enum`() {
        assertEquals(100, encodeValue("dynamicRange", "dr100"))
        assertEquals(200, encodeValue("dynamicRange", "dr200"))
        assertEquals(400, encodeValue("dynamicRange", "dr400"))
    }

    /** Auto never appeared in the captures, so it is refused rather than sent as DR100. */
    @Test
    fun `automatic dynamic range has no code`() {
        assertNull(encodeValue("dynamicRange", "dr-auto"))
    }

    @Test
    fun `grain strength and size encode together as one flat enum`() {
        assertEquals(1, grainCode("off", "small"))
        assertEquals(1, grainCode("off", "large"))
        assertEquals(2, grainCode("weak", "small"))
        assertEquals(3, grainCode("strong", "small"))
        assertEquals(4, grainCode("weak", "large"))
        assertEquals(5, grainCode("strong", "large"))
    }

    /** Off ignores the size, so a recipe that carries no size is still writable. */
    @Test
    fun `grain off encodes without a size`() {
        assertEquals(1, grainCode("off", null))
        assertNull(grainCode("weak", null))
    }

    // ─── Tones ──────────────────────────────────────────────────────────────

    @Test
    fun `tone parameters are multiplied by ten`() {
        assertEquals(15, encodeValue("highlightTone", 1.5))
        assertEquals(-20, encodeValue("shadowTone", -2.0))
        assertEquals(0, encodeValue("color", 0))
        assertEquals(50, encodeValue("clarity", 5))
    }

    @Test
    fun `the colour temperature goes as its own number`() {
        assertEquals(5500, encodeValue("colorTemperature", 5500))
    }

    @Test
    fun `white-balance shifts keep their sign`() {
        assertEquals(-9, encodeValue("wbShiftRed", -9))
        assertEquals(9, encodeValue("wbShiftBlue", 9))
    }

    // ─── Simulations and white balance ──────────────────────────────────────

    @Test
    fun `every simulation the field source knows has a code`() {
        FilmSimulations.all.forEach { simulation ->
            assertNotNull(
                FILM_SIMULATION_CODES[simulation.id],
                "${simulation.id} has no code",
            )
        }
    }

    @Test
    fun `no two simulations share a code`() {
        assertEquals(FILM_SIMULATION_CODES.size, FILM_SIMULATION_CODES.values.toSet().size)
    }

    @Test
    fun `every white-balance option the field source offers appears in the table`() {
        RecipeFields.WHITE_BALANCE_OPTIONS.forEach { option ->
            assertTrue(
                WHITE_BALANCE_CODES.containsKey(option.id),
                "${option.id} is offered but not mapped",
            )
        }
    }

    /**
     * Guessing `0x8022` for white priority because ambience is `0x8021` is precisely the
     * inference this table forbids.
     */
    @Test
    fun `the four unobserved white-balance modes have no code`() {
        listOf("auto-white-priority", "custom-1", "custom-2", "custom-3").forEach { id ->
            assertNull(WHITE_BALANCE_CODES[id], "$id has been given a guessed code")
        }
    }

    /**
     * The bug this pins: a lookup code is a bit pattern, not a number. `highIsoNR` −4 is
     * `0x8000` and the colour-temperature mode is `0x8007`, both above i16's 32767 — packed
     * signed they clamp to `0x7FFF`, which is the wrong noise reduction and the wrong white
     * balance on a recipe that looked fine.
     */
    @Test
    fun `lookup values go on the wire unsigned`() {
        assertEquals(StepKind.U16, wireKind(Packing.LOOKUP))
        assertEquals(StepKind.U16, wireKind(Packing.U16))
        assertEquals(StepKind.I16, wireKind(Packing.I16))
        assertEquals(StepKind.I16, wireKind(Packing.TONE10))

        assertTrue(HIGH_ISO_NR_CODES.getValue(-4) > Short.MAX_VALUE)
        assertTrue(WHITE_BALANCE_CODES.getValue(COLOR_TEMP_MODE)!! > Short.MAX_VALUE)
    }

    // ─── Verified generations ───────────────────────────────────────────────

    @Test
    fun `the codes are claimed as verified only where they were observed`() {
        assertTrue(propertyCodesVerifiedFor(SensorGeneration.XTRANS_V))
        assertTrue(propertyCodesVerifiedFor(SensorGeneration.XTRANS_IV))
        assertFalse(propertyCodesVerifiedFor(SensorGeneration.GFX))
    }

    @Test
    fun `an unknown field encodes to nothing rather than throwing`() {
        assertNull(encodeValue("somethingNewer", 3))
    }
}
