package dev.bondarenko.fujirecipes.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

/** The breathing room between the menu and the control that opened it. */
private val MenuGap = 4.dp

/**
 * A menu that stays attached to the control that opened it, even at the edge of the screen.
 *
 * `DropdownMenu` cannot: its position provider keeps popups at least 48dp inside the window,
 * so for an anchor that already sits in that band — a bottom bar, a floating toolbar, a split
 * button — every "above the anchor" placement is rejected and it falls back to pinning the
 * menu to the bottom of the window, which reads as a menu belonging to nothing. That fallback
 * also ignores `offset`, so there is nothing to nudge.
 *
 * This places the menu itself: flush above the anchor when there is room, below it otherwise,
 * aligned to the anchor's leading edge and clamped to the window either way. The content is
 * `DropdownMenuItem`s, so the items are still M3's.
 */
@Composable
fun FujiAnchoredMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!expanded) return

    val gap = with(LocalDensity.current) { MenuGap.roundToPx() }
    val positionProvider = remember(gap) { AnchoredMenuPositionProvider(gap) }

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraSmall,
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 3.dp,
            shadowElevation = 3.dp,
        ) {
            Column(
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                content = content,
            )
        }
    }
}

private class AnchoredMenuPositionProvider(private val gap: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val start = if (layoutDirection == LayoutDirection.Ltr) {
            anchorBounds.left
        } else {
            anchorBounds.right - popupContentSize.width
        }
        val x = start.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))

        val above = anchorBounds.top - gap - popupContentSize.height
        val y = if (above >= 0) {
            above
        } else {
            (anchorBounds.bottom + gap)
                .coerceAtMost((windowSize.height - popupContentSize.height).coerceAtLeast(0))
        }
        return IntOffset(x, y)
    }
}
