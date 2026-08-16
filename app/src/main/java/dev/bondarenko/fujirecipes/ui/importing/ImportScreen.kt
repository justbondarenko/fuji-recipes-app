package dev.bondarenko.fujirecipes.ui.importing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.bondarenko.fujirecipes.FujiRecipesApp
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.data.fields.FilmSimulations
import dev.bondarenko.fujirecipes.data.importing.ImportRow
import dev.bondarenko.fujirecipes.data.importing.ImportStatus
import dev.bondarenko.fujirecipes.ui.common.FujiCenteredLoading
import dev.bondarenko.fujirecipes.ui.common.FujiIconPanel
import dev.bondarenko.fujirecipes.ui.library.FilmSimBadge
import dev.bondarenko.fujirecipes.ui.library.LibraryPanel
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme

/**
 * Import from camera — FEAT-007 T-16.
 *
 * The screen is a sequence rather than a form: connect, read, review, import. Each stage
 * shows one thing to decide and one action, because the whole flow happens with a camera
 * hanging off the phone by a cable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    state: ImportUiState,
    onBack: () -> Unit,
    onConnect: () -> Unit,
    onRead: () -> Unit,
    onToggle: (Int) -> Unit,
    onImport: () -> Unit,
    onBackToReview: () -> Unit,
    onDone: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.import_title),
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
            // Tight, so the review rows read as one segmented block. Every other stage emits a
            // single item, where spacing does not apply; the review stage asks for its own
            // breathing space with `SectionGap`.
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            when (val stage = state.stage) {
            ImportStage.NeedsCamera -> item {
                ImportPanel(
                    title = stringResource(R.string.import_needs_camera_title),
                    body = stringResource(R.string.import_needs_camera_body),
                    actionLabel = stringResource(R.string.camera_action_connect),
                    onAction = onConnect,
                    modifier = Modifier.fillParentMaxSize(),
                )
            }

            ImportStage.Ready -> item {
                ImportPanel(
                    title = state.cameraModel?.let {
                        stringResource(R.string.import_ready_title, it)
                    } ?: stringResource(R.string.import_ready_title_generic),
                    body = stringResource(R.string.import_ready_body),
                    actionLabel = stringResource(R.string.import_action_read),
                    onAction = onRead,
                    modifier = Modifier.fillParentMaxSize(),
                )
            }

            is ImportStage.Reading -> item {
                FujiCenteredLoading(
                    label = stringResource(R.string.import_reading, stage.current, stage.total),
                    // Reading knows which slot it is on, so the indicator says so rather than
                    // spinning anonymously through seven of them.
                    progress = {
                        if (stage.total == 0) 0f else stage.current.toFloat() / stage.total
                    },
                    modifier = Modifier.fillParentMaxSize(),
                )
            }

            ImportStage.Review -> reviewItems(state, onToggle, onImport, onRead)

            ImportStage.Importing -> item {
                FujiCenteredLoading(
                    label = stringResource(R.string.import_importing),
                    progress = null,
                    modifier = Modifier.fillParentMaxSize(),
                )
            }

            is ImportStage.Done -> item {
                LibraryPanel(
                    title = stringResource(R.string.import_done_title, stage.imported),
                    body = stringResource(R.string.import_done_body),
                    primaryLabel = stringResource(R.string.action_done),
                    onPrimary = onDone,
                )
            }

            is ImportStage.Failed -> item {
                LibraryPanel(
                    title = stringResource(R.string.import_failed_title),
                    body = stage.message,
                    primaryLabel = if (stage.retryable) {
                        stringResource(R.string.import_action_back_to_review)
                    } else {
                        stringResource(R.string.import_action_read_again)
                    },
                    onPrimary = if (stage.retryable) onBackToReview else onRead,
                )
            }
        }
    }
}
}

/**
 * This screen's identity: recipes coming down off the camera, so the arrow points down —
 * `MaterialShapes.Arrow` starts at 270°, and 90° is that turned through 180°.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ImportPanel(
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FujiIconPanel(
        icon = painterResource(R.drawable.ic_linked_camera),
        shape = MaterialShapes.Pill.toShape(),
        title = title,
        body = body,
        actionLabel = actionLabel,
        onAction = onAction,
        modifier = modifier,
    )
}

private fun androidx.compose.foundation.lazy.LazyListScope.reviewItems(
    state: ImportUiState,
    onToggle: (Int) -> Unit,
    onImport: () -> Unit,
    onRead: () -> Unit,
) {
    if (state.rows.isEmpty()) {
        item {
            LibraryPanel(
                title = stringResource(R.string.import_empty_title),
                body = stringResource(R.string.import_empty_body),
                primaryLabel = stringResource(R.string.import_action_read_again),
                onPrimary = onRead,
            )
        }
        return
    }

    val summary = state.summary

    item {
        Text(
            modifier = Modifier.padding(bottom = SectionGap),
            text = stringResource(R.string.import_summary, summary.total, summary.selected),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    // Worth saying rather than leaving the reader to notice every row is unticked.
    if (summary.nothingNew) {
        item { Note(stringResource(R.string.import_nothing_new)) }
    }

    itemsIndexed(state.rows, key = { _, it -> it.slot }) { index, row ->
        ImportRowCard(
            row = row,
            index = index,
            count = state.rows.size,
            onToggle = { onToggle(row.slot) },
        )
    }

    item {
        Button(
            onClick = onImport,
            enabled = state.canImport,
            modifier = Modifier.fillMaxWidth().padding(top = SectionGap),
        ) {
            Text(
                if (state.canImport) {
                    stringResource(R.string.import_action_import, summary.selected)
                } else {
                    stringResource(R.string.import_action_import_none)
                },
            )
        }
    }

    item {
        OutlinedButton(onClick = onRead, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.import_action_read_again))
        }
    }
}

/** One slot in the M3 multi-select list, sectioned variant — the export list's twin. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ImportRowCard(row: ImportRow, index: Int, count: Int, onToggle: () -> Unit) {
    ListItem(
        onClick = onToggle,
        selected = row.selected,
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
                Checkbox(checked = row.selected, onCheckedChange = null)
                FilmSimBadge(simulationId = row.filmSimulationId, size = 40.dp)
            }
        },
        supportingContent = {
            Column {
                Text(FilmSimulations.byId(row.filmSimulationId)?.label.orEmpty())
                Text(
                    text = row.statusText(),
                    color = when (row.status) {
                        // A duplicate is information, not an error: the recipe is safe, it is
                        // just already held. Only the name clash is worth a warning colour.
                        ImportStatus.NAME_WARNING -> MaterialTheme.colorScheme.error
                        else -> Color.Unspecified
                    },
                )
            }
        },
    ) {
        Text(
            text = "C${row.slot} · ${row.name}",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ImportRow.statusText(): String = when (status) {
    ImportStatus.NEW -> stringResource(R.string.import_status_new)
    ImportStatus.CONFIG_DUPLICATE ->
        stringResource(R.string.import_status_duplicate, existingName.orEmpty())

    ImportStatus.NAME_WARNING ->
        stringResource(R.string.import_status_name, existingName.orEmpty())
}

@Composable
private fun Note(text: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Composable
fun ImportRouteContent(
    onBack: () -> Unit,
    onDone: () -> Unit,
    contentPadding: PaddingValues,
) {
    val container = (LocalContext.current.applicationContext as FujiRecipesApp).container
    val viewModel: ImportViewModel = viewModel(factory = ImportViewModel.factory(container))
    val state by viewModel.state.collectAsStateWithLifecycle()

    ImportScreen(
        state = state,
        onBack = onBack,
        onConnect = viewModel::connect,
        onRead = viewModel::read,
        onToggle = viewModel::toggle,
        onImport = viewModel::import,
        onBackToReview = viewModel::backToReview,
        onDone = onDone,
        contentPadding = contentPadding,
    )
}

// ─── Previews ───────────────────────────────────────────────────────────────

private val previewRows = listOf(
    ImportRow(
        slot = 1,
        name = "1970s Summer",
        settings = mapOf("filmSimulation" to "nostalgic-negative"),
        status = ImportStatus.NEW,
        selected = true,
    ),
    ImportRow(
        slot = 2,
        name = "Slot C2",
        settings = mapOf("filmSimulation" to "classic-chrome"),
        status = ImportStatus.CONFIG_DUPLICATE,
        existingName = "Kodachrome 64",
        selected = false,
    ),
    ImportRow(
        slot = 4,
        name = "Acros Night",
        settings = mapOf("filmSimulation" to "acros"),
        status = ImportStatus.NAME_WARNING,
        existingName = "Acros Night",
        selected = true,
    ),
)

@Composable
private fun PreviewScreen(state: ImportUiState) {
    FujiTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            ImportScreen(
                state = state,
                onBack = {},
                onConnect = {}, onRead = {}, onToggle = {}, onImport = {},
                onBackToReview = {}, onDone = {},
                contentPadding = PaddingValues(0.dp),
            )
        }
    }
}

@Preview(name = "Import — needs a camera", showBackground = true)
@Composable
private fun ImportNeedsCameraPreview() =
    PreviewScreen(ImportUiState(stage = ImportStage.NeedsCamera))

@Preview(name = "Import — ready", showBackground = true)
@Composable
private fun ImportReadyPreview() =
    PreviewScreen(ImportUiState(stage = ImportStage.Ready, cameraModel = "X-T50"))

@Preview(name = "Import — reading", showBackground = true)
@Composable
private fun ImportReadingPreview() =
    PreviewScreen(ImportUiState(stage = ImportStage.Reading(3, 7)))

@Preview(name = "Import — review", showBackground = true)
@Preview(name = "Import — review, dark", showBackground = true, uiMode = 0x20)
@Composable
private fun ImportReviewPreview() =
    PreviewScreen(ImportUiState(stage = ImportStage.Review, rows = previewRows))

/** The camera holds nothing this library does not already have. */
@Preview(name = "Import — nothing new", showBackground = true)
@Composable
private fun ImportNothingNewPreview() = PreviewScreen(
    ImportUiState(
        stage = ImportStage.Review,
        rows = previewRows.map {
            it.copy(
                status = ImportStatus.CONFIG_DUPLICATE,
                existingName = "Kodachrome 64",
                selected = false,
            )
        },
    ),
)

/** A real answer, not a failure: the camera has no custom recipes saved. */
@Preview(name = "Import — empty camera", showBackground = true)
@Composable
private fun ImportEmptyPreview() = PreviewScreen(ImportUiState(stage = ImportStage.Review))

@Preview(name = "Import — done", showBackground = true)
@Composable
private fun ImportDonePreview() = PreviewScreen(ImportUiState(stage = ImportStage.Done(3)))

@Preview(name = "Import — no connection", showBackground = true)
@Composable
private fun ImportOfflinePreview() = PreviewScreen(
    ImportUiState(
        stage = ImportStage.Failed(
            "Saving needs a connection — reading the camera did not. Nothing was imported, " +
                "and your choices are still here to try again.",
        ),
        rows = previewRows,
    ),
)

private val SectionGap = 10.dp
