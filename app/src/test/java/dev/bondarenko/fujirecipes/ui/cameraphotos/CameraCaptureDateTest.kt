package dev.bondarenko.fujirecipes.ui.cameraphotos

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CameraCaptureDateTest {
    @Test
    fun `formats a PTP timestamp for people`() {
        assertEquals(
            "Sep 5, 2026, 1:20 PM",
            formatCameraCaptureDate("20260905T132036", Locale.US),
        )
    }

    @Test
    fun `returns null for an unknown date shape`() {
        assertNull(formatCameraCaptureDate("not-a-date", Locale.US))
    }
}
