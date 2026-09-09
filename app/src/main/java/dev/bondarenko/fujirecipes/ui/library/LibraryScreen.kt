package dev.bondarenko.fujirecipes.ui.library

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import dev.bondarenko.fujirecipes.ui.theme.icons.StarBorder
import dev.bondarenko.fujirecipes.ui.theme.icons.StarRate
import androidx.activity.compose.BackHandler
import androidx.compose.material3.IconButton
import androidx.compose.ui.res.pluralStringResource
import dev.bondarenko.fujirecipes.ui.theme.icons.Close
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import dev.bondarenko.fujirecipes.ui.theme.icons.BookmarkStacks
import dev.bondarenko.fujirecipes.ui.theme.icons.Delete
import dev.bondarenko.fujirecipes.ui.theme.icons.Edit
import dev.bondarenko.fujirecipes.ui.theme.icons.FilterAltOff
import dev.bondarenko.fujirecipes.ui.theme.icons.FujiIcons
import dev.bondarenko.fujirecipes.ui.theme.icons.PhotoCamera
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
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
import dev.bondarenko.fujirecipes.core.result.LibraryError
import dev.bondarenko.fujirecipes.data.library.LibraryFilters
import dev.bondarenko.fujirecipes.data.library.SortDirection
import dev.bondarenko.fujirecipes.data.library.SortId
import dev.bondarenko.fujirecipes.camera.canWrite
import androidx.compose.material3.MaterialShapes
import dev.bondarenko.fujirecipes.ui.camera.WriteSheetHost
import dev.bondarenko.fujirecipes.ui.common.FujiAnchoredMenu
import dev.bondarenko.fujirecipes.ui.common.FujiIconPanel
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme
import dev.bondarenko.fujirecipes.ui.theme.TabularFigures

