package dev.bondarenko.fujirecipes.data.importing

import dev.bondarenko.fujirecipes.data.exporting.RECIPE_FORMAT_ID
import dev.bondarenko.fujirecipes.data.exporting.RECIPE_FORMAT_VERSION
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

/**
 * Reading an export file back — FEAT-012, `recipe-format.spec.md` SF-003 to SF-007 and SF-015.
 *
 * **Transcribed from** `fuji-recipes-book/shared/format/import.ts` and
 * `shared/format/migrate.ts` (`coding-standards.md` P3). The format is shared, so a file the
 * web client accepts must import here and a file it refuses must be refused here — with the
 * same reason, because the reason is what the user acts on.
 *
 * **Parsing only.** Nothing here validates a recipe, decides what a conflict is, or writes
 * anything: SF-014 wants an invalid recipe *listed* rather than fatal, so that belongs to
 * [reviewFile]. What this decides is whether the **file** can be read at all.
 *
 * Pure — no `android.*`, no I/O beyond a byte array handed to it.
 */

/** Why a whole file was refused. Named, so the screen can say which (`coding-standards.md` P5). */
enum class RejectionReason { UNREADABLE, UNKNOWN_FORMAT, FUTURE_VERSION, TRAVERSAL, EMPTY, TOO_LARGE }

class ImportRejected(val reason: RejectionReason, message: String) : Exception(message)

/** SF-006's message, quoted from the requirement. */
const val FUTURE_VERSION_MESSAGE = "This export came from a newer version of the app."

data class ParsedFileRecipe(
    /**
     * Position in the import, from zero, across every file in an archive. SF-014 and
     * `contracts.md`'s `failed[].index` both report by index, and a name cannot be the key —
     * names are not unique and a recipe may not have one.
     */
    val index: Int,
    /**
     * The entry as it stands in the file.
     *
     * A [JsonElement] rather than an object, because "this entry is a number" is exactly the
     * kind of thing a hand-written file contains and SF-014 says one bad recipe must not block
     * the others. The reference wraps such an entry in a marker object to keep its types
     * happy; here the review reads the element and reports the row.
     */
    val recipe: JsonElement,
    /** The archive entry this came from, for a review row that can be traced back. */
    val source: String? = null,
)

data class ParsedImport(
    /** The version the file declared, or the current one when it declared none. */
    val version: Int,
    val recipes: List<ParsedFileRecipe>,
)

private val json = Json { ignoreUnknownKeys = true }

/**
 * One JSON document, in any of SF-005's three shapes.
 *
 * The shape test is structural rather than "does it have a `format` key": a hand-written file
 * is a real case, and a bare object with a mistyped `format` should be told its format is
 * wrong rather than silently treated as a single recipe. So an object carrying either
 * `format` or `recipes` is an envelope and is held to SF-002's rules; anything else
 * object-shaped is one recipe.
 */
fun readJsonFile(text: String, source: String? = null): ParsedImport {
    val subject = if (source != null) "$source is" else "That file is"

    val parsed = runCatching { json.parseToJsonElement(text) }.getOrElse { error ->
        throw ImportRejected(
            RejectionReason.UNREADABLE,
            "$subject not valid JSON: ${error.message ?: "it could not be parsed"}",
        )
    }

    if (parsed is JsonArray) return withVersion(RECIPE_FORMAT_VERSION, parsed, source)

    if (parsed !is JsonObject) {
        val found = if (parsed is JsonNull) "null" else "a plain value"
        throw ImportRejected(
            RejectionReason.UNREADABLE,
            "${source ?: "That file"} holds $found where a recipe or an export was expected.",
        )
    }

    val looksLikeEnvelope = "format" in parsed || "recipes" in parsed
    // SF-005's third shape: a single bare recipe, as the single-recipe export writes.
    if (!looksLikeEnvelope) return withVersion(RECIPE_FORMAT_VERSION, listOf(parsed), source)

    val format = (parsed["format"] as? JsonPrimitive)?.takeIf { it.isString }?.content
    if (format != RECIPE_FORMAT_ID) {
        // SF-003. Naming what was found as well as what was expected, because the likeliest
        // cause is a file from a different application entirely.
        throw ImportRejected(
            RejectionReason.UNKNOWN_FORMAT,
            "This is not a $RECIPE_FORMAT_ID file — its format is " +
                (format?.let { "“$it”" } ?: "missing") + ".",
        )
    }

    val version = (parsed["version"] as? JsonPrimitive)?.takeIf { !it.isString }?.intOrNull
    if (version == null || version < 1) {
        // SF-004. A file claiming `version: "1"` or `version: 0` is malformed rather than from
        // another era, and saying so is more use than a migration failure.
        throw ImportRejected(
            RejectionReason.UNREADABLE,
            "The file's version is ${parsed["version"] ?: "missing"}; " +
                "it must be a whole number of 1 or more.",
        )
    }

    if (version > RECIPE_FORMAT_VERSION) {
        // SF-006, with the required message first and the numbers after it.
        throw ImportRejected(
            RejectionReason.FUTURE_VERSION,
            "$FUTURE_VERSION_MESSAGE It is version $version; this build reads up to " +
                "version $RECIPE_FORMAT_VERSION.",
        )
    }

    val recipes = parsed["recipes"] as? JsonArray
        ?: throw ImportRejected(RejectionReason.UNREADABLE, "The file has no recipes array.")

    return withVersion(version, recipes, source)
}

