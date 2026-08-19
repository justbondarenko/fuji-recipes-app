package dev.bondarenko.fujirecipes.ui.cleanup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.bondarenko.fujirecipes.FujiRecipesApp
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.data.cleanup.ExactDuplicateGroup
import dev.bondarenko.fujirecipes.data.cleanup.FieldDifference
import dev.bondarenko.fujirecipes.data.cleanup.SimilarRecipePair
import dev.bondarenko.fujirecipes.data.fields.FilmSimulations
import dev.bondarenko.fujirecipes.data.model.Recipe
import dev.bondarenko.fujirecipes.ui.common.FujiLoadingIndicator
import dev.bondarenko.fujirecipes.ui.common.SectionHeader
import dev.bondarenko.fujirecipes.ui.theme.TabularFigures
import dev.bondarenko.fujirecipes.ui.theme.icons.ArrowForward
import dev.bondarenko.fujirecipes.ui.theme.icons.CleaningServices
import dev.bondarenko.fujirecipes.ui.theme.icons.Delete
import dev.bondarenko.fujirecipes.ui.theme.icons.FujiIcons
import dev.bondarenko.fujirecipes.ui.theme.icons.StarRate
import dev.bondarenko.fujirecipes.ui.theme.icons.StarShine

@Composable
fun CleanupScreen(
    state: CleanupUiState,
    onFindDuplicates: () -> Unit,
    onSelectKeep: (groupId: String, recipeId: String) -> Unit,
    onDeleteGroupDuplicates: (groupId: String) -> Unit,
    onDeleteAllDuplicates: () -> Unit,
    onOpenRecipe: (String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    var groupPendingDelete by remember { mutableStateOf<ExactDuplicateGroup?>(null) }
    var confirmDeleteAll by remember { mutableStateOf(false) }

    when (val stage = state.stage) {
        CleanupStage.Initial -> {
            InitialCenteredCleanupScreen(
                onFindDuplicates = onFindDuplicates,
                modifier = modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            )
        }

        CleanupStage.Scanning -> {
            ScanningCleanupScreen(
                modifier = modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            )
        }

        is CleanupStage.Results -> {
            val scanResult = stage.result

            if (scanResult.isEmpty) {
                CleanLibraryScreen(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                )
            } else {
                LazyColumn(
                    modifier = modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp + contentPadding.calculateTopPadding(),
                        bottom = 24.dp + contentPadding.calculateBottomPadding(),
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Header with title and Scan again button (no subtitle)
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.cleanup_title),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )

                            OutlinedButton(
                                onClick = onFindDuplicates,
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.cleanup_action_scan_again),
                                    maxLines = 1,
                                )
                            }
                        }
                    }

                    // ─── Section 1: 100% Match Duplicates ────────────────────
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        SectionHeader(stringResource(R.string.cleanup_exact_title))
                        Text(
                            text = stringResource(R.string.cleanup_exact_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                        )
                    }

                    if (scanResult.exactGroups.isEmpty()) {
                        item {
                            EmptySectionCard(message = stringResource(R.string.cleanup_exact_none))
                        }
                    } else {
                        items(scanResult.exactGroups, key = { it.id }) { group ->
                            val keepId = stage.selectedKeepMap[group.id] ?: group.defaultKeepId
                            ExactDuplicateGroupCard(
                                group = group,
                                selectedKeepId = keepId,
                                isDeleting = stage.isDeleting,
                                onSelectKeep = { recipeId -> onSelectKeep(group.id, recipeId) },
                                onDeleteDuplicates = { groupPendingDelete = group },
                                onOpenRecipe = onOpenRecipe,
                            )
                        }

                        if (scanResult.exactGroups.size > 1) {
                            item {
                                OutlinedButton(
                                    onClick = { confirmDeleteAll = true },
                                    enabled = !stage.isDeleting,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error,
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Icon(
                                        imageVector = FujiIcons.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        stringResource(
                                            R.string.cleanup_delete_all_duplicates,
                                            scanResult.totalDuplicateRecipesCount,
                                        ),
                                    )
                                }
                            }
                        }
                    }

                    // ─── Section 2: Highly Similar Recipes ───────────────────
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        SectionHeader(stringResource(R.string.cleanup_similar_title))
                        Text(
                            text = stringResource(R.string.cleanup_similar_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                        )
                    }

                    if (scanResult.similarPairs.isEmpty()) {
                        item {
                            EmptySectionCard(message = stringResource(R.string.cleanup_similar_none))
                        }
                    } else {
                        items(
                            scanResult.similarPairs,
                            key = { "${it.recipeA.id}_${it.recipeB.id}" },
                        ) { pair ->
                            SimilarRecipePairCard(
                                pair = pair,
                                onOpenRecipe = onOpenRecipe,
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onFindDuplicates,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = FujiIcons.CleaningServices,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.cleanup_action_scan_again))
                        }
                    }
                }
            }
        }
    }

    // Confirmation dialog for deleting duplicate group
    groupPendingDelete?.let { group ->
        val duplicatesCount = group.recipes.size - 1
        AlertDialog(
            onDismissRequest = { groupPendingDelete = null },
            title = { Text(stringResource(R.string.cleanup_delete_confirm_title)) },
            text = { Text(stringResource(R.string.cleanup_delete_confirm_body, duplicatesCount)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteGroupDuplicates(group.id)
                        groupPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.cleanup_delete_group_duplicates, duplicatesCount))
                }
            },
            dismissButton = {
                TextButton(onClick = { groupPendingDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    // Confirmation dialog for deleting all duplicates
    if (confirmDeleteAll) {
        val totalDuplicates = (state.stage as? CleanupStage.Results)?.result?.totalDuplicateRecipesCount ?: 0
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            title = { Text(stringResource(R.string.cleanup_delete_confirm_title)) },
            text = { Text(stringResource(R.string.cleanup_delete_confirm_body, totalDuplicates)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAllDuplicates()
                        confirmDeleteAll = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.cleanup_delete_all_duplicates, totalDuplicates))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteAll = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

/**
 * Centered initial screen with Square container (72dp) containing CleaningServices icon (36dp),
 * Title, Subtitle (80% width max), and CTA Button.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun InitialCenteredCleanupScreen(
    onFindDuplicates: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(MaterialShapes.Square.toShape())
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = FujiIcons.CleaningServices,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(36.dp),
                )
            }

            Text(
                text = stringResource(R.string.cleanup_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = stringResource(R.string.cleanup_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(0.8f),
            )

            Button(
                onClick = onFindDuplicates,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(stringResource(R.string.cleanup_action_find))
            }
        }
    }
}

@Composable
private fun ScanningCleanupScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            FujiLoadingIndicator(size = 64.dp)
            Text(
                text = stringResource(R.string.cleanup_scanning),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * Centered clean library screen with bare StarShine icon, Title, Subtitle (80% width max).
 * No scan again button is rendered in this state.
 */
@Composable
private fun CleanLibraryScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = FujiIcons.StarShine,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp),
            )

            Text(
                text = stringResource(R.string.cleanup_clean_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = stringResource(R.string.cleanup_clean_body),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(0.8f),
            )
        }
    }
}

