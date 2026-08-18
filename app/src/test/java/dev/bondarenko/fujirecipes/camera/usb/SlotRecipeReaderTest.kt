package dev.bondarenko.fujirecipes.camera.usb

import dev.bondarenko.fujirecipes.camera.FakeCamera
import dev.bondarenko.fujirecipes.camera.plan.PRESET_SLOT_PROPERTY
import dev.bondarenko.fujirecipes.camera.ptp.PtpSession
import dev.bondarenko.fujirecipes.camera.ptp.PtpTimeoutError
import dev.bondarenko.fujirecipes.camera.ptp.PtpTransport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * FEAT-007 T-05, against [FakeCamera] — no hardware.
 *
 * The fixture is the reading the sibling repo took off a real X-T50's C6 on 2026-08-09, which
 * the owner had named "1970s Summer" and confirmed against the camera's own menu. So these
 * are not round-trips against this app's own assumptions: the raw values are what a camera
 * actually held, and the decoded recipe is what its menu actually showed.
 */
class SlotRecipeReaderTest {

    private val noSleep: (Long) -> Unit = {}

    private fun connected(camera: FakeCamera): PtpSession =
        PtpSession(PtpTransport(camera)).apply { open() }

    /** The X-T50's C6, property by property. */
    private val summer70s = mutableMapOf(
        0xd190 to 400, // dynamic range
        0xd192 to 19, // film simulation — Nostalgic Negative
        0xd195 to 5, // grain — strong / large
        0xd196 to 3, // colour chrome effect — strong
        0xd197 to 3, // colour chrome FX blue — strong
        0xd199 to 0x8007, // white balance — colour temperature
        0xd19c to 6500, // colour temperature
        0xd19d to 0xffec, // highlight tone — −20 as sent, i.e. −2.0
    )

    private fun cameraHolding(vararg slots: Pair<Int, MutableMap<Int, Int>>): FakeCamera =
        FakeCamera().apply {
            (1..7).forEach { slotNames[it] = null }
            slots.forEach { (slot, registers) -> slotSettings[slot] = registers }
            echoWrites = false
        }

    @Test
    fun `a configured slot decodes into the recipe the camera's menu shows`() {
        val camera = cameraHolding(6 to summer70s)
        camera.slotNames[6] = "1970s Summer"

        val recipes = readSlotRecipes(connected(camera), sleep = noSleep)

        assertEquals(1, recipes.size)
        val recipe = recipes.single()
        assertEquals(6, recipe.slot)
        assertEquals("1970s Summer", recipe.name)
        assertEquals("nostalgic-negative", recipe.settings["filmSimulation"])
        assertEquals("dr400", recipe.settings["dynamicRange"])
        assertEquals("strong", recipe.settings["grainEffect"])
        assertEquals("large", recipe.settings["grainSize"])
        assertEquals("color-temp", recipe.settings["whiteBalance"])
        assertEquals(6500, recipe.settings["colorTemperature"])
        assertEquals(-2.0, recipe.settings["highlightTone"])
    }

    /** An unconfigured slot is not an empty recipe. */
    @Test
    fun `a slot that decodes to nothing is dropped rather than imported empty`() {
        val camera = cameraHolding(1 to summer70s, 2 to mutableMapOf())

        val recipes = readSlotRecipes(connected(camera), sleep = noSleep)

        assertEquals(listOf(1), recipes.map { it.slot })
    }

    @Test
    fun `a camera with nothing saved yields nothing`() {
        val camera = cameraHolding()

        assertTrue(readSlotRecipes(connected(camera), sleep = noSleep).isEmpty())
    }

    @Test
    fun `a slot with settings but no name is labelled by its slot number`() {
        val camera = cameraHolding(2 to summer70s)

        assertEquals("Slot C2", readSlotRecipes(connected(camera), sleep = noSleep).single().name)
    }

    /**
     * `grainEffect` and `grainSize` are both `0xD195`. Reading per field would ask the camera
     * for the same property twice per slot — fourteen round trips instead of seven, over a
     * link that is already slow.
     *
     * Once **per slot**, not once overall: the registers change with the selector, so the
     * cache cannot outlive the slot it was filled for.
     */
    @Test
    fun `a property carrying two fields is read once per slot and yields both`() {
        val camera = cameraHolding(1 to summer70s)

        val recipe = readSlotRecipes(connected(camera), sleep = noSleep).single()

        assertEquals("strong", recipe.settings["grainEffect"])
        assertEquals("large", recipe.settings["grainSize"])
        assertEquals(7, camera.reads.count { it == 0xd195 }, "read more than once per slot")
    }