/** SF-007 — the migration chain runs before anything looks at the recipes. */
private fun withVersion(
    version: Int,
    recipes: List<JsonElement>,
    source: String?,
): ParsedImport = ParsedImport(
    version = version,
    recipes = migrateRecipes(version, recipes)
        .mapIndexed { index, recipe -> ParsedFileRecipe(index, recipe, source) },
)

// ─── SF-007: the migration chain ────────────────────────────────────────────

/**
 * A step from one format version to the next. Pure: it returns a new list and never touches
 * its input, because the screen shows what was in the file and what will be imported, and
 * those are two views of the same import.
 */
typealias Migration = (List<JsonElement>) -> List<JsonElement>

/**
 * Keyed by the version being migrated *from*. `MIGRATIONS[1]` takes a version-1 file to
 * version 2.
 *
 * The `1 -> 2` slot is the identity, as §5 specifies: version 2 does not exist yet. The point
 * of carrying it now is that the first real migration will be written under pressure —
 * someone's backup will not load — and that is the worst moment to be designing where
 * migrations go and how they compose.
 */
val MIGRATIONS: Map<Int, Migration> = mapOf(1 to { recipes -> recipes })

fun migrateRecipes(from: Int, recipes: List<JsonElement>): List<JsonElement> =
    runChain(from, RECIPE_FORMAT_VERSION, recipes, MIGRATIONS)

/**
 * The chain itself, parameterised on its target version and its steps.
 *
 * Separate from [migrateRecipes] for §5's own reason: with the current version at 1 there are
 * no steps to run, so a test of [migrateRecipes] can only ever prove that nothing happens —
 * while this signature lets a test drive a two-step chain today and show that steps compose in
 * order and that a gap is an error rather than a silent skip.
 */
fun runChain(
    from: Int,
    to: Int,
    recipes: List<JsonElement>,
    migrations: Map<Int, Migration>,
): List<JsonElement> {
    require(from <= to) {
        "Cannot migrate down: the file is version $from and this build knows version $to."
    }

    var current = recipes
    for (version in from until to) {
        val step = migrations[version]
            ?: error("No migration from format version $version to ${version + 1}.")
        current = step(current)
    }
    return current
}

// ─── SF-015: archives ───────────────────────────────────────────────────────

/**
 * The path check, and the one piece of security in this file.
 *
 * An entry is refused if any segment is `..`, or if it is absolute — both are ways of naming a
 * location outside the archive's own root. Backslashes count as separators: a ZIP written on
 * Windows uses them, and a check that only understood `/` would wave `..\escaped.json`
 * straight through.
 *
 * Nothing here extracts to disk, so an escaping path could not overwrite a file even if it got
 * past. The check exists anyway because SF-015 says to refuse the *whole file*, and because
 * "nothing writes to disk" is a property of today's code rather than a promise about
 * tomorrow's.
 */
fun escapesRoot(entryName: String): Boolean {
    if (entryName.startsWith("/") || entryName.startsWith("\\")) return true
    if (Regex("^[A-Za-z]:").containsMatchIn(entryName)) return true
    return entryName.split(Regex("[/\\\\]+")).any { it == ".." }
}

/**
 * SF-015 — every `.json` entry at any depth; everything else ignored silently.
 *
 * **Refused whole, before a single entry is read.** SF-015 says the *file* is refused, not the
 * offending entry: an archive containing an escaping path is not a backup with a bad row in
 * it, it is a file built to do something else.
 *
 * "Ignored silently" is the requirement's word — a `README.txt` in someone's archive is not a
 * problem to report.
 */
