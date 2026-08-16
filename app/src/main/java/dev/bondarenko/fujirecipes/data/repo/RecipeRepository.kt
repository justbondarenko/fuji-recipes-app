package dev.bondarenko.fujirecipes.data.repo

import dev.bondarenko.fujirecipes.core.result.LibraryError
import dev.bondarenko.fujirecipes.core.result.LibraryResult
import dev.bondarenko.fujirecipes.data.model.Recipe
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonObject

/**
 * The library, to everything above it — FEAT-001 T-11.
 *
 * The one interface-with-one-implementation this codebase allows
 * (`coding-standards.md` P8), and it keeps the exemption now that the implementation is
 * local: the seam is what let the network implementation be replaced with an on-device one
 * without a screen or a ViewModel changing shape.
 */
interface RecipeRepository {
    val library: Flow<LibraryState>

    /**
     * Read the library off the device into [library].
     *
     * Idempotent and cheap to call again — a screen that wants to recover from an
     * [LibraryError.Unreadable] simply calls it. Every mutation below reloads on success, so
     * nothing else has to.
     */
    suspend fun load()

    /**
     * The mutations — FEAT-003 T-03.
     *
     * Each returns its outcome rather than throwing, and **nothing changes on failure**: the
     * library is rewritten whole through a temporary file, so a save that did not land has
     * simply not happened.
     */
    suspend fun create(body: JsonObject): LibraryResult<Recipe>
    suspend fun update(id: String, body: JsonObject): LibraryResult<Recipe>
    suspend fun delete(id: String): LibraryResult<Unit>

    /**
     * Import several recipes at once — FEAT-007, FEAT-012.
     *
     * One call rather than a loop over [create], because it is **atomic**: every entry is
     * decided against the library as it stands, and the result is written once. A failure
     * writes nothing, so the library never ends up holding half a camera or half a file.
     */
    suspend fun importAll(body: JsonObject): LibraryResult<ImportOutcome>
}

/**
 * What an import did.
 *
 * `replaced` is only ever non-zero for a file import: replacing needs an id collision, and
 * nothing read off a camera carries an id.
 */
data class ImportOutcome(
    val imported: Int,
    val skipped: Int,
    val replaced: Int,
    val failed: List<ImportFailure>,
)

data class ImportFailure(
    /** Which recipe in the request. */
    val index: Int?,
    /** The field path that failed validation, e.g. `settings.clarity`. */
    val path: String?,
    val message: String,
)

/**
 * What is known about the library right now.
 *
 * Recipes and an error are **not** alternatives, and that is still the design. A failed save
 * with a library already on screen must leave the recipes where they are and say the save
 * failed — collapsing this into a `Result` would force the UI to choose between showing the
 * library and reporting the failure, and the right answer is both.
 */
data class LibraryState(
    val recipes: List<Recipe> = emptyList(),
    /** The most recent failure, or null if the last operation succeeded. */
    val error: LibraryError? = null,
    /**
     * When the library was last changed on this device.
     *
     * Stamped by the store on every write, so it survives a restart and answers the question
     * the list footer asks: how recent is what I am looking at.
     */
    val lastUpdatedAt: String? = null,
    /** False until the first read completes — the difference between an empty library and
     *  one that has not loaded yet. */
    val hasLoaded: Boolean = false,
)
