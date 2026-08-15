package dev.bondarenko.fujirecipes.ui.library

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.data.fields.FilmSimulations
import dev.bondarenko.fujirecipes.data.library.LibraryFilters
import dev.bondarenko.fujirecipes.data.library.SortId
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme
import dev.bondarenko.fujirecipes.ui.theme.TabularFigures

/**
 * The toolbar — FEAT-001 T-20, rebuilt compact.
 *
 * **Two rows, not five.** The first version rendered sort, rating, simulations and tags as
 * four permanent chip rows, which cost about a third of the screen to controls that are
 * mostly idle — on the one screen whose job is showing as many recipes as possible.
 *
 * What replaces it is the web client's own shape (`LibraryToolbar.vue`):
 *
 * | Row | Always visible | Why |
 * |---|---|---|
 * | Search | yes | The fastest route to a known recipe, and the thing most reached for |
 * | Filters button + sort menu | yes, one line | The badge says how many axes are set without opening anything |
 * | "Showing n of m · Clear all" | only when narrowed | A restored filter must be visible, or the library looks like it lost things |
 * | The filter controls themselves | on demand, in a sheet | They are set rarely and read never |
 *
 * The badge counts **axes**, not values: three tags and a rating is "2", because that is
 * how many things you would have to clear.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryToolbar(
    state: LibraryUiState,
    onSearchChange: (String) -> Unit,
    onSortChange: (SortId) -> Unit,
    onFiltersChange: (LibraryFilters) -> Unit,
    onClearSearchAndFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var filtersOpen by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        /**
         * The **search app bar** (`m3.material.io/components/app-bars`): the search field is
         * the bar, rather than a text field sitting under a title.
         *
         * `SearchBarDefaults.InputField` is used on its own rather than inside a `SearchBar`,
         * because the expanding container `SearchBar` provides exists to show suggestions
         * over the screen — and there is nothing to suggest. The whole library is already in
         * memory and the list narrows on every keystroke, so the results *are* the screen
         * behind it. Using the input field alone keeps M3's shape, colour and height tokens
         * without an overlay that would cover the answer.
         */
        /**
         * The container is ours, not `SearchBarDefaults`'.
         *
         * M3's default is `surfaceContainerHigh`, which in this palette is stone-200 — the
         * exact colour of the page, so the bar vanished. It takes the card treatment instead
         * (`surfaceContainerLow` with a hairline outline), which is what every other raised
         * thing in this app looks like.
         */
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
        ) {
        SearchBarDefaults.InputField(
            query = state.search,
            onQueryChange = onSearchChange,
            onSearch = {},
            expanded = false,
            onExpandedChange = {},
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.search_placeholder)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (state.search.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(
                            Icons.Filled.Clear,
                            contentDescription = stringResource(R.string.action_clear_search),
                        )
                    }
                }
            },
        )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FiltersButton(
                activeCount = state.filters.activeCount,
                onClick = { filtersOpen = true },
            )
            SortMenu(sort = state.sort, onSortChange = onSortChange)

            Spacer(Modifier.weight(1f))

            // The library's size, which the search app bar replaced a headline row for.
            if (state.hasLoaded && state.totalCount > 0) {
                Text(
                    text = pluralStringResource(
                        R.plurals.count_total,
                        state.totalCount,
                        state.totalCount,
                    ),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFeatureSettings = TabularFigures,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Only when there is something to explain. A restored filter that is invisible is
        // how a library appears to have lost recipes overnight.
        if (state.isNarrowed) {
            NarrowedSummary(
                visible = state.visible.size,
                total = state.totalCount,
                onClearAll = onClearSearchAndFilters,
            )
        }
    }

    if (filtersOpen) {
        ModalBottomSheet(
            onDismissRequest = { filtersOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            FiltersSheet(
                state = state,
                onFiltersChange = onFiltersChange,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
    }
}

@Composable
private fun FiltersButton(activeCount: Int, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_tune),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(stringResource(R.string.filters))
            if (activeCount > 0) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                    modifier = Modifier.padding(start = 2.dp),
                ) { Text(activeCount.toString()) }
            }
        }
    }
}

