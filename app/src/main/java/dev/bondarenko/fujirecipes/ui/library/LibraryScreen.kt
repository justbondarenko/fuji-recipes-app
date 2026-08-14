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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import dev.bondarenko.fujirecipes.data.fields.FilmSimulations
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
        modifier = modifier.fillMaxSize().padding(contentPadding),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // The extra bottom inset clears the centre-docked FAB, which floats above the
            // navigation bar and would otherwise sit on top of the last card with no way to
            // scroll it out from under.
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp,
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

            if (state.servedFromSnapshotAt != null) {
                item { SnapshotBanner(state.servedFromSnapshotAt) }
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
                    }
                }
            }
        }
    }
}

/** `n recipes`, or `n of m recipes` while narrowed — the web client's counter. */
@Composable
private fun CountChip(state: LibraryUiState) {
    val text = if (state.isNarrowed) {
        stringResource(R.string.count_narrowed, state.visible.size, state.totalCount)
    } else {
        pluralStringResource(R.plurals.count_total, state.totalCount, state.totalCount)
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = TabularFigures),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryToolbar(
    state: LibraryUiState,
    onSearchChange: (String) -> Unit,
    onSortChange: (SortId) -> Unit,
    onFiltersChange: (LibraryFilters) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = state.search,
            onValueChange = onSearchChange,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            placeholder = { Text(stringResource(R.string.search_placeholder)) },
            shape = MaterialTheme.shapes.small,
        )

        ChipRow {
            SortId.entries.forEach { sort ->
                FilterChip(
                    selected = state.sort == sort,
                    onClick = { onSortChange(sort) },
                    label = { Text(stringResource(sort.labelRes())) },
                )
            }
        }

        // Minimum rating, as four steps rather than a slider: it is a filter with five
        // meaningful values, and a slider for that is a smaller target and less legible.
        ChipRow {
            (1..5).forEach { rating ->
                FilterChip(
                    selected = state.filters.minRating == rating,
                    onClick = {
                        onFiltersChange(
                            state.filters.copy(
                                // Tapping the active one clears it, so the axis can be
                                // switched off without hunting for a separate control.
                                minRating = if (state.filters.minRating == rating) 0 else rating,
                            ),
                        )
                    },
                    label = { Text(stringResource(R.string.filter_rating, rating)) },
                )
            }
        }

        if (state.availableSimulations.isNotEmpty()) {
            ChipRow {
                state.availableSimulations.forEach { id ->
                    FilterChip(
                        selected = id in state.filters.simulations,
                        onClick = { onFiltersChange(state.filters.toggleSimulation(id)) },
                        label = { Text(FilmSimulations.labelFor(id)) },
                    )
                }
            }
        }

        if (state.availableTags.isNotEmpty()) {
            ChipRow {
                state.availableTags.forEach { tag ->
                    FilterChip(
                        selected = tag in state.filters.tags,
                        onClick = { onFiltersChange(state.filters.toggleTag(tag)) },
                        label = { Text(tag) },
                    )
                }
            }
        }
    }
}

/**
 * A horizontally scrolling row of chips.
 *
 * `LazyRow` would be wrong inside a `LazyColumn` item for these counts, and a plain
 * `Row` with horizontal scroll keeps every chip measurable for the tests.
 */
@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

private fun LibraryFilters.toggleTag(tag: String) =
    copy(tags = if (tag in tags) tags - tag else tags + tag)

private fun LibraryFilters.toggleSimulation(id: String) =
    copy(simulations = if (id in simulations) simulations - id else simulations + id)

private fun SortId.labelRes(): Int = when (this) {
    SortId.NAME -> R.string.sort_name
    SortId.RATING -> R.string.sort_rating
    SortId.UPDATED -> R.string.sort_updated
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
    RecipeCardModel("a", "Kodachrome 64", "classic-chrome", 5, listOf("street", "warm"), 3, "2026-08-12T07:25:02.104Z"),
    RecipeCardModel("b", "Acros Night", "acros-r", 0, emptyList(), null, null),
    RecipeCardModel("c", "Reala Sunday", "reala-ace", 4, listOf("family"), 1, null),
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
