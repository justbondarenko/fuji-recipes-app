package dev.bondarenko.fujirecipes.core.net

import dev.bondarenko.fujirecipes.core.settings.ConnectionConfig
import kotlinx.coroutines.test.runTest
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
 * `ApiClient` against the contract's envelopes — FEAT-001 T-07.
 *
 * One case per branch of `toError`. The point is not that the client can parse JSON, it is
 * that the failures stay *distinguishable*: `coding-standards.md` P5 says a 403, a sleeping
 * database and a socket timeout have three different remedies, and this is where that
 * becomes checkable.
 *
 * Envelope shapes are taken from `fuji-recipes-book/src/server/utils/errors.ts`.
 */
class ApiClientTest {

    private lateinit var server: MockWebServer

    @BeforeTest
    fun start() {
        server = MockWebServer()
        server.start()
    }

    @AfterTest
    fun stop() {
        server.shutdown()
    }

    private fun clientFor(config: ConnectionConfig = configured()): ApiClient =
        ApiClient(
            config = { config },
            client = OkHttpClient.Builder()
                .addInterceptor(AccessInterceptor { config })
                .build(),
        )

    private fun configured() = ConnectionConfig(
        baseUrl = server.url("/").toString().trimEnd('/'),
        clientId = "token-id.access",
        clientSecret = "token-secret",
    )

    private fun respond(code: Int, body: String) {
        server.enqueue(
            MockResponse().setResponseCode(code)
                .setHeader("Content-Type", "application/json")
                .setBody(body),
        )
    }

    @Test
    fun `a successful list returns recipes and the raw body`() = runTest {
        val body = """
            {"recipes":[
              {"id":"a","name":"Kodachrome 64","rating":5,"tags":["street"],"sortKey":1000,
               "settings":{"filmSimulation":"classic-chrome"},
               "createdAt":"2026-07-02T18:04:11.000Z","updatedAt":"2026-08-12T07:22:39.512Z",
               "lastWrittenSlot":3,"lastWrittenAt":"2026-08-12T07:25:02.104Z"}
            ]}
        """.trimIndent()
        respond(200, body)

        val result = assertIs<ApiResult.Success<RecipeListPayload>>(clientFor().listRecipes())

        assertEquals(1, result.value.recipes.size)
        assertEquals("Kodachrome 64", result.value.recipes.first().name)
        assertEquals("classic-chrome", result.value.recipes.first().filmSimulationId)
        // The bytes, not a re-serialisation — this is what the snapshot stores.
        assertEquals(body, result.value.rawBody)
    }

    @Test
    fun `the access service token is sent on every request`() = runTest {
        respond(200, """{"recipes":[]}""")

        clientFor().listRecipes()

        val request = server.takeRequest()
        assertEquals("token-id.access", request.getHeader(AccessInterceptor.CLIENT_ID_HEADER))
        assertEquals("token-secret", request.getHeader(AccessInterceptor.CLIENT_SECRET_HEADER))
    }

    @Test
    fun `no headers are sent when the token is not configured`() = runTest {
        respond(200, """{"recipes":[]}""")

        clientFor(configured().copy(clientSecret = "")).listRecipes()

        val request = server.takeRequest()
        // Half a credential is not a partial credential, it is an invalid one.
        assertEquals(null, request.getHeader(AccessInterceptor.CLIENT_ID_HEADER))
        assertEquals(null, request.getHeader(AccessInterceptor.CLIENT_SECRET_HEADER))
    }

    @Test
    fun `403 forbidden is the credentials, and says so`() = runTest {
        respond(403, """{"error":"forbidden","message":"This token was not accepted."}""")

        val failure = assertIs<ApiResult.Failure>(clientFor().listRecipes())
        val error = assertIs<ApiError.Forbidden>(failure.error)
        assertEquals("This token was not accepted.", error.message)
    }

    @Test
    fun `access_unconfigured is the server's gate, never the database`() = runTest {
        respond(
            503,
            """{"error":"access_unconfigured","message":"This deployment has no access gate: CF_ACCESS_POLICY_AUD is not set."}""",
        )

        val failure = assertIs<ApiResult.Failure>(clientFor().listRecipes())
        val error = assertIs<ApiError.AccessUnconfigured>(failure.error)
        // The missing variable is inside the server's prose, so the UI must show it.
        assertTrue(error.message!!.contains("CF_ACCESS_POLICY_AUD"))
    }

    @Test
    fun `storage_unavailable shares 503 with access_unconfigured and stays distinct`() = runTest {
        respond(503, """{"error":"storage_unavailable","message":"Storage is unreachable: D1"}""")

        val failure = assertIs<ApiResult.Failure>(clientFor().listRecipes())
        assertIs<ApiError.StorageUnavailable>(failure.error)
    }

    @Test
    fun `404 carries the id that was missing`() = runTest {
        respond(404, """{"error":"not_found","message":"No such recipe.","id":"abc"}""")

        val failure = assertIs<ApiResult.Failure>(clientFor().listRecipes())
        assertEquals("abc", assertIs<ApiError.NotFound>(failure.error).id)
    }

    @Test
    fun `422 carries the field paths`() = runTest {
        respond(
            422,
            """{"error":"validation_failed","message":"Invalid.","fields":[{"path":"settings.clarity","message":"must be between -5 and 5"}]}""",
        )

        val failure = assertIs<ApiResult.Failure>(clientFor().listRecipes())
        val error = assertIs<ApiError.ValidationFailed>(failure.error)
        assertEquals("settings.clarity", error.fields.single().path)
    }

    @Test
    fun `500 carries the request id so a log line can be found`() = runTest {
        respond(500, """{"error":"internal","message":"Boom.","requestId":"req-42"}""")

        val failure = assertIs<ApiResult.Failure>(clientFor().listRecipes())
        assertEquals("req-42", assertIs<ApiError.Internal>(failure.error).requestId)
    }

    @Test
    fun `a refusal with no envelope is still reported as credentials`() = runTest {
        // What Cloudflare Access itself returns when it intercepts before the Worker runs:
        // an HTML login page, not the API's envelope.
        respond(403, "<html><body>Sign in</body></html>")

        val failure = assertIs<ApiResult.Failure>(clientFor().listRecipes())
        assertIs<ApiError.Forbidden>(failure.error)
    }

    @Test
    fun `an unparseable 200 is malformed, not an internal server error`() = runTest {
        // A captive portal answering a JSON request with a login page.
        respond(200, "<html>Wi-Fi sign-in</html>")

        val failure = assertIs<ApiResult.Failure>(clientFor().listRecipes())
        assertIs<ApiError.Malformed>(failure.error)
    }

    @Test
    fun `a dropped connection is a network failure, not a server failure`() = runTest {
        server.enqueue(MockResponse().apply { socketPolicy = SocketPolicy.DISCONNECT_AT_START })

        val failure = assertIs<ApiResult.Failure>(clientFor().listRecipes())
        assertIs<ApiError.Network>(failure.error)
    }

    @Test
    fun `an unknown error code keeps the status and the code`() = runTest {
        respond(418, """{"error":"teapot","message":"Short and stout."}""")

        val failure = assertIs<ApiResult.Failure>(clientFor().listRecipes())
        val error = assertIs<ApiError.Unexpected>(failure.error)
        assertEquals(418, error.status)
        assertEquals("teapot", error.code)
    }

    @Test
    fun `no request is attempted without a base url`() = runTest {
        val failure = assertIs<ApiResult.Failure>(
            clientFor(ConnectionConfig()).listRecipes(),
        )
        assertIs<ApiError.Network>(failure.error)
        assertEquals(0, server.requestCount)
    }
}
