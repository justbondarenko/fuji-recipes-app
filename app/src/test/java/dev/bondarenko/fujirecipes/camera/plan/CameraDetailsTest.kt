package dev.bondarenko.fujirecipes.camera.plan

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CameraDetailsTest {

    @Test
    fun `a battery percentage is accepted across its whole range`() {
        assertEquals(0, plausibleBatteryPercent(0))
        assertEquals(87, plausibleBatteryPercent(87))
        assertEquals(100, plausibleBatteryPercent(100))
    }

    /**
     * The property's meaning is taken on `libfuji`'s word, not from a capture. Anything outside
     * the range that word implies means the code holds something else on this body — bars, a
     * packed struct — and the honest answer is to show nothing.
     */
    @Test
    fun `a value that cannot be a percentage is dropped`() {
        assertNull(plausibleBatteryPercent(101))
        assertNull(plausibleBatteryPercent(-1))
        assertNull(plausibleBatteryPercent(0xffff))
    }

    @Test
    fun `a shutter count is accepted up to an order past any rated life`() {
        assertEquals(1, plausibleShutterCount(1))
        assertEquals(142_037, plausibleShutterCount(142_037))
        assertEquals(9_999_999, plausibleShutterCount(9_999_999))
    }

    /** Zero is a property meaning something else: nobody holds a camera that never fired. */
    @Test
    fun `zero and absurd shutter counts are dropped`() {
        assertNull(plausibleShutterCount(0))
        assertNull(plausibleShutterCount(-5))
        assertNull(plausibleShutterCount(10_000_000))
        assertNull(plausibleShutterCount(0xffffffffL))
    }

    @Test
    fun `a lens name is trimmed`() {
        assertEquals("XF23mmF2 R WR", plausibleLensName("  XF23mmF2 R WR  "))
    }

    @Test
    fun `a blank or unprintable lens name is dropped`() {
        assertNull(plausibleLensName(""))
        assertNull(plausibleLensName("   "))
        // Control bytes mean the payload was never really a PTP string.
        assertNull(plausibleLensName("XF23\u0001mm"))
    }

    @Test
    fun `a body that answered nothing is empty`() {
        assertTrue(CameraDetails().isEmpty)
        assertFalse(CameraDetails(firmware = "1.20").isEmpty)
    }

    /**
     * Firmware and serial come from `GetDeviceInfo`, which is ISO 15740 rather than a
     * reverse-engineered guess — so a card showing only those two must not carry the caveat.
     */
    @Test
    fun `only vendor-property fields raise the caveat`() {
        assertFalse(CameraDetails(firmware = "1.20", serialNumber = "A1B2").hasUnverifiedFields)
        assertTrue(CameraDetails(batteryPercent = 50).hasUnverifiedFields)
        assertTrue(CameraDetails(shutterCount = 10).hasUnverifiedFields)
        assertTrue(CameraDetails(lens = "XF35mmF1.4 R").hasUnverifiedFields)
    }
}
