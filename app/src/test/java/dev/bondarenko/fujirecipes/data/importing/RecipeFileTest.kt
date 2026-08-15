package dev.bondarenko.fujirecipes.data.importing

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * `recipe-format.spec.md` §6's conformance fixtures, as tests.
 *
 * §6 calls them a release gate: a file this app exported has to import here, and the failures
 * the spec names have to fail the way it names them.
 */
class RecipeFileTest {

    private fun envelope(version: Int = 1, recipes: String = "[]") = """
        {"format":"fuji-recipe","version":$version,"exportedAt":"2026-08-04T10:00:00.000Z",
         "recipes":$recipes}
    """.trimIndent()

    private val oneRecipe = """{"name":"Kodachrome 64","settings":{"filmSimulation":"classic-chrome"}}"""

    // ─── SF-005: the three shapes ───────────────────────────────────────────

    @Test
    fun `reads the envelope`() {
        val parsed = readJsonFile(envelope(recipes = "[$oneRecipe]"))

        assertEquals(1, parsed.version)
        assertEquals(1, parsed.recipes.size)
        assertEquals(0, parsed.recipes[0].index)
        assertEquals(
            "Kodachrome 64",
            (parsed.recipes[0].recipe as JsonObject)["name"]?.let { (it as JsonPrimitive).content },
        )
    }

    @Test
    fun `reads a bare array`() {
        val parsed = readJsonFile("[$oneRecipe,$oneRecipe]")

        assertEquals(2, parsed.recipes.size)
        assertEquals(listOf(0, 1), parsed.recipes.map { it.index })
    }

    @Test
    fun `reads a bare recipe object`() {
        val parsed = readJsonFile(oneRecipe)

        assertEquals(1, parsed.recipes.size)
        assertEquals(RECIPE_FORMAT_VERSION_FOR_TEST, parsed.version)
    }

    // ─── SF-003, SF-004, SF-006: refusing the file ──────────────────────────

    @Test
    fun `refuses another application's format`() {
        val rejected = assertFailsWith<ImportRejected> {
            readJsonFile("""{"format":"something-else","version":1,"recipes":[]}""")
        }

        assertEquals(RejectionReason.UNKNOWN_FORMAT, rejected.reason)
        assertTrue(rejected.message!!.contains("something-else"))
    }

    @Test
    fun `refuses a file from a newer app, in the words the spec requires`() {
        val rejected = assertFailsWith<ImportRejected> { readJsonFile(envelope(version = 99)) }

        assertEquals(RejectionReason.FUTURE_VERSION, rejected.reason)
        assertTrue(rejected.message!!.startsWith(FUTURE_VERSION_MESSAGE))
        assertTrue(rejected.message!!.contains("99"))
    }

    @Test
    fun `refuses a version that is not a whole number of one or more`() {
        listOf(""""1"""", "0", "1.5").forEach { version ->
            val rejected = assertFailsWith<ImportRejected> {
                readJsonFile("""{"format":"fuji-recipe","version":$version,"recipes":[]}""")
            }
            assertEquals(RejectionReason.UNREADABLE, rejected.reason)
        }
    }

    @Test
    fun `refuses text that is not JSON, and says which file`() {
        val rejected = assertFailsWith<ImportRejected> { readJsonFile("not json", "bundle/a.json") }

        assertEquals(RejectionReason.UNREADABLE, rejected.reason)
        assertTrue(rejected.message!!.contains("bundle/a.json"))
    }

    /** SF-014's ground floor: a non-object entry survives parsing so the review can list it. */
    @Test
    fun `keeps an entry that is not an object, for the review to fail`() {
        val parsed = readJsonFile("""[$oneRecipe, 42]""")

        assertEquals(2, parsed.recipes.size)
        assertTrue(validateFileRecipe(parsed.recipes[1].recipe).isNotEmpty())
    }

    // ─── SF-015: archives ───────────────────────────────────────────────────

    private fun zip(entries: Map<String, String>): ByteArray {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { out ->
            entries.forEach { (name, content) ->
                out.putNextEntry(ZipEntry(name))
                out.write(content.toByteArray())
                out.closeEntry()
            }
        }
        return bytes.toByteArray()
    }

    @Test
    fun `reads every json entry at any depth, in a stable order`() {
        val parsed = readImportFile(
            zip(
                mapOf(
                    "nested/deeper/c.json" to oneRecipe,
                    "README.txt" to "not a recipe",
                    "a.json" to oneRecipe,
                    "nested/b.json" to oneRecipe,
                ),
            ),
        )

        assertEquals(3, parsed.recipes.size)
        assertEquals(
            listOf("a.json", "nested/b.json", "nested/deeper/c.json"),
            parsed.recipes.map { it.source },
        )
        assertEquals(listOf(0, 1, 2), parsed.recipes.map { it.index })
    }

    @Test
    fun `refuses the whole archive when one path points outside it`() {
        val rejected = assertFailsWith<ImportRejected> {
            readImportFile(zip(mapOf("a.json" to oneRecipe, "../escaped.json" to oneRecipe)))
        }

        assertEquals(RejectionReason.TRAVERSAL, rejected.reason)
    }

    @Test
    fun `sees the escape whichever separator wrote it`() {
        assertTrue(escapesRoot("../escaped.json"))
        assertTrue(escapesRoot("nested/../../escaped.json"))
        assertTrue(escapesRoot("""..\escaped.json"""))
        assertTrue(escapesRoot("/etc/passwd"))
        assertTrue(escapesRoot("""C:\windows\x.json"""))

        assertFalse(escapesRoot("nested/deeper/a.json"))
        // A name that merely starts with dots is not a traversal.
        assertFalse(escapesRoot("..hidden/a.json"))
    }

    @Test
    fun `refuses an archive with no json in it`() {
        val rejected = assertFailsWith<ImportRejected> {
            readImportFile(zip(mapOf("README.txt" to "nothing here")))
        }

        assertEquals(RejectionReason.EMPTY, rejected.reason)
    }

    @Test
    fun `tells a zip from json by its own bytes, not its name`() {
        assertTrue(looksLikeZip(zip(mapOf("a.json" to oneRecipe))))
        assertFalse(looksLikeZip(oneRecipe.toByteArray()))
    }

    // ─── SF-007: the migration chain ────────────────────────────────────────

    @Test
    fun `runs the steps in order and returns a copy`() {
        val steps = mapOf<Int, Migration>(
            1 to { recipes -> recipes + JsonPrimitive("from 1") },
            2 to { recipes -> recipes + JsonPrimitive("from 2") },
        )

        val out = runChain(1, 3, listOf(JsonPrimitive("start")), steps)

        assertEquals(listOf("start", "from 1", "from 2"), out.map { (it as JsonPrimitive).content })
    }

    @Test
    fun `a missing step is an error rather than a silent skip`() {
        assertFailsWith<IllegalStateException> {
            runChain(1, 3, emptyList(), mapOf(1 to { recipes -> recipes }))
        }
    }

    @Test
    fun `will not migrate down`() {
        assertFailsWith<IllegalArgumentException> { runChain(3, 1, emptyList(), MIGRATIONS) }
    }
}

/** The version this build writes; the bare shapes are read as it. */
private const val RECIPE_FORMAT_VERSION_FOR_TEST = 1
