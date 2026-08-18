package dev.bondarenko.fujirecipes.ui.library

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import dev.bondarenko.fujirecipes.core.result.LibraryResult
import dev.bondarenko.fujirecipes.core.settings.UiPreferences
import dev.bondarenko.fujirecipes.core.settings.ViewPreferences
import dev.bondarenko.fujirecipes.data.model.Recipe
import dev.bondarenko.fujirecipes.data.repo.ImportOutcome
import dev.bondarenko.fujirecipes.data.repo.LibraryState
import dev.bondarenko.fujirecipes.data.repo.RecipeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private fun parse(text: String): JsonObject = Json.parseToJsonElement(text).jsonObject

    private val recipe = Recipe.fromJson(
        parse(
            """
            {
              "id": "recipe-1",
              "name": "Kodachrome 64",
              "rating": 5,
              "tags": ["street"],
              "settings": {
                "filmSimulation": "classic-chrome",
                "highlightTone": 1.5
              }
            }
            """.trimIndent(),
        ),
    )

    private class FakeRecipeRepository(initialRecipes: List<Recipe>) : RecipeRepository {
        val stateFlow = MutableStateFlow(
            LibraryState(
                recipes = initialRecipes,
                hasLoaded = true,
            ),
        )
        override val library: Flow<LibraryState> = stateFlow
        override suspend fun load() {}
        override suspend fun create(body: JsonObject): LibraryResult<Recipe> = throw UnsupportedOperationException()
        override suspend fun update(id: String, body: JsonObject): LibraryResult<Recipe> = throw UnsupportedOperationException()
        override suspend fun delete(id: String): LibraryResult<Unit> = throw UnsupportedOperationException()
        override suspend fun importAll(body: JsonObject): LibraryResult<ImportOutcome> = throw UnsupportedOperationException()
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `library state reflects showPhotos, showTags, showFilmSimulation, and showRating from UiPreferences`() = runTest(testDispatcher) {
        val uiDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("test_library_ui_prefs.preferences_pb") },
        )
        val uiPreferences = UiPreferences(uiDataStore)

        val viewDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("test_library_view_prefs.preferences_pb") },
        )
        val viewPreferences = ViewPreferences(viewDataStore)
        val repository = FakeRecipeRepository(listOf(recipe))

        val viewModel = LibraryViewModel(
            repository = repository,
            preferences = viewPreferences,
            uiPreferences = uiPreferences,
        )

        advanceUntilIdle()
        val initialState = viewModel.state.first { it.hasLoaded }
        assertTrue(initialState.hasLoaded)
        assertTrue(initialState.showPhotos)
        assertTrue(initialState.showTags)
        assertTrue(initialState.showFilmSimulation)
        assertTrue(initialState.showRating)

        uiPreferences.setShowPhotos(false)
        uiPreferences.setShowTags(false)
        uiPreferences.setShowFilmSimulation(false)
        uiPreferences.setShowRating(false)
        advanceUntilIdle()

        val updatedState = viewModel.state.first { !it.showPhotos && !it.showTags && !it.showFilmSimulation && !it.showRating }
        assertFalse(updatedState.showPhotos)
        assertFalse(updatedState.showTags)
        assertFalse(updatedState.showFilmSimulation)
        assertFalse(updatedState.showRating)
    }
}
