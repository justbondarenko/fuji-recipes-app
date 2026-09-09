package dev.bondarenko.fujirecipes.camera.plan

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CameraReportTest {

    private fun report(
        properties: List<ProbedProperty> = emptyList(),
        usbMode: UsbMode = UsbMode.RAW_CONVERSION,
        usbModeRaw: Int? = 6,
        slots: List<SlotProbe> = emptyList(),
    ) = CameraReport(
        appVersion = "1.1.0",
        capturedAt = "2026-09-08T21:17:00Z",
        manufacturer = "FUJIFILM",
        model = "X-T50",
        firmware = "1.10",
        standardVersion = 100,
        vendorExtensionId = 6L,
        vendorExtensionVersion = 100,
        vendorExtensionDescription = "fujifilm.co.jp: 1.0;",
        usbMode = usbMode,
        usbModeRaw = usbModeRaw,
        operationsSupported = listOf(0x1001, 0x900c),
        propertiesListed = listOf(0xd18c),
        selectedSlot = 1,
        properties = properties,
        slots = slots,
    )

    // ─── Provenance ─────────────────────────────────────────────────────────

    /**
     * The one thing about this report that is a promise rather than a convenience: it is
     * written to be pasted into a public issue, and it must not carry the owner's hardware id.
     */
    @Test
    fun `the serial number never reaches the text`() {
        val text = renderCameraReport(report())

        assertFalse(text.contains("A1B2C3D4"))
        assertTrue(text.contains("serial"))
        assertTrue(text.contains("omitted on purpose"))
    }

    @Test
    fun `the body, firmware and capture time are all named`() {
        val text = renderCameraReport(report())

        assertTrue(text.contains("X-T50"))
        assertTrue(text.contains("FUJIFILM"))
        assertTrue(text.contains("1.10"))
        assertTrue(text.contains("2026-09-08T21:17:00Z"))
        assertTrue(text.contains("C1"))
    }

    // ─── The USB mode line ──────────────────────────────────────────────────

    @Test
    fun `the usb mode prints its number as well as its name`() {
        assertTrue(renderCameraReport(report()).contains("6 (RAW conv./backup restore)"))
    }

    /** A mode nobody has seen is the whole finding, so the number must survive. */
    @Test
    fun `an unrecognised mode is reported as the value the body gave`() {
        val text = renderCameraReport(
            report(usbMode = UsbMode.UNRECOGNISED, usbModeRaw = 11),
        )

        assertTrue(text.contains("11 (unrecognised)"))
    }

    @Test
    fun `a body that would not say is not given a mode`() {
        val text = renderCameraReport(
            report(usbMode = UsbMode.UNREPORTED, usbModeRaw = null),
        )

        assertTrue(text.contains("USB mode") && text.contains("not reported"))
    }

    // ─── Property rows ──────────────────────────────────────────────────────

    @Test
    fun `a described property shows type, access, value and allowed values`() {
        val text = renderCameraReport(
            report(
                listOf(
                    ProbedProperty(
                        code = 0xd18c,
                        listedByBody = true,
                        outcome = ProbeOutcome.DESCRIBED,
                        dataTypeLabel = "UInt16",
                        writable = true,
                        value = "1",
                        allowed = "1..7 step 1",
                    ),
                ),
            ),
        )

        assertTrue(text.contains("0xD18C"))
        assertTrue(text.contains("PresetSlot"))
        assertTrue(text.contains("UInt16"))
        assertTrue(text.contains("rw"))
        assertTrue(text.contains("[1..7 step 1]"))
    }

    @Test
    fun `a refused property records the refusal rather than being dropped`() {
        val text = renderCameraReport(
            report(
                listOf(
                    ProbedProperty(
                        code = 0xd36a,
                        listedByBody = false,
                        outcome = ProbeOutcome.REFUSED,
                        refusal = "DevicePropNotSupported",
                    ),
                ),
            ),
        )

        assertTrue(text.contains("0xD36A"))
        assertTrue(text.contains("refused: DevicePropNotSupported"))
    }

    /**
     * The point of the whole exercise: a code this build will not name still appears, with the
     * body's own answer beside it. That row is the evidence that settles what the code means.
     */
    @Test
    fun `an unnamed code still gets a row carrying the body's answer`() {
        val text = renderCameraReport(
            report(
                listOf(
                    ProbedProperty(
                        code = 0xd007,
                        listedByBody = true,
                        outcome = ProbeOutcome.DESCRIBED,
                        dataTypeLabel = "UInt16",
                        writable = false,
                        value = "200",
                        allowed = "100, 200, 400",
                    ),
                ),
            ),
        )

        assertTrue(text.contains("0xD007"))
        assertTrue(text.contains("[100, 200, 400]"))
        // No invented name — the disputed shooting range is deliberately unnamed.
        assertFalse(text.contains("ColorTemperature"))
        assertFalse(text.contains("DRangeMode"))
    }

    @Test
    fun `a code the body did not list is marked`() {
        val text = renderCameraReport(
            report(
                listOf(
                    ProbedProperty(0xd18c, listedByBody = true, outcome = ProbeOutcome.REFUSED, refusal = "x"),
                    ProbedProperty(0xd36a, listedByBody = false, outcome = ProbeOutcome.REFUSED, refusal = "x"),
                ),
            ),
        )

        assertTrue(text.lines().any { it.startsWith("  0xD18C") })
        assertTrue(text.lines().any { it.startsWith("· 0xD36A") })
    }

    // ─── Allowed values ─────────────────────────────────────────────────────

    @Test
    fun `a range renders as bounds and step`() {
        assertEquals("1..7 step 1", renderAllowedValues(min = 1L, max = 7L, step = 1L))
        assertEquals("0..100 step 5", renderAllowedValues(min = 0L, max = 100L, step = 5L))
    }

    @Test
    fun `an enumeration renders as its values`() {
        assertEquals("1, 2, 3", renderAllowedValues(enumeration = listOf(1L, 2L, 3L)))
    }

    /** A body that enumerates hundreds of shutter speeds must not bury the rest of the report. */
    @Test
    fun `a long enumeration is truncated and says how much it left out`() {
        val rendered = renderAllowedValues(enumeration = (1L..100L).toList(), limit = 3)

        assertEquals("1, 2, 3, … 97 more", rendered)
    }

    @Test
    fun `a property that declares nothing renders nothing`() {
        assertNull(renderAllowedValues())
        assertNull(renderAllowedValues(enumeration = emptyList()))
    }

    @Test
    fun `a report from a body that answered nothing still renders`() {
        val text = renderCameraReport(report())

        assertTrue(text.contains("PROPERTIES (0 probed"))
        assertTrue(text.contains("none"))
    }

    // ─── The slot table ─────────────────────────────────────────────────────

    private fun slot(number: Int, name: String?, filmSim: Int, nr: Int?) = SlotProbe(
        slot = number,
        name = name,
        values = mapOf(0xd192 to filmSim, 0xd1a1 to nr),
    )

    /**
     * The question the table exists to answer, and it has to be answerable by reading across
     * one line: a row identical in all seven columns is a constant the camera keeps there, and
     * one that varies is a setting. `0xD1A1` is the live case — `eggricesoy/filmkit` calls it a
     * sentinel and this project's own table calls it a noise-reduction encoding.
     */
    @Test
    fun `a row shows the same code across every slot`() {
        val text = renderCameraReport(
            report(
                slots = listOf(
                    slot(1, "Kodak Gold 200", 0x0b, 0x8000),
                    slot(2, "Acros Night", 0x0c, 0x8000),
                ),
            ),
        )

        val filmSimRow = text.lines().first { it.contains("0xD192") }
        val nrRow = text.lines().first { it.contains("0xD1A1") }

        assertTrue(filmSimRow.contains("000B"), filmSimRow)
        assertTrue(filmSimRow.contains("000C"), filmSimRow)
        // Identical in both columns, which is the finding.
        assertEquals(2, Regex("8000").findAll(nrRow).count(), nrRow)
    }

    @Test
    fun `slot names are listed above the table`() {
        val text = renderCameraReport(
            report(slots = listOf(slot(1, "Kodak Gold 200", 0x0b, 0x8000))),
        )

        assertTrue(text.contains("CUSTOM SLOT NAMES"))
        assertTrue(text.contains("C1  “Kodak Gold 200”"))
    }

    @Test
    fun `an unnamed slot says so rather than being blank`() {
        val text = renderCameraReport(report(slots = listOf(slot(3, null, 0x0b, null))))

        assertTrue(text.contains("C3  (unnamed)"))
    }

    /** A refused code has to be visibly different from one that answered zero. */
    @Test
    fun `a refused slot value is marked, not shown as a number`() {
        val text = renderCameraReport(report(slots = listOf(slot(1, "x", 0x0b, null))))

        val nrRow = text.lines().first { it.contains("0xD1A1") }

        assertTrue(nrRow.contains("————"), nrRow)
        assertFalse(nrRow.contains("0000"), nrRow)
    }

    @Test
    fun `a body with no selector gets no table and says why`() {
        val text = renderCameraReport(report())

        assertTrue(text.contains("CUSTOM SLOTS (0)"))
        assertTrue(text.contains("would not take the slot selector"))
    }
}
