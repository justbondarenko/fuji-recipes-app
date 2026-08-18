package dev.bondarenko.fujirecipes.ui.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import dev.bondarenko.fujirecipes.core.settings.RecipeViewMode
import dev.bondarenko.fujirecipes.core.settings.UiPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): Pair<SettingsViewModel, UiPreferences> {
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("test_settings_prefs.preferences_pb") },
        )
        val prefs = UiPreferences(testDataStore)
        val viewModel = SettingsViewModel(prefs)
        return viewModel to prefs
    }

    @Test
    fun `starts with default UI preferences`() = runTest(testDispatcher) {
        val (viewModel, _) = createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(RecipeViewMode.GRID, state.recipeViewMode)
        assertTrue(state.showPhotos)
        assertTrue(state.showTags)
        assertTrue(state.showFilmSimulation)
        assertTrue(state.showRating)
    }

    @Test
    fun `changing recipe view mode updates preferences and state`() = runTest(testDispatcher) {
        val (viewModel, _) = createViewModel()
        advanceUntilIdle()

        viewModel.onSelectRecipeViewMode(RecipeViewMode.LIST)
        advanceUntilIdle()

        val state = viewModel.state.first { it.recipeViewMode == RecipeViewMode.LIST }
        assertEquals(RecipeViewMode.LIST, state.recipeViewMode)
    }

    @Test
    fun `toggling photos updates preferences and state`() = runTest(testDispatcher) {
        val (viewModel, _) = createViewModel()
        advanceUntilIdle()

        viewModel.onToggleShowPhotos(false)
        advanceUntilIdle()

        val state = viewModel.state.first { !it.showPhotos }
        assertFalse(state.showPhotos)
    }

    @Test
    fun `toggling tags updates preferences and state`() = runTest(testDispatcher) {
        val (viewModel, _) = createViewModel()
        advanceUntilIdle()

        viewModel.onToggleShowTags(false)
        advanceUntilIdle()

        val state = viewModel.state.first { !it.showTags }
        assertFalse(state.showTags)
    }

    @Test
    fun `toggling film simulation updates preferences and state`() = runTest(testDispatcher) {
        val (viewModel, _) = createViewModel()
        advanceUntilIdle()

        viewModel.onToggleShowFilmSimulation(false)
        advanceUntilIdle()

        val state = viewModel.state.first { !it.showFilmSimulation }
        assertFalse(state.showFilmSimulation)
    }

    @Test
    fun `toggling rating updates preferences and state`() = runTest(testDispatcher) {
        val (viewModel, _) = createViewModel()
        advanceUntilIdle()

        viewModel.onToggleShowRating(false)
        advanceUntilIdle()

        val state = viewModel.state.first { !it.showRating }
        assertFalse(state.showRating)
    }
}
