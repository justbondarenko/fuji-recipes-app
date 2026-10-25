package dev.bondarenko.fujirecipes.ui.raweditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.ui.theme.icons.FujiIcons
import dev.bondarenko.fujirecipes.ui.theme.icons.Compare
import dev.bondarenko.fujirecipes.ui.theme.icons.ScreenshotFrame2
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
internal fun RawEditorPreview(
    state: RawEditorState, grid: Boolean, showComparison: Boolean, onShowComparison: (Boolean) -> Unit,
    onViewport: (RawViewport?) -> Unit, modifier: Modifier = Modifier, onCrop: () -> Unit = {},
) {
    val image = IntSize(
        state.fullWidth.takeIf { it > 0 } ?: state.preview?.width ?: 1,
        state.fullHeight.takeIf { it > 0 } ?: state.preview?.height ?: 1,
    )
    var view by remember { mutableStateOf(IntSize.Zero) }
    var position by remember(state.cameraImage, image, state.adjustments.frameKey(), view) { mutableStateOf<RawZoom?>(null) }
    val fit = RawZoom(0f).clamped(image, IntSize(view.width.coerceAtLeast(1), view.height.coerceAtLeast(1)))
    val zoom = position ?: fit
    val zoomed = zoom.scale > fit.scale + .0001f
    val viewport = if ((zoomed || fit.scale == 1f) && state.fullWidth > 0 && view != IntSize.Zero) zoom.viewport(image, view) else null
    val requestViewport by rememberUpdatedState(onViewport)
    val compare by rememberUpdatedState(onShowComparison)
    var compareStarting by rememberSaveable(state.name) { mutableStateOf(state.cameraImage == null) }
    var comparisonMenu by remember { mutableStateOf(false) }
    val comparisonImage = if (compareStarting) state.startingImage else state.cameraImage
    val canCompare = !state.busy && comparisonImage != null
    fun toggleZoom(at: Offset = Offset(view.width / 2f, view.height / 2f)) {
        position = if (zoomed) null else zoom.transform(image, view, at, Offset.Zero, 1f / zoom.scale)
    }
    val toggle by rememberUpdatedState<(Offset) -> Unit>({ toggleZoom(it) })
    LaunchedEffect(viewport, state.busy) {
        if (!state.busy) {
            if (viewport != null) delay(100)
            requestViewport(viewport)
        }
    }
    DisposableEffect(Unit) { onDispose { requestViewport(null) } }
    val rawLabel = stringResource(R.string.raw_editor_raw_image)
    val compareLabel = stringResource(when {
        showComparison -> R.string.raw_editor_show_raw
        compareStarting -> R.string.raw_editor_show_starting
        else -> R.string.raw_editor_show_camera
    })
    val zoomLabel = stringResource(R.string.raw_editor_zoom_100)
    val fitLabel = stringResource(R.string.raw_editor_zoom_fit)
    val panLeft = stringResource(R.string.raw_editor_pan_left)
    val panRight = stringResource(R.string.raw_editor_pan_right)
    val panUp = stringResource(R.string.raw_editor_pan_up)
    val panDown = stringResource(R.string.raw_editor_pan_down)
    Box(
        modifier.background(Color.Black).clipToBounds().testTag("raw-editor-preview").onSizeChanged { view = it }
            .pointerInput(image, view, comparisonImage, state.busy) {
                detectTapGestures(
                    onDoubleTap = { if (!state.busy) toggle(it) },
                    onLongPress = { if (canCompare) compare(true) },
                    onPress = { try { tryAwaitRelease() } finally { compare(false) } },
                )
            }
            .pointerInput(image, view, state.busy) {
                detectTransformGestures { centroid, pan, factor, _ ->
                    if (!state.busy) {
                        compare(false)
                        val current = position ?: fit
                        position = current.transform(image, view, centroid, pan, factor)
                    }
                }
            }
            .semantics(mergeDescendants = true) {
                customActions = buildList {
                    if (canCompare) add(CustomAccessibilityAction(compareLabel) { compare(!showComparison); true })
                    if (!state.busy) {
                        add(CustomAccessibilityAction(if (zoomed) fitLabel else zoomLabel) { toggleZoom(); true })
                        if (zoomed) for ((label, direction) in listOf(panLeft to Offset(view.width / 2f, 0f),
                            panRight to Offset(-view.width / 2f, 0f), panUp to Offset(0f, view.height / 2f),
                            panDown to Offset(0f, -view.height / 2f))) {
                            add(CustomAccessibilityAction(label) { position = zoom.copy(offset = zoom.offset + direction).clamped(image, view); true })
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (showComparison) {
            comparisonImage?.let { Image(it.asImageBitmap(), stringResource(if (compareStarting) R.string.raw_editor_starting_image else R.string.raw_editor_camera_image),
                Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
        } else Canvas(Modifier.fillMaxSize()
            .then(if (grid) Modifier.testTag("raw-editor-optics-grid") else Modifier)
            .semantics { contentDescription = rawLabel }) {
            val origin = zoom.origin(image, view)
            val target = IntSize((image.width * zoom.scale).roundToInt().coerceAtLeast(1),
                (image.height * zoom.scale).roundToInt().coerceAtLeast(1))
            val translation = IntOffset(origin.x.roundToInt(), origin.y.roundToInt())
            state.preview?.let { drawImage(it.asImageBitmap(), dstOffset = translation, dstSize = target) }
            state.detail?.takeIf { it.edits == state.adjustments && it.viewport.scale == zoom.scale }?.let {
                drawImage(it.bitmap.asImageBitmap(), dstOffset = translation + IntOffset(it.viewport.left, it.viewport.top),
                    filterQuality = FilterQuality.None)
            }
            if (grid) for (line in 1..3) {
                val x = origin.x + target.width * line / 4f
                val y = origin.y + target.height * line / 4f
                drawLine(Color.White.copy(alpha = .35f), Offset(x, origin.y), Offset(x, origin.y + target.height))
                drawLine(Color.White.copy(alpha = .35f), Offset(origin.x, y), Offset(origin.x + target.width, y))
            }
        }
        if (!showComparison && !state.busy) Surface(Modifier.align(Alignment.TopEnd).padding(8.dp),
            color = Color.Black.copy(alpha = .75f), contentColor = Color.White) {
            TextButton(onClick = { toggleZoom() }, Modifier.testTag("raw-editor-zoom").semantics { contentDescription = if (zoomed) fitLabel else zoomLabel }) {
                Text(if (zoomed) "${(zoom.scale * 100).roundToInt()}%" else fitLabel, color = Color.White)
            }
        }
        Box(Modifier.align(Alignment.BottomStart).padding(8.dp)) {
            Surface(shape = androidx.compose.foundation.shape.CircleShape,
                color = Color.Black.copy(alpha = .7f), contentColor = Color.White) {
                IconButton(
                    onClick = { if (showComparison) compare(false) else comparisonMenu = true },
                    enabled = !state.busy && (state.cameraImage != null || state.startingImage != null),
                    modifier = Modifier.testTag("raw-editor-compare"),
                ) { Icon(FujiIcons.Compare, stringResource(if (showComparison) R.string.raw_editor_show_raw else R.string.raw_editor_compare)) }
            }
            DropdownMenu(expanded = comparisonMenu && !state.busy, onDismissRequest = { comparisonMenu = false }) {
                listOf(false, true).forEach { starting ->
                    DropdownMenuItem(
                        text = { Text(stringResource(if (starting) R.string.raw_editor_before_edits else R.string.raw_editor_embedded_jpeg)) },
                        leadingIcon = { RadioButton(selected = compareStarting == starting, onClick = null) },
                        enabled = (if (starting) state.startingImage else state.cameraImage) != null,
                        onClick = { compareStarting = starting; comparisonMenu = false; compare(true) },
                    )
                }
            }
        }
        val loading = viewport != null && (state.detailLoading || state.detail?.viewport != viewport || state.detail.edits != state.adjustments)
        Surface(Modifier.align(Alignment.BottomEnd).padding(8.dp),
            shape = androidx.compose.foundation.shape.CircleShape,
            color = Color.Black.copy(alpha = .7f), contentColor = Color.White) {
            IconButton(onClick = onCrop, enabled = !state.busy, modifier = Modifier.testTag("raw-editor-crop")) {
                Icon(FujiIcons.ScreenshotFrame2, stringResource(R.string.raw_editor_framing))
            }
        }
        if (showComparison && !state.busy) Surface(Modifier.align(Alignment.TopCenter).padding(8.dp).testTag("raw-editor-comparison-indicator"),
            shape = androidx.compose.foundation.shape.CircleShape,
            color = Color.Black.copy(alpha = .8f), contentColor = Color.White) {
            Text(stringResource(if (compareStarting) R.string.raw_editor_starting_note else R.string.raw_editor_comparison_note),
                Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
        if (state.busy || (loading && !showComparison)) Surface(Modifier.align(Alignment.BottomCenter).padding(horizontal = 64.dp),
            color = Color.Black.copy(alpha = .8f), contentColor = Color.White) {
            Text(stringResource(when {
                state.busy -> R.string.raw_editor_processing
                state.error != null -> R.string.raw_editor_detail_failed
                else -> R.string.raw_editor_detail_loading
            }), Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
        }
    }
}
