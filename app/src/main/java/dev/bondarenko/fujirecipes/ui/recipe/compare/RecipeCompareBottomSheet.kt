package dev.bondarenko.fujirecipes.ui.recipe.compare

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
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
import dev.bondarenko.fujirecipes.data.compare.RecipeComparisonGroup
import dev.bondarenko.fujirecipes.data.compare.RecipeComparisonRow
import dev.bondarenko.fujirecipes.data.fields.FieldGroup
import dev.bondarenko.fujirecipes.ui.common.FujiIconPanel
import dev.bondarenko.fujirecipes.ui.common.FujiLoadingIndicator
import dev.bondarenko.fujirecipes.ui.common.SectionHeader
import dev.bondarenko.fujirecipes.ui.recipe.RecipeHeader
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme
import dev.bondarenko.fujirecipes.ui.theme.TabularFigures

/**
 * Material 3 Expressive Modal Bottom Sheet for comparing two recipes side-by-side.
 *
 * Starts at 70% screen height (partially expanded) and expands to full height as user scrolls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeCompareBottomSheet(
    baseRecipeId: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    initialTargetRecipeId: String? = null,
    onNavigateToRecipe: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val container = (context.applicationContext as FujiRecipesApp).container
    val viewModel: RecipeCompareViewModel = viewModel(
        factory = RecipeCompareViewModel.factory(container, baseRecipeId, initialTargetRecipeId),
        key = "compare_$baseRecipeId",
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(baseRecipeId) {
        if (initialTargetRecipeId == null) {
            viewModel.onClearTargetRecipe()
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier,
    ) {
        RecipeCompareContent(
            state = state,
            onSelectTargetRecipe = viewModel::onSelectTargetRecipe,
            onClearTargetRecipe = viewModel::onClearTargetRecipe,
            onDifferencesOnlyChange = viewModel::onDifferencesOnlyChange,
            onNavigateToRecipe = { targetId ->
                onDismiss()
                onNavigateToRecipe?.invoke(targetId)
            },
            onDismiss = onDismiss,
        )
    }
}

/**
 * Content of the Recipe Compare Sheet.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RecipeCompareContent(
    state: RecipeCompareUiState,
    onSelectTargetRecipe: (String) -> Unit,
    onClearTargetRecipe: () -> Unit,
    onDifferencesOnlyChange: (Boolean) -> Unit,
    onNavigateToRecipe: ((String) -> Unit)? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center,
            ) {
                FujiLoadingIndicator()
            }
        }

        state.hasNoCandidates -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                FujiIconPanel(
                    icon = painterResource(R.drawable.ic_compare),
                    shape = MaterialShapes.Pill.toShape(),
                    title = stringResource(R.string.compare_title),
                    body = stringResource(R.string.compare_no_other_recipes),
                    actionLabel = stringResource(R.string.action_back),
                    onAction = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Selection stage: user picks which recipe to compare with
        state.isSelectingTarget && state.baseRecipe != null -> {
            RecipeTargetPicker(
                baseRecipe = state.baseRecipe,
                candidates = state.availableCandidates,
                onSelectTargetRecipe = onSelectTargetRecipe,
                modifier = modifier,
            )
        }

        // Comparison stage: face-to-face table comparison
        state.baseRecipe != null && state.targetRecipe != null -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Dual Column Header (Identical box layout on left and right)
                item(key = "header_cards") {
                    RecipeCompareHeaderCards(
                        baseRecipe = state.baseRecipe,
                        targetRecipe = state.targetRecipe,
                        candidates = state.availableCandidates,
                        onSelectTargetRecipe = onSelectTargetRecipe,
                    )
                }

                // Differences Only Filter Switch
                item(key = "diff_switch") {
                    DifferencesOnlyCard(
                        checked = state.differencesOnly,
                        totalDifferences = state.totalDifferences,
                        onCheckedChange = onDifferencesOnlyChange,
                    )
                }

                // Recipe Name Comparison Row
                item(key = "recipe_names_row") {
                    ComparisonParameterTile(
                        row = RecipeComparisonRow(
                            fieldId = "recipeName",
                            label = stringResource(R.string.compare_field_recipe),
                            valueA = state.baseRecipe.name,
                            valueB = state.targetRecipe.name,
                            isSame = state.baseRecipe.name == state.targetRecipe.name,
                            group = FieldGroup.SIMULATION,
                            isDefaultA = false,
                            isDefaultB = false,
                        ),
                    )
                }

                // Empty notice when differencesOnly is ON and all parameters match
                if (state.differencesOnly && state.groups.isEmpty()) {
                    item(key = "all_match") {
                        Text(
                            text = stringResource(R.string.compare_all_match),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                        )
                    }
                }

                // Grouped Comparison Sections
                state.groups.forEach { group ->
                    item(key = "group_${group.group.id}") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionHeader(group.group.label)
                            ComparisonGroupGrid(group = group)
                        }
                    }
                }

                // Link to go to target recipe at the very bottom in the right column
                if (onNavigateToRecipe != null) {
                    item(key = "footer_target_link") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Spacer(modifier = Modifier.weight(1f))
                            Spacer(modifier = Modifier.weight(1.3f))
                            FilledTonalButton(
                                onClick = { onNavigateToRecipe(state.targetRecipe.id) },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = stringResource(R.string.compare_open_target),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Initial target picker view: user selects which recipe from library to compare with.
 */
