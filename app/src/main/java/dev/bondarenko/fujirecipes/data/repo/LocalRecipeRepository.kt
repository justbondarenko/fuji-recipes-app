package dev.bondarenko.fujirecipes.data.repo

import dev.bondarenko.fujirecipes.core.result.FieldProblem
import dev.bondarenko.fujirecipes.core.result.LibraryError
import dev.bondarenko.fujirecipes.core.result.LibraryResult
import dev.bondarenko.fujirecipes.core.store.LibraryStore
import dev.bondarenko.fujirecipes.core.store.StoreRead
import dev.bondarenko.fujirecipes.core.store.StoreWrite
import dev.bondarenko.fujirecipes.data.importing.validateFileRecipe
import dev.bondarenko.fujirecipes.data.model.Recipe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * The only [RecipeRepository]: the library lives on this device and nowhere else.
 *
 * It replaced a network implementation that fetched from a Cloudflare Worker and kept a
 * snapshot for the train. **The snapshot became the library.** Recipes move in and out of
 * this app by export and import now, deliberately and by hand, which is the whole of the
 * sync story — see `steering/architecture.md` §4.
 *
 * What that changes for everything above it is smaller than it sounds. Ids, `sortKey` and
 * the timestamps used to be the server's to assign, and are assigned here instead; every
 * screen still hands over a body and gets back the stored recipe.
 */
