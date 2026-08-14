package dev.bondarenko.fujirecipes.data.repo

import dev.bondarenko.fujirecipes.core.net.ApiError
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
    /** When [recipes] were fetched, if they came from the snapshot. Null once refreshed. */
    val servedFromSnapshotAt: String? = null,
    /** False until the first read of either source completes — the difference between an
     *  empty library and one that has not loaded yet. */
    val hasLoaded: Boolean = false,
)
