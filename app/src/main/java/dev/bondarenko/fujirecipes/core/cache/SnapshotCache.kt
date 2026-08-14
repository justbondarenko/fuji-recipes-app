package dev.bondarenko.fujirecipes.core.cache

import dev.bondarenko.fujirecipes.data.model.Recipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File

/**
 * The library, on disk — FEAT-001 T-10.
 *
 * Shape: `specs/features/FEAT-001-recipe-list/02-schema.json`.
 *
 * One file holding one response. Not a database, because there is nothing to query: the
 * whole library is loaded at once, filtered in memory, and the server owns every write
 * (`steering/architecture.md` §4).
 *
 * **What it stores is the response text, unaltered.** Re-serialising parsed models would
 * drop any field this build does not know about, and `data-model.md` §1 requires those to
 * survive a round trip through an older client. So the cache is closer to a downloaded file
 * than to a model store.
 */
class SnapshotCache(private val file: File) {

    /**
     * Read the snapshot, or nothing.
     *
     * Every failure is the same answer — no snapshot — and deliberately so. This is a
     * cache: an unreadable, truncated, or foreign one costs exactly one request to replace,
     * and a caller made to distinguish "corrupt" from "absent" would have the same handling
     * for both.
     */
    suspend fun read(expectedBaseUrl: String): Snapshot? = withContext(Dispatchers.IO) {
        runCatching {
            if (!file.exists()) return@runCatching null

            val root = json.parseToJsonElement(file.readText()).jsonObject

            val version = root["snapshotVersion"]?.jsonPrimitive?.content?.toIntOrNull()
            if (version != SNAPSHOT_VERSION) return@runCatching null

            // A snapshot from another deployment is not stale, it is *someone else's*.
            // Showing it after the base URL changes would present one library as another.
            val baseUrl = root["baseUrl"]?.jsonPrimitive?.content
            if (baseUrl != expectedBaseUrl) return@runCatching null

            Snapshot(
                fetchedAt = root["fetchedAt"]?.jsonPrimitive?.content.orEmpty(),
                baseUrl = baseUrl,
                recipes = root["recipes"]?.jsonArray.orEmpty().map { Recipe.fromJson(it.jsonObject) },
            )
        }.getOrNull()
    }

    /**
     * Store a successful response.
     *
     * [rawBody] is the exact `GET /api/recipes` text; its `recipes` array is lifted out and
     * placed in the envelope untouched.
     */
    suspend fun write(rawBody: String, baseUrl: String, fetchedAt: String): Unit =
        withContext(Dispatchers.IO) {
            runCatching {
                val recipes = json.parseToJsonElement(rawBody).jsonObject["recipes"]?.jsonArray
                    ?: return@runCatching

                val envelope = buildJsonObject {
                    put("snapshotVersion", SNAPSHOT_VERSION)
                    put("fetchedAt", fetchedAt)
                    put("baseUrl", baseUrl)
                    put("recipes", recipes)
                }

                file.parentFile?.mkdirs()
                // Written beside and moved into place: a process killed mid-write would
                // otherwise leave a truncated file, and `read` would answer "no snapshot"
                // for a library that was perfectly good a moment ago.
                val temporary = File(file.parentFile, file.name + ".tmp")
                temporary.writeText(json.encodeToString(JsonObject.serializer(), envelope))
                temporary.renameTo(file)
            }
            Unit
        }

    suspend fun clear(): Unit = withContext(Dispatchers.IO) {
        runCatching { file.delete() }
        Unit
    }

    companion object {
        const val SNAPSHOT_VERSION = 1
        const val FILE_NAME = "library-snapshot.json"

        private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    }
}

data class Snapshot(
    val fetchedAt: String,
    val baseUrl: String,
    val recipes: List<Recipe>,
)
