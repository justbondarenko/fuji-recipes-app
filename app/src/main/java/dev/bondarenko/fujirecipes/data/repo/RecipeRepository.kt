package dev.bondarenko.fujirecipes.data.repo

import dev.bondarenko.fujirecipes.core.net.ApiError
import dev.bondarenko.fujirecipes.core.net.ApiResult
import kotlinx.serialization.json.JsonObject
import dev.bondarenko.fujirecipes.data.model.Recipe
import kotlinx.coroutines.flow.Flow

/**
 * The library, to everything above it — FEAT-001 T-11.
 *
 * The one interface-with-one-implementation this codebase allows
 * (`coding-standards.md` P8), and it earns the exemption by being the seam that a future
 * offline-write story would swap: `architecture.md` §4 rejects a Room mirror for v1, and
 * this is what makes that a replacement rather than a rewrite.
 */
interface RecipeRepository {
    val library: Flow<LibraryState>
    suspend fun refresh()

    /**
     * The mutations — FEAT-003 T-03.
     *
     * Each returns its outcome rather than throwing, and **nothing local changes on
     * failure**: there is no optimistic update and no queue (`architecture.md` §4), so a
     * save that did not reach the server has simply not happened. On success the library is
     * refreshed, which is what makes the list and the view follow along.
     */
    suspend fun create(body: JsonObject): ApiResult<Recipe>
    suspend fun update(id: String, body: JsonObject): ApiResult<Recipe>
    suspend fun delete(id: String): ApiResult<Unit>
}

/**
 * What is known about the library right now.
 *
 * Recipes and an error are **not** alternatives, and that is the whole design. A refresh
 * that fails while a snapshot is loaded must leave the recipes on screen and say the
 * refresh failed — collapsing this into a `Result` would force the UI to choose between
 * showing the library and reporting the failure, and with no signal in the field the right
 * answer is both.
 */
data class LibraryState(
    val recipes: List<Recipe> = emptyList(),
    /** Whether a network refresh is in flight. */
    val isRefreshing: Boolean = false,
    /** The most recent refresh failure, or null if the last one succeeded. */
    val error: ApiError? = null,
    /**
     * When these recipes were fetched — from the network just now, or from the snapshot on
     * a cold start.
     *
     * One field rather than a separate "this is stale" flag: the list shows the time either
     * way and lets the reader judge. A three-minute-old copy and a three-day-old one are
     * both "cached", and only one of them is worth doing something about.
     */
    val lastUpdatedAt: String? = null,
    /** False until the first read of either source completes — the difference between an
     *  empty library and one that has not loaded yet. */
    val hasLoaded: Boolean = false,
)
