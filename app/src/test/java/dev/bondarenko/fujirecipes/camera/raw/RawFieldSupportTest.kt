package dev.bondarenko.fujirecipes.camera.raw

import dev.bondarenko.fujirecipes.data.fields.RecipeFields
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The lab must not offer a control that does nothing, so "what this adapter supports" and
 * "what a patch actually applied" have to be the same list. These hold them together.
 */
class RawFieldSupportTest {

    @Test
    fun `a fully populated recipe applies exactly the supported fields`() {
        val supported = rawSupportedFieldIds("X100VI")
        // Colour temperature only reaches the profile under the Kelvin white balance, and
        // `color` only on a colour film simulation — so the recipe that should apply
        // *everything* is the one that satisfies both conditions.
        val patch = patchRawDevelopmentProfile("X100VI", nativeProfile(), everyFieldSet())

        assertEquals(supported, patch.appliedFields.toSet())
    }

    @Test
    fun `every supported id is a real recipe field`() {
        // A typo here would produce a control the panel never draws and a word the patch never
        // writes, and nothing else would notice.
        rawSupportedFieldIds("X-T50").forEach { id ->
            assertTrue(RecipeFields.byId(id) != null, "$id is not a recipe field")
        }
    }

    @Test
    fun `unrendered fields are never reported as applied`() {
        val patch = patchRawDevelopmentProfile("X100VI", nativeProfile(), everyFieldSet())

        RAW_UNRENDERED_FIELD_IDS.forEach { id ->
            assertTrue(id in patch.preservedFields, "$id should be preserved")
            assertTrue(id !in patch.appliedFields, "$id should not be applied")
        }
    }

    @Test
    fun `an uncalibrated body supports nothing`() {
        assertEquals(emptySet(), rawSupportedFieldIds("X-T4"))
        assertEquals(emptySet(), rawSupportedFieldIds(null))
    }

    @Test
    fun `a monochrome recipe preserves the camera's colour word`() {
        val settings = everyFieldSet().toMutableMap()
            .apply { put("filmSimulation", JsonPrimitive("acros")) }
        val patch = patchRawDevelopmentProfile("X100VI", nativeProfile(), JsonObject(settings))

        assertTrue("color" in patch.preservedFields)
        assertTrue("color" !in patch.appliedFields)
    }

    /** Every field the native adapter knows, set to something it can encode. */
    private fun everyFieldSet(): JsonObject = JsonObject(
        mapOf(
            "filmSimulation" to JsonPrimitive("classic-chrome"),
            "dynamicRange" to JsonPrimitive("dr400"),
            "grainEffect" to JsonPrimitive("weak"),
            "grainSize" to JsonPrimitive("large"),
            "colorChromeEffect" to JsonPrimitive("strong"),
            "colorChromeFxBlue" to JsonPrimitive("weak"),
            "whiteBalance" to JsonPrimitive("color-temp"),
            "colorTemperature" to JsonPrimitive(5500),
            "wbShiftRed" to JsonPrimitive(2),
            "wbShiftBlue" to JsonPrimitive(-2),
            "highlightTone" to JsonPrimitive(1.5),
            "shadowTone" to JsonPrimitive(-1.0),
            "color" to JsonPrimitive(2),
            "sharpness" to JsonPrimitive(-1),
            "highIsoNR" to JsonPrimitive(-3),
            "clarity" to JsonPrimitive(4),
            "exposureCompensation" to JsonPrimitive(0.667),
        ),
    )

    private fun nativeProfile(): ByteArray = ByteArray(625) { (it % 251).toByte() }.also {
        ByteBuffer.wrap(it).order(ByteOrder.LITTLE_ENDIAN).putShort(0, 28)
    }
}
