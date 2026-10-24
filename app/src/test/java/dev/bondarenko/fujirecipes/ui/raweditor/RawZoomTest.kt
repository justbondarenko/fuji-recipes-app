package dev.bondarenko.fujirecipes.ui.raweditor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.*
import org.junit.Test

class RawZoomTest {
    @Test fun zoomAnchorsAtFingerAndBoundsTheVisibleSensorRegion() {
        for (image in listOf(IntSize(7728, 5152), IntSize(5152, 7728))) {
            val view = IntSize(1000, 800)
            val fit = RawZoom(0f).clamped(image, view)
            val finger = Offset(400f, 350f)
            val full = fit.transform(image, view, finger, Offset.Zero, 100f)
            assertEquals(1f, full.scale, 0f)
            val before = (finger - fit.origin(image, view)) / fit.scale
            val after = finger - full.origin(image, view)
            assertEquals(before.x, after.x, 2f)
            assertEquals(before.y, after.y, 2f)
            for (pan in listOf(Offset.Zero, Offset(100000f, -100000f), Offset(-100000f, 100000f))) {
                val edge = full.copy(offset = pan).clamped(image, view)
                val tile = edge.viewport(image, view)
                assertTrue(tile.left >= 0 && tile.top >= 0)
                assertTrue(tile.left + tile.width <= image.width && tile.top + tile.height <= image.height)
                assertEquals(view.width, tile.width)
                assertEquals(view.height, tile.height)
            }
            val back = full.transform(image, view, finger, Offset.Zero, .0001f)
            assertEquals(fit.scale, back.scale, 0f)
            assertEquals(0f, back.offset.x, .001f)
            assertEquals(0f, back.offset.y, .001f)
        }
        assertEquals(1f, RawZoom(0f).clamped(IntSize(40, 30), IntSize(1000, 800)).scale, 0f)
        assertThrows(IllegalArgumentException::class.java) { RawViewport(0, 0, 100, 100, Float.NaN) }
        assertThrows(IllegalArgumentException::class.java) { RawViewport(0, 0, 5000, 100, 1f) }
    }
}
