package dev.bondarenko.fujirecipes.camera.usb

import dev.bondarenko.fujirecipes.camera.FakeCamera
import dev.bondarenko.fujirecipes.camera.ptp.ObjectInfoTest
import dev.bondarenko.fujirecipes.camera.ptp.PtpObject
import dev.bondarenko.fujirecipes.camera.ptp.PtpSession
import dev.bondarenko.fujirecipes.camera.ptp.PtpTransport
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CameraMediaReaderTest {
    private fun opened(camera: FakeCamera) = PtpSession(PtpTransport(camera)).also { it.open() }

    @Test
    fun `lists only JPEGs and sorts newest first`() {
        val camera = FakeCamera()
        add(camera, 10, "OLD.JPG", "20250101T000000", PtpObject.FORMAT_JPEG)
        add(camera, 20, "RAW.RAF", "20270101T000000", 0x3800)
        add(camera, 30, "NEW.JPG", "20260101T000000", PtpObject.FORMAT_JPEG)

        val media = listCameraJpegs(opened(camera))

        assertEquals(listOf("NEW.JPG", "OLD.JPG"), media.map { it.info.filename })
    }

    @Test
    fun `lists RAF candidates independently of their vendor object format`() {
        val camera = FakeCamera()
        add(camera, 10, "OLD.RAF", "20250101T000000", 0xb103)
        add(camera, 20, "NEW.raf", "20260101T000000", 0x3800)
        add(camera, 30, "JPEG.JPG", "20270101T000000", PtpObject.FORMAT_JPEG)

        val media = listCameraRafs(opened(camera))

        assertEquals(listOf("NEW.raf", "OLD.RAF"), media.map { it.info.filename })
    }

    @Test
    fun `lists matching JPEG and RAF as independent files`() {
        val camera = FakeCamera()
        add(camera, 10, "DSCF0001.JPG", "20260101T000000", PtpObject.FORMAT_JPEG)
        add(camera, 11, "DSCF0001.RAF", "20260101T000000", 0xb103)
        add(camera, 20, "DSCF0002.JPG", "20260102T000000", PtpObject.FORMAT_JPEG)

        val files = listCameraFiles(opened(camera))

        assertEquals(
            listOf("DSCF0002.JPG", "DSCF0001.RAF", "DSCF0001.JPG"),
            files.map { it.info.filename },
        )
        assertEquals(listOf(20, 11, 10), files.map { it.handle })
    }

    @Test
    fun `streams a RAF and verifies its signature`() {
        val camera = FakeCamera()
        val raf = "FUJIFILMCCD-RAW ".toByteArray() + ByteArray(1024) { 7 }
        add(camera, 7, "DSCF0001.RAF", "20260101T000000", 0xb103, raf)
        val session = opened(camera)
        val media = listCameraRafs(session).single()
        val output = ByteArrayOutputStream()

        val written = downloadCameraRaf(session, media, output)

        assertEquals(raf.size.toLong(), written)
        assertContentEquals(raf, output.toByteArray())
    }

    @Test
    fun `rejects a camera object that is not really a RAF`() {
        val camera = FakeCamera()
        add(camera, 7, "DSCF0001.RAF", "20260101T000000", 0xb103, byteArrayOf(1, 2, 3))
        val session = opened(camera)

        assertFailsWith<CameraMediaError> {
            downloadCameraRaf(session, listCameraRafs(session).single(), ByteArrayOutputStream())
        }
    }

    @Test
    fun `returns valid thumbnails and rejects malformed ones`() {
        val camera = FakeCamera()
        camera.thumbnails[1] = byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte(), 1)
        camera.thumbnails[2] = byteArrayOf(1, 2, 3)
        val session = opened(camera)

        assertContentEquals(camera.thumbnails[1], readCameraThumbnail(session, 1))
        assertNull(readCameraThumbnail(session, 2))
    }

    @Test
    fun `streams an object larger than the old control-container limit`() {
        val camera = FakeCamera()
        val jpeg = ByteArray(9 * 1024 * 1024) { (it and 0xff).toByte() }.also {
            it[0] = 0xff.toByte()
            it[1] = 0xd8.toByte()
            it[2] = 0xff.toByte()
        }
        add(camera, 7, "LARGE.JPG", "20260101T000000", PtpObject.FORMAT_JPEG, jpeg)
        camera.chunkSize = 31 * 1024
        val session = opened(camera)
        val media = listCameraJpegs(session).single()
        val output = ByteArrayOutputStream(jpeg.size)

        val written = downloadCameraJpeg(session, media, output)

        assertEquals(jpeg.size.toLong(), written)
        assertContentEquals(jpeg, output.toByteArray())
    }

    @Test
    fun `rejects an advertised image before downloading when it is too large`() {
        val camera = FakeCamera()
        add(camera, 7, "HUGE.JPG", "20260101T000000", PtpObject.FORMAT_JPEG, byteArrayOf(1))
        camera.objectInfos[7] = ObjectInfoTest.objectInfo("HUGE.JPG", MAX_CAMERA_JPEG_BYTES + 1)
        val session = opened(camera)
        val media = listCameraJpegs(session).single()

        val error = assertFailsWith<CameraMediaError> {
            downloadCameraJpeg(session, media, ByteArrayOutputStream())
        }

        assertEquals(CameraMediaFailure.TOO_LARGE, error.reason)
    }

    // ─── Cancellation ───────────────────────────────────────────────────────

    private fun jpeg(size: Int): ByteArray = ByteArray(size) { (it and 0xff).toByte() }.also {
        it[0] = 0xff.toByte()
        it[1] = 0xd8.toByte()
        it[2] = 0xff.toByte()
    }

    /**
     * The point of draining rather than abandoning: after a cancel the camera is still in step,
     * so the very next download works. Hanging up mid-container would leave the tail of the
     * cancelled image queued, and the next command would read it as its own reply.
     */
    @Test
    fun `a cancelled download stops writing and leaves the session usable`() {
        val camera = FakeCamera()
        val big = jpeg(512 * 1024)
        val small = jpeg(64)
        add(camera, 7, "BIG.JPG", "20260102T000000", PtpObject.FORMAT_JPEG, big)
        add(camera, 8, "SMALL.JPG", "20260101T000000", PtpObject.FORMAT_JPEG, small)
        camera.chunkSize = 16 * 1024
        val session = opened(camera)
        val files = listCameraJpegs(session)
        val output = ByteArrayOutputStream()
        var polls = 0

        val error = assertFailsWith<CameraMediaError> {
            downloadCameraJpeg(
                session = session,
                media = files.first { it.handle == 7 },
                output = output,
                isCancelled = { polls++ > 0 },
            )
        }

        assertEquals(CameraMediaFailure.CANCELLED, error.reason)
        assertTrue(output.size() < big.size, "the whole image was written anyway")

        val second = ByteArrayOutputStream()
        val written = downloadCameraJpeg(session, files.first { it.handle == 8 }, second)

        assertEquals(small.size.toLong(), written)
        assertContentEquals(small, second.toByteArray())
    }

    /** Cancelled before the first chunk lands: nothing is written at all. */
    @Test
    fun `a download cancelled at the outset writes nothing`() {
        val camera = FakeCamera()
        add(camera, 7, "BIG.JPG", "20260101T000000", PtpObject.FORMAT_JPEG, jpeg(256 * 1024))
        camera.chunkSize = 16 * 1024
        val session = opened(camera)
        val output = ByteArrayOutputStream()

        val error = assertFailsWith<CameraMediaError> {
            downloadCameraJpeg(
                session = session,
                media = listCameraJpegs(session).single(),
                output = output,
                isCancelled = { true },
            )
        }

        assertEquals(CameraMediaFailure.CANCELLED, error.reason)
        assertEquals(0, output.size())
    }

    /** A cancel that never fires must not change what a download does. */
    @Test
    fun `a download that is never cancelled is unaffected`() {
        val camera = FakeCamera()
        val image = jpeg(128 * 1024)
        add(camera, 7, "DSCF0001.JPG", "20260101T000000", PtpObject.FORMAT_JPEG, image)
        camera.chunkSize = 16 * 1024
        val session = opened(camera)
        val output = ByteArrayOutputStream()

        val written = downloadCameraJpeg(
            session = session,
            media = listCameraJpegs(session).single(),
            output = output,
            isCancelled = { false },
        )

        assertEquals(image.size.toLong(), written)
        assertContentEquals(image, output.toByteArray())
    }

    /** Progress must not keep climbing after the user has stopped it. */
    @Test
    fun `progress stops being reported once cancelled`() {
        val camera = FakeCamera()
        add(camera, 7, "BIG.JPG", "20260101T000000", PtpObject.FORMAT_JPEG, jpeg(512 * 1024))
        camera.chunkSize = 16 * 1024
        val session = opened(camera)
        val reported = mutableListOf<Long>()
        var polls = 0

        assertFailsWith<CameraMediaError> {
            downloadCameraJpeg(
                session = session,
                media = listCameraJpegs(session).single(),
                output = ByteArrayOutputStream(),
                onProgress = { written, _ -> reported += written },
                isCancelled = { polls++ > 1 },
            )
        }

        assertTrue(reported.isNotEmpty())
        assertTrue(reported.last() < 512 * 1024, "progress ran to completion after a cancel")
    }

    private fun add(
        camera: FakeCamera,
        handle: Int,
        filename: String,
        date: String,
        format: Int,
        contents: ByteArray = byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte()),
    ) {
        camera.objectInfos[handle] = ObjectInfoTest.objectInfo(filename, contents.size.toLong(), format, date)
        camera.objects[handle] = contents
        camera.objectStorageIds[handle] = camera.storageIds.single()
        camera.objectFormats[handle] = format
        camera.objectParents[handle] = 0
    }
}
