package dev.bondarenko.fujirecipes.camera.plan

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UsbModeTest {

    @Test
    fun `maps the three values libfuji documents`() {
        assertEquals(UsbMode.TETHER_SHOOTING, usbModeFor(5))
        assertEquals(UsbMode.RAW_CONVERSION, usbModeFor(6))
        assertEquals(UsbMode.WEBCAM, usbModeFor(8))
    }

    @Test
    fun `a value this build has not seen is unrecognised, not guessed at`() {
        assertEquals(UsbMode.UNRECOGNISED, usbModeFor(0))
        assertEquals(UsbMode.UNRECOGNISED, usbModeFor(7))
        assertEquals(UsbMode.UNRECOGNISED, usbModeFor(99))
    }

    /**
     * The load-bearing half. A mode warning shown to someone whose camera is set correctly is
     * worse than no warning at all, so only a positively-identified wrong mode may raise one.
     */
    @Test
    fun `only a positively identified wrong mode warns`() {
        assertTrue(UsbMode.TETHER_SHOOTING.isKnownWrongMode)
        assertTrue(UsbMode.WEBCAM.isKnownWrongMode)

        assertFalse(UsbMode.RAW_CONVERSION.isKnownWrongMode)
        assertFalse(UsbMode.UNREPORTED.isKnownWrongMode)
        assertFalse(UsbMode.UNRECOGNISED.isKnownWrongMode)
    }
}
