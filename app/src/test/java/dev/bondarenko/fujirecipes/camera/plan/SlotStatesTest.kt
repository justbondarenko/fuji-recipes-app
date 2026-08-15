package dev.bondarenko.fujirecipes.camera.plan

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * FEAT-006 T-16.
 *
 * Mirrors `fuji-recipes-book/tests/unit/slots.spec.ts` @ `0c17106`.
 *
 * The distinction under test is the one that costs a recipe when it is lost: **"the camera says
 * nothing is named here" and "the camera would not tell us" are different facts.** Collapsing
 * them shows an empty-looking slot for one the app knows nothing about, and somebody writes
 * over a recipe that was not stored anywhere else.
 */
class SlotStatesTest {

    @Test
    fun `there is always one entry per slot, C1 first`() {
        val states = slotStates(emptyList())

        assertEquals(LAST_SLOT - FIRST_SLOT + 1, states.size)
        assertEquals((FIRST_SLOT..LAST_SLOT).toList(), states.map { it.slot })
    }

    @Test
    fun `a named slot reports its name`() {
        val states = slotStates(listOf(SlotNameReading(3, "Kodachrome 64", read = true)))
        val third = states.first { it.slot == 3 }

        assertEquals(SlotStatus.NAMED, third.status)
        assertEquals("Kodachrome 64", third.name)
        assertTrue(third.occupied)
    }

    /** Answered, with nothing to identify. Not "empty" — the app cannot know that. */
    @Test
    fun `a slot the camera answered for with no name is unnamed, not unknown`() {
        val states = slotStates(listOf(SlotNameReading(2, null, read = true)))
        val second = states.first { it.slot == 2 }

        assertEquals(SlotStatus.UNNAMED, second.status)
        assertNull(second.name)
        assertFalse(second.occupied)
    }

    /** A blank string from the camera is the same fact as a null one. */
    @Test
    fun `a blank name is unnamed`() {
        val states = slotStates(listOf(SlotNameReading(2, "   ", read = true)))

        assertEquals(SlotStatus.UNNAMED, states.first { it.slot == 2 }.status)
    }

    @Test
    fun `a slot the camera refused is unreadable, not unnamed`() {
        val states = slotStates(listOf(SlotNameReading(4, null, read = false)))

        assertEquals(SlotStatus.UNREADABLE, states.first { it.slot == 4 }.status)
    }

    @Test
    fun `a slot with no reading at all is unknown`() {
        val states = slotStates(listOf(SlotNameReading(1, "Portra 2", read = true)))

        assertEquals(SlotStatus.UNKNOWN, states.first { it.slot == 7 }.status)
    }

    /** A name shown during a re-read is a name from before the re-read. */
    @Test
    fun `loading wins over a leftover reading`() {
        val states = slotStates(
            listOf(SlotNameReading(1, "Kodachrome 64", read = true)),
            loading = true,
        )

        assertTrue(states.all { it.status == SlotStatus.READING })
        assertTrue(states.all { it.name == null })
    }

    // ─── Which slots need a second tap ──────────────────────────────────────

    @Test
    fun `a named slot needs an acknowledgement, and it names what is being replaced`() {
        val state = slotStates(listOf(SlotNameReading(3, "Acros Night", read = true)))
            .first { it.slot == 3 }

        val caution = slotCaution(state)

        assertIs<SlotCaution.Named>(caution)
        assertEquals("Acros Night", caution.recipeName)
        assertEquals(3, caution.slot)
    }

    /**
     * The rule that keeps the confirmation worth reading: an untouched camera answers for all
     * seven slots with no name, and putting a dialog in front of every one of them trains
     * people to tap through the dialog that exists to protect them.
     */
    @Test
    fun `an unnamed slot needs no acknowledgement`() {
        val state = slotStates(listOf(SlotNameReading(2, null, read = true)))
            .first { it.slot == 2 }

        assertNull(slotCaution(state))
    }

    @Test
    fun `a slot the camera would not describe needs an acknowledgement of its own kind`() {
        val unreadable = slotStates(listOf(SlotNameReading(4, null, read = false)))
            .first { it.slot == 4 }
        val unknown = slotStates(emptyList()).first { it.slot == 7 }

        assertIs<SlotCaution.Unknown>(slotCaution(unreadable))
        assertIs<SlotCaution.Unknown>(slotCaution(unknown))
    }

    @Test
    fun `a slot still being read needs no acknowledgement, because it is not choosable yet`() {
        val reading = slotStates(emptyList(), loading = true).first()

        assertNull(slotCaution(reading))
    }

    @Test
    fun `no state at all cautions about nothing rather than crashing`() {
        assertNull(slotCaution(null))
    }
}
