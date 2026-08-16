package dev.bondarenko.fujirecipes.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.bondarenko.fujirecipes.ui.theme.LocalReducedMotion

/**
 * The one loading indicator in this app.
 *
 * M3's shape-morphing `LoadingIndicator`, reachable since the AGP 9.3 / `compileSdk` 37 move.
 * Everything that spins in this app goes through here, so a future change of mind about what
 * loading looks like is one body, not a hunt through screens.
 */
/**
 * A whole-screen wait: M3's contained loading indicator in the middle of whatever it is given.
 *
 * For the screens where waiting *is* the content — the library before its first load, reading
 * C1–C7 off the camera — as opposed to a bar tucked under a line of text in a sheet that has
 * other things on it.
 *
 * Pass `Modifier.fillParentMaxSize()` from a `LazyItemScope`, or `fillMaxSize()` otherwise, so
 * the centring is against the viewport rather than against the content's own height.
 *
 * [progress] is null when there is nothing to count, [label] when there is nothing to say.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FujiCenteredLoading(
    modifier: Modifier = Modifier,
    label: String? = null,
    progress: (() -> Float)? = null,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (progress == null) {
                ContainedLoadingIndicator()
            } else {
                ContainedLoadingIndicator(progress = { progress().coerceIn(0f, 1f) })
            }
            if (label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * The one determinate progress bar in this app.
 *
 * M3's wavy indicator: the wave is the signal that something is genuinely happening on the
 * other end, which a flat bar sitting at 40% cannot distinguish from a stall — the reason it
 * matters most during a camera write, where the phone is not the thing you are looking at.
 *
 * Under reduced motion the wave flattens to zero amplitude rather than the bar disappearing:
 * the progress is still information, only the animation is the part that was objected to
 * (`design-system.md` §5).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FujiProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
) {
    LinearWavyProgressIndicator(
        progress = { progress().coerceIn(0f, 1f) },
        modifier = modifier.fillMaxWidth(),
        amplitude = if (LocalReducedMotion.current) {
            { 0f }
        } else {
            WavyProgressIndicatorDefaults.indicatorAmplitude
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FujiLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    LoadingIndicator(
        modifier = modifier.size(size),
        color = color,
    )
}
