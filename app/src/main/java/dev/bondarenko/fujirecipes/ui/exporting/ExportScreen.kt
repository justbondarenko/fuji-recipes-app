package dev.bondarenko.fujirecipes.ui.exporting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.bondarenko.fujirecipes.FujiRecipesApp
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.core.share.ShareFile
import dev.bondarenko.fujirecipes.data.exporting.ExportKind
import dev.bondarenko.fujirecipes.data.fields.FilmSimulations
import dev.bondarenko.fujirecipes.data.model.Recipe
import dev.bondarenko.fujirecipes.ui.library.FilmSimBadge
import dev.bondarenko.fujirecipes.ui.library.LibraryPanel
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Export — FEAT-008 T-12.
 *
 * A selection list and one action. The file is built here and handed to the OS, which is the
 * whole design: this screen knows nothing about Drive, mail or the file system, and does not
 * need to.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    state: ExportUiState,
    onBack: () -> Unit,
    onToggle: (String) -> Unit,
    onSelectAll: () -> Unit,
    onSelectNone: () -> Unit,
    onChooseKind: (ExportKind) -> Unit,
    onExport: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.export_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
            ),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 24.dp + contentPadding.calculateBottomPadding(),
            ),
            // Tight, because the recipe rows below form one segmented block and stepped
            // corners only read as a group when the rows nearly touch. Everything that is not
            // a row asks for its own breathing space with `SectionGap`.
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            when {
            !state.hasLoaded -> item { Body(stringResource(R.string.export_loading)) }

            state.isEmptyLibrary -> item {
                LibraryPanel(
                    title = stringResource(R.string.export_empty_title),
                    body = stringResource(R.string.export_empty_body),
                )
            }

            else -> {
                item { Body(stringResource(R.string.export_intro), Modifier.padding(SectionGap)) }

                state.error?.let { message -> item { Panel(message, alert = true) } }

                item {
                    FormatChoice(
                        state.kind,
                        onChooseKind,
                        modifier = Modifier.padding(bottom = SectionGap),
                    )
                }

                item { SelectionHeader(state, onSelectAll, onSelectNone) }

                itemsIndexed(state.recipes, key = { _, it -> it.id }) { index, recipe ->
                    RecipeRow(
                        recipe = recipe,
                        selected = recipe.id in state.selected,
                        index = index,
                        count = state.recipes.size,
                        onToggle = { onToggle(recipe.id) },
                    )
                }

                item {
                    Button(
                        onClick = onExport,
                        enabled = state.canExport,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = SectionGap),
                    ) {
                        Text(
                            if (state.canExport) {
                                // Names the file it will produce: a share sheet that opens
                                // with a surprise filename is one you have to undo.
                                stringResource(R.string.export_action, state.filename)
                            } else {
                                stringResource(R.string.export_action_none)
                            },
                        )
                    }
                }
            }
        }
    }
}
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FormatChoice(
    kind: ExportKind,
    onChoose: (ExportKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            ToggleButton(
                checked = kind == ExportKind.JSON,
                onCheckedChange = { onChoose(ExportKind.JSON) },
                shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.export_format_json))
            }
            ToggleButton(
                checked = kind == ExportKind.ZIP,
                onCheckedChange = { onChoose(ExportKind.ZIP) },
                shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.export_format_zip))
            }
        }

        // A line on when to want each, rather than making the reader guess what a ZIP buys.
        Body(
            stringResource(
                if (kind == ExportKind.JSON) {
                    R.string.export_format_json_hint
                } else {
                    R.string.export_format_zip_hint
                },
            ),
        )
    }
}

@Composable
private fun SelectionHeader(
    state: ExportUiState,
    onSelectAll: () -> Unit,
    onSelectNone: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.export_selected, state.selected.size, state.total),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = if (state.allSelected) onSelectNone else onSelectAll) {
            Text(
                stringResource(
                    if (state.allSelected) R.string.export_select_none else R.string.export_select_all,
                ),
            )
        }
    }
}

