package dev.bondarenko.fujirecipes.data.importing

import dev.bondarenko.fujirecipes.camera.plan.areConfigsEqual
import dev.bondarenko.fujirecipes.camera.usb.SlotRecipe
import dev.bondarenko.fujirecipes.data.model.Recipe
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.put

/**
 * What each slot read off the camera *is*, before anything is written.
 *
 * **Transcribed from** `fuji-recipes-book/src/utils/import-review.ts` at commit `0c17106`
 * (`coding-standards.md` P3), minus its id-conflict path and its resolutions — see below.
 *
 * Pure, and under `data/` rather than `ui/` for the same reason `data/library/LibraryView.kt`
 * is (P7): this is the part with rules in it. The screen renders rows; this decides what a row
 * *is*.
 *
 * **Three statuses, not the web client's five.** The reference has to handle a file whose
 * recipes carry ids, so it has an id-collision status and a three-way resolution per row.
 * Nothing read off a camera carries an id — `coding-standards.md` P2 forbids the phone
 * inventing one and `POST /api/import` assigns it — so that branch is unreachable here.
 * Building it anyway would be speculative (P8), and untestable besides.
 */

enum class ImportStatus {
    /** Nothing in the library matches. */
    NEW,

    /**
     * An existing recipe, or an earlier slot in this same read, has the identical camera
     * configuration.
     */
    CONFIG_DUPLICATE,

    /**
     * A *different* recipe already has this name. A warning, not a problem: names are not
     * unique, and the import proceeds as a second recipe.
     */
    NAME_WARNING,
}

data class ImportRow(
    val slot: Int,
    val name: String,
    val settings: Map<String, Any?>,
    val status: ImportStatus,
    /** The recipe this collides with, for a duplicate or a name warning. */
    val existingName: String? = null,
    /** Whether it will be imported. Duplicates start off; everything else starts on. */
    val selected: Boolean,
) {
    val filmSimulationId: String? get() = settings["filmSimulation"] as? String
}

/**
 * One row per slot, in slot order.
 *
 * Duplicates are looked for in the library **and** in the rows already reviewed, so two
 * identical slots do not both import — the second names the first.
 */
fun reviewSlots(slots: List<SlotRecipe>, library: List<Recipe>): List<ImportRow> {
    val librarySettings = library.map { it to it.settings.toPlainMap() }
    val byName = library.associateBy { it.name.trim().lowercase() }

    val reviewed = mutableListOf<ImportRow>()

    slots.forEach { slot ->
        val duplicateInLibrary = librarySettings
            .firstOrNull { (_, settings) -> areConfigsEqual(slot.settings, settings) }
            ?.first

        val duplicateInBatch = reviewed
            .firstOrNull { areConfigsEqual(slot.settings, it.settings) }

        val existingByName = byName[slot.name.trim().lowercase()]

        reviewed += when {
            duplicateInLibrary != null -> ImportRow(
                slot = slot.slot,
                name = slot.name,
                settings = slot.settings,
                status = ImportStatus.CONFIG_DUPLICATE,
                existingName = duplicateInLibrary.name,
                // Off by default: the photographer asked to import what is on the camera, and
                // this is already in the library. Still listed, and still selectable, because
                // a deliberate second copy is their call and not this function's.
                selected = false,
            )

            duplicateInBatch != null -> ImportRow(
                slot = slot.slot,
                name = slot.name,
                settings = slot.settings,
                status = ImportStatus.CONFIG_DUPLICATE,
                existingName = duplicateInBatch.name,
                selected = false,
            )

            existingByName != null -> ImportRow(
                slot = slot.slot,
                name = slot.name,
                settings = slot.settings,
                status = ImportStatus.NAME_WARNING,
                existingName = existingByName.name,
                selected = true,
            )

            else -> ImportRow(
                slot = slot.slot,
                name = slot.name,
                settings = slot.settings,
                status = ImportStatus.NEW,
                selected = true,
            )
        }
    }

    return reviewed
}

data class ImportSummary(
    val total: Int,
    val new: Int,
    val duplicates: Int,
    val warnings: Int,
    val selected: Int,
) {
    /** Nothing on the camera is missing from the library. Worth saying rather than implying. */
    val nothingNew: Boolean get() = total > 0 && new == 0 && warnings == 0
}

fun summarise(rows: List<ImportRow>) = ImportSummary(
    total = rows.size,
    new = rows.count { it.status == ImportStatus.NEW },
    duplicates = rows.count { it.status == ImportStatus.CONFIG_DUPLICATE },
    warnings = rows.count { it.status == ImportStatus.NAME_WARNING },
    selected = rows.count { it.selected },
)

/**
 * The body for `POST /api/import` — the selected rows and nothing else.
 *
 * **No ids.** P2: the phone never invents one the server would assign, and
 * `src/server/api/import.post.ts` fills it in. `resolutions` is empty and always will be:
 * with no ids there is nothing to collide, so there is nothing to resolve.
 */
fun importBody(rows: List<ImportRow>): JsonObject = buildJsonObject {
    put(
        "recipes",
        JsonArray(
            rows.filter { it.selected }.map { row ->
                buildJsonObject {
                    put("name", row.name)
                    put("settings", row.settings.toJsonObject())
                }
            },
        ),
    )
    put("resolutions", JsonObject(emptyMap()))
}

// ─── JSON at the seam ───────────────────────────────────────────────────────

/**
 * A recipe's stored settings as the plain types the comparison talks in.
 *
 * Only primitives: a nested object or array is not a setting any field defines, and carrying
 * it into the comparison would compare something neither camera nor form can produce.
 */
internal fun JsonObject.toPlainMap(): Map<String, Any?> = mapNotNull { (key, element) ->
    val primitive = element as? JsonPrimitive ?: return@mapNotNull null
    val value: Any? = when {
        primitive.isString -> primitive.content
        else -> primitive.booleanOrNull ?: primitive.doubleOrNull
    }
    value?.let { key to it }
}.toMap()

internal fun Map<String, Any?>.toJsonObject(): JsonObject = buildJsonObject {
    forEach { (key, value) ->
        when (value) {
            is String -> put(key, value)
            is Boolean -> put(key, value)
            // Whole numbers go out as integers. A camera temperature written as `6500.0`
            // is the same value, but it is not what the rest of the platform stores and a
            // needless difference in the wire form is a needless difference in a diff.
            is Number -> if (value.toDouble() % 1.0 == 0.0) {
                put(key, value.toLong())
            } else {
                put(key, value.toDouble())
            }

            else -> Unit
        }
    }
}
