package dev.bondarenko.fujirecipes.camera.usb

import dev.bondarenko.fujirecipes.camera.FakeCamera
import dev.bondarenko.fujirecipes.camera.ptp.PtpSession
import dev.bondarenko.fujirecipes.camera.ptp.PtpTransport
import dev.bondarenko.fujirecipes.camera.ptp.unpackU16
import dev.bondarenko.fujirecipes.camera.raw.RAW_CONVERSION_TRIGGER_PROPERTY
import dev.bondarenko.fujirecipes.camera.raw.RAW_PROFILE_PROPERTY
import dev.bondarenko.fujirecipes.data.model.Recipe
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
        val session = PtpSession(PtpTransport(camera))
        session.open()
        val directory = createTempDirectory("raw-session-test").toFile()
        val raf = directory.resolve("source.raf").apply { writeBytes(byteArrayOf(9, 8, 7)) }
        val output = directory.resolve("result.jpg")

        val result = developRaw(session, raf, Recipe("r1", "Test"), output)

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
        val session = PtpSession(PtpTransport(camera))
        session.open()
        val directory = createTempDirectory("raw-session-test").toFile()
        val raf = directory.resolve("source.raf")
            .apply { writeBytes(byteArrayOf(9, 8, 7)) }

        val error = assertFailsWith<RawProfileCalibrationRequired> {
            developRaw(session, raf, Recipe("r1", "Test"), directory.resolve("result.jpg"))
        }

        assertContentEquals(profile, error.profile)
        assertEquals("X-T50", error.cameraModel)
        assertFalse(camera.writes.any { it.property == RAW_CONVERSION_TRIGGER_PROPERTY })
    }

    @Test
    fun `uses trigger zero with the captured X-T50 profile`() {
        val camera = FakeCamera(
            model = "X-T50",
            propertyCodes = listOf(RAW_PROFILE_PROPERTY, RAW_CONVERSION_TRIGGER_PROPERTY),
        )
        camera.propertyValues[RAW_PROFILE_PROPERTY] = xT50Profile()
        camera.afterPropertyWrite = { property, payload ->
            if (property == RAW_CONVERSION_TRIGGER_PROPERTY) {
                assertEquals(0, unpackU16(payload))
                camera.objectInfos[92] = byteArrayOf(1)
                camera.objects[92] = testJpeg(10_560, 6_000)
            }
        }
        val session = PtpSession(PtpTransport(camera)).also { it.open() }
        val directory = createTempDirectory("raw-session-test").toFile()
        val raf = directory.resolve("source.raf").apply { writeBytes(byteArrayOf(9, 8, 7)) }

        val result = developRaw(
            session,
            raf,
            Recipe("r1", "Test"),
            directory.resolve("result.jpg"),
        )

        assertEquals(10_560, result.width)
        assertEquals(6_000, result.height)
    }

    @Test
    fun `does not read back the staged profile before triggering conversion`() {
        val camera = FakeCamera(
            model = "X-T50",
            propertyCodes = listOf(RAW_PROFILE_PROPERTY, RAW_CONVERSION_TRIGGER_PROPERTY),
        ).apply {
            propertyValues[RAW_PROFILE_PROPERTY] = xT50Profile()
            echoWrites = false
            afterPropertyWrite = { property, _ ->
                if (property == RAW_CONVERSION_TRIGGER_PROPERTY) {
                    objectInfos[93] = byteArrayOf(1)
                    objects[93] = testJpeg(10_560, 6_000)
                }
            }
        }
        val session = PtpSession(PtpTransport(camera)).also { it.open() }
        val directory = createTempDirectory("raw-session-test").toFile()
        val raf = directory.resolve("source.raf").apply { writeBytes(byteArrayOf(9, 8, 7)) }

        val result = developRaw(
            session,
            raf,
            Recipe("r1", "Test"),
            directory.resolve("result.jpg"),
        )

        assertEquals(10_560, result.width)
        assertEquals(1, camera.reads.count { it == RAW_PROFILE_PROPERTY })
    }

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
