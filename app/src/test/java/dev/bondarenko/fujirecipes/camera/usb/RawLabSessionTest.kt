package dev.bondarenko.fujirecipes.camera.usb

import dev.bondarenko.fujirecipes.camera.FakeCamera
import dev.bondarenko.fujirecipes.camera.ptp.PtpSession
import dev.bondarenko.fujirecipes.camera.ptp.PtpTransport
import dev.bondarenko.fujirecipes.camera.ptp.ResponseCode
import dev.bondarenko.fujirecipes.camera.raw.RAW_CONVERSION_TRIGGER_PROPERTY
import dev.bondarenko.fujirecipes.camera.raw.RAW_PROFILE_PROPERTY
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * The lab's central bet: one upload, many renders.
 *
 * These hold the Kotlin side of that to its promise. Whether the **camera** honours it is
 * `specs/plans/raw-development-lab.md`'s Gate A, which only hardware can answer.
 */
class RawLabSessionTest {

    @Test
    fun `renders repeatedly from a single upload`() {
        val camera = renderingCamera()
        val session = openSession(camera)
        val lab = RawLabSession()
        val directory = createTempDirectory("raw-lab-test").toFile()
        val raf = directory.raf()

        repeat(3) { index ->
            lab.render(
                session,
                raf,
                settingsWith("filmSimulation", FILM_SIMULATIONS[index]),
                directory.resolve("render-$index.jpg"),
            )
        }

        assertEquals(1, camera.uploadCount)
        assertEquals(3, camera.writes.count { it.property == RAW_CONVERSION_TRIGGER_PROPERTY })
        assertTrue(lab.isLoaded)
    }

    @Test
    fun `reads the base profile again for every render`() {
        // The camera's answer describes the RAF *and* the settings it currently holds, so a
        // cached base would carry the previous render's values into the next patch.
        val camera = renderingCamera()
        val session = openSession(camera)
        val lab = RawLabSession()
        val directory = createTempDirectory("raw-lab-test").toFile()
        val raf = directory.raf()

        repeat(2) { index ->
            lab.render(session, raf, JsonObject(emptyMap()), directory.resolve("r$index.jpg"))
        }

        assertEquals(2, camera.reads.count { it == RAW_PROFILE_PROPERTY })
    }

    @Test
    fun `uploads again after the load is invalidated`() {
        val camera = renderingCamera()
        val session = openSession(camera)
        val lab = RawLabSession()
        val directory = createTempDirectory("raw-lab-test").toFile()
        val raf = directory.raf()

        lab.render(session, raf, JsonObject(emptyMap()), directory.resolve("first.jpg"))
        lab.invalidate()
        assertFalse(lab.isLoaded)
        lab.render(session, raf, JsonObject(emptyMap()), directory.resolve("second.jpg"))

        assertEquals(2, camera.uploadCount)
    }

    @Test
    fun `uploads again when a different RAF is rendered`() {
        val camera = renderingCamera()
        val session = openSession(camera)
        val lab = RawLabSession()
        val directory = createTempDirectory("raw-lab-test").toFile()

        lab.render(session, directory.raf("one.raf"), JsonObject(emptyMap()), directory.resolve("a.jpg"))
        lab.render(session, directory.raf("two.raf"), JsonObject(emptyMap()), directory.resolve("b.jpg"))

        assertEquals(2, camera.uploadCount)
    }

    @Test
    fun `a refused render keeps the uploaded file`() {
        // A camera that will not take the profile says nothing about the RAF it is holding, and
        // throwing the upload away would cost another full transfer to learn the same thing.
        val camera = renderingCamera()
        val session = openSession(camera)
        val lab = RawLabSession()
        val directory = createTempDirectory("raw-lab-test").toFile()
        val raf = directory.raf()

        lab.render(session, raf, JsonObject(emptyMap()), directory.resolve("first.jpg"))
        camera.refuseProperty[RAW_PROFILE_PROPERTY] = ResponseCode.ACCESS_DENIED
        assertFailsWith<Exception> {
            lab.render(session, raf, JsonObject(emptyMap()), directory.resolve("refused.jpg"))
        }

        assertTrue(lab.isLoaded)
        assertEquals(1, camera.uploadCount)
    }

    private fun openSession(camera: FakeCamera): PtpSession =
        PtpSession(PtpTransport(camera)).also { it.open() }

    /** A body that produces one new object per conversion trigger, as the X100VI adapter expects. */
    private fun renderingCamera(): FakeCamera = FakeCamera(
        model = "X100VI",
        propertyCodes = listOf(RAW_PROFILE_PROPERTY, RAW_CONVERSION_TRIGGER_PROPERTY),
    ).apply {
        propertyValues[RAW_PROFILE_PROPERTY] = nativeProfile()
        var handle = 100
        afterPropertyWrite = { property, _ ->
            if (property == RAW_CONVERSION_TRIGGER_PROPERTY) {
                objectInfos[handle] = byteArrayOf(1)
                objects[handle] = TEST_JPEG
                handle += 1
            }
        }
    }

    private fun File.raf(name: String = "source.raf"): File =
        resolve(name).apply { writeBytes(byteArrayOf(9, 8, 7)) }

    private fun settingsWith(key: String, value: String): JsonObject =
        buildJsonObject { put(key, value) }

    private fun nativeProfile(): ByteArray = ByteArray(625).also {
        ByteBuffer.wrap(it).order(ByteOrder.LITTLE_ENDIAN).putShort(0, 28)
    }

    private companion object {
        val FILM_SIMULATIONS = listOf("provia", "velvia", "classic-chrome")

        val TEST_JPEG = byteArrayOf(
            0xff.toByte(), 0xd8.toByte(),
            0xff.toByte(), 0xe0.toByte(), 0x00, 0x02,
            0xff.toByte(), 0xc0.toByte(), 0x00, 0x07, 0x08,
            0x17, 0x70, 0x29, 0x40,
        )
    }
}
