package dev.bondarenko.fujirecipes.core.store

import dev.bondarenko.fujirecipes.camera.FakeCamera
import dev.bondarenko.fujirecipes.camera.ptp.ObjectInfoTest
import dev.bondarenko.fujirecipes.camera.ptp.PtpSession
import dev.bondarenko.fujirecipes.camera.ptp.PtpTransport
import dev.bondarenko.fujirecipes.camera.usb.CameraMediaObject
import dev.bondarenko.fujirecipes.camera.ptp.parseObjectInfo
import java.io.ByteArrayInputStream
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RawDevelopmentCacheTest {
    @Test
    fun `validates and atomically imports a RAF`() {
        val directory = createTempDirectory("raw-cache-test").toFile()
        val cache = RawDevelopmentCache(directory)
        val bytes = "FUJIFILMCCD-RAW payload".toByteArray()

        val file = cache.importRaf("DSCF 0001.RAF", ByteArrayInputStream(bytes), bytes.size.toLong())

        assertEquals("DSCF_0001.raf", file.name)
        assertContentEquals(bytes, file.readBytes())
        assertFalse(directory.resolve("DSCF_0001.raf.part").exists())
    }

    @Test
    fun `rejects a non-RAF without leaving a partial file`() {
        val directory = createTempDirectory("raw-cache-test").toFile()
        val cache = RawDevelopmentCache(directory)

        assertFailsWith<IllegalArgumentException> {
            cache.importRaf("wrong.bin", ByteArrayInputStream(byteArrayOf(1, 2, 3)), 3)
        }

        assertTrue(directory.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun `streams and validates a RAF selected from a camera card`() {
        val directory = createTempDirectory("raw-cache-test").toFile()
        val cache = RawDevelopmentCache(directory)
        val bytes = "FUJIFILMCCD-RAW camera payload".toByteArray()
        val infoBytes = ObjectInfoTest.objectInfo("DSCF0002.RAF", bytes.size.toLong(), 0xb103)
        val camera = FakeCamera().apply {
            objectInfos[22] = infoBytes
            objects[22] = bytes
        }
        val session = PtpSession(PtpTransport(camera)).also { it.open() }
        val media = CameraMediaObject(22, parseObjectInfo(infoBytes))

        val file = cache.download(session, media)

        assertContentEquals(bytes, file.readBytes())
        assertFalse(directory.resolve("DSCF0002.raf.part").exists())
    }
}
