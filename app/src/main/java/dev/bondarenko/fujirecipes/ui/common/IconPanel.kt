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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * The standard shape of a page that has one thing to say and one thing to do.
 *
 * An icon in an M3 Expressive shape, a heading, a line of explanation and a single call to
 * action, centred on the screen. Five screens were each drawing their own version of this;
 * they now differ only in the parts that should differ — the shape, the icon and the words.
 *
 * The shape is the page's identity, per `m3.material.io/styles/shape/overview-principles`:
 * `MaterialShapes.Pill` for reading a photo, `Arrow` for the two imports, `Pentagon` for the
 * camera. Callers pass a [Shape] rather than a `RoundedPolygon`, so a rotated arrow is just
 * `MaterialShapes.Arrow.toShape(startAngle = 90)` at the call site.
 *
 * [icon] must be the same drawable the toolbar uses for that destination. A page that
 * announces itself with a different glyph than the item you pressed to get there reads as a
 * different place.
 */
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
    /** Wider than tall for `Pill`, which otherwise normalises into a squircle. */
    shapeSize: DpSize = DpSize(140.dp, 140.dp),
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
                    modifier = Modifier.size(56.dp),
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
                // The one action on the page, at M3's large button height rather than the
                // default — it is the only thing to press, so it should not look like a
                // footnote under the paragraph.
                val buttonModifier = Modifier
                    .padding(top = 8.dp)
                    .heightIn(min = ButtonDefaults.LargeContainerHeight)
                val label: @Composable () -> Unit = {
                    Text(text = actionLabel, style = MaterialTheme.typography.titleMedium)
                }
                if (actionIsPrimary) {
                    Button(
                        onClick = onAction,
                        modifier = buttonModifier,
                        contentPadding = ButtonDefaults.contentPaddingFor(
                            ButtonDefaults.LargeContainerHeight,
                        ),
                    ) { label() }
                } else {
                    OutlinedButton(
                        onClick = onAction,
                        modifier = buttonModifier,
                        contentPadding = ButtonDefaults.contentPaddingFor(
                            ButtonDefaults.LargeContainerHeight,
                        ),
                    ) { label() }
                }
            }

            extra?.invoke(this)
        }
    }
}
