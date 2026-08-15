package dev.bondarenko.fujirecipes.data.model

import dev.bondarenko.fujirecipes.data.library.ViewableRecipe
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * One recipe, as it is stored and as the export files carry it — FEAT-001 T-03.
 *
 * Canonical shape: `fuji-recipes-book/specs/shared/recipe.schema.json`. This is not a
 * redefinition of it, it is the subset this client reads, plus a rule for everything else.
 *
 * **The rule for everything else is that it survives.** `data-model.md` §1 gives the
 * `recipes` table an `extra` column precisely so a property written by a newer client is
 * not dropped by an older one, and an Android model with a fixed field list would undo that
 * guarantee on every read-modify-write. So:
 *
 * - `settings` is a raw [JsonObject]. FEAT-001 reads exactly one key out of it; FEAT-002
 *   will interpret the rest. Nothing is parsed into a typed settings class, because a typed
 *   class is a list of the fields we know about today.
 * - [extra] collects unrecognised **top-level** keys, and [toJson] puts them back.
 *
 * **`lastWrittenSlot` and `lastWrittenAt` are deliberately not declared here.** This client
 * does not record, display or reason about when a recipe went to a camera — the owner's
 * decision, and the web client remains free to keep doing so. Leaving them out of the known
 * keys is what makes that true *and* safe: they fall into [extra] like any other field this
 * build does not interpret, so they survive a round trip untouched rather than being
 * dropped. Not tracking something is not the same as destroying it.
 *
 * Timestamps stay strings. They are ISO-8601 UTC with milliseconds on the wire, they are
 * only ever compared and formatted, and parsing them into a date type here would mean
 * re-serialising them on the way out — which is a way to change them.
 */
@Serializable
data class Recipe(
    val id: String,
    override val name: String,
    val notes: String = "",
    override val rating: Int = 0,
    override val tags: List<String> = emptyList(),
    override val sortKey: Double = 0.0,
    val settings: JsonObject = JsonObject(emptyMap()),
    override val createdAt: String = "",
    override val updatedAt: String = "",

    /**
     * Top-level keys this build does not know, carried verbatim.
     *
     * `@Transient` because it is not a wire field — it *is* the wire fields that have no
     * home. [fromJson] fills it and [toJson] spreads it back out, so a round trip through
     * this class is lossless.
     */
    @Transient val extra: JsonObject = JsonObject(emptyMap()),
) : ViewableRecipe {
    /** The film simulation id, or null when `settings` does not carry a usable one. */
    override val filmSimulationId: String?
        get() = settings["filmSimulation"]?.let {
            runCatching { it.jsonPrimitive.content }.getOrNull()
        }

    /**
     * Re-serialise, unknown keys included.
     *
     * [extra] goes down first so the known fields win: a stored unknown key that happens to
     * be named `name` cannot rewrite the real one. Same precedence as the web client's
     * `rowToEntity`, and for the same reason.
     */
    fun toJson(): JsonObject {
        val known = json.encodeToJsonElement(serializer(), this) as JsonObject
        return if (extra.isEmpty()) known else JsonObject(extra + known)
    }

    companion object {
        /** Every key this class declares. Anything else on the wire is [extra]. */
        private val KNOWN_KEYS = setOf(
            "id", "name", "notes", "rating", "tags", "sortKey", "settings",
            "createdAt", "updatedAt",
        )

        val json: Json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = true
        }

        fun fromJson(element: JsonObject): Recipe =
            json.decodeFromJsonElement(serializer(), element)
                .copy(extra = JsonObject(element.filterKeys { it !in KNOWN_KEYS }))
    }
}

