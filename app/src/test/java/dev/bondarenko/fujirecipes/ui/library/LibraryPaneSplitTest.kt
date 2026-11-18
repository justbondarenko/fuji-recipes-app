package dev.bondarenko.fujirecipes.ui.library

import androidx.compose.runtime.mutableFloatStateOf
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryPaneSplitTest {
    @Test
    fun centerIsMeasuredAcrossTheWindowIncludingTheRail() {
        val fraction = mutableFloatStateOf(0.5f)
        assertEquals(340, LibraryPaneSplit(840, 760, 300, fraction).listWidth)
        assertEquals(420, LibraryPaneSplit(840, 840, 300, fraction).listWidth)
        assertEquals(290, LibraryPaneSplit(860, 720, 300, fraction).listWidth)
    }

    @Test
    fun draggingRespectsMinimumPaneWidths() {
        val split = LibraryPaneSplit(1000, 920, 300, mutableFloatStateOf(0.5f))
        split.resizeBy(150f)
        assertEquals(570, split.listWidth)
        split.resizeBy(500f)
        assertEquals(620, split.listWidth)
        split.resizeBy(-1000f)
        assertEquals(300, split.listWidth)
    }

    @Test
    fun temporaryWindowConstraintsDoNotOverwriteThePreferredSplit() {
        val fraction = mutableFloatStateOf(0.5f)
        val wide = LibraryPaneSplit(1200, 1120, 300, fraction)
        wide.resizeTo(0.7f)
        val narrow = LibraryPaneSplit(800, 720, 300, fraction)
        assertEquals(420, narrow.listWidth)
        assertEquals(760, wide.listWidth)
    }
}
