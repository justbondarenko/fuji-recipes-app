package dev.bondarenko.fujirecipes.camera.usb

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class UsbInterfaceSelectionTest {
    @Test
    fun `claiming an interface activates its setting before handing it to PTP`() {
        val events = mutableListOf<String>()
        val selected = claimAndActivateUsbInterface(
            candidates = listOf("PTP alternate 1", "fallback"),
            claim = { events += "claim $it"; true },
            activate = { events += "activate $it"; true },
            release = { events += "release $it" },
        )
        assertEquals("PTP alternate 1", selected)
        assertEquals(listOf("claim PTP alternate 1", "activate PTP alternate 1"), events)
    }

    @Test
    fun `an alternate setting that cannot be activated is released before trying the next interface`() {
        val events = mutableListOf<String>()
        val selected = claimAndActivateUsbInterface(
            candidates = listOf(1, 2, 3),
            claim = { events += "claim $it"; it != 1 },
            activate = { events += "activate $it"; it == 3 },
            release = { events += "release $it" },
        )
        assertEquals(3, selected)
        assertEquals(listOf("claim 1", "claim 2", "activate 2", "release 2", "claim 3", "activate 3"), events)
    }

    @Test
    fun `no usable interface is returned when activation fails for all candidates`() {
        val released = mutableListOf<Int>()
        val selected = claimAndActivateUsbInterface(
            candidates = listOf(1, 2), claim = { true }, activate = { false }, release = { released += it },
        )
        assertNull(selected)
        assertEquals(listOf(1, 2), released)
    }

    @Test
    fun `activation exceptions also release the interface`() {
        val released = mutableListOf<Int>()
        assertFailsWith<SecurityException> {
            claimAndActivateUsbInterface(
                candidates = listOf(1, 2), claim = { true },
                activate = { throw SecurityException("USB access revoked") },
                release = { released += it },
            )
        }
        assertEquals(listOf(1), released)
    }
}