@Composable
private fun RecipeTargetPicker(
    baseRecipe: RecipeHeader,
    candidates: List<RecipeCandidate>,
    onSelectTargetRecipe: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.compare_select_recipe),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Compare “${baseRecipe.name}” with another recipe in your library.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(candidates, key = { it.id }) { candidate ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectTargetRecipe(candidate.id) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = candidate.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ) {
                                Text(
                                    text = candidate.filmSimulationLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                            if (candidate.rating > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = candidate.rating.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary,
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier
                                            .padding(start = 2.dp)
                                            .size(12.dp),
                                    )
                                }
                            }
                        }
                    }

                    Icon(
                        painter = painterResource(R.drawable.ic_compare),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/**
 * Side-by-side header cards for Base Recipe (Current) and Target Recipe (Selectable).
 *
 * Enforces strictly identical visual structure and equal dimensions using [IntrinsicSize.Max].
 */
@Composable
private fun RecipeCompareHeaderCards(
    baseRecipe: RecipeHeader,
    targetRecipe: RecipeHeader,
    candidates: List<RecipeCandidate>,
    onSelectTargetRecipe: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Base Recipe Card (Pinned on left)
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = stringResource(R.string.compare_current),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }

                    Text(
                        text = baseRecipe.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Text(
                    text = baseRecipe.filmSimulationLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        // Target Recipe Card (Right) — IDENTICAL structure
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
        ) {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(enabled = candidates.size > 1) { menuExpanded = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                        ) {
                            Text(
                                text = stringResource(R.string.compare_target),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }

                        Text(
                            text = targetRecipe.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Text(
                        text = targetRecipe.filmSimulationLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                candidates.forEach { candidate ->
                    val isSelected = candidate.id == targetRecipe.id
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = candidate.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = candidate.filmSimulationLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        trailingIcon = {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        },
                        onClick = {
                            onSelectTargetRecipe(candidate.id)
                            menuExpanded = false
                        },
                    )
                }
            }
        }
    }
}

/**
 * Filter card with switch to show only differing parameters.
 */
@Composable
private fun DifferencesOnlyCard(
    checked: Boolean,
    totalDifferences: Int,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.compare_differences_only),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.compare_differences_count,
                        totalDifferences,
                        totalDifferences,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (totalDifferences > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

/**
 * Renders all comparison rows for one [FieldGroup].
 */
@Composable
private fun ComparisonGroupGrid(
    group: RecipeComparisonGroup,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        group.rows.forEach { row ->
            ComparisonParameterTile(row = row)
        }
    }
}

/**
 * Individual row card comparing one field face-to-face.
 *
 * Layout:
 * [ Value A (Left) ]  [ Field Name (Center) ]  [ Value B (Right) ]
 *
 * When items match: subtle/dull color (`onSurfaceVariant` at 65% opacity), container `surfaceContainerLow`.
 * When items differ: prominent "black" color (`onSurface`, bold weight), container `surfaceContainer`.
 */
@Composable
private fun ComparisonParameterTile(
    row: RecipeComparisonRow,
    modifier: Modifier = Modifier,
) {
    val isSame = row.isSame

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSame) {
                MaterialTheme.colorScheme.surfaceContainerLow
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Value A (Base Recipe) — Left column
            Text(
                text = row.valueA,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFeatureSettings = TabularFigures,
                ),
                fontWeight = if (isSame) FontWeight.Normal else FontWeight.SemiBold,
                color = if (isSame) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            // Field Name — Center column (in place of "vs")
            Column(
                modifier = Modifier.weight(1.3f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                compareFieldIcon(row.fieldId)?.let { icon ->
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        tint = if (isSame) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    text = row.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isSame) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Value B (Target Recipe) — Right column
            Text(
                text = row.valueB,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFeatureSettings = TabularFigures,
                ),
                fontWeight = if (isSame) FontWeight.Normal else FontWeight.SemiBold,
                color = if (isSame) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@DrawableRes
private fun compareFieldIcon(id: String): Int? = when (id) {
    "recipeName" -> R.drawable.ic_label
    "filmSimulation" -> R.drawable.ic_camera_roll
    "dynamicRange", "dRangePriority" -> R.drawable.ic_contrast
    "highlightTone" -> R.drawable.ic_tonality
    "shadowTone" -> R.drawable.ic_tonality_2
    "color" -> R.drawable.ic_palette
    "sharpness" -> R.drawable.ic_details
    "highIsoNR" -> R.drawable.ic_deblur
    "clarity" -> R.drawable.ic_diamond
    "grainEffect" -> R.drawable.ic_grain
    "grainSize" -> R.drawable.ic_transition_dissolve
    "colorChromeEffect", "colorChromeFxBlue" -> R.drawable.ic_colors
    "whiteBalance", "colorTemperature" -> R.drawable.ic_wb_auto
    "wbShift", "wbShiftRed", "wbShiftBlue" -> R.drawable.ic_discover_tune
    "exposureCompensation" -> R.drawable.ic_exposure
    else -> null
}

// ─── Previews ───────────────────────────────────────────────────────────────

private val previewBaseHeader = RecipeHeader(
    id = "a",
    name = "Kodachrome 64",
    filmSimulationId = "classic-chrome",
    filmSimulationLabel = "Classic Chrome",
    rating = 5,
    tags = listOf("street", "warm"),
    notes = "",
)

private val previewTargetHeader = RecipeHeader(
    id = "b",
    name = "Portra 400",
    filmSimulationId = "classic-chrome",
    filmSimulationLabel = "Classic Chrome",
    rating = 4,
    tags = listOf("portrait"),
    notes = "",
)

private val previewCandidates = listOf(
    RecipeCandidate("b", "Portra 400", "Classic Chrome", 4),
    RecipeCandidate("c", "Acros Night", "Acros+R", 5),
)

private val previewGroups = listOf(
    RecipeComparisonGroup(
        FieldGroup.SIMULATION,
        listOf(
            RecipeComparisonRow("filmSimulation", "Film simulation", "Classic Chrome", "Classic Chrome", true, FieldGroup.SIMULATION, false, false),
            RecipeComparisonRow("dynamicRange", "Dynamic range", "DR400", "DR200", false, FieldGroup.SIMULATION, false, false),
        ),
    ),
    RecipeComparisonGroup(
        FieldGroup.TONE,
        listOf(
            RecipeComparisonRow("highlightTone", "Highlight tone", "+1.5", "-1.0", false, FieldGroup.TONE, false, false),
            RecipeComparisonRow("shadowTone", "Shadow tone", "0", "0", true, FieldGroup.TONE, true, true),
            RecipeComparisonRow("color", "Color", "+2", "+1", false, FieldGroup.TONE, false, false),
            RecipeComparisonRow("sharpness", "Sharpness", "0", "0", true, FieldGroup.TONE, true, true),
        ),
    ),
)

@Preview(name = "Compare — Table (Light)", showBackground = true, heightDp = 800)
@Preview(name = "Compare — Table (Dark)", showBackground = true, uiMode = 0x20, heightDp = 800)
@Composable
private fun RecipeComparePreview() {
    FujiTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            RecipeCompareContent(
                state = RecipeCompareUiState(
                    isLoading = false,
                    baseRecipe = previewBaseHeader,
                    targetRecipe = previewTargetHeader,
                    availableCandidates = previewCandidates,
                    groups = previewGroups,
                    differencesOnly = false,
                    totalDifferences = 3,
                    totalFields = 6,
                ),
                onSelectTargetRecipe = {},
                onClearTargetRecipe = {},
                onDifferencesOnlyChange = {},
                onDismiss = {},
            )
        }
    }
}

@Preview(name = "Compare — Picker", showBackground = true, heightDp = 600)
@Composable
private fun RecipeComparePickerPreview() {
    FujiTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            RecipeCompareContent(
                state = RecipeCompareUiState(
                    isLoading = false,
                    baseRecipe = previewBaseHeader,
                    targetRecipe = null,
                    availableCandidates = previewCandidates,
                ),
                onSelectTargetRecipe = {},
                onClearTargetRecipe = {},
                onDifferencesOnlyChange = {},
                onDismiss = {},
            )
        }
    }
}
