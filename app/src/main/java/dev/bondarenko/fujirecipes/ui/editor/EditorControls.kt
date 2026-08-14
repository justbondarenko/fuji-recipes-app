package dev.bondarenko.fujirecipes.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.data.fields.EnumFieldDef
import dev.bondarenko.fujirecipes.data.fields.FilmSimulations
import dev.bondarenko.fujirecipes.data.fields.NumberField
import dev.bondarenko.fujirecipes.data.fields.SensorGeneration
import dev.bondarenko.fujirecipes.ui.library.FilmSimBadge
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme
import dev.bondarenko.fujirecipes.ui.theme.TabularFigures
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A number, with `−` on its left and `+` on its right — FEAT-003 T-07.
 *
 * **The field between them is editable, not a readout.** Colour temperature runs 2500–10000
 * in steps of 100, so stepping it end to end is 75 taps; the range that makes buttons
 * pleasant for `±4` makes them useless here, and typing is the escape hatch.
 *
 * The buttons clamp to the field's range, so they cannot produce an invalid value. Typing
 * can, which is why `RecipeValidation` checks the typed value rather than trusting the
 * control.
 */
// ponytail: one control for every numeric field, per the owner's "for now". If the wide
// ranges prove slow in use, the narrow signed fields (±4, ±5, ±9) are the ones that would
// benefit from direct value selection instead.
@Composable
fun NumberStepper(
    field: NumberField,
    value: Double?,
    onValueChange: (Double?) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    val step = field.stepFor(SensorGeneration.XTRANS_V)

    // The text is local so a half-typed "-" or "" is not immediately clobbered by the
    // formatted model value; the model is updated only when the text parses.
    var text by remember(field.id, value) { mutableStateOf(display(value, step)) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = field.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )

            IconButton(
                onClick = {
                    val next = ((value ?: 0.0) - step).coerceIn(field.min, field.max)
                    text = display(next, step)
                    onValueChange(next)
                },
                enabled = (value ?: 0.0) > field.min,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_remove),
                    contentDescription = stringResource(R.string.decrease, field.label),
                )
            }

            OutlinedTextField(
                value = text,
                onValueChange = { entered ->
                    text = entered
                    val parsed = entered.toDoubleOrNull()
                    // A partial entry ("-", "") leaves the model alone rather than
                    // resetting it; validation runs on the parsed value at save time.
                    if (parsed != null) onValueChange(parsed)
                },
                singleLine = true,
                isError = error != null,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    textAlign = TextAlign.Center,
                    fontFeatureSettings = TabularFigures,
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.width(96.dp),
            )

            IconButton(
                onClick = {
                    val next = ((value ?: 0.0) + step).coerceIn(field.min, field.max)
                    text = display(next, step)
                    onValueChange(next)
                },
                enabled = (value ?: 0.0) < field.max,
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.increase, field.label),
                )
            }
        }

        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
    }
}

/** `2`, `-1`, `1.5` — no trailing `.0`, because these are typed as well as read. */
private fun display(value: Double?, step: Double): String {
    if (value == null) return ""
    return if (step < 1.0 && abs(value % 1.0) > 0.001) {
        String.format("%.1f", value)
    } else {
        value.roundToInt().toString()
    }
}

/** An enum field — FEAT-003 T-08. Labels from the field table, never prettified ids. */
@Composable
fun EnumDropdown(
    field: EnumFieldDef,
    value: String?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = field.label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )

        TextButton(onClick = { expanded = true }) {
            Text(field.labelFor(value ?: field.defaultValue as? String))
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            field.options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onValueChange(option.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** The film simulation, with its swatch — it is the field that defines the recipe. */
@Composable
fun FilmSimulationPicker(
    value: String?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FilmSimBadge(value, size = 32.dp)
        Text(
            text = stringResource(R.string.field_film_simulation),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = { expanded = true }) {
            Text(FilmSimulations.labelFor(value))
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            FilmSimulations.all.forEach { simulation ->
                DropdownMenuItem(
                    leadingIcon = { FilmSimBadge(simulation.id, size = 24.dp) },
                    text = { Text(simulation.label) },
                    onClick = {
                        onValueChange(simulation.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** Rating — FEAT-003 T-09. Tapping the current value clears it, so 0 is reachable. */
@Composable
fun RatingInput(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        (1..5).forEach { star ->
            IconButton(onClick = { onRatingChange(if (rating == star) 0 else star) }) {
                Icon(
                    painter = if (star <= rating) {
                        rememberVectorPainter(Icons.Filled.Star)
                    } else {
                        painterResource(R.drawable.ic_star_border)
                    },
                    contentDescription = stringResource(R.string.rating_of_five, star),
                    tint = if (star <= rating) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

/** Tags — FEAT-003 T-09. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagInput(
    tags: List<String>,
    onTagsChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    var entry by remember { mutableStateOf("") }

    fun commit() {
        val tag = entry.trim()
        // Silently ignoring a duplicate rather than erroring: the user's intent is already
        // satisfied, and an error for "this is already true" is noise.
        if (tag.isNotEmpty() && tag !in tags) onTagsChange(tags + tag)
        entry = ""
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (tags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                tags.forEach { tag ->
                    InputChip(
                        selected = false,
                        onClick = { onTagsChange(tags - tag) },
                        label = { Text(tag) },
                        trailingIcon = {
                            Icon(
                                Icons.Filled.Clear,
                                contentDescription = stringResource(R.string.remove_tag, tag),
                                modifier = Modifier.size(16.dp),
                            )
                        },
                    )
                }
            }
        }

        OutlinedTextField(
            value = entry,
            onValueChange = { entry = it },
            singleLine = true,
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            label = { Text(stringResource(R.string.add_tag)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            trailingIcon = {
                if (entry.isNotBlank()) {
                    IconButton(onClick = ::commit) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = stringResource(R.string.add_tag),
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(name = "Controls — light", showBackground = true)
@Preview(name = "Controls — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun EditorControlsPreview() {
    FujiTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NumberStepper(
                field = dev.bondarenko.fujirecipes.data.fields.RecipeFields.byId("sharpness") as NumberField,
                value = 2.0,
                onValueChange = {},
            )
            NumberStepper(
                field = dev.bondarenko.fujirecipes.data.fields.RecipeFields.byId("colorTemperature") as NumberField,
                value = 5500.0,
                onValueChange = {},
                error = "Colour temperature runs from 2500 to 10000.",
            )
            FilmSimulationPicker(value = "classic-chrome", onValueChange = {})
            RatingInput(rating = 3, onRatingChange = {})
            TagInput(tags = listOf("street", "warm"), onTagsChange = {})
        }
    }
}
