package dev.bondarenko.fujirecipes.data.repo

import dev.bondarenko.fujirecipes.core.cache.SnapshotCache
import dev.bondarenko.fujirecipes.core.net.ApiClient
import dev.bondarenko.fujirecipes.core.net.ApiResult
import dev.bondarenko.fujirecipes.core.net.ImportResult
import dev.bondarenko.fujirecipes.data.model.Recipe
import kotlinx.serialization.json.JsonObject
import dev.bondarenko.fujirecipes.core.settings.ConnectionConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The only [RecipeRepository] — FEAT-001 T-11.
 *
 * Snapshot first, then network. The library is on screen before the request is sent, which
 * is what makes the app usable on a train, and the refresh replaces it when it lands.
 */
class NetworkRecipeRepository(
    private val api: ApiClient,
    private val cache: SnapshotCache,
    /**
     * The current connection, not the store that holds it.
     *
     * Depending on `ConnectionSettings` would drag a `Context` and DataStore into every
     * test of this class, for one value it reads once per refresh.
     */
    private val config: suspend () -> ConnectionConfig,
    private val now: () -> String,
) : RecipeRepository {

    private val state = MutableStateFlow(LibraryState())
    override val library: StateFlow<LibraryState> = state.asStateFlow()

    /**
     * One refresh at a time.
     *
     * Two in flight — a pull-to-refresh landing on top of the launch fetch — could finish
     * out of order and leave the older response on screen and in the snapshot.
     */
    private val refreshing = Mutex()

    override suspend fun refresh() {
        if (!refreshing.tryLock()) return
        try {
            val connection = config()

            // The snapshot is only read when there is nothing on screen yet. A refresh with
            // a library already loaded must not step backwards to a cached copy.
            if (!state.value.hasLoaded) {
                cache.read(expectedBaseUrl = connection.baseUrl)?.let { snapshot ->
                    state.update {
                        it.copy(
                            recipes = snapshot.recipes,
                            lastUpdatedAt = snapshot.fetchedAt,
                            hasLoaded = true,
                        )
                    }
                }
            }

            state.update { it.copy(isRefreshing = true) }

            when (val result = api.listRecipes()) {
                is ApiResult.Success -> {
                    val fetchedAt = now()
                    cache.write(
                        rawBody = result.value.rawBody,
                        baseUrl = connection.baseUrl,
                        fetchedAt = fetchedAt,
                    )
                    state.update {
                        it.copy(
                            recipes = result.value.recipes,
                            isRefreshing = false,
                            error = null,
                            lastUpdatedAt = fetchedAt,
                            hasLoaded = true,
                        )
                    }
                }

                is ApiResult.Failure -> {
                    /**
                     * **The recipes are left alone.**
                     *
                     * A failed refresh reports a failed refresh; it does not empty the
                     * library. The snapshot is untouched too — overwriting a good cache
                     * with nothing because the train went into a tunnel is how an offline
                     * app becomes useless exactly when it is needed.
                     */
                    state.update {
                        it.copy(
                            isRefreshing = false,
                            error = result.error,
                            hasLoaded = true,
                        )
                    }
                }
            }
        } finally {
            refreshing.unlock()
        }
    }

    override suspend fun create(body: JsonObject): ApiResult<Recipe> =
        afterMutation(api.createRecipe(body))

    override suspend fun update(id: String, body: JsonObject): ApiResult<Recipe> =
        afterMutation(api.updateRecipe(id, body))

    override suspend fun delete(id: String): ApiResult<Unit> =
        afterMutation(api.deleteRecipe(id))

    override suspend fun importAll(body: JsonObject): ApiResult<ImportResult> =
        afterMutation(api.importRecipes(body))

    /**
     * Refresh on success, leave everything alone on failure.
     *
     * A refetch rather than patching the in-memory list: the server assigns `sortKey`,
     * `updatedAt` and — on create — the `id`, so the response is the truth and guessing at
     * it locally is how two clients start disagreeing. The library is one request.
     */
    private suspend fun <T> afterMutation(result: ApiResult<T>): ApiResult<T> {
        if (result is ApiResult.Success) {
            // `hasLoaded` is already true here, so this is a plain network refresh — it will
            // not step backwards to the snapshot.
            refresh()
        }
        return result
    }
}
