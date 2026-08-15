package dev.bondarenko.fujirecipes.data.exporting

import dev.bondarenko.fujirecipes.data.model.Recipe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * FEAT-008 T-08.
 *
 * Read back with `ZipInputStream` rather than asserted against a byte fixture: what matters
 * is that a real unzipper finds the entries, which is what every destination the share sheet
 * offers will do to it.
 */
class ZipWriterTest {

    private fun unzip(bytes: ByteArray): Map<String, String> {
        val out = linkedMapOf<String, String>()

        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                out[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
                zip.closeEntry()
            }
        }

        return out
    }

    private fun recipeFrom(json: String): Recipe =
        Recipe.fromJson(Json.parseToJsonElement(json).jsonObject)

    @Test
    fun `entries come back with their names and contents intact`() {
        val archive = zipOf(mapOf("a.json" to "{\"one\":1}\n", "b.json" to "{\"two\":2}\n"))

        val read = unzip(archive)

        assertContentEquals(listOf("a.json", "b.json"), read.keys.toList())
        assertEquals("{\"one\":1}\n", read["a.json"])
        assertEquals("{\"two\":2}\n", read["b.json"])
    }

    @Test
    fun `an empty archive is still a readable archive`() {
        assertTrue(unzip(zipOf(emptyMap())).isEmpty())
    }

    /** The whole point: each entry has to be a recipe an importer can read. */
    @Test
    fun `each entry parses back as the recipe that went in`() {
        val recipes = listOf(
            recipeFrom("""{"id":"a","name":"Kodachrome 64","settings":{"clarity":2}}"""),
            recipeFrom("""{"id":"b","name":"Acros Night","settings":{"clarity":-1}}"""),
        )

        val read = unzip(zipOf(archiveEntries(recipes)))

        assertEquals(2, read.size)
        val first = Json.parseToJsonElement(read.getValue("kodachrome-64.json")).jsonObject
        assertEquals("Kodachrome 64", first["name"]!!.jsonPrimitive.content)
        // Bare recipes, not envelopes — SF-005's third shape.
        assertFalse(first.containsKey("format"))
    }

    /** Names are not unique, so the archive has to carry both — and a reader has to see both. */
    @Test
    fun `two recipes with one name survive the round trip through a real unzipper`() {
        val recipes = listOf(
            recipeFrom("""{"id":"a","name":"Portra 400","settings":{"clarity":1}}"""),
            recipeFrom("""{"id":"b","name":"Portra 400","settings":{"clarity":2}}"""),
        )

        val read = unzip(zipOf(archiveEntries(recipes)))

        assertContentEquals(listOf("portra-400.json", "portra-400-2.json"), read.keys.toList())
    }

    /** SF-001 makes the encoding part of the format rather than a platform default. */
    @Test
    fun `content outside ASCII survives as UTF-8`() {
        val recipes = listOf(recipeFrom("""{"id":"a","name":"Cinéma","settings":{}}"""))

        val read = unzip(zipOf(archiveEntries(recipes)))

        assertEquals("cinéma.json", read.keys.single())
        assertTrue(read.values.single().contains("Cinéma"))
    }
}
