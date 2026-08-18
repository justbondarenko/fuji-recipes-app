package dev.bondarenko.fujirecipes.data.library

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The selection pipeline against the web client — FEAT-001 T-15.
 *
 * One test per scenario in the Search, Filters and Sorting sections of
 * `03-behavior.feature`. This is the suite that makes "parity with
 * `fuji-recipes-book/src/utils/library-view.ts`" a checked claim rather than an asserted
 * one, which is the whole reason the pipeline is pure (`coding-standards.md` P7).
 */
class LibraryViewTest {

    private data class Fixture(
        override val name: String,
        override val rating: Int = 0,
        override val tags: List<String> = emptyList(),
        override val sortKey: Double = 0.0,
        override val createdAt: String = "2026-01-01T00:00:00.000Z",
        override val updatedAt: String = "2026-01-01T00:00:00.000Z",
        override val filmSimulationId: String? = "provia",
    ) : ViewableRecipe

    private fun select(recipes: List<Fixture>, view: LibraryView) = selectRecipes(recipes, view)
        .map { it.name }

    // --- Search ---

    @Test
    fun `search narrows the list`() {
        val recipes = listOf(Fixture("Kodachrome 64"), Fixture("Acros Night"), Fixture("Portra Warm"))
        assertEquals(listOf("Acros Night"), select(recipes, LibraryView(search = "acro")))
    }

    @Test
    fun `search matches tags as well as names`() {
        val recipes = listOf(Fixture("Portra Warm", tags = listOf("street")), Fixture("Acros Night"))
        assertEquals(listOf("Portra Warm"), select(recipes, LibraryView(search = "street")))
    }

    @Test
    fun `every search term must match somewhere`() {
        val recipes = listOf(
            Fixture("Acros Night", tags = listOf("street")),
            Fixture("Acros Day", tags = listOf("landscape")),
        )
        // A term may land on the name or on any one tag — that is what makes this useful.
        assertEquals(listOf("Acros Night"), select(recipes, LibraryView(search = "acros street")))
    }

    @Test
    fun `search ignores case`() {
        assertTrue(matchesSearch(Fixture("Kodachrome 64"), "KODACHROME"))
    }

    @Test
    fun `an empty search matches everything`() {
        val recipes = List(5) { Fixture("Recipe $it") }
        assertEquals(5, select(recipes, LibraryView(search = "   ")).size)
    }

    @Test
    fun `search is substring, not prefix`() {
        // Recipes get named "Kodachrome 64" and the memorable part is not the first word.
        assertTrue(matchesSearch(Fixture("Kodachrome 64"), "chrome"))
    }

    // --- Filters ---

    @Test
    fun `a minimum rating excludes lower-rated recipes`() {
        val recipes = listOf(Fixture("Five", 5), Fixture("Three", 3), Fixture("Unrated", 0))
        assertEquals(
            listOf("Five"),
            select(recipes, LibraryView(filters = LibraryFilters(minRating = 4))),
        )
    }

    @Test
    fun `a minimum rating of zero includes unrated recipes`() {
        val recipes = listOf(Fixture("Five", 5), Fixture("Three", 3), Fixture("Unrated", 0))
        assertEquals(3, select(recipes, LibraryView(filters = LibraryFilters(minRating = 0))).size)
    }

    @Test
    fun `a rating range excludes recipes outside the min and max bounds`() {
        val recipes = listOf(
            Fixture("Five", 5),
            Fixture("Four", 4),
            Fixture("Three", 3),
            Fixture("Two", 2),
            Fixture("Unrated", 0),
        )
        val view = LibraryView(filters = LibraryFilters(minRating = 2, maxRating = 4))
        assertEquals(listOf("Four", "Three", "Two"), select(recipes, view))
    }

