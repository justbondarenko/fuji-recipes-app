package dev.bondarenko.fujirecipes.ui.cleanup

import dev.bondarenko.fujirecipes.core.result.LibraryResult
import dev.bondarenko.fujirecipes.data.model.Recipe
import dev.bondarenko.fujirecipes.data.repo.LibraryState
import dev.bondarenko.fujirecipes.data.repo.RecipeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CleanupViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeRepository(initialRecipes: List<Recipe>) : RecipeRepository {
        val deletedIds = mutableListOf<String>()
        private val _library = MutableStateFlow(LibraryState(recipes = initialRecipes, hasLoaded = true))
        override val library: StateFlow<LibraryState> = _library

        override suspend fun load() {}
        override suspend fun create(body: JsonObject): LibraryResult<Recipe> = error("Not needed")
        override suspend fun update(id: String, body: JsonObject): LibraryResult<Recipe> = error("Not needed")
        override suspend fun delete(id: String): LibraryResult<Unit> {
            deletedIds += id
            _library.value = _library.value.copy(recipes = _library.value.recipes.filterNot { it.id == id })
            return LibraryResult.Success(Unit)
        }
        override suspend fun importAll(body: JsonObject) = error("Not needed")
    }

    private fun parse(text: String): JsonObject = Json.parseToJsonElement(text).jsonObject

    private fun makeRecipe(id: String, name: String, rating: Int = 0): Recipe {
        val json = """
            {
              "id": "$id",
              "name": "$name",
              "rating": $rating,
              "settings": {
                "filmSimulation": "classic-chrome",
                "highlightTone": 1.0
              }
            }
        """.trimIndent()
        return Recipe.fromJson(parse(json))
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `findDuplicates keeps scanning state for at least 3 seconds`() = runTest {
        val r1 = makeRecipe("r1", "Recipe 1", rating = 3)
        val r2 = makeRecipe("r2", "Recipe 2", rating = 5)
        val repo = FakeRepository(listOf(r1, r2))
        val viewModel = CleanupViewModel(repo)

        advanceUntilIdle()
        viewModel.findDuplicates(minDelayMs = 3000L)

        // At 1500ms, should still be Scanning
        advanceTimeBy(1500)
        assertEquals(CleanupStage.Scanning, viewModel.state.value.stage)

        // At 3000ms+, should transition to Results
        advanceTimeBy(1600)
        val stage = viewModel.state.value.stage
        assertIs<CleanupStage.Results>(stage)
        assertEquals(1, stage.result.exactGroups.size)
    }

    @Test
    fun `findDuplicates populates scan results and default keep mapping`() = runTest {
        val r1 = makeRecipe("r1", "Recipe 1", rating = 3)
        val r2 = makeRecipe("r2", "Recipe 2", rating = 5)
        val repo = FakeRepository(listOf(r1, r2))
        val viewModel = CleanupViewModel(repo)

        advanceUntilIdle()
        assertEquals(2, viewModel.state.value.totalRecipes)

        viewModel.findDuplicates(minDelayMs = 0L)
        advanceUntilIdle()

        val stage = viewModel.state.value.stage
        assertIs<CleanupStage.Results>(stage)
        assertEquals(1, stage.result.exactGroups.size)
        // r2 has higher rating, so it is the default keep
        assertEquals("r2", stage.selectedKeepMap["group_1"])

        // Switch keep selection to r1
        viewModel.selectKeep("group_1", "r1")
        val updatedStage = viewModel.state.value.stage
        assertIs<CleanupStage.Results>(updatedStage)
        assertEquals("r1", updatedStage.selectedKeepMap["group_1"])
    }

    @Test
    fun `deleteDuplicatesForGroup deletes unselected recipes and rescans`() = runTest {
        val r1 = makeRecipe("r1", "Recipe 1", rating = 3)
        val r2 = makeRecipe("r2", "Recipe 2", rating = 5)
        val repo = FakeRepository(listOf(r1, r2))
        val viewModel = CleanupViewModel(repo)

        advanceUntilIdle()
        viewModel.findDuplicates(minDelayMs = 0L)
        advanceUntilIdle()

        // r2 is kept, so r1 should be deleted
        viewModel.deleteDuplicatesForGroup("group_1")
        advanceUntilIdle()

        assertEquals(listOf("r1"), repo.deletedIds)
        val stage = viewModel.state.value.stage
        assertIs<CleanupStage.Results>(stage)
        assertTrue(stage.result.exactGroups.isEmpty())
    }
}
