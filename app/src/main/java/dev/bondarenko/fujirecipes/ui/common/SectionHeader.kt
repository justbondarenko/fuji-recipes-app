package dev.bondarenko.fujirecipes.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A group heading, above its card rather than inside it.
 *
 * Out here at `titleLarge` — 18sp bold — it is a prominent heading, and the controls below
 * are plainly its contents.
 *
 * Used by both the recipe view and the editor so the two screens structure a parameter set the same way.
 */
// =========================================================================================
// SECTION HEADERS STYLING & SPACING (Shared by Recipe View & Recipe Editor)
// =========================================================================================
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier, showDivider: Boolean = true) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            // 💡 MARGIN TOP: Change `8.dp` below to increase/decrease space above the section divider
            .then(if (showDivider) Modifier.padding(top = 8.dp) else Modifier)
    ) {
        if (showDivider) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                // 💡 MARGIN BOTTOM: Space between the divider line and the section title text
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
        // 💡 SECTION TITLE FONT STYLE & SIZE:
        // - To use a theme preset: e.g. MaterialTheme.typography.titleLarge (18sp) or titleMedium (16sp)
        // - Or set custom font size directly: add `fontSize = 18.sp`
        style = MaterialTheme.typography.titleLarge,
        // 💡 FONT WEIGHT: FontWeight.Bold, FontWeight.SemiBold, FontWeight.Normal, etc.
        fontWeight = FontWeight.Bold,
        // 💡 FONT COLOR:
        color = MaterialTheme.colorScheme.onSurface,
        // 💡 SPACING: Adjust padding around title text
        modifier = modifier.fillMaxWidth().padding(start = 4.dp, bottom = 4.dp),
    )
}
