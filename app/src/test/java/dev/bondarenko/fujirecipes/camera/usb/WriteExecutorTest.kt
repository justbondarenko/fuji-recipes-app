package dev.bondarenko.fujirecipes.camera.usb

import dev.bondarenko.fujirecipes.camera.FakeCamera
import dev.bondarenko.fujirecipes.camera.plan.PRESET_SLOT_PROPERTY
import dev.bondarenko.fujirecipes.camera.plan.buildWritePlan
import dev.bondarenko.fujirecipes.camera.ptp.PtpSession
import dev.bondarenko.fujirecipes.camera.ptp.PtpTransport
import dev.bondarenko.fujirecipes.camera.ptp.ResponseCode
import dev.bondarenko.fujirecipes.camera.ptp.packU16
import dev.bondarenko.fujirecipes.data.fields.SensorGeneration
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * FEAT-006 T-07, against [FakeCamera] — no hardware.
 *
 * Mirrors `fuji-recipes-book/tests/unit/camera-write.spec.ts` @ `0c17106`. Every case here is
 * a way a real write goes wrong: a body that refuses one property, a body that refuses the
 * slot selector, a cable pulled mid-write, a body that takes a value and will not read it
 * back.
 */
class WriteExecutorTest {

    private val settings: JsonObject = Json.parseToJsonElement(
        """
        {
          "filmSimulation": "classic-chrome",
          "dynamicRange": "dr400",
          "highlightTone": 1.5,
          "sharpness": -1,
          "clarity": 3,
          "grainEffect": "strong",
          "grainSize": "large",
          "whiteBalance": "daylight"
        }
        """.trimIndent(),
    ).jsonObject

    private val plan = buildWritePlan(
        slot = 3,
        generation = SensorGeneration.XTRANS_V,
        recipeName = "Kodachrome 64",
        settings = settings,
    )

    private fun connected(camera: FakeCamera): PtpSession =
        PtpSession(PtpTransport(camera)).apply { open() }

    /** No real sleeping in tests; the settle is asserted separately. */
    private val noSleep: (Long) -> Unit = {}

    // ─── The clean run ──────────────────────────────────────────────────────

    @Test
    fun `writes every step in the plan's order and verifies each`() {
        val camera = FakeCamera()
        val outcome = executeWritePlan(connected(camera), plan, sleep = noSleep)

        assertEquals(plan.total, outcome.written)
        assertEquals(plan.steps.map { it.property }, camera.writes.map { it.property })
        assertTrue(outcome.steps.all { it.status == StepStatus.WRITTEN })
        assertTrue(outcome.warnings.isEmpty())
        assertTrue(outcome.slotTouched)
    }

    @Test
    fun `the slot selector goes first, carrying the slot number`() {
        val camera = FakeCamera()
        executeWritePlan(connected(camera), plan, sleep = noSleep)

        assertEquals(PRESET_SLOT_PROPERTY, camera.writes.first().property)
        assertEquals(packU16(3).toList(), camera.writes.first().payload.toList())
    }

    /** Keyed on the step's id, so a change to the plan's order cannot silently drop it. */
    @Test
    fun `the body is given time to settle after the slot switch, once`() {
        val settles = mutableListOf<Long>()
        executeWritePlan(connected(FakeCamera()), plan, sleep = { settles += it })

        assertEquals(listOf(SLOT_SETTLE_MS), settles)
    }

    @Test
    fun `progress is reported once per step, in order, counting up to the total`() {
        val progress = mutableListOf<WriteProgress>()
        executeWritePlan(
            connected(FakeCamera()),
            plan,
            onProgress = { progress += it },
            sleep = noSleep,
        )

        assertEquals(plan.total, progress.size)
        assertEquals((1..plan.total).toList(), progress.map { it.done })
        assertTrue(progress.all { it.total == plan.total })
        assertEquals(plan.steps.map { it.label }, progress.map { it.label })
    }

    // ─── Refusals ───────────────────────────────────────────────────────────

    /** One refused property costs one setting; abandoning the recipe would cost sixteen. */
    @Test
    fun `a refused setting is recorded and the rest are still written`() {
        val camera = FakeCamera()
        camera.refuseProperty[0xd1a2] = ResponseCode.INVALID_DEVICE_PROP_VALUE

        val outcome = executeWritePlan(connected(camera), plan, sleep = noSleep)

        val clarity = outcome.steps.first { it.id == "clarity" }
        assertEquals(StepStatus.REFUSED, clarity.status)
        assertEquals(plan.total - 1, outcome.written)
        assertEquals(plan.total, outcome.steps.size)
    }

    /** P5: the name *and* the number, because a search for "0x200C" finds the answer. */
    @Test
    fun `a refusal names what the camera said and quotes its code`() {
        val camera = FakeCamera()
        camera.refuseProperty[0xd1a2] = ResponseCode.INVALID_DEVICE_PROP_VALUE

        val outcome = executeWritePlan(connected(camera), plan, sleep = noSleep)

        val detail = outcome.steps.first { it.id == "clarity" }.detail
        assertNotNull(detail)
        assertTrue(detail.contains("InvalidDevicePropValue"))
        assertTrue(detail.contains("0x200C"))
        assertTrue(outcome.warnings.any { it.contains("Clarity") })
    }

