package dev.bondarenko.fujirecipes.ui.camera

import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.camera.CameraModels
import dev.bondarenko.fujirecipes.camera.CameraState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * FEAT-005 T-15.
 *
 * Mirrors `fuji-recipes-book/tests/unit/camera-chip.spec.ts` @ `0c17106`.
 *
 * The rule under test is an accessibility one, not a styling one: **colour is never the only
 * signal.** A green pill and a grey pill are the same pill to a colour-blind reader and to
 * anyone outdoors, which is where this app is used.
 */
class CameraChipLookTest {

    private val states = listOf(
        CameraState.NoUsbHost,
        CameraState.Disconnected,
        CameraState.Connecting,
        CameraState.Connected(CameraModels.identify("X100VI")),
        CameraState.Writing(slot = 3, done = 7, total = 17, current = "Clarity"),
        CameraState.Error("Device busy", ptpCode = 0x2019),
    )

    @Test
    fun `every state has its own icon`() {
        val icons = states.map { cameraChipLook(it).icon }

        // Writing and Connected share the camera glyph deliberately — the progress bar is
        // Writing's own signal — so the distinct count is one less than the state count.
        assertEquals(
            states.size - 1,
            icons.toSet().size,
            "two states other than Connected/Writing share an icon",
        )
    }

    @Test
    fun `every state has its own words`() {
        val labels = states.map { cameraChipLook(it).modelLabel ?: cameraChipLook(it).labelRes }

        assertEquals(states.size, labels.toSet().size, "two states read the same")
    }

    @Test
    fun `every state has its own tone`() {
        // Connected and Writing share READY: the camera is working in both, and the bar is
        // what separates them.
        val tones = states.map { cameraChipLook(it).tone }

        assertEquals(states.size - 1, tones.toSet().size)
    }

    /** "X100VI" says more than "Connected", and it is how a misidentification is noticed. */
    @Test
    fun `connected shows the body's own name`() {
        val look = cameraChipLook(CameraState.Connected(CameraModels.identify("X-T5")))

        assertEquals("X-T5", look.modelLabel)
    }

    @Test
    fun `a body that reported no name falls back to a word rather than an empty pill`() {
        val look = cameraChipLook(CameraState.Connected(CameraModels.identify("")))

        assertNull(look.modelLabel)
        assertEquals(R.string.camera_chip_connected, look.labelRes)
    }

    @Test
    fun `only the two waiting states spin`() {
        assertTrue(cameraChipLook(CameraState.Connecting).spinning)
        assertFalse(cameraChipLook(CameraState.Disconnected).spinning)
        assertFalse(cameraChipLook(CameraState.Connected(CameraModels.identify("X-T5"))).spinning)
    }

    @Test
    fun `only a write shows progress, and it tracks the steps`() {
        assertNull(cameraChipLook(CameraState.Connecting).progress)

        val look = cameraChipLook(
            CameraState.Writing(slot = 1, done = 5, total = 10, current = "Sharpness"),
        )
        assertEquals(0.5f, look.progress)
    }

    /** A plan with no steps must not divide by zero on the way to the screen. */
    @Test
    fun `a write with no steps reports no progress rather than dividing by zero`() {
        val look = cameraChipLook(CameraState.Writing(slot = 1, done = 0, total = 0, current = ""))

        assertEquals(0f, look.progress)
    }

    /** Not an error, and the tone says so: this phone will never grow USB host support. */
    @Test
    fun `no USB host is neutral rather than alarming`() {
        val look = cameraChipLook(CameraState.NoUsbHost)

        assertEquals(CameraChipTone.NEUTRAL, look.tone)
        assertEquals(R.string.camera_chip_no_usb, look.labelRes)
    }
}