class LocalRecipeRepository(
    private val store: LibraryStore,
    private val now: () -> String,
    private val newId: () -> String,
) : RecipeRepository {

    private val state = MutableStateFlow(LibraryState())
    override val library: StateFlow<LibraryState> = state.asStateFlow()

    /**
     * One mutation at a time.
     *
     * Two writes in flight — a swipe-delete landing on top of a save — would each rewrite the
     * whole file from the list they started with, and the loser's changes would vanish.
     */
    private val mutating = Mutex()

    override suspend fun load() = mutating.withLock { reload() }

    /** Reads the file into [state]. Assumes the caller holds [mutating]. */
    private suspend fun reload() {
        when (val read = store.read()) {
            is StoreRead.Loaded -> state.update {
                it.copy(
                    // Ascending `sortKey` is the library's own order, and the one every
                    // "manual" reading of the list assumes.
                    recipes = read.library.recipes.sortedBy { recipe -> recipe.sortKey },
                    error = null,
                    lastUpdatedAt = read.library.updatedAt.takeIf { at -> at.isNotBlank() },
                    hasLoaded = true,
                )
            }

            // **The recipes are left alone, and so is the file.** Nothing is written after
            // this until a read succeeds — see `editable`. Overwriting a library that would
            // not parse is how a recoverable file becomes a lost one.
            is StoreRead.Unreadable -> state.update {
                it.copy(error = LibraryError.Unreadable(read.message), hasLoaded = true)
            }
        }
    }

    override suspend fun create(body: JsonObject): LibraryResult<Recipe> = mutating.withLock {
        val current = editable() ?: return@withLock unreadable()

        val at = now()
        val candidate = JsonObject(
            body + mapOf(
                // P2 in reverse: the phone no longer *invents* what a server would assign,
                // it *is* the thing that assigns it. Ids, order and both timestamps are set
                // here so that no screen has to guess at them.
                "id" to JsonPrimitive(newId()),
                "sortKey" to JsonPrimitive(nextSortKey(current)),
                "createdAt" to JsonPrimitive(at),
                "updatedAt" to JsonPrimitive(at),
            ),
        )

        val recipe = decode(candidate) ?: return@withLock LibraryResult.Failure(rejection(candidate))
        commit(current + recipe, at)?.let { return@withLock LibraryResult.Failure(it) }
        LibraryResult.Success(recipe)
    }

    override suspend fun update(id: String, body: JsonObject): LibraryResult<Recipe> =
        mutating.withLock {
            val current = editable() ?: return@withLock unreadable()

            val existing = current.firstOrNull { it.id == id }
                ?: return@withLock LibraryResult.Failure(
                    LibraryError.NotFound("That recipe is no longer in your library.", id),
                )

            val at = now()
            // A partial body, applied key by key over what is stored — so a key this build
            // never displayed is carried through an edit untouched (`coding-standards.md` P2).
            // Identity and order are then forced back: they are not the caller's to change.
            val candidate = JsonObject(
                existing.toJson() + body + mapOf(
                    "id" to JsonPrimitive(existing.id),
                    "sortKey" to JsonPrimitive(existing.sortKey),
                    "createdAt" to JsonPrimitive(existing.createdAt),
                    "updatedAt" to JsonPrimitive(at),
                ),
            )

            val updated = decode(candidate)
                ?: return@withLock LibraryResult.Failure(rejection(candidate))
            val next = current.map { if (it.id == id) updated else it }
            commit(next, at)?.let { return@withLock LibraryResult.Failure(it) }
            LibraryResult.Success(updated)
        }

    /** Idempotent: deleting a recipe that is already gone is the outcome the caller wanted. */
    override suspend fun delete(id: String): LibraryResult<Unit> = mutating.withLock {
        val current = editable() ?: return@withLock unreadable()
        if (current.none { it.id == id }) return@withLock LibraryResult.Success(Unit)

        val at = now()
        commit(current.filterNot { it.id == id }, at)
            ?.let { return@withLock LibraryResult.Failure(it) }
        LibraryResult.Success(Unit)
    }

    /**
     * Import a batch — the local half of what `POST /api/import` used to do.
     *
     * [body] is `{ "recipes": [...], "resolutions": { id: "skip" | "replace" | "keep-both" } }`,
     * the shape `data/importing` builds for both the camera and the file flows.
     *
     * **Atomic.** Every entry is decided against the library as it stands and the result is
     * written once, so a failure part-way through writes nothing. An entry that fails
     * validation is reported in [ImportOutcome.failed] rather than failing the batch — one
     * bad recipe in a file of forty is not a reason to import none of them (SF-014).
     */
    override suspend fun importAll(body: JsonObject): LibraryResult<ImportOutcome> =
        mutating.withLock {
            val current = editable() ?: return@withLock unreadable()

            val entries = (body["recipes"] as? JsonArray).orEmpty()
            val resolutions = (body["resolutions"] as? JsonObject).orEmpty()

            val at = now()
            val working = current.toMutableList()
            val failed = mutableListOf<ImportFailure>()
            var imported = 0
            var skipped = 0
            var replaced = 0

            entries.forEachIndexed { index, element ->
                val entry = element as? JsonObject
                if (entry == null) {
                    failed += ImportFailure(index, null, "This entry is not a recipe object.")
                    return@forEachIndexed
                }

                val id = entry.string("id")
                val existingAt = if (id == null) -1 else working.indexOfFirst { it.id == id }
                val resolution = if (existingAt < 0) null else resolutions.string(id)

                // Decided before it is decoded, so a skip costs nothing and a recipe that
                // cannot be read is only reported when it was actually going to be written.
                if (existingAt >= 0 && resolution != REPLACE && resolution != KEEP_BOTH) {
                    // "skip", and anything undecided. An id collision with no resolution is
                    // not a licence to overwrite: the caller was asked and did not answer.
                    skipped++
                    return@forEachIndexed
                }

                val recipe = if (resolution == REPLACE) {
                    // A replaced recipe *is* the one it lands on, changed: same id, same
                    // place in the library, and not something made today.
                    val previous = working[existingAt]
                    stored(entry, previous.id, previous.sortKey, at, previous.createdAt)
                } else {
                    stored(
                        entry = entry,
                        // Keep-both is a new recipe. Otherwise an id the file carried and the
                        // library does not is kept as it is — that is what makes an export a
                        // backup rather than a copy.
                        id = if (resolution == KEEP_BOTH) newId() else id ?: newId(),
                        sortKey = nextSortKey(working),
                        at = at,
                        createdAt = null,
                    )
                }

                if (recipe == null) {
                    val problem = validateFileRecipe(entry).firstOrNull()
                    failed += ImportFailure(
                        index = index,
                        path = problem?.fieldId?.ifBlank { null },
                        message = problem?.message
                            ?: "This recipe is not in a form this app can store.",
                    )
                    return@forEachIndexed
                }

                if (resolution == REPLACE) {
                    working[existingAt] = recipe
                    replaced++
                } else {
                    working += recipe
                    imported++
                }
            }

            commit(working, at)?.let { return@withLock LibraryResult.Failure(it) }

            LibraryResult.Success(
                ImportOutcome(
                    imported = imported,
                    skipped = skipped,
                    replaced = replaced,
                    failed = failed,
                ),
            )
        }

    // ─── The parts every mutation shares ────────────────────────────────────

    /**
     * The list to build on, or null when it must not be built on.
     *
     * Loads first if nothing has been read yet, and refuses outright once the store has
     * reported a file it could not parse. That refusal is the point: every write replaces the
     * whole file, so writing on top of a library we cannot read would destroy it.
     */
    private suspend fun editable(): List<Recipe>? {
        if (!state.value.hasLoaded) reload()
        return if (state.value.error is LibraryError.Unreadable) null else state.value.recipes
    }

    private fun unreadable(): LibraryResult.Failure =
        LibraryResult.Failure(
            state.value.error as? LibraryError.Unreadable
                ?: LibraryError.Unreadable("The stored library could not be read."),
        )

    /** Writes the library and publishes it. Returns the failure, or null on success. */
    private suspend fun commit(next: List<Recipe>, at: String): LibraryError? =
        when (val write = store.write(next, at)) {
            is StoreWrite.Ok -> {
                state.update {
                    it.copy(
                        recipes = next.sortedBy { recipe -> recipe.sortKey },
                        error = null,
                        lastUpdatedAt = at,
                        hasLoaded = true,
                    )
                }
                null
            }

            is StoreWrite.Failed -> {
                val error = LibraryError.Storage(write.message)
                // The failure is published, the recipes are not touched: the file still holds
                // what it held, and so does the screen.
                state.update { it.copy(error = error) }
                error
            }
        }

    /**
     * A candidate as a stored recipe, or null when it is not one.
     *
     * Validated **and** decoded here, because they are two ways for the same object to be
     * unusable and a caller that handled only the first would still crash on the second —
     * an entry out of a file this app did not write is not obliged to be well formed.
     */
    private fun decode(candidate: JsonObject): Recipe? {
        if (validateFileRecipe(candidate).isNotEmpty()) return null
        return runCatching { Recipe.fromJson(candidate) }.getOrNull()
    }

    /** Why [decode] said no, as something a person can read. */
    private fun rejection(candidate: JsonObject): LibraryError.Invalid {
        val problems = validateFileRecipe(candidate)
        if (problems.isEmpty()) {
            return LibraryError.Invalid("This recipe is not in a form this app can store.")
        }

        return LibraryError.Invalid(
            problems.first().let { problem ->
                if (problem.fieldId.isBlank()) problem.message else "${problem.fieldId}: ${problem.message}"
            },
            problems.map { FieldProblem(it.fieldId, it.message) },
        )
    }

    /** An imported entry as a stored recipe: its own content, this library's bookkeeping. */
    private fun stored(
        entry: JsonObject,
        id: String,
        sortKey: Double,
        at: String,
        createdAt: String?,
    ): Recipe? = decode(
        JsonObject(
            entry + mapOf(
                "id" to JsonPrimitive(id),
                "sortKey" to JsonPrimitive(sortKey),
                // A file that carries its own `createdAt` keeps it — restoring a backup
                // should not tell you every recipe was made today.
                "createdAt" to JsonPrimitive(createdAt ?: entry.string("createdAt") ?: at),
                "updatedAt" to JsonPrimitive(at),
            ),
        ),
    )

    /** Appended at the end, in the order the batch was reviewed in (SF-009). */
    private fun nextSortKey(recipes: List<Recipe>): Double =
        (recipes.maxOfOrNull { it.sortKey } ?: 0.0) + 1.0

    private companion object {
        const val REPLACE = "replace"
        const val KEEP_BOTH = "keep-both"

        fun Map<String, JsonElement>.string(key: String?): String? =
            key?.let { (this[it] as? JsonPrimitive)?.takeIf { value -> value.isString }?.content }
    }
}
