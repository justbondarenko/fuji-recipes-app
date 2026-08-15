package dev.bondarenko.fujirecipes.camera

import dev.bondarenko.fujirecipes.data.fields.SensorGeneration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * FEAT-005 T-05.
 *
 * Mirrors `fuji-recipes-book/tests/unit/camera-models.spec.ts` @ `0c17106`.
 *
 * The table is the one piece of camera knowledge with no single citable document behind it,
 * so what is tested is the *shape* of its answers rather than its length: an unrecognised body
 * refuses writes and does not claim a generation, and a body without slot registers refuses
 * for a different, stated reason.
 */
class CameraModelsTest {

    @Test
    fun `no two known model names normalise to the same key`() {
        assertEquals(
            CameraModels.MODEL_GENERATIONS.size,
            CameraModels.MODEL_LOOKUP.size,
            "two model names collide once punctuation is dropped",
        )
    }

    @Test
    fun `punctuation does not decide recognition`() {
        listOf("X-T5", "XT5", "x-t5", " X-T5 ").forEach { reported ->
            val identity = CameraModels.identify(reported)
            assertTrue(identity.recognised, "$reported was not recognised")
            assertEquals(SensorGeneration.XTRANS_V, identity.generation)
        }
    }

    @Test
    fun `a known X-Trans V body is writable`() {
        val identity = CameraModels.identify("X100VI")

        assertTrue(identity.recognised)
        assertEquals(SensorGeneration.XTRANS_V, identity.generation)
        assertEquals("X-Trans V", identity.label)
        assertTrue(identity.writable)
        assertNull(identity.note)
    }

    /** WR-01: X-Trans III has no slot registers, and the message says exactly that. */
    @Test
    fun `a body without custom slots is recognised, refused, and says why`() {
        val identity = CameraModels.identify("X-T2")

        assertTrue(identity.recognised)
        assertEquals(SensorGeneration.XTRANS_III, identity.generation)
        assertEquals("X-Trans III", identity.label)
        assertFalse(identity.writable)
        assertTrue(identity.note!!.contains("no custom slots"))
    }

    /**
     * The claim this module exists to avoid: labelling an unknown body "Bayer CMOS" would be
     * an assertion about a sensor nobody has looked at.
     */
    @Test
    fun `an unrecognised body does not claim a generation it cannot know`() {
        val identity = CameraModels.identify("X-T99")

        assertFalse(identity.recognised)
        assertFalse(identity.writable)
        assertEquals(CameraModels.UNRECOGNISED_LABEL, identity.label)
        assertEquals(SensorGeneration.CMOS, identity.generation)
        assertFalse(identity.label.contains("Bayer"))
        assertTrue(identity.note!!.contains("Storing recipes works normally"))
    }

    @Test
    fun `the two reasons a write is unavailable stay distinguishable`() {
        val unknown = CameraModels.identify("X-T99").note
        val slotless = CameraModels.identify("X-T2").note

        assertNotNull(unknown)
        assertNotNull(slotless)
        assertTrue(unknown != slotless, "an unknown body and a slotless one read the same")
    }

    @Test
    fun `an empty or missing model is unrecognised rather than a crash`() {
        listOf(null, "", "   ").forEach { reported ->
            val identity = CameraModels.identify(reported)
            assertFalse(identity.recognised)
            assertFalse(identity.writable)
        }
    }

    /** Nothing is inferred from a name: a GFX 50 body is not folded into the `gfx` row. */
    @Test
    fun `the GFX 50 series is Bayer CMOS and not writable`() {
        val identity = CameraModels.identify("GFX 50S")

        assertEquals(SensorGeneration.CMOS, identity.generation)
        assertFalse(identity.writable)
    }

    @Test
    fun `an unlisted GFX body is unrecognised rather than assumed`() {
        assertFalse(CameraModels.identify("GFX 200S").recognised)
    }

    @Test
    fun `every table entry carries a generation the field source knows`() {
        CameraModels.MODEL_GENERATIONS.forEach { (name, generation) ->
            assertNotNull(SensorGeneration.fromId(generation.id), "$name has an unknown id")
        }
    }
}
