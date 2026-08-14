package dev.bondarenko.fujirecipes.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp, bottom = 2.dp),
    )
}
