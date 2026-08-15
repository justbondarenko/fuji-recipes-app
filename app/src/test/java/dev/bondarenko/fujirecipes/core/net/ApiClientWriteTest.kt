package dev.bondarenko.fujirecipes.core.net

import dev.bondarenko.fujirecipes.core.settings.ConnectionConfig
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * The write path — FEAT-003 T-02.
 *
 * The assertion that matters most is the last group: a PATCH carries **only** what changed.
 * That is the contract, and it is also what stops an edit from destroying a field this build
 * does not model — a key that is never sent is never overwritten.
 */
class ApiClientWriteTest {

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

    private fun config() = ConnectionConfig(
        baseUrl = server.url("/").toString().trimEnd('/'),
        clientId = "id.access",
        clientSecret = "secret",
    )

    private fun client() = ApiClient(
        config = ::config,
        client = OkHttpClient.Builder().addInterceptor(AccessInterceptor(::config)).build(),
    )

    private fun respond(code: Int, body: String = "") {
        server.enqueue(
            MockResponse().setResponseCode(code)
                .setHeader("Content-Type", "application/json")
                .setBody(body),
        )
    }

    private val stored = """
        {"id":"new-id","name":"Kodachrome 64","notes":"","rating":0,"tags":[],
         "sortKey":3000,"settings":{"filmSimulation":"classic-chrome"},
         "createdAt":"2026-08-14T00:00:00.000Z","updatedAt":"2026-08-14T00:00:00.000Z"}
    """.trimIndent()

    private fun body(build: JsonObject.() -> Unit = {}) = buildJsonObject {
        put("name", "Kodachrome 64")
    }

    // --- create ---

    @Test
    fun `a create posts to the collection and returns the stored recipe`() = runTest {
        respond(201, stored)

        val result = assertIs<ApiResult.Success<*>>(client().createRecipe(body()))
        val request = server.takeRequest()

        assertEquals("POST", request.method)
        assertEquals("/api/recipes", request.path)
        assertEquals("new-id", (result.value as dev.bondarenko.fujirecipes.data.model.Recipe).id)
    }

    @Test
    fun `a create sends the access token like every other request`() = runTest {
        respond(201, stored)
        client().createRecipe(body())

        val request = server.takeRequest()
        assertEquals("id.access", request.getHeader(AccessInterceptor.CLIENT_ID_HEADER))
    }

    @Test
    fun `a create carries a JSON body`() = runTest {
        respond(201, stored)
        client().createRecipe(body())

        val request = server.takeRequest()
        assertTrue(request.getHeader("Content-Type").orEmpty().startsWith("application/json"))
        assertEquals(
            "Kodachrome 64",
            Json.parseToJsonElement(request.body.readUtf8()).jsonObject["name"]
                ?.jsonPrimitive?.content,
        )
    }

    @Test
    fun `a duplicate id comes back as id_exists, not as a generic failure`() = runTest {
        respond(409, """{"error":"id_exists","message":"Already used.","id":"abc"}""")

        val failure = assertIs<ApiResult.Failure>(client().createRecipe(body()))
        assertEquals("abc", assertIs<ApiError.IdExists>(failure.error).id)
    }

    // --- update ---

    @Test
    fun `an update patches the item route`() = runTest {
        respond(200, stored)
        client().updateRecipe("abc", buildJsonObject { put("rating", 4) })

        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/recipes/abc", request.path)
    }

    @Test
    fun `an update sends only the keys it was given`() = runTest {
        // The whole point of a partial update: keys that are never sent are never
        // overwritten, so a setting this build does not model survives an edit.
        respond(200, stored)
        client().updateRecipe("abc", buildJsonObject { put("rating", 4) })

        val sent = Json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
        assertEquals(setOf("rating"), sent.keys)
    }

    @Test
    fun `a rejected value comes back with the field path the server named`() = runTest {
        respond(
            422,
            """{"error":"validation_failed","message":"Invalid.","fields":[{"path":"settings.clarity","message":"must be between -5 and 5"}]}""",
        )

        val failure = assertIs<ApiResult.Failure>(
            client().updateRecipe("abc", buildJsonObject { put("rating", 4) }),
        )
        val error = assertIs<ApiError.ValidationFailed>(failure.error)
        assertEquals("settings.clarity", error.fields.single().path)
    }

    @Test
    fun `a refused token on a write is credentials, not validation`() = runTest {
        respond(403, """{"error":"forbidden","message":"Token not accepted."}""")

        val failure = assertIs<ApiResult.Failure>(client().createRecipe(body()))
        assertIs<ApiError.Forbidden>(failure.error)
    }

    @Test
    fun `a save with no connection is a network failure`() = runTest {
        server.enqueue(MockResponse().apply { socketPolicy = SocketPolicy.DISCONNECT_AT_START })

        val failure = assertIs<ApiResult.Failure>(client().createRecipe(body()))
        assertIs<ApiError.Network>(failure.error)
    }

    // --- delete ---

    @Test
    fun `a delete has no body and succeeds on 204`() = runTest {
        respond(204)

        assertIs<ApiResult.Success<*>>(client().deleteRecipe("abc"))
        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/recipes/abc", request.path)
        assertEquals(0, request.body.size)
    }

    @Test
    fun `a delete of something already gone is still a success`() = runTest {
        // The contract makes this idempotent, and treating it as an error would make a
        // double-tap look like a failure.
        respond(204)
        assertIs<ApiResult.Success<*>>(client().deleteRecipe("gone"))
    }
}