/**
 * One recipe in the M3 multi-select list, sectioned variant.
 *
 * `index` and `count` are what make the rows read as one block rather than a stack of cards:
 * `ListItemDefaults.segmentedShapes` rounds the outer corners of the first and last rows and
 * squares the ones between them.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RecipeRow(
    recipe: Recipe,
    selected: Boolean,
    index: Int,
    count: Int,
    onToggle: () -> Unit,
) {
    ListItem(
        onClick = onToggle,
        selected = selected,
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = Modifier.fillMaxWidth(),
        leadingContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // The row itself toggles, so the box is a state readout, not a second target.
                Checkbox(checked = selected, onCheckedChange = null)
                FilmSimBadge(simulationId = recipe.filmSimulationId, size = 40.dp)
            }
        },
        supportingContent = {
            Text(FilmSimulations.byId(recipe.filmSimulationId)?.label.orEmpty())
        },
    ) {
        Text(text = recipe.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Body(text: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun Panel(text: String, alert: Boolean = false) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (alert) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (alert) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Composable
fun ExportRouteContent(
    onBack: () -> Unit,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val container = (context.applicationContext as FujiRecipesApp).container
    val viewModel: ExportViewModel = viewModel(factory = ExportViewModel.factory(container))
    val state by viewModel.state.collectAsStateWithLifecycle()

    ExportScreen(
        state = state,
        onBack = onBack,
        onToggle = viewModel::toggle,
        onSelectAll = viewModel::selectAll,
        onSelectNone = viewModel::selectNone,
        onChooseKind = viewModel::chooseKind,
        onExport = {
            val file = viewModel.buildFile()
            if (file != null) {
                runCatching { ShareFile.share(context, file.filename, file.bytes) }
                    // The distinction worth keeping: the file could not be prepared, so
                    // nothing left the app. That is not the same as no app taking it, which
                    // the system's own sheet handles.
                    .onFailure { viewModel.onShareFailed(it.message.orEmpty()) }
            }
        },
        contentPadding = contentPadding,
    )
}

// ─── Previews ───────────────────────────────────────────────────────────────

private fun sample(id: String, name: String, simulation: String) = Recipe(
    id = id,
    name = name,
    settings = buildJsonObject { put("filmSimulation", simulation) },
)

private val sampleRecipes = listOf(
    sample("a", "Kodachrome 64", "classic-chrome"),
    sample("b", "Acros Night", "acros"),
    sample("c", "1970s Summer", "nostalgic-negative"),
)

@Composable
private fun PreviewScreen(state: ExportUiState) {
    FujiTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            ExportScreen(
                state = state,
                onBack = {},
                onToggle = {}, onSelectAll = {}, onSelectNone = {}, onChooseKind = {},
                onExport = {},
                contentPadding = PaddingValues(0.dp),
            )
        }
    }
}

@Preview(name = "Export — all selected", showBackground = true)
@Preview(name = "Export — all selected, dark", showBackground = true, uiMode = 0x20)
@Composable
private fun ExportAllPreview() = PreviewScreen(
    ExportUiState(
        recipes = sampleRecipes,
        selected = sampleRecipes.map { it.id }.toSet(),
        hasLoaded = true,
    ),
)

@Preview(name = "Export — some selected, as a ZIP", showBackground = true)
@Composable
private fun ExportPartialPreview() = PreviewScreen(
    ExportUiState(
        recipes = sampleRecipes,
        selected = setOf("a"),
        kind = ExportKind.ZIP,
        hasLoaded = true,
    ),
)

@Preview(name = "Export — nothing selected", showBackground = true)
@Composable
private fun ExportNonePreview() = PreviewScreen(
    ExportUiState(recipes = sampleRecipes, selected = emptySet(), hasLoaded = true),
)

@Preview(name = "Export — empty library", showBackground = true)
@Composable
private fun ExportEmptyPreview() = PreviewScreen(ExportUiState(hasLoaded = true))

@Preview(name = "Export — could not be prepared", showBackground = true)
@Composable
private fun ExportFailedPreview() = PreviewScreen(
    ExportUiState(
        recipes = sampleRecipes,
        selected = sampleRecipes.map { it.id }.toSet(),
        hasLoaded = true,
        error = "No space left on device",
    ),
)

private val SectionGap = 10.dp