@Composable
private fun SortMenu(sort: SortId, onSortChange: (SortId) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Row {
        TextButton(
            onClick = { expanded = true },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_sort),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(stringResource(sort.labelRes()))
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(12.dp),
        ) {
            SortId.entries.forEach { option ->
                DropdownMenuItem(
                    leadingIcon = {
                        val iconModifier = Modifier.size(20.dp)
                        when (option) {
                            SortId.NAME -> Icon(
                                painter = painterResource(R.drawable.ic_sort_by_alpha),
                                contentDescription = null,
                                modifier = iconModifier,
                            )
                            SortId.RATING -> Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                modifier = iconModifier,
                            )
                            SortId.UPDATED -> Icon(
                                painter = painterResource(R.drawable.ic_schedule),
                                contentDescription = null,
                                modifier = iconModifier,
                            )
                        }
                    },
                    text = { Text(stringResource(option.labelRes())) },
                    trailingIcon = {
                        if (option == sort) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                    onClick = {
                        onSortChange(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun NarrowedSummary(visible: Int, total: Int, onClearAll: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.showing_of, visible, total),
            style = MaterialTheme.typography.labelMedium.copy(
                fontFeatureSettings = TabularFigures,
            ),
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        TextButton(onClick = onClearAll) {
            Text(
                text = stringResource(R.string.action_clear_all),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

/** The controls themselves, which only exist while the sheet is open. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FiltersSheet(
    state: LibraryUiState,
    onFiltersChange: (LibraryFilters) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            // The sheet sits over the navigation bar, so it cannot borrow that bar's inset:
            // its own last row is what the gesture strip would otherwise take.
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_tune),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.filters),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        RatingButtonGroup(
            selectedRating = state.filters.minRating,
            onRatingChange = { newRating ->
                onFiltersChange(state.filters.copy(minRating = newRating))
            },
        )

        if (state.availableSimulations.isNotEmpty()) {
            FilterGroup(
                label = stringResource(R.string.filter_simulation),
                icon = painterResource(R.drawable.ic_photo_camera),
            ) {
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
            FilterGroup(
                label = stringResource(R.string.filter_tags_all_of),
                icon = painterResource(R.drawable.ic_label),
            ) {
                state.availableTags.forEach { tag ->
                    FilterChip(
                        selected = tag in state.filters.tags,
                        onClick = { onFiltersChange(state.filters.toggleTag(tag)) },
                        label = { Text(tag) },
                        leadingIcon = selectedCheck(tag in state.filters.tags),
                    )
                }
            }
        }
    }
}

@Composable
private fun RatingButtonGroup(
    selectedRating: Int,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.filter_min_rating),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        val count = 5
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            (1..5).forEachIndexed { index, rating ->
                val selected = selectedRating == rating

                // M3 Expressive morphing corner radii
                val topStart = if (selected || index == 0) 24.dp else 8.dp
                val bottomStart = if (selected || index == 0) 24.dp else 8.dp
                val topEnd = if (selected || index == count - 1) 24.dp else 8.dp
                val bottomEnd = if (selected || index == count - 1) 24.dp else 8.dp

                val animTopStart by animateDpAsState(targetValue = topStart, label = "topStart")
                val animBottomStart by animateDpAsState(targetValue = bottomStart, label = "bottomStart")
                val animTopEnd by animateDpAsState(targetValue = topEnd, label = "topEnd")
                val animBottomEnd by animateDpAsState(targetValue = bottomEnd, label = "bottomEnd")

                val shape = RoundedCornerShape(
                    topStart = animTopStart,
                    bottomStart = animBottomStart,
                    topEnd = animTopEnd,
                    bottomEnd = animBottomEnd,
                )

                val targetContainerColor = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                }
                val containerColor by animateColorAsState(
                    targetValue = targetContainerColor,
                    label = "containerColor",
                )

                val targetContentColor = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
                val contentColor by animateColorAsState(
                    targetValue = targetContentColor,
                    label = "contentColor",
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(shape)
                        .background(containerColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(),
                        ) {
                            onRatingChange(if (selected) 0 else rating)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 4.dp),
                    ) {
                        if (selected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = contentColor,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = contentColor.copy(alpha = 0.6f),
                            )
                        }
                        Text(
                            text = rating.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = contentColor,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterGroup(
    label: String,
    icon: Painter? = null,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (icon != null) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Wrapping rather than scrolling: in a sheet there is room to show every option,
        // and a horizontal scroller hides the ones past the edge.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

/**
 * The leading check M3 filter chips show when selected.
 *
 * Null rather than an invisible icon when unselected: a chip that reserves the space either
 * way changes width on selection, and a row of chips that reflows as you tap them is hard to
 * aim at a second time.
 */
private fun selectedCheck(selected: Boolean): (@Composable () -> Unit)? =
    if (!selected) {
        null
    } else {
        {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(FilterChipDefaults.IconSize),
            )
        }
    }

internal fun LibraryFilters.toggleTag(tag: String) =
    copy(tags = if (tag in tags) tags - tag else tags + tag)

internal fun LibraryFilters.toggleSimulation(id: String) =
    copy(simulations = if (id in simulations) simulations - id else simulations + id)

internal fun SortId.labelRes(): Int = when (this) {
    SortId.NAME -> R.string.sort_name
    SortId.RATING -> R.string.sort_rating
    SortId.UPDATED -> R.string.sort_updated
}

@Preview(name = "Toolbar — light", showBackground = true)
@Preview(name = "Toolbar — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun LibraryToolbarPreview() {
    FujiTheme {
        Column(modifier = Modifier.padding(12.dp)) {
            LibraryToolbar(
                state = LibraryUiState(
                    search = "acros",
                    filters = LibraryFilters(tags = listOf("street"), minRating = 4),
                    totalCount = 10,
                    visible = emptyList(),
                    hasLoaded = true,
                ),
                onSearchChange = {}, onSortChange = {}, onFiltersChange = {},
                onClearSearchAndFilters = {},
            )
        }
    }
}
