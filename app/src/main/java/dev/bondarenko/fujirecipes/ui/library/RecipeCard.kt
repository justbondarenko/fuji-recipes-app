package dev.bondarenko.fujirecipes.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
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
 * Designed according to Material 3 Expressive best practices:
 * - Balanced 16dp / 14dp padding all around
 * - Circular avatar badge with no corner artifacts
 * - Clear typographic hierarchy (titleMedium SemiBold + bodySmall)
 * - Vertically centered rating pill and tags
 *
 * Omission rules:
 * - rating 0 shows **no** pill, not a zero
 * - no tags shows **no** tag row
 */
@Composable
fun RecipeCard(
    recipe: RecipeCardModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardColor())
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        FilmSimBadge(
            simulationId = recipe.filmSimulationId,
            size = 48.dp,
            shape = CircleShape,
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = recipe.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (recipe.tags.isNotEmpty()) {
                TagRow(recipe.tags, modifier = Modifier.padding(top = 2.dp))
            }
        }

        if (recipe.rating > 0) {
            RatingBadge(recipe.rating)
        }
    }
}

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
        modifier = modifier,
    ) {
        Text(
            text = rating.toString(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontFeatureSettings = TabularFigures,
            ),
        )
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = stringResource(R.string.rating_of_five, rating),
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier
                .padding(start = 2.dp)
                .size(11.dp),
        )
    }
}

/** At most five, then a `+n`. A row of twenty chips is not a card, it is a paragraph. */
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
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 8.dp, vertical = 2.dp),
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
