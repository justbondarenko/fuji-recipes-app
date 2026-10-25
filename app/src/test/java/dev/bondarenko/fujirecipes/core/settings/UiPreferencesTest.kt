package dev.bondarenko.fujirecipes.core.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class UiPreferencesTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private fun createPreferences(): UiPreferences {
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("test_ui_prefs.preferences_pb") },
        )
        return UiPreferences(testDataStore)
    }

    @Test
    fun `default preferences values match specifications`() = runTest(testDispatcher) {
        val prefs = createPreferences()
        val stored = prefs.preferences.first()

        assertEquals(RecipeViewMode.GRID, stored.recipeViewMode)
        assertTrue(stored.showPhotos)
        assertTrue(stored.showTags)
        assertTrue(stored.showFilmSimulation)
        assertTrue(stored.showRating)
    }

    @Test
    fun `recipe view mode updates and persists`() = runTest(testDispatcher) {
        val prefs = createPreferences()

        prefs.setRecipeViewMode(RecipeViewMode.LIST)
        val updated = prefs.preferences.first()
        assertEquals(RecipeViewMode.LIST, updated.recipeViewMode)

        prefs.setRecipeViewMode(RecipeViewMode.GRID)
        val reverted = prefs.preferences.first()
        assertEquals(RecipeViewMode.GRID, reverted.recipeViewMode)
    }

    @Test
    fun `showPhotos toggle updates and persists`() = runTest(testDispatcher) {
        val prefs = createPreferences()

        prefs.setShowPhotos(false)
        val updated = prefs.preferences.first()
        assertFalse(updated.showPhotos)

        prefs.setShowPhotos(true)
        val reverted = prefs.preferences.first()
        assertTrue(reverted.showPhotos)
    }

    @Test
    fun `showTags toggle updates and persists`() = runTest(testDispatcher) {
        val prefs = createPreferences()

        prefs.setShowTags(false)
        val updated = prefs.preferences.first()
        assertFalse(updated.showTags)

        prefs.setShowTags(true)
        val reverted = prefs.preferences.first()
        assertTrue(reverted.showTags)
    }

    @Test
    fun `showFilmSimulation toggle updates and persists`() = runTest(testDispatcher) {
        val prefs = createPreferences()

        prefs.setShowFilmSimulation(false)
        val updated = prefs.preferences.first()
        assertFalse(updated.showFilmSimulation)

        prefs.setShowFilmSimulation(true)
        val reverted = prefs.preferences.first()
        assertTrue(reverted.showFilmSimulation)
    }

    @Test
    fun `showRating toggle updates and persists`() = runTest(testDispatcher) {
        val prefs = createPreferences()

        prefs.setShowRating(false)
        val updated = prefs.preferences.first()
        assertFalse(updated.showRating)

        prefs.setShowRating(true)
        val reverted = prefs.preferences.first()
        assertTrue(reverted.showRating)
    }

    @Test
    fun `RecipeViewMode fromId falls back to GRID for unknown or null`() {
        assertEquals(RecipeViewMode.GRID, RecipeViewMode.fromId("grid"))
        assertEquals(RecipeViewMode.LIST, RecipeViewMode.fromId("list"))
        assertEquals(RecipeViewMode.GRID, RecipeViewMode.fromId("invalid"))
        assertEquals(RecipeViewMode.GRID, RecipeViewMode.fromId(null))
    }

    @Test
    fun `compact and sidebar pins preserve separate orders and limits`() = runTest(testDispatcher) {
        val prefs = createPreferences()
        val compact = listOf(NavDestinationId.RAW_EDITOR, NavDestinationId.LIBRARY)
        prefs.setPinnedNavIds(compact)
        assertEquals(compact, prefs.preferences.first().railPinnedNavIds)

        val sidebar = NavDestinationId.entries.reversed().take(8)
        prefs.setPinnedNavIds(sidebar + sidebar + NavDestinationId.LIBRARY, forRail = true)
        assertEquals(sidebar, prefs.preferences.first().railPinnedNavIds)
        assertEquals(compact, prefs.preferences.first().pinnedNavIds)

        prefs.setPinnedNavIds(NavDestinationId.entries + NavDestinationId.entries)
        assertEquals(NavDestinationId.entries.take(4), prefs.preferences.first().pinnedNavIds)
        assertEquals(sidebar, prefs.preferences.first().railPinnedNavIds)
        prefs.setPinnedNavIds(emptyList(), forRail = true)
        prefs.setPinnedNavIds(emptyList())
        assertEquals(sidebar, prefs.preferences.first().railPinnedNavIds)
        assertEquals(NavDestinationId.entries.take(4), prefs.preferences.first().pinnedNavIds)

        val stored = "unknown," + (sidebar + sidebar).joinToString(",") { it.id }
        assertEquals(sidebar, NavDestinationId.fromStoredString(stored, 8))
        assertEquals(sidebar.take(4), NavDestinationId.fromStoredString(stored))
        assertEquals(NavDestinationId.DEFAULT_PINNED, NavDestinationId.fromStoredString("unknown"))
    }
}
