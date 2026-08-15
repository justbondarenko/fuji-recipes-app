package dev.bondarenko.fujirecipes.ui.camera

import dev.bondarenko.fujirecipes.camera.CameraModels
import dev.bondarenko.fujirecipes.camera.CameraState
import dev.bondarenko.fujirecipes.camera.plan.DroppedField
import dev.bondarenko.fujirecipes.camera.plan.WritePlan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

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

    @Test
    fun `an error state opens on the connection stage so it can be retried`() {
        val error = CameraState.Error("Device busy", ptpCode = 0x2019)

        assertEquals(WriteStage.Connect, openingStage(error, clean))
    }
}
