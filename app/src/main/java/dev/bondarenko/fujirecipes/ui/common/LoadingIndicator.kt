package dev.bondarenko.fujirecipes.ui.common

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.bondarenko.fujirecipes.ui.theme.LocalReducedMotion

/**
 * Progress, eased rather than stepped.
 *
 * Every determinate source in this app counts whole things — slot 3 of 7, field 11 of 17 — so
 * feeding it straight to an indicator makes it jump in visible increments. The spring M3's own
 * determinate sample uses carries it between steps instead: no bounce, very low stiffness, and
 * a visibility threshold fine enough that the last fraction still lands.
 *
 * Reduced motion takes the raw value. The number is the information; the easing is not.
 */
@Composable
private fun easedProgress(progress: () -> Float): Float {
    val target = progress().coerceIn(0f, 1f)
    if (LocalReducedMotion.current) return target

    val eased by animateFloatAsState(
        targetValue = target,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessVeryLow,
            visibilityThreshold = 1 / 1000f,
        ),
        label = "progress",
    )
    return eased
}

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
                val eased = easedProgress(progress)
                ContainedLoadingIndicator(progress = { eased })
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
 * The value is eased through [easedProgress], because a write counts fields one at a time and
 * an un-eased bar lurches once per field.
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
    val eased = easedProgress(progress)
    LinearWavyProgressIndicator(
        progress = { eased },
        modifier = modifier.fillMaxWidth(),
        amplitude = if (LocalReducedMotion.current) {
            { 0f }
        } else {
            WavyProgressIndicatorDefaults.indicatorAmplitude
        },
    )
}

/**
 * The one inline spinner in this app.
 *
 * M3's shape-morphing `LoadingIndicator`, reachable since the AGP 9.3 / `compileSdk` 37 move.
 * Everything that spins inside other content goes through here; a wait that owns the whole
 * screen wants [FujiCenteredLoading] instead.
 */
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
