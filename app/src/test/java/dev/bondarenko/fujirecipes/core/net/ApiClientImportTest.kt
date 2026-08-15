package dev.bondarenko.fujirecipes.core.net

import dev.bondarenko.fujirecipes.core.settings.ConnectionConfig
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * `POST /api/import` — FEAT-007 T-14.
 *
 * A contract test: what goes on the wire against what `fuji-recipes-book/specs/contracts.md`
 * says, and what comes back mapped through the same error envelope every other route uses.
 */
class ApiClientImportTest {

    private lateinit var server: MockWebServer

    @BeforeTest
    fun start() {
        server = MockWebServer()
        server.start()
    }

    @AfterTest
    fun stop() {
        runCatching { server.shutdown() }
    }

    private fun client() = ApiClient(
        config = {
            ConnectionConfig(
                baseUrl = server.url("/").toString().trimEnd('/'),
                clientId = "id.access",
                clientSecret = "secret",
            )
        },
    )

    private fun body() = buildJsonObject {
        put(
            "recipes",
            JsonArray(
                listOf(
                    buildJsonObject {
                        put("name", "1970s Summer")
                        put(
                            "settings",
                            buildJsonObject { put("filmSimulation", "nostalgic-negative") },
                        )
                    },
                ),
            ),
        )
        put("resolutions", buildJsonObject { })
    }

    @Test
    fun `posts to the import route`() = runTest {
        server.enqueue(MockResponse().setBody("""{"imported":1,"skipped":0,"replaced":0}"""))

        client().importRecipes(body())

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/import", request.path)
    }

    /** P2: the server assigns ids, and a client-supplied one would be the phone inventing it. */
    @Test
    fun `sends no recipe ids`() = runTest {
        server.enqueue(MockResponse().setBody("""{"imported":1,"skipped":0,"replaced":0}"""))

        client().importRecipes(body())

        val sent = Json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
        val recipe = sent["recipes"]!!.jsonArray.single().jsonObject

        assertFalse(recipe.containsKey("id"))
        assertEquals("1970s Summer", recipe["name"]!!.jsonPrimitive.content)
    }

    @Test
    fun `carries the Access headers like every other route`() = runTest {
        server.enqueue(MockResponse().setBody("""{"imported":1,"skipped":0,"replaced":0}"""))

        client().importRecipes(body())

        val request = server.takeRequest()
        assertEquals("id.access", request.getHeader("CF-Access-Client-Id"))
        assertEquals("secret", request.getHeader("CF-Access-Client-Secret"))
    }

    @Test
    fun `reads the success envelope`() = runTest {
        server.enqueue(
            MockResponse().setBody("""{"imported":5,"skipped":2,"replaced":0,"failed":[]}"""),
        )

        val result = client().importRecipes(body())

        val success = assertIs<ApiResult.Success<ImportResult>>(result)
        assertEquals(5, success.value.imported)
        assertEquals(2, success.value.skipped)
        assertTrue(success.value.failed.isEmpty())
    }

    /** A field the server reports and this build drops is how the two start to disagree. */
    @Test
    fun `reads the failures, naming the field path the server gave`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"imported":0,"skipped":0,"replaced":0,
                   "failed":[{"index":4,"path":"settings.clarity","message":"out of range"}]}""",
            ),
        )

        val success = assertIs<ApiResult.Success<ImportResult>>(client().importRecipes(body()))

        val failure = success.value.failed.single()
        assertEquals(4, failure.index)
        assertEquals("settings.clarity", failure.path)
        assertEquals("out of range", failure.message)
    }

    @Test
    fun `an envelope missing optional counts reads as zero rather than failing`() = runTest {
        server.enqueue(MockResponse().setBody("""{"imported":3}"""))

        val success = assertIs<ApiResult.Success<ImportResult>>(client().importRecipes(body()))

        assertEquals(3, success.value.imported)
        assertEquals(0, success.value.replaced)
        assertTrue(success.value.failed.isEmpty())
    }

    // ─── Failures, through the same envelope as every other route (P5) ──────

    @Test
    fun `a validation failure comes back as ValidationFailed`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(422).setBody(
                """{"error":"validation_failed","message":"no",
                   "fields":[{"path":"settings.clarity","message":"must be between -5 and 5"}]}""",
            ),
        )

        val failure = assertIs<ApiResult.Failure>(client().importRecipes(body()))
        val error = assertIs<ApiError.ValidationFailed>(failure.error)

        assertEquals("settings.clarity", error.fields.single().path)
    }

    @Test
    fun `a refused token comes back as Forbidden rather than a generic failure`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(403).setBody("""{"error":"forbidden"}"""),
        )

        val failure = assertIs<ApiResult.Failure>(client().importRecipes(body()))
        assertIs<ApiError.Forbidden>(failure.error)
    }

    /** The import is atomic, so nothing partial has to be reported to the caller. */
    @Test
    fun `a storage failure comes back retryable`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(503).setBody("""{"error":"storage_unavailable"}"""),
        )

        val failure = assertIs<ApiResult.Failure>(client().importRecipes(body()))
        assertIs<ApiError.StorageUnavailable>(failure.error)
    }
}
