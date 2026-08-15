package dev.bondarenko.fujirecipes.camera.ptp

import dev.bondarenko.fujirecipes.camera.FakeCamera
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * FEAT-005 T-10.
 *
 * Mirrors the session half of `fuji-recipes-book/tests/unit/ptp-session.spec.ts` and the
 * transport cases of `tests/unit/usb-transport.spec.ts` @ `0c17106`, against [FakeCamera].
 *
 * These are the cases that would otherwise only be discovered in the field, with the camera in
 * the phone's only USB port (`architecture.md` C1).
 */
class PtpSessionTest {

    private fun session(camera: FakeCamera) = PtpSession(PtpTransport(camera))

    @Test
    fun `opens a session and reads the model`() {
        val camera = FakeCamera(model = "X-T5")
        val session = session(camera)

        val info = session.open()

        assertEquals("X-T5", info.model)
        assertEquals("X-T5", session.productName)
        assertTrue(session.isOpen)
    }

    /** A cable left in across an app restart leaves the camera's side open. */
    @Test
    fun `treats SessionAlreadyOpen as a success`() {
        val camera = FakeCamera()
        camera.refuseOperation[Operation.OPEN_SESSION] = ResponseCode.SESSION_ALREADY_OPEN

        // The fake still answers GetDeviceInfo, which is the point: the session continues.
        val session = session(camera)
        session.open()

        assertTrue(session.isOpen)
        assertEquals("X100VI", session.productName)
    }

    @Test
    fun `a refused session names the operation and the code`() {
        val camera = FakeCamera()
        camera.refuseOperation[Operation.OPEN_SESSION] = ResponseCode.DEVICE_BUSY

        val error = assertFailsWith<PtpError> { session(camera).open() }

        assertEquals(ResponseCode.DEVICE_BUSY, error.code)
        assertEquals(Operation.OPEN_SESSION, error.operation)
        assertTrue(error.message!!.contains("DeviceBusy"))
    }

    /**
     * Whatever failed left the pipe in an unknown state, so the interface is released rather
     * than kept for a retry that would inherit it.
     */
    @Test
    fun `a failed open releases the device`() {
        val camera = FakeCamera()
        camera.refuseOperation[Operation.OPEN_SESSION] = ResponseCode.DEVICE_BUSY

        runCatching { session(camera).open() }

        assertTrue(camera.closed)
    }

    @Test
    fun `close ends the session and releases the device`() {
        val camera = FakeCamera()
        val session = session(camera)
        session.open()

        session.close()

        assertFalse(session.isOpen)
        assertTrue(camera.closed)
    }

    /** An unplugged camera cannot be told the session is over; the release still happens. */
    @Test
    fun `close never throws`() {
        val camera = FakeCamera()
        val session = session(camera)
        session.open()
        camera.refuseOperation[Operation.CLOSE_SESSION] = ResponseCode.GENERAL_ERROR

        session.close()

        assertTrue(camera.closed)
    }

    @Test
    fun `keeps the device info after close so a message can still name the body`() {
        val camera = FakeCamera(model = "X-H2S")
        val session = session(camera)
        session.open()

        session.close()

        assertEquals("X-H2S", session.deviceInfo?.model)
    }

    // ─── Reassembly and stale replies ───────────────────────────────────────

    /** A device info dataset does not fit in one bulk read on a real body either. */
    @Test
    fun `reassembles a reply that spans several reads`() {
        val camera = FakeCamera(model = "X100VI")
        camera.chunkSize = 8

        assertEquals("X100VI", session(camera).open().model)
    }

    /**
     * The check that stops a property read from returning another property's value: a read
     * that timed out leaves its answer queued, and the next read collects it.
     */
    @Test
    fun `refuses a reply belonging to an earlier transaction`() {
        val camera = FakeCamera()
        camera.stageStaleReply(transactionId = 99)

        val error = assertFailsWith<PtpFramingError> { session(camera).open() }

        assertTrue(error.message!!.contains("transaction 99"))
        assertTrue(error.message!!.contains("session needs resetting"))
    }

