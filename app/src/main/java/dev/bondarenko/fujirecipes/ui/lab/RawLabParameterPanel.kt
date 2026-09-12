package dev.bondarenko.fujirecipes.ui.lab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
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
 * The lab's editing surface.
 *
 * The same controls as the recipe form — a field that behaves differently in two places is a
 * field nobody trusts — but a shorter list: only what the camera's RAW engine will act on. A
 * control that cannot change the picture has no business on a page whose whole purpose is
 * watching the picture change, so the recipe's other fields are left to the recipe form and
 * carried through a save untouched.
 */
@Composable
fun RawLabParameterPanel(
    settings: JsonObject,
    fields: List<RecipeField>,
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
            .filter { group -> fields.any { it.group == group } }
            .forEach { group ->
                item(key = group.id) {
                    FieldSection(
                        title = group.labHeading(),
                        fields = fields.filter { it.group == group },
                        settings = settings,
                        onSettingChange = onSettingChange,
                    )
                }
            }
    }
}

/**
 * The group's name, as the lab should say it.
 *
 * §4 calls the shooting group "Recommendations — not written to the camera", which is true of
 * a **custom slot** and false here: exposure compensation is one of the words the RAW profile
 * carries, verified at native index 4. The only field of that group the lab draws is that one,
 * so it gets a heading that does not contradict the picture in front of you.
 */
@Composable
private fun FieldGroup.labHeading(): String = when (this) {
    FieldGroup.SHOOTING -> stringResource(R.string.lab_group_exposure)
    else -> label
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
