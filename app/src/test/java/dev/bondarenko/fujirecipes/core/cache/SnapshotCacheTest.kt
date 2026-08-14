package dev.bondarenko.fujirecipes.core.cache

import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The snapshot on disk — FEAT-001 T-10.
 *
 * Envelope: `specs/features/FEAT-001-recipe-list/02-schema.json`.
 */
class SnapshotCacheTest {

    private val directory: File = Files.createTempDirectory("snapshot-test").toFile()
    private val file = File(directory, SnapshotCache.FILE_NAME)
    private val cache = SnapshotCache(file)

    private val baseUrl = "https://recipes.example.com"

    private val body = """
        {"recipes":[
          {"id":"a","name":"Kodachrome 64","rating":5,"tags":["street"],"sortKey":1000,
           "settings":{"filmSimulation":"classic-chrome"},"stackId":"future-field"},
          {"id":"b","name":"Acros Night","rating":0,"tags":[],"sortKey":2000,
           "settings":{"filmSimulation":"acros-r"}}
        ]}
    """.trimIndent()

    @AfterTest
    fun cleanUp() {
        directory.deleteRecursively()
    }

    @Test
    fun `a written snapshot reads back`() = runTest {
        cache.write(body, baseUrl, "2026-08-14T09:31:04.221Z")

        val snapshot = assertNotNull(cache.read(baseUrl))
        assertEquals(2, snapshot.recipes.size)
        assertEquals("Kodachrome 64", snapshot.recipes.first().name)
        assertEquals("2026-08-14T09:31:04.221Z", snapshot.fetchedAt)
    }

    @Test
    fun `order is preserved, because the client's sorts fall back to it`() = runTest {
        cache.write(body, baseUrl, "2026-08-14T09:31:04.221Z")

        val snapshot = assertNotNull(cache.read(baseUrl))
        assertEquals(listOf("a", "b"), snapshot.recipes.map { it.id })
    }

    @Test
    fun `a field this build does not model survives the cache`() = runTest {
        cache.write(body, baseUrl, "2026-08-14T09:31:04.221Z")

        val snapshot = assertNotNull(cache.read(baseUrl))
        assertTrue(snapshot.recipes.first().extra.containsKey("stackId"))
    }

    @Test
    fun `a snapshot from another deployment is not served`() = runTest {
        cache.write(body, baseUrl, "2026-08-14T09:31:04.221Z")

        // Pointing the app at a second instance must not show the first one's library.
        assertNull(cache.read("https://someone-elses.example.com"))
    }

    @Test
    fun `a snapshot with an unrecognised version is discarded`() = runTest {
        file.parentFile.mkdirs()
        file.writeText("""{"snapshotVersion":99,"fetchedAt":"x","baseUrl":"$baseUrl","recipes":[]}""")

        // A cache costs one request to replace, so an envelope from the future is dropped
        // rather than guessed at.
        assertNull(cache.read(baseUrl))
    }

    @Test
    fun `a truncated file reads as no snapshot rather than throwing`() = runTest {
        file.parentFile.mkdirs()
        file.writeText("""{"snapshotVersion":1,"recipes":[{"id""")

        assertNull(cache.read(baseUrl))
    }

    @Test
    fun `no file is simply no snapshot`() = runTest {
        assertNull(cache.read(baseUrl))
    }

    @Test
    fun `a failed write leaves the previous snapshot intact`() = runTest {
        cache.write(body, baseUrl, "2026-08-14T09:31:04.221Z")

        // A body with no `recipes` array is not a library, and must not replace one.
        cache.write("""{"error":"forbidden"}""", baseUrl, "2026-08-14T10:00:00.000Z")

        val snapshot = assertNotNull(cache.read(baseUrl))
        assertEquals(2, snapshot.recipes.size)
        assertEquals("2026-08-14T09:31:04.221Z", snapshot.fetchedAt)
    }
}
