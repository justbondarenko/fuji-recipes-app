package dev.bondarenko.fujirecipes.camera.usb

import dev.bondarenko.fujirecipes.camera.FakeCamera
import dev.bondarenko.fujirecipes.camera.plan.PRESET_BLOCK
import dev.bondarenko.fujirecipes.camera.plan.PRESET_MATRIX_CODES
import dev.bondarenko.fujirecipes.camera.plan.PRESET_NAME_PROPERTY
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
import dev.bondarenko.fujirecipes.camera.ptp.packPtpString
import dev.bondarenko.fujirecipes.camera.ptp.packU16
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
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

    /** A body whose seven slots hold different values for one preset property. */
    private fun cameraWithSlots(varying: Map<Int, Int>): FakeCamera = FakeCamera().apply {
        (1..7).forEach { slot ->
            slotNames[slot] = "Recipe $slot"
            slotSettings[slot] = mutableMapOf(
                0xd192 to (varying[slot] ?: 0x0b),
                // Identical in every slot, which is what makes it a constant rather than a
                // setting — the distinction the whole slot table exists to show.
                0xd1a1 to 0x8000,
            )
        }
    }

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

    // ─── The general walk ───────────────────────────────────────────────────

    /**
     * The preset registers follow the selector, so a reading of them among the general
     * properties would record whichever slot happened to be current under no label at all.
     */
    @Test
    fun `the preset block is left out of the general walk`() {
        val report = read(cameraWithSlots(emptyMap()))

        assertTrue(report.properties.none { it.code in PRESET_BLOCK })
        assertTrue(report.properties.any { it.code == USB_MODE_PROPERTY })
    }

    @Test
    fun `codes outside the block that the body never listed are probed anyway`() {
        val report = read(FakeCamera())

        // 0xD36A and 0xD36D are not in FakeCamera's device info and must still appear.
        assertTrue(report.properties.any { it.code == 0xd36a })
        assertTrue(report.properties.any { it.code == 0xd36d })
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
            describeAnswers[USB_MODE_PROPERTY] =
                rangeDescription(USB_MODE_PROPERTY, current = 6, min = 0, max = 8)
        }

        val mode = read(camera).properties.first { it.code == USB_MODE_PROPERTY }

        assertEquals(ProbeOutcome.DESCRIBED, mode.outcome)
        assertEquals("UInt16", mode.dataTypeLabel)
        assertEquals("6", mode.value)
        assertEquals("0..8 step 1", mode.allowed)
    }

    /**
     * Not a rare path: an X-T50 in RAW conversion mode advertises `GetDevicePropDesc` and then
     * refuses it for every property, so every row on that body comes from the raw read.
     */
    @Test
    fun `a property with no description is recorded as raw bytes`() {
        val camera = FakeCamera().apply {
            propertyValues[0xd36a] = byteArrayOf(0x0a, 0x00, 0x00, 0x00)
        }

        val battery = read(camera).properties.first { it.code == 0xd36a }

        assertEquals(ProbeOutcome.VALUE_ONLY, battery.outcome)
        assertEquals("0A 00 00 00", battery.value)
    }

    /**
     * A string carries its own length, and that length is checkable — so it is the one payload
     * worth decoding without a declared type. `“Kodak Gold 200”` beats sixteen bytes of UCS-2.
     */
    @Test
    fun `an undescribed payload that is plainly a string is decoded`() {
        val camera = FakeCamera().apply {
            propertyValues[0xd36d] = packPtpString("XF23mmF2 R WR")
        }

        val lens = read(camera).properties.first { it.code == 0xd36d }

        assertEquals("“XF23mmF2 R WR”", lens.value)
    }

    /** The length check is what keeps a number from being read as a string. */
    @Test
    fun `a number whose first byte looks like a length is still hex`() {
        val camera = FakeCamera().apply {
            // 0x0F would claim fifteen characters and a 31-byte payload.
            propertyValues[0xd36a] = byteArrayOf(0x0f, 0x00)
        }

        assertEquals("0F 00", read(camera).properties.first { it.code == 0xd36a }.value)
    }

    @Test
    fun `a refusal is a recorded result, not a missing row`() {
        val refused = read(FakeCamera()).properties.first { it.code == 0xd36d }

        assertEquals(ProbeOutcome.REFUSED, refused.outcome)
        assertEquals("DevicePropNotSupported", refused.refusal)
    }

    // ─── The slot table ─────────────────────────────────────────────────────

    @Test
    fun `all seven slots are read, each with its own name`() {
        val report = read(cameraWithSlots(emptyMap()))

        assertEquals(7, report.slots.size)
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), report.slots.map { it.slot })
        assertEquals("Recipe 1", report.slots.first().name)
        assertEquals("Recipe 7", report.slots.last().name)
    }

    /**
     * The whole point of reading seven slots rather than one: a value that varies is a setting,
     * and one that does not is a constant the camera keeps there. Neither is visible from C1.
     */
    @Test
    fun `a value that varies between slots is recorded per slot`() {
        val report = read(cameraWithSlots(mapOf(1 to 0x0b, 2 to 0x11, 3 to 0x14)))

        assertEquals(0x0b, report.slots[0].values[0xd192])
        assertEquals(0x11, report.slots[1].values[0xd192])
        assertEquals(0x14, report.slots[2].values[0xd192])

        // And one that does not vary reads the same every time.
        assertTrue(report.slots.all { it.values[0xd1a1] == 0x8000 })
    }

    @Test
    fun `every slot carries the whole preset table, refusals included`() {
        val report = read(cameraWithSlots(emptyMap()))

        report.slots.forEach { slot ->
            assertEquals(PRESET_MATRIX_CODES.toSet(), slot.values.keys)
        }
        // FakeCamera's slots hold two codes; the rest are refused and recorded as null.
        assertNull(report.slots.first().values[0xd19d])
    }

    @Test
    fun `the selector is driven to each slot in turn`() {
        val camera = cameraWithSlots(emptyMap())

        read(camera)

        val selected = camera.writes
            .filter { it.property == PRESET_SLOT_PROPERTY }
            .map { unpack(it.payload) }

        assertEquals(listOf(1, 1, 2, 3, 4, 5, 6, 7), selected)
    }

    /**
     * A body with no selector — a card reader — has no slots to walk, and finding that out
     * costs one round trip rather than a hundred and eighty refusals.
     */
    @Test
    fun `a body that refuses the selector yields no slot table and no wasted reads`() {
        val camera = FakeCamera().apply {
            refuseProperty[PRESET_SLOT_PROPERTY] = ResponseCode.ACCESS_DENIED
        }

        val report = read(camera)

        assertTrue(report.slots.isEmpty())
        assertNull(report.selectedSlot)
        assertTrue(report.properties.isNotEmpty())
        assertTrue(camera.reads.none { it == PRESET_NAME_PROPERTY })
    }

    @Test
    fun `a body with a selector says which slot the general walk was taken at`() {
        assertEquals(1, read(cameraWithSlots(emptyMap())).selectedSlot)
    }

    // ─── Failure and progress ───────────────────────────────────────────────

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

    @Test
    fun `progress counts every step, slots included, and ends at the total`() {
        val seen = mutableListOf<Pair<Int, Int>>()

        val report = readCameraReport(
            session = connected(cameraWithSlots(emptyMap())),
            appVersion = "1.1.0",
            capturedAt = "2026-09-08T21:17:00Z",
            onProgress = { done, total -> seen += done to total },
            sleep = {},
        )

        val expected = report.properties.size + 7 * (1 + PRESET_MATRIX_CODES.size)

        assertEquals(expected, seen.size)
        assertEquals(1 to expected, seen.first())
        assertEquals(expected to expected, seen.last())
    }

    private fun unpack(payload: ByteArray): Int =
        (payload[0].toInt() and 0xff) or ((payload[1].toInt() and 0xff) shl 8)
}