    /** A name written into the wrong slot is worse than no write at all. */
    @Test
    fun `a refused slot selector abandons the write before anything else is attempted`() {
        val camera = FakeCamera()
        camera.refuseProperty[PRESET_SLOT_PROPERTY] = ResponseCode.DEVICE_BUSY

        val interrupted = assertFailsWith<WriteInterrupted> {
            executeWritePlan(connected(camera), plan, sleep = noSleep)
        }

        assertEquals(InterruptReason.REFUSED, interrupted.reason)
        assertEquals(1, interrupted.outcome.steps.size)
        assertEquals(0, interrupted.outcome.written)
        assertTrue(interrupted.message!!.contains("Nothing further was written"))
    }

    /** Nothing landed, so there is nothing to warn about. */
    @Test
    fun `a write that never touched the slot raises no partial-write warning`() {
        val camera = FakeCamera()
        camera.refuseProperty[PRESET_SLOT_PROPERTY] = ResponseCode.DEVICE_BUSY

        val interrupted = assertFailsWith<WriteInterrupted> {
            executeWritePlan(connected(camera), plan, sleep = noSleep)
        }

        assertFalse(interrupted.outcome.slotTouched)
        assertNull(partialWriteWarning(interrupted.outcome))
    }

    // ─── The cable ──────────────────────────────────────────────────────────

    @Test
    fun `an unplug mid-write stops immediately and warns about the partial slot`() {
        val camera = FakeCamera()
        camera.unplugAfterWrites = 4

        val interrupted = assertFailsWith<WriteInterrupted> {
            executeWritePlan(connected(camera), plan, sleep = noSleep)
        }

        assertEquals(InterruptReason.DISCONNECTED, interrupted.reason)
        assertTrue(interrupted.outcome.slotTouched)

        val warning = partialWriteWarning(interrupted.outcome)
        assertNotNull(warning)
        assertTrue(warning.contains("C3"))
        assertTrue(warning.contains("partly written"))
        assertTrue(warning.contains("Writing it again"))
    }

    @Test
    fun `an unplug does not attempt the remaining steps`() {
        val camera = FakeCamera()
        camera.unplugAfterWrites = 3

        val interrupted = assertFailsWith<WriteInterrupted> {
            executeWritePlan(connected(camera), plan, sleep = noSleep)
        }

        assertTrue(interrupted.outcome.steps.size < plan.total)
        assertEquals(3, camera.writes.size)
    }

    // ─── Cancelling ─────────────────────────────────────────────────────────

    /** A SetDevicePropValue that has been sent cannot be unsent. */
    @Test
    fun `cancelling takes effect at a step boundary and reports how far it got`() {
        val camera = FakeCamera()
        var seen = 0

        val interrupted = assertFailsWith<WriteInterrupted> {
            executeWritePlan(
                connected(camera),
                plan,
                onProgress = { seen += 1 },
                isCancelled = { seen >= 3 },
                sleep = noSleep,
            )
        }

        assertEquals(InterruptReason.CANCELLED, interrupted.reason)
        assertEquals(3, interrupted.outcome.written)
        assertNotNull(partialWriteWarning(interrupted.outcome))
    }

    // ─── Verification ───────────────────────────────────────────────────────

    /**
     * A body that takes the value and will not return it has not failed. Some bodies refuse
     * every read in this property block, and calling that a failed write would be wrong.
     */
    @Test
    fun `a body that will not read a value back leaves it unverified, not failed`() {
        val camera = FakeCamera()
        camera.echoWrites = false

        val outcome = executeWritePlan(connected(camera), plan, sleep = noSleep)

        assertTrue(outcome.steps.all { it.status == StepStatus.UNVERIFIED })
        assertEquals(0, outcome.written)
        assertTrue(outcome.slotTouched)
    }

    @Test
    fun `a value the camera adjusted is reported as a mismatch and warned about`() {
        val camera = FakeCamera()
        camera.readBackAs[0xd1a2] = packU16(999)

        val outcome = executeWritePlan(connected(camera), plan, sleep = noSleep)

        val clarity = outcome.steps.first { it.id == "clarity" }
        assertEquals(StepStatus.MISMATCHED, clarity.status)
        // Hex, and greppable against the encoding tables: this is the one diagnostic the user
        // reads on a phone with a camera hanging off it.
        assertTrue(clarity.detail!!.contains("0x03E7"), "unreadable detail: ${clarity.detail}")
        assertTrue(clarity.detail!!.contains("0x001E"), "the sent value is not named")
        assertTrue(outcome.warnings.any { it.contains("read back changed") })
        // It did not stop the write: the value is already in the camera.
        assertEquals(plan.total, outcome.steps.size)
    }

    @Test
    fun `a short read is a mismatch rather than a pass`() {
        val camera = FakeCamera()
        camera.readBackAs[0xd1a2] = ByteArray(1)

        val outcome = executeWritePlan(connected(camera), plan, sleep = noSleep)

        assertEquals(
            StepStatus.MISMATCHED,
            outcome.steps.first { it.id == "clarity" }.status,
        )
    }

    // ─── The plan's own refusal ─────────────────────────────────────────────

    @Test
    fun `a refused plan has nothing to execute`() {
        val refused = buildWritePlan(
            slot = 3,
            generation = SensorGeneration.XTRANS_III,
            recipeName = "Kodachrome 64",
            settings = settings,
        )
        val camera = FakeCamera()

        val outcome = executeWritePlan(connected(camera), refused, sleep = noSleep)

        assertTrue(camera.writes.isEmpty())
        assertFalse(outcome.slotTouched)
    }
}
