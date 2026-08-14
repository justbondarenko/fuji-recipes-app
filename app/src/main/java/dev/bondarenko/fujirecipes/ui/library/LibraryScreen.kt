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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    onCreateRecipe: () -> Unit,
    onOpenConnection: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
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
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (state.hasLoaded && state.totalCount > 0) CountChip(state)
                }
            }

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
                            RecipeCard(recipe, onClick = { onOpenRecipe(recipe.id) })
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

/**
 * The library's size, always the total.
 *
 * The narrowed count lives in the toolbar's summary bar instead, so the two do not say the
 * same thing in two places — and so this stays a stable fact about the library rather than
 * a number that changes as you type.
 */
@Composable
private fun CountChip(state: LibraryUiState) {
    Text(
        text = pluralStringResource(R.plurals.count_total, state.totalCount, state.totalCount),
        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = TabularFigures),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** The wiring, kept out of the screen so the screen stays previewable. */
@Composable
fun LibraryRouteContent(
    onOpenRecipe: (String) -> Unit,
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
            onCreateRecipe = {}, onOpenConnection = {},
            contentPadding = PaddingValues(0.dp),
        )
    }
}
