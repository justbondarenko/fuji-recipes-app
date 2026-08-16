package dev.bondarenko.fujirecipes.ui.importing

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import dev.bondarenko.fujirecipes.ui.common.DownArrow
import androidx.compose.material3.toShape
import androidx.compose.material3.ListItemDefaults
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
import dev.bondarenko.fujirecipes.data.importing.FileRow
import dev.bondarenko.fujirecipes.data.importing.FileRowStatus
import dev.bondarenko.fujirecipes.data.importing.Resolution
import dev.bondarenko.fujirecipes.ui.common.FujiIconPanel
import dev.bondarenko.fujirecipes.ui.common.FujiLoadingIndicator
import dev.bondarenko.fujirecipes.ui.library.FilmSimBadge
import dev.bondarenko.fujirecipes.ui.library.LibraryPanel
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Import from a file — FEAT-012.
 *
 * The same sequence as the camera import (`ImportScreen`): choose, review, import. What is
 * different is the review — a file carries ids, so a row can collide with a recipe the library
 * already has, and SF-012 says that collision is the user's decision rather than the app's.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FileImportScreen(
    state: FileImportUiState,
    onBack: () -> Unit,
    onChoose: () -> Unit,
    onResolve: (Int, Resolution) -> Unit,
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
                    text = stringResource(R.string.file_import_title),
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
            // Tight, so the file rows read as one segmented block; other items ask for space.
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            when (val stage = state.stage) {
                FileImportStage.Ready -> item {
                    // Same arrow-down as importing from the camera: both bring recipes in,
                    // and only the source differs.
                    FujiIconPanel(
                        icon = painterResource(R.drawable.ic_file_save),
                        shape = DownArrow.toShape(),
                        title = stringResource(R.string.file_import_ready_title),
                        body = stringResource(R.string.file_import_ready_body),
                        actionLabel = stringResource(R.string.file_import_action_choose),
                        onAction = onChoose,
                        modifier = Modifier.fillParentMaxSize(),
                    )
                }

                FileImportStage.Reading -> item {
                    Waiting(stringResource(R.string.file_import_reading))
                }

                FileImportStage.Review -> reviewItems(state, onResolve, onImport, onChoose)

                FileImportStage.Importing -> item {
                    Waiting(stringResource(R.string.file_import_importing))
                }

                is FileImportStage.Done -> doneItems(stage, state, onDone, onChoose)

                is FileImportStage.Rejected -> item {
                    LibraryPanel(
                        title = stringResource(R.string.file_import_rejected_title),
                        body = stage.message,
                        primaryLabel = stringResource(R.string.file_import_action_choose_another),
                        onPrimary = onChoose,
                    )
                }

                is FileImportStage.Failed -> item {
                    LibraryPanel(
                        title = stringResource(R.string.file_import_failed_title),
                        body = stage.message,
                        primaryLabel = stringResource(R.string.file_import_action_back_to_review),
                        onPrimary = onBackToReview,
                    )
                }
            }
        }
    }
}

private fun LazyListScope.reviewItems(
    state: FileImportUiState,
    onResolve: (Int, Resolution) -> Unit,
    onImport: () -> Unit,
    onChoose: () -> Unit,
) {
    val summary = state.summary

    item {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = state.fileName?.let {
                    stringResource(R.string.file_import_summary, it, summary.total)
                } ?: stringResource(R.string.file_import_summary_untitled, summary.total),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = countsLine(state),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (summary.undecided > 0) {
        item { Note(stringResource(R.string.file_import_undecided, summary.undecided)) }
    }

    itemsIndexed(state.rows, key = { _, it -> it.index }) { index, row ->
        FileRowCard(
            row = row,
            resolution = state.resolutions[row.index],
            index = index,
            count = state.rows.size,
            onResolve = { onResolve(row.index, it) },
        )
    }

    item {
        Button(
            onClick = onImport,
            enabled = state.canImport,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (summary.importing > 0) {
                    stringResource(R.string.file_import_action_import, summary.importing)
                } else {
                    stringResource(R.string.file_import_action_import_none)
                },
            )
        }
    }

    item {
        OutlinedButton(onClick = onChoose, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.file_import_action_choose_another))
        }
    }
}

