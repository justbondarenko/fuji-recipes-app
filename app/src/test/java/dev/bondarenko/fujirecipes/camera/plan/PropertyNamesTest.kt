package dev.bondarenko.fujirecipes.camera.plan

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PropertyNamesTest {

    /**
     * The two tables that carry the preset block must not drift apart.
     *
     * `FIELD_PROPERTIES` decides what gets written; this one decides what a report calls it.
     * A code added to one and not the other means a report labels a property by its number
     * while the app happily writes to it, which is the sort of divergence nobody notices until
     * a report is used to debug the wrong thing.
     */
    @Test
    fun `every code the app writes to has a name a report can print`() {
        FIELD_PROPERTIES.forEach { (fieldId, mapping) ->
            val code = mapping.code ?: return@forEach

            assertNotNull(
                propertyName(code),
                "field “$fieldId” writes to 0x${code.toString(16).uppercase()} but " +
                    "PropertyNames.kt has no name for it",
            )
        }
    }

    @Test
    fun `every code the app writes to is probed by a report`() {
        FIELD_PROPERTIES.forEach { (fieldId, mapping) ->
            val code = mapping.code ?: return@forEach

            assertTrue(
                code in ALWAYS_PROBED_PROPERTIES,
                "field “$fieldId” writes to 0x${code.toString(16).uppercase()} but a report " +
                    "would not ask about it",
            )
        }
    }

    @Test
    fun `the properties this build reads outside the preset block are named and probed`() {
        listOf(
            USB_MODE_PROPERTY,
            STANDARD_BATTERY_PROPERTY,
            BATTERY_LEVEL_PROPERTY,
            TOTAL_SHOT_COUNT_PROPERTY,
            LENS_NAME_PROPERTY,
            PRESET_SLOT_PROPERTY,
            PRESET_NAME_PROPERTY,
        ).forEach { code ->
            assertNotNull(propertyName(code), "0x${code.toString(16)} has no name")
            assertTrue(code in ALWAYS_PROBED_PROPERTIES, "0x${code.toString(16)} is not probed")
        }
    }

    /**
     * The exclusion is the point, not an oversight.
     *
     * `eggricesoy/filmkit` and `petabyt/libfuji` name this range and contradict each other on
     * every code below. Naming either way round would put a guess into a document people build
     * on; the body's own declared type and enumeration settle it instead.
     */
    @Test
    fun `the disputed shooting range is deliberately unnamed`() {
        listOf(0xd001, 0xd007, 0xd00a, 0xd017, 0xd019, 0xd104, 0xd10a, 0xd171).forEach { code ->
            assertNull(
                propertyName(code),
                "0x${code.toString(16).uppercase()} is disputed between filmkit and libfuji " +
                    "and must not be named",
            )
        }
    }

    /** The two unidentified codes inside the block are named *as* unidentified. */
    @Test
    fun `the unidentified preset codes say so rather than being absent`() {
        assertTrue(propertyName(0xd191)!!.contains("unidentified"))
        assertTrue(propertyName(0xd1a5)!!.contains("unidentified"))
    }

    @Test
    fun `standard properties and operations carry their ISO names`() {
        assertEquals("BatteryLevel", propertyName(0x5001))
        assertEquals("ExposureTime", propertyName(0x500d))
        assertEquals("GetDeviceInfo", operationName(0x1001))
        assertEquals("SendObject", operationName(0x100d))
    }

    /** A vendor operation has no ISO name, and inventing one would be the same mistake. */
    @Test
    fun `vendor operations are left unnamed`() {
        assertNull(operationName(0x900c))
        assertNull(operationName(0x9020))
    }

    /**
     * The two halves of the walk must partition the codes: the preset block is read per slot
     * and everything else once. A code in both would be recorded twice under different
     * meanings; a code in neither would not be recorded at all.
     */
    @Test
    fun `the preset block covers exactly the slot-following registers`() {
        assertEquals(0xd18c, PRESET_BLOCK.first)
        assertEquals(0xd1a5, PRESET_BLOCK.last)
        assertTrue(PRESET_SLOT_PROPERTY in PRESET_BLOCK)
        assertTrue(PRESET_NAME_PROPERTY in PRESET_BLOCK)

        FIELD_PROPERTIES.values.mapNotNull { it.code }.forEach { code ->
            assertTrue(code in PRESET_BLOCK, "0x${code.toString(16)} is not in the preset block")
        }
    }

    /** The name is a string, so it gets its own line rather than a column of hex. */
    @Test
    fun `the matrix leaves out the name and keeps everything else`() {
        assertEquals(PRESET_BLOCK.count() - 1, PRESET_MATRIX_CODES.size)
        assertTrue(PRESET_NAME_PROPERTY !in PRESET_MATRIX_CODES)
        assertTrue(PRESET_SLOT_PROPERTY in PRESET_MATRIX_CODES)
    }

    @Test
    fun `the probe list is sorted and free of duplicates`() {
        assertEquals(ALWAYS_PROBED_PROPERTIES.sorted(), ALWAYS_PROBED_PROPERTIES)
        assertEquals(ALWAYS_PROBED_PROPERTIES.distinct(), ALWAYS_PROBED_PROPERTIES)
    }
}
