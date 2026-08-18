package dev.bondarenko.fujirecipes.ui.recipe.compare

import dev.bondarenko.fujirecipes.core.result.LibraryResult
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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeCompareViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private fun parse(text: String): JsonObject = Json.parseToJsonElement(text).jsonObject

    private val recipeA = Recipe.fromJson(
        parse(
            """
            {
              "id": "recipe-a",
              "name": "Kodachrome 64",
              "rating": 5,
              "settings": {
                "filmSimulation": "classic-chrome",
                "highlightTone": 1.5,
                "shadowTone": 0
              }
            }
            """.trimIndent(),
        ),
    )

    private val recipeB = Recipe.fromJson(
        parse(
            """
            {
              "id": "recipe-b",
              "name": "Portra 400",
              "rating": 4,
              "settings": {
                "filmSimulation": "classic-chrome",
                "highlightTone": -1.0,
                "shadowTone": 0
              }
            }
            """.trimIndent(),
        ),
    )

    private val recipeC = Recipe.fromJson(
        parse(
            """
            {
              "id": "recipe-c",
              "name": "Acros Night",
              "rating": 5,
              "settings": {
                "filmSimulation": "acros-r",
                "highlightTone": 2.0,
                "shadowTone": 1.0
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
    fun `starts in selection mode without preselecting a recipe`() = runTest(testDispatcher) {
        val repo = FakeRecipeRepository(listOf(recipeA, recipeB, recipeC))
        val viewModel = RecipeCompareViewModel(
            baseRecipeId = "recipe-a",
            repository = repo,
        )

        advanceUntilIdle()
        val state = viewModel.state.first { !it.isLoading }

        assertFalse(state.isLoading)
        assertEquals("Kodachrome 64", state.baseRecipe?.name)
        assertNull(state.targetRecipe)
        assertTrue(state.isSelectingTarget)
        assertEquals(2, state.availableCandidates.size)
        assertFalse(state.hasNoCandidates)
    }

    @Test
    fun `selecting candidate activates comparison view`() = runTest(testDispatcher) {
        val repo = FakeRecipeRepository(listOf(recipeA, recipeB, recipeC))
        val viewModel = RecipeCompareViewModel(
            baseRecipeId = "recipe-a",
            repository = repo,
        )

        advanceUntilIdle()
        viewModel.onSelectTargetRecipe("recipe-b")
        advanceUntilIdle()

        val state = viewModel.state.first { it.targetRecipe != null }

        assertFalse(state.isSelectingTarget)
        assertEquals("Portra 400", state.targetRecipe?.name)
        assertTrue(state.groups.isNotEmpty())
    }

    @Test
    fun `handles library with no candidates gracefully`() = runTest(testDispatcher) {
        val repo = FakeRecipeRepository(listOf(recipeA))
        val viewModel = RecipeCompareViewModel(
            baseRecipeId = "recipe-a",
            repository = repo,
        )

        advanceUntilIdle()
        val state = viewModel.state.first { !it.isLoading }

        assertTrue(state.hasNoCandidates)
        assertFalse(state.isSelectingTarget)
        assertEquals("Kodachrome 64", state.baseRecipe?.name)
    }

    @Test
    fun `switching target recipe updates comparison results`() = runTest(testDispatcher) {
        val repo = FakeRecipeRepository(listOf(recipeA, recipeB, recipeC))
        val viewModel = RecipeCompareViewModel(
            baseRecipeId = "recipe-a",
            repository = repo,
            initialTargetRecipeId = "recipe-b",
        )

        advanceUntilIdle()
        viewModel.onSelectTargetRecipe("recipe-c")
        advanceUntilIdle()

        val state = viewModel.state.first { it.targetRecipe?.id == "recipe-c" }
        assertEquals("Acros Night", state.targetRecipe?.name)
    }

    @Test
    fun `clearing target recipe returns to selection mode`() = runTest(testDispatcher) {
        val repo = FakeRecipeRepository(listOf(recipeA, recipeB, recipeC))
        val viewModel = RecipeCompareViewModel(
            baseRecipeId = "recipe-a",
            repository = repo,
            initialTargetRecipeId = "recipe-b",
        )

        advanceUntilIdle()
        viewModel.onClearTargetRecipe()
        advanceUntilIdle()

        val state = viewModel.state.first { !it.isLoading && it.targetRecipe == null }
        assertTrue(state.isSelectingTarget)
    }

    @Test
    fun `toggling differencesOnly filters out matching rows`() = runTest(testDispatcher) {
        val repo = FakeRecipeRepository(listOf(recipeA, recipeB))
        val viewModel = RecipeCompareViewModel(
            baseRecipeId = "recipe-a",
            repository = repo,
            initialTargetRecipeId = "recipe-b",
        )

        advanceUntilIdle()
        viewModel.onDifferencesOnlyChange(true)
        advanceUntilIdle()

        val state = viewModel.state.first { it.differencesOnly }
        assertTrue(state.differencesOnly)
        assertTrue(state.groups.all { group -> group.rows.none { it.isSame } })
    }
}
