package dev.bondarenko.fujirecipes.ui.cameraphotos

import java.util.Locale
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CameraCaptureDateTest {
    @Test
    fun `formats a PTP timestamp for people`() {
        val formatted = formatCameraCaptureDate("20260905T132036", Locale.US)
        assertEquals(
            "Sep 5, 2026, 1:20 PM",
            formatted?.replace('\u202F', ' '),
        )
    }

    @Test
    fun `returns null for an unknown date shape`() {
        assertNull(formatCameraCaptureDate("not-a-date", Locale.US))
    }

    @Test
    fun `day is camera local even with fractional seconds and timezone suffix`() {
        assertEquals(LocalDate.of(2026, 9, 5), cameraCaptureDay("20260905T003036.5+1400"))
        assertEquals("Sep 5, 2026", formatCameraCaptureDay(LocalDate.of(2026, 9, 5), Locale.US))
    }

    @Test
    fun `missing and invalid calendar dates stay unknown`() {
        listOf("", "not-a-date", "20260230T120000", "20261301T120000").forEach {
            assertNull(cameraCaptureDay(it))
            assertNull(formatCameraCaptureDate(it, Locale.US))
        }
    }
}
