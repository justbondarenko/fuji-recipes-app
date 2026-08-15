package dev.bondarenko.fujirecipes.ui.photo

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.bondarenko.fujirecipes.FujiRecipesApp
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.data.photo.MatchResult
import dev.bondarenko.fujirecipes.data.photo.PhotoReadFailure
import dev.bondarenko.fujirecipes.data.photo.PhotoRecipe
import dev.bondarenko.fujirecipes.ui.common.SectionHeader
import dev.bondarenko.fujirecipes.ui.library.FilmSimBadge
import dev.bondarenko.fujirecipes.ui.library.LibraryPanel
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Read a recipe from a photo — FEAT-009 T-10.
 *
 * One question, one answer: *which of my recipes is this?* Everything on the screen is in
 * service of that, and the settings themselves are shown underneath rather than above,
 * because the name is what was asked for.
 */
@Composable
fun PhotoReaderScreen(
    state: PhotoReaderUiState,
    onChoosePhoto: () -> Unit,
    onOpenRecipe: (String) -> Unit,
    onSaveAsNew: () -> Unit,
    onReset: () -> Unit,
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
        item { SectionHeader(stringResource(R.string.photo_title)) }

        when (val stage = state.stage) {
            PhotoReaderStage.Empty -> item {
                LibraryPanel(
                    title = stringResource(R.string.photo_action_choose),
                    body = stringResource(R.string.photo_intro),
                    primaryLabel = stringResource(R.string.photo_action_choose),
                    onPrimary = onChoosePhoto,
                )
            }

            PhotoReaderStage.Reading -> item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Body(stringResource(R.string.photo_reading))
                }
            }

            is PhotoReaderStage.Failed -> item {
                val (title, body) = stage.reason.message()
                LibraryPanel(
                    title = title,
                    body = body,
                    primaryLabel = stringResource(R.string.photo_action_choose),
                    onPrimary = onChoosePhoto,
                )
            }

            is PhotoReaderStage.Result -> resultItems(
                recipe = stage.recipe,
                matches = stage.matches,
                onOpenRecipe = onOpenRecipe,
                onSaveAsNew = onSaveAsNew,
                onChoosePhoto = onChoosePhoto,
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.resultItems(
    recipe: PhotoRecipe,
    matches: MatchResult,
    onOpenRecipe: (String) -> Unit,
    onSaveAsNew: () -> Unit,
    onChoosePhoto: () -> Unit,
) {
    // The answer first. The settings are evidence for it and sit underneath.
    item { MatchPanel(matches, onOpenRecipe) }

    item {
        Button(onClick = onSaveAsNew, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.photo_action_save))
        }
    }

    item { SectionHeader(stringResource(R.string.photo_settings)) }

    recipe.cameraModel?.let { model ->
        item { Body(stringResource(R.string.photo_shot_on, model)) }
    }

    items(recipe.rawValues.entries.toList(), key = { it.key }) { (label, value) ->
        SettingRow(label = label, value = value, simulationId = recipe.settings["filmSimulation"] as? String)
    }

    item {
        OutlinedButton(onClick = onChoosePhoto, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.photo_action_another))
        }
    }
}

@Composable
private fun MatchPanel(matches: MatchResult, onOpenRecipe: (String) -> Unit) {
    val best = matches.best

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when {
            best == null -> {
                Panel(
                    title = stringResource(R.string.photo_match_none_title),
                    body = if (matches.recipesChecked == 0) {
                        stringResource(R.string.photo_match_empty_body)
                    } else {
                        stringResource(R.string.photo_match_none_body)
                    },
                )
            }

            best.isExact -> {
                Panel(
                    title = stringResource(R.string.photo_match_exact_title, best.recipe.name),
                    body = stringResource(R.string.photo_match_exact_body),
                    highlight = true,
                )
                Button(
                    onClick = { onOpenRecipe(best.recipe.id) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.photo_action_open_match, best.recipe.name))
                }
            }

            else -> {
                Panel(
                    title = stringResource(
                        R.string.photo_match_near_title,
                        best.recipe.name,
                        best.percentage,
                    ),
                    body = stringResource(R.string.photo_match_near_body),
                    highlight = true,
                )
                // What differs, named. This is the part that teaches you something.
                best.mismatches.forEach { difference ->
                    Body(
                        stringResource(
                            R.string.photo_difference,
                            difference.label,
                            difference.photoValue,
                            difference.savedValue,
                        ),
                    )
                }
                OutlinedButton(
                    onClick = { onOpenRecipe(best.recipe.id) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.photo_action_open_match, best.recipe.name))
                }
            }
        }
    }
}

@Composable
private fun SettingRow(label: String, value: String, simulationId: String?) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (label == "Film simulation") {
                FilmSimBadge(simulationId = simulationId, size = 28.dp)
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun PhotoReadFailure.message(): Pair<String, String> = when (this) {
    PhotoReadFailure.NOT_JPEG -> stringResource(R.string.photo_error_not_jpeg_title) to
        stringResource(R.string.photo_error_not_jpeg_body)

    PhotoReadFailure.TOO_LARGE -> stringResource(R.string.photo_error_too_large_title) to
        stringResource(R.string.photo_error_too_large_body)

    // These two are different facts and must not read the same (P5): one photo was stripped,
    // the other came from someone else's camera.
    PhotoReadFailure.NO_EXIF -> stringResource(R.string.photo_error_no_exif_title) to
        stringResource(R.string.photo_error_no_exif_body)

    PhotoReadFailure.NOT_FUJIFILM -> stringResource(R.string.photo_error_not_fuji_title) to
        stringResource(R.string.photo_error_not_fuji_body)

    PhotoReadFailure.UNREADABLE -> stringResource(R.string.photo_error_unreadable_title) to
        stringResource(R.string.photo_error_unreadable_body)
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
private fun Panel(title: String, body: String, highlight: Boolean = false) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (highlight) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (highlight) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = if (highlight) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
fun PhotoReaderRouteContent(
    onOpenRecipe: (String) -> Unit,
    onSaveAsNew: (prefill: String, name: String) -> Unit,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val container = (context.applicationContext as FujiRecipesApp).container

    val viewModel: PhotoReaderViewModel = viewModel(
        factory = PhotoReaderViewModel.factory(container) { uri ->
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(android.net.Uri.parse(uri))
                        ?.use { it.readBytes() }
                }.getOrNull()
            }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    /**
     * The system photo picker — **no storage permission**.
     *
     * `PickVisualMedia` hands back a one-shot read grant for the single image chosen, which is
     * both less to ask for and less to explain than `READ_MEDIA_IMAGES`.
     */
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let { viewModel.read(it.toString()) } }

    PhotoReaderScreen(
        state = state,
        onChoosePhoto = {
            picker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        },
        onOpenRecipe = onOpenRecipe,
        onSaveAsNew = {
            viewModel.prefillJson()?.let { onSaveAsNew(it, viewModel.suggestedName()) }
        },
        onReset = viewModel::reset,
        contentPadding = contentPadding,
    )
}
