package dev.bondarenko.fujirecipes.core.store

import dev.bondarenko.fujirecipes.data.model.Recipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File

/**
 * The library, on disk. **This is the library** — there is no copy of it anywhere else.
 *
 * It replaces the snapshot cache that used to sit in front of the Worker. The difference is
 * not the file format, it is what a failure means: a cache that will not read costs one
 * request to replace, and a store that will not read is the whole library. So every path in
 * here distinguishes "there is nothing stored yet" from "something is stored and could not be
 * read", and only the first one is allowed to look like an empty library.
 *
 * One file rather than a database, for the reason `architecture.md` §4 gave the cache: there
 * is nothing to query. The whole library is loaded at once, filtered in memory, and a
 * personal recipe collection is measured in tens.
 *
 * **What it stores is recipe objects, not this build's model of one.** `Recipe.toJson`
 * puts back the top-level keys this build does not know (`Recipe.extra`), so a file written
 * by the web client and read back here survives a round trip whole.
 */
class LibraryStore(private val file: File) {

    /**
     * Read the library.
     *
     * Returns [Loaded] with an empty list for a device that has never stored anything —
     * a new install, and a legitimately empty library. [Unreadable] is a file that exists
     * and did not parse, which must never be shown as "no recipes yet".
     */
    suspend fun read(): StoreRead = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext StoreRead.Loaded(StoredLibrary())

        runCatching {
            val root = json.parseToJsonElement(file.readText()).jsonObject

            val version = root["libraryVersion"]?.jsonPrimitive?.content?.toIntOrNull()
            // A file from a future build. Refusing it is the only honest answer: reading it
            // with today's rules and then saving over it would discard whatever the newer
            // version added.
            if (version == null || version > LIBRARY_VERSION) {
                return@runCatching StoreRead.Unreadable(
                    if (version == null) {
                        "The library file on this device does not say what format it is in."
                    } else {
                        "The library file on this device is in format $version, which this " +
                            "version of the app does not understand."
                    },
                )
            }

            StoreRead.Loaded(
                StoredLibrary(
                    updatedAt = root["updatedAt"]?.jsonPrimitive?.content.orEmpty(),
                    recipes = root["recipes"]?.jsonArray.orEmpty()
                        .map { Recipe.fromJson(it.jsonObject) },
                ),
            )
        }.getOrElse { error ->
            StoreRead.Unreadable(error.message ?: "The stored library could not be read.")
        }
    }

    /**
     * Replace the stored library.
     *
     * The whole file every time. A personal library is small enough that rewriting it costs
     * nothing measurable, and a partial write is a class of corruption not worth being able
     * to have.
     */
    suspend fun write(recipes: List<Recipe>, updatedAt: String): StoreWrite =
        withContext(Dispatchers.IO) {
            runCatching {
                val envelope = buildJsonObject {
                    put("libraryVersion", LIBRARY_VERSION)
                    put("updatedAt", updatedAt)
                    put("recipes", JsonArray(recipes.map { it.toJson() }))
                }

                file.parentFile?.mkdirs()
                // Written beside and moved into place. A process killed mid-write would
                // otherwise truncate the file, and unlike the cache this replaced there is
                // nowhere to fetch the library back from.
                val temporary = File(file.parentFile, file.name + ".tmp")
                temporary.writeText(json.encodeToString(JsonObject.serializer(), envelope))
                if (!temporary.renameTo(file)) {
                    temporary.copyTo(file, overwrite = true)
                    temporary.delete()
                }
                StoreWrite.Ok
            }.getOrElse { error ->
                StoreWrite.Failed(error.message ?: "The library could not be saved to this device.")
            }
        }

    companion object {
        const val LIBRARY_VERSION = 1
        const val FILE_NAME = "library.json"

        private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    }
}

/** What was stored: the recipes, and when the file was last written. */
data class StoredLibrary(
    val updatedAt: String = "",
    val recipes: List<Recipe> = emptyList(),
)

sealed interface StoreRead {
    data class Loaded(val library: StoredLibrary) : StoreRead

    /** A file is there and could not be read. Never rendered as an empty library. */
    data class Unreadable(val message: String) : StoreRead
}

sealed interface StoreWrite {
    data object Ok : StoreWrite
    data class Failed(val message: String) : StoreWrite
}