    /**
     * P5: a camera that stopped answering and a pipe that is out of step have different
     * remedies — check the cable versus reset the session — so they must not be one type.
     */
    @Test
    fun `a camera that stops answering is a timeout, not a framing error`() {
        val camera = FakeCamera()
        camera.silent = true

        assertFailsWith<PtpTimeoutError> { session(camera).open() }
    }

    // ─── Properties ─────────────────────────────────────────────────────────

    @Test
    fun `reads a property's raw bytes`() {
        val camera = FakeCamera()
        camera.propertyValues[0xd18c] = packU16(3)
        val session = session(camera)
        session.open()

        assertContentEquals(packU16(3), session.readPropertyBytes(0xd18c))
    }

    @Test
    fun `a property the body does not have is refused with its own code`() {
        val camera = FakeCamera()
        val session = session(camera)
        session.open()

        val error = assertFailsWith<PtpError> { session.readPropertyBytes(0xd199) }

        assertEquals(ResponseCode.DEVICE_PROP_NOT_SUPPORTED, error.code)
    }

    @Test
    fun `writes a property's raw bytes and the camera receives them`() {
        val camera = FakeCamera()
        val session = session(camera)
        session.open()

        session.setPropertyBytes(0xd18c, packU16(3))

        assertEquals(1, camera.writes.size)
        assertEquals(0xd18c, camera.writes[0].property)
        assertContentEquals(packU16(3), camera.writes[0].payload)
    }

    /**
     * The refusal is let out untouched, carrying the code, because the write executor needs
     * exactly that to name which property the camera refused and what it said (P5).
     */
    @Test
    fun `a refused write carries the response code the executor reports`() {
        val camera = FakeCamera()
        camera.refuseProperty[0xd1a2] = ResponseCode.INVALID_DEVICE_PROP_VALUE
        val session = session(camera)
        session.open()

        val error = assertFailsWith<PtpError> { session.setPropertyBytes(0xd1a2, packU16(9)) }

        assertEquals(ResponseCode.INVALID_DEVICE_PROP_VALUE, error.code)
        assertEquals(Operation.SET_DEVICE_PROP_VALUE, error.operation)
    }

    @Test
    fun `reports what the body says it supports`() {
        val session = session(FakeCamera())
        session.open()

        assertTrue(session.supportsOperation(Operation.SET_DEVICE_PROP_VALUE))
        assertTrue(session.supportsProperty(0xd18c))
        assertFalse(session.supportsProperty(0xd199))
    }

    /**
     * Same reasoning as the transaction-id check, one layer up: a description belonging to
     * another property would be read as this one's allowed values.
     */
    @Test
    fun `refuses a description that belongs to another property`() {
        val camera = FakeCamera()
        // Asked about the slot property, the body describes the name property instead.
        camera.describeAnswers[0xd18c] = description(code = 0xd18d)
        val session = session(camera)
        session.open()

        val error = assertFailsWith<PtpFramingError> { session.describeProperty(0xd18c) }

        assertTrue(error.message!!.contains("0xD18C"))
        assertTrue(error.message!!.contains("0xD18D"))
    }

    @Test
    fun `describes a property the body answers for`() {
        val camera = FakeCamera()
        camera.describeAnswers[0xd18c] = description(code = 0xd18c, currentValue = 3)
        val session = session(camera)
        session.open()

        val described = session.describeProperty(0xd18c)

        assertEquals(0xd18c, described.code)
        assertEquals(3L, described.currentValue)
        assertTrue(described.writable)
    }

    /** A minimal `DevicePropDesc` dataset: code, type, GetSet, default, current. */
    private fun description(code: Int, currentValue: Int = 0): ByteArray =
        packU16(code) + packU16(DataType.UINT16) + byteArrayOf(1) + packU16(0) +
            packU16(currentValue)
}
