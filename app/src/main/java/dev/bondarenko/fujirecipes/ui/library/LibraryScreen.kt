package dev.bondarenko.fujirecipes.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.bondarenko.fujirecipes.FujiRecipesApp
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.core.net.ApiError
import dev.bondarenko.fujirecipes.data.library.LibraryFilters
import dev.bondarenko.fujirecipes.data.library.SortId
import dev.bondarenko.fujirecipes.camera.canWrite
import dev.bondarenko.fujirecipes.ui.camera.WriteSheetHost
import dev.bondarenko.fujirecipes.ui.recipe.RecipeViewBottomSheet
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme
import dev.bondarenko.fujirecipes.ui.theme.TabularFigures

/**
 * The list — FEAT-001 T-19, T-20, T-22.
 *
 * Takes state and lambdas, never a ViewModel, so every state below previews and tests
 * without a graph. [LibraryRoute] does the wiring.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onSearchChange: (String) -> Unit,
    onSortChange: (SortId) -> Unit,
    onFiltersChange: (LibraryFilters) -> Unit,
    onClearSearchAndFilters: () -> Unit,
    onRefresh: () -> Unit,
    onOpenRecipe: (String) -> Unit,
    onEditRecipe: (String) -> Unit,
    onDeleteRecipe: (String) -> Unit,
    onCreateRecipe: () -> Unit,
    onOpenConnection: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    canWriteToCamera: Boolean = false,
) {
    // Which row is slid open, owned here rather than by each row.
    var openRowId by rememberSaveable { mutableStateOf<String?>(null) }
    // Which recipe is open in the Material 3 Bottom Sheet
    var activeRecipeId by rememberSaveable { mutableStateOf<String?>(null) }
    // Which recipe is pending confirmation for deletion from swipe action
    var recipePendingDelete by remember { mutableStateOf<RecipeCardModel?>(null) }
    // Which recipe is currently open in the Write to Camera sheet
    var writeRecipeId by rememberSaveable { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // The shell's inset already reserves the floating bar's height, so the list
            // scrolls under it and the last card can still clear it.
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp + contentPadding.calculateTopPadding(),
                bottom = 12.dp + contentPadding.calculateBottomPadding(),
            ),
            // 💡 GAP BETWEEN ROWS — kept small so the segmented corners still read as one
            //    block; past about 8dp they stop looking joined.
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            when {
                // Nothing on screen and a failure: the error *is* the screen. Crucially a
                // 403 lands here rather than rendering as an empty library.
                state.isBlockingError -> item {
                    LibraryErrorPanel(
                        error = state.error!!,
                        onRetry = onRefresh,
                        onOpenConnection = onOpenConnection,
                    )
                }

                // fillParentMaxSize so the indicator centres against the viewport, not against
                // its own height at the top of the list.
                !state.hasLoaded -> item { LibraryLoading(Modifier.fillParentMaxSize()) }

                state.isEmptyLibrary -> item {
                    LibraryPanel(
                        title = stringResource(R.string.empty_library_title),
                        body = stringResource(R.string.empty_library_body),
                        primaryLabel = stringResource(R.string.action_create_recipe),
                        onPrimary = onCreateRecipe,
                    )
                }

                else -> {
                    item {
                        Column {
                            LibraryToolbar(
                                state = state,
                                onSearchChange = onSearchChange,
                                onSortChange = onSortChange,
                                onFiltersChange = onFiltersChange,
                                onClearSearchAndFilters = onClearSearchAndFilters,
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                    }

                    if (state.hasNoMatches) {
                        item {
                            LibraryPanel(
                                title = stringResource(R.string.no_matches_title),
                                body = pluralStringResource(
                                    R.plurals.no_matches_body,
                                    state.totalCount,
                                    state.totalCount,
                                    ),
                                primaryLabel = stringResource(R.string.action_clear_filters),
                                onPrimary = onClearSearchAndFilters,
                            )
                        }
                    } else {
                        itemsIndexed(state.visible, key = { _, recipe -> recipe.id }) { index, recipe ->
                            SwipeActionsRow(
                                isOpen = openRowId == recipe.id,
                                // One at a time: two rows open at once is how a delete gets
                                // pressed on the wrong recipe.
                                onOpenChange = { open ->
                                    openRowId = if (open) recipe.id else null
                                },
                                actions = {
                                    // Right to left as specified: delete is furthest from the
                                    // edge, edit nearest it.
                                    SwipeAction(
                                        icon = rememberVectorPainter(Icons.Filled.Delete),
                                        label = stringResource(R.string.action_delete),
                                        onClick = { recipePendingDelete = recipe },
                                        position = ButtonGroupPosition.Start,
                                        container = MaterialTheme.colorScheme.errorContainer,
                                        content = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                    val writeTooltipState = rememberTooltipState(isPersistent = true)
                                    TooltipBox(
                                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                        tooltip = {
                                            PlainTooltip {
                                                Text(stringResource(R.string.camera_not_connected_tooltip))
                                            }
                                        },
                                        state = writeTooltipState,
                                    ) {
                                        SwipeAction(
                                            icon = painterResource(R.drawable.ic_photo_camera),
                                            label = stringResource(R.string.action_write),
                                            onClick = {
                                                if (canWriteToCamera) {
                                                    openRowId = null
                                                    writeRecipeId = recipe.id
                                                } else {
                                                    coroutineScope.launch {
                                                        writeTooltipState.show()
                                                    }
                                                }
                                            },
                                            position = ButtonGroupPosition.Middle,
                                            enabled = true,
                                            container = if (canWriteToCamera) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                                            content = if (canWriteToCamera) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                                        )
                                    }
                                    SwipeAction(
                                        icon = rememberVectorPainter(Icons.Filled.Edit),
                                        label = stringResource(R.string.action_edit),
                                        onClick = { onEditRecipe(recipe.id) },
                                        position = ButtonGroupPosition.End,
                                    )
                                },
                            ) {
                                RecipeCard(
                                    recipe = recipe,
                                    shapes = ListItemDefaults.segmentedShapes(
                                        index = index,
                                        count = state.visible.size,
                                    ),
                                    onClick = {
                                        activeRecipeId = recipe.id
                                        onOpenRecipe(recipe.id)
                                    },
                                )
                            }
                        }

                        // Last line of the list, not a banner at the top: it answers a
                        // question you only ask once you are already looking.
                        state.lastUpdatedAt?.let { updatedAt ->
                            item {
                                Column {
                                    Spacer(Modifier.height(10.dp))
                                    LastUpdatedFooter(updatedAt)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    activeRecipeId?.let { recipeId ->
        RecipeViewBottomSheet(
            recipeId = recipeId,
            onDismiss = { activeRecipeId = null },
            onEdit = { id ->
                activeRecipeId = null
                onEditRecipe(id)
            },
        )
    }

    writeRecipeId?.let { recipeId ->
        WriteSheetHost(
            recipeId = recipeId,
            onDismiss = { writeRecipeId = null },
        )
    }

    recipePendingDelete?.let { recipe ->
        AlertDialog(
            onDismissRequest = { recipePendingDelete = null },
            title = { Text(stringResource(R.string.delete_title)) },
            text = { Text(stringResource(R.string.delete_body, recipe.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = recipe.id
                        recipePendingDelete = null
                        openRowId = null
                        onDeleteRecipe(id)
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { recipePendingDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

/** The wiring, kept out of the screen so the screen stays previewable. */
@Composable
fun LibraryRouteContent(
    onOpenRecipe: (String) -> Unit,
    onEditRecipe: (String) -> Unit,
    onCreateRecipe: () -> Unit,
    onOpenConnection: () -> Unit,
    contentPadding: PaddingValues,
) {
    val container = (LocalContext.current.applicationContext as FujiRecipesApp).container
    val viewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.factory(container))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val camera by container.cameraController.state.collectAsStateWithLifecycle()

    LibraryScreen(
        state = state,
        onSearchChange = viewModel::onSearchChange,
        onSortChange = viewModel::onSortChange,
        onFiltersChange = viewModel::onFiltersChange,
        onClearSearchAndFilters = viewModel::onClearSearchAndFilters,
        onRefresh = viewModel::refresh,
        onOpenRecipe = onOpenRecipe,
        onEditRecipe = onEditRecipe,
        onDeleteRecipe = viewModel::onDeleteRecipe,
        onCreateRecipe = onCreateRecipe,
        onOpenConnection = onOpenConnection,
        contentPadding = contentPadding,
        canWriteToCamera = camera.canWrite,
    )
}

