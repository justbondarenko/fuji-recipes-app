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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import dev.bondarenko.fujirecipes.ui.common.SectionHeader
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
@Composable
fun ExportScreen(
    state: ExportUiState,
    onToggle: (String) -> Unit,
    onSelectAll: () -> Unit,
    onSelectNone: () -> Unit,
    onChooseKind: (ExportKind) -> Unit,
    onExport: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp + contentPadding.calculateTopPadding(),
            bottom = 24.dp + contentPadding.calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SectionHeader(stringResource(R.string.export_title)) }

        when {
            !state.hasLoaded -> item { Body(stringResource(R.string.export_loading)) }

            state.isEmptyLibrary -> item {
                LibraryPanel(
                    title = stringResource(R.string.export_empty_title),
                    body = stringResource(R.string.export_empty_body),
                )
            }

            else -> {
                item { Body(stringResource(R.string.export_intro)) }

                state.error?.let { message -> item { Panel(message, alert = true) } }

                item { FormatChoice(state.kind, onChooseKind) }

                item { SelectionHeader(state, onSelectAll, onSelectNone) }

                items(state.recipes, key = { it.id }) { recipe ->
                    RecipeRow(
                        recipe = recipe,
                        selected = recipe.id in state.selected,
                        onToggle = { onToggle(recipe.id) },
                    )
                }

                item {
                    Button(
                        onClick = onExport,
                        enabled = state.canExport,
                        modifier = Modifier.fillMaxWidth(),
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

@Composable
private fun FormatChoice(kind: ExportKind, onChoose: (ExportKind) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = kind == ExportKind.JSON,
                onClick = { onChoose(ExportKind.JSON) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) {
                Text(stringResource(R.string.export_format_json))
            }
            SegmentedButton(
                selected = kind == ExportKind.ZIP,
                onClick = { onChoose(ExportKind.ZIP) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
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

@Composable
private fun RecipeRow(recipe: Recipe, selected: Boolean, onToggle: () -> Unit) {
    Surface(
        onClick = onToggle,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = selected, onCheckedChange = { onToggle() })
            FilmSimBadge(simulationId = recipe.filmSimulationId, size = 40.dp)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = FilmSimulations.byId(recipe.filmSimulationId)?.label.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Body(text: String) {
    Text(
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
fun ExportRouteContent(contentPadding: PaddingValues) {
    val context = LocalContext.current
    val container = (context.applicationContext as FujiRecipesApp).container
    val viewModel: ExportViewModel = viewModel(factory = ExportViewModel.factory(container))
    val state by viewModel.state.collectAsStateWithLifecycle()

    ExportScreen(
        state = state,
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
