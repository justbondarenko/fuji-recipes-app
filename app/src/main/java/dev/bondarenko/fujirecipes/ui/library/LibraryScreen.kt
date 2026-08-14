package dev.bondarenko.fujirecipes.ui.library

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
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
) {
    // Which row is slid open, owned here rather than by each row.
    var openRowId by rememberSaveable { mutableStateOf<String?>(null) }
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
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

                !state.hasLoaded -> item { LibraryLoading() }

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
                        LibraryToolbar(
                            state = state,
                            onSearchChange = onSearchChange,
                            onSortChange = onSortChange,
                            onFiltersChange = onFiltersChange,
                            onClearSearchAndFilters = onClearSearchAndFilters,
                        )
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
                        items(state.visible, key = { it.id }) { recipe ->
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
                                        contentDescription = stringResource(R.string.action_delete),
                                        onClick = { onDeleteRecipe(recipe.id) },
                                        container = MaterialTheme.colorScheme.errorContainer,
                                        content = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                    SwipeAction(
                                        icon = painterResource(R.drawable.ic_photo_camera),
                                        contentDescription = stringResource(R.string.action_write_to_camera),
                                        onClick = {},
                                        // Shown but inert until FEAT-006. A control that
                                        // appears between builds is harder to learn than one
                                        // that is visibly not ready.
                                        enabled = false,
                                    )
                                    SwipeAction(
                                        icon = rememberVectorPainter(Icons.Filled.Edit),
                                        contentDescription = stringResource(R.string.action_edit),
                                        onClick = { onEditRecipe(recipe.id) },
                                    )
                                },
                            ) {
                                RecipeCard(recipe, onClick = { onOpenRecipe(recipe.id) })
                            }
                        }

                        // Last line of the list, not a banner at the top: it answers a
                        // question you only ask once you are already looking.
                        state.lastUpdatedAt?.let { updatedAt ->
                            item { LastUpdatedFooter(updatedAt) }
                        }
                    }
                }
            }
        }
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
