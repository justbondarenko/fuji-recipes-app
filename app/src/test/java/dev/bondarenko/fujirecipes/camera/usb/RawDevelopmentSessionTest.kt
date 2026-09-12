package dev.bondarenko.fujirecipes.camera.usb

import dev.bondarenko.fujirecipes.camera.FakeCamera
import dev.bondarenko.fujirecipes.camera.ptp.PtpSession
import dev.bondarenko.fujirecipes.camera.ptp.PtpTransport
import dev.bondarenko.fujirecipes.camera.ptp.unpackU16
import dev.bondarenko.fujirecipes.camera.raw.RAW_CONVERSION_TRIGGER_PROPERTY
import dev.bondarenko.fujirecipes.camera.raw.RAW_PROFILE_PROPERTY
import dev.bondarenko.fujirecipes.data.model.Recipe
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith

class RawDevelopmentSessionTest {
    @Test
    fun `uploads patches renders downloads and deletes only the new result`() {
        val camera = FakeCamera(
            model = "X100VI",
            propertyCodes = listOf(RAW_PROFILE_PROPERTY, RAW_CONVERSION_TRIGGER_PROPERTY),
        )
        camera.propertyValues[RAW_PROFILE_PROPERTY] = nativeProfile()
        val jpeg = testJpeg(width = 10_560, height = 6_000)
        camera.afterPropertyWrite = { property, payload ->
            if (property == RAW_CONVERSION_TRIGGER_PROPERTY) {
                assertEquals(0, unpackU16(payload))
                camera.objectInfos[91] = byteArrayOf(1)
                camera.objects[91] = jpeg
            }
        }
        val session = openSession(camera)
        val directory = createTempDirectory("raw-session-test").toFile()
        val raf = directory.rafFile()
        val output = directory.resolve("result.jpg")

        val result = RawLabSession().render(session, raf, Recipe("r1", "Test").settings, output)

        assertContentEquals(raf.readBytes(), camera.sentObject)
        assertContentEquals(jpeg, result.jpeg.readBytes())
        assertEquals(91, result.outputHandle)
        assertEquals(10_560, result.width)
        assertEquals(6_000, result.height)
        assertFalse(camera.objects.containsKey(91))
        assertFalse(camera.objectInfos.containsKey(91))
    }

    @Test
    fun `rejects an unrecognised X-T50 profile without triggering conversion`() {
        val camera = FakeCamera(
            model = "X-T50",
            propertyCodes = listOf(RAW_PROFILE_PROPERTY, RAW_CONVERSION_TRIGGER_PROPERTY),
        )
        val profile = nativeProfile()
        camera.propertyValues[RAW_PROFILE_PROPERTY] = profile
        val session = openSession(camera)
        val directory = createTempDirectory("raw-session-test").toFile()

        val error = assertFailsWith<RawProfileCalibrationRequired> {
            RawLabSession().render(
                session,
                directory.rafFile(),
                Recipe("r1", "Test").settings,
                directory.resolve("result.jpg"),
            )
        }

        assertContentEquals(profile, error.profile)
        assertEquals("X-T50", error.cameraModel)
        assertFalse(camera.writes.any { it.property == RAW_CONVERSION_TRIGGER_PROPERTY })
    }

    @Test
    fun `uses trigger zero with the captured X-T50 profile`() {
        val camera = xT50Camera(handle = 92)
        val session = openSession(camera)
        val directory = createTempDirectory("raw-session-test").toFile()

        val result = RawLabSession().render(
            session,
            directory.rafFile(),
            Recipe("r1", "Test").settings,
            directory.resolve("result.jpg"),
        )

        assertEquals(10_560, result.width)
        assertEquals(6_000, result.height)
        assertEquals(0, unpackU16(camera.writes.last { it.property == RAW_CONVERSION_TRIGGER_PROPERTY }.payload))
    }

    @Test
    fun `does not read back the staged profile before triggering conversion`() {
        val camera = xT50Camera(handle = 93).apply { echoWrites = false }
        val session = openSession(camera)
        val directory = createTempDirectory("raw-session-test").toFile()

        val result = RawLabSession().render(
            session,
            directory.rafFile(),
            Recipe("r1", "Test").settings,
            directory.resolve("result.jpg"),
        )

        assertEquals(10_560, result.width)
        assertEquals(1, camera.reads.count { it == RAW_PROFILE_PROPERTY })
    }

    private fun openSession(camera: FakeCamera): PtpSession =
        PtpSession(PtpTransport(camera)).also { it.open() }

    /** The fake's `objectInfos` decide which handles exist, so each render needs its own. */
    private fun xT50Camera(handle: Int): FakeCamera = FakeCamera(
        model = "X-T50",
        propertyCodes = listOf(RAW_PROFILE_PROPERTY, RAW_CONVERSION_TRIGGER_PROPERTY),
    ).apply {
        propertyValues[RAW_PROFILE_PROPERTY] = xT50Profile()
        var next = handle
        afterPropertyWrite = { property, _ ->
            if (property == RAW_CONVERSION_TRIGGER_PROPERTY) {
                objectInfos[next] = byteArrayOf(1)
                objects[next] = testJpeg(10_560, 6_000)
                next += 1
            }
        }
    }

    private fun File.rafFile(): File =
        resolve("source.raf").apply { writeBytes(byteArrayOf(9, 8, 7)) }

    private fun nativeProfile(): ByteArray = ByteArray(625).also {
        ByteBuffer.wrap(it).order(ByteOrder.LITTLE_ENDIAN).putShort(0, 28)
    }

    private fun xT50Profile(): ByteArray {
        val encoded = checkNotNull(javaClass.getResource("/camera/raw/x-t50-d185.b64"))
            .readText()
            .trim()
        return Base64.getMimeDecoder().decode(encoded)
    }

    private fun testJpeg(width: Int, height: Int): ByteArray = byteArrayOf(
        0xff.toByte(), 0xd8.toByte(),
        0xff.toByte(), 0xe0.toByte(), 0x00, 0x02,
        0xff.toByte(), 0xc0.toByte(), 0x00, 0x07, 0x08,
        (height shr 8).toByte(), height.toByte(),
        (width shr 8).toByte(), width.toByte(),
    )
}
