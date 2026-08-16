package dev.bondarenko.fujirecipes.data.repo

import dev.bondarenko.fujirecipes.core.net.ApiResult
import dev.bondarenko.fujirecipes.core.net.ImportResult
import dev.bondarenko.fujirecipes.data.model.Recipe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * A library held in memory, for running the app with no server.
 *
 * `AppContainer` reaches for this **in debug builds only**, and only when no connection has
 * been configured — the case that otherwise parks you on the setup form with nothing to look
 * at, which is exactly when you are trying to look at something. A release build with no
 * configuration still goes to setup, because there the form is the right answer.
 *
 * Writes work and are lost on process death. That is the point: it is a fixture, not an
 * offline mode, and `architecture.md` §4 is explicit that offline writes are not a v1 story.
 */
class DemoRecipeRepository : RecipeRepository {

    private val state = MutableStateFlow(
        LibraryState(recipes = SEED, hasLoaded = true, lastUpdatedAt = "2026-08-16T00:00:00.000Z"),
    )

    override val library = state.asStateFlow()

    override suspend fun refresh() = Unit

    override suspend fun create(body: JsonObject): ApiResult<Recipe> {
        val recipe = Recipe.fromJson(body).copy(id = "demo-${state.value.recipes.size + 1}")
        state.update { it.copy(recipes = it.recipes + recipe) }
        return ApiResult.Success(recipe)
    }

    override suspend fun update(id: String, body: JsonObject): ApiResult<Recipe> {
        val recipe = Recipe.fromJson(body).copy(id = id)
        state.update { current ->
            current.copy(recipes = current.recipes.map { if (it.id == id) recipe else it })
        }
        return ApiResult.Success(recipe)
    }

    override suspend fun delete(id: String): ApiResult<Unit> {
        state.update { current ->
            current.copy(recipes = current.recipes.filterNot { it.id == id })
        }
        return ApiResult.Success(Unit)
    }

    override suspend fun importAll(body: JsonObject): ApiResult<ImportResult> =
        ApiResult.Success(ImportResult(imported = 0, skipped = 0, replaced = 0, failed = emptyList()))

    private companion object {
        /**
         * Enough variety to exercise the screens rather than to be a plausible library: a
         * five-star and an unrated one, a long name, a tagless one, and a monochrome
         * simulation so the applicability rules have something to hide.
         */
        val SEED = listOf(
            demo(
                id = "demo-1",
                name = "Kodachrome 64",
                rating = 5,
                tags = listOf("street", "warm", "vintage"),
                simulation = "classic-chrome",
                extras = mapOf("highlightTone" to 1.0, "shadowTone" to 2.0, "color" to 3.0),
            ),
            demo(
                id = "demo-2",
                name = "Acros Night",
                rating = 4,
                tags = listOf("monochrome", "moody"),
                simulation = "acros-r",
                extras = mapOf("highlightTone" to 2.0, "sharpness" to -1.0),
            ),
            demo(
                id = "demo-3",
                name = "A Deliberately Rather Long Recipe Name",
                rating = 0,
                tags = emptyList(),
                simulation = "eterna",
                extras = mapOf("clarity" to -3.0),
            ),
        )

        fun demo(
            id: String,
            name: String,
            rating: Int,
            tags: List<String>,
            simulation: String,
            extras: Map<String, Double>,
        ) = Recipe(
            id = id,
            name = name,
            rating = rating,
            tags = tags,
            notes = "Sample data — this build has no server configured.",
            settings = JsonObject(
                buildMap {
                    put("filmSimulation", JsonPrimitive(simulation))
                    extras.forEach { (key, value) -> put(key, JsonPrimitive(value)) }
                },
            ),
            updatedAt = "2026-08-16T00:00:00.000Z",
        )
    }
}
