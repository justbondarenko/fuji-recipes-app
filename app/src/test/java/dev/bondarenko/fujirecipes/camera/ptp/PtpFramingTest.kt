package dev.bondarenko.fujirecipes.camera.ptp

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * FEAT-005 T-02.
 *
 * Mirrors `fuji-recipes-book/tests/unit/ptp.spec.ts` @ `0c17106`. Byte arrays and no camera:
 * every case here is one the transport would otherwise only fail on in the field, with a
 * cable in the only USB port the phone has (`architecture.md` C1).
 */
class PtpFramingTest {

    private fun u16At(bytes: ByteArray, offset: Int) = unpackU16(bytes, offset)

    // ─── The container header ───────────────────────────────────────────────

    @Test
    fun `writes length type code and transaction id little-endian`() {
        val packed = packContainer(
            type = ContainerType.COMMAND,
            code = Operation.OPEN_SESSION,
            transactionId = 7,
            params = listOf(1),
        )

        assertEquals(16, containerLength(packed))
        assertEquals(ContainerType.COMMAND, u16At(packed, 4))
        assertEquals(Operation.OPEN_SESSION, u16At(packed, 6))
        assertEquals(7L, unpackU32(packed, 8))
        assertEquals(1L, unpackU32(packed, 12))
    }

    @Test
    fun `counts the header and the payload in the length`() {
        val packed = packContainer(
            type = ContainerType.DATA,
            code = Operation.SET_DEVICE_PROP_VALUE,
            transactionId = 2,
            data = byteArrayOf(1, 2, 3, 4, 5),
        )

        assertEquals((CONTAINER_HEADER_SIZE + 5).toLong(), containerLength(packed))
        assertEquals(CONTAINER_HEADER_SIZE + 5, packed.size)
    }

    @Test
    fun `keeps at most five params, since a container has room for five`() {
        val packed = packContainer(
            type = ContainerType.COMMAND,
            code = Operation.GET_DEVICE_PROP_VALUE,
            transactionId = 1,
            params = listOf(1, 2, 3, 4, 5, 6, 7),
        )

        assertEquals((CONTAINER_HEADER_SIZE + MAX_PARAMS * 4).toLong(), containerLength(packed))
        assertEquals(listOf(1, 2, 3, 4, 5), unpackContainer(packed).params)
    }

    @Test
    fun `packs a negative param as its unsigned form rather than throwing`() {
        val packed = packContainer(
            type = ContainerType.COMMAND,
            code = Operation.GET_DEVICE_PROP_VALUE,
            transactionId = 1,
            params = listOf(-1),
        )

        assertEquals(0xffffffffL, unpackU32(packed, 12))
    }

    // ─── Unpacking, by container type ───────────────────────────────────────

    @Test
    fun `reads a response's params`() {
        val packed = packContainer(
            type = ContainerType.RESPONSE,
            code = ResponseCode.OK,
            transactionId = 9,
            params = listOf(0xd18c, 3),
        )

        val container = unpackContainer(packed)

        assertEquals(ContainerType.RESPONSE, container.type)
        assertEquals(ResponseCode.OK, container.code)
        assertEquals(9, container.transactionId)
        assertEquals(listOf(0xd18c, 3), container.params)
        assertEquals(0, container.data.size)
    }

    /** The case that produces a plausible wrong number when it is got wrong. */
    @Test
    fun `reads a data container's payload, and takes no params from it`() {
        val payload = byteArrayOf(0x2c, 0x01, 0x00, 0x00)
        val packed = packContainer(
            type = ContainerType.DATA,
            code = Operation.GET_DEVICE_PROP_VALUE,
            transactionId = 4,
            data = payload,
        )

        val container = unpackContainer(packed)

        assertContentEquals(payload, container.data)
        assertTrue(container.params.isEmpty())
    }

    @Test
    fun `round-trips a payload that is not a multiple of four`() {
        val payload = byteArrayOf(3, 0x43, 0x00, 0x33, 0x00, 0x00, 0x00)
        val packed = packContainer(ContainerType.DATA, 0x1016, 1, data = payload)

        assertContentEquals(payload, unpackContainer(packed).data)
    }

    @Test
    fun `refuses anything shorter than a header`() {
        assertFailsWith<PtpFramingError> { unpackContainer(ByteArray(11)) }
    }

    @Test
    fun `reads a container that sits inside a larger buffer`() {
        val packed = packContainer(ContainerType.RESPONSE, ResponseCode.OK, 5, params = listOf(42))
        val buffer = ByteArray(8) + packed + ByteArray(16)

        val container = unpackContainer(buffer, offset = 8, length = packed.size)

        assertEquals(ResponseCode.OK, container.code)
        assertEquals(5, container.transactionId)
        assertEquals(listOf(42), container.params)
    }

    @Test
    fun `returns zero length for a buffer too short to hold one`() {
        assertEquals(0L, containerLength(byteArrayOf(1, 2, 3)))
    }

