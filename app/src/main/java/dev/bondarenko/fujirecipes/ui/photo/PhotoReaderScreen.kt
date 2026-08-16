package dev.bondarenko.fujirecipes.ui.photo

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.bondarenko.fujirecipes.FujiRecipesApp
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.data.fields.FilmSimulations
import dev.bondarenko.fujirecipes.data.photo.MatchResult
import dev.bondarenko.fujirecipes.data.photo.PhotoReadFailure
import dev.bondarenko.fujirecipes.data.photo.PhotoRecipe
import dev.bondarenko.fujirecipes.data.photo.RecipeMatch
import dev.bondarenko.fujirecipes.ui.common.FujiIconPanel
import dev.bondarenko.fujirecipes.ui.common.FujiLoadingIndicator
import dev.bondarenko.fujirecipes.ui.common.SectionHeader
import dev.bondarenko.fujirecipes.ui.library.FilmSimBadge
import dev.bondarenko.fujirecipes.ui.theme.TabularFigures
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
    when (val stage = state.stage) {
        PhotoReaderStage.Empty -> {
            EmptyPhotoReaderState(
                onChoosePhoto = onChoosePhoto,
                modifier = modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            )
        }

        PhotoReaderStage.Reading -> {
            ReadingPhotoState(
                modifier = modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            )
        }

        is PhotoReaderStage.Failed -> {
            FailedPhotoState(
                reason = stage.reason,
                onChoosePhoto = onChoosePhoto,
                modifier = modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            )
        }

        is PhotoReaderStage.Result -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp + contentPadding.calculateTopPadding(),
                    bottom = 24.dp + contentPadding.calculateBottomPadding(),
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item { SectionHeader(stringResource(R.string.photo_title)) }

                resultItems(
                    recipe = stage.recipe,
                    matches = stage.matches,
                    onOpenRecipe = onOpenRecipe,
                    onSaveAsNew = onSaveAsNew,
                    onChoosePhoto = onChoosePhoto,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EmptyPhotoReaderState(
    onChoosePhoto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FujiIconPanel(
        // The same glyph as the Read item in the toolbar.
        icon = painterResource(R.drawable.ic_image_search),
        shape = MaterialShapes.Pill.toShape(),
        title = stringResource(R.string.photo_title),
        body = stringResource(R.string.photo_intro),
        actionLabel = stringResource(R.string.photo_action_choose),
        onAction = onChoosePhoto,
        modifier = modifier,
    )
}

@Composable
private fun ReadingPhotoState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FujiLoadingIndicator(size = 36.dp)
            Text(
                text = stringResource(R.string.photo_reading),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun FailedPhotoState(
    reason: PhotoReadFailure,
    onChoosePhoto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (title, body) = reason.message()
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onChoosePhoto,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(stringResource(R.string.photo_action_choose))
            }
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
    val best = matches.best
    item {
        if (best != null) {
            MatchedRecipeCard(
                match = best,
                onOpenRecipe = onOpenRecipe,
                onSaveAsNew = onSaveAsNew,
            )
        } else {
            NoMatchCard(
                matches = matches,
                onSaveAsNew = onSaveAsNew,
            )
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
private fun MatchedRecipeCard(
    match: RecipeMatch,
    onOpenRecipe: (String) -> Unit,
    onSaveAsNew: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val recipe = match.recipe
    val shape = RoundedCornerShape(20.dp)

    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        tonalElevation = 1.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilmSimBadge(
                    simulationId = recipe.filmSimulationId,
                    size = 52.dp,
                    shape = CircleShape,
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = recipe.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = FilmSimulations.labelFor(recipe.filmSimulationId),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        if (recipe.rating > 0) {
                            RatingBadge(rating = recipe.rating)
                        }
                    }
                }
            }

            // Match description / differences
            if (match.isExact) {
                Text(
                    text = stringResource(R.string.photo_match_exact_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = stringResource(
                        R.string.photo_match_near_title,
                        recipe.name,
                        match.percentage,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )

                match.mismatches.forEach { difference ->
                    Text(
                        text = stringResource(
                            R.string.photo_difference,
                            difference.label,
                            difference.photoValue,
                            difference.savedValue,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Horizontally aligned action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onSaveAsNew,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.photo_action_save))
                }

                Button(
                    onClick = { onOpenRecipe(recipe.id) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.photo_action_view_recipe))
                }
            }
        }
    }
}

@Composable
private fun NoMatchCard(
    matches: MatchResult,
    onSaveAsNew: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        tonalElevation = 1.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.photo_match_none_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (matches.recipesChecked == 0) {
                    stringResource(R.string.photo_match_empty_body)
                } else {
                    stringResource(R.string.photo_match_none_body)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onSaveAsNew,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.photo_action_save))
            }
        }
    }
}

@Composable
private fun RatingBadge(
    rating: Int,
    modifier: Modifier = Modifier,
) {
    Badge(
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        ) {
            Text(
                text = rating.toString(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = TabularFigures,
                ),
            )
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = stringResource(R.string.rating_of_five, rating),
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(11.dp),
            )
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