    @Test
    fun `a rating range from 0 to 3 includes unrated recipes and up to 3 stars`() {
        val recipes = listOf(
            Fixture("Five", 5),
            Fixture("Four", 4),
            Fixture("Three", 3),
            Fixture("Unrated", 0),
        )
        val view = LibraryView(filters = LibraryFilters(minRating = 0, maxRating = 3))
        assertEquals(listOf("Three", "Unrated"), select(recipes, view))
    }

    @Test
    fun `an exact rating range matches only recipes with that exact rating`() {
        val recipes = listOf(Fixture("Five", 5), Fixture("Four", 4), Fixture("Three", 3))
        val view = LibraryView(filters = LibraryFilters(minRating = 4, maxRating = 4))
        assertEquals(listOf("Four"), select(recipes, view))
    }

    @Test
    fun `several tags are required together`() {
        val both = Fixture("Both", tags = listOf("street", "warm"))
        val one = Fixture("One", tags = listOf("street"))
        assertTrue(matchesFilters(both, LibraryFilters(tags = listOf("street", "warm"))))
        assertFalse(matchesFilters(one, LibraryFilters(tags = listOf("street", "warm"))))
    }

    @Test
    fun `several simulations are any-of`() {
        val recipes = listOf(
            Fixture("Chrome", filmSimulationId = "classic-chrome"),
            Fixture("Acros", filmSimulationId = "acros"),
            Fixture("Velvia", filmSimulationId = "velvia"),
        )
        val view = LibraryView(
            filters = LibraryFilters(simulations = listOf("classic-chrome", "velvia")),
        )
        assertEquals(listOf("Chrome", "Velvia"), select(recipes, view).sorted())
    }

    @Test
    fun `search and filters combine`() {
        val recipes = listOf(Fixture("Acros Night", 5), Fixture("Acros Day", 2))
        val view = LibraryView(search = "acros", filters = LibraryFilters(minRating = 4))
        assertEquals(listOf("Acros Night"), select(recipes, view))
    }

    @Test
    fun `a recipe with no film simulation is excluded by a simulation filter`() {
        val recipe = Fixture("Bare", filmSimulationId = null)
        assertFalse(matchesFilters(recipe, LibraryFilters(simulations = listOf("provia"))))
    }

    // --- Sorting ---

    @Test
    fun `the default sort is by name ascending`() {
        assertEquals(SortId.NAME, SortId.Default)
        assertEquals(SortDirection.ASCENDING, SortDirection.Default)
        val recipes = listOf(Fixture("Zulu", sortKey = 1.0), Fixture("Alpha", sortKey = 2.0))
        assertEquals(listOf("Alpha", "Zulu"), select(recipes, LibraryView()))
    }

    @Test
    fun `name sort ascending orders A-Z`() {
        val recipes = listOf(Fixture("Zulu"), Fixture("Alpha"), Fixture("Bravo"))
        assertEquals(
            listOf("Alpha", "Bravo", "Zulu"),
            select(recipes, LibraryView(sort = SortId.NAME, sortDirection = SortDirection.ASCENDING)),
        )
    }

    @Test
    fun `name sort descending orders Z-A`() {
        val recipes = listOf(Fixture("Zulu"), Fixture("Alpha"), Fixture("Bravo"))
        assertEquals(
            listOf("Zulu", "Bravo", "Alpha"),
            select(recipes, LibraryView(sort = SortId.NAME, sortDirection = SortDirection.DESCENDING)),
        )
    }

    @Test
    fun `name sort orders numbers the way people read them`() {
        val recipes = listOf(Fixture("Portra 10"), Fixture("Portra 2"))
        assertEquals(listOf("Portra 2", "Portra 10"), select(recipes, LibraryView()))
    }

    @Test
    fun `name sort ignores case and accents`() {
        assertEquals(0, compareNamesNaturally("acros", "ACROS"))
        assertEquals(0, compareNamesNaturally("resume", "résumé"))
    }

