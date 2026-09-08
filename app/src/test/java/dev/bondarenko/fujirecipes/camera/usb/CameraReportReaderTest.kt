package dev.bondarenko.fujirecipes.camera.usb

import dev.bondarenko.fujirecipes.camera.FakeCamera
import dev.bondarenko.fujirecipes.camera.plan.ALWAYS_PROBED_PROPERTIES
import dev.bondarenko.fujirecipes.camera.plan.PRESET_SLOT_PROPERTY
import dev.bondarenko.fujirecipes.camera.plan.ProbeOutcome
import dev.bondarenko.fujirecipes.camera.plan.USB_MODE_PROPERTY
import dev.bondarenko.fujirecipes.camera.plan.UsbMode
import dev.bondarenko.fujirecipes.camera.ptp.DataType
import dev.bondarenko.fujirecipes.camera.ptp.Operation
import dev.bondarenko.fujirecipes.camera.ptp.PtpSession
import dev.bondarenko.fujirecipes.camera.ptp.PtpTimeoutError
import dev.bondarenko.fujirecipes.camera.ptp.PtpTransport
import dev.bondarenko.fujirecipes.camera.ptp.ResponseCode
import dev.bondarenko.fujirecipes.camera.ptp.packU16
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CameraReportReaderTest {

    private fun connected(camera: FakeCamera): PtpSession =
        PtpSession(PtpTransport(camera)).also { it.open() }

    private fun read(camera: FakeCamera) =
        readCameraReport(
            session = connected(camera),
            appVersion = "1.1.0",
            capturedAt = "2026-09-08T21:17:00Z",
            sleep = {},
        )

    /** A `DevicePropDesc` dataset with a range form. */
    private fun rangeDescription(code: Int, current: Int, min: Int, max: Int): ByteArray =
        packU16(code) + packU16(DataType.UINT16) + byteArrayOf(1) + packU16(0) +
            packU16(current) + byteArrayOf(0x01) + packU16(min) + packU16(max) + packU16(1)

    // ─── What ends up in the report ─────────────────────────────────────────

    @Test
    fun `the device info the session already read is carried through`() {
        val report = read(FakeCamera(model = "X-T50"))

        assertEquals("FUJIFILM", report.manufacturer)
        assertEquals("X-T50", report.model)
        assertEquals("1.20", report.firmware)
        assertEquals(100, report.standardVersion)
        assertTrue(report.operationsSupported.contains(Operation.GET_DEVICE_PROP_VALUE))
    }

    @Test
    fun `the usb mode is recorded as both a name and a number`() {
        val camera = FakeCamera().apply { propertyValues[USB_MODE_PROPERTY] = packU16(6) }

        val report = read(camera)

        assertEquals(UsbMode.RAW_CONVERSION, report.usbMode)
        assertEquals(6, report.usbModeRaw)
    }

    /**
     * The preset block follows the slot selector, so a report has to say which slot it read.
     * C1 is chosen for reproducibility.
     */
    @Test
    fun `the selector is pointed at C1 and the report says so`() {
        val camera = FakeCamera()

        val report = read(camera)

        assertEquals(1, report.selectedSlot)
        assertTrue(camera.writes.any { it.property == PRESET_SLOT_PROPERTY })
    }

    @Test
    fun `a body that refuses the selector still yields the rest of the report`() {
        val camera = FakeCamera().apply {
            refuseProperty[PRESET_SLOT_PROPERTY] = ResponseCode.ACCESS_DENIED
        }

        val report = read(camera)

        assertEquals(null, report.selectedSlot)
        assertTrue(report.properties.isNotEmpty())
    }

    // ─── Which codes get probed ─────────────────────────────────────────────

    /**
     * Fuji bodies do not reliably list their vendor properties, so a walk of the body's own
     * list alone would miss exactly the codes the report exists to document.
     */
    @Test
    fun `codes the body never listed are probed anyway`() {
        val report = read(FakeCamera())

        ALWAYS_PROBED_PROPERTIES.forEach { code ->
            assertNotNull(
                report.properties.firstOrNull { it.code == code },
                "0x${code.toString(16)} was not probed",
            )
        }
    }

    @Test
    fun `a code the body listed is marked as listed, and one it did not is not`() {
        // FakeCamera's device info lists 0xD18C, 0xD18D and 0xD18E.
        val report = read(FakeCamera())

        assertEquals(true, report.properties.first { it.code == 0xd18c }.listedByBody)
        assertEquals(false, report.properties.first { it.code == 0xd36a }.listedByBody)
    }

    @Test
    fun `every probed code appears once, however many lists named it`() {
        val report = read(FakeCamera())

        assertEquals(report.properties.size, report.properties.map { it.code }.distinct().size)
    }

    // ─── Per-property outcomes ──────────────────────────────────────────────

    @Test
    fun `a described property records its type, access and allowed values`() {
        val camera = FakeCamera().apply {
            describeAnswers[PRESET_SLOT_PROPERTY] =
                rangeDescription(PRESET_SLOT_PROPERTY, current = 1, min = 1, max = 7)
        }

        val slot = read(camera).properties.first { it.code == PRESET_SLOT_PROPERTY }

        assertEquals(ProbeOutcome.DESCRIBED, slot.outcome)
        assertEquals("UInt16", slot.dataTypeLabel)
        assertEquals(true, slot.writable)
        assertEquals("1", slot.value)
        assertEquals("1..7 step 1", slot.allowed)
    }

    /** A body that carries a property but will not describe it still contributes its bytes. */
    @Test
    fun `a property with no description is recorded as raw bytes`() {
        val camera = FakeCamera().apply {
            propertyValues[0xd36a] = byteArrayOf(0x2a, 0x00)
        }

        val battery = read(camera).properties.first { it.code == 0xd36a }

        assertEquals(ProbeOutcome.VALUE_ONLY, battery.outcome)
        assertEquals("2A 00", battery.value)
    }

    /**
     * Bytes are not decoded without a declared type. A number invented here would be read as
     * the camera's own answer by whoever the report is sent to.
     */
    @Test
    fun `undescribed bytes are never turned into a number`() {
        val camera = FakeCamera().apply { propertyValues[0xd310] = byteArrayOf(0x10, 0x27) }

        val value = read(camera).properties.first { it.code == 0xd310 }.value

        assertEquals("10 27", value)
    }

    @Test
    fun `a refusal is a recorded result, not a missing row`() {
        val report = read(FakeCamera())
        val refused = report.properties.first { it.code == 0xd36d }

        assertEquals(ProbeOutcome.REFUSED, refused.outcome)
        assertEquals("DevicePropNotSupported", refused.refusal)
    }

    /**
     * The opposite of `CameraFactsReader`: this one is started by the user and its output is
     * the record, so a dead pipe must stop it rather than produce a report full of holes.
     */
    @Test
    fun `a camera that stops answering fails the report rather than half-filling it`() {
        val camera = FakeCamera()
        val session = connected(camera)
        camera.silent = true

        assertFailsWith<PtpTimeoutError> {
            readCameraReport(session, "1.1.0", "2026-09-08T21:17:00Z", sleep = {})
        }
    }

    // ─── Progress ───────────────────────────────────────────────────────────

    @Test
    fun `progress counts up to the number of properties probed`() {
        val seen = mutableListOf<Pair<Int, Int>>()

        val report = readCameraReport(
            session = connected(FakeCamera()),
            appVersion = "1.1.0",
            capturedAt = "2026-09-08T21:17:00Z",
            onProgress = { done, total -> seen += done to total },
            sleep = {},
        )

        assertEquals(report.properties.size, seen.size)
        assertEquals(1 to report.properties.size, seen.first())
        assertEquals(report.properties.size to report.properties.size, seen.last())
    }
}
