package dev.bondarenko.fujirecipes.data.library

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The repair rules — FEAT-001 T-09.
 *
 * These exist to survive a stored value this build did not write: an older format, a
 * hand-edited entry, a sort since removed from the selector. Each field falls back on its
 * own, because losing a remembered sort over an unrecognised tag is worse than either
 * problem alone.
 *
 * Ported from `parseStoredView` in `fuji-recipes-book/src/utils/library-view.ts`, and the
 * asymmetry in the last test is the interesting one.
 */
class StoredLibraryViewTest {

    @Test
    fun `nothing stored gives the documented defaults`() {
        val view = StoredLibraryView.repair(null, null, null, null)

        assertEquals(SortId.NAME, view.sort)
        assertEquals(0, view.filters.minRating)
        assertTrue(view.filters.isEmpty)
    }

    @Test
    fun `an unknown film simulation is dropped`() {
        val view = StoredLibraryView.repair(
            tags = null,
            minRating = null,
            simulations = listOf("classic-chrome", "velvia-ii-from-2030"),
            sort = null,
        )

        assertEquals(listOf("classic-chrome"), view.filters.simulations)
    }

    @Test
    fun `a rating outside the range is clamped rather than discarded`() {
        // The intent — "only the good ones" — is legible even when the number is not.
        assertEquals(5, StoredLibraryView.repair(null, 9, null, null).filters.minRating)
        assertEquals(0, StoredLibraryView.repair(null, -3, null, null).filters.minRating)
    }

    @Test
    fun `a sort this build no longer offers falls back to the default`() {
        // Keeping it would leave the control blank with no way to read the list's order.
        assertEquals(SortId.NAME, StoredLibraryView.repair(null, null, null, "written").sort)
        assertEquals(SortId.NAME, StoredLibraryView.repair(null, null, null, "manual").sort)
        assertEquals(SortId.RATING, StoredLibraryView.repair(null, null, null, "rating").sort)
    }

    @Test
    fun `an unrecognised tag is kept, unlike an unrecognised simulation`() {
        /**
         * The deliberate asymmetry. The library has not loaded when this runs, so a tag
         * that is momentarily absent — mid-import on the web client — is not a reason to
         * silently drop a filter. It narrows to the "no recipes match" panel, which says so
         * and offers the way out. A simulation id, by contrast, comes from a closed set
         * this build knows in full, so an unknown one can only ever be wrong.
         */
        val view = StoredLibraryView.repair(
            tags = listOf("a-tag-nothing-carries"),
            minRating = null,
            simulations = listOf("a-sim-that-does-not-exist"),
            sort = null,
        )

        assertEquals(listOf("a-tag-nothing-carries"), view.filters.tags)
        assertTrue(view.filters.simulations.isEmpty())
    }

    @Test
    fun `a broken field does not take the rest of the view with it`() {
        val view = StoredLibraryView.repair(
            tags = listOf("street"),
            minRating = 99,
            simulations = listOf("nope"),
            sort = "rating",
        )

        assertEquals(SortId.RATING, view.sort)
        assertEquals(listOf("street"), view.filters.tags)
        assertEquals(5, view.filters.minRating)
    }

    @Test
    fun `the filter badge counts axes, not values`() {
        val filters = LibraryFilters(
            tags = listOf("street", "warm", "night"),
            minRating = 4,
            maxRating = 5,
            simulations = emptyList(),
        )
        assertEquals(2, filters.activeCount)

        val maxRestrictedFilters = LibraryFilters(
            tags = emptyList(),
            minRating = 0,
            maxRating = 3,
            simulations = emptyList(),
        )
        assertEquals(1, maxRestrictedFilters.activeCount)

        val defaultFilters = LibraryFilters()
        assertEquals(0, defaultFilters.activeCount)
    }

    @Test
    fun `min and max rating are clamped and ordered`() {
        val view = StoredLibraryView.repair(
            tags = null,
            minRating = 6,
            maxRating = -1,
            simulations = null,
            sort = null,
            sortDirection = null,
        )
        assertEquals(0, view.filters.minRating)
        assertEquals(5, view.filters.maxRating)
    }

    @Test
    fun `sort direction is repaired and falls back to default when unknown`() {
        val ascView = StoredLibraryView.repair(null, null, null, null, "name", "asc")
        assertEquals(SortDirection.ASCENDING, ascView.sortDirection)

        val descView = StoredLibraryView.repair(null, null, null, null, "name", "desc")
        assertEquals(SortDirection.DESCENDING, descView.sortDirection)

        val fallbackNameView = StoredLibraryView.repair(null, null, null, null, "name", "invalid")
        assertEquals(SortDirection.ASCENDING, fallbackNameView.sortDirection)

        val fallbackRatingView = StoredLibraryView.repair(null, null, null, null, "rating", null)
        assertEquals(SortDirection.DESCENDING, fallbackRatingView.sortDirection)
    }

    @Test
    fun `blank and duplicate tags are cleaned up`() {
        val view = StoredLibraryView.repair(listOf("street", "street", "", "  "), null, null, null)
        assertEquals(listOf("street"), view.filters.tags)
    }
}
