package dev.bondarenko.fujirecipes.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A group heading, above its card rather than inside it.
 *
 * It used to be a `labelMedium` line tucked inside the card's top padding, which made it
 * read as the first row's caption rather than as the name of the group. Out here at
 * `titleMedium` — Lora, 16sp — it is a heading, and the card below is plainly its contents.
 *
 * Added after design review; used by both the recipe view and the editor so the two screens
 * structure a parameter set the same way.
 */
/**
 * A divider above the heading, except on the first section.
 *
 * M3's divider guidance: a rule earns its place when it separates groups that would
 * otherwise run together. Sections here already sit in their own containers, so the divider
 * goes *above the heading* rather than between cards — it marks where one group of settings
 * ends and the next begins, which is a different job from the row separators inside a card.
 */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier, showDivider: Boolean = true) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (showDivider) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }
        SectionTitle(text)
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.fillMaxWidth().padding(start = 4.dp, bottom = 2.dp),
    )
}
