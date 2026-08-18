package dev.bondarenko.fujirecipes.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val ShapeSize = 140.dp

/**
 * The standard shape of a page that has one thing to say and one thing to do.
 *
 * An icon in an M3 Expressive shape, a heading, a line of explanation and a single call to
 * action, centred on the screen. Screens share this unified structure, differing only in
 * the shape, icon, title, body, and action.
 *
 * The shape is the page's identity, per `m3.material.io/styles/shape/overview-principles`:
 * e.g., `MaterialShapes.Pill`, `MaterialShapes.Pentagon`, or `MaterialShapes.Square`.
 * Callers pass a [Shape] rather than a `RoundedPolygon`, so the call site reads
 * `MaterialShapes.<Shape>.toShape()`.
 *
 * [icon] must be the same drawable the toolbar uses for that destination. A page that
 * announces itself with a different glyph than the item you pressed to get there reads as a
 * different place.
 */

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FujiIconPanel(
    icon: ImageVector,
    shape: Shape,
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    shapeSize: Dp = ShapeSize,
    iconSize: Dp = 56.dp,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    actionIsPrimary: Boolean = true,
    extra: (@Composable ColumnScope.() -> Unit)? = null,
) {
    FujiIconPanel(
        icon = rememberVectorPainter(icon),
        shape = shape,
        title = title,
        modifier = modifier,
        body = body,
        containerColor = containerColor,
        contentColor = contentColor,
        shapeSize = shapeSize,
        iconSize = iconSize,
        actionLabel = actionLabel,
        onAction = onAction,
        actionIsPrimary = actionIsPrimary,
        extra = extra,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FujiIconPanel(
    icon: Painter,
    shape: Shape,
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    shapeSize: Dp = ShapeSize,
    iconSize: Dp = 56.dp,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    /** False for a step away from the happy path — disconnecting, mainly. */
    actionIsPrimary: Boolean = true,
    /** Anything the page needs under the action; the camera's per-state notes use it. */
    extra: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                // Square, always. A shape stretched to a rectangle is no longer the shape M3
                // drew — the pill was distorted when this took a DpSize.
                modifier = Modifier
                    .size(shapeSize)
                    .clip(shape)
                    .background(containerColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(iconSize),
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (body != null) {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (actionLabel != null && onAction != null) {
                // M3's medium button: the one action on the page, sized by the spec rather
                // than by a modifier of our own.
                val buttonModifier = Modifier
                    .padding(top = 8.dp)
                    .heightIn(min = ButtonDefaults.MediumContainerHeight)
                val padding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight)
                val label: @Composable () -> Unit = { Text(actionLabel) }
                if (actionIsPrimary) {
                    Button(
                        onClick = onAction,
                        modifier = buttonModifier,
                        contentPadding = padding,
                    ) { label() }
                } else {
                    OutlinedButton(
                        onClick = onAction,
                        modifier = buttonModifier,
                        contentPadding = padding,
                    ) { label() }
                }
            }

            extra?.invoke(this)
        }
    }
}
