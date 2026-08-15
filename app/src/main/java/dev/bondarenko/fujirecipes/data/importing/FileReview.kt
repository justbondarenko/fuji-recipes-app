package dev.bondarenko.fujirecipes.data.importing

import dev.bondarenko.fujirecipes.camera.plan.areConfigsEqual
import dev.bondarenko.fujirecipes.data.fields.FieldContext
import dev.bondarenko.fujirecipes.data.fields.RecipeFields
import dev.bondarenko.fujirecipes.data.fields.RecipeValidation
import dev.bondarenko.fujirecipes.data.fields.SensorGeneration
import dev.bondarenko.fujirecipes.data.model.Recipe
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put

/**
 * What each entry of an import file *is*, before anything is written — FEAT-012.
 *
 * **Transcribed from** `fuji-recipes-book/src/utils/import-review.ts`
 * (`coding-standards.md` P3), and it is the full five statuses this time: a file carries ids,
 * so the id-collision branch that camera import could never reach (`ImportReview.kt`) is
 * exactly the branch that matters here.
 *
 * Pure, and under `data/` rather than `ui/` for the same reason `LibraryView.kt` is (P7): this
 * is the part with rules in it. The screen renders rows; this decides what a row is.
 */

enum class FileRowStatus {
    /** Valid, and nothing in the library has this id. */
    NEW,

    /** Valid, but an existing recipe has the same id (SF-010) — needs a resolution. */
    CONFLICT,

    /** Valid, but an existing recipe, or an earlier row, has the same camera configuration. */
    CONFIG_DUPLICATE,

    /** Valid and new, but a *different* recipe already uses this name (SF-011). */
    NAME_WARNING,

    /** Fails validation. Never importable (SF-014). */
    INVALID,
}

/**
 * SF-012 — exactly three, no more and no fewer.
 *
 * The ids are the values `RecipeRepository.importAll` reads; a fourth option here would be
 * one nothing acts on.
 */
enum class Resolution(val id: String) {
    SKIP("skip"),
    REPLACE("replace"),
    KEEP_BOTH("keep-both"),
}

data class FileRow(
    /** Index in the parsed import; the key everything else is keyed by. */
    val index: Int,
    /** The archive entry it came from, when there was one. */
    val source: String? = null,
    /** The entry as the file holds it, unknown keys and all (SF-017). */
    val recipe: JsonElement,
    /** For display. A recipe with no usable name still needs a row (SF-014). */
    val name: String,
    val status: FileRowStatus,
    /** Every failing field, with its path. Empty unless [INVALID]. */
    val errors: List<RecipeValidation.Problem> = emptyList(),
    /** The library recipe, or earlier row, this collides with. */
    val existingName: String? = null,
) {
    val id: String? get() = (recipe as? JsonObject)?.get("id")?.stringOrNull

    val filmSimulationId: String?
        get() = (recipe as? JsonObject)?.get("settings")?.let { it as? JsonObject }
            ?.get("filmSimulation")?.stringOrNull

    /** SF-014: an invalid row can never be imported, whatever the user does. */
    val importable: Boolean get() = status != FileRowStatus.INVALID

    /** A row the user has to decide about before the import can run. */
    val needsResolution: Boolean
        get() = status == FileRowStatus.CONFLICT || status == FileRowStatus.CONFIG_DUPLICATE

    /**
     * What the user may choose, which is not the same question for both kinds of collision.
     *
     * SF-012's three options are about an **id** collision: replacing means overwriting the
     * recipe that holds that id. A configuration duplicate collides on nothing that can be
     * keyed on — it is a different recipe that happens to be set up identically — so
     * "Replace" would have nothing to replace, and the reference offering it there is why a
     * skipped duplicate imports anyway on the web. Two honest options beat three where one
     * is a lie.
     */
    val resolutionOptions: List<Resolution>
        get() = when (status) {
            FileRowStatus.CONFLICT -> listOf(Resolution.SKIP, Resolution.REPLACE, Resolution.KEEP_BOTH)
            FileRowStatus.CONFIG_DUPLICATE -> listOf(Resolution.SKIP, Resolution.KEEP_BOTH)
            else -> emptyList()
        }
}

/**
 * One row per entry, in file order.
 *
 * Order matters beyond presentation: SF-009 has the importer append in file order, so these
 * rows are the order the library ends up in.
 */
