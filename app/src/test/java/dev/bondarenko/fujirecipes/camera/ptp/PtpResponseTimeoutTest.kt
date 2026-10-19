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
}
