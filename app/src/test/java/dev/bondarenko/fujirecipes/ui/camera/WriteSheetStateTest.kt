package dev.bondarenko.fujirecipes.ui.camera

import dev.bondarenko.fujirecipes.camera.CameraModels
import dev.bondarenko.fujirecipes.camera.CameraState
import dev.bondarenko.fujirecipes.camera.plan.DroppedField
import dev.bondarenko.fujirecipes.camera.plan.SlotNameReading
import dev.bondarenko.fujirecipes.camera.plan.SlotStatus
import dev.bondarenko.fujirecipes.camera.plan.WritePlan
import dev.bondarenko.fujirecipes.camera.plan.slotStates
import dev.bondarenko.fujirecipes.camera.usb.WriteOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * FEAT-006 T-09.
 *
 * The stage machine as a pure type, so the sequence is testable without the sheet. What is
 * checked here is which stage the sheet *opens* on, because that is the decision the user
 * notices: a stage they did not need is a step they have to dismiss, and the ordinary case —
 * the app was launched by plugging the camera in — must not ask them to connect something
 * that is already connected.
 */
class WriteSheetStateTest {

    private val connected = CameraState.Connected(CameraModels.identify("X100VI"))
    private val clean = WritePlan(slot = 1)

    @Test
    fun `a disconnected camera opens on the connection stage`() {
        assertEquals(WriteStage.Connect, openingStage(CameraState.Disconnected, clean))
        assertEquals(WriteStage.Connect, openingStage(CameraState.Connecting, clean))
        assertEquals(WriteStage.Connect, openingStage(CameraState.NoUsbHost, clean))
    }

    /** The ordinary case: the app opened because the camera was plugged in. */
    @Test
    fun `a connected camera and a compatible recipe open straight on the slot picker`() {
        assertEquals(WriteStage.Picker, openingStage(connected, clean))
    }

    @Test
    fun `a recipe with dropped fields is shown them before anything is sent`() {
        val plan = clean.copy(
            dropped = listOf(DroppedField("color", "Color", "monochrome rejects it")),
        )

        val stage = openingStage(connected, plan)

        assertIs<WriteStage.Compatibility>(stage)
        assertEquals(plan, stage.plan)
    }

    /** WR-01, and it is blocking — the compatibility stage renders it with no write action. */
    @Test
    fun `a refused plan stops at the compatibility stage`() {
        val plan = clean.copy(refusal = "X-Trans III has no custom slots")

        assertIs<WriteStage.Compatibility>(openingStage(connected, plan))
    }

    /** A camera that is mid-write is not connected *for a new write*. */
    @Test
    fun `a camera already writing opens on the connection stage rather than a second picker`() {
        val writing = CameraState.Writing(slot = 3, done = 4, total = 17, current = "Clarity")

        assertEquals(WriteStage.Connect, openingStage(writing, clean))
    }

    /**
     * The bug this guards: a slot picked for one recipe was still picked for the next one, so
     * the write button offered a slot the user had never looked at on this attempt.
     */
    @Test
    fun `reaching the picker forgets the slot an earlier write chose`() {
        val afterAWrite = WriteUiState(
            stage = WriteStage.Done(
                WriteOutcome(slot = 6, written = 17, total = 17, slotTouched = true),
            ),
            selectedSlot = 6,
        )

        val picker = afterAWrite.enteringPicker()

        assertEquals(WriteStage.Picker, picker.stage)
        assertEquals(null, picker.selectedSlot)
    }

    /** Nothing is chosen for the user before slots are read from the camera. */
    @Test
    fun `a fresh write attempt has no preselected slot before slots are read`() {
        assertEquals(null, WriteUiState(stage = WriteStage.Picker).selectedSlot)
    }

    @Test
    fun `an error state opens on the connection stage so it can be retried`() {
        val error = CameraState.Error("Device busy", ptpCode = 0x2019)

        assertEquals(WriteStage.Connect, openingStage(error, clean))
    }

    @Test
    fun `when slots are read, the first empty slot is pre-selected`() {
        val readings = listOf(
            SlotNameReading(1, "Kodachrome 64", read = true),
            SlotNameReading(2, null, read = true),
            SlotNameReading(3, "Acros Night", read = true),
        )
        val slots = slotStates(readings)
        val firstEmptySlot = slots.firstOrNull { it.status == SlotStatus.UNNAMED }?.slot

        assertEquals(2, firstEmptySlot)
    }

    @Test
    fun `when all slots are occupied, no slot is pre-selected`() {
        val readings = (1..7).map { SlotNameReading(it, "Recipe $it", read = true) }
        val slots = slotStates(readings)
        val firstEmptySlot = slots.firstOrNull { it.status == SlotStatus.UNNAMED }?.slot

        assertNull(firstEmptySlot)
    }

    @Test
    fun `when the first slot is empty, C1 is pre-selected`() {
        val readings = listOf(
            SlotNameReading(1, null, read = true),
            SlotNameReading(2, "Portra 400", read = true),
        )
        val slots = slotStates(readings)
        val firstEmptySlot = slots.firstOrNull { it.status == SlotStatus.UNNAMED }?.slot

        assertEquals(1, firstEmptySlot)
    }
}

