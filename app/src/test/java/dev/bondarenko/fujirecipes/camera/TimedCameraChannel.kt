package dev.bondarenko.fujirecipes.camera

import dev.bondarenko.fujirecipes.camera.ptp.BulkChannel
import dev.bondarenko.fujirecipes.camera.ptp.ContainerType
import dev.bondarenko.fujirecipes.camera.ptp.DEFAULT_TIMEOUT_MS
import dev.bondarenko.fujirecipes.camera.ptp.PtpContainer
import dev.bondarenko.fujirecipes.camera.ptp.PtpTimeoutError
import dev.bondarenko.fujirecipes.camera.ptp.unpackContainer

/** Models USB response latency without making a test wait on the wall clock. */
class TimedCameraChannel(val camera: FakeCamera) : BulkChannel by camera {
    val commands = mutableListOf<PtpContainer>()
    val readTimeouts = mutableListOf<Int>()
    var responseDelayMs: (PtpContainer) -> Int = { 0 }

    override fun write(bytes: ByteArray) {
        val container = unpackContainer(bytes)
        if (container.type == ContainerType.COMMAND) commands += container
        camera.write(bytes)
    }

    override fun read(maxBytes: Int): ByteArray = read(maxBytes, DEFAULT_TIMEOUT_MS)

    override fun read(maxBytes: Int, timeoutMs: Int): ByteArray {
        readTimeouts += timeoutMs
        if (responseDelayMs(commands.last()) > timeoutMs) throw PtpTimeoutError(timeoutMs)
        return camera.read(maxBytes)
    }
}
