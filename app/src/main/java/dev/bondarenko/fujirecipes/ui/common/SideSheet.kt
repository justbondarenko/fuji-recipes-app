package dev.bondarenko.fujirecipes.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import kotlin.math.roundToInt

/** Detached M3 modal side sheet; Dialog owns modal focus, accessibility and Back. */
@Composable
fun FujiModalSideSheet(
    visible: Boolean,
    title: String,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    val visibility = remember { MutableTransitionState(false) }
    visibility.targetState = visible
    if (!visibility.currentState && !visibility.targetState) return

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false, windowTitle = title),
    ) {
        // Draw the theme's scrim once, including during the exit animation.
        val window = (LocalView.current.parent as DialogWindowProvider).window
        SideEffect { window.setDimAmount(0f) }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val sheetWidth = minOf((maxWidth - 32.dp).coerceAtLeast(0.dp), 400.dp)
            val density = LocalDensity.current
            val direction = if (LocalLayoutDirection.current == LayoutDirection.Ltr) 1 else -1
            val widthPx = with(density) { sheetWidth.toPx() }
            val flingVelocity = with(density) { 125.dp.toPx() }
            var dragOffset by remember { mutableFloatStateOf(0f) }
            var dragging by remember { mutableStateOf(false) }
            val offset by animateFloatAsState(dragOffset, animationSpec = if (dragging) snap() else spring())

            AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
                Box(Modifier.fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
                    .focusProperties { canFocus = false }
                    .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDismissRequest)
                    .clearAndSetSemantics {})
            }
            AnimatedVisibility(
                visibleState = visibility,
                enter = slideInHorizontally { it * direction },
                exit = slideOutHorizontally { it * direction },
                modifier = Modifier.align(Alignment.CenterEnd)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(16.dp)
                    .offset { IntOffset((offset * direction).roundToInt(), 0) },
            ) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxHeight().width(sheetWidth)
                        .semantics { paneTitle = title; isTraversalGroup = true }
                        .draggable(
                            state = rememberDraggableState { dragOffset = (dragOffset + it * direction).coerceIn(0f, widthPx) },
                            orientation = Orientation.Horizontal,
                            onDragStarted = { dragging = true },
                            onDragStopped = { velocity ->
                                dragging = false
                                if (dragOffset > widthPx / 2 || velocity * direction > flingVelocity) onDismissRequest()
                                else dragOffset = 0f
                            },
                        ),
                ) { content() }
            }
        }
    }
}
