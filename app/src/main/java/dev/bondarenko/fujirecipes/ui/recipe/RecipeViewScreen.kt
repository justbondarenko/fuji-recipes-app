package dev.bondarenko.fujirecipes.ui.recipe

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.bondarenko.fujirecipes.FujiRecipesApp
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.data.fields.FieldFormatting
import dev.bondarenko.fujirecipes.data.fields.FieldGroup
import dev.bondarenko.fujirecipes.ui.common.SectionHeader
import dev.bondarenko.fujirecipes.ui.common.errorMessageFor
import dev.bondarenko.fujirecipes.ui.editor.RatingInput
import dev.bondarenko.fujirecipes.ui.editor.TagInput
import dev.bondarenko.fujirecipes.ui.library.FilmSimBadge
import dev.bondarenko.fujirecipes.ui.library.LibraryPanel
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme
import dev.bondarenko.fujirecipes.ui.theme.TabularFigures

/**
 * A recipe, laid out for reading — FEAT-002 T-08.
 *
 * **No editable inputs.** Reading happens far more often than changing, and a form is a poor
 * way to read: it invites edits you did not mean to make and buries the values you came for
 * among controls. One action gets from here to the editor.
 *
 * Values sitting at their default render at reduced emphasis (`field-definitions.md` §5), so
 * a recipe's character — the handful of things actually dialled in — is scannable without
 * reading every row.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeViewScreen(
    state: RecipeViewUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onChangedOnlyChange: (Boolean) -> Unit,
    onRatingChange: (Int) -> Unit = {},
    onTagsChange: (List<String>) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            // Not the recipe name: it is already the largest thing on the screen, two
            // lines below. The bar's job here is saying where back goes.
            title = {
                Text(
                    text = stringResource(R.string.action_back_to_list),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.clickable(onClick = onBack),
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                    )
                }
            },
            actions = {
                if (state.recipe != null) {
                    FilledTonalIconButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(percent = 50),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .width(36.dp)
                            .height(48.dp),
                    ) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.action_edit),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
            ),
        )

        when {
            state.isLoading -> Unit

            state.isNotFound -> LibraryPanel(
                title = stringResource(R.string.recipe_not_found_title),
                body = stringResource(R.string.recipe_not_found_body),
                primaryLabel = stringResource(R.string.action_back_to_list),
                onPrimary = onBack,
                modifier = Modifier.padding(16.dp),
            )

            else -> RecipeBody(state, onChangedOnlyChange, onRatingChange, onTagsChange)
        }
    }
}

@Composable
private fun RecipeBody(
    state: RecipeViewUiState,
    onChangedOnlyChange: (Boolean) -> Unit,
    onRatingChange: (Int) -> Unit,
    onTagsChange: (List<String>) -> Unit,
) {
    val recipe = state.recipe ?: return

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { RecipeHeaderBlock(recipe, onRatingChange, onTagsChange) }

        item {
            // A toggle, not a button: it has an on and an off state that persist, and a
            // chip made the reader guess which one they were looking at.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.changed_only),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = state.changedOnly, onCheckedChange = onChangedOnlyChange)
            }
        }

        if (state.saveError != null) {
            item {
                Text(
                    text = errorMessageFor(state.saveError),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        if (state.nothingChanged) {
            item {
                // Distinguished from "no rows": an empty screen with a filter on reads as a
                // bug, and this recipe is simply untouched.
                Text(
                    text = stringResource(R.string.nothing_changed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                )
            }
        }

        state.groups.forEachIndexed { index, group ->
            item(key = group.group.id) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionHeader(group.group.label, showDivider = index > 0)
                    SettingsGroupBlock(group)
                }
            }
        }

        if (recipe.notes.isNotBlank()) {
            item { NotesBlock(recipe.notes) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipeHeaderBlock(
    recipe: RecipeHeader,
    onRatingChange: (Int) -> Unit,
    onTagsChange: (List<String>) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilmSimBadge(
                    simulationId = recipe.filmSimulationId,
                    size = 56.dp,
                    shape = RoundedCornerShape(12.dp),
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = recipe.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = recipe.filmSimulationLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Rating and tags are the two things adjusted while looking at a photo, so they are
            // live here. Every other parameter stays read-only on this screen.
            RatingInput(rating = recipe.rating, onRatingChange = onRatingChange)

            TagInput(tags = recipe.tags, onTagsChange = onTagsChange)

            Button(
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_photo_camera),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_write_to_camera))
            }
        }
    }
}

@Composable
private fun SettingsGroupBlock(group: SettingsGroup) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        group.rows.forEachIndexed { index, row ->
            if (index > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
            SettingRow(row)
        }
    }
}

@Composable
private fun SettingRow(row: FieldFormatting.Row) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // 💡 ROW PADDING & SPACING: Change vertical = 10.dp to adjust height/spacing between rows
            .padding(horizontal = 4.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 💡 FIELD LABEL (Recipe View mode):
        // - Change font style/size: `style = MaterialTheme.typography.bodyMedium` (or add `fontSize = 14.sp`)
        // - Change font weight: add `fontWeight = FontWeight.SemiBold`
        Text(
            text = row.label,
            style = MaterialTheme.typography.bodyMedium,
            // §5: defaults at reduced emphasis, so changed fields are scannable. The label
            // dims with the value — a bright label beside a faint value reads as broken.
            color = if (row.isDefault) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.weight(1f),
        )
        // 💡 FIELD VALUE (Recipe View mode):
        // - Change font style/size: `style = MaterialTheme.typography.bodyMedium` (or add `fontSize = 14.sp`)
        // - Change font weight: add `fontWeight = FontWeight.Bold`
        Text(
            text = row.value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFeatureSettings = TabularFigures,
            ),
            color = if (row.isDefault) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

@Composable
private fun NotesBlock(notes: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.notes),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = notes,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun RecipeViewRouteContent(
    recipeId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    val container = (LocalContext.current.applicationContext as FujiRecipesApp).container
    val viewModel: RecipeViewModel =
        viewModel(factory = RecipeViewModel.factory(container, recipeId))
    val state by viewModel.state.collectAsStateWithLifecycle()

    RecipeViewScreen(
        state = state,
        onBack = onBack,
        onEdit = onEdit,
        onChangedOnlyChange = viewModel::onChangedOnlyChange,
        onRatingChange = viewModel::onRatingChange,
        onTagsChange = viewModel::onTagsChange,
    )
}

// --- previews ---

private val sampleHeader = RecipeHeader(
    id = "a",
    name = "Kodachrome 64",
    filmSimulationId = "classic-chrome",
    filmSimulationLabel = "Classic Chrome",
    rating = 5,
    tags = listOf("street", "warm"),
    notes = "Warm midday light. ETERNA is too flat for this.",
)

private fun row(id: String, label: String, value: String, default: Boolean = false) =
    FieldFormatting.Row(id, label, value, default, advisory = false)

private val sampleGroups = listOf(
    SettingsGroup(
        FieldGroup.SIMULATION,
        listOf(
            row("filmSimulation", "Film simulation", "Classic Chrome"),
            row("dynamicRange", "Dynamic range", "DR400"),
        ),
    ),
    SettingsGroup(
        FieldGroup.TONE,
        listOf(
            row("highlightTone", "Highlight tone", "+1.5"),
            row("shadowTone", "Shadow tone", "0", default = true),
            row("color", "Color", "+2"),
            row("sharpness", "Sharpness", "0", default = true),
        ),
    ),
    SettingsGroup(
        FieldGroup.WHITE_BALANCE,
        listOf(
            row("whiteBalance", "White balance", "Colour temperature"),
            row("colorTemperature", "Colour temperature", "5500K"),
            row(FieldFormatting.WB_SHIFT_ROW_ID, "WB shift", "R +3 / B -2"),
        ),
    ),
)

@Preview(name = "Recipe — light", showBackground = true, heightDp = 950)
@Preview(name = "Recipe — dark", showBackground = true, uiMode = 0x20, heightDp = 950)
@Composable
private fun RecipeViewPreview() {
    FujiTheme {
        RecipeViewScreen(
            state = RecipeViewUiState(
                isLoading = false,
                recipe = sampleHeader,
                groups = sampleGroups,
            ),
            onBack = {}, onEdit = {}, onChangedOnlyChange = {},
        )
    }
}

@Preview(name = "Recipe — nothing changed", showBackground = true, heightDp = 700)
@Composable
private fun RecipeNothingChangedPreview() {
    FujiTheme {
        RecipeViewScreen(
            state = RecipeViewUiState(
                isLoading = false,
                recipe = sampleHeader.copy(rating = 0, tags = emptyList(), notes = ""),
                groups = emptyList(),
                changedOnly = true,
                nothingChanged = true,
            ),
            onBack = {}, onEdit = {}, onChangedOnlyChange = {},
        )
    }
}

@Preview(name = "Recipe — not found", showBackground = true, heightDp = 600)
@Composable
private fun RecipeNotFoundPreview() {
    FujiTheme {
        RecipeViewScreen(
            state = RecipeViewUiState(isLoading = false, recipe = null),
            onBack = {}, onEdit = {}, onChangedOnlyChange = {},
        )
    }
}
