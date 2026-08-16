package dev.bondarenko.fujirecipes.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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

    /**
     * An absent value shows the field's **default**, not an empty box.
     *
     * A recipe written before a field existed simply has no key for it, and the camera will
     * apply the default — so a blank control claims the parameter is unset when it is not.
     * The read-only view already falls back this way; the two must agree.
     */
    val effective = value ?: (field.defaultValue as? Number)?.toDouble()

    // The text is local so a half-typed "-" or "" is not immediately clobbered by the
    // formatted model value; the model is updated only when the text parses.
    var text by remember(field.id, value) { mutableStateOf(display(effective, step)) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // 💡 FIELD LABEL (Number controls e.g. Highlight tone, Shadow tone, Sharpness, Clarity):
            // - Change font style/size: `style = MaterialTheme.typography.bodyMedium` (or add `fontSize = 14.sp`)
            // - Change font weight: add `fontWeight = FontWeight.SemiBold`
            // - Change font color: `color = MaterialTheme.colorScheme.onSurface`
            Text(
                text = field.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )

            FilledTonalIconButton(
                onClick = {
                    val next = ((effective ?: 0.0) - step).coerceIn(field.min, field.max)
                    text = display(next, step)
                    onValueChange(next)
                },
                enabled = (effective ?: 0.0) > field.min,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_remove),
                    contentDescription = stringResource(R.string.decrease, field.label),
                    modifier = Modifier.size(16.dp),
                )
            }

            BasicTextField(
                value = text,
                onValueChange = { entered ->
                    text = entered
                    val parsed = entered.toDoubleOrNull()
                    // A partial entry ("-", "") leaves the model alone rather than
                    // resetting it; validation runs on the parsed value at save time.
                    if (parsed != null) onValueChange(parsed)
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFeatureSettings = TabularFigures,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .width(58.dp)
                            .height(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f))
                            .border(
                                width = 1.dp,
                                color = if (error != null) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(10.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        innerTextField()
                    }
                },
            )

            FilledTonalIconButton(
                onClick = {
                    val next = ((effective ?: 0.0) + step).coerceIn(field.min, field.max)
                    text = display(next, step)
                    onValueChange(next)
                },
                enabled = (effective ?: 0.0) < field.max,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.increase, field.label),
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp),
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
        // 💡 FIELD LABEL (Dropdown fields):
        // - Change font style/size: `style = MaterialTheme.typography.bodyMedium` (or add `fontSize = 14.sp`)
        // - Change font weight: add `fontWeight = FontWeight.SemiBold`
        Text(
            text = field.label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )

        Box {
            TextButton(onClick = { expanded = true }) {
                Text(field.labelFor(value ?: field.defaultValue as? String))
            }

            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                val current = value ?: field.defaultValue as? String
                field.options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        trailingIcon = {
                            if (option.id == current) {
                                Icon(Icons.Filled.Check, contentDescription = null)
                            }
                        },
                        onClick = {
                            onValueChange(option.id)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

/**
 * A Material 3 Expressive connected single-choice button group for enum fields with
 * a small set of options (e.g. Dynamic Range, Grain, Color Chrome) placed below the label.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EnumButtonGroup(
    field: EnumFieldDef,
    value: String?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val current = value ?: field.defaultValue as? String
    val count = field.options.size

    Column(
        modifier = modifier.fillMaxWidth(),
        // 💡 SPACING BETWEEN LABEL AND BUTTONS: Change `6.dp` to increase/decrease gap
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // 💡 FIELD LABEL (Button Group fields e.g. Dynamic range, Grain effect, Color Chrome):
        // - Change font style/size: `style = MaterialTheme.typography.bodyMedium` (or add `fontSize = 14.sp`)
        // - Change font weight: add `fontWeight = FontWeight.SemiBold`
        Text(
            text = field.label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        // The corner morph on selection, the neighbour squeeze on press and the colour
        // cross-fade are all ButtonGroup/ToggleButton behaviour now — the shapes below only
        // say which position in the group each button holds.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            field.options.forEachIndexed { index, option ->
                val selected = option.id == current

                ToggleButton(
                    checked = selected,
                    onCheckedChange = { onValueChange(option.id) },
                    modifier = Modifier.weight(1f),
                    shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        count - 1 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                    colors = ToggleButtonDefaults.toggleButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        checkedContainerColor = MaterialTheme.colorScheme.primary,
                        checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                ) {
                    if (selected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(ToggleButtonDefaults.IconSize),
                        )
                        Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
                    }
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                }
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
        // 💡 FIELD LABEL (Film simulation picker):
        // - Change font style/size: `style = MaterialTheme.typography.bodyMedium` (or add `fontSize = 14.sp`)
        // - Change font weight: add `fontWeight = FontWeight.SemiBold`
        Text(
            text = stringResource(R.string.field_film_simulation),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Box {
            TextButton(onClick = { expanded = true }) {
                Text(FilmSimulations.labelFor(value))
            }

            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                FilmSimulations.all.forEach { simulation ->
                    DropdownMenuItem(
                        leadingIcon = { FilmSimBadge(simulation.id, size = 24.dp) },
                        text = { Text(simulation.label) },
                        trailingIcon = {
                            if (simulation.id == value) {
                                Icon(Icons.Filled.Check, contentDescription = null)
                            }
                        },
                        onClick = {
                            onValueChange(simulation.id)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

/**
 * Rating — FEAT-003 T-09, tightened after design review.
 *
 * Five `IconButton`s reserved 48dp each plus their own internal padding, so a five-star
 * control ran most of the screen width for a row of small glyphs. These are 40dp targets
 * with no padding between them: still comfortably tappable, roughly half the footprint.
 *
 * Tapping the current value clears it, so 0 is reachable without a separate control.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RatingInput(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The default 48dp interactive minimum is what set the stars so far apart: each button
    // reserved that much width around a 20dp glyph. Relaxing it lets the gap be the gap.
    //
    // 💡 STAR SPACING — `RatingStarGap` is half a star wide; raise it to loosen the row.
    // 💡 STAR SIZE — `RatingStarSize` drives both the glyph and the tap target.
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RatingStarGap),
    ) {
        (1..5).forEach { star ->
            // IconToggleButton rather than a 28dp Box with a ripple hung off it: a star is a
            // toggle, and the hand-built version was under the 48dp minimum touch target and
            // carried no toggle semantics for TalkBack.
            IconToggleButton(
                checked = star <= rating,
                onCheckedChange = { onRatingChange(if (rating == star) 0 else star) },
                colors = IconButtonDefaults.iconToggleButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.outline,
                    checkedContainerColor = Color.Transparent,
                    checkedContentColor = MaterialTheme.colorScheme.tertiary,
                ),
                modifier = Modifier.size(RatingStarSize),
            ) {
                Icon(
                    painter = if (star <= rating) {
                        rememberVectorPainter(Icons.Filled.Star)
                    } else {
                        painterResource(R.drawable.ic_star_border)
                    },
                    contentDescription = stringResource(R.string.rating_of_five, star),
                    modifier = Modifier.size(RatingStarSize),
                )
            }
        }
    }
    }
}

/** 💡 The star glyph, and the button around it. */
private val RatingStarSize = 24.dp

/** 💡 Half a star, per the header design. */
private val RatingStarGap = RatingStarSize / 2

/**
 * Tags — FEAT-003 T-09, reworked after design review.
 *
 * The text field used to be permanently on screen, which spent a full input row on
 * something used occasionally. It is now behind a `+` chip that sits at the end of the tags:
 * the field appears when asked for, takes focus, and goes away again once a tag is added or
 * the entry is abandoned.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagInput(
    tags: List<String>,
    onTagsChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    /**
     * Show at most this many, with a `+N` chip that unfolds the rest. Null shows them all.
     *
     * The chips stay a cloud rather than becoming a list when expanded — a `FlowRow` that
     * wraps is the same shape either way, just taller.
     */
    collapsedLimit: Int? = null,
) {
    // Keyed on the tag count so adding or removing one re-collapses rather than leaving the
    // "+N" chip claiming a number that is no longer true.
    var expanded by remember(tags.size) { mutableStateOf(false) }
    val hidden = if (collapsedLimit == null || expanded) {
        0
    } else {
        (tags.size - collapsedLimit).coerceAtLeast(0)
    }
    val visibleTags = if (hidden > 0) tags.take(collapsedLimit!!) else tags

    var adding by remember { mutableStateOf(false) }
    var entry by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    fun commit() {
        val tag = entry.trim()
        // A duplicate is silently ignored rather than reported: the user's intent is already
        // satisfied, and an error for "this is already true" is noise.
        if (tag.isNotEmpty() && tag !in tags) onTagsChange(tags + tag)
        entry = ""
        adding = false
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                visibleTags.forEach { tag ->
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

                if (hidden > 0) {
                    // 💡 OVERFLOW CHIP — the "+4" that unfolds the rest of the cloud.
                    AssistChip(
                        onClick = { expanded = true },
                        label = { Text("+$hidden") },
                    )
                }

                if (!adding) {
                    AssistChip(
                        onClick = { adding = true },
                        label = {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = stringResource(R.string.add_tag),
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                }
            }
        }

        if (adding) {
            OutlinedTextField(
                value = entry,
                onValueChange = { entry = it },
                singleLine = true,
                isError = error != null,
                supportingText = error?.let { { Text(it) } },
                label = { Text(stringResource(R.string.add_tag)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { commit() }),
                trailingIcon = {
                    IconButton(onClick = { if (entry.isBlank()) adding = false else commit() }) {
                        Icon(
                            imageVector = if (entry.isBlank()) Icons.Filled.Clear else Icons.Filled.Add,
                            contentDescription = stringResource(
                                if (entry.isBlank()) R.string.action_cancel else R.string.add_tag,
                            ),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            )

            // Opening the field and then having to tap it is two taps for one intent.
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
        } else if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
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