fun reviewFile(parsed: List<ParsedFileRecipe>, library: List<Recipe>): List<FileRow> {
    val byId = library.associateBy { it.id }
    val byName = library.associateBy { it.name.trim().lowercase() }
    val librarySettings = library.map { it to it.settings.toPlainMap() }

    val reviewed = mutableListOf<FileRow>()

    parsed.forEach { entry ->
        reviewed += row(entry, byId, byName, librarySettings, reviewed)
    }

    return reviewed
}

private fun row(
    entry: ParsedFileRecipe,
    byId: Map<String, Recipe>,
    byName: Map<String, Recipe>,
    librarySettings: List<Pair<Recipe, Map<String, Any?>>>,
    reviewed: List<FileRow>,
): FileRow {
    val recipe = entry.recipe as? JsonObject
    val rawName = recipe?.get("name")?.stringOrNull?.trim()
    val name = if (rawName.isNullOrEmpty()) NO_NAME else rawName

    val problems = validateFileRecipe(entry.recipe)
    if (problems.isNotEmpty() || recipe == null) {
        return FileRow(
            index = entry.index,
            source = entry.source,
            recipe = entry.recipe,
            name = name,
            status = FileRowStatus.INVALID,
            errors = problems,
        )
    }

    val existingById = recipe["id"]?.stringOrNull?.let { byId[it] }
    if (existingById != null) {
        // SF-010: an id collision is a conflict, and the flow stops for a decision.
        return FileRow(
            index = entry.index,
            source = entry.source,
            recipe = recipe,
            name = name,
            status = FileRowStatus.CONFLICT,
            existingName = existingById.name,
        )
    }

    val settings = (recipe["settings"] as? JsonObject)?.toPlainMap().orEmpty()

    val duplicate = librarySettings.firstOrNull { (_, stored) -> areConfigsEqual(settings, stored) }
        ?.first?.name
        ?: reviewed.firstOrNull { earlier ->
            earlier.importable &&
                areConfigsEqual(settings, earlier.settingsMap())
        }?.name

    if (duplicate != null) {
        return FileRow(
            index = entry.index,
            source = entry.source,
            recipe = recipe,
            name = name,
            status = FileRowStatus.CONFIG_DUPLICATE,
            existingName = duplicate,
        )
    }

    val existingByName = byName[name.lowercase()]
    if (existingByName != null) {
        // SF-011: a name collision without an id collision is a *warning*. The import proceeds
        // — two recipes may legitimately share a name.
        return FileRow(
            index = entry.index,
            source = entry.source,
            recipe = recipe,
            name = name,
            status = FileRowStatus.NAME_WARNING,
            existingName = existingByName.name,
        )
    }

    return FileRow(
        index = entry.index,
        source = entry.source,
        recipe = recipe,
        name = name,
        status = FileRowStatus.NEW,
    )
}

private const val NO_NAME = "(no name)"

private fun FileRow.settingsMap(): Map<String, Any?> =
    ((recipe as? JsonObject)?.get("settings") as? JsonObject)?.toPlainMap().orEmpty()

/**
 * SF-014's validation, against the one field table this app has (P3).
 *
 * **A missing key is not an error.** §4 fills missing keys with their defaults on import — so
 * `valid-minimal.json`, which is a name and a simulation, has to come out valid here too.
 *
 * The applicability context comes from the recipe's own simulation and white balance, so a
 * monochrome recipe is not failed for the colour value it does not have.
 */
