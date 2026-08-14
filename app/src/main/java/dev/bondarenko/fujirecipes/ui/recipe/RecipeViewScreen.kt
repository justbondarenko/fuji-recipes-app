package dev.bondarenko.fujirecipes.ui.recipe

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
 * Recipe view presented inside a Material 3 Modal Bottom Sheet.
 *
 * It opens taking up to 80% screen space (partially expanded) and seamlessly expands
 * to full screen as the user scrolls through the Bento Grid parameters.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeViewBottomSheet(
    recipeId: String,
    onDismiss: () -> Unit,
    onEdit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = (LocalContext.current.applicationContext as FujiRecipesApp).container
    val viewModel: RecipeViewModel =
        viewModel(factory = RecipeViewModel.factory(container, recipeId), key = recipeId)
    val state by viewModel.state.collectAsStateWithLifecycle()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier,
    ) {
        RecipeViewContent(
            state = state,
            onClose = onDismiss,
            onEdit = { onEdit(recipeId) },
            onChangedOnlyChange = viewModel::onChangedOnlyChange,
            onRatingChange = viewModel::onRatingChange,
            onTagsChange = viewModel::onTagsChange,
        )
    }
}

/**
 * Full-screen Recipe View Screen (used for deep linking or standalone view routes).
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

        RecipeViewContent(
            state = state,
            onClose = onBack,
            onEdit = onEdit,
            onChangedOnlyChange = onChangedOnlyChange,
            onRatingChange = onRatingChange,
            onTagsChange = onTagsChange,
        )
    }
}

/**
 * Shared Recipe View Content containing the Hero Card and Bento Grid parameters.
 */
@Composable
fun RecipeViewContent(
    state: RecipeViewUiState,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    onChangedOnlyChange: (Boolean) -> Unit,
    onRatingChange: (Int) -> Unit = {},
    onTagsChange: (List<String>) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(280.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        state.isNotFound -> {
            LibraryPanel(
                title = stringResource(R.string.recipe_not_found_title),
                body = stringResource(R.string.recipe_not_found_body),
                primaryLabel = stringResource(R.string.action_back_to_list),
                onPrimary = onClose,
                modifier = modifier.padding(16.dp),
            )
        }

        else -> {
            RecipeBentoBody(
                state = state,
                onEdit = onEdit,
                onChangedOnlyChange = onChangedOnlyChange,
                onRatingChange = onRatingChange,
                onTagsChange = onTagsChange,
                modifier = modifier,
            )
        }
    }
}

/**
 * Bento Grid body for the recipe view.
 */
@Composable
private fun RecipeBentoBody(
    state: RecipeViewUiState,
    onEdit: () -> Unit,
    onChangedOnlyChange: (Boolean) -> Unit,
    onRatingChange: (Int) -> Unit,
    onTagsChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val recipe = state.recipe ?: return

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Hero Header Card
        item {
            RecipeHeaderBlock(
                recipe = recipe,
                onEdit = onEdit,
                onRatingChange = onRatingChange,
                onTagsChange = onTagsChange,
            )
        }

        // Changed settings toggle
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.changed_only),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
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
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }

        if (state.nothingChanged) {
            item {
                Text(
                    text = stringResource(R.string.nothing_changed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                )
            }
        }

        // Bento Grid Sections
        state.groups.forEachIndexed { index, group ->
            item(key = group.group.id) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader(group.group.label, showDivider = index > 0)
                    BentoGroupGrid(group)
                }
            }
        }

        if (recipe.notes.isNotBlank()) {
            item {
                BentoNotesCard(recipe.notes)
            }
        }
    }
}

/**
 * Header card containing recipe thumbnail, name, simulation, rating, tags, and actions.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipeHeaderBlock(
    recipe: RecipeHeader,
    onEdit: () -> Unit,
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilmSimBadge(
                    simulationId = recipe.filmSimulationId,
                    size = 56.dp,
                    shape = CircleShape,
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = recipe.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = recipe.filmSimulationLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                FilledTonalIconButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(percent = 50),
                    modifier = Modifier
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

/**
 * 2-column Bento Grid for section parameters.
 */
@Composable
private fun BentoGroupGrid(group: SettingsGroup) {
    val rows = group.rows
    val pairs = rows.chunked(2)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        pairs.forEach { pair ->
            if (pair.size == 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BentoParameterTile(
                        row = pair[0],
                        modifier = Modifier.weight(1f),
                    )
                    BentoParameterTile(
                        row = pair[1],
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                BentoParameterTile(
                    row = pair[0],
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * Individual Bento Grid parameter card.
 */
@Composable
private fun BentoParameterTile(
    row: FieldFormatting.Row,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (row.isDefault) {
                MaterialTheme.colorScheme.surfaceContainerLow
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = row.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = row.value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = TabularFigures,
                ),
                color = if (row.isDefault) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Full-width Bento Card for recipe notes.
 */
@Composable
private fun BentoNotesCard(notes: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.notes),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = notes,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
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
