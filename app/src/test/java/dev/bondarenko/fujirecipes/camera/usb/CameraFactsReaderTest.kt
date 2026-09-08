package dev.bondarenko.fujirecipes.camera.usb

import dev.bondarenko.fujirecipes.camera.FakeCamera
import dev.bondarenko.fujirecipes.camera.plan.BATTERY_LEVEL_PROPERTY
import dev.bondarenko.fujirecipes.camera.plan.LENS_NAME_PROPERTY
import dev.bondarenko.fujirecipes.camera.plan.TOTAL_SHOT_COUNT_PROPERTY
import dev.bondarenko.fujirecipes.camera.plan.USB_MODE_PROPERTY
import dev.bondarenko.fujirecipes.camera.plan.UsbMode
import dev.bondarenko.fujirecipes.camera.ptp.DataType
import dev.bondarenko.fujirecipes.camera.ptp.Operation
import dev.bondarenko.fujirecipes.camera.ptp.PtpSession
import dev.bondarenko.fujirecipes.camera.ptp.PtpTransport
import dev.bondarenko.fujirecipes.camera.ptp.ResponseCode
import dev.bondarenko.fujirecipes.camera.ptp.packPtpString
import dev.bondarenko.fujirecipes.camera.ptp.packU16
import dev.bondarenko.fujirecipes.camera.ptp.packU32
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CameraFactsReaderTest {

    private fun connected(camera: FakeCamera): PtpSession =
        PtpSession(PtpTransport(camera)).also { it.open() }

    // ─── USB mode ───────────────────────────────────────────────────────────

    @Test
    fun `reads the mode the body reports`() {
        val camera = FakeCamera()
        camera.propertyValues[USB_MODE_PROPERTY] = packU16(6)

        assertEquals(UsbMode.RAW_CONVERSION, readUsbMode(connected(camera)))
    }

    @Test
    fun `names the wrong mode rather than leaving the user to guess`() {
        val camera = FakeCamera()
        camera.propertyValues[USB_MODE_PROPERTY] = packU16(5)

        val mode = readUsbMode(connected(camera))

        assertEquals(UsbMode.TETHER_SHOOTING, mode)
        assertTrue(mode.isKnownWrongMode)
    }

    /**
     * A card-reader body, and any body whose firmware predates the property, both refuse it.
     * Neither is a fault and neither may be presented as a diagnosis.
     */
    @Test
    fun `a body that refuses the property is unreported, not wrong`() {
        val camera = FakeCamera()

        val mode = readUsbMode(connected(camera))

        assertEquals(UsbMode.UNREPORTED, mode)
        assertFalse(mode.isKnownWrongMode)
    }

    /** `GetDevicePropDesc` carries the type and the value together, so it is preferred. */
    @Test
    fun `takes the mode from the description when the body describes it`() {
        val camera = FakeCamera()
        camera.describeAnswers[USB_MODE_PROPERTY] =
            description(USB_MODE_PROPERTY, currentValue = 8)

        assertEquals(UsbMode.WEBCAM, readUsbMode(connected(camera)))
    }

    // ─── Details ────────────────────────────────────────────────────────────

    @Test
    fun `reads battery, shutter count and lens, with firmware and serial from device info`() {
        val camera = FakeCamera()
        camera.propertyValues[BATTERY_LEVEL_PROPERTY] = packU16(87)
        camera.propertyValues[TOTAL_SHOT_COUNT_PROPERTY] = packU32(142_037)
        camera.propertyValues[LENS_NAME_PROPERTY] = packPtpString("XF23mmF2 R WR")

        val session = connected(camera)
        val details = readCameraDetails(session, session.deviceInfo)

        assertEquals(87, details.batteryPercent)
        assertEquals(142_037, details.shutterCount)
        assertEquals("XF23mmF2 R WR", details.lens)
        assertEquals("1.20", details.firmware)
        assertEquals("A1B2C3D4", details.serialNumber)
    }

    /** Width comes from the payload length, so a two-byte count reads as a count. */
    @Test
    fun `decodes a numeric property at whatever width the body sent`() {
        val camera = FakeCamera()
        camera.propertyValues[TOTAL_SHOT_COUNT_PROPERTY] = packU16(9_001)

        val session = connected(camera)

        assertEquals(9_001, readCameraDetails(session, session.deviceInfo).shutterCount)
    }

    @Test
    fun `one refused property costs only that one field`() {
        val camera = FakeCamera()
        camera.propertyValues[BATTERY_LEVEL_PROPERTY] = packU16(42)
        // Shutter count and lens are simply never answered.

        val session = connected(camera)
        val details = readCameraDetails(session, session.deviceInfo)

        assertEquals(42, details.batteryPercent)
        assertNull(details.shutterCount)
        assertNull(details.lens)
        assertEquals("1.20", details.firmware)
    }

    /**
     * The guard earns its place here: `0xD36A` answering 4,660 means it is not a percentage on
     * this body, and the screen must show nothing rather than a plausible wrong number.
     */
    @Test
    fun `an implausible value is dropped rather than shown`() {
        val camera = FakeCamera()
        camera.propertyValues[BATTERY_LEVEL_PROPERTY] = packU16(4_660)
        camera.propertyValues[TOTAL_SHOT_COUNT_PROPERTY] = packU32(0)

        val session = connected(camera)
        val details = readCameraDetails(session, session.deviceInfo)

        assertNull(details.batteryPercent)
        assertNull(details.shutterCount)
    }

    @Test
    fun `a body that answers none of them yields an empty set of details`() {
        val camera = FakeCamera()
        val session = connected(camera)

        val details = readCameraDetails(session, null)

        assertTrue(details.isEmpty)
    }

    /**
     * The whole point of swallowing failures: a diagnostic read must never be able to take
     * down a connection that would otherwise work.
     */
    @Test
    fun `a body that refuses the operation outright still yields details`() {
        val camera = FakeCamera()
        val session = connected(camera)
        camera.refuseOperation[Operation.GET_DEVICE_PROP_DESC] = ResponseCode.OPERATION_NOT_SUPPORTED
        camera.refuseOperation[Operation.GET_DEVICE_PROP_VALUE] = ResponseCode.ACCESS_DENIED

        val details = readCameraDetails(session, session.deviceInfo)

        assertNull(details.batteryPercent)
        assertEquals("1.20", details.firmware)
        assertEquals(UsbMode.UNREPORTED, readUsbMode(session))
    }

    /** A minimal `DevicePropDesc` dataset: code, type, GetSet, default, current. */
    private fun description(code: Int, currentValue: Int): ByteArray =
        packU16(code) + packU16(DataType.UINT16) + byteArrayOf(1) + packU16(0) +
            packU16(currentValue)
}