fun validateFileRecipe(entry: JsonElement): List<RecipeValidation.Problem> {
    val recipe = entry as? JsonObject
        ?: return listOf(RecipeValidation.Problem("", "this entry is not a recipe object"))

    val problems = mutableListOf<RecipeValidation.Problem>()

    val name = recipe["name"]
    when {
        name == null -> problems += RecipeValidation.Problem("name", "is required")
        name.stringOrNull == null -> problems += RecipeValidation.Problem("name", "must be text")
    }

    recipe["notes"]?.let {
        if (it.stringOrNull == null) problems += RecipeValidation.Problem("notes", "must be text")
    }

    recipe["rating"]?.let {
        val rating = (it as? JsonPrimitive)?.takeIf { p -> !p.isString }?.intOrNull
        if (rating == null) {
            problems += RecipeValidation.Problem("rating", "must be a whole number")
        } else {
            RecipeValidation.validateRating(rating)?.let(problems::add)
        }
    }

    recipe["tags"]?.let { tags ->
        val list = (tags as? JsonArray)?.map { it.stringOrNull }
        when {
            list == null -> problems += RecipeValidation.Problem("tags", "must be a list")
            list.any { it == null } -> problems += RecipeValidation.Problem("tags", "must be text")
            else -> RecipeValidation.validateTags(list.filterNotNull())?.let(problems::add)
        }
    }

    val settings = recipe["settings"]
    if (settings != null && settings !is JsonObject) {
        problems += RecipeValidation.Problem("settings", "must be an object of parameters")
    }

    // Only when the name is usable: reporting every out-of-range parameter on an entry that is
    // not a recipe at all buries the one problem that matters.
    if (problems.isEmpty()) {
        val values = settings as? JsonObject ?: JsonObject(emptyMap())
        problems += RecipeValidation.validate(
            name = recipe["name"]?.stringOrNull.orEmpty(),
            notes = recipe["notes"]?.stringOrNull.orEmpty(),
            rating = (recipe["rating"] as? JsonPrimitive)?.intOrNull ?: 0,
            tags = (recipe["tags"] as? JsonArray)?.mapNotNull { it.stringOrNull }.orEmpty(),
            settings = values,
            context = FieldContext(
                // The widest field set, as everywhere else in this app: `sensorGeneration` was
                // dropped as a column, and assuming a narrower body would fail a recipe for a
                // parameter its own camera supports.
                generation = SensorGeneration.XTRANS_V,
                filmSimulationId = values["filmSimulation"]?.stringOrNull
                    ?: RecipeFields.byId("filmSimulation")?.defaultValue as? String,
                grainEffectOff = (values["grainEffect"]?.stringOrNull ?: "off") == "off",
                whiteBalanceId = values["whiteBalance"]?.stringOrNull ?: "auto",
            ),
        )
    }

    return problems
}

data class FileImportSummary(
    val total: Int,
    val new: Int,
    val conflicts: Int,
    val warnings: Int,
    val invalid: Int,
    /** Conflicts still waiting for a decision — the reason Import stays disabled. */
    val undecided: Int,
) {
    /** How many recipes the import would actually write. */
    val importing: Int get() = total - invalid - undecided
}

fun summarise(rows: List<FileRow>, resolutions: Map<Int, Resolution>) = FileImportSummary(
    total = rows.size,
    new = rows.count { it.status == FileRowStatus.NEW },
    conflicts = rows.count { it.needsResolution },
    warnings = rows.count { it.status == FileRowStatus.NAME_WARNING },
    invalid = rows.count { it.status == FileRowStatus.INVALID },
    undecided = rows.count { it.needsResolution && resolutions[it.index] == null },
)

/**
 * The body for `RecipeRepository.importAll`.
 *
 * `resolutions` is keyed by **recipe id**, and only rows that collide on an id appear in it:
 * recipes absent from `resolutions` and not conflicting are imported.
 *
 * **A skipped row without an id is dropped here rather than passed on.** The reference sends
 * `resolutions` keyed by id and lets the importer decide, which works for an id collision and
 * silently fails for a configuration duplicate — that row has no colliding id, so nothing
 * keys it, and the recipe the user just chose to skip is imported anyway. Dropping it here is
 * the only place that decision can be honoured.
 */
fun fileImportBody(rows: List<FileRow>, resolutions: Map<Int, Resolution>): JsonObject {
    val chosen = mutableMapOf<String, String>()
    val recipes = mutableListOf<JsonElement>()

    rows.filter { it.importable }.forEach { row ->
        val resolution = resolutions[row.index]
        val id = row.id

        if (resolution == Resolution.SKIP && (id == null || row.status != FileRowStatus.CONFLICT)) {
            return@forEach
        }
        if (row.needsResolution && resolution == null) return@forEach

        if (resolution != null && id != null && row.status == FileRowStatus.CONFLICT) {
            chosen[id] = resolution.id
        }
        recipes += row.recipe
    }

    return buildJsonObject {
        put("recipes", JsonArray(recipes))
        put(
            "resolutions",
            JsonObject(chosen.mapValues { (_, value) -> JsonPrimitive(value) }),
        )
    }
}

private val JsonElement.stringOrNull: String?
    get() = (this as? JsonPrimitive)?.takeIf { it.isString }?.content
