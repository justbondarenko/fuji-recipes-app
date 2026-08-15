package dev.bondarenko.fujirecipes.data.importing

import dev.bondarenko.fujirecipes.data.model.Recipe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The review's rules — `recipe-format.spec.md` SF-010 to SF-014, and the request body
 * `contracts.md` specifies.
 */
class FileReviewTest {

    private val libraryId = "3f2a5c18-6b4e-4f0a-9c21-8d7e5b3a1f60"

    private fun recipe(json: String) = Json.parseToJsonElement(json).jsonObject

    private fun parsed(vararg entries: String) =
        entries.mapIndexed { index, text -> ParsedFileRecipe(index, recipe(text)) }

    private val library = listOf(
        Recipe(
            id = libraryId,
            name = "Kodachrome 64",
            settings = recipe("""{"filmSimulation":"classic-chrome","color":2}"""),
        ),
    )

    @Test
    fun `a recipe the library has never seen is new`() {
        val rows = reviewFile(
            parsed("""{"name":"Portra","settings":{"filmSimulation":"astia"}}"""),
            library,
        )

        assertEquals(FileRowStatus.NEW, rows.single().status)
        assertEquals(emptyList(), rows.single().resolutionOptions)
    }

    @Test
    fun `an id the library already holds is a conflict with three ways out`() {
        val rows = reviewFile(
            parsed("""{"id":"$libraryId","name":"Kodachrome 64 v2","settings":{"filmSimulation":"velvia"}}"""),
            library,
        )

        val row = rows.single()
        assertEquals(FileRowStatus.CONFLICT, row.status)
        assertEquals("Kodachrome 64", row.existingName)
        assertEquals(
            listOf(Resolution.SKIP, Resolution.REPLACE, Resolution.KEEP_BOTH),
            row.resolutionOptions,
        )
    }

    @Test
    fun `the same settings under another id is a duplicate, with no replace to offer`() {
        val rows = reviewFile(
            parsed("""{"name":"A copy","settings":{"filmSimulation":"classic-chrome","color":2}}"""),
            library,
        )

        val row = rows.single()
        assertEquals(FileRowStatus.CONFIG_DUPLICATE, row.status)
        assertEquals("Kodachrome 64", row.existingName)
        assertEquals(listOf(Resolution.SKIP, Resolution.KEEP_BOTH), row.resolutionOptions)
    }

    @Test
    fun `a duplicate of an earlier row in the same file names that row`() {
        val rows = reviewFile(
            parsed(
                """{"name":"First","settings":{"filmSimulation":"astia","color":1}}""",
                """{"name":"Second","settings":{"filmSimulation":"astia","color":1}}""",
            ),
            library,
        )

        assertEquals(FileRowStatus.NEW, rows[0].status)
        assertEquals(FileRowStatus.CONFIG_DUPLICATE, rows[1].status)
        assertEquals("First", rows[1].existingName)
    }

    /** SF-011: a name collision without an id collision is a warning, and the import proceeds. */
    @Test
    fun `a shared name is a warning, not a conflict`() {
        val rows = reviewFile(
            parsed("""{"name":"kodachrome 64","settings":{"filmSimulation":"velvia"}}"""),
            library,
        )

        assertEquals(FileRowStatus.NAME_WARNING, rows.single().status)
        assertEquals(false, rows.single().needsResolution)
    }

    // ─── SF-014 ─────────────────────────────────────────────────────────────

    @Test
    fun `a recipe with no name is listed rather than fatal, and names the field`() {
        val rows = reviewFile(
            parsed(
                """{"settings":{"filmSimulation":"astia"}}""",
                """{"name":"Fine","settings":{"filmSimulation":"eterna"}}""",
            ),
            library,
        )

        assertEquals(FileRowStatus.INVALID, rows[0].status)
        assertEquals("name", rows[0].errors.single().fieldId)
        assertEquals(FileRowStatus.NEW, rows[1].status)
    }

    @Test
    fun `an out-of-range parameter fails only its own recipe, and names the path`() {
        val rows = reviewFile(
            parsed(
                """{"name":"Too clear","settings":{"filmSimulation":"astia","clarity":99}}""",
                """{"name":"Fine","settings":{"filmSimulation":"eterna"}}""",
            ),
            library,
        )

        assertEquals(FileRowStatus.INVALID, rows[0].status)
        assertEquals("clarity", rows[0].errors.single().fieldId)
        assertEquals(FileRowStatus.NEW, rows[1].status)
    }

    /** §4: missing keys are filled with their defaults on import, so their absence is not a fault. */
    @Test
    fun `a minimal recipe is valid`() {
        val rows = reviewFile(parsed("""{"name":"Minimal"}"""), library)

        assertEquals(FileRowStatus.NEW, rows.single().status)
    }