private fun LazyListScope.doneItems(
    stage: FileImportStage.Done,
    state: FileImportUiState,
    onDone: () -> Unit,
    onChoose: () -> Unit,
) {
    // The server's own refusals, before the summary panel: SF-014 says an invalid recipe is
    // named rather than swallowed, and that holds when the server is the one that found it.
    if (state.serverFailures.isNotEmpty()) {
        items(state.serverFailures) { failure ->
            Note(
                stringResource(
                    R.string.file_import_server_failure,
                    (failure.index ?: 0) + 1,
                    failure.path ?: "",
                    failure.message,
                ),
            )
        }
    }

    item {
        LibraryPanel(
            title = stringResource(R.string.file_import_done_title, stage.imported),
            body = buildString {
                append(stringResource(R.string.file_import_done_body))
                if (stage.replaced > 0 || stage.skipped > 0) {
                    append(" ")
                    append(
                        stringResource(
                            R.string.file_import_done_detail,
                            stage.replaced,
                            stage.skipped,
                        ),
                    )
                }
                if (state.serverFailures.isNotEmpty()) {
                    append(" ")
                    append(
                        stringResource(
                            R.string.file_import_done_failed,
                            state.serverFailures.size,
                        ),
                    )
                }
            },
            primaryLabel = stringResource(R.string.action_done),
            onPrimary = onDone,
            secondaryLabel = stringResource(R.string.file_import_action_choose_another),
            onSecondary = onChoose,
        )
    }
}

/**
 * The breakdown, with the empty categories left out.
 *
 * A file with nothing wrong in it should not have to say "0 that cannot be imported" — a zero
 * reads as a category the reader has to check rather than one that does not apply.
 */
