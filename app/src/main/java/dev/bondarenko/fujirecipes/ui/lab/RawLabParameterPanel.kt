package dev.bondarenko.fujirecipes.ui.lab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.data.fields.EnumFieldDef
import dev.bondarenko.fujirecipes.data.fields.FieldGroup
import dev.bondarenko.fujirecipes.data.fields.NumberField
import dev.bondarenko.fujirecipes.data.fields.RecipeField
import dev.bondarenko.fujirecipes.ui.common.SectionHeader
import dev.bondarenko.fujirecipes.ui.editor.EnumButtonGroup
import dev.bondarenko.fujirecipes.ui.editor.EnumDropdown
import dev.bondarenko.fujirecipes.ui.editor.FilmSimulationPicker
import dev.bondarenko.fujirecipes.ui.editor.NumberStepper
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * The lab's editing surface, in two bands.
 *
 * The same controls as the recipe form — a field that behaves differently in two places is a
 * field nobody trusts — but ordered by a different question: not "what is this recipe" but
 * "what will the camera do with it". Anything the connected body's RAW engine cannot act on is
 * still editable, because it belongs in a saved recipe, and is grouped under a heading that
 * says so rather than being disabled or hidden.
 */
@Composable
fun RawLabParameterPanel(
    settings: JsonObject,
    renderedFields: List<RecipeField>,
    storedOnlyFields: List<RecipeField>,
    onSettingChange: (String, JsonElement?) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        FieldGroup.entries
            .filter { group -> renderedFields.any { it.group == group } }
            .forEach { group ->
                item(key = "rendered-${group.id}") {
                    FieldSection(
                        title = group.label,
                        fields = renderedFields.filter { it.group == group },
                        settings = settings,
                        onSettingChange = onSettingChange,
                    )
                }
            }

        if (storedOnlyFields.isNotEmpty()) {
            item(key = "stored-only") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader(stringResource(R.string.lab_band_stored_only))
                    Text(
                        text = stringResource(R.string.lab_band_stored_only_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        storedOnlyFields.forEach { field ->
                            FieldControl(field, settings, onSettingChange)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldSection(
    title: String,
    fields: List<RecipeField>,
    settings: JsonObject,
    onSettingChange: (String, JsonElement?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(title)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            fields.forEach { field -> FieldControl(field, settings, onSettingChange) }
        }
    }
}

@Composable
private fun FieldControl(
    field: RecipeField,
    settings: JsonObject,
    onSettingChange: (String, JsonElement?) -> Unit,
) {
    when (field) {
        is EnumFieldDef -> when {
            field.id == "filmSimulation" -> FilmSimulationPicker(
                value = settings.stringOrNull(field.id),
                onValueChange = { onSettingChange(field.id, JsonPrimitive(it)) },
            )

            field.options.size <= 4 -> EnumButtonGroup(
                field = field,
                value = settings.stringOrNull(field.id),
                onValueChange = { onSettingChange(field.id, JsonPrimitive(it)) },
            )

            else -> EnumDropdown(
                field = field,
                value = settings.stringOrNull(field.id),
                onValueChange = { onSettingChange(field.id, JsonPrimitive(it)) },
            )
        }

        is NumberField -> NumberStepper(
            field = field,
            value = settings.numberOrNull(field.id),
            onValueChange = { onSettingChange(field.id, it?.let(::JsonPrimitive)) },
        )
    }
}

private fun JsonObject.stringOrNull(key: String): String? =
    (this[key] as? JsonPrimitive)?.content

private fun JsonObject.numberOrNull(key: String): Double? =
    this[key]?.jsonPrimitive?.doubleOrNull