    @Test
    fun `a monochrome recipe is not failed for the colour it cannot have`() {
        val rows = reviewFile(
            parsed("""{"name":"Acros","settings":{"filmSimulation":"acros","color":null}}"""),
            library,
        )

        assertEquals(FileRowStatus.NEW, rows.single().status)
    }

    @Test
    fun `an entry that is not a recipe object is one failing row`() {
        val rows = reviewFile(
            listOf(ParsedFileRecipe(0, kotlinx.serialization.json.JsonPrimitive(42))),
            library,
        )

        assertEquals(FileRowStatus.INVALID, rows.single().status)
        assertTrue(rows.single().errors.isNotEmpty())
    }

    // ─── The request body ───────────────────────────────────────────────────

    @Test
    fun `sends the valid recipes untouched, unknown keys and all`() {
        val rows = reviewFile(
            parsed(
                """{"name":"Keep me","settings":{"filmSimulation":"astia"},"futureField":"kept"}""",
                """{"settings":{"filmSimulation":"astia"}}""",
            ),
            library,
        )

        val body = fileImportBody(rows, emptyMap())
        val recipes = body["recipes"] as JsonArray

        assertEquals(1, recipes.size)
        assertEquals("kept", (recipes[0] as JsonObject)["futureField"]?.let { it.toString().trim('"') })
        assertEquals(JsonObject(emptyMap()), body["resolutions"])
    }

    @Test
    fun `a resolved conflict travels as a resolution keyed by its id`() {
        val rows = reviewFile(
            parsed("""{"id":"$libraryId","name":"Kodachrome 64","settings":{"filmSimulation":"velvia"}}"""),
            library,
        )

        val body = fileImportBody(rows, mapOf(0 to Resolution.REPLACE))

        assertEquals(1, (body["recipes"] as JsonArray).size)
        assertEquals(
            """{"$libraryId":"replace"}""",
            body["resolutions"].toString(),
        )
    }

    /** The reason skip is honoured here and not left to the server: a duplicate has no id to key. */
    @Test
    fun `a skipped duplicate is dropped rather than sent`() {
        val rows = reviewFile(
            parsed("""{"name":"A copy","settings":{"filmSimulation":"classic-chrome","color":2}}"""),
            library,
        )

        val body = fileImportBody(rows, mapOf(0 to Resolution.SKIP))

        assertEquals(0, (body["recipes"] as JsonArray).size)
    }

    @Test
    fun `a skipped id conflict is still sent, so the server can report it`() {
        val rows = reviewFile(
            parsed("""{"id":"$libraryId","name":"Kodachrome 64","settings":{"filmSimulation":"velvia"}}"""),
            library,
        )

        val body = fileImportBody(rows, mapOf(0 to Resolution.SKIP))

        assertEquals(1, (body["recipes"] as JsonArray).size)
        assertEquals("""{"$libraryId":"skip"}""", body["resolutions"].toString())
    }

    @Test
    fun `an undecided conflict is not sent, so the server never has to guess`() {
        val rows = reviewFile(
            parsed("""{"id":"$libraryId","name":"Kodachrome 64","settings":{"filmSimulation":"velvia"}}"""),
            library,
        )

        assertEquals(0, ((fileImportBody(rows, emptyMap()))["recipes"] as JsonArray).size)
    }

    @Test
    fun `the summary counts what the screen has to say`() {
        val rows = reviewFile(
            parsed(
                """{"name":"New one","settings":{"filmSimulation":"astia"}}""",
                """{"id":"$libraryId","name":"Clash","settings":{"filmSimulation":"velvia"}}""",
                """{"name":"kodachrome 64","settings":{"filmSimulation":"eterna"}}""",
                """{"settings":{"filmSimulation":"astia"}}""",
            ),
            library,
        )

        val undecided = summarise(rows, emptyMap())
        assertEquals(4, undecided.total)
        assertEquals(1, undecided.new)
        assertEquals(1, undecided.conflicts)
        assertEquals(1, undecided.warnings)
        assertEquals(1, undecided.invalid)
        assertEquals(1, undecided.undecided)
        assertEquals(2, undecided.importing)

        val decided = summarise(rows, mapOf(1 to Resolution.REPLACE))
        assertEquals(0, decided.undecided)
        assertEquals(3, decided.importing)
    }

    @Test
    fun `a row remembers the archive entry it came from`() {
        val rows = reviewFile(
            listOf(ParsedFileRecipe(0, recipe("""{"name":"From a zip"}"""), "nested/a.json")),
            library,
        )

        assertEquals("nested/a.json", rows.single().source)
        assertNull(rows.single().id)
    }
}
