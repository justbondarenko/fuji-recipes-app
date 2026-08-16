package dev.bondarenko.fujirecipes.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.data.fields.FilmSimulations
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme
import dev.bondarenko.fujirecipes.ui.theme.TabularFigures

/** What a card needs. A plain holder, so a preview and a test can build one. */
data class RecipeCardModel(
    val id: String,
    val name: String,
    val filmSimulationId: String?,
    val rating: Int,
    val tags: List<String>,
)

/**
 * One row of the library — FEAT-001 T-17.
 *
 * Implements Material 3 Single-Action List Item with Segmented corner shaping.
 *
 * Omission rules:
 * - rating 0 shows **no** pill, not a zero
 * - no tags shows **no** tag row
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RecipeCard(
    recipe: RecipeCardModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shapes: ListItemShapes? = null,
) {
    ListItem(
        onClick = onClick,
        shapes = shapes ?: ListItemDefaults.segmentedShapes(index = 0, count = 1),
        colors = ListItemDefaults.segmentedColors(containerColor = cardColor()),
        // 💡 ROW PADDING — how much air the whole row has. Raise `RowVerticalPadding` for a
        //    taller, calmer list; lower it to fit more recipes on screen.
        contentPadding = PaddingValues(
            horizontal = RowHorizontalPadding,
            vertical = RowVerticalPadding,
        ),
        modifier = modifier.fillMaxWidth(),
        leadingContent = {
            // 💡 FILM SIMULATION BADGE SIZE — the round swatch on the left.
            FilmSimBadge(
                simulationId = recipe.filmSimulationId,
                size = 48.dp,
                shape = CircleShape,
            )
        },
        supportingContent = if (recipe.tags.isEmpty()) {
            null
        } else {
            {
                // 💡 GAP BETWEEN TITLE AND TAGS — `TitleToTagsGap` below.
                TagRow(recipe.tags, modifier = Modifier.padding(top = TitleToTagsGap))
            }
        },
        trailingContent = if (recipe.rating == 0) {
            null
        } else {
            { RatingBadge(recipe.rating) }
        },
    ) {
        // 💡 RECIPE NAME (the list row title):
        //    - Size: change `titleLarge` to `titleMedium` (smaller) or `headlineSmall` (bigger)
        //    - Weight: `FontWeight.SemiBold` -> `Bold` / `Medium` / `Normal`
        Text(
            text = recipe.name,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// 💡 ROW SPACING KNOBS — all of the list row's breathing space, in one place.
/** Air above and below each row's content. */
private val RowVerticalPadding = 14.dp
/** Air at the left and right edges of a row. */
private val RowHorizontalPadding = 16.dp
/** The gap between the recipe name and its tag row. */
private val TitleToTagsGap = 6.dp

/** Cards sit one step off the page, and the step is not symmetrical between schemes. */
@Composable
private fun cardColor() =
    if (MaterialTheme.colorScheme.surface == MaterialTheme.colorScheme.surfaceContainerHigh) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

@Composable
private fun RatingBadge(
    rating: Int,
    modifier: Modifier = Modifier,
) {
    Badge(
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        // 💡 RATING PILL SIZE — the padding is what makes the pill bigger, not the text.
        modifier = modifier.padding(horizontal = 4.dp, vertical = 6.dp),
    ) {
        // 💡 RATING NUMBER — `labelMedium` -> `labelSmall` (smaller) / `labelLarge` (bigger).
        Text(
            text = rating.toString(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFeatureSettings = TabularFigures,
            ),
        )
        // 💡 RATING STAR SIZE — keep it a touch under the number's cap height.
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = stringResource(R.string.rating_of_five, rating),
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier
                .padding(start = 3.dp)
                .size(13.dp),
        )
    }
}

/**
 * At most five, then a `+n`. A row of twenty chips is not a card, it is a paragraph.
 *
 * 💡 TAG SPACING — `Arrangement.spacedBy` is the gap between chips.
 */
@Composable
private fun TagRow(tags: List<String>, modifier: Modifier = Modifier) {
    val visible = tags.take(MAX_VISIBLE_TAGS)
    val overflow = tags.size - visible.size

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        visible.forEach { tag -> TagChip(tag) }
        if (overflow > 0) TagChip("+$overflow")
    }
}

private const val MAX_VISIBLE_TAGS = 5

@Composable
private fun TagChip(text: String) {
    // 💡 TAG CHIP — `labelSmall` is the text size; the padding below is the chip's size.
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Preview(name = "Card — light", showBackground = true)
@Preview(name = "Card — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun RecipeCardPreview() {
    FujiTheme {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RecipeCard(
                RecipeCardModel(
                    id = "a",
                    name = "Kodachrome 64",
                    filmSimulationId = "classic-chrome",
                    rating = 5,
                    tags = listOf("street", "warm", "summer", "portra", "faded", "extra"),
                ),
                onClick = {},
            )
            // The omission rules, drawn: no rating pill, no tag row.
            RecipeCard(
                RecipeCardModel(
                    id = "b",
                    name = "Acros Night",
                    filmSimulationId = "acros-r",
                    rating = 0,
                    tags = emptyList(),
                ),
                onClick = {},
            )
            // A simulation this build does not know: grey swatch, raw id as the label.
            RecipeCard(
                RecipeCardModel(
                    id = "c",
                    name = "From A Newer Client",
                    filmSimulationId = "velvia-ii",
                    rating = 3,
                    tags = listOf("test"),
                ),
                onClick = {},
            )
        }
    }
}