@Composable
private fun EmptySectionCard(message: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
    }
}

/**
 * Card representing a group of 100% identical recipes.
 */
@Composable
private fun ExactDuplicateGroupCard(
    group: ExactDuplicateGroup,
    selectedKeepId: String,
    isDeleting: Boolean,
    onSelectKeep: (String) -> Unit,
    onDeleteDuplicates: () -> Unit,
    onOpenRecipe: (String) -> Unit,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${group.recipes.size} Identical Recipes",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = FilmSimulations.labelFor(group.recipes.firstOrNull()?.filmSimulationId),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                group.recipes.forEach { recipe ->
                    val isSelectedToKeep = recipe.id == selectedKeepId
                    ExactRecipeRow(
                        recipe = recipe,
                        isKeep = isSelectedToKeep,
                        onSelect = { onSelectKeep(recipe.id) },
                        onOpen = { onOpenRecipe(recipe.id) },
                    )
                }
            }

            val duplicatesCount = group.recipes.size - 1
            Button(
                onClick = onDeleteDuplicates,
                enabled = !isDeleting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = FujiIcons.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.cleanup_delete_group_duplicates, duplicatesCount))
            }
        }
    }
}

@Composable
private fun ExactRecipeRow(
    recipe: Recipe,
    isKeep: Boolean,
    onSelect: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isKeep) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = BorderStroke(
            1.dp,
            if (isKeep) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RadioButton(
                selected = isKeep,
                onClick = onSelect,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (recipe.rating > 0) {
                        CleanupRatingBadge(rating = recipe.rating)
                    }
                    if (recipe.tags.isNotEmpty()) {
                        Text(
                            text = recipe.tags.take(2).joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Badge(
                containerColor = if (isKeep) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                contentColor = if (isKeep) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
            ) {
                Text(
                    text = if (isKeep) stringResource(R.string.cleanup_keep_badge) else stringResource(R.string.cleanup_remove_badge),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                )
            }

            IconButton(onClick = onOpen, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = FujiIcons.ArrowForward,
                    contentDescription = stringResource(R.string.action_edit),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/**
 * Card representing a pair of highly similar recipes with 1-3 differences.
 */
@Composable
private fun SimilarRecipePairCard(
    pair: SimilarRecipePair,
    onOpenRecipe: (String) -> Unit,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${pair.differences.size} Difference${if (pair.differences.size > 1) "s" else ""}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = FilmSimulations.labelFor(pair.recipeA.filmSimulationId),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Side by side recipe header buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenRecipe(pair.recipeA.id) },
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = pair.recipeA.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "View recipe →",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenRecipe(pair.recipeB.id) },
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = pair.recipeB.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "View recipe →",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Differences list
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                pair.differences.forEach { diff ->
                    DifferenceRow(diff = diff)
                }
            }
        }
    }
}

@Composable
private fun DifferenceRow(
    diff: FieldDifference,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = diff.label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = "${diff.valueA}  vs  ${diff.valueB}",
                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = TabularFigures),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun CleanupRatingBadge(rating: Int, modifier: Modifier = Modifier) {
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
                imageVector = FujiIcons.StarRate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(10.dp),
            )
        }
    }
}

@Composable
fun CleanupRouteContent(
    onOpenRecipe: (String) -> Unit,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val container = (context.applicationContext as FujiRecipesApp).container
    val viewModel: CleanupViewModel = viewModel(factory = CleanupViewModel.factory(container))
    val state by viewModel.state.collectAsStateWithLifecycle()

    CleanupScreen(
        state = state,
        onFindDuplicates = { viewModel.findDuplicates(minDelayMs = 3000L) },
        onSelectKeep = viewModel::selectKeep,
        onDeleteGroupDuplicates = viewModel::deleteDuplicatesForGroup,
        onDeleteAllDuplicates = viewModel::deleteAllDuplicates,
        onOpenRecipe = onOpenRecipe,
        contentPadding = contentPadding,
    )
}