/**
 * The list — FEAT-001 T-19, T-20, T-22.
 *
 * Takes state and lambdas, never a ViewModel, so every state below previews and tests
 * without a graph. [LibraryRoute] does the wiring.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onSearchChange: (String) -> Unit,
    onSortChange: (SortId) -> Unit,
    onSortDirectionChange: (SortDirection) -> Unit = {},
    onToggleSortDirection: () -> Unit = {},
    onFiltersChange: (LibraryFilters) -> Unit,
    onClearSearchAndFilters: () -> Unit,
    onRetry: () -> Unit,
    onOpenRecipe: (String) -> Unit,
    onEditRecipe: (String) -> Unit,
    onDeleteRecipe: (String) -> Unit,
    onDeleteRecipes: (Set<String>) -> Unit,
    onCreateRecipe: () -> Unit,
    onImportFromCamera: () -> Unit,
    onDevelopRaw: (String) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onRateRecipes: (Set<String>, Int) -> Unit = { _, _ -> },
    onSelectionChange: (Boolean) -> Unit = {},
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    canWriteToCamera: Boolean = false,
) {
    // Which row is slid open, owned here rather than by each row.
    var openRowId by rememberSaveable { mutableStateOf<String?>(null) }
    // Which recipe is pending confirmation for deletion from swipe action
    var recipePendingDelete by remember { mutableStateOf<RecipeCardModel?>(null) }
    // Which recipe is currently open in the Write to Camera sheet
    var writeRecipeId by rememberSaveable { mutableStateOf<String?>(null) }
    // Selection mode: empty means off. Deliberately not saved — a rotation mid-selection is
    // rare, and coming back to a cleared list is safer than coming back to a stale one.
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    var deleteSelectionPending by remember { mutableStateOf(false) }
    val selecting = selectedIds.isNotEmpty()

    var ratingPickerOpen by remember { mutableStateOf(false) }

    // The gesture everyone tries first to get out of selection mode.
    BackHandler(enabled = selecting) { selectedIds = emptySet() }

    LaunchedEffect(selecting) { onSelectionChange(selecting) }
    DisposableEffect(Unit) { onDispose { onSelectionChange(false) } }
    val coroutineScope = rememberCoroutineScope()

    // No pull-to-refresh: there is nowhere to refresh *from*. The library is a file on this
    // phone, and the flow that feeds this screen already republishes on every change.
    Box(modifier = modifier.fillMaxSize()) {
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
                // Nothing on screen and a failure: the error *is* the screen. Crucially an
                // unreadable library file lands here rather than rendering as an empty one.
                state.isBlockingError -> item {
                    LibraryErrorPanel(error = state.error!!, onRetry = onRetry)
                }

                // fillParentMaxSize so the indicator centres against the viewport, not against
                // its own height at the top of the list.
                !state.hasLoaded -> item { LibraryLoading(Modifier.fillParentMaxSize()) }

                /**
                 * An empty library is a page with one thing to say and one thing to do, so it
                 * is drawn as one — the same `FujiIconPanel` the photo reader, both imports
                 * and the camera use, rather than the bordered card it used to be.
                 *
                 * **The action is Import from camera**, not Create a recipe. A first launch
                 * is almost never someone who wants to type twenty parameters in; it is
                 * someone whose recipes are already on the body in C1–C7. Creating one by
                 * hand is still here, under the button and quieter, and the create FAB is on
                 * screen the whole time regardless.
                 */
                state.isEmptyLibrary -> item {
                    FujiIconPanel(
                        // The library's own toolbar glyph: this is still the library, however
                        // empty. The shape is the down arrow the two import screens carry,
                        // because getting recipes *in* is what the page is for.
                        icon = FujiIcons.BookmarkStacks,
                        shape = MaterialShapes.Pill.toShape(),
                        title = stringResource(R.string.empty_library_title),
                        body = stringResource(R.string.empty_library_body),
                        actionLabel = stringResource(R.string.empty_library_import),
                        onAction = onImportFromCamera,
                        modifier = Modifier.fillParentMaxSize(),
                        extra = {
                            Text(
                                text = stringResource(R.string.empty_library_or),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            TextButton(onClick = onCreateRecipe) {
                                Text(stringResource(R.string.empty_library_create))
                            }
                        },
                    )
                }

                else -> {
                    item {
                        Column {
                            if (selecting) {
                                LibrarySelectionBar(
                                    count = selectedIds.size,
                                    allSelected = selectedIds.size == state.visible.size,
                                    onSelectAll = {
                                        selectedIds = state.visible.map { it.id }.toSet()
                                    },
                                )
                            } else {
                            LibraryToolbar(
                                state = state,
                                onSearchChange = onSearchChange,
                                onSortChange = onSortChange,
                                onSortDirectionChange = onSortDirectionChange,
                                onToggleSortDirection = onToggleSortDirection,
                                onFiltersChange = onFiltersChange,
                                onClearSearchAndFilters = onClearSearchAndFilters,
                                onOpenSettings = onOpenSettings,
                            )
                            }
                            Spacer(Modifier.height(10.dp))
                        }
                    }

                    if (state.hasNoMatches) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillParentMaxHeight(0.72f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Icon(
                                        imageVector = FujiIcons.FilterAltOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(44.dp),
                                    )
                                    Text(
                                        text = stringResource(R.string.no_matches_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    } else {
                        itemsIndexed(state.visible, key = { _, recipe -> recipe.id }) { index, recipe ->
                            val card: @Composable () -> Unit = {
                                RecipeCard(
                                    recipe = recipe,
                                    shapes = ListItemDefaults.segmentedShapes(
                                        index = index,
                                        count = state.visible.size,
                                    ),
                                    showPhoto = state.showPhotos,
                                    showTags = state.showTags,
                                    showFilmSimulation = state.showFilmSimulation,
                                    showRating = state.showRating,
                                    selected = recipe.id in selectedIds,
                                    onLongClick = { selectedIds = selectedIds + recipe.id },
                                    onClick = {
                                        if (selecting) {
                                            selectedIds = if (recipe.id in selectedIds) {
                                                selectedIds - recipe.id
                                            } else {
                                                selectedIds + recipe.id
                                            }
                                        } else {
                                            onOpenRecipe(recipe.id)
                                        }
                                    },
                                )
                            }

                            // Swiping a row open while picking rows would put a single-recipe
                            // delete next to a selection's delete. One at a time.
                            if (selecting) {
                                card()
                                return@itemsIndexed
                            }

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
                                        icon = rememberVectorPainter(FujiIcons.Delete),
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
                                            icon = rememberVectorPainter(FujiIcons.PhotoCamera),
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
                                        icon = rememberVectorPainter(FujiIcons.Edit),
                                        label = stringResource(R.string.action_edit),
                                        onClick = { onEditRecipe(recipe.id) },
                                        position = ButtonGroupPosition.End,
                                    )
                                },
                            ) {
                                card()
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

        if (selecting) {
            LibrarySelectionToolbar(
                ratingPickerOpen = ratingPickerOpen,
                onRatingPickerOpenChange = { ratingPickerOpen = it },
                onDelete = { deleteSelectionPending = true },
                onRate = { rating ->
                    val ids = selectedIds
                    selectedIds = emptySet()
                    onRateRecipes(ids, rating)
                },
                onClear = { selectedIds = emptySet() },
                // The screen's Box fills the whole scaffold, bars included, so the bottom
                // inset has to be applied here or the toolbar lands behind the navigation bar.
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = contentPadding.calculateBottomPadding()),
            )
        }
    }

    writeRecipeId?.let { recipeId ->
        WriteSheetHost(
            recipeId = recipeId,
            onDismiss = { writeRecipeId = null },
        )
    }

    if (deleteSelectionPending) {
        val count = selectedIds.size
        AlertDialog(
            onDismissRequest = { deleteSelectionPending = false },
            title = { Text(pluralStringResource(R.plurals.delete_selection_title, count, count)) },
            text = { Text(pluralStringResource(R.plurals.delete_selection_body, count, count)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val ids = selectedIds
                        deleteSelectionPending = false
                        selectedIds = emptySet()
                        onDeleteRecipes(ids)
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteSelectionPending = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
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

/**
 * What the search row becomes while rows are being picked: how many, all of them, or none,
 * and the one destructive thing the mode exists for.
 */
@Composable
private fun LibrarySelectionBar(count: Int, allSelected: Boolean, onSelectAll: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = pluralStringResource(R.plurals.selected_count, count, count),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onSelectAll, enabled = !allSelected) {
            Text(stringResource(R.string.action_select_all))
        }
    }
}

/**
 * The actions for the rows that are picked, in the corner the create button vacates.
 *
 * Rating opens a menu rather than a dialog: five values is a list, and a dialog for a list of
 * five would be a bigger interruption than the change it makes.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LibrarySelectionToolbar(
    ratingPickerOpen: Boolean,
    onRatingPickerOpenChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onRate: (Int) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HorizontalFloatingToolbar(
        expanded = true,
        // Vibrant, not standard: the standard container is the same tone as the cards it
        // floats over, which left the actions reading as three loose icons.
        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
        // No navigationBarsPadding here: the shell's Scaffold has already inset this content
        // area above its own bar, and insetting twice would park the toolbar behind it.
        modifier = modifier.padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = FujiIcons.Delete,
                contentDescription = stringResource(R.string.action_delete),
            )
        }

        Box {
            IconButton(onClick = { onRatingPickerOpenChange(true) }) {
                Icon(
                    imageVector = FujiIcons.StarRate,
                    contentDescription = stringResource(R.string.action_set_rating),
                )
            }
            FujiAnchoredMenu(
                expanded = ratingPickerOpen,
                onDismissRequest = { onRatingPickerOpenChange(false) },
            ) {
                // Descending, and zero last: it is the "no rating" entry, not a sixth star.
                (5 downTo 0).forEach { value ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (value == 0) {
                                    stringResource(R.string.rating_none)
                                } else {
                                    pluralStringResource(R.plurals.rating_stars, value, value)
                                },
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (value == 0) FujiIcons.StarBorder else FujiIcons.StarRate,
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            onRatingPickerOpenChange(false)
                            onRate(value)
                        },
                    )
                }
            }
        }

        IconButton(onClick = onClear) {
            Icon(
                imageVector = FujiIcons.Close,
                contentDescription = stringResource(R.string.action_clear_selection),
            )
        }
    }
}

/** The wiring, kept out of the screen so the screen stays previewable. */
@Composable
fun LibraryRouteContent(
    onOpenRecipe: (String) -> Unit,
    onEditRecipe: (String) -> Unit,
    onCreateRecipe: () -> Unit,
    onImportFromCamera: () -> Unit,
    onDevelopRaw: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onSelectionChange: (Boolean) -> Unit,
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
        onSortDirectionChange = viewModel::onSortDirectionChange,
        onToggleSortDirection = viewModel::onToggleSortDirection,
        onFiltersChange = viewModel::onFiltersChange,
        onClearSearchAndFilters = viewModel::onClearSearchAndFilters,
        onRetry = viewModel::retry,
        onOpenRecipe = onOpenRecipe,
        onEditRecipe = onEditRecipe,
        onDeleteRecipe = viewModel::onDeleteRecipe,
        onDeleteRecipes = viewModel::onDeleteRecipes,
        onCreateRecipe = onCreateRecipe,
        onImportFromCamera = onImportFromCamera,
        onDevelopRaw = onDevelopRaw,
        onOpenSettings = onOpenSettings,
        onRateRecipes = viewModel::onRateRecipes,
        onSelectionChange = onSelectionChange,
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
            onClearSearchAndFilters = {}, onRetry = {}, onOpenRecipe = {},
            onEditRecipe = {}, onDeleteRecipe = {}, onDeleteRecipes = {}, onCreateRecipe = {},
            onImportFromCamera = {},
            contentPadding = PaddingValues(0.dp),
        )
    }
}

@Preview(name = "List — unreadable library", showBackground = true, heightDp = 700)
@Composable
private fun LibraryUnreadablePreview() {
    FujiTheme {
        LibraryScreen(
            state = LibraryUiState(
                hasLoaded = true,
                error = LibraryError.Unreadable("Unexpected character at offset 412."),
            ),
            onSearchChange = {}, onSortChange = {}, onFiltersChange = {},
            onClearSearchAndFilters = {}, onRetry = {}, onOpenRecipe = {},
            onEditRecipe = {}, onDeleteRecipe = {}, onDeleteRecipes = {}, onCreateRecipe = {},
            onImportFromCamera = {},
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
            onClearSearchAndFilters = {}, onRetry = {}, onOpenRecipe = {},
            onEditRecipe = {}, onDeleteRecipe = {}, onDeleteRecipes = {}, onCreateRecipe = {},
            onImportFromCamera = {},
            contentPadding = PaddingValues(0.dp),
        )
    }
}

@Preview(name = "List — no matches", showBackground = true, heightDp = 700)
@Composable
private fun LibraryNoMatchesPreview() {
    FujiTheme {
        LibraryScreen(
            state = LibraryUiState(
                hasLoaded = true,
                totalCount = 5,
                search = "xyz",
                visible = emptyList(),
            ),
            onSearchChange = {}, onSortChange = {}, onFiltersChange = {},
            onClearSearchAndFilters = {}, onRetry = {}, onOpenRecipe = {},
            onEditRecipe = {}, onDeleteRecipe = {}, onDeleteRecipes = {}, onCreateRecipe = {},
            onImportFromCamera = {},
            contentPadding = PaddingValues(0.dp),
        )
    }
}