@Composable
private fun countsLine(state: FileImportUiState): String {
    val summary = state.summary

    return listOfNotNull(
        summary.new.takeIf { it > 0 }?.let { stringResource(R.string.file_import_count_new, it) },
        summary.undecided.takeIf { it > 0 }
            ?.let { stringResource(R.string.file_import_count_decide, it) },
        summary.warnings.takeIf { it > 0 }
            ?.let { stringResource(R.string.file_import_count_warning, it) },
        summary.invalid.takeIf { it > 0 }
            ?.let { stringResource(R.string.file_import_count_invalid, it) },
    ).joinToString(" · ")
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FileRowCard(
    row: FileRow,
    resolution: Resolution?,
    index: Int,
    count: Int,
    onResolve: (Resolution) -> Unit,
) {
    ListItem(
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = Modifier.fillMaxWidth(),
        leadingContent = { FilmSimBadge(simulationId = row.filmSimulationId, size = 40.dp) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(FilmSimulations.byId(row.filmSimulationId)?.label.orEmpty())
                Text(
                    text = row.statusText(),
                    color = when (row.status) {
                        // Invalid is the only status that costs the user a recipe. A
                        // duplicate or a name clash is information.
                        FileRowStatus.INVALID -> MaterialTheme.colorScheme.error
                        else -> Color.Unspecified
                    },
                )
                // SF-014: the failing field path, because "invalid" alone cannot be acted on.
                row.errors.forEach { problem ->
                    Text(
                        text = listOfNotNull(problem.fieldId.takeIf { it.isNotEmpty() }, problem.message)
                            .joinToString(" — "),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                row.source?.let {
                    Text(stringResource(R.string.file_import_from_entry, it))
                }

                // The resolutions live inside the row because each conflict is decided on its
                // own — a screen-level control could not say which file it applied to.
                if (row.resolutionOptions.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        row.resolutionOptions.forEach { option ->
                            FilterChip(
                                selected = resolution == option,
                                onClick = { onResolve(option) },
                                label = { Text(option.label()) },
                            )
                        }
                    }
                }
            }
        },
    ) {
        Text(text = row.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Resolution.label(): String = when (this) {
    Resolution.SKIP -> stringResource(R.string.file_import_resolution_skip)
    Resolution.REPLACE -> stringResource(R.string.file_import_resolution_replace)
    Resolution.KEEP_BOTH -> stringResource(R.string.file_import_resolution_keep_both)
}

@Composable
private fun FileRow.statusText(): String = when (status) {
    FileRowStatus.NEW -> stringResource(R.string.file_import_status_new)
    FileRowStatus.CONFLICT ->
        stringResource(R.string.file_import_status_conflict, existingName.orEmpty())

    FileRowStatus.CONFIG_DUPLICATE ->
        stringResource(R.string.file_import_status_duplicate, existingName.orEmpty())

    FileRowStatus.NAME_WARNING ->
        stringResource(R.string.file_import_status_name, existingName.orEmpty())

    FileRowStatus.INVALID -> stringResource(R.string.file_import_status_invalid)
}

@Composable
private fun Waiting(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FujiLoadingIndicator(size = 20.dp)
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun Note(text: String) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
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

/**
 * The picker, and reading what it hands back.
 *
 * `OpenDocument` rather than `GetContent`: it returns a document the app can re-open, and the
 * MIME list is a filter rather than a restriction — a `.zip` served as
 * `application/octet-stream` by one file provider still has to be choosable, which is why the
 * wildcard is in the list and why the format is decided by the bytes rather than the name.
 */
@Composable
fun FileImportRouteContent(
    onBack: () -> Unit,
    onDone: () -> Unit,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val container = (context.applicationContext as FujiRecipesApp).container

    val viewModel: FileImportViewModel = viewModel(
        factory = FileImportViewModel.factory(container) { uri ->
            withContext(Dispatchers.IO) {
                runCatching {
                    val parsed = android.net.Uri.parse(uri)
                    val bytes = context.contentResolver.openInputStream(parsed)
                        ?.use { it.readBytes() } ?: return@runCatching null

                    ChosenFile(name = context.contentResolver.displayName(parsed), bytes = bytes)
                }.getOrNull()
            }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { viewModel.choose(it.toString()) } }

    FileImportScreen(
        state = state,
        onBack = onBack,
        onChoose = {
            picker.launch(arrayOf("application/json", "application/zip", "text/*", "*/*"))
        },
        onResolve = viewModel::resolve,
        onImport = viewModel::import,
        onBackToReview = viewModel::backToReview,
        onDone = onDone,
        contentPadding = contentPadding,
    )
}

/** The name the provider shows for a document, falling back to the last path segment. */
private fun android.content.ContentResolver.displayName(uri: android.net.Uri): String =
    runCatching {
        query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
    }.getOrNull() ?: uri.lastPathSegment ?: "the file"

// ─── Previews ───────────────────────────────────────────────────────────────

@Composable
private fun PreviewScreen(state: FileImportUiState) {
    FujiTheme {
        FileImportScreen(
            state = state,
            onBack = {},
            onChoose = {},
            onResolve = { _, _ -> },
            onImport = {},
            onBackToReview = {},
            onDone = {},
            contentPadding = PaddingValues(),
        )
    }
}

@Preview(name = "File import — ready", showBackground = true)
@Composable
private fun FileImportReadyPreview() = PreviewScreen(FileImportUiState())

@Preview(name = "File import — review", showBackground = true, heightDp = 900)
@Preview(name = "File import — review, dark", showBackground = true, heightDp = 900, uiMode = 0x20)
@Composable
private fun FileImportReviewPreview() = PreviewScreen(
    FileImportUiState(
        stage = FileImportStage.Review,
        fileName = "fuji-recipes-2026-08-15.json",
        rows = previewRows,
    ),
)

@Preview(name = "File import — refused", showBackground = true)
@Composable
private fun FileImportRejectedPreview() = PreviewScreen(
    FileImportUiState(
        stage = FileImportStage.Rejected(
            dev.bondarenko.fujirecipes.data.importing.RejectionReason.FUTURE_VERSION,
            "This export came from a newer version of the app. It is version 2; this build " +
                "reads up to version 1.",
        ),
    ),
)

@Preview(name = "File import — done", showBackground = true)
@Composable
private fun FileImportDonePreview() = PreviewScreen(
    FileImportUiState(stage = FileImportStage.Done(imported = 3, skipped = 1, replaced = 1)),
)

private val previewRows: List<FileRow>
    get() {
        fun recipe(name: String, simulation: String) = kotlinx.serialization.json.buildJsonObject {
            put("name", kotlinx.serialization.json.JsonPrimitive(name))
            put(
                "settings",
                kotlinx.serialization.json.buildJsonObject {
                    put("filmSimulation", kotlinx.serialization.json.JsonPrimitive(simulation))
                },
            )
        }

        return listOf(
            FileRow(
                index = 0,
                recipe = recipe("Kodachrome 64", "classic-chrome"),
                name = "Kodachrome 64",
                status = FileRowStatus.NEW,
            ),
            FileRow(
                index = 1,
                recipe = recipe("Portra 400", "astia"),
                name = "Portra 400",
                status = FileRowStatus.CONFLICT,
                existingName = "Portra 400",
            ),
            FileRow(
                index = 2,
                source = "nested/acros.json",
                recipe = recipe("Acros Night", "acros"),
                name = "Acros Night",
                status = FileRowStatus.CONFIG_DUPLICATE,
                existingName = "Acros Night",
            ),
            FileRow(
                index = 3,
                recipe = recipe("", "eterna"),
                name = "(no name)",
                status = FileRowStatus.INVALID,
                errors = listOf(
                    dev.bondarenko.fujirecipes.data.fields.RecipeValidation.Problem(
                        "name",
                        "is required",
                    ),
                ),
            ),
        )
    }
