package dev.bondarenko.fujirecipes.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.bondarenko.fujirecipes.R
import kotlin.math.roundToInt

internal val LocalLibraryPaneSplit = compositionLocalOf<LibraryPaneSplit?> { null }
internal val LibraryTwoPaneMinWidth = 720.dp
internal val LibraryPaneMinWidth = 300.dp

internal class LibraryPaneSplit(
    private val windowWidth: Int,
    contentWidth: Int,
    minPaneWidth: Int,
    private val preferredFraction: MutableFloatState,
) {
    private val railWidth = windowWidth - contentWidth
    // Keep the center available even if a display inset makes the rail unusually wide.
    private val minListWidth = minOf(minPaneWidth, (windowWidth / 2 - railWidth).coerceAtLeast(0))
    val range = (railWidth + minListWidth).toFloat() / windowWidth..1f - minPaneWidth.toFloat() / windowWidth
    val fraction: Float get() = preferredFraction.floatValue.coerceIn(range)
    val listWidth: Int get() = (windowWidth * fraction).roundToInt() - railWidth

    fun resizeBy(pixels: Float) {
        resizeTo(fraction + pixels / windowWidth)
    }

    fun resizeTo(fraction: Float): Boolean {
        val next = fraction.coerceIn(range)
        if (this.fraction == next) return false
        preferredFraction.floatValue = next
        return true
    }
}

/** Both panes keep their composition positions as the window folds or the divider moves. */
@Composable
internal fun LibraryDetailLayout(
    split: LibraryPaneSplit?,
    detailOpen: Boolean,
    modifier: Modifier = Modifier,
    list: @Composable () -> Unit,
    detail: @Composable () -> Unit,
) {
    val dividerColor = MaterialTheme.colorScheme.outlineVariant
    Layout(
        modifier = modifier.clipToBounds(),
        content = {
            Box(Modifier.fillMaxSize().clipToBounds()) { list() }
            if (split != null || detailOpen) {
                Box(Modifier.fillMaxSize().clipToBounds()) { detail() }
            }
            if (split != null) {
                Box(Modifier.background(dividerColor))
                LibraryPaneHandle(split)
            }
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val listWidth = split?.listWidth?.coerceIn(0, width) ?: width
        val detailWidth = if (split == null) width else width - listWidth
        val listPlaceable = measurables[0].measure(Constraints.fixed(listWidth, height))
        val detailPlaceable = measurables.getOrNull(1)?.measure(Constraints.fixed(detailWidth, height))
        val divider = measurables.getOrNull(2)?.measure(Constraints.fixed(1.dp.roundToPx(), height))
        val handle = measurables.getOrNull(3)?.measure(constraints.copy(minWidth = 0, minHeight = 0))

        layout(width, height) {
            // Measuring the compact list preserves its scroll, search and selection state.
            if (split != null || !detailOpen) listPlaceable.placeRelative(0, 0)
            detailPlaceable?.placeRelative(if (split == null) 0 else listWidth, 0)
            divider?.placeRelative(listWidth - divider.width / 2, 0)
            handle?.placeRelative(listWidth - handle.width / 2, (height - handle.height) / 2)
        }
    }
}

@Composable
private fun LibraryPaneHandle(split: LibraryPaneSplit) {
    val interaction = remember { MutableInteractionSource() }
    val dragging by interaction.collectIsDraggedAsState()
    val focused by interaction.collectIsFocusedAsState()
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val description = stringResource(R.string.library_resize_panes)
    val dragState = rememberDraggableState { split.resizeBy(it) }
    Box(
        modifier = Modifier
            .size(width = 48.dp, height = 72.dp)
            .draggable(
                state = dragState,
                orientation = Orientation.Horizontal,
                interactionSource = interaction,
                reverseDirection = rtl,
            )
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val step = when (event.key) {
                    Key.DirectionLeft -> -0.05f
                    Key.DirectionRight -> 0.05f
                    else -> return@onKeyEvent false
                }
                split.resizeTo(split.fraction + if (rtl) -step else step)
                true
            }
            .semantics {
                contentDescription = description
                progressBarRangeInfo = ProgressBarRangeInfo(split.fraction, split.range)
                setProgress { split.resizeTo(it) }
            }
            .focusable(interactionSource = interaction),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(width = if (dragging || focused) 8.dp else 4.dp, height = 48.dp)
                .background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape),
        )
    }
}