    // ─── The value codecs ───────────────────────────────────────────────────

    @Test
    fun `packs and unpacks unsigned 16-bit little-endian`() {
        val packed = packU16(400)
        assertContentEquals(byteArrayOf(0x90.toByte(), 0x01), packed)
        assertEquals(400, unpackU16(packed))
    }

    @Test
    fun `packs signed 16-bit, which the tone values need`() {
        assertEquals(-15, unpackI16(packI16(-15)))
        assertEquals(15, unpackI16(packI16(15)))
    }

    @Test
    fun `clamps a signed value rather than wrapping it`() {
        assertEquals(32767, unpackI16(packI16(40000)))
        assertEquals(-32768, unpackI16(packI16(-40000)))
    }

    @Test
    fun `rounds a fractional value, since the wire takes integers`() {
        assertEquals(15, unpackI16(packI16(14.6)))
    }

    @Test
    fun `packs unsigned 32-bit for params`() {
        assertEquals(0xd18cL, unpackU32(packU32(0xd18c)))
    }

    @Test
    fun `reads at an offset`() {
        val bytes = byteArrayOf(0xff.toByte(), 0xff.toByte()) + packU16(7)
        assertEquals(7, unpackU16(bytes, 2))
    }

    // ─── PTP strings ────────────────────────────────────────────────────────

    @Test
    fun `writes a length in characters including the terminator, then UCS-2 LE`() {
        val packed = packPtpString("C3")

        assertEquals(3, packed[0].toInt())
        assertEquals('C'.code, unpackU16(packed, 1))
        assertEquals('3'.code, unpackU16(packed, 3))
        assertEquals(0, unpackU16(packed, 5))
        assertEquals(7, packed.size)
    }

    /** Not a `1` and a NUL. The camera reads the length byte and believes it. */
    @Test
    fun `writes an empty string as a single zero byte`() {
        assertContentEquals(byteArrayOf(0), packPtpString(""))
    }

    @Test
    fun `round-trips a preset name`() {
        assertEquals("Kodachrome 64", unpackPtpString(packPtpString("Kodachrome 64")))
    }

    @Test
    fun `stops at the terminator rather than at the buffer's end`() {
        val bytes = packPtpString("C3") + byteArrayOf(0x41, 0x00, 0x42, 0x00)
        assertEquals("C3", unpackPtpString(bytes))
    }

    @Test
    fun `refuses a string too long for a one-byte length`() {
        assertFailsWith<PtpFramingError> { packPtpString("x".repeat(MAX_PTP_STRING_LENGTH + 1)) }
    }

    @Test
    fun `reads an empty string from an empty buffer rather than throwing`() {
        assertEquals("", unpackPtpString(ByteArray(0)))
        assertEquals("", unpackPtpString(byteArrayOf(0)))
    }

    @Test
    fun `reads at an offset, for a payload that carries more than the string`() {
        val bytes = byteArrayOf(9, 9) + packPtpString("X-T5")
        assertEquals("X-T5", unpackPtpString(bytes, offset = 2))
    }

    // ─── Response codes (P5) ────────────────────────────────────────────────

    @Test
    fun `names the codes a person needs to act on`() {
        assertEquals("DeviceBusy", responseName(ResponseCode.DEVICE_BUSY))
        assertEquals("AccessDenied", responseName(ResponseCode.ACCESS_DENIED))
        assertEquals(
            "InvalidDevicePropValue",
            responseName(ResponseCode.INVALID_DEVICE_PROP_VALUE),
        )
    }

    @Test
    fun `falls back to hex for a code it does not know`() {
        assertEquals("0x20FF", responseName(0x20ff))
    }

    @Test
    fun `passes OK through and throws a named error otherwise`() {
        expectOk(Operation.OPEN_SESSION, ResponseCode.OK)

        val error = assertFailsWith<PtpError> {
            expectOk(Operation.SET_DEVICE_PROP_VALUE, ResponseCode.DEVICE_BUSY)
        }

        assertEquals(ResponseCode.DEVICE_BUSY, error.code)
        assertEquals(Operation.SET_DEVICE_PROP_VALUE, error.operation)
        assertTrue(error.message!!.contains("DeviceBusy"))
    }

    // ─── Data types ─────────────────────────────────────────────────────────

    @Test
    fun `measures the scalar types and refuses to guess at an unknown one`() {
        assertEquals(2, dataTypeSize(DataType.UINT16))
        assertEquals(2, dataTypeSize(DataType.INT16))
        assertEquals(4, dataTypeSize(DataType.UINT32))
        assertNull(dataTypeSize(DataType.STRING))
        assertNull(dataTypeSize(0x1234))
    }

    @Test
    fun `names an array type without mistaking a string for one`() {
        assertEquals("UInt16", dataTypeName(DataType.UINT16))
        assertEquals("UInt16[]", dataTypeName(DataType.UINT16 or ARRAY_TYPE_FLAG))
        assertEquals("String", dataTypeName(DataType.STRING))
    }
}