    @Test
    fun `leading zeros do not change a number's value`() {
        assertEquals(0, compareNamesNaturally("Roll 007", "Roll 7"))
        assertTrue(compareNamesNaturally("Roll 007", "Roll 8") < 0)
    }

    @Test
    fun `rating sort descending puts the highest first`() {
        val recipes = listOf(Fixture("Two", 2), Fixture("Five", 5), Fixture("Three", 3))
        assertEquals(
            listOf("Five", "Three", "Two"),
            select(recipes, LibraryView(sort = SortId.RATING, sortDirection = SortDirection.DESCENDING)),
        )
    }

    @Test
    fun `rating sort ascending puts the lowest first`() {
        val recipes = listOf(Fixture("Two", 2), Fixture("Five", 5), Fixture("Three", 3))
        assertEquals(
            listOf("Two", "Three", "Five"),
            select(recipes, LibraryView(sort = SortId.RATING, sortDirection = SortDirection.ASCENDING)),
        )
    }

    @Test
    fun `updated sort descending puts the most recent first`() {
        val recipes = listOf(
            Fixture("Old", updatedAt = "2026-01-01T00:00:00.000Z"),
            Fixture("New", updatedAt = "2026-08-01T00:00:00.000Z"),
        )
        assertEquals(
            listOf("New", "Old"),
            select(recipes, LibraryView(sort = SortId.UPDATED, sortDirection = SortDirection.DESCENDING)),
        )
    }

    @Test
    fun `updated sort ascending puts the oldest first`() {
        val recipes = listOf(
            Fixture("Old", updatedAt = "2026-01-01T00:00:00.000Z"),
            Fixture("New", updatedAt = "2026-08-01T00:00:00.000Z"),
        )
        assertEquals(
            listOf("Old", "New"),
            select(recipes, LibraryView(sort = SortId.UPDATED, sortDirection = SortDirection.ASCENDING)),
        )
    }

    @Test
    fun `equal sort values fall back to manual order in both directions`() {
        // Both rated 4; the one the user placed first stays first.
        val recipes = listOf(
            Fixture("Second", 4, sortKey = 2000.0),
            Fixture("First", 4, sortKey = 1000.0),
        )
        assertEquals(
            listOf("First", "Second"),
            select(recipes, LibraryView(sort = SortId.RATING, sortDirection = SortDirection.DESCENDING)),
        )
        assertEquals(
            listOf("First", "Second"),
            select(recipes, LibraryView(sort = SortId.RATING, sortDirection = SortDirection.ASCENDING)),
        )
    }

    @Test
    fun `equal sort keys fall back to creation time`() {
        val recipes = listOf(
            Fixture("Later", 4, sortKey = 1000.0, createdAt = "2026-05-01T00:00:00.000Z"),
            Fixture("Earlier", 4, sortKey = 1000.0, createdAt = "2026-01-01T00:00:00.000Z"),
        )
        assertEquals(
            listOf("Earlier", "Later"),
            select(recipes, LibraryView(sort = SortId.RATING, sortDirection = SortDirection.DESCENDING)),
        )
    }

    @Test
    fun `sorting does not mutate the input`() {
        // The caller holds the repository's list; sorting in place would rewrite manual
        // order as a side effect of choosing "Name A-Z".
        val recipes = listOf(Fixture("Zulu"), Fixture("Alpha"))
        selectRecipes(recipes, LibraryView())
        assertEquals(listOf("Zulu", "Alpha"), recipes.map { it.name })
    }

    // --- Reversal ---

    @Test
    fun `clearing search and filters restores the full list in its previous order`() {
        val recipes = listOf(
            Fixture("Alpha", 1, sortKey = 1000.0),
            Fixture("Beta", 5, sortKey = 2000.0),
        )
        val narrowed = LibraryView(search = "beta", filters = LibraryFilters(minRating = 4))
        assertEquals(1, select(recipes, narrowed).size)
        assertEquals(listOf("Alpha", "Beta"), select(recipes, LibraryView()))
    }
}
