package dev.bondarenko.fujirecipes.camera.ptp

import dev.bondarenko.fujirecipes.camera.FakeCamera
import dev.bondarenko.fujirecipes.camera.TimedCameraChannel
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PtpResponseTimeoutTest {
    @Test
    fun `ordinary responses still time out at five seconds and identify the property`() {
        val channel = TimedCameraChannel(FakeCamera())
        val session = PtpSession(PtpTransport(channel)).also { it.open() }
        channel.responseDelayMs = { 8_000 }

        val error = assertFailsWith<PtpTimeoutError> { session.readPropertyBytes(0xd185) }
        assertEquals(DEFAULT_TIMEOUT_MS, error.timeoutMs)
        assertEquals(Operation.GET_DEVICE_PROP_VALUE, error.operation)
        assertEquals(0xd185, error.property)
        assertTrue(error.message.orEmpty().contains("property 0xd185"))
    }

    @Test
    fun `nested response timeouts restore the outer and default waits after exceptions`() {
        val channel = TimedCameraChannel(FakeCamera())
        val session = PtpSession(PtpTransport(channel)).also { it.open() }
        channel.readTimeouts.clear()

        session.withResponseTimeout(30_000) {
            session.getObjectHandles(-1)
            assertFailsWith<IllegalStateException> {
                session.withResponseTimeout(1_000) {
                    session.getObjectHandles(-1)
                    error("stop nested work")
                }
            }
            session.getObjectHandles(-1)
        }
        session.getObjectHandles(-1)

        // Every handle response has a data phase and an acknowledgement.
        assertEquals(listOf(30_000, 30_000, 1_000, 1_000, 30_000, 30_000, 5_000, 5_000), channel.readTimeouts)
        assertFailsWith<IllegalArgumentException> { session.withResponseTimeout(0) {} }
    }

    @Test
    fun `scoped waits cover streamed upload acknowledgements and every download chunk`() {
        val camera = FakeCamera().apply {
            objects[1] = ByteArray(100) { 1 }
            objectInfos[1] = byteArrayOf(1)
            chunkSize = 16
        }
        val channel = TimedCameraChannel(camera)
        val session = PtpSession(PtpTransport(channel)).also { it.open() }
        channel.responseDelayMs = { 8_000 }
        channel.readTimeouts.clear()

        session.withResponseTimeout(30_000) {
            session.sendFujiRawObjectInfo(byteArrayOf(1))
            session.sendFujiRawObject(ByteArrayInputStream(byteArrayOf(1, 2, 3)), 3)
            val output = ByteArrayOutputStream()
            session.getObject(1, output, maxBytes = 100)
            assertEquals(100, output.size())
        }
        assertTrue(channel.readTimeouts.size > 6)
        assertTrue(channel.readTimeouts.all { it == 30_000 })
    }

    @Test
    fun `aborting a broken session releases USB without another protocol command`() {
        val channel = TimedCameraChannel(FakeCamera())
        val session = PtpSession(PtpTransport(channel)).also { it.open() }
        channel.responseDelayMs = { 31_000 }
        assertFailsWith<PtpTimeoutError> { session.getObjectHandles(-1) }
        val sent = channel.commands.size

        session.abort()

        assertFalse(session.isOpen)
        assertTrue(channel.camera.closed)
        assertEquals(sent, channel.commands.size)
    }

    @Test
    fun `an immediate USB error retains its actual wait and property instead of becoming a timeout`() {
        val channel = TimedCameraChannel(FakeCamera())
        val session = PtpSession(PtpTransport(channel)).also { it.open() }
        channel.readFailure = { PtpIoError(UsbTransferDirection.READ, 0x81, 65_536, 5_000, 2) }

        val error = assertFailsWith<PtpIoError> { session.readPropertyBytes(0xd185) }

        assertEquals(2, error.elapsedMs)
        assertEquals(5_000, error.timeoutMs)
        assertEquals(UsbTransferDirection.READ, error.direction)
        assertEquals(0x81, error.endpointAddress)
        assertEquals(Operation.GET_DEVICE_PROP_VALUE, error.operation)
        assertEquals(0xd185, error.property)
        assertTrue(error.message.orEmpty().contains("after 2 ms"))
        assertFalse(error.message.orEmpty().contains("did not answer within"))
    }

    @Test
    fun `I-O failure context is preserved for writes streaming uploads and downloads`() {
        val operations = listOf(Operation.SET_DEVICE_PROP_VALUE, Operation.FUJI_SEND_OBJECT, Operation.GET_OBJECT)
        operations.forEach { operation ->
            val channel = TimedCameraChannel(FakeCamera())
            val session = PtpSession(PtpTransport(channel)).also { it.open() }
            channel.readFailure = { PtpIoError(UsbTransferDirection.READ, 0x81, 65_536, 5_000, 0) }
            val error = assertFailsWith<PtpIoError> {
                when (operation) {
                    Operation.SET_DEVICE_PROP_VALUE -> session.setPropertyBytes(0xd183, packU16(0))
                    Operation.FUJI_SEND_OBJECT -> session.sendFujiRawObject(ByteArrayInputStream(byteArrayOf(1)), 1)
                    else -> session.getObject(1, ByteArrayOutputStream(), maxBytes = 100)
                }
            }
            assertEquals(operation, error.operation)
            assertEquals(0, error.elapsedMs)
            assertEquals(0xd183.takeIf { operation == Operation.SET_DEVICE_PROP_VALUE }, error.property)
        }
    }

    @Test
    fun `an initial USB write failure releases the connection and identifies OpenSession`() {
        val camera = FakeCamera()
        var writes = 0
        val channel = object : BulkChannel by camera {
            override fun write(bytes: ByteArray) {
                writes++
                throw PtpIoError(UsbTransferDirection.WRITE, 0x02, bytes.size, 5_000, 1)
            }
        }
        val session = PtpSession(PtpTransport(channel))

        val error = assertFailsWith<PtpIoError> { session.open() }

        assertEquals(Operation.OPEN_SESSION, error.operation)
        assertEquals(UsbTransferDirection.WRITE, error.direction)
        assertEquals(1, writes)
        assertTrue(camera.closed)
        assertFalse(session.isOpen)
    }
}
