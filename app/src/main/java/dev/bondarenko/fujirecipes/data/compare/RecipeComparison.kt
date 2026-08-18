package dev.bondarenko.fujirecipes.data.compare

import dev.bondarenko.fujirecipes.data.fields.FieldContext
import dev.bondarenko.fujirecipes.data.fields.FieldFormatting
import dev.bondarenko.fujirecipes.data.fields.FieldGroup
import dev.bondarenko.fujirecipes.data.fields.RecipeFields
import dev.bondarenko.fujirecipes.data.model.Recipe
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * A single row comparing one configuration field between two recipes face-to-face.
 */
data class RecipeComparisonRow(
    val fieldId: String,
    val label: String,
    val valueA: String,
    val valueB: String,
    val isSame: Boolean,
    val group: FieldGroup,
    val isDefaultA: Boolean,
    val isDefaultB: Boolean,
)

/**
 * A group of comparison rows grouped under a [FieldGroup] category.
 */
data class RecipeComparisonGroup(
    val group: FieldGroup,
    val rows: List<RecipeComparisonRow>,
)

/**
 * The complete comparison result between two recipes.
 */
data class RecipeComparisonResult(
    val groups: List<RecipeComparisonGroup>,
    val totalFields: Int,
    val differencesCount: Int,
) {
    val allMatch: Boolean get() = differencesCount == 0 && totalFields > 0
}

object RecipeComparison {

    /**
     * Builds the [FieldContext] required for field applicability calculation.
     */
    fun contextFor(recipe: Recipe): FieldContext {
        val generation = RecipeFields.generationOf(
            runCatching { recipe.extra["sensorGeneration"]?.jsonPrimitive?.content }.getOrNull()
        )
        val grainEffect = runCatching {
            recipe.settings["grainEffect"]?.jsonPrimitive?.content
        }.getOrNull()
        val whiteBalance = runCatching {
            recipe.settings["whiteBalance"]?.jsonPrimitive?.content
        }.getOrNull()

        return FieldContext(
            generation = generation,
            filmSimulationId = recipe.filmSimulationId,
            grainEffectOff = grainEffect == null || grainEffect == "off",
            whiteBalanceId = whiteBalance ?: "auto",
        )
    }

    /**
     * Compares [base] recipe with [target] recipe face-to-face for every applicable field.
     */
    fun compare(base: Recipe, target: Recipe): RecipeComparisonResult {
        val contextA = contextFor(base)
        val contextB = contextFor(target)

        val rowsA = FieldFormatting.rowsFor(base.settings, contextA).associateBy { it.fieldId }
        val rowsB = FieldFormatting.rowsFor(target.settings, contextB).associateBy { it.fieldId }

        val comparisonRows = mutableListOf<RecipeComparisonRow>()

        for (field in RecipeFields.all) {
            val fieldId = if (field.id == "wbShiftRed") FieldFormatting.WB_SHIFT_ROW_ID else field.id
            if (field.id == "wbShiftBlue") continue // Handled by combined wbShift

            val rowA = rowsA[fieldId]
            val rowB = rowsB[fieldId]

            // If this field is not applicable to either recipe, omit it
            if (rowA == null && rowB == null) continue

            val valA = rowA?.value ?: "—"
            val valB = rowB?.value ?: "—"
            val isSame = (rowA != null && rowB != null && rowA.value == rowB.value)

            val label = rowA?.label ?: rowB?.label ?: field.label
            val group = rowA?.let { RecipeFields.byId(it.fieldId)?.group ?: FieldGroup.WHITE_BALANCE }
                ?: rowB?.let { RecipeFields.byId(it.fieldId)?.group ?: FieldGroup.WHITE_BALANCE }
                ?: field.group

            comparisonRows += RecipeComparisonRow(
                fieldId = fieldId,
                label = label,
                valueA = valA,
                valueB = valB,
                isSame = isSame,
                group = group,
                isDefaultA = rowA?.isDefault ?: true,
                isDefaultB = rowB?.isDefault ?: true,
            )
        }

        val groups = comparisonRows.groupBy { it.group }
            .toList()
            .sortedBy { (group, _) -> group.ordinal }
            .map { (group, rows) -> RecipeComparisonGroup(group, rows) }

        val totalFields = comparisonRows.size
        val differencesCount = comparisonRows.count { !it.isSame }

        return RecipeComparisonResult(
            groups = groups,
            totalFields = totalFields,
            differencesCount = differencesCount,
        )
    }
}