    /**
     * The refusal has to be cached too, or every field sharing a refused property asks for it
     * again. `getOrPut` re-invokes on a null value, which is exactly how that regresses.
     */
    @Test
    fun `a refused property is asked for once per slot, not once per field sharing it`() {
        val camera = cameraHolding(1 to summer70s)
        camera.refuseProperties += 0xd195

        readSlotRecipes(connected(camera), sleep = noSleep)

        assertEquals(7, camera.reads.count { it == 0xd195 })
    }

    /** One unknown value costs one field, not the slot. */
    @Test
    fun `a code this build cannot name is omitted and the rest of the slot survives`() {
        val camera = cameraHolding(1 to summer70s.toMutableMap().apply { this[0xd192] = 0x99 })

        val recipe = readSlotRecipes(connected(camera), sleep = noSleep).single()

        assertNull(recipe.settings["filmSimulation"])
        assertEquals("dr400", recipe.settings["dynamicRange"])
    }

    @Test
    fun `a property the body will not report costs that field only`() {
        val camera = cameraHolding(1 to summer70s)
        camera.refuseProperties += 0xd19c

        val recipe = readSlotRecipes(connected(camera), sleep = noSleep).single()

        assertNull(recipe.settings["colorTemperature"])
        assertEquals("color-temp", recipe.settings["whiteBalance"])
    }

    @Test
    fun `a slot the body refuses outright does not cost the others`() {
        val camera = cameraHolding(1 to summer70s, 3 to summer70s.toMutableMap())
        camera.refuseNameForSlots += 3

        // Refusing the name alone does not lose the slot — only the name does.
        val recipes = readSlotRecipes(connected(camera), sleep = noSleep)

        assertEquals(listOf(1, 3), recipes.map { it.slot })
        assertEquals("Slot C3", recipes[1].name)
    }

    @Test
    fun `progress is reported once per slot, counting to seven`() {
        val seen = mutableListOf<Pair<Int, Int>>()

        readSlotRecipes(
            connected(cameraHolding(1 to summer70s)),
            onProgress = { current, total -> seen += current to total },
            sleep = noSleep,
        )

        assertEquals((1..7).map { it to 7 }, seen)
    }

    @Test
    fun `each slot is selected before it is read, with a settle in between`() {
        val camera = cameraHolding(1 to summer70s)
        val settles = mutableListOf<Long>()

        readSlotRecipes(connected(camera), sleep = { settles += it })

        assertEquals(
            (1..7).toList(),
            camera.writes.filter { it.property == PRESET_SLOT_PROPERTY }
                .map { (it.payload[0].toInt() and 0xff) },
        )
        assertEquals(7, settles.size)
    }

    /** Reading must change nothing: only the selector is ever written. */
    @Test
    fun `reading writes no setting into any slot`() {
        val camera = cameraHolding(1 to summer70s)

        readSlotRecipes(connected(camera), sleep = noSleep)

        assertTrue(camera.writes.all { it.property == PRESET_SLOT_PROPERTY })
    }

    /** Every later reply would be answering an earlier question. */
    @Test
    fun `a failure of the pipe itself propagates`() {
        val camera = cameraHolding(1 to summer70s)
        camera.unplugAfterWrites = 2

        assertFailsWith<PtpTimeoutError> { readSlotRecipes(connected(camera), sleep = noSleep) }
    }

    /** P2: the server assigns ids, so nothing read off a camera carries one. */
    @Test
    fun `a slot recipe carries no id`() {
        val recipe = readSlotRecipes(connected(cameraHolding(1 to summer70s)), sleep = noSleep)
            .single()

        assertFalse(recipe.settings.containsKey("id"))
    }

    @Test
    fun `readSlotRecipe decodes a single requested slot`() {
        val camera = cameraHolding(3 to summer70s)
        camera.slotNames[3] = "Kodachrome"

        val recipe = readSlotRecipe(connected(camera), slot = 3, sleep = noSleep)

        assertNotNull(recipe)
        assertEquals(3, recipe.slot)
        assertEquals("Kodachrome", recipe.name)
        assertEquals("nostalgic-negative", recipe.settings["filmSimulation"])
    }

    @Test
    fun `readSlotRecipe returns null for unconfigured slot`() {
        val camera = cameraHolding()

        val recipe = readSlotRecipe(connected(camera), slot = 4, sleep = noSleep)

        assertNull(recipe)
    }
}
