package dev.bondarenko.fujirecipes.ui.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.data.fields.FilmSimulations
import dev.bondarenko.fujirecipes.data.library.LibraryFilters
import dev.bondarenko.fujirecipes.data.library.SortDirection
import dev.bondarenko.fujirecipes.data.library.SortId
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme
import dev.bondarenko.fujirecipes.ui.theme.TabularFigures
import kotlin.math.roundToInt

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
    onSortDirectionChange: (SortDirection) -> Unit,
    onToggleSortDirection: () -> Unit,
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
            SortControls(
                sort = state.sort,
                sortDirection = state.sortDirection,
                onSortChange = onSortChange,
                onSortDirectionChange = onSortDirectionChange,
                onToggleSortDirection = onToggleSortDirection,
            )

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
private fun SortControls(
    sort: SortId,
    sortDirection: SortDirection,
    onSortChange: (SortId) -> Unit,
    onSortDirectionChange: (SortDirection) -> Unit,
    onToggleSortDirection: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        TextButton(
            onClick = { expanded = true },
            contentPadding = PaddingValues(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
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

        IconButton(
            onClick = onToggleSortDirection,
            modifier = Modifier.size(36.dp),
        ) {
            val isAsc = sortDirection == SortDirection.ASCENDING
            Icon(
                painter = painterResource(
                    if (isAsc) R.drawable.ic_arrow_upward else R.drawable.ic_arrow_downward,
                ),
                contentDescription = stringResource(R.string.sort_direction_toggle),
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(
                text = stringResource(R.string.sort_by),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
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

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text(
                text = stringResource(R.string.sort_order),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
            SortDirection.entries.forEach { direction ->
                val isSelected = direction == sortDirection
                DropdownMenuItem(
                    leadingIcon = {
                        Icon(
                            painter = painterResource(
                                if (direction == SortDirection.ASCENDING) R.drawable.ic_arrow_upward else R.drawable.ic_arrow_downward,
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    text = {
                        val labelRes = when (sort) {
                            SortId.NAME -> if (direction == SortDirection.ASCENDING) R.string.sort_name_asc else R.string.sort_name_desc
                            SortId.RATING -> if (direction == SortDirection.DESCENDING) R.string.sort_rating_desc else R.string.sort_rating_asc
                            SortId.UPDATED -> if (direction == SortDirection.DESCENDING) R.string.sort_updated_desc else R.string.sort_updated_asc
                        }
                        Text(stringResource(labelRes))
                    },
                    trailingIcon = {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                    onClick = {
                        onSortDirectionChange(direction)
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

/**
 * The controls themselves, which only exist while the sheet is open.
 * Made vertically scrollable to eliminate any height glitching or layout loops with large tag counts.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FiltersSheet(
    state: LibraryUiState,
    onFiltersChange: (LibraryFilters) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
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

            if (!state.filters.isEmpty) {
                TextButton(
                    onClick = { onFiltersChange(LibraryFilters()) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.action_clear_all),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        RatingRangeFilter(
            minRating = state.filters.minRating,
            maxRating = state.filters.maxRating,
            onRangeChange = { min, max ->
                onFiltersChange(state.filters.copy(minRating = min, maxRating = max))
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
            CollapsibleTagsFilter(
                availableTags = state.availableTags,
                selectedTags = state.filters.tags,
                onToggleTag = { tag -> onFiltersChange(state.filters.toggleTag(tag)) },
            )
        }
    }
}

/**
 * Rating range filter (0★ unrated to 5★), allowing users to pick an inclusive range from X to Y.
 */
@Composable
private fun RatingRangeFilter(
    minRating: Int,
    maxRating: Int,
    onRangeChange: (Int, Int) -> Unit,
) {
    val isNarrowed = minRating > 0 || maxRating < 5

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
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
                    text = stringResource(R.string.filter_rating_range),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val ratingSummary = when {
                    !isNarrowed -> stringResource(R.string.filter_rating_any)
                    minRating == maxRating -> if (minRating == 0) {
                        stringResource(R.string.filter_rating_unrated)
                    } else {
                        stringResource(R.string.filter_rating_exact, minRating)
                    }
                    minRating == 0 -> stringResource(R.string.filter_rating_unrated_to, maxRating)
                    else -> stringResource(R.string.filter_rating_from_to, minRating, maxRating)
                }

                Text(
                    text = ratingSummary,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = if (isNarrowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (isNarrowed) {
                    TextButton(
                        onClick = { onRangeChange(0, 5) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.action_reset),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
        ) {
            RangeSlider(
                value = minRating.toFloat()..maxRating.toFloat(),
                onValueChange = { range ->
                    val newMin = range.start.roundToInt().coerceIn(0, 5)
                    val newMax = range.endInclusive.roundToInt().coerceIn(0, 5)
                    onRangeChange(minOf(newMin, newMax), maxOf(newMin, newMax))
                },
                valueRange = 0f..5f,
                steps = 4,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                (0..5).forEach { star ->
                    Text(
                        text = if (star == 0) "0★" else "$star★",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFeatureSettings = TabularFigures,
                        ),
                        color = if (star in minRating..maxRating && isNarrowed) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        },
                    )
                }
            }
        }
    }
}

/**
 * Collapsible tag filter with smooth expand/collapse behavior.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CollapsibleTagsFilter(
    availableTags: List<String>,
    selectedTags: List<String>,
    onToggleTag: (String) -> Unit,
) {
    var isExpanded by rememberSaveable { mutableStateOf(selectedTags.isNotEmpty() || availableTags.size <= 8) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "tags_expand_arrow",
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_label),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.filter_tags_all_of),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (selectedTags.isNotEmpty()) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ) {
                        Text(selectedTags.size.toString())
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = if (isExpanded) {
                        stringResource(R.string.filter_tags_collapse)
                    } else {
                        stringResource(R.string.filter_tags_expand, availableTags.size)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(arrowRotation),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        // Preview row when collapsed but tags are selected
        if (!isExpanded && selectedTags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                selectedTags.forEach { tag ->
                    FilterChip(
                        selected = true,
                        onClick = { onToggleTag(tag) },
                        label = { Text(tag) },
                        leadingIcon = selectedCheck(true),
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                availableTags.forEach { tag ->
                    val isSelected = tag in selectedTags
                    FilterChip(
                        selected = isSelected,
                        onClick = { onToggleTag(tag) },
                        label = { Text(tag) },
                        leadingIcon = selectedCheck(isSelected),
                    )
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
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

/**
 * The leading check M3 filter chips show when selected.
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
                    filters = LibraryFilters(tags = listOf("street"), minRating = 4, maxRating = 5),
                    sort = SortId.NAME,
                    sortDirection = SortDirection.ASCENDING,
                    totalCount = 10,
                    visible = emptyList(),
                    hasLoaded = true,
                ),
                onSearchChange = {},
                onSortChange = {},
                onSortDirectionChange = {},
                onToggleSortDirection = {},
                onFiltersChange = {},
                onClearSearchAndFilters = {},
            )
        }
    }
}
