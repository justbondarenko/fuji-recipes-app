package dev.bondarenko.fujirecipes.core.store

import dev.bondarenko.fujirecipes.data.model.Recipe
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * The store, which is now the library itself rather than a copy of one.
 *
 * The tests that matter most are the two that distinguish *absent* from *unreadable*: only
 * the first is an empty library, and confusing them is how a recoverable file gets reported
 * as "no recipes yet" and then written over.
 */
class LibraryStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun store(name: String = "library.json") =
        LibraryStore(File(folder.root, name))

    @Test
    fun `a device that has never stored anything has an empty library, not a failure`() =
        runTest {
            val read = store().read()

            val loaded = assertIs<StoreRead.Loaded>(read)
            assertEquals(emptyList<Recipe>(), loaded.library.recipes)
            assertEquals("", loaded.library.updatedAt)
        }

    @Test
    fun `what is written comes back, timestamp and all`() = runTest {
        val store = store()
        val recipes = listOf(
            Recipe(id = "a", name = "Kodachrome 64", rating = 5, sortKey = 1.0),
            Recipe(id = "b", name = "Acros Night", sortKey = 2.0),
        )

        assertEquals(StoreWrite.Ok, store.write(recipes, "2026-08-15T09:00:00.000Z"))

        val loaded = assertIs<StoreRead.Loaded>(store.read())
        assertEquals(listOf("a", "b"), loaded.library.recipes.map { it.id })
        assertEquals("Kodachrome 64", loaded.library.recipes[0].name)
        assertEquals(5, loaded.library.recipes[0].rating)
        assertEquals("2026-08-15T09:00:00.000Z", loaded.library.updatedAt)
    }

    /**
     * The guarantee `Recipe.extra` exists for, taken all the way to the disk.
     *
     * A property written by a newer client — or by the web app — has to survive being loaded
     * and saved by this build, or every edit on this phone would quietly strip it.
     */
    @Test
    fun `a key this build does not know survives a round trip`() = runTest {
        val store = store()
        val original = Recipe.fromJson(
            buildJsonObject {
                put("id", "a")
                put("name", "Kodachrome 64")
                put("lastWrittenSlot", 3)
                put("settings", buildJsonObject { put("filmSimulation", "classic-chrome") })
            },
        )

        store.write(listOf(original), "2026-08-15T09:00:00.000Z")
        val loaded = assertIs<StoreRead.Loaded>(store.read())

        assertEquals(
            JsonPrimitive(3),
            loaded.library.recipes.single().extra["lastWrittenSlot"],
        )
    }

    @Test
    fun `a file that will not parse is unreadable, not empty`() = runTest {
        val file = File(folder.root, "library.json")
        file.writeText("{ this is not json")

        val read = LibraryStore(file).read()

        assertIs<StoreRead.Unreadable>(read)
    }

    /**
     * A library written by a future build is refused rather than read with today's rules,
     * because reading it and saving over it would discard whatever that build added.
     */
    @Test
    fun `a newer format is refused`() = runTest {
        val file = File(folder.root, "library.json")
        file.writeText(
            """{"libraryVersion": 99, "updatedAt": "", "recipes": []}""",
        )

        assertIs<StoreRead.Unreadable>(LibraryStore(file).read())
    }

    @Test
    fun `a file with no version at all is refused too`() = runTest {
        val file = File(folder.root, "library.json")
        file.writeText("""{"recipes": []}""")

        assertIs<StoreRead.Unreadable>(LibraryStore(file).read())
    }

    @Test
    fun `writing creates the directory it was pointed at`() = runTest {
        val file = File(File(folder.root, "nested"), "library.json")

        assertEquals(StoreWrite.Ok, LibraryStore(file).write(emptyList(), "2026-08-15T09:00:00.000Z"))
        assertTrue(file.exists())
    }

    @Test
    fun `an empty library is a written file, not a missing one`() = runTest {
        val store = store()
        store.write(emptyList(), "2026-08-15T09:00:00.000Z")

        val loaded = assertIs<StoreRead.Loaded>(store.read())
        assertEquals(emptyList<Recipe>(), loaded.library.recipes)
        // The distinction a fresh install does not have: this library is empty on purpose.
        assertEquals("2026-08-15T09:00:00.000Z", loaded.library.updatedAt)
    }

    @Test
    fun `settings are stored whole`() = runTest {
        val store = store()
        val settings = buildJsonObject {
            put("filmSimulation", "classic-chrome")
            put("highlight", -1)
            put("colourChromeFx", "strong")
        }

        store.write(
            listOf(Recipe(id = "a", name = "One", settings = settings)),
            "2026-08-15T09:00:00.000Z",
        )

        val loaded = assertIs<StoreRead.Loaded>(store.read())
        assertEquals(settings, loaded.library.recipes.single().settings)
    }
}
