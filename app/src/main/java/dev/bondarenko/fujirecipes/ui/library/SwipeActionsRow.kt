package dev.bondarenko.fujirecipes.ui.library

import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Which way the row is resting. */
enum class SwipeState { Closed, Open }

/** Position of a button within an M3 Connected Button Group. */
enum class ButtonGroupPosition { Start, Middle, End, Standalone }

/**
 * A list row that slides aside to reveal its actions — M3 lists, swipe behaviour.
 *
 * Implements Material 3 Connected Button Group standard variant with rounded connected shapes.
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
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(8.dp))
            RowScopeActions.actions()
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset {
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
 * One revealed action as an M3 Connected Button Group item with horizontal icon + label.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RowScopeActions.SwipeAction(
    icon: Painter,
    label: String,
    onClick: () -> Unit,
    position: ButtonGroupPosition = ButtonGroupPosition.Standalone,
    enabled: Boolean = true,
    container: Color = MaterialTheme.colorScheme.secondaryContainer,
    content: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    val shape = when (position) {
        ButtonGroupPosition.Start -> RoundedCornerShape(
            topStart = 24.dp,
            bottomStart = 24.dp,
            topEnd = 8.dp,
            bottomEnd = 8.dp,
        )
        ButtonGroupPosition.Middle -> RoundedCornerShape(percent = 50)
        ButtonGroupPosition.End -> RoundedCornerShape(
            topStart = 8.dp,
            bottomStart = 8.dp,
            topEnd = 24.dp,
            bottomEnd = 24.dp,
        )
        ButtonGroupPosition.Standalone -> RoundedCornerShape(percent = 50)
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
        ),
        contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
        modifier = Modifier.heightIn(min = ButtonDefaults.MediumContainerHeight),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.MediumIconSize),
            )
            Text(
                text = label,
                maxLines = 1,
            )
        }
    }
}
