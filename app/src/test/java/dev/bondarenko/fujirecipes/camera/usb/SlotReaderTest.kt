package dev.bondarenko.fujirecipes.camera.usb

import dev.bondarenko.fujirecipes.camera.FakeCamera
import dev.bondarenko.fujirecipes.camera.plan.PRESET_SLOT_PROPERTY
import dev.bondarenko.fujirecipes.camera.ptp.PtpSession
import dev.bondarenko.fujirecipes.camera.ptp.PtpTimeoutError
import dev.bondarenko.fujirecipes.camera.ptp.PtpTransport
import dev.bondarenko.fujirecipes.camera.ptp.unpackU16
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * FEAT-006 T-17, against [FakeCamera] — no hardware.
 *
 * What the slot picker shows comes from here, so what is tested is that the reader never turns
 * a failure into a fact: a slot the camera refused comes back marked unread, not as a slot
 * with no name. The picker renders those two differently on purpose, and it can only do that
 * if this layer keeps them apart.
 */
class SlotReaderTest {

    private val noSleep: (Long) -> Unit = {}

    private fun connected(camera: FakeCamera): PtpSession =
        PtpSession(PtpTransport(camera)).apply { open() }

    private fun cameraWith(vararg names: Pair<Int, String?>): FakeCamera =
        FakeCamera().apply {
            // A body answers for every slot; the ones without a name answer with a blank.
            (1..7).forEach { slotNames[it] = null }
            names.forEach { (slot, name) -> slotNames[slot] = name }
        }

    @Test
    fun `reads a name for every slot, C1 first`() {
        val camera = cameraWith(1 to "Kodachrome 64", 3 to "Acros Night", 6 to "Portra 2")

        val readings = readSlotNames(connected(camera), sleep = noSleep)

        assertEquals((1..7).toList(), readings.map { it.slot })
        assertEquals("Kodachrome 64", readings[0].name)
        assertEquals("Acros Night", readings[2].name)
        assertEquals("Portra 2", readings[5].name)
        assertTrue(readings.all { it.read })
    }

    @Test
    fun `a blank name comes back as no name rather than an empty string`() {
        val readings = readSlotNames(connected(cameraWith(1 to null)), sleep = noSleep)

        assertNull(readings[0].name)
        assertTrue(readings[0].read, "the camera answered, so the slot was read")
    }

    /** The distinction the picker is built on. */
    @Test
    fun `a slot the camera refuses is marked unread rather than unnamed`() {
        val camera = cameraWith(1 to "Kodachrome 64", 2 to "Acros Night")
        camera.refuseNameForSlots += 2

        val readings = readSlotNames(connected(camera), sleep = noSleep)

        assertEquals("Kodachrome 64", readings[0].name)
        assertTrue(readings[0].read)

        assertFalse(readings[1].read)
        assertNull(readings[1].name)
    }

    @Test
    fun `one refused slot does not cost the others`() {
        val camera = cameraWith(1 to "A", 2 to "B", 3 to "C")
        camera.refuseNameForSlots += 2

        val readings = readSlotNames(connected(camera), sleep = noSleep)

        assertEquals(6, readings.count { it.read })
        assertEquals("C", readings[2].name)
    }

    /** Every reply after a broken pipe would be answering an earlier question. */
    @Test
    fun `a failure of the pipe itself propagates rather than reporting seven empty slots`() {
        val camera = cameraWith(1 to "Kodachrome 64")
        camera.unplugAfterWrites = 2

        assertFailsWith<PtpTimeoutError> { readSlotNames(connected(camera), sleep = noSleep) }
    }

    /**
     * The settle is not optional: the body switches its internal name register when the
     * selector changes, and reading too early returns the *previous* slot's name — which is
     * worse than no name, because it is a wrong fact the picker would show as true.
     */
    @Test
    fun `each slot is selected before its name is read, with a settle in between`() {
        val camera = cameraWith(1 to "A")
        val settles = mutableListOf<Long>()

        readSlotNames(connected(camera), sleep = { settles += it })

        val selected = camera.writes
            .filter { it.property == PRESET_SLOT_PROPERTY }
            .map { unpackU16(it.payload) }

        assertEquals((1..7).toList(), selected)
        assertEquals(7, settles.size)
        assertTrue(settles.all { it == SLOT_SETTLE_MS })
    }

    /** Reading must not change anything: only the selector is written. */
    @Test
    fun `the read leaves no setting written into any slot`() {
        val camera = cameraWith(1 to "A")

        readSlotNames(connected(camera), sleep = noSleep)

        assertTrue(camera.writes.all { it.property == PRESET_SLOT_PROPERTY })
    }
}
