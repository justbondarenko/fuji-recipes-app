package dev.bondarenko.fujirecipes.camera.raw

import dev.bondarenko.fujirecipes.camera.ptp.parseObjectInfo
import dev.bondarenko.fujirecipes.data.model.Recipe
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RawDevelopmentProfileTest {
    @Test
    fun `Fuji RAW object info uses vendor format and upload name`() {
        val info = parseObjectInfo(fujiRawObjectInfo(42_000_000))

        assertEquals(0, info.storageId)
        assertEquals(FUJI_RAW_OBJECT_FORMAT, info.format)
        assertEquals(42_000_000, info.compressedSize)
        assertEquals(FUJI_RAW_UPLOAD_NAME, info.filename)
    }

    @Test
    fun `patches verified X100VI words and preserves everything else`() {
        val base = nativeProfile()
        val recipe = Recipe(
            id = "r1",
            name = "Test",
            settings = buildJsonObject {
                put("filmSimulation", "classic-chrome")
                put("dynamicRange", "dr400")
                put("highlightTone", 1.5)
                put("shadowTone", -2.0)
                put("wbShiftRed", 3)
            },
        )

        val patch = patchRawDevelopmentProfile("X100VI", base, recipe)
        val start = 625 - 28 * 4
        val words = ByteBuffer.wrap(patch.bytes).order(ByteOrder.LITTLE_ENDIAN)

        assertEquals(0x0b, words.getInt(start + 8 * 4))
        assertEquals(400, words.getInt(start + 6 * 4))
        assertEquals(15, words.getInt(start + 16 * 4))
        assertEquals(-20, words.getInt(start + 17 * 4))
        assertEquals(3, words.getInt(start + 13 * 4))
        assertContentEquals(base.copyOfRange(2, start), patch.bytes.copyOfRange(2, start))
        assertTrue("filmSimulation" in patch.appliedFields)
        assertTrue("dRangePriority" in patch.preservedFields)
    }

    @Test
    fun `patches the captured X-T50 native profile without changing its header`() {
        val base = xT50Profile()
        val patch = patchRawDevelopmentProfile(
            "X-T50",
            base,
            Recipe(
                id = "r1",
                name = "Test",
                settings = buildJsonObject { put("filmSimulation", "reala-ace") },
            ),
        )
        val start = 625 - 29 * 4

        assertContentEquals(base.copyOfRange(0, start), patch.bytes.copyOfRange(0, start))
        assertEquals(
            0x14,
            ByteBuffer.wrap(patch.bytes).order(ByteOrder.LITTLE_ENDIAN).getInt(start + 8 * 4),
        )
    }

    @Test
    fun `rejects an X-T50 profile with another processor identifier`() {
        val profile = xT50Profile().apply { this[3] = 'X'.code.toByte() }

        assertFailsWith<UnsupportedRawProfile> {
            patchRawDevelopmentProfile("X-T50", profile, Recipe(id = "r1", name = "Test"))
        }
    }

    private fun nativeProfile(): ByteArray = ByteArray(625) { (it % 251).toByte() }.also {
        ByteBuffer.wrap(it).order(ByteOrder.LITTLE_ENDIAN).putShort(0, 28)
    }

    private fun xT50Profile(): ByteArray {
        val encoded = checkNotNull(javaClass.getResource("/camera/raw/x-t50-d185.b64"))
            .readText()
            .trim()
        return Base64.getMimeDecoder().decode(encoded)
    }
}
