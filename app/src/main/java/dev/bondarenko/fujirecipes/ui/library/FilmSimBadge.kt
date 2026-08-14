package dev.bondarenko.fujirecipes.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.bondarenko.fujirecipes.data.fields.FilmSimulations
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme

/**
 * The film-simulation swatch — FEAT-001 T-16.
 *
 * A circle filled with the simulation's swatch colour, with its sample image over the top.
 * `design-system.md` §3: the swatch is a real value, not a fallback, because the web badge
 * paints it under the image and shows it alone when the image is missing. The same here,
 * which is why an unknown simulation is a grey circle rather than a broken row.
 *
 * Decorative by default: the card names the simulation in text right beside it, so a screen
 * reader announcing it twice would be noise.
 */
@Composable
fun FilmSimBadge(
    simulationId: String?,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
) {
    val simulation = FilmSimulations.byId(simulationId)
    val ring = if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) {
        Color.Black.copy(alpha = 0.10f)
    } else {
        Color.White.copy(alpha = 0.20f)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(simulation?.swatch ?: FilmSimulations.FallbackSwatch)
            .border(1.dp, ring, CircleShape)
            .clearAndSetSemantics { },
    ) {
        simulation?.image?.let { image ->
            Image(
                painter = painterResource(image),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
            )
        }
    }
}

/** Cheap perceptual luminance — enough to pick which ring reads as a hairline. */
private fun Color.luminance(): Float = 0.299f * red + 0.587f * green + 0.114f * blue

@Preview(name = "Badges — light", showBackground = true)
@Preview(name = "Badges — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun FilmSimBadgePreview() {
    FujiTheme {
        Row {
            FilmSimBadge("classic-chrome")
            FilmSimBadge("velvia")
            FilmSimBadge("acros")
            // The two that have to survive: an id from a newer client, and none at all.
            FilmSimBadge("a-simulation-from-2030")
            FilmSimBadge(null)
        }
    }
}
