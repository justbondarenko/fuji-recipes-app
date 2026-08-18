package dev.bondarenko.fujirecipes.data.exporting

import dev.bondarenko.fujirecipes.data.model.Recipe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Building an export file, per `recipe-format.spec.md` §2, §3 and §4.
 *
 * **Transcribed from** `fuji-recipes-book/shared/format/export.ts` at commit `0c17106`
 * (`coding-standards.md` P3). That spec is **binding** and shared with the web client: a file
 * written here must import cleanly there, and vice versa.
 *
 * This is the only place a recipe becomes a file. Pure — no `android.*`, no I/O — so the
 * whole format can be tested against the spec's own example rather than against our output.
 *
 * **Nothing here validates.** Export is a projection of what is already stored, and a stored
 * recipe has already been through the field rules. Refusing to export a recipe that
 * somehow no longer validates would take away the backup at exactly the moment it is most
 * wanted.
 *
 * **And nothing here re-serialises a value.** The reference canonicalises numbers on the way
 * out — half-steps, thirds to three decimals — because its recipes can come from a form. Ours
 * come from the server already validated, and `settings` is held as a raw [JsonObject], so
 * every value is copied exactly as it arrived (`coding-standards.md` P2: "it never repairs a
 * value it received"). That is also what makes SF-017 free.
 */

const val RECIPE_FORMAT_ID = "fuji-recipe"
const val RECIPE_FORMAT_VERSION = 1

/**
 * SF-008 — local bookkeeping that never leaves, plus one column that no longer exists.
 *
 * `sortKey` is this library's order and means nothing in another one; the importer assigns
 * fresh keys. `lastWrittenSlot` and `lastWrittenAt` describe some phone's last camera write.
 * `sensorGeneration` was dropped as a column in D1 migration 0002.
 */
val EXPORT_EXCLUDED = setOf("sortKey", "lastWrittenSlot", "lastWrittenAt", "sensorGeneration")

/** §3's key order for a recipe object. */
val RECIPE_KEY_ORDER = listOf(
    "id",
    "name",
    "notes",
    "rating",
    "tags",
    "images",
    "settings",
    "createdAt",
    "updatedAt",
)

/**
 * §3's order inside `settings` — **explicit, and deliberately not derived from
 * `RecipeFields`.**
 *
 * The reference derived it from its field source, which was true until a UI change moved
 * `dRangePriority` into the recommendations group and silently reordered every exported file.
 * Its conformance suite caught it, which is exactly what that suite is for: a **display**
 * grouping and a **serialisation** order are different concerns that happened to coincide,
 * and only one of them is a compatibility promise.
 *
 * A test asserts every settings field the app knows appears here, so a new field cannot be
 * quietly left out — and if one ever is, the unknown-key pass below exports it anyway
 * (SF-017).
 */
val SETTINGS_KEY_ORDER = listOf(
    "filmSimulation",
    "dynamicRange",
    "dRangePriority",
    "highlightTone",
    "shadowTone",
    "color",
    "sharpness",
    "highIsoNR",
    "clarity",
    "grainEffect",
    "grainSize",
    "colorChromeEffect",
    "colorChromeFxBlue",
    "whiteBalance",
    "colorTemperature",
    "wbShiftRed",
    "wbShiftBlue",
    "monochromaticColorWc",
    "monochromaticColorMg",
    "exposureCompensation",
    "isoMin",
    "isoMax",
)

/**
 * Two-space indent and a trailing newline.
 *
 * §4 does not say, and says why it pins a key order at all: readable diffs of two backups. A
 * single-line file defeats that, and a file without a final newline is the one thing every
 * text tool complains about. SF-001's "UTF-8, no BOM" needs nothing done — Kotlin strings
 * carry no BOM and everything downstream writes UTF-8.
 */
private val json = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
    encodeDefaults = true
}

/**
 * One recipe as it appears in a file.
 *
 * - SF-008's keys are dropped.
 * - Documented keys come first, in §3's order; **unknown keys follow in their own order**,
 *   because SF-017 requires a newer implementation's fields to survive a round trip through
 *   this one. Copied verbatim: this build has no idea what they mean, so normalising them
 *   would be guessing.
 * - A key the stored recipe does not have is **left out** rather than filled with its
 *   default. Defaults are filled on *import*; inventing them here would put values in a file
 *   the photographer never chose.
 * - `null` stays `null` (§4), which is how "not applicable to this body" survives as
 *   something different from "an older file that lacked the key".
 */
