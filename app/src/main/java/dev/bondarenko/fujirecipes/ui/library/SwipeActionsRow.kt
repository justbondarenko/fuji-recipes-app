package dev.bondarenko.fujirecipes.ui.library

import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Which way the row is resting. */
enum class SwipeState { Closed, Open }

/**
 * A list row that slides aside to reveal its actions — M3 lists, swipe behaviour.
 *
 * Not `SwipeToDismissBox`: that commits an action when the swipe passes a threshold, which
 * is right for one destructive gesture and wrong for a menu of three. This holds open at an
 * anchor so the actions can be read and then chosen, and a tap anywhere else closes it.
 *
 * **Only one row is open at a time.** Two rows open at once is how a delete gets pressed on
 * the wrong recipe, so opening this one asks the caller to close whatever was open.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeActionsRow(
    isOpen: Boolean,
    onOpenChange: (Boolean) -> Unit,
    actions: @Composable RowScopeActions.() -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    var actionsWidth by remember { mutableIntStateOf(0) }

    val state = remember {
        AnchoredDraggableState(
            initialValue = SwipeState.Closed,
            positionalThreshold = { distance: Float -> distance * 0.4f },
            velocityThreshold = { with(density) { 120.dp.toPx() } },
            snapAnimationSpec = spring(),
            decayAnimationSpec = exponentialDecay(),
        )
    }

    // The open anchor is the measured width of the actions, so the row slides exactly far
    // enough to show them and no further.
    LaunchedEffect(actionsWidth) {
        state.updateAnchors(
            DraggableAnchors {
                SwipeState.Closed at 0f
                if (actionsWidth > 0) SwipeState.Open at -actionsWidth.toFloat()
            },
        )
    }

    // The caller owns "which row is open"; this keeps the gesture and that state agreeing
    // in both directions.
    LaunchedEffect(isOpen) {
        if (isOpen && state.currentValue != SwipeState.Open && actionsWidth > 0) {
            state.animateTo(SwipeState.Open)
        } else if (!isOpen && state.currentValue != SwipeState.Closed) {
            state.animateTo(SwipeState.Closed)
        }
    }

    LaunchedEffect(state.currentValue) {
        onOpenChange(state.currentValue == SwipeState.Open)
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .onSizeChanged { actionsWidth = it.width },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(8.dp))
            RowScopeActions.actions()
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset {
                    /**
                     * `offset`, not `requireOffset()`.
                     *
                     * The anchors depend on measuring the action row, so they are set after
                     * the first layout pass — and `requireOffset()` throws outright if it is
                     * read before then. It is NaN on that first frame, which means "resting
                     * closed", so that is what it draws.
                     */
                    val x = state.offset.takeIf { !it.isNaN() } ?: 0f
                    IntOffset(x.roundToInt(), 0)
                }
                .anchoredDraggable(state, Orientation.Horizontal),
        ) {
            content()
        }
    }
}

/** Marker receiver, so the actions slot reads as a list of actions rather than any Row. */
object RowScopeActions

/**
 * One revealed action as an M3 Expressive round narrow icon button.
 *
 * Disabled actions are shown rather than hidden: "write to camera" exists and is coming
 * (FEAT-006), and a row whose action set changes shape between builds is harder to learn
 * than one where a control is visibly not ready yet.
 */
@Composable
fun RowScopeActions.SwipeAction(
    icon: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    container: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.secondaryContainer,
    content: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(percent = 50),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
        ),
        modifier = Modifier
            .width(36.dp)
            .height(52.dp),
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(20.dp))
    }
}
