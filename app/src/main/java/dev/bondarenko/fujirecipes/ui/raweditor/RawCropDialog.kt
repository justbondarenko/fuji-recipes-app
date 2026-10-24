package dev.bondarenko.fujirecipes.ui.raweditor

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.data.raweditor.RawCrop
import dev.bondarenko.fujirecipes.data.raweditor.RawAdjustments
import dev.bondarenko.fujirecipes.ui.common.FujiAnchoredMenu
import dev.bondarenko.fujirecipes.ui.theme.icons.Check
import dev.bondarenko.fujirecipes.ui.theme.icons.UnfoldMore
import dev.bondarenko.fujirecipes.ui.theme.icons.FujiIcons
import dev.bondarenko.fujirecipes.ui.theme.icons.Refresh
import dev.bondarenko.fujirecipes.ui.theme.icons.Tune

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun RawCropDialog(
    bitmap: Bitmap, initial: RawAdjustments, onDismiss: () -> Unit, onApply: (RawAdjustments) -> Unit,
    onAdjust: (RawAdjustments) -> Unit,
    imageAspect: Float = bitmap.width.toFloat() / bitmap.height,
    rendering: Boolean = false, error: String? = null,
) {
    var crop by remember { mutableStateOf(initial.crop) }
    var ratio by remember { mutableFloatStateOf(0f) }
    // Original and a matching fixed preset remain distinct selections.
    var originalRatio by remember { mutableStateOf(false) }
    var ratioMenuOpen by remember { mutableStateOf(false) }
    val ratios = listOf(stringResource(R.string.raw_editor_crop_free) to 0f,
        stringResource(R.string.raw_editor_crop_original) to imageAspect,
        "1:1" to 1f, "3:2" to 1.5f, "2:3" to 2f / 3, "4:3" to 4f / 3,
        "3:4" to .75f, "16:9" to 16f / 9, "9:16" to 9f / 16)
    val selectedRatio = if (originalRatio) 1 else ratios.indices.first { index ->
        index != 1 && kotlin.math.abs(ratios[index].second - ratio) < .0001f
    }
    val ratioDescription = stringResource(R.string.raw_editor_crop_aspect, ratios[selectedRatio].first)
    fun rotate(delta: Int) {
        val next = initial.copy(crop = crop).rotated((initial.quarterTurns + delta + 4) % 4)
        crop = next.crop
        if (ratio > 0) ratio = 1 / ratio
        onAdjust(next)
    }
    val straighten = RawParam(R.string.raw_editor_straighten, FujiIcons.Tune, initial.straighten, -15f..15f,
        true, 1f, 1, "°") { onAdjust(initial.copy(crop = crop, straighten = it)) }
    val currentStraighten by rememberUpdatedState(straighten)
    val straightenLabel = stringResource(R.string.raw_editor_straighten)
    val resetStraighten = stringResource(R.string.raw_editor_reset_parameter, straightenLabel)
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val currentCrop by rememberUpdatedState(crop)
    val currentRatio by rememberUpdatedState(ratio)
    val aspect = bitmap.width.toFloat() / bitmap.height
    val geometryReady = !rendering && error == null
    val description = stringResource(R.string.raw_editor_crop_hint)
    val labels = listOf(R.string.raw_editor_crop_left, R.string.raw_editor_crop_right,
        R.string.raw_editor_crop_up, R.string.raw_editor_crop_down,
        R.string.raw_editor_crop_smaller, R.string.raw_editor_crop_larger).map { stringResource(it) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.raw_editor_framing), Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Box {
                        FilledTonalButton(
                            onClick = { ratioMenuOpen = true }, enabled = geometryReady,
                            modifier = Modifier.testTag("raw-crop-aspect-selector").semantics { contentDescription = ratioDescription },
                        ) {
                            Text(ratios[selectedRatio].first)
                            Icon(FujiIcons.UnfoldMore, null, Modifier.padding(start = 8.dp))
                        }
                        FujiAnchoredMenu(
                            expanded = ratioMenuOpen,
                            onDismissRequest = { ratioMenuOpen = false },
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            groups = listOf({
                                ratios.forEachIndexed { index, (name, value) ->
                                    DropdownMenuItem(
                                        selected = index == selectedRatio,
                                        enabled = geometryReady,
                                        onClick = {
                                            ratio = value
                                            originalRatio = index == 1
                                            if (value > 0) crop = crop.fitAspect(value, imageAspect)
                                            ratioMenuOpen = false
                                        },
                                        text = { Text(name) },
                                        shapes = MenuDefaults.itemShape(index, ratios.size),
                                        selectedLeadingIcon = { Icon(FujiIcons.Check, null) },
                                        colors = MenuDefaults.selectableItemVibrantColors(),
                                    )
                                }
                            }),
                        )
                    }
                    FilledTonalIconButton(onClick = { rotate(-1) }) {
                        Icon(FujiIcons.Refresh, stringResource(R.string.raw_editor_rotate_left), Modifier.graphicsLayer { scaleX = -1f })
                    }
                    FilledTonalIconButton(onClick = { rotate(1) }) {
                        Icon(FujiIcons.Refresh, stringResource(R.string.raw_editor_rotate_right))
                    }
                }
                Text(description, style = MaterialTheme.typography.bodySmall)
                val photo: @Composable (Modifier) -> Unit = { modifier ->
                    Box(modifier.background(Color.Black), contentAlignment = Alignment.Center) {
                        Image(bitmap.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                        Canvas(Modifier.fillMaxSize().testTag("raw-crop-preview").onSizeChanged { canvasSize = it }
                            .semantics {
                                contentDescription = description
                                customActions = if (!geometryReady) emptyList() else listOf(
                                    CustomAccessibilityAction(labels[0]) { crop = crop.move(-.02f, 0f); true },
                                    CustomAccessibilityAction(labels[1]) { crop = crop.move(.02f, 0f); true },
                                    CustomAccessibilityAction(labels[2]) { crop = crop.move(0f, -.02f); true },
                                    CustomAccessibilityAction(labels[3]) { crop = crop.move(0f, .02f); true },
                                    CustomAccessibilityAction(labels[4]) { crop = scaleCrop(crop, .9f); true },
                                    CustomAccessibilityAction(labels[5]) { crop = scaleCrop(crop, 1.1f); true },
                                )
                            }
                            .pointerInput(bitmap, canvasSize, geometryReady) {
                                if (!geometryReady) return@pointerInput
                                val bounds = imageBounds(Size(canvasSize.width.toFloat(), canvasSize.height.toFloat()), aspect)
                                var corner = -1
                                detectDragGestures(
                                    onDragStart = { point ->
                                        val rect = cropBounds(currentCrop, bounds)
                                        val corners = listOf(rect.topLeft, rect.topRight, rect.bottomRight, rect.bottomLeft)
                                        corner = corners.indices.minBy { (point - corners[it]).getDistance() }
                                            .takeIf { (point - corners[it]).getDistance() <= 32.dp.toPx() }?.plus(1)
                                            ?: if (rect.contains(point)) 0 else -1
                                    },
                                    onDrag = { change, delta ->
                                        if (corner >= 0) {
                                            change.consume()
                                            val dx = delta.x / bounds.width
                                            val dy = delta.y / bounds.height
                                            crop = if (corner == 0) currentCrop.move(dx, dy) else currentCrop.resize(
                                                dx, dy, corner == 1 || corner == 4, corner <= 2,
                                                if (currentRatio == 0f) 0f else currentRatio / imageAspect,
                                            )
                                        }
                                    },
                                )
                            }) {
                            val bounds = imageBounds(size, aspect)
                            val rect = cropBounds(crop, bounds)
                            val shade = Color.Black.copy(alpha = .6f)
                            drawRect(shade, bounds.topLeft, Size(bounds.width, rect.top - bounds.top))
                            drawRect(shade, Offset(bounds.left, rect.bottom), Size(bounds.width, bounds.bottom - rect.bottom))
                            drawRect(shade, Offset(bounds.left, rect.top), Size(rect.left - bounds.left, rect.height))
                            drawRect(shade, Offset(rect.right, rect.top), Size(bounds.right - rect.right, rect.height))
                            drawRect(Color.White, rect.topLeft, rect.size, style = Stroke(2.dp.toPx()))
                            val divisions = if (initial.straighten == 0f) 3 else 8
                            for (line in 1 until divisions) {
                                val x = rect.left + rect.width * line / divisions
                                val y = rect.top + rect.height * line / divisions
                                drawLine(Color.White.copy(alpha = .5f), Offset(x, rect.top), Offset(x, rect.bottom))
                                drawLine(Color.White.copy(alpha = .5f), Offset(rect.left, y), Offset(rect.right, y))
                            }
                            for (point in listOf(rect.topLeft, rect.topRight, rect.bottomLeft, rect.bottomRight)) drawCircle(Color.White, 6.dp.toPx(), point)
                        }
                    }
                }
                val straightenControl: @Composable () -> Unit = {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$straightenLabel  ${straighten.formatValue(java.util.Locale.getDefault())}",
                            Modifier.pointerInput(Unit) { detectTapGestures(onDoubleTap = { currentStraighten.onChange(0f) }) },
                            style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        RawTickDial(straighten, Modifier.fillMaxWidth().semantics {
                            contentDescription = straightenLabel
                            progressBarRangeInfo = ProgressBarRangeInfo(straighten.value, straighten.range)
                            setProgress { value -> currentStraighten.onChange(currentStraighten.snap(value)); true }
                            customActions = listOf(CustomAccessibilityAction(resetStraighten) { currentStraighten.onChange(0f); true })
                        })
                        Box(Modifier.height(4.dp).fillMaxWidth()) { if (rendering) LinearProgressIndicator(Modifier.fillMaxWidth()) }
                    }
                }
                val controls: @Composable (Modifier) -> Unit = { modifier ->
                    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                            TextButton(onClick = { crop = RawCrop(); ratio = 0f; originalRatio = false; onAdjust(initial.rotated(0).copy(crop = RawCrop(), straighten = 0f)) }) { Text(stringResource(R.string.raw_editor_reset)) }
                            Button(onClick = { onApply(initial.copy(crop = crop)) }, enabled = geometryReady) { Text(stringResource(R.string.raw_editor_crop_apply)) }
                        }
                    }
                }
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    if (maxWidth >= 700.dp) Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            photo(Modifier.weight(1f).fillMaxWidth())
                            straightenControl()
                        }
                        controls(Modifier.width(280.dp).verticalScroll(rememberScrollState()))
                    } else Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        photo(Modifier.weight(1f).fillMaxWidth())
                        straightenControl()
                        controls(Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

internal fun imageBounds(size: Size, aspect: Float): Rect {
    val width = minOf(size.width, size.height * aspect)
    val height = width / aspect
    return Rect((size.width - width) / 2, (size.height - height) / 2, (size.width + width) / 2, (size.height + height) / 2)
}

private fun cropBounds(crop: RawCrop, image: Rect) = Rect(image.left + crop.left * image.width,
    image.top + crop.top * image.height, image.left + crop.right * image.width, image.top + crop.bottom * image.height)

private fun scaleCrop(crop: RawCrop, factor: Float): RawCrop {
    val maximum = minOf(1 / crop.width, 1 / crop.height)
    val minimum = minOf(maximum, maxOf(.05f / crop.width, .05f / crop.height))
    val scale = factor.coerceIn(minimum, maximum)
    val width = crop.width * scale
    val height = crop.height * scale
    val left = ((crop.left + crop.right - width) / 2).coerceIn(0f, 1 - width)
    val top = ((crop.top + crop.bottom - height) / 2).coerceIn(0f, 1 - height)
    return RawCrop(left, top, left + width, top + height)
}