private val sampleRecipes = listOf(
    RecipeCardModel("a", "Kodachrome 64", "classic-chrome", 5, listOf("street", "warm")),
    RecipeCardModel("b", "Acros Night", "acros-r", 0, emptyList()),
    RecipeCardModel("c", "Reala Sunday", "reala-ace", 4, listOf("family")),
)

@Preview(name = "List — light", showBackground = true, heightDp = 900)
@Preview(name = "List — dark", showBackground = true, uiMode = 0x20, heightDp = 900)
@Composable
private fun LibraryScreenPreview() {
    FujiTheme {
        LibraryScreen(
            state = LibraryUiState(
                visible = sampleRecipes,
                totalCount = 3,
                hasLoaded = true,
                availableTags = listOf("street", "warm", "family"),
                availableSimulations = listOf("classic-chrome", "acros-r", "reala-ace"),
            ),
            onSearchChange = {}, onSortChange = {}, onFiltersChange = {},
            onClearSearchAndFilters = {}, onRefresh = {}, onOpenRecipe = {},
            onEditRecipe = {}, onDeleteRecipe = {},
            onCreateRecipe = {}, onOpenConnection = {},
            contentPadding = PaddingValues(0.dp),
        )
    }
}

@Preview(name = "List — refused token", showBackground = true, heightDp = 700)
@Composable
private fun LibraryForbiddenPreview() {
    FujiTheme {
        LibraryScreen(
            state = LibraryUiState(
                hasLoaded = true,
                error = ApiError.Forbidden("This token was not accepted."),
            ),
            onSearchChange = {}, onSortChange = {}, onFiltersChange = {},
            onClearSearchAndFilters = {}, onRefresh = {}, onOpenRecipe = {},
            onEditRecipe = {}, onDeleteRecipe = {},
            onCreateRecipe = {}, onOpenConnection = {},
            contentPadding = PaddingValues(0.dp),
        )
    }
}

@Preview(name = "List — empty library", showBackground = true, heightDp = 700)
@Composable
private fun LibraryEmptyPreview() {
    FujiTheme {
        LibraryScreen(
            state = LibraryUiState(hasLoaded = true, totalCount = 0),
            onSearchChange = {}, onSortChange = {}, onFiltersChange = {},
            onClearSearchAndFilters = {}, onRefresh = {}, onOpenRecipe = {},
            onEditRecipe = {}, onDeleteRecipe = {},
            onCreateRecipe = {}, onOpenConnection = {},
            contentPadding = PaddingValues(0.dp),
        )
    }
}
