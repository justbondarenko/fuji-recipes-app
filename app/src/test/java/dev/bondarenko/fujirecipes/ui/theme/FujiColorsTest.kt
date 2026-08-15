package dev.bondarenko.fujirecipes.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * The palette against `specs/steering/design-system.md` §2.
 *
 * A hex digit typed wrong in a colour scheme is invisible — the app still builds, still
 * runs, and looks very slightly not like the web client, which nobody notices until the two
 * are side by side. These are the roles that carry the parity: the monochrome primary that
 * makes this theme *noir* rather than branded, the stone surfaces, and the amber accent
 * that is the only saturated colour in the interface.
 */
class FujiColorsTest {

    @Test
    fun `light primary is stone-950, not a brand colour`() {
        assertEquals(Color(0xFF0C0A09), FujiLightColors.primary)
        assertEquals(Color(0xFFFFFFFF), FujiLightColors.onPrimary)
    }

    @Test
    fun `dark primary inverts to stone-50`() {
        assertEquals(Color(0xFFFAFAF9), FujiDarkColors.primary)
        assertEquals(Color(0xFF0C0A09), FujiDarkColors.onPrimary)
    }

    @Test
    fun `surface matches the web client's html background`() {
        // stone-200 light, stone-900 dark — `bg-stone-200 dark:bg-surface-900`.
        assertEquals(Color(0xFFE7E5E4), FujiLightColors.surface)
        assertEquals(Color(0xFF1C1917), FujiDarkColors.surface)
    }

    @Test
    fun `cards sit one step off the page in both schemes`() {
        // The card colour is the one surface role the list depends on, and it is *lighter*
        // than the page in light and *lighter* than the page in dark — not symmetrical,
        // which is why it is asserted rather than assumed.
        assertEquals(Color(0xFFFAFAF9), FujiLightColors.surfaceContainerLow)
        assertEquals(Color(0xFF292524), FujiDarkColors.surfaceContainer)
        assertNotEquals(FujiLightColors.surface, FujiLightColors.surfaceContainerLow)
        assertNotEquals(FujiDarkColors.surface, FujiDarkColors.surfaceContainer)
    }

    @Test
    fun `amber is the accent, carried on tertiary`() {
        assertEquals(Color(0xFFD97706), FujiLightColors.tertiary)
        assertEquals(Color(0xFFFBBF24), FujiDarkColors.tertiary)
    }

    @Test
    fun `the palette is monochrome except for the accent and errors`() {
        // A stone value has equal-ish channels; the point of `noir` is that primary and
        // secondary carry no hue at all. This catches someone "warming up" the palette
        // back toward PRD §5.2's superseded Cappuccino without amending the design system.
        listOf(
            FujiLightColors.primary, FujiLightColors.secondary,
            FujiDarkColors.primary, FujiDarkColors.secondary,
        ).forEach { color ->
            val channels = listOf(color.red, color.green, color.blue)
            val spread = channels.max() - channels.min()
            assert(spread < 0.06f) { "$color carries a hue; the primary ramp is stone" }
        }
    }
}
