package dev.bondarenko.fujirecipes.core.net

import dev.bondarenko.fujirecipes.core.settings.ConnectionConfig
import dev.bondarenko.fujirecipes.data.model.Recipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * The HTTP client — FEAT-001 T-07.
 *
 * Hand-rolled over OkHttp rather than Retrofit or Ktor: the whole contract is six routes
 * (`steering/tech-stack.md` §2), and an interface plus a converter factory would be an
 * annotation layer over `Request.Builder`.
 *
 * Every response goes through [toError] on the way out, so no caller ever sees a status
 * code. That is what makes `coding-standards.md` P5 enforceable — a UI that wants to tell a
 * revoked token apart from a sleeping database has the distinction handed to it.
 */
class ApiClient(
    private val config: () -> ConnectionConfig,
    private val client: OkHttpClient = defaultClient(config),
) {

    /**
     * `GET /api/recipes` — the whole library, ordered by `sortKey` ascending.
     *
     * Returns the raw response text alongside the parsed recipes: the snapshot cache stores
     * what the server sent, byte for byte, and re-serialising the parsed models to produce
     * it would be a way to quietly drop a field this build does not know
     * (`coding-standards.md` P2).
     */
    suspend fun listRecipes(): ApiResult<RecipeListPayload> = request("/api/recipes") { body ->
        val root = json.parseToJsonElement(body).jsonObject
        val recipes = root["recipes"]?.jsonArray.orEmpty().map { Recipe.fromJson(it.jsonObject) }
        RecipeListPayload(recipes = recipes, rawBody = body)
    }

    private suspend fun <T> request(
        path: String,
        parse: (String) -> T,
    ): ApiResult<T> = withContext(Dispatchers.IO) {
        val current = config()
        if (current.baseUrl.isBlank()) {
            return@withContext ApiResult.Failure(
                ApiError.Network("No server address is configured."),
            )
        }

        val call = Request.Builder().url(current.baseUrl + path).get().build()

        try {
            client.newCall(call).execute().use { response ->
                // `string()` once: the body is a one-shot stream, and reading it twice
                // yields an empty second read rather than an error.
                val body = response.body.string()

                if (!response.isSuccessful) {
                    return@withContext ApiResult.Failure(toError(response.code, body))
                }

                try {
                    ApiResult.Success(parse(body))
                } catch (error: Exception) {
                    // A 200 whose body will not parse. Not the server's fault as far as we
                    // can tell — a captive portal answering with an HTML login page lands
                    // here, and calling that an internal server error would be wrong.
                    ApiResult.Failure(
                        ApiError.Malformed(
                            "The server's answer could not be read: ${error.message}",
                        ),
                    )
                }
            }
        } catch (error: IOException) {
            ApiResult.Failure(ApiError.Network(error.message))
        }
    }

    /**
     * A non-2xx response, as the thing that actually failed.
     *
     * Keyed on the envelope's `error` code rather than the status, because the codes are
     * what `fuji-recipes-book/specs/contracts.md` specifies and two of them share 503 —
     * `storage_unavailable` and `access_unconfigured` — with completely different remedies.
     * The status is only the fallback when there is no envelope to read, which is what a
     * proxy or the Access login page itself would return.
     */
    private fun toError(status: Int, body: String): ApiError {
        val envelope = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
        val code = envelope?.string("error")
        val message = envelope?.string("message")

        return when (code) {
            "forbidden" -> ApiError.Forbidden(message)
            "access_unconfigured" -> ApiError.AccessUnconfigured(message)
            "storage_unavailable" -> ApiError.StorageUnavailable(message)
            "not_found" -> ApiError.NotFound(message, envelope.string("id"))
            "id_exists" -> ApiError.IdExists(message, envelope.string("id"))
            "bad_request" -> ApiError.BadRequest(message)
            "internal" -> ApiError.Internal(message, envelope.string("requestId"))
            "validation_failed" -> ApiError.ValidationFailed(
                message,
                envelope["fields"]?.jsonArray.orEmpty().mapNotNull { entry ->
                    val field = runCatching { entry.jsonObject }.getOrNull() ?: return@mapNotNull null
                    FieldError(
                        path = field.string("path").orEmpty(),
                        message = field.string("message").orEmpty(),
                    )
                },
            )
            "method_not_allowed" -> ApiError.MethodNotAllowed(
                message,
                envelope["allowed"]?.jsonArray.orEmpty().mapNotNull { it.stringOrNull() },
            )

            // No envelope, or a code this build has not learned. The status still carries
            // the two facts worth acting on, so they are not thrown away.
            null -> when (status) {
                401, 403 -> ApiError.Forbidden(
                    "The server refused this request (HTTP $status) without an error body. " +
                        "This is usually Cloudflare Access rejecting the service token.",
                )
                in 500..599 -> ApiError.Unexpected(
                    "The server failed with HTTP $status.", status, null,
                )
                else -> ApiError.Unexpected("The server answered HTTP $status.", status, null)
            }

            else -> ApiError.Unexpected(message, status, code)
        }
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true; isLenient = true }

        private fun JsonObject.string(key: String): String? =
            this[key]?.stringOrNull()

        private fun kotlinx.serialization.json.JsonElement.stringOrNull(): String? =
            runCatching { jsonPrimitive.content }.getOrNull()

        fun defaultClient(config: () -> ConnectionConfig): OkHttpClient =
            OkHttpClient.Builder()
                .addInterceptor(AccessInterceptor(config))
                .build()
    }
}

/**
 * A successful list response: the models, and the bytes they came from.
 *
 * Both, because they serve different masters — the UI reads [recipes], and [rawBody] is
 * what the snapshot stores so that a field this build cannot model still comes back after a
 * restart.
 */
data class RecipeListPayload(
    val recipes: List<Recipe>,
    val rawBody: String,
)
