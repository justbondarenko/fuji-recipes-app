package dev.bondarenko.fujirecipes.ui.library

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import dev.bondarenko.fujirecipes.ui.theme.LocalReducedMotion
import kotlin.math.roundToInt

/**
 * Remeasure the list at each animation frame. The detail keeps its final width and is
 * revealed by the same moving edge, so its controls never squeeze through a zero-width pane.
 * Both slots keep their composition positions when a window folds or changes size.
 */
@Composable
fun LibraryDetailLayout(
    twoPane: Boolean,
    detailOpen: Boolean,
    modifier: Modifier = Modifier,
    list: @Composable () -> Unit,
    detail: @Composable () -> Unit,
) {
    val reducedMotion = LocalReducedMotion.current
    val transition = updateTransition(twoPane && detailOpen, label = "Library detail reveal")
    val progress by transition.animateFloat(
        transitionSpec = {
            tween(durationMillis = if (reducedMotion) 0 else 400, easing = FastOutSlowInEasing)
        },
        label = "List width",
    ) { open -> if (open) 1f else 0f }
    val keepDetail = detailOpen || transition.currentState || transition.targetState

    Layout(
        modifier = modifier.clipToBounds(),
        content = {
            Box(Modifier.fillMaxSize()) { list() }
            if (keepDetail) {
                Box(Modifier.fillMaxSize().clipToBounds()) {
                    detail()
                    if (twoPane) VerticalDivider()
                }
            }
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val targetListWidth = (width * 0.4f).roundToInt()
            .coerceIn(300.dp.roundToPx(), 360.dp.roundToPx())
            .coerceAtMost(width)
        val listWidth = if (twoPane) {
            (width - (width - targetListWidth) * progress).roundToInt()
        } else {
            width
        }
        val listPlaceable = measurables[0].measure(Constraints.fixed(listWidth, height))
        val detailWidth = if (twoPane) width - targetListWidth else width
        val detailPlaceable = measurables.getOrNull(1)?.measure(Constraints.fixed(detailWidth, height))

        layout(width, height) {
            // Do not place the list behind a compact detail: it must not receive touches or
            // accessibility focus. Measuring it preserves its scroll/search/selection state.
            if (twoPane || !detailOpen) listPlaceable.placeRelative(0, 0)
            if (detailPlaceable != null && (!twoPane || progress > 0f)) {
                detailPlaceable.placeRelative(if (twoPane) listWidth else 0, 0)
            }
        }
    }
}
