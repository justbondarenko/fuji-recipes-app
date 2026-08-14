package dev.bondarenko.fujirecipes.data.repo

import dev.bondarenko.fujirecipes.core.cache.SnapshotCache
import dev.bondarenko.fujirecipes.core.net.ApiClient
import dev.bondarenko.fujirecipes.core.net.ApiError
import dev.bondarenko.fujirecipes.core.settings.ConnectionConfig
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Snapshot-then-network, and what happens when the network is not there — FEAT-001 T-11.
 *
 * The rule this file exists for: **a failed refresh reports a failed refresh, it does not
 * empty the library.** An offline app that blanks itself the moment the signal drops is
 * useless exactly when it is needed (`steering/architecture.md` §4, constraint C4).
 */
class NetworkRecipeRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var directory: File
    private lateinit var cache: SnapshotCache

    private val body = """
        {"recipes":[
          {"id":"a","name":"Kodachrome 64","rating":5,"tags":["street"],"sortKey":1000,
           "settings":{"filmSimulation":"classic-chrome"}},
          {"id":"b","name":"Acros Night","rating":0,"tags":[],"sortKey":2000,
           "settings":{"filmSimulation":"acros-r"}}
        ]}
    """.trimIndent()

    @BeforeTest
    fun start() {
        server = MockWebServer()
        server.start()
        directory = Files.createTempDirectory("repo-test").toFile()
        cache = SnapshotCache(File(directory, SnapshotCache.FILE_NAME))
    }

    @AfterTest
    fun stop() {
        runCatching { server.shutdown() }
        directory.deleteRecursively()
    }

    private fun config() = ConnectionConfig(
        baseUrl = server.url("/").toString().trimEnd('/'),
        clientId = "id.access",
        clientSecret = "secret",
    )

    private fun repository(
        connection: ConnectionConfig = config(),
        now: () -> String = { "2026-08-14T09:31:04.221Z" },
    ) = NetworkRecipeRepository(
        api = ApiClient(config = { connection }),
        cache = cache,
        config = { connection },
        now = now,
    )

    private fun respondOk() {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(body),
        )
    }

    @Test
    fun `a successful refresh publishes the library and caches it`() = runTest {
        respondOk()
        val repository = repository()

        repository.refresh()

        val state = repository.library.value
        assertEquals(listOf("a", "b"), state.recipes.map { it.id })
        assertNull(state.error)
        assertTrue(state.hasLoaded)
        assertEquals(false, state.isRefreshing)
        // Cached for the next cold start.
        assertNotNull(cache.read(config().baseUrl))
    }

    @Test
    fun `with no network and a snapshot, the library still lists`() = runTest {
        cache.write(body, config().baseUrl, "2026-08-13T20:00:00.000Z")
        server.enqueue(MockResponse().apply { socketPolicy = SocketPolicy.DISCONNECT_AT_START })

        val repository = repository()
        repository.refresh()

        val state = repository.library.value
        assertEquals(2, state.recipes.size)
        assertIs<ApiError.Network>(state.error)
        // The banner needs to say *when* the copy was taken, or "cached" is meaningless.
        assertEquals("2026-08-13T20:00:00.000Z", state.servedFromSnapshotAt)
    }

    @Test
    fun `with no network and no snapshot, it is an error and not an empty library`() = runTest {
        server.enqueue(MockResponse().apply { socketPolicy = SocketPolicy.DISCONNECT_AT_START })

        val repository = repository()
        repository.refresh()

        val state = repository.library.value
        assertTrue(state.recipes.isEmpty())
        assertIs<ApiError.Network>(state.error)
        // `hasLoaded` with no recipes and an error is what lets the UI tell "nothing here"
        // apart from "could not look".
        assertTrue(state.hasLoaded)
    }

    @Test
    fun `a failed refresh does not clear an already-loaded library`() = runTest {
        respondOk()
        val repository = repository()
        repository.refresh()
        assertEquals(2, repository.library.value.recipes.size)

        // The server goes away rather than queueing a disconnect: by the second refresh the
        // connection is pooled, and OkHttp transparently retries a stale-connection failure
        // on a fresh socket — which would consume an empty queue and produce a parse error
        // instead of the network failure this test is about.
        server.shutdown()
        repository.refresh()

        val state = repository.library.value
        assertEquals(2, state.recipes.size)
        assertIs<ApiError.Network>(state.error)
    }

    @Test
    fun `a failed refresh does not overwrite a good snapshot`() = runTest {
        cache.write(body, config().baseUrl, "2026-08-13T20:00:00.000Z")
        server.enqueue(
            MockResponse().setResponseCode(503)
                .setBody("""{"error":"storage_unavailable","message":"D1 is down"}"""),
        )

        repository().refresh()

        val snapshot = assertNotNull(cache.read(config().baseUrl))
        assertEquals(2, snapshot.recipes.size)
        assertEquals("2026-08-13T20:00:00.000Z", snapshot.fetchedAt)
    }

    @Test
    fun `a refused token surfaces as forbidden, not as an empty library`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(403)
                .setBody("""{"error":"forbidden","message":"Token not accepted."}"""),
        )

        val repository = repository()
        repository.refresh()

        assertIs<ApiError.Forbidden>(repository.library.value.error)
        assertTrue(repository.library.value.recipes.isEmpty())
    }

    @Test
    fun `a successful refresh clears a previous error`() = runTest {
        server.enqueue(MockResponse().apply { socketPolicy = SocketPolicy.DISCONNECT_AT_START })
        val repository = repository()
        repository.refresh()
        assertNotNull(repository.library.value.error)

        respondOk()
        repository.refresh()

        assertNull(repository.library.value.error)
        // No longer a cached copy, so the offline banner must come down.
        assertNull(repository.library.value.servedFromSnapshotAt)
    }

    @Test
    fun `a snapshot from a different deployment is not shown`() = runTest {
        cache.write(body, "https://someone-elses.example.com", "2026-08-13T20:00:00.000Z")
        server.enqueue(MockResponse().apply { socketPolicy = SocketPolicy.DISCONNECT_AT_START })

        val repository = repository()
        repository.refresh()

        assertTrue(repository.library.value.recipes.isEmpty())
    }

    @Test
    fun `an empty library is a success, not a failure`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"recipes":[]}"""),
        )

        val repository = repository()
        repository.refresh()

        val state = repository.library.value
        assertTrue(state.recipes.isEmpty())
        assertNull(state.error)
        assertTrue(state.hasLoaded)
    }
}
