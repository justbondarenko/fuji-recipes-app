package dev.bondarenko.fujirecipes.data.repo

import dev.bondarenko.fujirecipes.core.result.LibraryError
import dev.bondarenko.fujirecipes.core.result.LibraryResult
import dev.bondarenko.fujirecipes.core.store.LibraryStore
import dev.bondarenko.fujirecipes.data.model.Recipe
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The repository that made the app offline — the bookkeeping a Worker used to do.
 *
 * Two properties run through most of these: **the library is what is on disk**, so a second
 * repository over the same file has to see the same recipes; and **a write that fails changes
 * nothing**, so a library that could not be read is never written over.
 */
class LocalRecipeRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var file: File

    /** Advances on every read, so "was this stamped again?" is a real question. */
    private var clock = 0

    private var ids = 0

    private fun repository(): LocalRecipeRepository {
        if (!::file.isInitialized) file = File(folder.root, "library.json")
        return LocalRecipeRepository(
            store = LibraryStore(file),
            now = { "2026-08-15T09:00:0${clock++}.000Z" },
            newId = { "id-${++ids}" },
        )
    }

    private fun body(
        name: String,
        simulation: String = "provia",
        rating: Int = 0,
    ): JsonObject = buildJsonObject {
        put("name", name)
        put("rating", rating)
        put("settings", buildJsonObject { put("filmSimulation", simulation) })
    }

    // ─── Reading ────────────────────────────────────────────────────────────

    @Test
    fun `a fresh install loads an empty library rather than an error`() = runTest {
        val repository = repository()

        repository.load()

        val state = repository.library.value
        assertTrue(state.hasLoaded)
        assertNull(state.error)
        assertEquals(emptyList<Recipe>(), state.recipes)
    }

    @Test
    fun `what one instance saved, the next one reads`() = runTest {
        assertIs<LibraryResult.Success<*>>(repository().create(body("Kodachrome 64")))

        val second = repository()
        second.load()

        assertEquals(listOf("Kodachrome 64"), second.library.value.recipes.map { it.name })
    }

    // ─── Create ─────────────────────────────────────────────────────────────

    @Test
    fun `create assigns the identity and the timestamps the server used to`() = runTest {
        val repository = repository()

        val result = assertIs<LibraryResult.Success<*>>(repository.create(body("Kodachrome 64")))
        val recipe = repository.library.value.recipes.single()

        assertEquals("id-1", recipe.id)
        assertEquals("Kodachrome 64", recipe.name)
        assertEquals(recipe.createdAt, recipe.updatedAt)
        assertTrue(recipe.createdAt.isNotBlank())
        assertEquals(recipe, result.value)
    }

    @Test
    fun `recipes are appended, so sortKey rises with each one`() = runTest {
        val repository = repository()

        repository.create(body("First"))
        repository.create(body("Second", simulation = "velvia"))

        val keys = repository.library.value.recipes.map { it.sortKey }
        assertEquals(keys.sorted(), keys)
        assertTrue(keys[0] < keys[1])
    }

    @Test
    fun `a recipe with no name is refused, and nothing is written`() = runTest {
        val repository = repository()

        val result = repository.create(buildJsonObject { put("rating", 3) })

        assertIs<LibraryError.Invalid>(assertIs<LibraryResult.Failure>(result).error)
        assertEquals(emptyList<Recipe>(), repository.library.value.recipes)
    }

    // ─── Update ─────────────────────────────────────────────────────────────

    @Test
    fun `a partial update changes only what it names`() = runTest {
        val repository = repository()
        repository.create(body("Kodachrome 64", rating = 3))
        val before = repository.library.value.recipes.single()

        val result = repository.update(before.id, buildJsonObject { put("rating", 5) })

        val after = assertIs<LibraryResult.Success<*>>(result).value as Recipe
        assertEquals(5, after.rating)
        assertEquals(before.name, after.name)
        assertEquals(before.settings, after.settings)
        assertEquals(before.createdAt, after.createdAt)
        assertEquals(before.sortKey, after.sortKey)
        assertNotEquals(before.updatedAt, after.updatedAt)
    }

    /**
     * `coding-standards.md` P2, at the layer that could most easily break it: an edit is a
     * read-modify-write now, so a key this build does not model has to come out the other side.
     */
    @Test
    fun `an unknown key survives an edit`() = runTest {
        val repository = repository()
        repository.create(
            buildJsonObject {
                put("name", "Kodachrome 64")
                put("lastWrittenSlot", 3)
                put("settings", buildJsonObject { put("filmSimulation", "provia") })
            },
        )
        val id = repository.library.value.recipes.single().id

        repository.update(id, buildJsonObject { put("rating", 4) })

        val after = repository.library.value.recipes.single()
        assertEquals(4, after.rating)
        assertEquals(JsonPrimitive(3), after.extra["lastWrittenSlot"])
    }

    @Test
    fun `updating a recipe that is gone says so rather than creating one`() = runTest {
        val repository = repository()
        repository.load()

        val result = repository.update("nobody", buildJsonObject { put("rating", 5) })

        assertIs<LibraryError.NotFound>(assertIs<LibraryResult.Failure>(result).error)
        assertEquals(emptyList<Recipe>(), repository.library.value.recipes)
    }

    @Test
    fun `an update may not rewrite the id`() = runTest {
        val repository = repository()
        repository.create(body("Kodachrome 64"))
        val id = repository.library.value.recipes.single().id

        repository.update(id, buildJsonObject { put("id", "something-else") })

        assertEquals(id, repository.library.value.recipes.single().id)
    }

    // ─── Delete ─────────────────────────────────────────────────────────────

    @Test
    fun `delete removes the recipe and leaves the rest`() = runTest {
        val repository = repository()
        repository.create(body("First"))
        repository.create(body("Second", simulation = "velvia"))
        val first = repository.library.value.recipes.first().id

        assertIs<LibraryResult.Success<*>>(repository.delete(first))

        assertEquals(listOf("Second"), repository.library.value.recipes.map { it.name })
    }

    @Test
    fun `deleting something already gone is the outcome the caller wanted`() = runTest {
        val repository = repository()
        repository.load()

        assertIs<LibraryResult.Success<*>>(repository.delete("nobody"))
    }

    // ─── Import ─────────────────────────────────────────────────────────────

    @Test
    fun `entries with no id are imported and given one`() = runTest {
        val repository = repository()

        val result = repository.importAll(
            importBody(listOf(body("From C1"), body("From C2", simulation = "velvia"))),
        )

        val outcome = assertIs<LibraryResult.Success<*>>(result).value as ImportOutcome
        assertEquals(2, outcome.imported)
        assertEquals(0, outcome.skipped)
        assertEquals(0, outcome.replaced)
        assertEquals(
            listOf("From C1", "From C2"),
            repository.library.value.recipes.map { it.name },
        )
        assertTrue(repository.library.value.recipes.all { it.id.isNotBlank() })
    }

    @Test
    fun `an id the library does not hold is kept, so a backup restores as itself`() = runTest {
        val repository = repository()

        repository.importAll(
            importBody(
                listOf(
                    JsonObject(body("Kodachrome 64") + mapOf("id" to JsonPrimitive("from-file"))),
                ),
            ),
        )

        assertEquals("from-file", repository.library.value.recipes.single().id)
    }

    @Test
    fun `an id collision with no resolution is skipped rather than overwritten`() = runTest {
        val repository = repository()
        repository.create(body("Original"))
        val id = repository.library.value.recipes.single().id

        val result = repository.importAll(
            importBody(listOf(JsonObject(body("Incoming") + mapOf("id" to JsonPrimitive(id))))),
        )

        val outcome = assertIs<LibraryResult.Success<*>>(result).value as ImportOutcome
        assertEquals(0, outcome.imported)
        assertEquals(1, outcome.skipped)
        assertEquals("Original", repository.library.value.recipes.single().name)
    }

    @Test
    fun `replace overwrites in place and keeps the original's place in the library`() = runTest {
        val repository = repository()
        repository.create(body("Original"))
        val before = repository.library.value.recipes.single()

        val result = repository.importAll(
            importBody(
                listOf(JsonObject(body("Incoming") + mapOf("id" to JsonPrimitive(before.id)))),
                resolutions = mapOf(before.id to "replace"),
            ),
        )

        val outcome = assertIs<LibraryResult.Success<*>>(result).value as ImportOutcome
        assertEquals(1, outcome.replaced)
        assertEquals(0, outcome.imported)

        val after = repository.library.value.recipes.single()
        assertEquals(before.id, after.id)
        assertEquals("Incoming", after.name)
        assertEquals(before.sortKey, after.sortKey)
        // A replaced recipe is the same recipe, changed — not a new one made today.
        assertEquals(before.createdAt, after.createdAt)
    }

    @Test
    fun `keep-both imports alongside, under an id of its own`() = runTest {
        val repository = repository()
        repository.create(body("Original"))
        val id = repository.library.value.recipes.single().id

        val result = repository.importAll(
            importBody(
                listOf(JsonObject(body("Incoming") + mapOf("id" to JsonPrimitive(id)))),
                resolutions = mapOf(id to "keep-both"),
            ),
        )

        val outcome = assertIs<LibraryResult.Success<*>>(result).value as ImportOutcome
        assertEquals(1, outcome.imported)

        val recipes = repository.library.value.recipes
        assertEquals(listOf("Original", "Incoming"), recipes.map { it.name })
        assertEquals(2, recipes.map { it.id }.distinct().size)
    }

    /**
     * SF-014: one bad recipe in a batch is named, and the rest still land. Failing the whole
     * import would punish forty good recipes for one.
     */
    @Test
    fun `an invalid entry is reported and the rest are imported`() = runTest {
        val repository = repository()

        val result = repository.importAll(
            importBody(
                listOf(
                    body("Good one"),
                    buildJsonObject { put("rating", 3) },
                    body("Another good one", simulation = "velvia"),
                ),
            ),
        )

        val outcome = assertIs<LibraryResult.Success<*>>(result).value as ImportOutcome
        assertEquals(2, outcome.imported)
        assertEquals(1, outcome.failed.size)
        assertEquals(1, outcome.failed.single().index)
        assertEquals(
            listOf("Good one", "Another good one"),
            repository.library.value.recipes.map { it.name },
        )
    }

    // ─── Refusing to write over a library it cannot read ─────────────────────

    @Test
    fun `an unreadable library is reported and never written over`() = runTest {
        file = File(folder.root, "library.json")
        file.writeText("{ this is not json")
        val repository = repository()

        val result = repository.create(body("Kodachrome 64"))

        assertIs<LibraryError.Unreadable>(assertIs<LibraryResult.Failure>(result).error)
        // The whole point: the bytes that would not parse are still there to be recovered.
        assertEquals("{ this is not json", file.readText())
    }

    @Test
    fun `an unreadable library refuses an import too`() = runTest {
        file = File(folder.root, "library.json")
        file.writeText("{ this is not json")
        val repository = repository()

        val result = repository.importAll(importBody(listOf(body("Kodachrome 64"))))

        assertIs<LibraryError.Unreadable>(assertIs<LibraryResult.Failure>(result).error)
        assertEquals("{ this is not json", file.readText())
    }

    @Test
    fun `the library state reports the failure without emptying the screen`() = runTest {
        file = File(folder.root, "library.json")
        file.writeText("{ this is not json")
        val repository = repository()

        repository.load()

        val state = repository.library.value
        assertTrue(state.hasLoaded)
        assertIs<LibraryError.Unreadable>(state.error)
    }

    private fun importBody(
        recipes: List<JsonObject>,
        resolutions: Map<String, String> = emptyMap(),
    ): JsonObject = buildJsonObject {
        put("recipes", JsonArray(recipes))
        put(
            "resolutions",
            JsonObject(resolutions.mapValues { (_, value) -> JsonPrimitive(value) }),
        )
    }
}
