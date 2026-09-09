package dev.bondarenko.fujirecipes.camera.plan

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CameraDetailsTest {

    // ─── Battery ────────────────────────────────────────────────────────────

    /**
     * The bug this type exists to prevent. An X-T50 reports 10 on a scale whose declared
     * maximum is 10 — a full battery — and the old code rendered that as "10%".
     */
    @Test
    fun `a full battery on a ten-point scale is not ten percent`() {
        val level = plausibleBatteryLevel(10, declaredMax = 10)!!

        assertEquals(10, level.value)
        assertEquals(10, level.max)
        assertTrue(level.maxDeclared)
        assertFalse(level.isPercentage)
    }

    @Test
    fun `a body that declares no scale gets the assumed one, and says so`() {
        val level = plausibleBatteryLevel(10)!!

        assertEquals(10, level.value)
        assertEquals(ASSUMED_BATTERY_MAX, level.max)
        assertFalse(level.maxDeclared)
    }

    /** A body that really does report percent is rendered as percent. */
    @Test
    fun `a hundred-point scale is a percentage`() {
        val level = plausibleBatteryLevel(87, declaredMax = 100)!!

        assertEquals(87, level.value)
        assertTrue(level.isPercentage)
    }

    @Test
    fun `a value above the body's own scale is dropped`() {
        assertNull(plausibleBatteryLevel(11, declaredMax = 10))
        assertNull(plausibleBatteryLevel(101, declaredMax = 100))
        // The assumed scale applies the same way.
        assertNull(plausibleBatteryLevel(11))
        assertNull(plausibleBatteryLevel(0xffff))
    }

    @Test
    fun `a negative level is dropped`() {
        assertNull(plausibleBatteryLevel(-1))
        assertNull(plausibleBatteryLevel(-1, declaredMax = 10))
    }

    @Test
    fun `a flat battery is a reading, not an absence`() {
        assertEquals(0, plausibleBatteryLevel(0, declaredMax = 10)?.value)
    }

    /** A declared maximum that is not a scale means the property is not a battery level. */
    @Test
    fun `an implausible declared scale is refused outright`() {
        assertNull(plausibleBatteryLevel(0, declaredMax = 0))
        assertNull(plausibleBatteryLevel(5, declaredMax = 65535))
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
        assertTrue(CameraDetails(battery = BatteryLevel(5, 10, maxDeclared = true)).hasUnverifiedFields)
        assertTrue(CameraDetails(shutterCount = 10).hasUnverifiedFields)
        assertTrue(CameraDetails(lens = "XF35mmF1.4 R").hasUnverifiedFields)
    }
}
