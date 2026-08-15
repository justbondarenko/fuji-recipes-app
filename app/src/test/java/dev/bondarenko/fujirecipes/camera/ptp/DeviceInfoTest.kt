package dev.bondarenko.fujirecipes.camera.ptp

import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * FEAT-005 T-03.
 *
 * Mirrors the dataset half of `fuji-recipes-book/tests/unit/ptp-session.spec.ts` @ `0c17106`.
 *
 * The point of these is the offset arithmetic. `model` is the twelfth of fourteen fields and
 * every one before it is variable-length, so reading it correctly means every array and every
 * string before it was walked correctly too. A parser that drifts does not fail — it returns a
 * plausible wrong string, and the app then confidently refuses to write to an X100VI.
 */
class DeviceInfoTest {

    private class Dataset {
        private val out = ByteArrayOutputStream()

        fun u8(value: Int) = apply { out.write(value and 0xff) }
        fun u16(value: Int) = apply { out.write(packU16(value)) }
        fun u32(value: Int) = apply { out.write(packU32(value)) }
        fun string(value: String) = apply { out.write(packPtpString(value)) }
        fun u16Array(vararg values: Int) = apply {
            u32(values.size)
            values.forEach { u16(it) }
        }

        fun bytes(): ByteArray = out.toByteArray()
    }

    private fun deviceInfo(
        model: String = "X100VI",
        properties: IntArray = intArrayOf(0xd18c, 0xd18d, 0xd18e),
    ): ByteArray = Dataset()
        .u16(100)
        .u32(6)
        .u16(100)
        .string("fujifilm.co.jp: 1.0;")
        .u16(0)
        .u16Array(0x1001, 0x1002, 0x1003, 0x1015, 0x1016)
        .u16Array(0x4002)
        .u16Array(*properties)
        .u16Array()
        .u16Array(0x3801)
        .string("FUJIFILM")
        .string(model)
        .string("1.20")
        .string("A1B2C3D4")
        .bytes()

    @Test
    fun `reads the model through five arrays and two strings before it`() {
        val info = parseDeviceInfo(deviceInfo())

        assertEquals("X100VI", info.model)
        assertEquals("FUJIFILM", info.manufacturer)
        assertEquals("1.20", info.deviceVersion)
        assertEquals("A1B2C3D4", info.serialNumber)
    }

    @Test
    fun `reads the property codes the body admits to having`() {
        val info = parseDeviceInfo(deviceInfo(properties = intArrayOf(0xd18c, 0xd1a5)))

        assertEquals(listOf(0xd18c, 0xd1a5), info.devicePropertiesSupported)
        assertTrue(info.operationsSupported.contains(Operation.SET_DEVICE_PROP_VALUE))
    }

    @Test
    fun `an empty array does not consume a string after it`() {
        val info = parseDeviceInfo(deviceInfo(properties = intArrayOf()))

        assertTrue(info.devicePropertiesSupported.isEmpty())
        assertEquals("X100VI", info.model)
    }

    @Test
    fun `a truncated dataset names the dataset rather than throwing an index error`() {
        val full = deviceInfo()
        val error = assertFailsWith<PtpFramingError> {
            parseDeviceInfo(full.copyOfRange(0, full.size - 6))
        }

        assertTrue(error.message!!.contains("device info dataset"))
    }

    // ─── DevicePropDesc ─────────────────────────────────────────────────────

    @Test
    fun `reads a range-form property`() {
        val bytes = Dataset()
            .u16(0xd18c)
            .u16(DataType.UINT16)
            .u8(1)
            .u16(1)
            .u16(3)
            .u8(0x01)
            .u16(1)
            .u16(7)
            .u16(1)
            .bytes()

        val description = parseDevicePropertyDescription(bytes)

        assertEquals(0xd18c, description.code)
        assertTrue(description.writable)
        assertEquals(3L, description.currentValue)
        assertEquals(PropertyForm.RANGE, description.form)
        assertEquals(PropertyRange(1, 7, 1), description.range)
    }

    @Test
    fun `reads an enum-form property and keeps the values the camera named`() {
        val bytes = Dataset()
            .u16(0xd192)
            .u16(DataType.UINT16)
            .u8(1)
            .u16(1)
            .u16(2)
            .u8(0x02)
            .u16(3)
            .u16(1)
            .u16(2)
            .u16(3)
            .bytes()

        val description = parseDevicePropertyDescription(bytes)

        assertEquals(PropertyForm.ENUM, description.form)
        assertEquals(listOf(1L, 2L, 3L), description.enumeration)
    }

    /** Some bodies omit the form. The type and the current value are already useful. */
    @Test
    fun `accepts a dataset that stops after the current value`() {
        val bytes = Dataset()
            .u16(0xd18d)
            .u16(DataType.STRING)
            .u8(1)
            .string("")
            .string("Kodachrome 64")
            .bytes()

        val description = parseDevicePropertyDescription(bytes)

        assertEquals(PropertyForm.NONE, description.form)
        assertEquals("Kodachrome 64", description.currentValue)
    }

    @Test
    fun `a read-only property says so`() {
        val bytes = Dataset()
            .u16(0xd1a5)
            .u16(DataType.UINT16)
            .u8(0)
            .u16(0)
            .u16(0)
            .bytes()

        assertEquals(false, parseDevicePropertyDescription(bytes).writable)
    }

    @Test
    fun `refuses a form flag ISO 15740 does not define`() {
        val bytes = Dataset()
            .u16(0xd18c)
            .u16(DataType.UINT16)
            .u8(1)
            .u16(1)
            .u16(1)
            .u8(0x07)
            .bytes()

        assertFailsWith<PtpFramingError> { parseDevicePropertyDescription(bytes) }
    }

    @Test
    fun `refuses a data type it cannot measure rather than guessing four bytes`() {
        val bytes = Dataset()
            .u16(0xd18c)
            .u16(0x1234)
            .u8(1)
            .u32(0)
            .bytes()

        assertFailsWith<PtpFramingError> { parseDevicePropertyDescription(bytes) }
    }
}