fun readArchive(entries: Map<String, ByteArray>): ParsedImport {
    entries.keys.firstOrNull(::escapesRoot)?.let { escaping ->
        throw ImportRejected(
            RejectionReason.TRAVERSAL,
            "This archive contains a path that points outside it ($escaping), so none of it " +
                "was imported.",
        )
    }

    // Sorted, so the same archive imports in the same order twice: a ZIP's entry order is
    // whatever the writer chose, and SF-009 appends in file order — which has to mean
    // something stable.
    val files = entries.keys.filter { it.endsWith(".json", ignoreCase = true) }.sorted()

    if (files.isEmpty()) {
        throw ImportRejected(
            RejectionReason.EMPTY,
            "This archive holds no .json files, so there is nothing to import.",
        )
    }

    val recipes = mutableListOf<ParsedFileRecipe>()
    // The lowest version any entry declared: the archive is migrated from the oldest thing in
    // it. A future-version entry has already refused the whole archive by throwing.
    var version = RECIPE_FORMAT_VERSION

    files.forEach { name ->
        val parsed = readJsonFile(entries.getValue(name).decodeToString(), name)
        version = minOf(version, parsed.version)

        parsed.recipes.forEach { entry ->
            recipes += entry.copy(index = recipes.size, source = name)
        }
    }

    if (recipes.isEmpty()) {
        throw ImportRejected(
            RejectionReason.EMPTY,
            "Every .json file in this archive was empty, so there is nothing to import.",
        )
    }

    return ParsedImport(version, recipes)
}

/**
 * The largest archive this will unpack, and the largest any single entry may expand to.
 *
 * An export of the design limit of ~2000 recipes is a few megabytes of JSON, so 64 MB is far
 * above any real backup and far below what a decompression bomb needs to hurt the process.
 * The cap is on the *expanded* bytes because that is the number a bomb inflates — a 1 MB
 * archive can hold gigabytes.
 */
private const val MAX_EXPANDED_BYTES = 64L * 1024 * 1024

/**
 * A ZIP's entries as bytes, keyed by name.
 *
 * `java.util.zip` rather than a compression library: it is in the JDK, it streams, and a
 * recipe archive is a handful of small text files (`coding-standards.md` P8).
 */
fun unzip(bytes: ByteArray): Map<String, ByteArray> {
    val entries = linkedMapOf<String, ByteArray>()
    var expanded = 0L

    ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
        while (true) {
            val entry = zip.nextEntry ?: break
            if (entry.isDirectory) continue

            // Copied through a buffer rather than `readBytes()`, and counted as it goes: the
            // point of the cap is to stop *before* the memory is allocated. A bomb's single
            // entry is gigabytes, and measuring it afterwards would be measuring it from
            // inside the failure.
            val content = ByteArrayOutputStream()
            val buffer = ByteArray(16 * 1024)
            while (true) {
                val read = zip.read(buffer)
                if (read <= 0) break

                expanded += read
                if (expanded > MAX_EXPANDED_BYTES) {
                    throw ImportRejected(
                        RejectionReason.TOO_LARGE,
                        "This archive expands to more than " +
                            "${MAX_EXPANDED_BYTES / 1024 / 1024} MB, which is far larger than " +
                            "any library of recipes.",
                    )
                }
                content.write(buffer, 0, read)
            }

            entries[entry.name] = content.toByteArray()
        }
    }

    if (entries.isEmpty()) {
        throw ImportRejected(
            RejectionReason.UNREADABLE,
            "This file could not be read as a ZIP archive.",
        )
    }

    return entries
}

/**
 * The whole file-reading path: bytes in, parsed recipes out.
 *
 * The ZIP is detected by its own magic bytes rather than by the filename, because the picker
 * hands back whatever the user chose and a `.zip` that is really JSON — or the reverse — is a
 * worse error message than it needs to be.
 */
fun readImportFile(bytes: ByteArray, source: String? = null): ParsedImport =
    if (looksLikeZip(bytes)) readArchive(unzip(bytes)) else readJsonFile(bytes.decodeToString(), source)

/** `PK`, the local file header every non-empty ZIP starts with. */
internal fun looksLikeZip(bytes: ByteArray): Boolean =
    bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() &&
        bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()