fun toExportRecipe(recipe: Recipe): JsonObject {
    val source = recipe.toJson()
    val out = linkedMapOf<String, JsonElement>()

    RECIPE_KEY_ORDER.forEach { key ->
        if (key in EXPORT_EXCLUDED) return@forEach
        if (key == "images" && recipe.images.isEmpty()) return@forEach
        val value = source[key] ?: return@forEach

        out[key] = if (key == "settings") toExportSettings(value) else value
    }

    // SF-017. After the documented keys, so §3's order still reads as §3's order.
    source.forEach { (key, value) ->
        if (key in out || key in EXPORT_EXCLUDED || key in RECIPE_KEY_ORDER) return@forEach
        out[key] = value
    }

    return JsonObject(out)
}

private fun toExportSettings(settings: JsonElement): JsonElement {
    // Not something this build understands as settings. Passed through rather than replaced
    // with an empty object, on the same reasoning as the unknown keys.
    val source = settings as? JsonObject ?: return settings
    val out = linkedMapOf<String, JsonElement>()

    SETTINGS_KEY_ORDER.forEach { key -> source[key]?.let { out[key] = it } }
    source.forEach { (key, value) -> if (key !in out) out[key] = value }

    return JsonObject(out)
}

/** §2's envelope, in §2's key order. */
fun buildEnvelope(recipes: List<Recipe>, exportedAt: Instant = Instant.now()): JsonObject =
    buildJsonObject {
        put("format", RECIPE_FORMAT_ID)
        put("version", RECIPE_FORMAT_VERSION)
        put("exportedAt", isoTimestamp(exportedAt))
        put("recipes", JsonArray(recipes.map(::toExportRecipe)))
    }

fun serialise(value: JsonElement): String = json.encodeToString(JsonElement.serializer(), value) + "\n"

/** The whole library-export path in one call. */
fun exportRecipes(recipes: List<Recipe>, exportedAt: Instant = Instant.now()): String =
    serialise(buildEnvelope(recipes, exportedAt))

/**
 * A single recipe as a bare object — SF-005's third shape.
 *
 * No envelope: the file is one recipe, and import accepts exactly this. It also makes the
 * file readable at a glance, which is the point of exporting one.
 */
fun exportRecipe(recipe: Recipe): String = serialise(toExportRecipe(recipe))

// ─── SF-018: filenames ──────────────────────────────────────────────────────

private val ISO_MILLIS: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC)

private val ISO_DATE: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC)

private fun isoTimestamp(at: Instant): String = ISO_MILLIS.format(at)

enum class ExportKind(val extension: String) { JSON("json"), ZIP("zip") }

/**
 * SF-018's multi-recipe filename: `fuji-recipes-YYYY-MM-DD.json` or `.zip`.
 *
 * The date is **UTC**, not local. SF-018 does not say, and local would read better on a phone
 * — but the format is UTC everywhere else, and the web client's server pins its own
 * `Content-Disposition` to UTC so that the same export produces the same name from either
 * side. Two clients disagreeing about a filename by a day is a small thing that looks like a
 * missing backup.
 */
fun libraryExportFilename(kind: ExportKind, at: Instant = Instant.now()): String =
    "fuji-recipes-${ISO_DATE.format(at)}.${kind.extension}"

/**
 * Lowercase; runs of anything that is not a letter or digit become one `-`; ends trimmed.
 *
 * Unicode letters are kept — a recipe named "Cinéma" should not become "cin-ma".
 */
fun slugify(name: String): String = name
    .lowercase()
    .replace(Regex("[^\\p{L}\\p{N}]+"), "-")
    .trim('-')

/**
 * SF-018's single-recipe filename.
 *
 * A name can slugify to nothing — all punctuation — and `.json` alone is not a filename. The
 * fallback names the recipe by the front of its id, which is unique and traceable back to the
 * library, rather than a generic `recipe.json` that a second export would overwrite.
 */
fun recipeExportFilename(recipe: Recipe): String {
    val slug = slugify(recipe.name)
    if (slug.isNotEmpty()) return "$slug.json"

    return if (recipe.id.isNotEmpty()) "recipe-${recipe.id.take(8)}.json" else "recipe.json"
}

/**
 * The archive's entries: one bare recipe per file, keyed by filename.
 *
 * **Names are de-duplicated**, and that is not a nicety. Recipe names are not unique, so two
 * recipes called "Portra 400" produce one filename twice — and a map keyed by name would
 * quietly ship one recipe where there were two. The first keeps the plain name; each later
 * clash gets `-2`, `-3` before the extension. Suffixing rather than falling back to ids keeps
 * the archive readable, which is the only reason to choose it over a single file.
 */
fun archiveEntries(recipes: List<Recipe>): Map<String, String> {
    val entries = linkedMapOf<String, String>()
    val used = mutableMapOf<String, Int>()

    recipes.forEach { recipe ->
        val base = recipeExportFilename(recipe).removeSuffix(".json")
        val seen = used.getOrDefault(base, 0)
        used[base] = seen + 1

        val name = if (seen == 0) "$base.json" else "$base-${seen + 1}.json"
        entries[name] = exportRecipe(recipe)
    }

    return entries
}
